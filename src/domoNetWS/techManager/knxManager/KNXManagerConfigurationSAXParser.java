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

package domoNetWS.techManager.knxManager;

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import common.Debug;

import domoML.domoDevice.*;

/**
 * Implements a SAX parser for reading an xml input stream from which take the
 * domoML devices description.
 */
public class KNXManagerConfigurationSAXParser extends DefaultHandler {

	/**
	 * Collect the list of domoDevice to be returned at the end of parsing. It's
	 * used an hashmap that maps the name of the device with the domodevice
	 * because during the parsing, it's needed, foreach row, to find if a
	 * domoDevice with that name already exists or not. If exists, it's
	 * sufficient to add the new service (rappresented as row) to it else it's
	 * needed to create the new device and add the service.
	 */
	HashMap<String, DomoDevice> deviceList = new HashMap<String, DomoDevice>();

	/** Maps group addresses to DomoML datatypes */
	HashMap<String, domoML.DomoMLDocument.DataType> groupAddress2DataTypeList = new HashMap<String, domoML.DomoMLDocument.DataType>();

	/** Keep if it's parsing inside the &quot;row&quot; tag. */
	private boolean rowFound = false;

	/** The configuration taken from the row parsed. */
	private static RowConfiguration rowConfiguration = new RowConfiguration();
	public String charsTrimmed;
	/**
	 * If rowFound is true, it's needed to find the &quot;colValue&quot; tag.
	 * Each &quot;colValue&quot; tag has an attribute named &quot;nr&quot; which
	 * identify the type of the information stored in the text field. Keeping
	 * the &quot;nr&quot attribute when a &quot;character&quot; is found, permit
	 * us to give a semantic to it.
	 */
	private int currentNr = 0;

	/** The manager that launch the device configurator. */
	private KNXManager knxManager;

	/**
	 * Constructor that simply calls the super constructor of the
	 * DefaultHandler.
	 */
	public KNXManagerConfigurationSAXParser(KNXManager knxManager) {
		super();
		this.knxManager = knxManager;
	}

	/** Handler at the beginning of a tag. */
	public final void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		charsTrimmed = "";
		if (name.equalsIgnoreCase("row")) {
			rowFound = true;
		}
		// Add a service
		else if (name.equalsIgnoreCase("colValue")) {
			// found a &quot;colValue&quot; tag... found useful data.
			// Convert the string value of &quot;nr&quot; attribute as int in
			// order to make a switch
			// (comparisons are fasters instead of string comparison with
			// some if test. It's possible do it beause I know that values
			// are int)
			currentNr = (int) new Integer(atts.getValue("nr"));
		}
	}

	/** Handler at the ending of a tag. */
	public final void endElement(final String uri, final String localName, final String qName) {

		if (!charsTrimmed.equals(""))
			// switching to give a semantic to the characters
			switch (currentNr) {
			case 1:
				int firstCharSpace = charsTrimmed.indexOf(" ");
				rowConfiguration.servicePrettyName = (charsTrimmed.substring(firstCharSpace));
				break;
			case 2:
				// rowConfiguration.servicePrettyName = charsTrimmed;
				break;
			case 3:
				// get the first occurrence of the space.
				// The tag text has a fixed format:
				// phisical address (with "." instead of "/") +
				// " " + 11th field.
				// I'm interested only to the phisical address with "/"s.
				int firstSpaceChar = charsTrimmed.indexOf(" ");
				if (firstSpaceChar > 0)
					rowConfiguration.phisicalAddress = (charsTrimmed.substring(0, firstSpaceChar));// .replace(".",
																									// "/");
				else
					Debug.getInstance().writeln("Physical address not found: " + charsTrimmed);

				rowConfiguration.deviceType = charsTrimmed;

				break;
			case 5:
				// verify if the transmission (if one) provide an ack
				// to do: verify that "A" is right
				if (charsTrimmed.equalsIgnoreCase("A"))
					rowConfiguration.ackProvided = true;
				else
					rowConfiguration.ackProvided = false;
				break;
			case 7:
				// verify if the field is readable
				if (charsTrimmed.equalsIgnoreCase("R"))
					rowConfiguration.isReadable = true;
				else
					rowConfiguration.isReadable = false;
				break;
			case 8:
				// verify if the field is writeable
				if (charsTrimmed.equalsIgnoreCase("W"))
					rowConfiguration.isWriteable = true;
				else
					rowConfiguration.isWriteable = false;
				break;
			case 9:
				// verify if it's able to transmit data. If not, I'm not
				// interested to it because I don't need devices without any
				// use.
				// The transmission is used for write and for read too.
				if (charsTrimmed.equalsIgnoreCase("T"))
					rowConfiguration.canTransmit = true;
				else
					rowConfiguration.canTransmit = false;
				break;
			case 11:
				// rowConfiguration.deviceType = charsTrimmed;
				break;
			case 12:
				rowConfiguration.serviceDescription = charsTrimmed;
				break;
			case 13:
				rowConfiguration.inputDataType = charsTrimmed;
				break;
			case 15:
				rowConfiguration.serviceName = charsTrimmed;
			default:
				// do nothing.
				break;
			}
		if (localName.equalsIgnoreCase("colValue"))
			currentNr = 0;
		else if (localName.equalsIgnoreCase("row")) {
			rowFound = false;
			// taken all the row information so it's possible to decide if
			// the data stored are useful testing the transmit, write and read
			// fields of the rowConfiguration
			if ((rowConfiguration.canTransmit) && (rowConfiguration.isReadable || rowConfiguration.isWriteable)) {
				// if (rowConfiguration.isReadable ||
				// rowConfiguration.isWriteable) {
				// the rowConfiguration has interesting data
				try {
					importDomoDeviceFromRow();
				} catch (ImportDeviceException ide) {
					// to nothing, not interesting row
				}
			}
		}
	}

	/**
	 * Handler when are found characters inside an element.
	 */
	public final void characters(final char[] ch, final int start, final int length) throws SAXException {

		if (rowFound && currentNr != 0) {
			// read the chars and put it in a string buffer
			StringBuffer chars = new StringBuffer();
			for (int i = 0; i < length; i++)
				chars.append(ch[start + i]);
			// remove empty initial spaces of the text of the tag
			charsTrimmed = chars.toString().trim();

			// if no input values valid. This test is useful if xml is not
			// formatted perfectly
		}
	}

	/**
	 * Get the rowConfiguration and try to create or modify a domoDevice.
	 * 
	 * @throws ImportDeviceException
	 *             if some conversion to DomoDevice (it's not possible to find a
	 *             right DomoDevice.DataType or the phisical address has a bad
	 *             format) doesn't go right.
	 */
	private void importDomoDeviceFromRow() throws ImportDeviceException {
		// checking if critical conversions goes right:
		// check if it's possible to convert string datatype to
		// DomoDevice.DataType
		DomoDevice.DataType dataType = knxManager.string2DataType.get(rowConfiguration.inputDataType);
		if (dataType == null)
			throw new ImportDeviceException("Error importing Konnex datatype " + rowConfiguration.inputDataType);
		// check if it was parsed a valid knx group address matching it
		// with the regular expression int/int/int
		if (!rowConfiguration.phisicalAddress.matches("[0-9]*.[0-9]*.[0-9]*"))
			throw new ImportDeviceException("Error importing Konnex phisical address: "
					+ rowConfiguration.phisicalAddress + " in " + rowConfiguration.toString());
		// anything goes right
		// checking if the domodevice already exists
		DomoDevice domoDevice = deviceList.get(rowConfiguration.phisicalAddress);
		if (domoDevice == null) {
			// the device not exists so it's needed to create new one.
			domoDevice = new DomoDevice(DomoDevice.DomoTech.KNX, rowConfiguration.deviceType, "", "", "", "",
					rowConfiguration.phisicalAddress);
		}
		// now it's required to insert the service to the domoDevice
		// If commaPosition has a positive value, more group address are set
		// but I'm interested only at the first that is the address where
		// it's possible to transmit else it's found only one group address and
		// I can take it.
		String serviceName = rowConfiguration.serviceName;
		int commaPosition = serviceName.indexOf(",");
		if (commaPosition > 0)
			serviceName = serviceName.substring(0, commaPosition);
		// add the serviceName and the datatype to an hashmap
		groupAddress2DataTypeList.put(serviceName, dataType);
		// if it's a read value provide an output attribute on the service tag
		if (rowConfiguration.isReadable) {
			DomoDeviceService service = domoDevice.addService(serviceName, rowConfiguration.serviceDescription,
					rowConfiguration.servicePrettyName);
			service.setOutput(dataType);
			service.setDescription(rowConfiguration.inputDescription);
		}
		// if it's a write value insert input
		if (rowConfiguration.isWriteable) {
			DomoDeviceService service = domoDevice.addService(serviceName, rowConfiguration.serviceDescription,
					rowConfiguration.servicePrettyName);
			DomoDeviceServiceInput input = service.addInput(rowConfiguration.inputName,
					rowConfiguration.inputDescription, dataType);
			if (dataType.equals(DomoDevice.DataType.BOOLEAN)) {
				input.addAllowed("0");
				input.addAllowed("1");
			}
		}
		// than add it to the hashmap.
		deviceList.put(rowConfiguration.phisicalAddress, domoDevice);
	}

	/**
	 * Return the java.util.HashMap that should contains the result of the
	 * parser.
	 * 
	 * @return The result of the parser.
	 */
	public final List<DomoDevice> getDomoDevices() {
		return new LinkedList<DomoDevice>(deviceList.values());
	}

	/**
	 * Return the java.util.HashMap that should contains the result of the
	 * parser.
	 * 
	 * @return The result of the parser.
	 */
	public final HashMap<String, domoML.DomoMLDocument.DataType> getGroupAddress2DataTypes() {
		return groupAddress2DataTypeList;
	}

	/**
	 * Rappresent a single row that will be parsed and used for defining and
	 * modify new or existing domoDevice.
	 */
	static class RowConfiguration {
		/** The name of the device. Taken from the 11 th row field. */
		public String deviceType;

		/** The real address of the device. Taken from the 3rd row field. */
		public String phisicalAddress;

		/**
		 * The name of the service. For convenction it's used the group address
		 * of the row. Taken from the 15th row field.
		 */
		public String serviceName;

		/** The description of the service. Taken from the 12th row field. */
		public String serviceDescription;

		/** The pretty name for the service. Taken from the 2nd row field. */
		public String servicePrettyName;

		/** The name of the input. Fixed field. */
		public String inputName = "value";

		/** If follow an ack after the transmission. */
		public boolean ackProvided = false;

		/**
		 * If the field of the service is readable. Taken from the 7th row
		 * field.
		 */
		public boolean isReadable = false;

		/**
		 * If the field of the service is writeable. Taken from the 8th row
		 * field.
		 */
		public boolean isWriteable = false;

		/**
		 * If it's able to transmit data. If not, I'm not interested to it
		 * because I don't need devices without any use. The transmission is
		 * used for write and for read too. Taken from the 9th row field.
		 */
		public boolean canTransmit = false;

		/** The datatype of the input field. Taken from the 13th row field. */
		public String inputDataType;

		/** The description of the input. Fixed field. */
		public String inputDescription = "";

		public final String toString() {
			return "deviceType: " + deviceType + " phisicalAddress: " + phisicalAddress + " serviceName: " + serviceName
					+ " serviceDescription: " + serviceDescription + " servicePrettyName: " + servicePrettyName
					+ " inputName: " + inputName + " ackProvided: " + ackProvided + " isReadable: " + isReadable
					+ " isWriteable: " + isWriteable + " canTransmit: " + canTransmit + " inputDataType: "
					+ inputDataType + " inputDescription: " + inputDescription;
		}
	}
}
