/* This file is part of DOMONET.

Copyright (C) 2006-2007 ISTI-CNR (Dario Russo)

DOMONET is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

DOMONET is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with DOMONET; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA */

package common;

import java.util.Calendar;

import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;

/**
 * Displays debug and logging messages. Debug messages can be formatted using
 * the return carrier and, at the beginning of each line, a timestamp is showed.
 * Messages are organized in levels. The level of debugging messages is defined
 * as preference in a file: 0: No messages 1: Default level 2: Debugging level
 * 3: High debugging level
 *
 * @author Dario Russo
 * @version 1.0
 */
public class Debug {

	/**
	 * Configuration file where the client takes parameters like TCP server port
	 * to listen messages from the DomoNet framework.
	 */
	private static String CONFIG_FILE = "src/common/debug.preferences";

	private static AppProperties prefs = null;

	private Debug() {
		try {
			Debug.prefs = AppPropertiesCollector.getInstance()
					.getAppProperties(CONFIG_FILE);
		} catch (SAXException | IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public static Debug getInstance() {
		return DebugHelper.INSTANCE;
	}

	private static class DebugHelper {
		private static Debug INSTANCE = new Debug();
	}

	/** Says if the next write operation is on a new line */
	private static boolean writeToNewLine = true;

	/**
	 * The string format of the date and time to be displayed with the message
	 */
	private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

	private static String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	/**
	 * Writes a message on the debug window.
	 * 
	 * @param message
	 *          The message to display.
	 * @param retCarr
	 *          true if new line is requested, false otherwise.
	 */
	private static final void write(final int level, final String message,
			final boolean retCarr) {
		int debugLevel = 100;
		try {
			if (Debug.prefs == null)
				Debug.prefs = AppPropertiesCollector.getInstance()
						.getAppProperties(CONFIG_FILE);
			debugLevel = Integer.parseInt(prefs.getProperty("debug-level", "3"));
		} catch (NumberFormatException | SAXException | IOException e) {
			System.out.println(e.getMessage());
		} finally {
			if (level <= debugLevel)
				if (isRunningOnWindows()) {
					try {
						FileOutputStream fout = new FileOutputStream("out.txt", true);
						if (writeToNewLine)
							// places date and time only at the beginning of a line
							new PrintStream(fout).print("[" + now() + "] ");
						new PrintStream(fout).print(message);
						if (retCarr) {
							new PrintStream(fout).println();
							writeToNewLine = true;
						} else
							writeToNewLine = false;
						fout.close();
					} catch (FileNotFoundException e) {
						System.out.println("File not found: " + e.getMessage());
					} catch (IOException e) {
						System.out
								.println("IOException during the clousure of the debug file: "
										+ e.getMessage());
					}
				} else {
					System.out.print("[" + now() + "] ");
					System.out.println(message);
					if (retCarr) {
						System.out.println("");
						writeToNewLine = true;
					} else
						writeToNewLine = false;
				}
		}
	}

	/**
	 * Writes a message on the debug window.
	 * 
	 * @param message
	 *          The message to display followed by new line.
	 */
	public final void write(final String message) {
		write(3, message, false);
	}

	/**
	 * Writes a message on the debug window.
	 * 
	 * @param message
	 *          The message to display followed by new line.
	 */
	public final void writeln(final String message) {
		write(3, message, true);
	}

	/**
	 * Writes a message on the debug window.
	 * 
	 * @param level
	 *          The level number for the message.
	 * @param message
	 *          The message to display followed by new line.
	 */
	public static final void writeln(int level, final String message) {
		write(level, message, true);
	}

	/** Verify if the system is running on Windows OS platform.
	 * 
	 * @return True if Windows OS is found. False otherwise.
	 */
	public static boolean isRunningOnWindows() {
		String os = null;

		if (os == null)
			os = System.getProperty("os.name");
		if (os.toLowerCase().indexOf("window") >= 0)
			return true;
		else
			return false;
	}
}