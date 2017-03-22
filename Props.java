/*************************************************************************
 * Author: Younes Bouab
 * Email:  YBINSF@YAHOO.COM
 * Date:   01-01-2017
 *************************************************************************/

package com.logscanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Props {

	// Constants
	private static final String sSTD_DATE_FORMAT = "MM-dd-yyyy hh24:mm:ss";
	private static final String sPROP_FILE_NAME = "LogScanner.properties";
	private static final String sREPORT_OUTPUT_FOLDER = "output";
	private static final String sREPORT_FILE_NAME = "LogScanner_Report";
	private static final int iMAX_EMAIL_SIZE_BYTES = 15000000;
	private static final int iMAX_EMAIL_FILE_SIZE_BYTES = 15000000;

	// Prop File Path
	private String currentDir = null;
	private String propFilePath = null;
	private long propFileSize = 0l;

	// Property Names
	private static final int iMAX_FILES_IN_PROP_FILE = 1000;
	private static final String LOG_FILE = "LOG_FILE_";
	private static final String LOG_FILE_DATE_FORMAT = "LOG_FILE_DATE_FORMAT_";
	private static final String LOG_FILE_DATE_START = "LOG_FILE_DATE_START_";
	private static final String sFILTER_SEPERATOR = ",";
	private static final String sFILTER_AND_WORDS = "FILTER_AND_WORDS";
	private static final String sFILTER_OR_WORDS = "FILTER_OR_WORDS";
	private static final String sFILTER_NOT_WORDS = "FILTER_NOT_WORDS";
	private static final String sFILTER_START_DATE = "FILTER_START_DATE";
	private static final String sFILTER_END_DATE = "FILTER_END_DATE";
	private static final String sEMAIL_SMTP = "EMAIL_SMTP";
	private static final String sEMAIL_SMTP_PORT = "EMAIL_SMTP_PORT";
	private static final String sEMAIL_USER = "EMAIL_USER";
	private static final String sEMAIL_PASSWORD = "EMAIL_PASSWORD";
	private static final String sEMAIL_SUBJECT = "EMAIL_SUBJECT";
	private static final String sEMAIL_ADDRESS = "EMAIL_ADDRESS";
	private static final String sEMAIL_HTML = "EMAIL_HTML";
	private static final String sEMAIL_MAX_SIZE_IN_BYTES = "EMAIL_MAX_SIZE_IN_BYTES";
	private static final String sEMAIL_MAX_FILE_SIZE_IN_BYTES = "EMAIL_MAX_FILE_SIZE_IN_BYTES";
	private static final String sREPORT_FOLDER = "OUTPUT_FOLDER";

	// Filter values
	private String filterAndWords = null;
	private String filterOrWords = null;
	private String filterNotWords = null;
	private String filterStartDate = null;
	private String filterEndDate = null;
	private String[] filterAndWordsArray = null;
	private String[] filterOrWordsArray = null;
	private String[] filterNotWordsArray = null;

	// Email values
	protected String smtp = null;
	protected String smtpPort = null;
	protected String smtpUser = null;
	protected String smtpPassword = null;
	protected String emailSubject = null;
	protected String emailAddress = null;
	protected String emailHTML = null;
	protected long maxEmailSizeBytes = 0;
	protected long maxEmailFileSizeBytes = 0;

	// Output Folder
	private String reportFilePath = null;
	private String reportFolder = null;
	private String reportFileName = null;

	// FilesInfo
	private Map<String, FileInfo> mapFileInfo = null;

	// Formatting Class
	private FileFormat fileFormat = null;

	protected void load() throws IOException {
		// Set needed variables
		setCurrentDir(null);

		Properties pProps = null;
		FileInputStream finMyFIN = null;
		File file = new File(getPropFilePath());
		if (file.exists()) {
			setPropFileSize(file.length());

			// Load Properties
			pProps = new Properties();
			finMyFIN = new FileInputStream(getPropFilePath());
			pProps.load(finMyFIN);

			// Get email properties
			setSmtp(pProps.getProperty(sEMAIL_SMTP));
			setSmtpPort(pProps.getProperty(sEMAIL_SMTP_PORT));
			setSmtpUser(pProps.getProperty(sEMAIL_USER));
			setSmtpPassword(pProps.getProperty(sEMAIL_PASSWORD));
			setEmailSubject(pProps.getProperty(sEMAIL_SUBJECT));
			setEmailAddress(pProps.getProperty(sEMAIL_ADDRESS));
			setMaxEmailSizeBytes(pProps.getProperty(sEMAIL_MAX_SIZE_IN_BYTES));
			setMaxEmailFileSizeBytes(pProps
					.getProperty(sEMAIL_MAX_FILE_SIZE_IN_BYTES));

			// Determines FileFormatting
			setEmailHTML(pProps.getProperty(sEMAIL_HTML));
			fileFormat = new FileFormat(getEmailHTML());

			// Get output folder
			setReportFolder(pProps.getProperty(sREPORT_FOLDER));
			setReportFileName(null);
			setReportFilePath(null);

			// Filter
			setFilterAndWords(pProps.getProperty(sFILTER_AND_WORDS));
			setFilterOrWords(pProps.getProperty(sFILTER_OR_WORDS));
			setFilterNotWords(pProps.getProperty(sFILTER_NOT_WORDS));
			setFilterAndWordsArray(Utils.prepFilter(getFilterAndWords(),
					getEmailHTML(), sFILTER_SEPERATOR));
			setFilterOrWordsArray(Utils.prepFilter(getFilterOrWords(),
					getEmailHTML(), sFILTER_SEPERATOR));
			setFilterNotWordsArray(Utils.prepFilter(getFilterNotWords(),
					getEmailHTML(), sFILTER_SEPERATOR));
			setFilterStartDate(Utils.prepString(pProps
					.getProperty(sFILTER_START_DATE)));
			setFilterStartDate(Utils.translateDate(getFilterStartDate(),
					"start"));
			setFilterEndDate(Utils.prepString(pProps
					.getProperty(sFILTER_END_DATE)));
			setFilterEndDate(Utils.translateDate(getFilterEndDate(), "end"));

			// Check Date Format
			setFilterEndDate(Utils.formatDate(
					Utils.convertToDate(getFilterEndDate(), sSTD_DATE_FORMAT),
					sSTD_DATE_FORMAT));

			// Get & Store File Properties
			mapFileInfo = new HashMap<String, FileInfo>();
			FileInfo previousFileInfo = new FileInfo();
			for (int i = 1; i <= iMAX_FILES_IN_PROP_FILE; i++) {
				String filePath = null;
				filePath = pProps.getProperty(LOG_FILE + i);
				if (!Utils.isEmpty(filePath)) {
					FileInfo fileInfo = new FileInfo();

					// path
					fileInfo.setTranslatedFromRegExp(filePath);
					fileInfo.setPath(filePath);

					// date format
					String sDateFormat = pProps
							.getProperty(LOG_FILE_DATE_FORMAT + i);
					if (sDateFormat == null)
						sDateFormat = previousFileInfo.getDateFormat();
					fileInfo.setDateFormat(sDateFormat);

					// date start position
					int iDateStartPos = -1;
					String sDateStartPos = pProps
							.getProperty(LOG_FILE_DATE_START + i);
					if (sDateStartPos == null)
						iDateStartPos = previousFileInfo.getDateStartPos();
					else
						iDateStartPos = Integer.parseInt(sDateStartPos);
					fileInfo.setDateStartPos(iDateStartPos);

					mapFileInfo.put(filePath, fileInfo);
					previousFileInfo = fileInfo;
				} else
					break;
			}
		}

		// Clean up
		if (finMyFIN != null)
			finMyFIN.close();
		pProps = null;
		finMyFIN = null;
		file = null;
	}

	private void setSmtpPassword(String smtpPassword) {
		this.smtpPassword = smtpPassword;
	}

	private void setSmtpUser(String smtpUser) {
		this.smtpUser = smtpUser;
	}

	// Getters/Setters
	public String getPropFilePath() {
		propFilePath = getCurrentDir() + sPROP_FILE_NAME;
		return propFilePath;
	}

	public File getPropFile() {
		File propFile = new File(getPropFilePath());
		return propFile;
	}

	public long getPropFileSize() {
		return propFileSize;
	}

	public void setPropFileSize(long propFileSize) {
		this.propFileSize = propFileSize;
	}

	public String getCurrentDir() {
		return currentDir;
	}

	public void setCurrentDir(String currentDir) {
		if (Utils.isEmpty(currentDir))
			this.currentDir = System.getProperty("user.dir") + "/";
		else
			this.currentDir = currentDir;
	}

	public String getFilterAndWords() {
		return filterAndWords;
	}

	public void setFilterAndWords(String filterAndWords) {
		this.filterAndWords = filterAndWords;
	}

	public String getFilterOrWords() {
		return filterOrWords;
	}

	public void setFilterOrWords(String filterOrWords) {
		this.filterOrWords = filterOrWords;
	}

	public String getFilterNotWords() {
		return filterNotWords;
	}

	public void setFilterNotWords(String filterNotWords) {
		this.filterNotWords = filterNotWords;
	}

	public String getFilterStartDate() {
		return filterStartDate;
	}

	public void setFilterStartDate(String filterStartDate) {
		this.filterStartDate = filterStartDate;
	}

	public String getFilterEndDate() {
		return filterEndDate;
	}

	public void setFilterEndDate(String filterEndDate) {
		this.filterEndDate = filterEndDate;
	}

	public String[] getFilterAndWordsArray() {
		return filterAndWordsArray;
	}

	public void setFilterAndWordsArray(String[] filterAndWordsArray) {
		this.filterAndWordsArray = filterAndWordsArray;
	}

	public String[] getFilterOrWordsArray() {
		return filterOrWordsArray;
	}

	public void setFilterOrWordsArray(String[] filterOrWordsArray) {
		this.filterOrWordsArray = filterOrWordsArray;
	}

	public String[] getFilterNotWordsArray() {
		return filterNotWordsArray;
	}

	public void setFilterNotWordsArray(String[] filterNotWordsArray) {
		this.filterNotWordsArray = filterNotWordsArray;
	}

	public String getSmtp() {
		return smtp;
	}

	public void setSmtp(String smtp) {
		this.smtp = smtp;
	}

	public void setSmtpPort(String smtpPort) {
		this.smtpPort = smtpPort;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getEmailHTML() {
		return emailHTML;
	}

	public void setEmailHTML(String emailHTML) {
		this.emailHTML = emailHTML;
		fileFormat = new FileFormat(emailHTML);
	}

	public FileFormat getFileFormat() {
		return fileFormat;
	}

	public String getReportFilePath() {
		return reportFilePath;
	}

	public void setReportFilePath(String reportFilePath) {
		if (Utils.isEmpty(reportFilePath))
			this.reportFilePath = getCurrentDir() + getReportFolder()
					+ getReportFileName();
		else
			this.reportFilePath = reportFilePath;
	}

	public void setReportFolder(String reportFolder) {
		if (Utils.isEmpty(reportFolder))
			this.reportFolder = sREPORT_OUTPUT_FOLDER + "/";
		else
			this.reportFolder = reportFolder + "/";

		// Create Directory
		File file = new File(getCurrentDir() + this.reportFolder);
		if (!file.isDirectory())
			file.mkdir();
	}

	public String getReportFolder() {
		return reportFolder;
	}

	public String getReportFileName() {
		return reportFileName;
	}

	public void setReportFileName(String reportFileName) {
		if (Utils.isEmpty(reportFileName))
			this.reportFileName = sREPORT_FILE_NAME + "."
					+ getFileFormat().getReportFileExtension();
		else
			this.reportFileName = reportFileName;
	}

	public Map<String, FileInfo> getMapFileInfo() {
		return mapFileInfo;
	}

	public void setMapFileInfo(Map<String, FileInfo> mapFileInfo) {
		this.mapFileInfo = mapFileInfo;
	}

	public void setPropFilePath(String propFilePath) {
		this.propFilePath = propFilePath;
	}

	public void setMaxEmailSizeBytes(String sMaxEmailSizeBytes) {

		if (sMaxEmailSizeBytes == null)
			this.maxEmailSizeBytes = iMAX_EMAIL_SIZE_BYTES;
		else
			this.maxEmailSizeBytes = Long.parseLong(sMaxEmailSizeBytes);
	}

	public long getMaxEmailSizeBytes() {
		return maxEmailSizeBytes;
	}

	public void setMaxEmailFileSizeBytes(String sMaxEmailFileSizeBytes) {

		if (sMaxEmailFileSizeBytes == null)
			this.maxEmailFileSizeBytes = iMAX_EMAIL_FILE_SIZE_BYTES;
		else
			this.maxEmailFileSizeBytes = Long.parseLong(sMaxEmailFileSizeBytes);
	}

	public long getMaxEmailFileSizeBytes() {
		return maxEmailFileSizeBytes;
	}

	protected static void checkReportFileExists(Props props,
			StringBuffer sbMainReportContent) {
		try {
			File file = new File(props.getReportFilePath());
			long lCounter = 1L;
			while (file.exists()) {
				String sUnique = "_" + lCounter + ".lsa";
				File file2 = new File(props.getReportFilePath() + sUnique);
				file.renameTo(file2);
				lCounter++;
			}
		} catch (Exception e) {
			Utils.handleException(new Date(), e, "renaming Report File",
					sbMainReportContent, props);

		}
	}

	public String getStmpUser() {
		return smtpUser;
	}

	public String getStmpPassword() {
		return smtpPassword;
	}

	public String getSmtpPort() {
		return smtpPort;
	}
}
