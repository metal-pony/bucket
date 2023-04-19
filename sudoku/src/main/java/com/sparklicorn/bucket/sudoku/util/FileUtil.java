package com.sparklicorn.bucket.sudoku.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.HttpURLConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Provides utility methods for working with files, HTTP connections.
 */
public class FileUtil {

	public static void transformLinesInFile(String fromPath, String toPath, Function<String, String> func, boolean skipEmptyLines) {
        try (
            Scanner scanner = new Scanner(new File(fromPath));
            PrintWriter pw = new PrintWriter(toPath);
        ) {
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.trim().isEmpty() && skipEmptyLines) {
					continue;
				}
				pw.println(func.apply(line));
			}
        } catch (IOException ex) {
			ex.printStackTrace();
		}
    }

	/**
	 * Returns a List containing the absolute paths of files found matching
	 * the given criteria in the given directory.
	 * <br/>File names are compared against filterRegex as lowercase.
	 * @param dir - Directory path to search in.
	 * @param filterRegex - Regular expression to match file names with.
	 * @param recurse - Whether to search directories recursively.
	 * @return A List of file paths, or <code>null</code> if the directory name
	 * provided does not exist or is not a real directory.
	 */
	public static List<String> findFiles(String dir, String filterRegex, boolean recurse) {
		ArrayList<String> result = null;
		File fDir = new File(dir);
		if (fDir.exists() && fDir.isDirectory()) {
			result = new ArrayList<>();
			for (File f : fDir.listFiles()) {
				if (f.isDirectory() && recurse) {
					result.addAll(findFiles(f.getAbsolutePath(), filterRegex, recurse));
				} else if (f.getName().toLowerCase().matches(filterRegex)) {
					result.add(f.getAbsolutePath());
				}
			}
		}
		return result;
	}

	/** Returns the content of a file.*/
	public static String getFileContent(String filepath) {
		StringBuilder s = new StringBuilder();
		try (Scanner scanner = new Scanner(new File(filepath))) {
			while (scanner.hasNextLine()) {
				s.append(scanner.nextLine());
				s.append(System.lineSeparator());
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return s.toString();
	}

	/** Returns the content of a file.*/
	public static String getFileContent(File file) {
		StringBuilder s = new StringBuilder();
		try (Scanner scanner = new Scanner(file)) {
			while (scanner.hasNextLine()) {
				s.append(scanner.nextLine());
				s.append(System.lineSeparator());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return s.toString().trim();
	}

	/**
	 * Attempts to read data from the given HttpURLConnection and save it with
	 * the specified filename.
	 * @param con - Connections that should provide some data.
	 * @param filename - Name of file to save the data to.
	 */
	public static void saveContentToFile(HttpURLConnection con, String filename) {
		if(con!=null) {
			try (
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			) {
				String input;
				while ((input = br.readLine()) != null) {
					pw.println(input);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			throw new NullPointerException("Provided HttspURLConnection was null.");
		}
	}

	 /** Attempts to read and return data from the given HttpURLConnection.*/
	public static String getContent(HttpURLConnection con) {
		StringBuilder strb = new StringBuilder();
		if (con!=null) {
			try (
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			) {
				String input;
				String line_sep = System.lineSeparator();
				while ((input = br.readLine()) != null) {
					strb.append(input);
					strb.append(line_sep);
				}
				br.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			throw new NullPointerException("Provided HttspURLConnection was null.");
		}
		return strb.toString();
	}
}
