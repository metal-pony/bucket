package com.sparklicorn.bucket.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class Files {

	/**
	 * Reads all content from the InputStream to a String. Decodes the bytes
	 * to UTF-8 characters.
	 * @param in - the InputStream to read from.
	 * @return A String containing the content from the stream.
	 * @throws IOException If an I/O error occurs while reading.
	 */
	public static String readString(InputStream in) throws IOException {
		InputStreamReader reader = new InputStreamReader(in);
		StringBuffer strBuffer = new StringBuffer();
		char[] buffer = new char[1<<14];
		int numRead;
		while ((numRead = reader.read(buffer)) > -1) {
			strBuffer.append(buffer, 0, numRead);
		}
		return strBuffer.toString();
	}

	/**
	 * Attempts to read from the given HttpURLConnection and save it with the specified filename.
	 * The given connection will be severed after reading.
	 * @param con - Connection that should provide some data.
	 * @param filename - Filename to save to.
	 */
	public static void saveContentToFile(HttpURLConnection con, String filename, int bufferSize) {
		if(con == null) {
			throw new NullPointerException("HttpURLConnection cannot be null");
		}

		try (
			BufferedInputStream in = new BufferedInputStream(con.getInputStream());
			FileOutputStream fileOutputStream = new FileOutputStream(filename)
		) {
			byte dataBuffer[] = new byte[bufferSize];
			int bytesRead;
			while ((bytesRead = in.read(dataBuffer, 0, bufferSize)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			con.disconnect();
		}
	}

	/**
	  * Attempts to read data from the HttpURLConnection.
	  * The given connection will be severed after reading.
	  * @param con - Connection that should provide some data.
	  * @return String containing the data read.
	 */
	public static String getContent(HttpURLConnection con) {
		if(con == null) {
			throw new NullPointerException("HttpURLConnection cannot be null");
		}

		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			StringBuilder strb = new StringBuilder();
			String input;
			String line_sep = System.lineSeparator();

			while ((input = br.readLine()) != null) {
				strb.append(input);
				strb.append(line_sep);
			}
			br.close();

			return strb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			con.disconnect();
		}

		return null;
	}
}
