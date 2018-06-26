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

package domoML.domoMessage;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;
import domoML.DomoMLElement;
import domoML.DomoMLDocument;

/**
 * Extends a domoML.DomoMLElement for implement an org.w3c.dom.Element of the
 * tree that represent a domoML.domoMessage.DomoMessage.
 */
public class DomoMessageInput extends DomoMLElement implements Element {

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;input&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 */
	public DomoMessageInput(final DocumentImpl ownerDoc) {
		super(ownerDoc, "input");
	}

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;input&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 * @param name
	 *            The name of the input to create.
	 * @param value
	 *            The value of the input to create.
	 * @param type
	 *            The DomoMLDocument.DataType of the input to create
	 */
	public DomoMessageInput(final DocumentImpl ownerDoc, final String name, final String value,
			final DomoMLDocument.DataType type) {
		this(ownerDoc);
		setName(name);
		setValue(value);
		setType(type);
	}

	/**
	 * Set the name attribute &quot;name&quot; for org.w3c.dom.Element.
	 *
	 * @param name
	 *            The name.
	 */
	public void setName(final String name) {
		setAttribute("name", name);
	}

	/**
	 * Get the attribute &quot;name&quot; for org.w3c.dom.Element.
	 *
	 * @return The name.
	 */
	public final String getName() {
		return getAttribute("name");
	}

	/**
	 * Set the name attribute &quot;value&quot; for org.w3c.dom.Element.
	 *
	 * @param value
	 *            The value.
	 */
	public void setValue(final String value) {
		setAttribute("value", value);
	}

	/**
	 * Get the attribute &quot;name&quot; for org.w3c.dom.Element.
	 *
	 * @return The name.
	 */
	public final String getValue() {
		return getAttribute("value");
	}

	/**
	 * Set the name attribute &quot;type&quot; for org.w3c.dom.Element.
	 *
	 * @param type
	 *            The type.
	 */
	public void setType(final DomoMLDocument.DataType type) {
		// set type of the parameter
		setAttribute("type", type.toString());
	}

	/**
	 * Get the attribute &quot;type&quot; for org.w3c.dom.Element.
	 *
	 * @return The type.
	 */
	public final DomoMLDocument.DataType getType() {
		return DomoMLDocument.DataType.valueOf(getAttribute("type"));
	}
}
