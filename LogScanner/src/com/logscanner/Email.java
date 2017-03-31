/*************************************************************************
 * Author: Younes Bouab
 * Email:  YBINSF@YAHOO.COM
 * Date:   01-01-2017
 *************************************************************************/

package com.logscanner;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;

public class Email {
	private static String sSMTP, sSMTPPort, sUser, sPassword;
	private static final String FILE_PATH_SEP = ",";
	private static final int iMaxFileBytesReadWrite = 1024000; // 1MB
	private static long iMaxEmailSizeInBytes = -1;
	private static long iMaxFileSizeInBytes = -1;
	private static String sBody_Trim_Msg = "Body content trimmed! Maximum Email size reached, ";
	private static byte[] baBodyTrimMsgBytes = null;
	private static String sFile_Trim_Msg = "File content was trimmed! Maximum Email size reached, ";
	private static byte[] baFileTrimMsgBytes = null;
	private String sPreTrimMsg = "\n\n***";
	private String sPostTrimMsg = "***";
	private Session session;

	// Prepare eMail
	public Email(String sSMTP, String sSMTPPort, String sUser, String sPassword) {
		this.sSMTP = sSMTP;
		this.sSMTPPort = sSMTPPort;
		this.sUser = sUser;
		this.sPassword = sPassword;

		// SMTP Properties
		Properties lSTMPProp = new Properties();
		lSTMPProp.put("mail.transport.protocol", "smtp");
		lSTMPProp.put("mail.smtp.host", sSMTP);
		if (sSMTPPort!=null && !"0".equals(sSMTPPort))
			lSTMPProp.put("mail.smtp.port", sSMTPPort);
		

		// Authentication
		Authenticator auth = null;
		if (sUser != null && sPassword != null)
		{
			lSTMPProp.put("mail.smtp.auth", "true");
			if (sSMTPPort!=null && !"0".equals(sSMTPPort))
			lSTMPProp.put("mail.smtp.socketFactory.port", sSMTPPort);
			lSTMPProp.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			auth = new SMTPAuthenticator();
		}

		// Session
		session = Session.getDefaultInstance(lSTMPProp, auth);
		// session.setDebug(true);
	}

	public void setAttachmentsProperties(long lMaxEmailSizeInBytes,
			long lMaxFileSizeInBytes) throws Exception {
		Email.iMaxEmailSizeInBytes = lMaxEmailSizeInBytes;
		Email.iMaxFileSizeInBytes = lMaxFileSizeInBytes;
		sBody_Trim_Msg += lMaxEmailSizeInBytes;
		baBodyTrimMsgBytes = sBody_Trim_Msg.getBytes("UTF8");
		sFile_Trim_Msg += lMaxEmailSizeInBytes;
		baFileTrimMsgBytes = sFile_Trim_Msg.getBytes("UTF8");
	}

	public void prepForMime(String sMime) throws Exception {
		if (!Utils.isEmpty(sMime) && sMime.toLowerCase().indexOf("html") != -1) {
			sPreTrimMsg = "<br><br><b>***";
			sPostTrimMsg = "***</b>";
		}
		sBody_Trim_Msg = sPreTrimMsg + sBody_Trim_Msg + iMaxEmailSizeInBytes
				+ sPostTrimMsg;
		baBodyTrimMsgBytes = sBody_Trim_Msg.getBytes("UTF8");
		sFile_Trim_Msg = sPreTrimMsg + sFile_Trim_Msg + iMaxEmailSizeInBytes
				+ sPostTrimMsg;
		baFileTrimMsgBytes = sFile_Trim_Msg.getBytes("UTF8");
	}

	public long getiMaxFilesSizeInBytes() {
		return iMaxFileSizeInBytes;
	}

	public int getiMaxFileBytesReadWrite() {
		return iMaxFileBytesReadWrite;
	}

	public InternetAddress[] setIAArray(String address) throws Exception {
		InternetAddress[] iAddressArray = null;
		if (address != null) {
			StringTokenizer lSTAddress = new StringTokenizer(address, ",");
			int numOfIA = lSTAddress.countTokens();
			iAddressArray = new InternetAddress[numOfIA];
			int i = 0;
			while (lSTAddress.hasMoreTokens()) {
				iAddressArray[i] = new InternetAddress(lSTAddress.nextToken());
				i++;
			}
		}
		return iAddressArray;
	}

	// Send eMail
	public void send(String sMime, String sTo, String sBcc, String sCc,
			String sFrom, String sSubject, String sBody, String sFiles,
			String sOutputDir) throws Exception {

		prepForMime(sMime);

		MimeMessage message = new MimeMessage(session);
		Multipart multipart = new MimeMultipart();

		// Set Email Recipients and Subject
		InternetAddress iaFromRecipients = new InternetAddress(sFrom);
		message.setFrom(iaFromRecipients);

		message.setRecipients(Message.RecipientType.TO, setIAArray(sTo));
		if (sBcc != null)
			message.setRecipients(Message.RecipientType.BCC, setIAArray(sBcc));

		if (sCc != null)
			message.setRecipients(Message.RecipientType.CC, setIAArray(sCc));

		message.setSubject(sSubject);

		// Body: Check Size!
		long iBodySize = 0;
		if (!Utils.isEmpty(sBody)) {
			byte[] baBody = sBody.getBytes("UTF8");
			iBodySize = baBody.length;
			if (iBodySize > iMaxEmailSizeInBytes) {
				int iTrimMsgLength = (int) iMaxEmailSizeInBytes;
				if (baBodyTrimMsgBytes != null)
					iTrimMsgLength -= baBodyTrimMsgBytes.length;
				byte[] bnewBody = new byte[iTrimMsgLength];
				System.arraycopy(baBody, 0, bnewBody, 0, iTrimMsgLength);
				sBody = new String(bnewBody) + sBody_Trim_Msg;
				iBodySize = sBody.getBytes("UTF8").length;
			}
		}

		// Attach Body
		MimeBodyPart mbpMessage = new MimeBodyPart();
		mbpMessage.setContent(sBody, sMime);
		multipart.addBodyPart(mbpMessage);
		sBody = null;

		if (iBodySize < iMaxEmailSizeInBytes) {
			// Files
			if (!Utils.isEmpty(sFiles))
				processAttachments(multipart, sMime, sFiles, iBodySize);
		}

		// Put Message
		message.setContent(multipart);

		// Send eMail!
		try {
			Transport.send(message);
		} catch (SendFailedException aSFE) {
			// Get the valid and invalid email addresses
			String sStatusMsg = "Checking if email sent was becuase of an invalid adress";
			Utils.log(sStatusMsg);
			Address[] aValid = aSFE.getValidUnsentAddresses();
			Address[] aInvalid = aSFE.getInvalidAddresses();
			String sInvalidList = "";
			String sValidList = "";
			if (aInvalid != null && aInvalid.length > 0) {

				for (int i = 0; i < aInvalid.length; i++)
					sInvalidList += aInvalid[i].toString() + ",";
				sInvalidList = sInvalidList.substring(0,
						sInvalidList.length() - 1);

				for (int i = 0; i < aValid.length; i++)
					sValidList += aValid[i].toString() + ",";
				sValidList = sValidList.substring(0, sInvalidList.length() - 1);

				sStatusMsg += "Could NOT send this email to the following addresses \""
						+ sInvalidList + "\"";
				Utils.log(sStatusMsg);

				sStatusMsg += "Attempting to send this email to the valid addresses only \""
						+ sValidList + "\"";
				Utils.log(sStatusMsg);

				send(sMime, sValidList, sCc, sBcc, sFrom, sSubject, sBody,
						sFiles, sOutputDir);
				sStatusMsg += "Email sent successfully!";
				Utils.log(sStatusMsg);
			}
			throw (aSFE);
		} finally {
			multipart = null;
			message = null;
		}
	}

	// Process Attachments
	public static String processAttachments(Multipart multipart, String sMime,
			String sFiles, long lBodySize) throws Exception {

		// Loop Through files
		String result = null;
		String[] saFiles = sFiles.split(FILE_PATH_SEP);

		// Get Total Size of email (body + attached files) and remove any excess
		// files!
		File file = null;
		long lTotalfilesSize = lBodySize;
		int saFilesLength = saFiles.length;
		int maxFilesIndex = 0;
		for (int i = 0; i < saFilesLength; i++) {
			String sFilePartPath = saFiles[i];
			file = new File(sFilePartPath);
			long fileSize = file.length();
			if (lTotalfilesSize + fileSize <= iMaxEmailSizeInBytes) {
				maxFilesIndex++;
				lTotalfilesSize += fileSize;
			} else {
				long lDiff = iMaxEmailSizeInBytes - lTotalfilesSize;
				if (lDiff > 0) {
					String validFilePartName = splitFile(sFilePartPath, lDiff,
							1);
					saFiles[i] = validFilePartName;
					lTotalfilesSize = iMaxEmailSizeInBytes;
					maxFilesIndex++;
				}
				break;
			}
		}

		// Split/Attach Files per max File Size/Email
		for (int i = 0; i < maxFilesIndex; i++) {
			String sFilePartPath = saFiles[i];
			file = new File(sFilePartPath);
			long lFileSize = file.length();

			String fileParts = null;
			if (iMaxFileSizeInBytes != -1 && lFileSize > iMaxFileSizeInBytes)
				fileParts = splitFile(sFilePartPath, iMaxFileSizeInBytes, -1);
			if (fileParts != null) {
				String[] saValidFiles = fileParts.split(FILE_PATH_SEP);
				int saValidFilesLength = saValidFiles.length;
				for (int j = 0; j < saValidFilesLength; j++)
					attachValidFile(saValidFiles[j], multipart, sMime);
			} else
				attachValidFile(sFilePartPath, multipart, sMime);
			file = null;
		}
		saFiles = null;
		return result;
	}

	public static void attachValidFile(String filePath, Multipart multipart,
			String sMime) throws MessagingException {
		MimeBodyPart fileMimeBodyPart = new MimeBodyPart();
		FileDataSource lFDS = new FileDataSource(filePath);
		DataHandler dh = new DataHandler(lFDS);
		fileMimeBodyPart.setDataHandler(dh);
		fileMimeBodyPart.setFileName(lFDS.getName());
		multipart.addBodyPart(fileMimeBodyPart);
		fileMimeBodyPart = null;
		lFDS = null;
		dh = null;
	}

	public static String splitFile(String sFileName, long iMaxFileSizeInBytes,
			long iMaxParts) throws IOException {
		String result = null;
		RandomAccessFile raf = new RandomAccessFile(sFileName, "r");
		long sourceFileSizeInBytes = raf.length();

		// Trim File Message
		if (sourceFileSizeInBytes > iMaxFileSizeInBytes) {
			// File Parts
			long numParts = iMaxParts;
			if (numParts == -1)
				numParts = sourceFileSizeInBytes / iMaxFileSizeInBytes;
			long remainingPartBytes = sourceFileSizeInBytes
					% iMaxFileSizeInBytes;
			BufferedOutputStream bos = null;
			String sPartFileName = null;

			// Read/Write Chunks of a File Part
			long numReadWriteParts = iMaxFileSizeInBytes
					/ iMaxFileBytesReadWrite;
			long remainingReadWriteBytes = iMaxFileSizeInBytes
					% iMaxFileBytesReadWrite;

			// Read/Write File Parts
			for (int i = 1; i <= numParts; i++) {
				// write chunks of the Part File at a time
				sPartFileName = sFileName + "_Part" + i;
				bos = new BufferedOutputStream(new FileOutputStream(
						sPartFileName));

				for (int j = 0; j < numReadWriteParts; j++)
					readWrite(raf, bos, iMaxFileBytesReadWrite, sFileName);

				if (remainingReadWriteBytes > 0)
					readWrite(raf, bos, remainingReadWriteBytes, sFileName);

				// Flush Part File if not last one
				bos.close();
				bos = null;

				// Return File Parts
				if (result == null)
					result = sPartFileName;
				else
					result += FILE_PATH_SEP + sPartFileName;
			}

			if (remainingPartBytes > 0
					&& (iMaxParts == -1 || (numParts + 1) <= iMaxParts)) {
				sPartFileName = sFileName + "_Part" + (numParts + 1);
				bos = new BufferedOutputStream(new FileOutputStream(
						sPartFileName));
				readWrite(raf, bos, remainingPartBytes, sFileName);
				readWrite(bos, baFileTrimMsgBytes);
				bos.close();
				bos = null;
				result += "," + sPartFileName;
			}

			// Cleanup
			raf.close();
			raf = null;
		}
		return result;
	}

	private static void readWrite(BufferedOutputStream bos,
			byte[] baTrimMsgBytes) throws IOException {
		bos.write(baTrimMsgBytes);
		bos.flush();
	}

	public static void readWrite(RandomAccessFile raf,
			BufferedOutputStream bos, long lBufSize, String sRafName)
			throws IOException {
		byte[] buf = new byte[(int) lBufSize];
		if (raf.read(buf) != -1) {
			bos.write(buf);
			bos.flush();
		} else
			throw (new IOException(
					"Was not able to read from RandomAccessFile " + sRafName));
	}

	private class SMTPAuthenticator extends javax.mail.Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			String username = sUser;
			String password = sPassword;
			return new PasswordAuthentication(username, password);
		}
	}
}
