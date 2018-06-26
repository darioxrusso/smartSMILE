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

import java.io.StringReader;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.w3c.dom.*;

import domoML.*;

/**
 * A generic device used in the web service. Every information is stored in a
 * DOM tree.
 */
public class DomoDevice extends DomoMLDocument implements Document {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /** Enumeration of tech type supported. */
    public static enum DomoTech {
	KNX, UPNP, BTICINO, X10, DOMOML
    };

    /** Create a new Document with tag &quot;device&quot;. */
    public DomoDevice() {
	appendChild(createElement("device"));
    }

    /**
     * Create a new Document from a DomoML description.
     *
     * @param domoDeviceString
     *            The description of the device as DomoML string.
     */
    public DomoDevice(String domoDeviceString) {
	this();
	// check if empty domoMLMessage
	if (!domoDeviceString.trim().equalsIgnoreCase(""))
	    try {
		XMLReader parser = XMLReaderFactory.createXMLReader();
		DomoDeviceSAXParser dmsp = new DomoDeviceSAXParser(
			(DomoDevice) this);
		parser.setContentHandler(dmsp);
		parser.setErrorHandler(dmsp);
		parser.parse(
			new InputSource(new StringReader(domoDeviceString)));
		// assign returned value to a temporary variable in order to
		// store values into the domoDeviceList.
	    } catch (Exception e) {
		e.printStackTrace();
	    }
    }

    /**
     * Create a new device with some parameters.
     *
     * @param tech
     *            The tech type used. Can be "KNX", "UPNP".
     * @param type
     *            The type of the device.
     * @param description
     *            The description of the device.
     * @param position
     *            The position of the device.
     * @param positionDescription
     *            The description of the position of the device.
     * @param manufacturer
     *            The manufacturer of the device.
     * @param serialNumber
     *            The serial number of the device.
     */
    public DomoDevice(final DomoTech tech, final String type,
	    final String description, final String position,
	    final String positionDescription, final String manufacturer,
	    final String serialNumber) {
	// create the root of the DOM
	this();
	// adding attributes
	setTech(tech);
	setType(type);
	setDescription(description);
	setPosition(position);
	setPositionDescription(positionDescription);
	setManufacturer(manufacturer);
	setSerialNumber(serialNumber);
    }

    /**
     * Create a new device with some parameters.
     *
     * @param tech
     *            The tech type used. Can be "KNX", "UPNP".
     * @param type
     *            The type of the device.
     * @param description
     *            The description of the device.
     * @param position
     *            The position of the device.
     * @param positionDescription
     *            The description of the position of the device.
     * @param manufacturer
     *            The manufacturer of the device.
     * @param serialNumber
     *            The serial number of the device.
     * @param url
     *            The url of the web services that manage this device.
     * @param id
     *            The id of the device inside the service located by url.
     */
    public DomoDevice(final DomoTech tech, final String type,
	    final String description, final String position,
	    final String positionDescription, final String manufacturer,
	    final String serialNumber, final String url, final String id) {
	// create the root of the DOM
	this(tech, type, description, position, positionDescription,
		manufacturer, serialNumber);
	// adding attributes
	setUrl(url);
	setId(id);
    }

    /**
     * Set the new Tech for the device.
     *
     * @param tech
     *            The new techType to be assigned.
     */
    public final void setTech(final DomoTech tech) {
	getDocumentElement().setAttribute("tech", tech.toString());
    }

    /**
     * Get the Tech assigned.
     *
     * @return the Tech of the device.
     */
    public final String getTech() {
	return getDocumentElement().getAttribute("tech");
    }

    /**
     * Set the type of the device (Light and so on).
     *
     * @param type
     *            The type of the device.
     */
    public final void setType(final String type) {
	getDocumentElement().setAttribute("type", type);
    }

    /**
     * Get the type of the device.
     *
     * @return The type of the device.
     */
    public final String getType() {
	return getDocumentElement().getAttribute("type");
    }

    /**
     * Set a description for the device.
     *
     * @param description
     *            The description for the device.
     */
    public final void setDescription(final String description) {
	getDocumentElement().setAttribute("description", description);
    }

    /**
     * Get the description for the device.
     *
     * @return the description for the device.
     */
    public final String getDescription() {
	return getDocumentElement().getAttribute("description");
    }

    /**
     * Set a position for the device.
     *
     * @param position
     *            The position for the device.
     */
    public final void setPosition(final String position) {
	getDocumentElement().setAttribute("position", position);
    }

    /**
     * Get the position for the device.
     *
     * @return The position for the device.
     */
    public final String getPosition() {
	return getDocumentElement().getAttribute("position");
    }

    /**
     * Set a description of the position for the device.
     *
     * @param positionDescription
     *            The description of the position for the device.
     */
    public final void setPositionDescription(final String positionDescription) {
	getDocumentElement().setAttribute("positionDescription",
		positionDescription);
    }

    /**
     * Get the description of the position for the device.
     *
     * @return The description of the position for the device.
     */
    public final String getPositionDescription() {
	return getDocumentElement().getAttribute("positionDescription");
    }

    /**
     * Set the manufacturer of the device.
     *
     * @param manufacturer
     *            The name of the manufacturer of the device.
     */
    public final void setManufacturer(final String manufacturer) {
	getDocumentElement().setAttribute("manufacturer", manufacturer);
    }

    /**
     * Get the location for the device.
     *
     * @return the manufacturer of the device.
     */
    public final String getManufacturer() {
	return getDocumentElement().getAttribute("manufacturer");
    }

    /**
     * Set the serial number of the device.
     *
     * @param serialNumber
     *            The name of the serialNumber of the device.
     */
    public final void setSerialNumber(final String serialNumber) {
	getDocumentElement().setAttribute("serialNumber", serialNumber);
    }

    /**
     * Get the serial number for the device.
     *
     * @return The serial number of the device.
     */
    public final String getSerialNumber() {
	return getDocumentElement().getAttribute("serialNumber");
    }

    /**
     * Set the url of the web services that contain the device.
     *
     * @param url
     *            The url of the web services that contain the device.
     */
    public final void setUrl(final String url) {
	getDocumentElement().setAttribute("url", url);

    }

    /**
     * Get the url of the web services that contain the device.
     *
     * @return The url of the web services that contain the device.
     */
    public final String getUrl() {
	return getDocumentElement().getAttribute("url");
    }

    /**
     * Set the id of the device on the web services.
     *
     * @param id
     *            The id of the device on the web services.
     */
    public final void setId(final String id) {
	getDocumentElement().setAttribute("id", id);
    }

    /**
     * Get the id of the device on the web services.
     *
     * @return The id of the device on the web services.
     */
    public final String getId() {
	return getDocumentElement().getAttribute("id");
    }

    /**
     * Get a list of all nodes that represent services available on the device.
     *
     * @return The list of nodes.
     */
    public final List<DomoDeviceService> getServices() {
	List<DomoDeviceService> serviceNodeList = new LinkedList<DomoDeviceService>();
	NodeList deviceChildren = getFirstChild().getChildNodes();
	for (int i = 0; i < deviceChildren.getLength(); i++) {
	    if (((Element) deviceChildren.item(i)).getTagName()
		    .equalsIgnoreCase("service")) {
		serviceNodeList.add((DomoDeviceService) deviceChildren.item(i));
	    }
	}
	return serviceNodeList;
    }

    /**
     * Set a service for the device.
     *
     * @param name
     *            The name of the service.
     * @param description
     *            The description of the service.
     * @param prettyName
     *            A pretty name for the service.
     * @return The new domoML.domoDevice.DomoDeviceService element.
     */
    public final DomoDeviceService addService(final String name,
	    final String description, final String prettyName) {
	DomoDeviceService serviceElement = new DomoDeviceService(this, name,
		description, prettyName);
	getDocumentElement().appendChild(serviceElement);
	// return the input Element
	return serviceElement;
    }

    /**
     * Set a service for the device.
     *
     * @param name
     *            The name of the service.
     * @param description
     *            The description of the service.
     * @param prettyName
     *            A pretty name for the service.
     * @param output
     *            The domoML.DomoMLDocument.DataType of the output of the
     *            service.
     * @return The new domoML.domoDevice.DomoDeviceService element.
     */
    public final DomoDeviceService addService(final String name,
	    final String description, final String prettyName,
	    DomoMLDocument.DataType output, String outputDescription) {
	DomoDeviceService serviceElement = new DomoDeviceService(this, name,
		description, prettyName, output, outputDescription);
	getDocumentElement().appendChild(serviceElement);
	// return the input Element
	return serviceElement;
    }

    /**
     * Get an Element with tag &quot;service&quot; and attribute
     * &quot;name&quot; with the specified. If element was not found it throws
     * NoElementFoudn exception.
     *
     * @param serviceName
     *            The value of the attribute &quot;name&quot;.
     * @return The element if found.
     * @throws NoElementFoundException
     *             If Element was not found.
     */
    public final List<DomoDeviceService> getService(final String serviceName)
	    throws NoElementFoundException {
	List<DomoDeviceService> returnList = new LinkedList<DomoDeviceService>();
	NodeList deviceChildNodes = getDocumentElement().getChildNodes();
	int i = 0;
	// searching for Element
	for (i = 0; i < deviceChildNodes.getLength(); i++)
	    if (((Element) deviceChildNodes.item(i)).getAttribute("name")
		    .matches(serviceName))
		returnList.add(((DomoDeviceService) deviceChildNodes.item(i)));
	if (!returnList.isEmpty())
	    return returnList;
	else
	    throw new NoElementFoundException();
    }

    /**
     * Get an Element with tag &quot;service&quot; and attribute
     * &quot;name&quot; with the specified. If element was not found it throws
     * NoElementFoudn exception.
     *
     * @param serviceName
     *            The value of the attribute &quot;name&quot;.
     * @return The element if found.
     * @throws NoElementFoundException
     *             If Element was not found.
     */
    public final List<DomoDeviceService> getNamedServices(
	    final String serviceName) throws NoElementFoundException {
	List<DomoDeviceService> returnList = new LinkedList<DomoDeviceService>();
	NodeList deviceChildNodes = getDocumentElement().getChildNodes();
	int i = 0;
	// searching for Element
	for (i = 0; i < deviceChildNodes.getLength(); i++)
	    if (((Element) deviceChildNodes.item(i)).getAttribute("name")
		    .matches(serviceName))
		returnList.add(((DomoDeviceService) deviceChildNodes.item(i)));
	if (!returnList.isEmpty())
	    return returnList;
	else
	    throw new NoElementFoundException();
    }

    /**
     * Give a list of DomoDevice from an XML String. Assume that
     * domoDevicesString has the following structure:
     * &quot;&lt;devices&gt;&lt;device.../&gt;&lt;device.../&gt;
     * &lt;/devices&gt;&quot;.
     *
     * @param domoDevicesString
     *            The String that represent the list of DomoML.
     * @return The list of DomoML.
     */
    public static List<DomoDevice> getDomoDeviceList(String domoDevicesString) {
	// create the list of domoML.domoDevice.DomoDevice to be returned
	List<DomoDevice> domoDevicesList = new LinkedList<DomoDevice>();
	if (!domoDevicesString.trim().equalsIgnoreCase(""))
	    try {
		XMLReader parser = XMLReaderFactory.createXMLReader();
		DomoDeviceSAXParser dmsp = new DomoDeviceSAXParser(
			domoDevicesList);
		parser.setContentHandler(dmsp);
		parser.setErrorHandler(dmsp);
		parser.parse(
			new InputSource(new StringReader(domoDevicesString)));
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	return domoDevicesList;
    }
}
