/*
 *  Copyright (C) 2004 Cidero, Inc.
 *
 *  Permission is hereby granted to any person obtaining a copy of 
 *  this software to use, copy, modify, merge, publish, and distribute
 *  the software for any non-commercial purpose, subject to the
 *  following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 *  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY IN CONNECTION WITH THE SOFTWARE.
 * 
 *  File: $RCSfile: AppPreferences.java,v $
 *
 */
package domoNetWS.techManager.upnpManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ArrayList;
import com.cidero.util.*;

import java.awt.Color;

/**
 * Basic property-based implementation of application preferences class.
 * Occupies territory somewhere between the Java Properties class and the Java
 * Preferences class. The basic reason for the class is that the Java
 * Preferences class takes over a bit too much control for my liking with regard
 * to loading/storing of preferences information on the various platforms, and
 * uses XML instead of simple 'name=value' semantics (which I like).
 *
 * This class manages the loading of system-level and user-level preferences,
 * and loads/stores preferences using the simple properties file format. The
 * underlying property file can be hand-edited if desired. The class allows for
 * existing comments in a property file to be preserved when the file is
 * programatically updated.
 * 
 * To allow for a transition to using the standard Java Preferences class (may
 * make sense at some point), similar method names are used where possible. The
 * exception is that the API doesn't support getXXX() calls that specify a
 * default value. This is intentional since we don't want to have to deal with
 * the overhead of managing two sets of default values. We'd rather depend on
 * the preference files being available on disk (at least for now)
 */
public class AppPreferences {
	private static Logger logger = Logger.getLogger("com.cidero.util");

	Properties props = new Properties();

	String userPrefDirPath;
	String userHome;
	String fileSep;

	/**
	 * Constructor
	 *
	 * @param userPreferencesDir
	 *            Directory name for user's preferences. This is the directory
	 *            name beneath the user's home directory. Convention is to use
	 *            ".cidero" to match up with how most UNIX apps store their
	 *            setup info.
	 */
	public AppPreferences(String userPreferencesDir) {
		userHome = System.getProperty("user.home");

		/*
		 * This works, but other code (getResource()) returns paths with forward
		 * slashes it seems. Everything seems to work ok under Windows XP if we
		 * just use a '/' as a separator, so use that for all OS's for now
		 */
		fileSep = System.getProperty("file.separator");
		if (!fileSep.equals("/") && !fileSep.equals("\\"))
			fileSep = "/";
		System.out.println("fileSep = " + fileSep);

		// modified by Dario Russo
		// userPrefDirPath = userHome + fileSep + userPreferencesDir;
		userPrefDirPath = userPreferencesDir;
	}

	/**
	 * Load system and user preferences associated with the given class. It is
	 * assumed that all system-level preference files are stored in a
	 * 'properties' directory under a directory that is in the application's
	 * class path. The user-level preference files are stored in the user's home
	 * directory, under the 'Cidero' sub-directory. The Cidero sub-directory is
	 * created if needed.
	 */
	public boolean load(String appName, String className) {
		// Load the shared set first.
		/*
		 * Modified by Dario Russo String sharedPrefPath = "properties" +
		 * fileSep + className + ".properties";
		 */
		String sharedPrefPath = userPrefDirPath + fileSep + className + fileSep + className + ".properties";
		sharedPrefPath = MrUtil.getResourcePath(sharedPrefPath);

		logger.info("loading default preferences from " + sharedPrefPath);

		try {
			props.load(new FileInputStream(sharedPrefPath));
		} catch (IOException e1) {
			logger.warning("Couldn't load shared preferences file " + sharedPrefPath);
			logger.warning("Please create one and place in your classpath");
			return false;
		}

		// Then the user-level set
		String userPrefPath = userPrefDirPath + fileSep + appName + fileSep + className + fileSep + className
				+ ".properties";
		logger.info("loading user preferences from " + userPrefPath);

		try {
			FileInputStream inStream = new FileInputStream(userPrefPath);
			props.load(inStream);

			// Now merge latest shared preferences (factory settings) with user
			// settings. This keeps the user's preference file up to date with
			// the latest supported fields (handles new software releases in
			// semi-transparent way)
			saveUserPreferences(appName, className);
		} catch (Exception e) {
			logger.info("Didn't find user preferences in " + userPrefPath + " Exception: " + e);
			logger.info("Creating initial version...");
			saveUserPreferences(appName, className);
		}

		return true;
	}

	/**
	 * Save user preferences, preserving the comment structure of the original
	 * shared preference file so that the user can see the comments if
	 * hand-editing the file. To make this work right, it is necessary to parse
	 * the shared preference file line-by-line and copy over comments & blank
	 * lines as appropriate. A somewhat kludgy implementation, but useful end
	 * functionality.
	 *
	 * For best cross-platform compatibility with regard to hand-editing, the
	 * files are always written using the <CR><LF> windows convention for
	 * end-of-line markers. Most UNIX text editors deal with this ok, while
	 * Windows notepad doesn't work well if the UNIX convention of just <LF>'s
	 * is used
	 */
	public void saveUserPreferences(String appName, String className) {
		/*
		 * Modified by Dario Russo String sharedPrefPath = "properties" +
		 * fileSep + className + ".properties";
		 */
		String sharedPrefPath = userPrefDirPath + fileSep + className + fileSep + className + ".properties";
		sharedPrefPath = MrUtil.getResourcePath(sharedPrefPath);

		String userPrefPath = userPrefDirPath + fileSep + appName + fileSep + className + ".properties";

		// If userPreferencesDir doesn't exist, create it
		File dir = new File(userPrefDirPath + fileSep + appName);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				logger.warning("Error creating user preference directory: " + userPrefDirPath + fileSep + appName);
				return;
			}
		}

		try {
			// Open shared preference file
			FileInputStream inputStream = new FileInputStream(sharedPrefPath);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			// Open user preference file
			FileOutputStream outputStream = new FileOutputStream(userPrefPath);
			PrintWriter writer = new PrintWriter(outputStream);

			String line;
			while ((line = reader.readLine()) != null) {
				String trimmedLine = line.trim();

				// Echo blank & comment lines to output file
				if ((trimmedLine.length() == 0) || trimmedLine.startsWith("#")) {
					writer.print(line);
					writer.print("\r\n");
					continue;
				}

				// Split each property name=value line across 1st '='
				int index = line.indexOf("=");
				if (index < 0) {
					logger.warning("Syntax error for line: " + line);
					continue;
				}

				String name = line.substring(0, index);
				String value = props.getProperty(name);

				writer.print(name + "=" + value);
				writer.print("\r\n");

			} // while( line != null )

			writer.flush();

			logger.info("Wrote user preferences to " + userPrefPath);
		} catch (Exception e) {
			logger.warning("Error saving user preferences to " + userPrefPath + " Exception: " + e);
		}
	}

	public String get(String key) {
		if (props.getProperty(key) != null)
			return props.getProperty(key).trim();
		else
			return null;
	}

	public String get(String key, String defaultValue) {
		return props.getProperty(key, defaultValue).trim();
	}

	public boolean getBoolean(String key) {
		String value = props.getProperty(key);
		if (value == null) {
			logger.warning("property '" + key + "' not found - returning 'false'");
			return false;
		}

		if (value.trim().equals("true"))
			return true;

		return false;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		String value = props.getProperty(key);
		if (value == null)
			return defaultValue;

		if (value.trim().equals("true"))
			return true;

		return false;
	}

	public int getInt(String key) {
		String value = props.getProperty(key);
		if (value == null) {
			logger.warning("property '" + key + "' not found - returning 0");
			return 0;
		}
		return Integer.parseInt(value.trim());
	}

	public int getInt(String key, int defaultValue) {
		String value = props.getProperty(key);
		if (value == null) {
			logger.warning("property '" + key + "' not found - returning default of " + defaultValue);
			return defaultValue;
		}

		return Integer.parseInt(value.trim());
	}

	public float getFloat(String key) {
		String value = props.getProperty(key);
		if (value == null) {
			logger.warning("property '" + key + "' not found - returning 0.0");
			return (float) 0.0;
		}
		return Float.parseFloat(value.trim());
	}

	public float getFloat(String key, float defaultValue) {
		String value = props.getProperty(key);
		if (value == null)
			return defaultValue;

		return Float.parseFloat(value);
	}

	public Color getColor(String key) {
		String value = props.getProperty(key);
		if (value == null)
			return null;

		try {
			Color color = Color.decode(value.trim());
			return color;
		} catch (NumberFormatException e) {
			logger.warning("property '" + key + "' color format error - value = " + value);
			return null;
		}

	}

	public Color getColor(String key, Color defaultColor) {
		String value = props.getProperty(key);
		if (value == null)
			return defaultColor;

		try {
			Color color = Color.decode(value.trim());
			return color;
		} catch (NumberFormatException e) {
			logger.warning("property '" + key + "' color format error - value = " + value);
			return defaultColor;
		}
	}

	public void put(String key, String value) {
		props.setProperty(key, value.trim());
	}

	public void putBoolean(String key, boolean value) {
		if (value)
			props.setProperty(key, "true");
		else
			props.setProperty(key, "false");
	}

	public void putInt(String key, int value) {
		props.setProperty(key, Integer.toString(value));
	}

	public void putFloat(String key, float value) {
		props.setProperty(key, Float.toString(value));
	}

	public void putColor(String key, Color color) {
		String str = "#" + to8BitHex(color.getRed()) + to8BitHex(color.getGreen()) + to8BitHex(color.getBlue());

		props.setProperty(key, str);
	}

	/**
	 * Change listener routines. Right now, only event fired is global
	 * 'PreferencesChanged' event - may want to add capability of tracking
	 * change to single preference (TODO)
	 */
	ArrayList changeListenerList = new ArrayList();

	public void addChangeListener(AppPreferencesChangeListener listener) {
		changeListenerList.add(listener);
	}

	public void removeChangeListener(AppPreferencesChangeListener listener) {
		changeListenerList.remove(listener);
	}

	public void fireAppPreferencesChanged() {
		for (int n = 0; n < changeListenerList.size(); n++) {
			AppPreferencesChangeListener listener = (AppPreferencesChangeListener) changeListenerList.get(n);

			listener.appPreferencesChanged();
		}
	}

	/**
	 * 2-digit hex formatter (inserts leading 0's if needed
	 */
	public String to8BitHex(int value) {
		String hexStr = Integer.toHexString(value);
		int size = hexStr.length();
		if (size == 1)
			hexStr = "0" + hexStr;
		return hexStr;
	}

	/**
	 * Simple test program
	 */
	public static void main(String args[]) {
		AppPreferences pref = new AppPreferences("cideroPrefTest");

		pref.load("MediaController", "MediaController");

		pref.putBoolean("autoAlphaFoldersEnabled", false);
		pref.putColor("renderer.pictureViewForeground", Color.white);
		pref.putColor("renderer.pictureViewBackground", Color.red);

		pref.saveUserPreferences("MediaController", "MediaController");

	}

}
