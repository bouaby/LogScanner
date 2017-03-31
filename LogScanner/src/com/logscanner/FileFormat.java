/*************************************************************************
 * Author: Younes Bouab
 * Email:  YBINSF@YAHOO.COM
 * Date:   01-01-2017
 *************************************************************************/

package com.logscanner;

public class FileFormat {

	// HTML Formatting
	private final static String sHTML_MIME = "text/html";
	private final static String sHTML_LINE_BREAK = "<br>\n";
	private final static String sHTML_BOLD = "<b>";
	private final static String sHTML_BOLD_POST = "</b>";
	private final static String sHTML_HEADER = "<html>\n<header>\n"
			+ "<title>Endur Support: LogScanner v0.1 (Beta)</title>\n</header>"
			+ "<body style=\"font:11pt calibri,geneva,sans-serif;margin-top:0px\">";
	private final static String sHTML_FOOTER = "\n</body>\n</html>";
	private static final String sHTML_COLOR = "<font color='#0000ff'>";
	private static final String sHTML_COLOR_POST = "</font>";
	private static final String sHTML_REPORT_FILE_EXTENSION = "html";

	private String mime = "text/plain";
	private String header = "*****************************************************************************************************************";
	private String footer = "";
	private String headerLine = "--------------------------------------------------------------------------------------"
			+ "----------------------------------------------------------------------------------------------";
	private String fileHeaderLine = "--------------------------------------------------------------------------------------"
			+ "----------------------------------------------------------------------------------------------";
	private String lineBreak = "\n";
	private String bold = "*";
	private String boldPost = "*";
	private String color = "";
	private String colorPost = "";
	private String reportFileExtension = "txt";

	protected FileFormat(String mime) {
		if (!Utils.isEmpty(mime)) {
			setMime(sHTML_MIME);
			setHeader(sHTML_HEADER);
			setFooter(sHTML_FOOTER);
			setLineBreak(sHTML_LINE_BREAK);
			setBold(sHTML_BOLD);
			setBoldPost(sHTML_BOLD_POST);
			setColor(sHTML_COLOR);
			setColorPost(sHTML_COLOR_POST);
			setReportFileExtension(sHTML_REPORT_FILE_EXTENSION);
		}
	}

	public String getReportFileExtension() {
		return reportFileExtension;
	}

	public void setReportFileExtension(String reportFileExtension) {
		this.reportFileExtension = reportFileExtension;
	}

	public String getMime() {
		return mime;
	}

	public void setMime(String mime) {
		this.mime = mime;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getFooter() {
		return footer;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}

	public String getHeaderLine() {
		return headerLine;
	}

	public void setHeaderLine(String headerLine) {
		this.headerLine = headerLine;
	}

	public String getFileHeaderLine() {
		return fileHeaderLine;
	}

	public void setFileHeaderLine(String fileHeaderLine) {
		this.fileHeaderLine = fileHeaderLine;
	}

	public String getLineBreak() {
		return lineBreak;
	}

	public void setLineBreak(String lineBreak) {
		this.lineBreak = lineBreak;
	}

	public String getBold() {
		return bold;
	}

	public void setBold(String bold) {
		this.bold = bold;
	}

	public String getBoldPost() {
		return boldPost;
	}

	public void setBoldPost(String boldPost) {
		this.boldPost = boldPost;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getColorPost() {
		return colorPost;
	}

	public void setColorPost(String colorPost) {
		this.colorPost = colorPost;
	}
}
