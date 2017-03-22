/*************************************************************************
 * Author: Younes Bouab
 * Email:  YBINSF@YAHOO.COM
 * Date:   01-01-2017
 *************************************************************************/

package com.logscanner;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;

public class LogScanner {

	// Global Variables
	private final static String MIME_TEXT="utf-8"; 
	private final static String MIME_HTML="text/html; charset=utf-8"; 
	private final static int iMAX_NUM_THREADS = 1000000;
	private Date dLSStartDate = null;
	private StringBuffer sbMainReportContent = null;
	private double dScanFilesSize = 0;
	private Props props = null;

	// Thread Handlers
	private int iThreadsCounter = 0;
	private Map<String, ScanThread> alScanThreadsHandler = null;
	private Map<String, TranslateThread> alTranslateThreadsHandler = null;

	// Main Method
	public static void main(String[] args) throws Exception {
		@SuppressWarnings("unused")
		LogScanner logMaster = new LogScanner();
		logMaster = null;
	}

	// Constructor
	private LogScanner() throws Exception {

		// Load property file
		dLSStartDate = new Date();
		String sStatusMsg = null;
		Date dStartDate = null;
		Date dEndDate = null;
		sbMainReportContent = new StringBuffer("");
		alScanThreadsHandler = new Hashtable<String, ScanThread>();
		alTranslateThreadsHandler = new Hashtable<String, TranslateThread>();

		// Load prop file
		dStartDate = new Date();
		props = new Props();
		props.load();

		dEndDate = new Date();
		Utils.checkReportFileExists(props, sbMainReportContent);

		// Print Report Header & properties details
		formatHeader(props, sbMainReportContent);
		formatLSTitle(dLSStartDate, "LogScanner started", sbMainReportContent,
				props);
		formatFileTitle(dLSStartDate, "Property File processing started for \""
				+ props.getPropFilePath(), sbMainReportContent, props);
		formatFileSummary(-1, props.getPropFileSize(), dLSStartDate, dEndDate,
				"Property File processing ended", sbMainReportContent, props);

		// Thread each file path translation and file scanning
		File file = null;
		for (Map.Entry<String, FileInfo> mLogFilesInfo : props.getMapFileInfo()
				.entrySet()) {
			dStartDate = new Date();
			FileInfo fileInfo = mLogFilesInfo.getValue();
			String sFilePath = fileInfo.getPath();
			if (Utils.isRegExp(sFilePath))
				addTranslateThreadToHandler(sFilePath);
			else {
				file = new File(sFilePath);
				if (file.isDirectory())
					addTranslateThreadToHandler(sFilePath);
				else if (file.exists())
					addScanThreadToHandler(null, sFilePath);
				else {
					sStatusMsg = "File \"" + sFilePath + "\" not found!";
					Utils.log(new Date(), sStatusMsg, sbMainReportContent,
							props);
				}
			}
		}

		// Join all threads to wait until all Threads are done then proceed
		for (Map.Entry<String, TranslateThread> entry : alTranslateThreadsHandler
				.entrySet()) {
			TranslateThread tt = entry.getValue();
			tt.join();
		}

		for (Map.Entry<String, ScanThread> entry : alScanThreadsHandler
				.entrySet()) {
			ScanThread st = entry.getValue();
			st.join();
		}

		// Append footer
		formatLSTitle(
				dEndDate,
				"File Translation/Scanning ended"
						+ Utils.getStats(dStartDate, dEndDate,
								(dScanFilesSize + props.getPropFileSize()), -1,
								(iThreadsCounter + 1)), sbMainReportContent,
				props);
		sbMainReportContent.append(props.getFileFormat().getFooter());

		// Writing Report File
		Utils.writeToReportFile(props, sbMainReportContent);
		sbMainReportContent = new StringBuffer("");

		// Read/Email Report File
		dStartDate = new Date();
		String emailStatus = "";
		if (!Utils.isEmpty(props.getSmtp())
				&& !Utils.isEmpty(props.getEmailAddress())) {
			dStartDate = new Date();
			sStatusMsg = "Sending email started";
			Utils.log(sStatusMsg);
			try {
				String sMime = MIME_TEXT;
				Email email = new Email(props.getSmtp(), props.getSmtpPort(),
						props.getStmpUser(), props.getStmpPassword());
				email.setAttachmentsProperties(props.getMaxEmailSizeBytes(),
						props.getMaxEmailFileSizeBytes());
				if(!Utils.isEmpty(props.getEmailHTML()))
				sMime=MIME_HTML;
				email.send(sMime, props.getEmailAddress(), null,
						null, props.getEmailAddress(), props.getEmailSubject(),
						Utils.readFile(props.getReportFilePath()),
						props.getReportFilePath(), props.getReportFolder());
				emailStatus = "Success!";
			} catch (Exception e) {
				Utils.handleException(new Date(), e,
						"emailing Report File using SMTP: \"" + props.getSmtp()
								+ "\"", null, props);
				emailStatus = e.toString() + "(SMTP: " + props.getSmtp() + ")";
			}

			// Email ended
			dEndDate = new Date();
			sStatusMsg = "Sending email ended"
					+ Utils.getStats(dStartDate, dEndDate, -1, -1, -1);
			Utils.log(dEndDate, sStatusMsg);

			// Write Email Status to file
			sbMainReportContent.append(props.getFileFormat().getLineBreak()
					+ sStatusMsg + " - Status: " + emailStatus);
			Utils.writeToReportFile(props, sbMainReportContent);
		}

		// The End!
		dEndDate = new Date();
		sStatusMsg = "LogScanner ended"
				+ Utils.getStats(dLSStartDate, dEndDate, -1, -1, -1);
		Utils.log(dEndDate, sStatusMsg);

		// Clean up
		alTranslateThreadsHandler = null;
		alScanThreadsHandler = null;
		sbMainReportContent = null;
		sStatusMsg = null;
		dStartDate = null;
		dEndDate = null;
		dLSStartDate = null;
		System.exit(0);
	}

	protected synchronized int addScanThreadToHandler(String sRegExp,
			String sFilePath) {
		int result = 0;
		try {
			if (iThreadsCounter < iMAX_NUM_THREADS) {
				// Get File Size
				File file = new File(sFilePath);
				double dFileSize = 0;
				dFileSize = file.length();
				FileInfo fileInfo = null;
				if (sRegExp != null)
					fileInfo = props.getMapFileInfo().get(sRegExp);
				else
					fileInfo = props.getMapFileInfo().get(sFilePath);

				fileInfo.setSize(dFileSize);
				dScanFilesSize += dFileSize;
				file = null;

				// ScanThread
				iThreadsCounter++;
				ScanThread scanThread = new ScanThread(iThreadsCounter,
						sRegExp, sFilePath, this);
				scanThread.start();
				alScanThreadsHandler.put(sFilePath, scanThread);
				result = iThreadsCounter;
			} else {
				result = -1;
				Utils.log("Maximum # of Threads reached, \"" + iThreadsCounter
						+ "\"! \"" + sFilePath + "\" will be ignored");
			}
		} catch (Exception e) {
			String sStatusMsg = "Exception occured while trying to add Scan File Thread \""
					+ sFilePath + "\"";
			Utils.log(new Date(), sStatusMsg);
			sbMainReportContent.append(sStatusMsg);
		}
		return result;
	}

	private synchronized int addTranslateThreadToHandler(String sKey) {
		int result = 0;
		try {
			if (iThreadsCounter < iMAX_NUM_THREADS) {
				// synchronized (alTranslateThreadsHandler) {

				iThreadsCounter++;
				TranslateThread translateThread = new TranslateThread(
						iThreadsCounter, sKey, this);
				translateThread.start();
				alTranslateThreadsHandler.put(sKey, translateThread);
				result = iThreadsCounter;
				// }
			} else {
				result = -1;
				Utils.log("Maximum # of files to translate files reached, \""
						+ iThreadsCounter + "\"! \"" + sKey
						+ "\" will be ignored");
			}
		} catch (Exception e) {
			String sStatusMsg = "Exception occured while trying to add Translated Path \""
					+ sKey + "\"";
			Utils.log(new Date(), sStatusMsg);
			sbMainReportContent.append(sStatusMsg);
		}
		return result;
	}

	protected static void formatFileTitle(Date dDate, String sStatusMsg,
			StringBuffer sbReportContent, Props props) {
		FileFormat fileFormat = props.getFileFormat();
		sbReportContent.append(fileFormat.getFileHeaderLine()
				+ fileFormat.getLineBreak() + fileFormat.getBold()
				+ fileFormat.getColor() + Utils.formatDate(dDate) + " - "
				+ sStatusMsg + fileFormat.getColorPost()
				+ fileFormat.getBoldPost() + fileFormat.getLineBreak()
				+ fileFormat.getFileHeaderLine() + fileFormat.getLineBreak());
	}

	protected static void formatLSTitle(Date dDate, String sStatusMsg,
			StringBuffer sbReportContent, Props props) {
		Utils.log(dDate, sStatusMsg);
		FileFormat fileFormat = props.getFileFormat();
		sbReportContent.append(fileFormat.getHeaderLine()
				+ fileFormat.getLineBreak() + fileFormat.getBold()
				+ fileFormat.getColor() + Utils.formatDate(dDate) + " - "
				+ sStatusMsg + fileFormat.getColorPost()
				+ fileFormat.getBoldPost() + fileFormat.getLineBreak()
				+ fileFormat.getHeaderLine() + fileFormat.getLineBreak()
				+ fileFormat.getBoldPost());
	}

	protected static void formatFileSummary(int iNumLinesPassFilter,
			double dFileSize, Date dStartDate, Date dEndDate,
			String sStatusMsg, StringBuffer sbReportContent, Props props) {
		FileFormat fileFormat = props.getFileFormat();
		Utils.log(dEndDate, sStatusMsg);
		sbReportContent.append(fileFormat.getColor()
				+ Utils.formatDate(dEndDate)
				+ " - "
				+ sStatusMsg
				+ Utils.getStats(dStartDate, dEndDate, dFileSize,
						iNumLinesPassFilter, 1) + fileFormat.getColorPost()
				+ fileFormat.getLineBreak() + fileFormat.getLineBreak()
				+ fileFormat.getLineBreak());
	}

	private static void formatHeader(Props props,
			StringBuffer sbMainReportContent) {
		FileFormat fileFormat = props.getFileFormat();
		String sFilterMsg = "Property File: \"" + props.getPropFilePath()
				+ "\"" + fileFormat.getLineBreak() + "Filter Start Date: "
				+ props.getFilterStartDate() + fileFormat.getLineBreak()
				+ "Filter End Date: " + props.getFilterEndDate()
				+ fileFormat.getLineBreak() + "Filter And Words: "
				+ props.getFilterAndWords() + fileFormat.getLineBreak()
				+ "Filter Or Words: " + props.getFilterOrWords()
				+ fileFormat.getLineBreak() + "Filter Not Words: "
				+ props.getFilterNotWords() + fileFormat.getLineBreak();

		sbMainReportContent.append(fileFormat.getHeader() + sFilterMsg);
	}

	public Props getProps() {
		return props;
	}

	public void appendToSBMainReportContent(StringBuffer sb) {
		sbMainReportContent.append(sb);
	}
}
