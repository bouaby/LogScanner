/*************************************************************************
 * Author: Younes Bouab
 * Email:  YBINSF@YAHOO.COM
 * Date:   01-01-2017
 *************************************************************************/

package com.logscanner;

import java.util.ArrayList;
import java.util.Date;

public class TranslateThread extends Thread {
	private int iThreadId = -1;
	private LogScanner logScanner = null;

	protected TranslateThread(int iThreadId, String key, LogScanner logScanner) {
		this.setName(key);
		this.iThreadId = iThreadId;
		this.logScanner = logScanner;
	}

	public void run() {
		String sRegExpression = this.getName();
		Date dStartDate = null;
		Date dEndDate = null;
		String sStatusMsg = null;
		StringBuffer sbReportContent = new StringBuffer("");
		Props props = null;
		try {

			// ///////////////////////////////////////////////
			// Start Thread
			// ///////////////////////////////////////////////
			dStartDate = new Date();
			props = logScanner.getProps();
			sStatusMsg = "Translation Thread #" + iThreadId + " \""
					+ sRegExpression + "\" started";
			Utils.log(sStatusMsg);
			LogScanner.formatFileTitle(dStartDate, sStatusMsg, sbReportContent,
					props);

			// ///////////////////////////////////////////////
			// Translate Path Ended
			// ///////////////////////////////////////////////
			ArrayList<String> saTranslatedFilePaths = Utils
					.getDirAndRegExpPathFiles(sRegExpression);
			dEndDate = new Date();
			int iNumTranslatedFilePaths = saTranslatedFilePaths.size();
			sStatusMsg = "Translation Thread #" + iThreadId
					+ " ended! Files Count: " + iNumTranslatedFilePaths
					+ " files";
			LogScanner.formatFileSummary(-1, 0, dStartDate, dEndDate,
					sStatusMsg, sbReportContent, props);

			// ///////////////////////////////////////////////
			// Start Scan Threads
			// ///////////////////////////////////////////////
			for (int i = 0; i < iNumTranslatedFilePaths; i++)
				logScanner.addScanThreadToHandler(sRegExpression,
						saTranslatedFilePaths.get(i));

		} catch (Exception e) {
			sStatusMsg = "proessing Translation Thread #" + iThreadId;
			Utils.handleException(new Date(), e, sStatusMsg, sbReportContent,
					props);
		} finally {

			// Add Translate File Report to main Report File
			logScanner.appendToSBMainReportContent(sbReportContent);

			// ///////////////////////////////////////////////
			// Cleanup
			// ///////////////////////////////////////////////
			dStartDate = null;
			dEndDate = null;
			sbReportContent = null;
		}
	}
}
