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

package domoML;

import org.apache.xerces.dom.*;
import org.w3c.dom.*;

/**
 * This must be extended by all DomoML classes as domoML.DomoDevice.domoDevice
 * and domoML.DomoMessage.domoMessage.
 */
public abstract class DomoMLDocument extends DocumentImpl implements Document {

	/** Enumeration of types for services supported. */
	public static enum DataType {
		INT, LONG, FLOAT, DOUBLE, CHAR, STRING, DATE, BYTE, BOOLEAN, TWOBIT, THREEBIT, FOURBIT, EIGHTBIT, ONEBYTE, TWOBYTE, MEDIALIST, STREAM
	};

	/**
	 * Give a domoML representation.
	 */
	public final String toString() {
		return toString(false);
	}

	/**
	 * Give a domoML representation.
	 *
	 * @param documentMarkup
	 *            If true get the markup in the document preceding the root
	 *            element too.
	 */
	public final String toString(boolean documentMarkup) {
		// get the method declared in
		// domoML.DomoMLElement.getDomoML(org.w3c.dom.Node, int, boolean)
		if (documentMarkup)
			return DomoMLElement.getDomoML(this, 0, false);
		else
			return DomoMLElement.getDomoML(this.getDocumentElement(), 0, false);
	}

	/**
	 * Create a new domoML.DomoMLElement inside the domoML.DomoDevice tree.
	 *
	 * @param tagName
	 *            The name of the element type to instantiate. For XML, this is
	 *            case-sensitive. For HTML, the tagName parameter may be
	 *            provided in any case, but it must be mapped to the canonical
	 *            uppercase form by the DOM implementation.
	 * @return The domoML.DomoMLElement with org.w3c.dom.Element interface.
	 * @throws DOMException(INVALID_NAME_ERR)
	 *             if the tag name is not acceptable.
	 */
	public Element createElement(String tagName) throws DOMException {
		boolean xml11Version = false; // by default 1.0
		if (errorChecking && !isXMLName(tagName, xml11Version)) {
			String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR",
					null);
			throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
		}
		return new DomoMLElement(this, tagName);
	} // createElement(String):Element
}
