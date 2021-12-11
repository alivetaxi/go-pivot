package lets.pivot.properties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "pivot")
public class PivotProperties {

	private String workingDir;
	private DirAndFile originalCsv;
	private DirAndFile errorLog;
	private DirAndFile resultXlsx;
	private List<String> rowNames;
	private List<String> sumColumnNames;

	@Data
	public static class DirAndFile {
		private String dirName;
		private String fileName;
	}

	public List<String> getRowNames() {
		return this.rowNames.stream().map(h -> h.replaceAll(Constants.UTF8_BOM, "")).collect(Collectors.toList());
	}

	public List<String[]> getSumColumnNames() {
		return this.sumColumnNames.stream().map(h -> h.replaceAll(Constants.UTF8_BOM, "").split("\\|"))
				.collect(Collectors.toList());
	}

	public String getOriginalCsvFileName() {
		return String.join("/", this.workingDir, this.originalCsv.dirName, this.originalCsv.fileName);
	}

	public String getErrorLogFileName() {
		return String.format(String.join("/", this.workingDir, this.errorLog.dirName, this.errorLog.fileName),
				new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
	}

	public String getResultXlsxFileName() {
		return String.join("/", this.workingDir, this.resultXlsx.dirName, this.resultXlsx.fileName);
	}

}
