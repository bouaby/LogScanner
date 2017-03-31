/*************************************************************************
 * Author: Younes Bouab
 * Email:  YBINSF@YAHOO.COM
 * Date:   01-01-2017
 *************************************************************************/

package com.logscanner;

public class FileInfo {

	private String translatedFromRegExp = null;
	private String path = null;
	private String dateFormat = null;
	private int dateStartPos = -1;
	private double size = -1;

	public String getTranslatedFromRegExp() {
		return translatedFromRegExp;
	}

	public void setTranslatedFromRegExp(String translatedFromRegExp) {
		this.translatedFromRegExp = translatedFromRegExp;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public int getDateStartPos() {
		return dateStartPos;
	}

	public void setDateStartPos(int dateStartPos) {
		this.dateStartPos = dateStartPos;
	}

	public double getSize() {
		return size;
	}

	public void setSize(double size) {
		this.size = size;
	}
}
