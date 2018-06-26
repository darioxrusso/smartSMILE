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

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import domoML.*;

/**
 * Implements a SAX parser for reading an xml input stream from which take the
 * domoML devices description.
 */
public class DomoDeviceSAXParser extends DefaultHandler {

	/**
	 * Collect the list of domoDevice to be returned at the end of parsing. It's
	 * init to null but if it's call the constructor of this class with a list
	 * as parameter, this field will be set with it.
	 */
	List<DomoDevice> domoDeviceList = null;

	/** Temporany domoDevice for store parsing values. */
	DomoDevice domoDevice;

	/** Store the last service crossed. */
	private DomoDeviceService currentService;

	/** Store the last input crossed. */
	private DomoDeviceServiceInput currentServiceInput;

	/** Store the last linked service crossed. */
	private DomoDeviceServiceLinkedService currentLinkedService;

	/** Store the last linked input crossed. */
	private DomoDeviceServiceLinkedServiceInput currentLinkedInput;

	/**
	 * Call the super constructor of the DefaultHandler and set the device to be
	 * composed.
	 *
	 * @param domoDevice
	 *            The device to be composed.
	 */
	public DomoDeviceSAXParser(DomoDevice domoDevice) {
		super();
		this.domoDevice = domoDevice;
	}

	/**
	 * Call the super constructor of the DefaultHandler and set the list of
	 * device to be returned at the end.
	 *
	 * @param domoDeviceList
	 *            The list to be fill.
	 */
	public DomoDeviceSAXParser(List<DomoDevice> domoDeviceList) {
		this.domoDeviceList = domoDeviceList;
	}

	/** Handler at the beginning of a tag. */
	public final void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		if (name.equalsIgnoreCase("device")) {
			if (domoDeviceList != null)
				// if a list is set, the domoDevice must build there because
				// is not passed as argument to the constructor.
				domoDevice = new DomoDevice();
			domoDevice.setTech(DomoDevice.DomoTech.valueOf(atts.getValue("tech")));
			domoDevice.setType(atts.getValue("type"));
			domoDevice.setDescription(atts.getValue("description"));
			domoDevice.setPositionDescription(atts.getValue("positionDescription"));
			domoDevice.setPosition(atts.getValue("position"));
			domoDevice.setManufacturer(atts.getValue("manufacturer"));
			domoDevice.setSerialNumber(atts.getValue("serialNumber"));
			domoDevice.setUrl(atts.getValue("url"));
			domoDevice.setId(atts.getValue("id"));
		}
		// Add a service
		else if (name.equalsIgnoreCase("service")) {
			if (atts.getValue("output") != null) {
				String outputDescription = atts.getValue("");
				// check if a description for the output is provided.
				// If not, an empty string is assigned to it.
				if (outputDescription == null)
					outputDescription = "";
				currentService = domoDevice.addService(atts.getValue("name"), atts.getValue("description"),
						atts.getValue("prettyName"), DomoMLDocument.DataType.valueOf(atts.getValue("output")),
						outputDescription);
				String outputName = atts.getValue("outputName");
				if (outputName != null)
					currentService.setOutputName(outputName);
			} else
				currentService = domoDevice.addService(atts.getValue("name"), atts.getValue("description"),
						atts.getValue("prettyName"));
		}
		// Add an input to the service
		else if (name.equalsIgnoreCase("input")) {
			currentServiceInput = currentService.addInput(atts.getValue("name"), atts.getValue("description"),
					DomoDevice.DataType.valueOf(atts.getValue("type")));
		}
		// Add an allowed value to the last
		else if (name.equalsIgnoreCase("allowed")) {
			currentServiceInput.addAllowed(atts.getValue("value"));
		}

		// Add an allowed value to the last
		else if (name.equalsIgnoreCase("linkedService")) {
			currentLinkedService = currentService.addLinkedService(atts.getValue("url"), atts.getValue("id"),
					atts.getValue("service"));
			if (atts.getValue("ifInput") != null && atts.getValue("hasValue") != null) {
				currentLinkedService.setIfInput(atts.getValue("ifInput"));
				currentLinkedService.setHasValue(atts.getValue("hasValue"));
			}
		}

		else if (name.equalsIgnoreCase("linkedInput")) {
			currentLinkedInput = currentLinkedService.addLinkedServiceInput(atts.getValue("from"), atts.getValue("to"),
					atts.getValue("value"));
		}
	}

	/** Handler at the ending of a tag. */
	public final void endElement(final String uri, final String localName, final String qName) {
		if (localName.equalsIgnoreCase("device"))
			if (domoDeviceList != null)
				// it was requested a list so add the device to it.
				domoDeviceList.add(domoDevice);
	}
}
