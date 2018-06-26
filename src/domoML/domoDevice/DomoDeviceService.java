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

import java.util.*;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import domoML.DomoMLElement;
import domoML.DomoMLDocument;
import domoML.domoDevice.NoAttributeFoundException;

/**
 * Extends a domoML.DomoMLElement for implement an org.w3c.dom.Element with tag
 * name &quot;service&quot; of the tree that rappresent a
 * domoML.domoMessage.DomoDevice.
 */
public class DomoDeviceService extends DomoMLElement implements Element {

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;service&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 */
	public DomoDeviceService(final DocumentImpl ownerDoc) {
		super(ownerDoc, "service");
	}

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;service&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 * @param name
	 *            The name of the service.
	 * @param description
	 *            The description of the service.
	 * @param prettyName
	 *            The pretty name for the service
	 */
	public DomoDeviceService(final DocumentImpl ownerDoc, final String name, final String description,
			final String prettyName) {
		this(ownerDoc);
		setName(name);
		setDescription(description);
		setPrettyName(prettyName);
	}

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;service&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 * @param name
	 *            The name of the service.
	 * @param description
	 *            The description of the service.
	 * @param prettyName
	 *            The pretty name for the service
	 * @param output
	 *            The domoML.DomoMLDocument.DataType of the output of the
	 *            service.
	 * @param outputDescription
	 *            A description of the output of the service.
	 * @param outputDescription
	 *            The name of the output.
	 */
	public DomoDeviceService(final DocumentImpl ownerDoc, final String name, final String description,
			final String prettyName, final DomoMLDocument.DataType output, final String outputDescription,
			final String outputName) {
		this(ownerDoc);
		setName(name);
		setDescription(description);
		setPrettyName(prettyName);
		setOutput(output);
		setOutputName(outputName);
		setOutputDescription(outputDescription);
	}

	/**
	 * Build a new org.w3c.dom.Element with tag name &quot;service&quot;
	 *
	 * @param ownerDoc
	 *            The parent of the Element.
	 * @param name
	 *            The name of the service.
	 * @param description
	 *            The description of the service.
	 * @param prettyName
	 *            The pretty name for the service
	 * @param output
	 *            The domoML.DomoMLDocument.DataType of the output of the
	 *            service.
	 * @param outputDescription
	 *            A description of the output of the service.
	 */
	public DomoDeviceService(final DocumentImpl ownerDoc, final String name, final String description,
			final String prettyName, final DomoMLDocument.DataType output, final String outputDescription) {
		this(ownerDoc);
		setName(name);
		setDescription(description);
		setPrettyName(prettyName);
		setOutput(output);
		setOutputDescription(outputDescription);
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
	 * Set the name attribute &quot;prettyName&quot; for org.w3c.dom.Element.
	 *
	 * @param prettyName
	 *            The pretty name.
	 */
	public void setPrettyName(final String prettyName) {
		setAttribute("prettyName", prettyName);
	}

	/**
	 * Get the attribute &quot;prettyName&quot; for org.w3c.dom.Element.
	 *
	 * @return The prettyName.
	 */
	public final String getPrettyName() {
		return getAttribute("prettyName");
	}

	/**
	 * Set the name attribute &quot;output&quot; for org.w3c.dom.Element.
	 *
	 * @param output
	 *            The output type for the service.
	 */
	public void setOutput(final DomoMLDocument.DataType output) {
		// set type of the parameter
		setAttribute("output", output.toString());
	}

	/**
	 * Get the attribute &quot;output&quot; for org.w3c.dom.Element.
	 *
	 * @return The output type.
	 * @throws domoML.domoDevice.NoElementFoundException
	 *             if no &quot;output&quot; attribute is found.
	 */
	public final DomoMLDocument.DataType getOutput() throws NoAttributeFoundException {
		if (!getAttribute("output").equalsIgnoreCase(""))
			// a value for output is set
			return DomoMLDocument.DataType.valueOf(getAttribute("output"));
		else
			throw new NoAttributeFoundException();
	}

	/**
	 * Set the name attribute &quot;prettyoutputDescription&quot; for
	 * org.w3c.dom.Element.
	 *
	 * @param outputDescription
	 *            The description of the output for the service.
	 */
	public void setOutputDescription(final String outputDescription) {
		setAttribute("outputDescription", outputDescription);
	}

	/**
	 * Get the attribute &quot;outputDescription&quot; for org.w3c.dom.Element.
	 *
	 * @return The output description;
	 * @throws domoML.domoDevice.NoElementFoundException
	 *             if no &quot;outputDescription&quot; attribute is found.
	 */
	public final String getOutputDescription() throws NoAttributeFoundException {
		if (!getAttribute("outputDescription").equalsIgnoreCase(""))
			// a value for outputDescription in set
			return getAttribute("outputDescription");
		else
			throw new NoAttributeFoundException();
	}

	/**
	 * Set the name attribute &quot;outputName&quot; for org.w3c.dom.Element.
	 *
	 * @param outputName
	 *            The name of the output for the service.
	 */
	public void setOutputName(final String outputName) {
		setAttribute("outputName", outputName);
	}

	/**
	 * Get the attribute &quot;outputName&quot; for org.w3c.dom.Element.
	 *
	 * @return The output description;
	 * @throws domoML.domoDevice.NoElementFoundException
	 *             if no &quot;outputName&quot; attribute is found.
	 */
	public final String getOutputName() throws NoAttributeFoundException {
		if (!getAttribute("outputName").equalsIgnoreCase(""))
			// a value for outputName in set
			return getAttribute("outputName");
		else
			throw new NoAttributeFoundException();
	}

	/**
	 * Add an input Element to the service.
	 *
	 * @param name
	 *            The name of the input.
	 * @param description
	 *            The description for the input.
	 * @param type
	 *            The DomoDevice.DataType of the input.
	 * @return The Element that represent the input.
	 */
	public final DomoDeviceServiceInput addInput(final String name, final String description,
			final DomoMLDocument.DataType type) {
		DomoDeviceServiceInput serviceInput = new DomoDeviceServiceInput((DocumentImpl) ownerDocument, name,
				description, type);
		appendChild(serviceInput);
		// return the input Element
		return serviceInput;
	}

	/**
	 * Add a linkedService Element to the service.
	 *
	 * @param url
	 *            The url of the web service that contains the domoDevice that
	 *            contains the service to be linked.
	 * @param id
	 *            The id of the domoDevice that contains the service to be
	 *            linked.
	 * @param service
	 *            The name of the service to be linked.
	 * @return The Element that rappresent the linked service.
	 */
	public final DomoDeviceServiceLinkedService addLinkedService(final String url, final String id,
			final String service) {
		DomoDeviceServiceLinkedService linkedService = new DomoDeviceServiceLinkedService((DocumentImpl) ownerDocument,
				url, id, service);
		appendChild(linkedService);
		// return the linkedService Element
		return linkedService;
	}

	/**
	 * Get a list of domoML.domoDevice.DomoDeviceServiceInputs under this
	 * service.
	 *
	 * @return The list of input Element.
	 */
	public final List<DomoDeviceServiceInput> getInputs() {
		List<DomoDeviceServiceInput> domoDeviceServiceInputs = new LinkedList<DomoDeviceServiceInput>();
		NodeList serviceChildren = getChildNodes();
		for (int i = 0; i < serviceChildren.getLength(); i++) {
			if (((DomoMLElement) serviceChildren.item(i)).getTagName().equalsIgnoreCase("input")) {
				domoDeviceServiceInputs.add((DomoDeviceServiceInput) serviceChildren.item(i));
			}
		}
		return domoDeviceServiceInputs;
	}

	/**
	 * Get a list of domoML.domoDevice.DomoDeviceServiceLinkedService under this
	 * service.
	 *
	 * @return The list of input Element.
	 */
	public final List<DomoDeviceServiceLinkedService> getLinkedServices() {
		List<DomoDeviceServiceLinkedService> domoDeviceServiceLinkedServices = new LinkedList<DomoDeviceServiceLinkedService>();
		NodeList serviceChildren = getChildNodes();
		for (int i = 0; i < serviceChildren.getLength(); i++) {
			if (((DomoMLElement) serviceChildren.item(i)).getTagName().equalsIgnoreCase("linkedService")) {
				domoDeviceServiceLinkedServices.add((DomoDeviceServiceLinkedService) serviceChildren.item(i));
			}
		}
		return domoDeviceServiceLinkedServices;
	}
}
