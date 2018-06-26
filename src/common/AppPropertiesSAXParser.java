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

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;

/**
 * Implements a SAX parser for reading an xml input stream from which take a
 * DomoMessage.
 */
public class AppPropertiesSAXParser extends DefaultHandler {

	/** Stores parsing values. */
	HashMap<String, String> properties = new HashMap<String, String>();

	/**
	 * Constructor that simply calls the super constructor of the
	 * DefaultHandler.
	 */
	public AppPropertiesSAXParser() {
		super();
	}

	/** Handler at the beginning of a tag. */
	public final void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		if (name.equalsIgnoreCase("property")) {
			// Create a new domomessage builded using the domoML
			properties.put(atts.getValue("name"), atts.getValue("value"));
		}
	}

	/**
	 * Return the properties that should contains the result of the parser.
	 * 
	 * @return The result of the parser.
	 */
	public final HashMap<String, String> getProperties() {
		return properties;
	}
}
