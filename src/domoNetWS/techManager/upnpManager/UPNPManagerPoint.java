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

package domoNetWS.techManager.upnpManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.cybergarage.upnp.*;
import org.cybergarage.upnp.ssdp.*;
import org.cybergarage.upnp.device.*;
import org.cybergarage.upnp.event.*;
import org.cybergarage.http.*;

import domoML.domoDevice.*;


/**
 * The listener for the UPnP devices. It captures every action performed in the
 * UPnP network.
 */
public class UPNPManagerPoint extends ControlPoint
		implements NotifyListener, EventListener, SearchResponseListener,
		DeviceChangeListener, HTTPRequestListener {

	/** The manager that contains this manager point. */
	protected UPNPManager upnpManager;

	/**
	 * Map the string that represent the datatype used in the upnp xml description
	 * to the corresponding DomoDevice.DataType.
	 */
	private HashMap<String, DomoDevice.DataType> string2DataType = new HashMap<String, DomoDevice.DataType>();

	/**
	 * Constructor of the manager point. Defaults value are used.
	 * 
	 * @param upnpManager
	 *          The manager that contains this manager point.
	 */
	UPNPManagerPoint(final UPNPManager upnpManager) {
		super();
		setNMPRMode(true);
		initDataType();
		this.upnpManager = upnpManager;
		// add listeners
		addNotifyListener(this);
		addSearchResponseListener(this);
		addEventListener(this);
		addDeviceChangeListener(this);
	}

	/**
	 * Constructor of the manager point. Defaults value are used.
	 * 
	 * @param upnpManager
	 *          The manager that contains this manager point.
	 * @param ssdpPort
	 *          The sspdPort to be used.
	 * @param httpPort
	 *          The httpPort to be used.
	 */
	UPNPManagerPoint(final UPNPManager upnpManager, final int ssdpPort,
			final int httpPort) {
		super(ssdpPort, httpPort);
		initDataType();
		this.upnpManager = upnpManager;
		// add listeners
		addNotifyListener(this);
		addSearchResponseListener(this);
		addEventListener(this);
		addDeviceChangeListener(this);
	}

	// //////////////////////////////////////////////
	// Listener
	// //////////////////////////////////////////////

	/**
	 * Notify when a device is added, removed or if is still alive
	 * 
	 * @param packet
	 *          The packet received to be analized.
	 */
	public void deviceNotifyReceived(SSDPPacket packet) {
		if (packet.isDiscover() == true) {
			String st = packet.getST();
			/*
			 * } else if (packet.isAlive() == true) { String usn = packet.getUSN();
			 * String nt = packet.getNT(); String url = packet.getLocation();
			 * writeToDebug("ssdp:alive : uuid = " + usn + ", NT = " + nt +
			 * ", location = " + url);
			 */
		} else if (packet.isByeBye() == true) {
			String usn = packet.getUSN();
			String nt = packet.getNT();
		}
	}

	/**
	 * Notify when the device responds
	 * 
	 * @param packet
	 *          The packet received from the device.
	 */
	public void deviceSearchResponseReceived(SSDPPacket packet) {
		String uuid = packet.getUSN();
		String st = packet.getST();
		String url = packet.getLocation();
	}

	/**
	 * Notify when a notify message is received. For every message it's checked if
	 * the corresponding domoDevice has a &quot;linkedService&quot; tag associated
	 * with another service.
	 */
	public void eventNotifyReceived(String uuid, long seq, String name,
			String value) {
		// get the service corresponding to the notification

		Service service = getSubscriberService(uuid);
		// recognized service searched
		if (service != null) {
			// get the device that contains the service
			Device device = service.getDevice();
			// get the domoDevice that contains the service
			DomoDeviceId domoDeviceId = upnpManager
					.getDomoDeviceIdFromAddress(device.getUDN());
			if (domoDeviceId != null) {
				DomoDevice domoDevice = upnpManager.domoNetWS
						.getDomoDevice(domoDeviceId);
				// checking for linked services getting the list of possible
				// actions
				Iterator actionListIterator = service.getActionList().iterator();
				while (actionListIterator.hasNext()) {
					Action action = (Action) actionListIterator.next();
					try {
						// in upnp it's sure that foreach device there is only
						// one
						// service with a certain name
						DomoDeviceService domoService = domoDevice
								.getService(action.getName()).get(0);
						// get the list of inputs
						List<DomoDeviceServiceInput> domoServiceInputList = domoService
								.getInputs();
						// for the moment it's implemented for one argument
						if (domoServiceInputList.size() == 1) {
							DomoDeviceServiceInput serviceInput = domoService.getInputs()
									.get(0);
							// convert the boolean values to a standard
							// representation
							if (serviceInput.getType().equals(DomoDevice.DataType.BOOLEAN)) {
								if (value.equalsIgnoreCase("on"))
									value = "1";
								else
									value = "0";
							}
							upnpManager.domoNetWS.searchAndExecuteLinkedServices(domoService,
									new DomoDeviceId("", ""), value, serviceInput.getType());
						}
					} catch (NoElementFoundException nefe) {
						// do nothing
					}
				}
			}
		}
	}

	public final void deviceRemoved(Device device) {
		upnpManager.removeDevice(device.getUDN());
	}

	/**
	 * Called when a new device is added to the net.
	 * 
	 * @param device
	 *          The device class that represents the new device.
	 */
	public final void deviceAdded(final Device device) {
		// get the service list from the device... but I'm interesting about
		// actions
		Iterator serviceListIterator = device.getServiceList().iterator();
		// create the new domoDevice
		DomoDevice domoDevice = new DomoDevice(DomoDevice.DomoTech.UPNP,
				// as device type takes the keyword between the
				// "urn:schemas-upnp-org:device:" and the last
				// occurrence of the ":" char
				device.getDeviceType().substring(
						new String("urn:schemas-upnp-org:device:").length(),
						device.getDeviceType().lastIndexOf(":")),
				device.getFriendlyName(), "", "", device.getManufacturer(),
				device.getUDN());
		while (serviceListIterator.hasNext()) {
			// getting services of the device
			Service currentService = (Service) serviceListIterator.next();
			if (!isSubscribed(currentService))
				subscribe(currentService);
			// currentService.setQueryListener(this);
			Iterator actionListIterator = currentService.getActionList().iterator();
			while (actionListIterator.hasNext()) {
				Action action = (Action) actionListIterator.next();
				// action.setActionListener(this);
				String actionName = action.getName();
				// create the service of the domoDevice
				Iterator actionOutputArgumentListIterator = action
						.getOutputArgumentList().iterator();
				while (actionOutputArgumentListIterator.hasNext()) {
					Argument outputArgument = (Argument) actionOutputArgumentListIterator
							.next();
					String outputArgumentName = outputArgument.getName();
					// The type of the input must be patched for some type
					// of devices and functions
					String outputArgumentDataType = currentService
							.getStateVariable(outputArgument.getRelatedStateVariableName())
							.getDataType();

					if (domoDevice.getType().equalsIgnoreCase("MediaServer")) {
						if ((actionName.equalsIgnoreCase("Browse")
								|| (actionName.equalsIgnoreCase("Search")))
								&& outputArgumentDataType.equalsIgnoreCase("String"))
							outputArgumentDataType = "MEDIALIST";
					}

					// create the service of the domoDevice
					DomoDeviceService domoService = domoDevice.addService(actionName, "",
							actionName + " (" + outputArgumentName + ")");
					domoService.setOutput(string2DataType.get(outputArgumentDataType));
					domoService.setOutputName(outputArgumentName);

					Iterator actionInputArgumentListIterator = action
							.getInputArgumentList().iterator();
					while (actionInputArgumentListIterator.hasNext()) {
						// setting input fields for domoDevice
						Argument inputArgument = (Argument) actionInputArgumentListIterator
								.next();
						String inputArgumentName = inputArgument.getName();
						String inputArgumentDataType = currentService
								.getStateVariable(inputArgument.getRelatedStateVariableName())
								.getDataType();
						domoService.addInput(inputArgumentName, "",
								string2DataType.get(inputArgumentDataType));
					}
				}
			}
		}
		// add the new device to the list of devices
		upnpManager.addDevice(domoDevice, device.getUDN());
	}

	private final void initDataType() {
		string2DataType.put("boolean", DomoDevice.DataType.BOOLEAN);
		string2DataType.put("ui1", DomoDevice.DataType.INT);
		string2DataType.put("ui2", DomoDevice.DataType.INT);
		string2DataType.put("i1", DomoDevice.DataType.INT);
		string2DataType.put("i2", DomoDevice.DataType.INT);
		string2DataType.put("i4", DomoDevice.DataType.INT);
		string2DataType.put("int", DomoDevice.DataType.INT);
		string2DataType.put("ui4", DomoDevice.DataType.LONG);
		string2DataType.put("time", DomoDevice.DataType.LONG);
		string2DataType.put("time.tz", DomoDevice.DataType.LONG);
		string2DataType.put("r4", DomoDevice.DataType.FLOAT);
		string2DataType.put("float", DomoDevice.DataType.FLOAT);
		string2DataType.put("r8", DomoDevice.DataType.DOUBLE);
		string2DataType.put("number", DomoDevice.DataType.DOUBLE);
		string2DataType.put("fixed.14.4", DomoDevice.DataType.DOUBLE);
		string2DataType.put("char", DomoDevice.DataType.CHAR);
		string2DataType.put("string", DomoDevice.DataType.STRING);
		string2DataType.put("uri", DomoDevice.DataType.STRING);
		string2DataType.put("uuid", DomoDevice.DataType.STRING);
		string2DataType.put("date", DomoDevice.DataType.DATE);
		string2DataType.put("dateTime", DomoDevice.DataType.DATE);
		string2DataType.put("dateTime.tz", DomoDevice.DataType.DATE);
		string2DataType.put("bin.base64", DomoDevice.DataType.BYTE);
		string2DataType.put("bin.hex", DomoDevice.DataType.BYTE);
		string2DataType.put("MEDIALIST", DomoDevice.DataType.MEDIALIST);
	}
}
