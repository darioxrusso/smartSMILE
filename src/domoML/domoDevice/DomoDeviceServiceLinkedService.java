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

import domoML.DomoMLElement;

/**
 * Extends a domoML.DomoMLElement for implement an org.w3c.dom.Element with tag
 * name &quot;linkedService&quot; of the tree that rappresent a
 * domoML.domoMessage.DomoDevice.
 */
public class DomoDeviceServiceLinkedService extends DomoMLElement implements Element {

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;linkedService&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 */
	public DomoDeviceServiceLinkedService(final DocumentImpl ownerDoc) {
		super(ownerDoc, "linkedService");
	}

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;input&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 * @param url
	 *            The url of the web service that contains the domoDevice that
	 *            contains the service to be linked.
	 * @param id
	 *            The id of the domoDevice that contains the service to be
	 *            linked.
	 * @param service
	 *            The name of the service of the domoDevice to be linked.
	 */
	public DomoDeviceServiceLinkedService(final DocumentImpl ownerDoc, final String url, final String id,
			final String service) {
		this(ownerDoc);
		setUrl(url);
		setId(id);
		setService(service);
	}

	/**
	 * Set the name attribute &quot;id&quot; for org.w3c.dom.Element.
	 *
	 * @param id
	 *            The id of the device that contains the service to be linked.
	 */
	public void setId(final String id) {
		setAttribute("id", id);
	}

	/**
	 * Get the attribute &quot;id&quot; for org.w3c.dom.Element.
	 *
	 * @return The id of the device that contains the service to be linked.
	 */
	public String getId() {
		return getAttribute("id");
	}

	/**
	 * Set the name attribute &quot;url&quot; for org.w3c.dom.Element.
	 *
	 * @param url
	 *            The url of the device that contains the service to be linked.
	 */
	public void setUrl(String url) {
		setAttribute("url", url);
	}

	/**
	 * Get the attribute &quot;url&quot; for org.w3c.dom.Element.
	 *
	 * @return The url of the device that contains the service to be linked.
	 */
	public String getUrl() {
		return getAttribute("url");
	}

	/**
	 * Set the name attribute &quot;service&quot; for org.w3c.dom.Element.
	 *
	 * @param service
	 *            The service of the device that contains the service to be
	 *            linked.
	 */
	public void setService(String service) {
		setAttribute("service", service);
	}

	/**
	 * Get the attribute &quot;service&quot; for org.w3c.dom.Element.
	 *
	 * @return The service of the device that contains the service to be linked.
	 */
	public String getService() {
		return getAttribute("service");
	}

	/**
	 * Set the name attribute &quot;ifInput&quot; for org.w3c.dom.Element.
	 *
	 * @param ifInput
	 *            The ifInput of the device that contains the service to be
	 *            linked.
	 */
	public void setIfInput(String ifInput) {
		setAttribute("ifInput", ifInput);
	}

	/**
	 * Get the attribute &quot;ifInput&quot; for org.w3c.dom.Element.
	 *
	 * @return The ifInput of the device that contains the service to be linked.
	 */
	public String getIfInput() {
		return getAttribute("ifInput");
	}

	/**
	 * Set the name attribute &quot;hasValue&quot; for org.w3c.dom.Element.
	 *
	 * @param hasValue
	 *            The hasValue of the device that contains the service to be
	 *            linked.
	 */
	public void setHasValue(String hasValue) {
		setAttribute("hasValue", hasValue);
	}

	/**
	 * Get the attribute &quot;hasValue&quot; for org.w3c.dom.Element.
	 *
	 * @return The hasValue of the device that contains the service to be
	 *         linked.
	 */
	public String getHasValue() {
		return getAttribute("hasValue");
	}

	/**
	 * Add a couple of input names to be made in relation.
	 * 
	 * @param from
	 *            The input name from which take the value.
	 * @param to
	 *            The input name to which replace value.
	 * @return The input Element with relation.
	 */
	public DomoDeviceServiceLinkedServiceInput addLinkedServiceInput(String from, String to, String value) {
		DomoDeviceServiceLinkedServiceInput serviceLinkedServiceInput = new DomoDeviceServiceLinkedServiceInput(
				(DocumentImpl) this.ownerDocument, from, to, value);
		appendChild(serviceLinkedServiceInput);
		// return the input Element
		return serviceLinkedServiceInput;
	}

	/**
	 * Get a list of domoML.domoDevice.DomoDeviceServiceLinkedServiceInputs
	 * under this linkedService.
	 *
	 * @return The list of input Element.
	 */
	public final List<DomoDeviceServiceLinkedServiceInput> getInputs() {
		List<DomoDeviceServiceLinkedServiceInput> domoDeviceServiceLinkedServiceInputs = new LinkedList<DomoDeviceServiceLinkedServiceInput>();
		NodeList serviceChildren = getChildNodes();
		for (int i = 0; i < serviceChildren.getLength(); i++) {
			if (((DomoMLElement) serviceChildren.item(i)).getTagName().equalsIgnoreCase("linkedInput")) {
				domoDeviceServiceLinkedServiceInputs.add((DomoDeviceServiceLinkedServiceInput) serviceChildren.item(i));
			}
		}
		return domoDeviceServiceLinkedServiceInputs;
	}
}
