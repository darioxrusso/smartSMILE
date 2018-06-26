/* This file is part of DOMONET.

 Copyright (C) 2006 Dario Russo

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

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;

import domoML.DomoMLElement;

/**
 * Extends a domoML.DomoMLElement for implement an org.w3c.dom.Element with tag
 * name &quot;linkedInput&quot; of the tree that rappresent a
 * domoML.domoMessage.DomoDevice.
 */
public class DomoDeviceServiceLinkedServiceInput extends DomoMLElement implements Element {

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;linkedInput&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 */
	public DomoDeviceServiceLinkedServiceInput(final DocumentImpl ownerDoc) {
		super(ownerDoc, "linkedInput");
	}

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;input&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 * @param from
	 *            The input name of the service of the domoDevice to be linked.
	 * @param to
	 *            The input name of this service to be linked.
	 */
	public DomoDeviceServiceLinkedServiceInput(final DocumentImpl ownerDoc, final String from, final String to,
			final String value) {
		this(ownerDoc);
		if (from != null)
			setFrom(from);
		setTo(to);
		if (value != null)
			setValue(value);
	}

	/**
	 * Set the name attribute &quot;from&quot; for org.w3c.dom.Element.
	 *
	 * @param from
	 *            The input name of the service of the domoDevice to be linked.
	 */
	public void setFrom(String from) {
		setAttribute("from", from);
	}

	/**
	 * Get the attribute &quot;from&quot; for org.w3c.dom.Element.
	 *
	 * @return The input name of the service of the domoDevice to be linked.
	 */
	public String getFrom() {
		return getAttribute("from");
	}

	/**
	 * Set the name attribute &quot;to&quot; for org.w3c.dom.Element.
	 *
	 * @param to
	 *            The input name of this service to be linked to from.
	 */
	public void setTo(String to) {
		setAttribute("to", to);
	}

	/**
	 * Get the attribute &quot;to&quot; for org.w3c.dom.Element.
	 *
	 * @return The input name of this service to be linked from.
	 */
	public String getTo() {
		return getAttribute("to");
	}

	/**
	 * Set the name attribute &quot;value&quot; for org.w3c.dom.Element.
	 *
	 * @param value
	 *            The input name of the service of the domoDevice to be linked.
	 */
	public void setValue(String value) {
		setAttribute("value", value);
	}

	/**
	 * Get the attribute &quot;value&quot; for org.w3c.dom.Element.
	 *
	 * @return The input name of the service of the domoDevice to be linked.
	 */
	public String getValue() {
		return getAttribute("value");
	}
}
