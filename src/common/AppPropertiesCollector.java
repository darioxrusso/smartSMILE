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

import java.util.*;
import java.io.*;

import org.xml.sax.*;

/**
 * This class offers a unique point of access to load and store preferences for
 * the whole application. Being a modular application, configuration files can
 * be stored in different files in different locations. To avoid to load and
 * create multiple instances for the same configuration file, this class
 * collects created instances (one for each configuration file) and return them
 * when requested. This class permits to change settings at runtime. Before to
 * return a value, it is verified if the file containing the settings was
 * changed in order to update loaded fields.
 */
public class AppPropertiesCollector {

	/** Contains the instances for all configuration files. */
	private static HashMap<String, AppProperties> appProperties = new HashMap<String, AppProperties>();

	/** Empty and private constructor. */
	private AppPropertiesCollector() {
	}

	/**
	 * Gets the instance for the proper configuration file.
	 * 
	 * @param filePath
	 *          The configuration file.
	 * @return The instance that manages the configuration file.
	 * @throws IOException
	 * @throws SAXException
	 */
	public AppProperties getAppProperties(final String filePath)
			throws SAXException, IOException {
		AppProperties tmpAppProperties = null;
		if (!(AppPropertiesCollector.appProperties.containsKey(filePath))) {
			System.out
			.println("Creating new AppProperties class for file: " + filePath + ". App properties lenght: " + appProperties.size());
			AppPropertiesCollector.appProperties.put(filePath,
					new AppProperties(filePath));
		}
		tmpAppProperties = appProperties.get(filePath);
		if (tmpAppProperties.toBeUpdated()) {
			tmpAppProperties.update();
		}
		return tmpAppProperties;
	}

	public static AppPropertiesCollector getInstance() {
		return AppPropertiesCollectorHelper.INSTANCE;
	}

	/** Implements Bill Pugh singleton design pattern. */
	private static class AppPropertiesCollectorHelper {
		private static AppPropertiesCollector INSTANCE = new AppPropertiesCollector();
	}
}
