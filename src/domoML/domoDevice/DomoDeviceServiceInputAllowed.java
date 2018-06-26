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

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;

import domoML.DomoMLElement;

/**
 * Extends a domoML.DomoMLElement for implement an org.w3c.dom.Element with tag
 * name &quot;allowed&quot; of the tree that rappresent a
 * domoML.domoMessage.DomoDevice.
 */
public class DomoDeviceServiceInputAllowed extends DomoMLElement implements Element {

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;allowed&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 */
	public DomoDeviceServiceInputAllowed(final DocumentImpl ownerDoc) {
		super(ownerDoc, "allowed");
	}

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;input&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 * @param value
	 *            The value allowed for the input.
	 */
	public DomoDeviceServiceInputAllowed(final DocumentImpl ownerDoc, final String value) {
		this(ownerDoc);
		setValue(value);
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
	 * Get the attribute &quot;value&quot; for org.w3c.dom.Element.
	 *
	 * @return The value.
	 */
	public final String getValue() {
		return getAttribute("value");
	}
}
