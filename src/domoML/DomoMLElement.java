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

/** This envelop all Elements of a DomoMLDocument. */
public class DomoMLElement extends ElementImpl {

	/** Factory constructor. */
	public DomoMLElement(DocumentImpl ownerDoc, String name) {
		super(ownerDoc, name);
	}

	// for ElementNSImpl
	protected DomoMLElement() {
		super();
	}

	/**
	 * Give a domoML rappresentation.
	 */
	public final String toString() {
		return getDomoML(this, 0, false);
	}

	/**
	 * Give a domoML rappresentation.
	 *
	 * @param element
	 *            The element to which start the rappresentation.
	 * @param currentLevel
	 *            The level of the element to which start. This is useful only
	 *            for format porpouse.
	 * @param formatted
	 *            If the rappresentation should be formatted.
	 * @return The string that rappresent the device.
	 */
	public final static String getDomoML(final Node element, final int currentLevel, final boolean formatted) {
		StringBuffer XMLString = new StringBuffer();
		NodeList children = element.getChildNodes();
		// Start from this element
		int i = 0;
		if (formatted)
			for (i = 0; i < currentLevel; i++)
				XMLString = XMLString.append(" ");
		XMLString = XMLString.append("<" + element.getNodeName());
		// print attibutes inside the element start tag
		NamedNodeMap attributes = element.getAttributes();
		if (attributes != null) {
			int k = 0;
			for (k = 0; k < attributes.getLength(); k++) {
				XMLString = XMLString.append(" " + attributes.item(k).getNodeName());
				XMLString = XMLString.append("=\"" + attributes.item(k).getNodeValue() + "\"");
			}
		}
		// check for element value or sub-elements
		if (element.hasChildNodes()) {
			XMLString = XMLString.append(">");
			if (formatted)
				XMLString = XMLString.append("\n");
			// print all child elements in the DOM tree
			int k = 0;
			for (k = 0; k < children.getLength(); k++) {
				Node child = children.item(k);
				if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
					XMLString = XMLString.append(getDomoML(children.item(k), currentLevel + 1, formatted));
				} else if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
					XMLString = XMLString.append(child.getNodeValue());
				}
			} // for loop ends here
				// print end tag
			if (formatted)
				for (i = 0; i < currentLevel; i++)
					XMLString = XMLString.append(" ");
			XMLString = XMLString.append("</" + element.getNodeName() + ">");
			if (formatted)
				XMLString = XMLString.append("\n");
		} else {
			// element seems to be empty
			XMLString = XMLString.append(" />");
			if (formatted)
				XMLString = XMLString.append("\n");
		} // else ends here
		return XMLString.toString();
	}// getDomoML ends here
}
