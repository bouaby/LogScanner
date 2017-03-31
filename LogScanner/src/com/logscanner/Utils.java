/*************************************************************************
 * Author: Younes Bouab
 * Email:  YBINSF@YAHOO.COM
 * Date:   01-01-2017
 *************************************************************************/

package com.logscanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

	private static final String sNUMBER_FORMAT = "%1$,.3f";
	protected final static String sSTD_DATE_FORMAT = "MM-dd-yyyy HH:mm:ss";
	private final static String sREG_EXPRESSION = "^";
	private final static String sREG_EXPRESSION_POST = "$";
	private static final String sREG_EXPRESSION_START = "^(.*)";
	private static final String sREG_EXPRESSION_END = "(.*)$";
	private static final String FILTER_OPERATOR_AND = "and";
	private static final String FILTER_OPERATOR_OR = "or";
	private static final String FILTER_OPERATOR_NOT = "not";

	public static String[] prepFilter(String sFilter, String sMime,
			String sSeperator) {
		String[] result = null;
		sFilter = sFilter.trim();
		if (!isEmpty(sFilter)) {
			result = sFilter.split(sSeperator);
			int iResultLength = result.length;
			// remove white spaces
			for (int i = 0; i < iResultLength; i++) {
				result[i] = result[i].trim();
				if (!isEmpty(sMime)) {
					result[i] = result[i].replaceAll("<", "&#60;");
					result[i] = result[i].replaceAll(">", "&#62;");
				}
			}
		}
		return result;
	}

	protected static String getStats(Date dDate1, Date dDate2,
			double dScannedFilesSize, int iFileLinesPassedCount, int iNumFiles) {
		String sResult = "";

		// Time Diff
		double diff = diffDate(dDate1, dDate2);

		if (iNumFiles > 1)
			sResult += "Files Count: " + iNumFiles + " - ";

		if (iFileLinesPassedCount >= 0)
			sResult += "Passed Filter: " + iFileLinesPassedCount + " - ";

		if (dScannedFilesSize > 0)
			sResult += "Data: " + formatFileBytesSize(dScannedFilesSize)
					+ " - ";
		if (diff > 0) {
			sResult += "Time: " + String.format(sNUMBER_FORMAT, diff) + " Sec";
			if (dScannedFilesSize > 0) {
				double perf = dScannedFilesSize / diff;
				sResult += " - Data/Time: " + formatFileBytesSize(perf)
						+ "/Sec";
			}
		}

		if (!isEmpty(sResult))
			sResult = "! " + sResult;

		return sResult;
	}

	protected static double diffDate(Date dDate1, Date dDate2) {
		double lDate1 = 0L;
		double lDate2 = 0L;
		double diff = -1;
		if (dDate1 != null && dDate2 != null) {
			lDate1 = dDate1.getTime();
			lDate2 = dDate2.getTime();
			diff = (lDate2 - lDate1) / 1000;
		}
		return diff;
	}

	protected static String encodeHTML(String sFileLine, Props props) {
		String result = sFileLine;
		if (props.getFileFormat().getMime().indexOf("html") != -1) {
			result = result.replaceAll("<", "&#60;");
			result = result.replaceAll(">", "&#62;");
		}
		return result;
	}

	protected static String prepString(String s) {
		String result = s;
		if (s == null)
			result = "TODAY";
		return result;
	}

	protected static void archiveReportFile(Props props,
			StringBuffer sbMainReportContent) {
		try {
			File file = new File(props.getReportFilePath());
			File file2 = new File(props.getReportFilePath());
			int counter = 1;
			boolean fileExists = false;
			while (file2.exists()) {
				fileExists = true;
				String sUnique = "_" + counter + ".lsa";
				file2 = new File(props.getReportFilePath() + sUnique);
				counter++;
			}
			if (fileExists)
				file.renameTo(file2);
		} catch (Exception e) {
			handleException(new Date(), e, "archiving existing Report File",
					sbMainReportContent, props);

		}
	}

	protected static boolean writeToReportFile(Props props, StringBuffer sb) {
		boolean result = false;
		if (sb != null) {
			result = writeToReportFile(props.getReportFilePath(), sb.toString());
		}
		return result;
	}

	protected static boolean writeToReportFile(String sReportFilePath, String s) {
		boolean result = false;
		BufferedWriter bw = null;
		FileWriter fw = null;
		String sStatusMsg = "Writing to report file \"" + sReportFilePath
				+ "\"";
		try {
			fw = new FileWriter(sReportFilePath, true);// true:append
			bw = new BufferedWriter(fw);
			if (s != null)
				bw.write(s);
			bw.flush();
			fw.flush();
			result = true;
			Utils.log(new Date(), sStatusMsg + " completed!");
		} catch (Exception e) {
			log(new Date(), sStatusMsg, e);
		} finally {
			try {
				bw.close();
				fw.close();
			} catch (Exception e) {
				log(new Date(), sStatusMsg, e);
			}
			bw = null;
			fw = null;
		}
		return result;
	}

	protected static void handleException(Date dDate, Exception e,
			String sOperation, StringBuffer sb, Props props) {
		try {
			//e.printStackTrace();
			String sMsg = "Exception \"" + e + "\" occured while " + sOperation;
			log(dDate, sMsg);
			if (sb != null)
				sb.append(sMsg);
		} catch (Exception ex) {
			System.out
					.println("Unforseen Exception in handleException method: "
							+ ex);
		} catch (Error err) {
			System.out.println("Unforseen Error in handleException method: "
					+ err);
		}
	}

	protected static String getMethodName() {
		String methodName = null;
		String className = null;
		StackTraceElement[] stackTraceElements = Thread.currentThread()
				.getStackTrace();
		int size = stackTraceElements.length;
		int iCallingClassSTIndex = 3;
		if (iCallingClassSTIndex < size) {
			className = stackTraceElements[iCallingClassSTIndex].getClassName();
			methodName = stackTraceElements[iCallingClassSTIndex]
					.getMethodName();
		}
		if (className == null)
			className = "UNKNOWN";
		if (methodName == null)
			methodName = "UNKNOWN";
		methodName = className + "." + methodName;
		return methodName;
	}

	public static String readFile(String sFilePath) throws IOException {
		String result = null;
		BufferedReader br = null;
		StringBuilder sb = null;
		String sStatusMsg = "reading to report file \"" + sFilePath;
		try {
			br = new BufferedReader(new FileReader(sFilePath));
			sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			result = sb.toString();
		} catch (Exception e) {
			log(new Date(), sStatusMsg, e);
		} finally {
			br.close();
		}
		return result;
	}

	protected static String formatDate(Date dDate) {
		if (dDate == null)
			dDate = new Date();
		return formatDate(dDate, sSTD_DATE_FORMAT);
	}

	protected static String formatDate(Date aDate, String aDateFormat) {
		String lDate = null;
		try {
			lDate = aDate.toString();
			SimpleDateFormat lSDFormat = new SimpleDateFormat(aDateFormat);
			lDate = lSDFormat.format(aDate);
		} catch (Exception e) {
		}
		return lDate;
	}

	protected static Date convertToDate(String aDateStr, String aDateFormat) {
		if (!isEmpty(aDateStr)) {
			try {
				int aDateStrLength = aDateStr.length();
				int iDateFormatLength = aDateFormat.length();
				if (aDateStrLength != iDateFormatLength
						&& iDateFormatLength < 20)
					aDateFormat = aDateFormat.substring(0, aDateStrLength);
				SimpleDateFormat lSDFormat = new SimpleDateFormat(aDateFormat);
				ParsePosition lPp = new ParsePosition(0);
				Date lDate = lSDFormat.parse(aDateStr, lPp);
				return lDate;
			} catch (Exception e) {
				return null;
			}
		} else
			return null;
	}

	protected static boolean compareStartDate(String sDate1,
			String sDate1Format, String sDate2, String sDate2Format) {
		boolean result = true;
		if (!isEmpty(sDate2))
			result = compareDates(sDate1, sDate1Format, sDate2, sDate2Format);
		return result;
	}

	protected static boolean compareEndDate(String sDate1, String sDate1Format,
			String sDate2, String sDate2Format) {
		boolean result = true;
		if (!isEmpty(sDate1))
			result = compareDates(sDate1, sDate1Format, sDate2, sDate2Format);
		return result;
	}

	protected static boolean compareDates(String aDate1, String aDate1Format,
			String aDate2, String aDate2Format) {
		boolean result = false;
		Date myDate1 = convertToDate(aDate1, aDate1Format);
		if (myDate1 != null) {
			Date myDate2 = convertToDate(aDate2, aDate2Format);
			if (myDate2 != null) {
				if (myDate1.getTime() > myDate2.getTime())
					result = true;
			}
		}
		return result;
	}

	protected static boolean isEmpty(String aStr) {
		String lStr = aStr;
		if (lStr == null || lStr.trim().length() == 0) {
			return true;
		} else {
			return false;
		}
	}

	protected static String translateDate(String aDate, String aDateType) {
		if (!isEmpty(aDate) && aDate.indexOf("TODAY") != -1) {
			Calendar lToday = Calendar.getInstance();
			lToday.set(Calendar.HOUR, 0);
			lToday.set(Calendar.MINUTE, 0);
			lToday.set(Calendar.SECOND, 0);

			if ("end".equalsIgnoreCase(aDateType)) {
				lToday.set(Calendar.HOUR, 23);
				lToday.set(Calendar.MINUTE, 59);
				lToday.set(Calendar.SECOND, 59);
			}

			int lSep1Pos = aDate.indexOf("-");
			if (lSep1Pos == -1)
				lSep1Pos = aDate.indexOf("+");
			if (lSep1Pos != -1) {
				int days = Integer.parseInt(aDate.substring(lSep1Pos,
						aDate.length()).trim());
				lToday.add(Calendar.DATE, days);
			}
			return formatDate(lToday.getTime(), sSTD_DATE_FORMAT);
		}
		return aDate;
	}

	protected static ArrayList<String> getDirAndRegExpPathFiles(String sRegExp) {

		ArrayList<String> result = new ArrayList<String>();
		String sFilePath = sRegExp;
		String sFilePattern = sFilePath;

		int sep1 = sRegExp.indexOf(sREG_EXPRESSION);
		int sep2 = sRegExp.indexOf(sREG_EXPRESSION_POST);
		if (sep1 != -1 && sep2 != -1) {
			sFilePath = sFilePath.substring(0, sep1);
			sFilePattern = sFilePattern.substring(sep1, sep2);
		}

		// File Directories
		File file = new File(sFilePath);
		if (file.isDirectory()) {
			File[] lDirFiles = file.listFiles();
			for (int i = 0; i < lDirFiles.length; i++) {
				File fFile = lDirFiles[i];
				if (fFile.exists()) {
					if (fFile.isDirectory()) {
						String sDirPath = fFile.getAbsolutePath();
						ArrayList<String> result2 = getDirAndRegExpPathFiles(sDirPath);
						result.addAll(result2);
					} else {
						String fileName = fFile.getName();
						boolean patternTest = true;
						if (isRegExp(sFilePattern))
							patternTest = fileNameMatchesPattern(fileName,
									sFilePattern);
						if (patternTest)
							result.add(fFile.getAbsolutePath());
					}
				}
			}
		}
		return result;
	}

	private static boolean fileNameMatchesPattern(String fileName,
			String sFilePattern) {
		boolean result = false;
		Pattern p = Pattern.compile(sFilePattern);
		Matcher m = p.matcher(fileName);
		boolean b = m.matches();
		if (b)
			result = true;
		return result;
	}

	// Parses DATE from log file
	protected static String getDateFromLine(String aLine,
			String aLogFileDateFormat, int aLogFileDateStart) {
		// Method Output
		String lDateValue = null;

		// Line Info;
		String lFileLine = aLine;
		String lLogFileDateFormat = aLogFileDateFormat;
		int lLogFileDateStart = aLogFileDateStart;
		int lLogFileDateEnd = lLogFileDateStart + aLogFileDateFormat.length();

		// Correction for a=AM|PM & Z=PST|GMT|...
		if (lLogFileDateFormat.indexOf(" a ") != -1) {
			lLogFileDateEnd = lLogFileDateEnd + 1;
		}
		if (lLogFileDateFormat.indexOf(" z") != -1) {
			lLogFileDateEnd = lLogFileDateEnd + 2;
		}

		// Get Date
		try {
			String lTempDateValue = lFileLine.substring(lLogFileDateStart,
					lLogFileDateEnd);

			// Check date
			Date lTempDate = convertToDate(lTempDateValue, lLogFileDateFormat);

			// Passed Test
			if (lTempDate != null) {
				SimpleDateFormat df = new SimpleDateFormat(lLogFileDateFormat);
				lDateValue = df.format(lTempDate);
			}

		} catch (Exception e) {
		}
		return lDateValue;
	}

	protected static void log(Date dDate, String msg, Exception e) {
		String sStatusMsg = "Exception occured while " + msg + ": " + e;
		log(dDate, sStatusMsg, null, null);
	}

	protected static void log(String msg) {
		log(new Date(), msg, null, null);
	}

	protected static void log(Date dDate, String msg) {
		log(dDate, msg, null, null);
	}

	protected static void log(Date dDate, String msg, StringBuffer sb,
			Props props) {
		String sFormattedMsg = formatDate(dDate) + " - " + getMethodName()
				+ " - " + msg;
		if (sb != null)
			sb.append(sFormattedMsg + props.getFileFormat().getLineBreak());
		System.out.println(sFormattedMsg);
	}

	protected static String formatFileBytesSize(double bytes) {
		double fileSize = bytes;
		double TB = Math.pow(1024, 4);
		double GB = Math.pow(1024, 3);
		String unit = "B";
		if (fileSize > TB) {
			fileSize = (fileSize / TB);
			unit = "TB";
		} else if (fileSize > GB) {
			fileSize = (fileSize / GB);
			unit = "GB";
		} else if (fileSize > 1000000) {
			fileSize = (fileSize / 1000000);
			unit = "MB";
		} else if (fileSize > 1024) {
			fileSize = (fileSize / 1024);
			unit = "KB";
		}
		String result = String.format(sNUMBER_FORMAT, fileSize) + " " + unit;
		return result;
	}

	protected static Comparator<String[]> comparer = new Comparator<String[]>() {
		public int compare(String[] f1, String[] f2) {
			Double f1Size = Double.valueOf(f1[3]);
			Double f2Size = Double.valueOf(f2[3]);
			return f2Size.compareTo(f1Size);
		}
	};

	protected static boolean isRegExp(String sFilePath) {
		boolean result = false;
		int index1 = sFilePath.indexOf(sREG_EXPRESSION);
		if (index1 != -1)
			result = true;
		return result;
	}

	protected static String checkLineForWords(String line, String operator,
			Props props) {

		String[] sWords = null;
		if ("and".equalsIgnoreCase(operator))
			sWords = props.getFilterNotWordsArray();
		else if ("and".equalsIgnoreCase(operator))
			sWords = props.getFilterNotWordsArray();
		else if ("and".equalsIgnoreCase(operator))
			sWords = props.getFilterNotWordsArray();

		String result = null;
		String tempResult = line;
		boolean testResult = false;
		if (sWords != null && line != null) {
			int iWordslength = sWords.length;
			if (iWordslength > 0) {
				for (int i = 0; i < iWordslength; i++) {
					Pattern p = Pattern.compile(sREG_EXPRESSION_START
							+ sWords[i].toLowerCase() + sREG_EXPRESSION_END);
					Matcher m = p.matcher(line.toLowerCase());
					testResult = m.matches();

					// Not
					if (FILTER_OPERATOR_NOT.equalsIgnoreCase(operator)) {
						if (testResult) {
							testResult = false;
							tempResult = null;
							break;
						}
					}
					// And
					else if (FILTER_OPERATOR_AND.equalsIgnoreCase(operator)) {
						if (!testResult)
							break;
						else
							tempResult = tempResult.replaceAll("(?i)"
									+ sWords[i], props.getFileFormat()
									.getBold()
									+ sWords[i]
									+ props.getFileFormat().getBoldPost());
					}
					// Or
					else if (FILTER_OPERATOR_OR.equalsIgnoreCase(operator)) {
						if (testResult) {
							tempResult = line.replaceAll("(?i)" + sWords[i],
									props.getFileFormat().getBold()
											+ sWords[i]
											+ props.getFileFormat()
													.getBoldPost());
							break;
						}
					}
				}
			}
		} else
			result = line;

		if (testResult)
			result = tempResult;

		return result;
	}
}
