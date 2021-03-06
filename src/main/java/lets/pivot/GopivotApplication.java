package lets.pivot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.alibaba.excel.EasyExcel;

import lets.pivot.properties.Constants;
import lets.pivot.properties.PivotProperties;

@SpringBootApplication
public class GopivotApplication implements CommandLineRunner {

	@Autowired
	private PivotProperties pivotProp;
	private Map<String, List<Object>> result = new TreeMap<String, List<Object>>();

	public static void main(String[] args) {
		SpringApplication.run(GopivotApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		String originalCsvFileName = pivotProp.getOriginalCsvFileName();
		String errorLogFileName = pivotProp.getErrorLogFileName();
		String resultXlsxFileName = pivotProp.getResultXlsxFileName();

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(originalCsvFileName), "UTF-8"));) {
			// 檢查計算欄位是否都存在
			List<String> headers = Arrays.asList(reader.readLine().split("\"\\,\"")).stream()
					.map(h -> h.replaceAll(Constants.UTF8_BOM, "").replaceAll("\"", "")).collect(Collectors.toList());

			int[] rowNamesIndex = new int[pivotProp.getRowNames().size()];
			int[][] sumColumnNamesIndex = new int[pivotProp.getSumColumnNames().size()][2];

			for (int i = 0; i < pivotProp.getRowNames().size(); i++) {
				if (!headers.contains(pivotProp.getRowNames().get(i))) {
					throw new Exception(String.format("欄位名稱「%s」不存在於表頭", pivotProp.getRowNames().get(i)));
				}
				rowNamesIndex[i] = headers.indexOf(pivotProp.getRowNames().get(i));
			}

			if (!headers.contains(pivotProp.getCreditDebitName())) {
				throw new Exception(String.format("欄位名稱「%s」不存在於表頭", pivotProp.getCreditDebitName()));
			}
			int creditDebitIndex = headers.indexOf(pivotProp.getCreditDebitName());

			for (int i = 0; i < pivotProp.getSumColumnNames().size(); i++) {
				for (int j = 0; j < pivotProp.getSumColumnNames().get(i).length; j++) {
					if (!headers.contains(pivotProp.getSumColumnNames().get(i)[j])) {
						throw new Exception(String.format("欄位名稱「%s」不存在於表頭", pivotProp.getSumColumnNames().get(i)[j]));
					}
					sumColumnNamesIndex[i][j] = headers.indexOf(pivotProp.getSumColumnNames().get(i)[j]);
				}
			}

			// 產生表頭
			List<List<String>> resultHeaders = new ArrayList<List<String>>();
			for (int i = 0; i < pivotProp.getRowNames().size(); i++) {
				resultHeaders.add(Arrays.asList(pivotProp.getRowNames().get(i)));
			}
			for (int i = 0; i < pivotProp.getSumColumnNames().size(); i++) {
				resultHeaders.add(Arrays.asList(pivotProp.getSumColumnNames().get(i)[0] + "-加總"));
			}

			// 計算樞紐
			String nextLineStr;
			String[] nextLine;
			String[] mapKeyArr = new String[pivotProp.getRowNames().size()];
			String mapKey;
			List<Object> lineContent;
			String creditDebit;
			BigDecimal thisValue;
			while ((nextLineStr = reader.readLine()) != null) {
				nextLine = nextLineStr.split(",");
				for (int i = 0; i < nextLine.length; i++) {
					nextLine[i] = nextLine[i].replaceAll("\\t", "").replaceAll("\"", "");
				}

				for (int i = 0; i < pivotProp.getRowNames().size(); i++) {
					mapKeyArr[i] = nextLine[rowNamesIndex[i]];
				}
				mapKey = String.join("^", mapKeyArr);
				creditDebit = nextLine[creditDebitIndex];

				if (result.containsKey(mapKey)) {
					lineContent = result.get(mapKey);
					for (int i = 0; i < pivotProp.getSumColumnNames().size(); i++) {
						thisValue = new BigDecimal(nextLine[sumColumnNamesIndex[i][0]]);
						lineContent.set(i + pivotProp.getRowNames().size(),
								new BigDecimal(lineContent.get(i + pivotProp.getRowNames().size()).toString())
										.add(getValueByCreditDebit(creditDebit, thisValue)));
					}
				} else {
					lineContent = new ArrayList<Object>();
					for (int i = 0; i < pivotProp.getRowNames().size(); i++) {
						lineContent.add(nextLine[rowNamesIndex[i]]);
					}
					for (int i = 0; i < pivotProp.getSumColumnNames().size(); i++) {
						thisValue = new BigDecimal(nextLine[sumColumnNamesIndex[i][0]]);
						lineContent.add(getValueByCreditDebit(creditDebit, thisValue));
					}
				}
				result.put(mapKey, lineContent);
			}

			EasyExcel.write(resultXlsxFileName).head(resultHeaders).sheet("Sheet1")
					.doWrite(new ArrayList<>(result.values()));
		} catch (Exception e) {
			Files.write(new File(errorLogFileName).toPath(), ExceptionUtils.getStackTrace(e).getBytes(),
					StandardOpenOption.CREATE);
		}
	}

	private BigDecimal getValueByCreditDebit(String creditDebit, BigDecimal orgValue) {
		return "D".equals(creditDebit) ? orgValue : orgValue.negate();
	}

}