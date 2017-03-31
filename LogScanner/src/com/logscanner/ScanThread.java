package com.logscanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;

public class ScanThread extends Thread {
	private int iThreadId = -1;
	private LogScanner logScanner = null;
	private String sRregExp = null;

	protected ScanThread(int iThreadId, String sRregExp, String key,
			LogScanner logScanner) {
		this.iThreadId = iThreadId;
		this.sRregExp = sRregExp;
		this.setName(key);
		this.logScanner = logScanner;
	}

	public void run() {
		Props props = null;
		FileInfo fileInfo = null;
		String sFilePath = this.getName();
		boolean bDateParsed = false;
		boolean bPassedDateFilter = false;
		double dFileSize = 0L;
		String sFileDateFormat = null;
		int iFileDateStart = -1;
		String sStatusMsg = null;
		int iFileLinesPassedCount = 0;
		int iFileLinesCounter = 0;
		Date dStartDate = null;
		Date dEndDate = null;
		FileReader frFileReader = null;
		BufferedReader brBufferReader = null;
		StringBuffer sbReportContent = null;

		try {
			// ///////////////////////////////////////////////
			// Start Thread
			// ///////////////////////////////////////////////
			sbReportContent = new StringBuffer("");
			props = logScanner.getProps();
			if (sRregExp == null)
				fileInfo = props.getMapFileInfo().get(this.getName());
			else
				fileInfo = props.getMapFileInfo().get(sRregExp);
			dStartDate = new Date();
			sFileDateFormat = fileInfo.getDateFormat();
			iFileDateStart = fileInfo.getDateStartPos();
			dFileSize = fileInfo.getSize();
			sStatusMsg = "Scan Thread #" + iThreadId + " \"" + this.getName()
					+ "\" started";
			Utils.log(sStatusMsg);
			LogScanner.formatFileTitle(dStartDate, sStatusMsg, sbReportContent,
					props);
			File file = new File(sFilePath);
			frFileReader = new FileReader(file);
			brBufferReader = new BufferedReader(frFileReader);

			// Filter Tests
			boolean bPassedFilter = false;
			boolean bPreviousPassedFilter = false;
			String sFileLine = null;
			String sEncodedFileLine = null;
			String sPreviousFileLine = null;
			String sPreviousEncodedFileLine = null;

			// Read & test file line by line
			while ((sFileLine = brBufferReader.readLine()) != null) {
				// Initialize
				bPassedFilter = false;

				if (!Utils.isEmpty(sFileLine)) {
					// Check if line similar to previous line
					if (sFileLine.equals(sPreviousFileLine)) {
						bPassedFilter = bPreviousPassedFilter;
						sEncodedFileLine = sPreviousEncodedFileLine;
					}
					// Run Filter Tests
					else {
						// 0 Parse Date
						String sFileLineDate = Utils.getDateFromLine(sFileLine,
								sFileDateFormat, iFileDateStart);

						// 1 HTML Encode
						sEncodedFileLine = Utils.encodeHTML(sFileLine, props);

						if (!Utils.isEmpty(sFileLineDate)) {
							bDateParsed = true;

							// Check End Date
							bPassedFilter = Utils.compareEndDate(
									props.getFilterEndDate(),
									Utils.sSTD_DATE_FORMAT, sFileLineDate,
									sFileDateFormat);

							// End File Scan if did not pass End Date
							if (!bPassedFilter) {
								sStatusMsg = "Scan Thread #" + iThreadId
										+ " stopped at line #"
										+ iFileLinesCounter + "!  Line Date \""
										+ sFileLineDate
										+ "\" is after Filter End Date \""
										+ props.getFilterEndDate() + "\"";
								LogScanner.formatFileSummary(
										iFileLinesPassedCount, dFileSize,
										dStartDate, dEndDate, sStatusMsg,
										sbReportContent, props);

							}

							// Check Start Date
							bPassedFilter = Utils.compareStartDate(
									sFileLineDate, sFileDateFormat,
									props.getFilterStartDate(),
									Utils.sSTD_DATE_FORMAT);

							if (bPassedFilter)
								bPassedDateFilter = true;
						} else
							// Previous line must have passed Filter
							bPassedFilter = bPreviousPassedFilter;

						if (bPassedFilter) {
							// 4 Not Filter
							sEncodedFileLine = Utils.checkLineForWords(
									sEncodedFileLine, "not", props);
							if (sEncodedFileLine != null) {
								bPassedFilter = true;
								// 5 And Filter
								sEncodedFileLine = Utils.checkLineForWords(
										sEncodedFileLine, "and", props);
								if (sEncodedFileLine != null) {
									bPassedFilter = true;
									// 6 Or Words Filter
									sEncodedFileLine = Utils.checkLineForWords(
											sEncodedFileLine, "or", props);
									if (sEncodedFileLine != null)
										bPassedFilter = true;
									else
										bPassedFilter = false;
								} else
									bPassedFilter = false;
							} else
								bPassedFilter = false;
						}
					}
				}

				// ///////////////////////////////////
				// 7 Cumulative Filter Test
				// ///////////////////////////////////
				if (bPassedFilter) {
					sbReportContent.append((iFileLinesCounter+1) + ": "
							+ sEncodedFileLine
							+ props.getFileFormat().getLineBreak());
					iFileLinesPassedCount++;
				}

				// Set Previous Filter Test Results
				sPreviousFileLine = sFileLine;
				sPreviousEncodedFileLine = sEncodedFileLine;
				bPreviousPassedFilter = bPassedFilter;
				iFileLinesCounter++;
			}

			// ///////////////////////////////////////////////
			// Append Scan Statuses to Report and Log
			// ///////////////////////////////////////////////
			dEndDate = new Date();
			if (!bDateParsed)
				sbReportContent
						.append("Not a single date was parsed successfuly from file!"
								+ props.getFileFormat().getLineBreak());
			else if (!bPassedDateFilter)
				sbReportContent.append("Date parsed, but none passed Filter!"
						+ props.getFileFormat().getLineBreak());
			sStatusMsg = "Scan Thread #" + iThreadId + " ended";
			LogScanner.formatFileSummary(iFileLinesPassedCount, dFileSize,
					dStartDate, dEndDate, sStatusMsg, sbReportContent, props);

		} catch (Exception e) {
			sStatusMsg = "Scaning Thread #" + iThreadId + " \"";
			Utils.handleException(new Date(), e, sStatusMsg, sbReportContent,
					props);
		}

		finally {

			// Add Scan File Report to main Report File
			logScanner.appendToSBMainReportContent(sbReportContent);

			// ///////////////////////////////////////////////
			// Cleanup
			// ///////////////////////////////////////////////
			sFileDateFormat = null;
			sStatusMsg = null;
			sbReportContent = null;
			dStartDate = null;
			dEndDate = null;
			frFileReader = null;
			brBufferReader = null;
			sFilePath = null;
		}
	}
}
