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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Represents a configuration file. It manages the configuration properties and
 * the file that contains them.
 */
public class AppProperties {

	/** Contains all properties of the file. */
	private HashMap<String, String> properties = new HashMap<String, String>();

	/** Stores the last version of the file that was loaded. */
	private long lastVersionTimeStamp;

	/** The object file containing the properties. */
	private File file;

	/**
	 * Takes the path of the file to parse in order to store properties.
	 * 
	 * @param filePath
	 *          the file path of the file to parse.
	 * @throws IOException
	 * @throws SAXException
	 */
	public AppProperties(final String filePath) throws IOException, SAXException {
		// the stream to open is in a file
		this.file = new File(filePath);
		update();
	}

	/**
	 * Parse the string containing the properties.
	 * 
	 * @param xmlProperties
	 *          The string containing the properties.
	 * @return An @code{HashMap} filled with the readed pair property-value.
	 * @throws SAXException
	 * @throws IOException
	 */
	private final HashMap<String, String> parseProperties(final String xmlProperties)
			throws SAXException, IOException {
		// parse the string
		XMLReader parser = XMLReaderFactory.createXMLReader();
		AppPropertiesSAXParser apsp = new AppPropertiesSAXParser();
		parser.setContentHandler(apsp);
		parser.setErrorHandler(apsp);
		parser.parse(new InputSource(new StringReader(xmlProperties)));
		// gets result
		return apsp.getProperties();
	}

	/**
	 * Read a file and return a string containing the file content.
	 * 
	 * @param filePath
	 *          The name of the file to open. Not sure if it can accept URLs or
	 *          just filenames. Path handling could be better, and buffer sizes
	 *          are hard-coded.
	 * @return The string.
	 */
	private final String readFileAsString(final String filePath)
			throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

	protected final boolean toBeUpdated() {
		if (this.file.lastModified() != this.lastVersionTimeStamp)
			return true;
		else
			return false;
	}

	protected final void update() throws SAXException, IOException {
		this.properties.clear();
		properties = parseProperties(readFileAsString(file.getAbsolutePath()));
		this.lastVersionTimeStamp = this.file.lastModified();
		System.out.println("Updated configuration file: " + file.getAbsolutePath() + " with "
				+ properties.size() + " items.");
	}

	/**
	 * Gets a property
	 * 
	 * @param name
	 *          the name of the property.
	 * @defaultValue the value to be returned if property name doesn't exists.
	 * @return the value.
	 * @throws IOException
	 * @throws SAXException
	 */
	public final String getProperty(final String name, final String defaultValue)
			throws SAXException, IOException {
		if (toBeUpdated()) 
			update(); 
		String value = this.properties.get(name);
		if (value == null)
			value = defaultValue; 
		return value;
	}

	/**
	 * Gets a property
	 * 
	 * @param name
	 *          the name of the property
	 * @return the value if the name exists, empty otherwise
	 * @throws IOException
	 * @throws SAXException
	 */
	public final String getProperty(final String name)
			throws SAXException, IOException {
		return getProperty(name, "");
	}

	/**
	 * Sets a property
	 * 
	 * @param the
	 *          name of the property
	 * @param the
	 *          value of the property
	 */
	public final void setProperty(final String name, final String value) {
		properties.put(name, value);
	}

	/**
	 * Gets all the key of properties
	 * 
	 * @return the set of keys
	 */
	public final Set<String> getPropertiesKeySet() {
		return this.properties.keySet();
	}

	/**
	 * Dumps the @code{HashMap} to the file.
	 * 
	 * @param filePath
	 *          the file to which dump
	 * @throws IOException
	 */
	public void dumpPropertiesToFile() throws IOException {
		// Create file
		FileWriter fstream = new FileWriter(this.file.getAbsolutePath());
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("<properties>");
		Iterator<String> propertyKeysIt = properties.keySet().iterator();
		while (propertyKeysIt.hasNext()) {
			String currentKey = ((String) propertyKeysIt.next());
			out.write(" <property name=\"" + currentKey + "\" value=\""
					+ properties.get(currentKey) + "\" />");
		}
		out.write("</properties>");
		// Close the output stream
		out.close();
	}

}
