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

package domoML.domoDevice;

import java.util.LinkedList;
import java.util.List;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import domoML.DomoMLDocument;
import domoML.DomoMLElement;

/**
 * Extends a domoML.DomoMLElement for implement an org.w3c.dom.Element with tag
 * name &quot;input&quot; of the tree that rappresent a
 * domoML.domoMessage.DomoDevice.
 */
public class DomoDeviceServiceInput extends DomoMLElement implements Element {

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;input&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 */
	public DomoDeviceServiceInput(final DocumentImpl ownerDoc) {
		super(ownerDoc, "input");
	}

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;input&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 * @param name
	 *            The name of the input.
	 * @param description
	 *            The description of the input.
	 * @param type
	 *            The domoML.DomoMLDocument.DataType of the input.
	 */
	public DomoDeviceServiceInput(final DocumentImpl ownerDoc, final String name, final String description,
			final DomoMLDocument.DataType type) {
		this(ownerDoc);
		setName(name);
		setDescription(description);
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
	 * Set the name attribute &quot;description&quot; for org.w3c.dom.Element.
	 *
	 * @param description
	 *            The description.
	 */
	public void setDescription(final String description) {
		setAttribute("description", description);
	}

	/**
	 * Get the attribute &quot;description&quot; for org.w3c.dom.Element.
	 *
	 * @return The description.
	 */
	public final String getDescription() {
		return getAttribute("description");
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
	 * @return The name.
	 */
	public final DomoMLDocument.DataType getType() {
		return DomoMLDocument.DataType.valueOf(getAttribute("type"));
	}

	/**
	 * Add an allowed value for the input provided.
	 * 
	 * @param value
	 *            The string allowed.
	 */
	public final DomoDeviceServiceInputAllowed addAllowed(String value) {
		DomoDeviceServiceInputAllowed serviceInputAllowed = new DomoDeviceServiceInputAllowed(
				(DocumentImpl) this.ownerDocument, value);
		appendChild(serviceInputAllowed);
		// return the input Element
		return serviceInputAllowed;
	}

	/**
	 * Get a list of domoML.domoDevice.DomoDeviceServiceInputs under this
	 * service.
	 * 
	 * @return The list of input Element.
	 */
	public final List<DomoDeviceServiceInputAllowed> getAllowed() {
		List<DomoDeviceServiceInputAllowed> allowed = new LinkedList<DomoDeviceServiceInputAllowed>();
		NodeList inputChildren = getChildNodes();
		for (int i = 0; i < inputChildren.getLength(); i++) {
			if (((DomoDeviceServiceInputAllowed) inputChildren.item(i)).getTagName().equalsIgnoreCase("allowed")) {
				allowed.add((DomoDeviceServiceInputAllowed) inputChildren.item(i));
			}
		}
		return allowed;
	}
}
