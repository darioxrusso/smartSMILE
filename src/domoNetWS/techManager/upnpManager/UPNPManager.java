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

import java.util.*;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.StringReader;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.*;

import domoML.domoDevice.*;
import domoML.domoMessage.*;
import domoNetWS.DomoNetWS;
import domoNetWS.techManager.TechManager;
import common.Debug;

/**
 * Extends the DomoManager class in order to implement a manager for the upnp
 * protocol.
 */
public class UPNPManager extends TechManager {

	/** Listener for the UPnP network. It implements listener action. */
	protected UPNPManagerPoint upnpManagerPoint = new UPNPManagerPoint(this);

	/**
	 * Map the serial of a DomoDevice with its DomoDeviceId as configured before.
	 */
	private HashMap<String, DomoDevice> dumpedDomoDeviceSerials = new HashMap<String, DomoDevice>();

	private MediaController controller;

	/**
	 * Constructor.
	 * 
	 * @param ssdpPort
	 *          The ssdp port to use.
	 * @param httpPort
	 *          The http port to use.
	 * @param domoNetWS
	 *          The reference of the domoNetWS that links the module.
	 */
	public UPNPManager(final int ssdpPort, final int httpPort,
			final DomoNetWS domoNetWS) {
		// this has no sense and param are not used exept for the DomoNetWS but the
		// super
		super(domoNetWS);
		Debug.getInstance().writeln(" Loading " + this.getClass().toString() + "using " + ssdpPort
				+ " as ssdp port and " + httpPort + " as http port.");
		this.upnpManagerPoint = new UPNPManagerPoint(this, ssdpPort, httpPort);
	}

	/**
	 * Constructor that uses default ssdp and http port.
	 * 
	 * @param domoNetWS
	 *          The reference of the domoNetWS that links the module.
	 */
	public UPNPManager(final DomoNetWS domoNetWS) {
		super(domoNetWS);
		Debug.getInstance().write(" Loading " + this.getClass().toString()
				+ " using default ssdp and http port... ");
		this.upnpManagerPoint = new UPNPManagerPoint(this);
		Debug.getInstance().writeln("done.");
	}

	public void start() {
		Debug.getInstance().write("Starting  " + this.getClass().toString() + " using "
				+ getHost() + ":" + getPort() + "... ");
		upnpManagerPoint.start();
		Debug.getInstance().writeln("done.");
	}

	/**
	 * Add a domoML device to the list of domoDevice in the DomoNetWS.
	 * 
	 * @param domoDevice
	 *          The string rappresentation of the device to add.
	 * @param address
	 *          The real address in the manager.
	 */
	public void addDevice(final DomoDevice domoDevice, final String address) {
		// Dirty trick to make Intel mediaRenderer always be recognised
		// anytime.
		System.out.println("Aggiungo: " + domoDevice.getType() + " "
				+ domoDevice.getManufacturer());
		if (domoDevice.getType().equals("MediaRenderer")
				&& domoDevice.getDescription().equals("XBMC (Domotica-TV)")) {
			doubleHash.add(domoNetWS.addDomoDevice(
					dumpedDomoDeviceSerials.get("IntelMediaRenderer")), address);
		} else if (domoDevice.getType().equals("MediaServer") && domoDevice
				.getDescription().equals("Intel's Media Server (DOMOTICA-TV)")) {
			doubleHash.add(domoNetWS.addDomoDevice(dumpedDomoDeviceSerials
					.get("uuid:214578e3-d4a0-f498-790e-d927709b781d")), address);
		} else
		// end of the dirty trick

		// call the addDevice of the DomoNetWS to adds it.
		// The DomoNetWS returns the domoDeviceId so it's possible fill
		// the doubleHash class correctly.
		if (!dumpedDomoDeviceSerials
				.containsKey((String) domoDevice.getSerialNumber()))
			// The phisical address was stored in domoDevices.serial field.
			doubleHash.add(domoNetWS.addDomoDevice(domoDevice), address);
		else
			// store the domoDevice loaded after
			doubleHash.add(
					domoNetWS.addDomoDevice(
							dumpedDomoDeviceSerials.get(domoDevice.getSerialNumber())),
					address);
	}

	/**
	 * Remove an upnp device to the list of domoDevice in the DomoNetWS.
	 * 
	 * @param device
	 *          The string rappresentation of the device to add.
	 */
	public void removeDevice(final String address) {
		// gets the domoDeviceId that rappresent the upnp device
		Iterator domoDeviceIdIt = doubleHash.getDomoDeviceId(address).iterator();
		// removes the upnp device from the list of the upnpManager
		doubleHash.removeAddress(address);
		// removes the domoDevice from the list of domoDevices
		while (domoDeviceIdIt.hasNext())
			domoNetWS.removeDomoDevice(((DomoDeviceId) domoDeviceIdIt.next()));
	}

	public void loadDumpedDomoDevice(final DomoDevice domoDevice) {
		dumpedDomoDeviceSerials.put(domoDevice.getSerialNumber(), domoDevice);
	}

	/**
	 * Execute a domoML.domoMessage.DomoMessage.
	 * 
	 * @param domoMessage
	 *          The message to be executed.
	 * 
	 * @return The resulting domoML.DomoMessage.DomoMessage after the execution.
	 */
	public final DomoMessage execute(final DomoMessage domoMessage)
			throws Exception {
		// upnpManagerPoint.execute(domoMessage);
		// creating the device receiver of the message
		Debug.getInstance().writeln("Execute on UPnP Net: " + domoMessage);
		Device device = upnpManagerPoint.getDevice(
				doubleHash.getAddress(new DomoDeviceId(domoMessage.getReceiverURL(),
						domoMessage.getReceiverId())));
		// device is not yet registred on domonet
		if (device == null)
			return new DomoMessage("", "", "", "", "",
					DomoMessage.MessageType.FAILURE);

		ArgumentList outArgList = null;
		// getting the action to execute on the upnp device
		if (device.getDeviceType().indexOf("device:MediaServer") >= 0) {
			Device mediaRendererDevice = null;
			if (domoMessage.getMessage().equals("Play")) {
				mediaRendererDevice = upnpManagerPoint
						.getDevice(doubleHash.getAddress(new DomoDeviceId(
								domoMessage.getInput("mediaRendererURL").getValue(), domoMessage
										.getInput("mediaRendererId").getValue())));
				// device is not yet viewed on domonet
				if (mediaRendererDevice == null)
					return new DomoMessage("", "", "", "", "",
							DomoMessage.MessageType.FAILURE);
			}
			activateMediaController(device, mediaRendererDevice);
			outArgList = controller.execute(domoMessage, device);

			DomoDeviceService domoService = domoNetWS.deviceList
					.get(new DomoDeviceId("", domoMessage.getReceiverId())).
					// in upnp it's sure that foreach device there is only one
					// service with a certain name !!! WRONG !!!
					getService(domoMessage.getMessage()).get(0);
			domoNetWS.searchAndExecuteLinkedServices(domoService,
					new DomoDeviceId("", ""), "", DomoMessage.DataType.STRING);
		} else if (device.getDeviceType().indexOf("device:MediaRenderer") >= 0) {
			Device mediaServerDevice = null;
			if (domoMessage.getMessage().equals("Play")) {
				mediaServerDevice = upnpManagerPoint.getDevice(doubleHash.getAddress(
						new DomoDeviceId(domoMessage.getInput("mediaServerId").getValue(),
								domoMessage.getInput("mediaServerURL").getValue())));
				// device is not yet viewed on domonet
				if (mediaServerDevice == null)
					return new DomoMessage("", "", "", "", "",
							DomoMessage.MessageType.FAILURE);
			}
			activateMediaController(mediaServerDevice, device);
			outArgList = controller.execute(domoMessage, device);

			DomoDeviceService domoService = domoNetWS.deviceList
					.get(new DomoDeviceId("", domoMessage.getReceiverId())).
					// in upnp it's sure that foreach device there is only one
					// service with a certain name
					getService(domoMessage.getMessage()).get(0);
			domoNetWS.searchAndExecuteLinkedServices(domoService,
					new DomoDeviceId("", ""), "", DomoMessage.DataType.STRING);
		} else {
			Action action = device.getAction(domoMessage.getMessage());
			// setting up arguments from domoMessage
			Iterator inputParameterElements = domoMessage.getInputParameterElements()
					.iterator();
			while (inputParameterElements.hasNext()) {
				DomoMessageInput messageInput = (DomoMessageInput) inputParameterElements
						.next();
				try {
					action.setArgumentValue(messageInput.getName(),
							messageInput.getValue());
				} catch (Exception e) {
					e.printStackTrace();
				}
				DomoDeviceService domoService = domoNetWS.deviceList
						.get(new DomoDeviceId("", domoMessage.getReceiverId())).
						// in upnp it's sure that foreach device there is only one
						// service with a certain name
						getService(domoMessage.getMessage()).get(0);
				domoNetWS.searchAndExecuteLinkedServices(domoService,
						new DomoDeviceId("", ""), messageInput.getValue(),
						messageInput.getType());
			}
			if (action.postControlAction()) {
				// operation executed successfully.
				// getting output argument list
				outArgList = action.getOutputArgumentList();
			} else {
				UPnPStatus err = action.getControlStatus();
				String msg = err.getDescription() + " ("
						+ Integer.toString(err.getCode()) + ")";
				return new DomoMessage("", "", "", "", msg,
						DomoMessage.MessageType.FAILURE);
			}
		}
		if (outArgList != null) {
			int nArgs = outArgList.size();
			// init the response message
			String msg = "";
			if (nArgs == 0)
				msg = "No response value";
			// concat response values
			for (int n = 0; n < nArgs; n++) {
				Argument arg = outArgList.getArgument(n);
				try {
					String outputName = domoMessage.getOutputName();
					if (arg.getName().equalsIgnoreCase(outputName)) {
						msg = arg.getValue();
						// see if message must be converted
						if (domoMessage.getOutput()
								.equals(DomoMessage.DataType.MEDIALIST)) {
							msg = string2MediaList(msg);
						}

						DomoDeviceService domoService = domoNetWS.deviceList
								.get(new DomoDeviceId("", domoMessage.getReceiverId())).
								// in upnp it's sure that foreach device there is only one
								// service with a certain name
								getService(domoMessage.getMessage()).get(0);
						domoNetWS.searchAndExecuteLinkedServices(domoService,
								new DomoDeviceId("", ""), escapeHTML(msg),
								domoMessage.getOutput());

					}
				} catch (domoML.domoMessage.NoAttributeFoundException e) {
					msg += arg.getName() + "=" + arg.getValue();
					if (n < nArgs - 1)
						msg += ", ";
				}
			}
			return new DomoMessage("", "", "", "", escapeHTML(msg),
					DomoMessage.MessageType.SUCCESS);
			// operation take a failure
		}
		return null;
	}

	/**
	 * Get the corresponding domoML.domoDevice.DomoDeviceId from the real address.
	 * 
	 * @param address
	 *          The real address.
	 * 
	 * @return The corresponding domoDeviceId
	 */
	public DomoDeviceId getDomoDeviceIdFromAddress(String address) {
		List<DomoDeviceId> domoDeviceIdList = doubleHash.getDomoDeviceId(address);
		if (domoDeviceIdList != null)
			return domoDeviceIdList.get(0);
		return null;
	}

	public static final String escapeHTML(String s) {
		StringBuffer sb = new StringBuffer();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '&':
				sb.append("&amp;");
				break;
			case '"':
				sb.append("&quot;");
				break;
			// be carefull with this one (non-breaking whitee space)
			// case ' ': sb.append("&nbsp;");break;
			case ' ':
				sb.append(" ");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}

	public static final String unescapeHTML(String s, int f) {
		String[][] escape = { { "&lt;", "<" }, { "&gt;", ">" }, { "&amp;", "&" },
				{ "&quot;", "\"" } };
		int i, j, k, l;

		i = s.indexOf("&", f);
		if (i > -1) {
			j = s.indexOf(";", i);
			// --------
			// we don't start from the beginning
			// the next time, to handle the case of
			// the &amp;
			// thanks to Pieter Hertogh for the bug fix!
			f = i + 1;
			// --------
			if (j > i) {
				// ok this is not most optimized way to
				// do it, a StringBuffer would be better,
				// this is left as an exercise to the reader!
				String temp = s.substring(i, j + 1);
				// search in escape[][] if temp is there
				k = 0;
				while (k < escape.length) {
					if (escape[k][0].equals(temp))
						break;
					else
						k++;
				}
				if (k < escape.length) {
					s = s.substring(0, i) + escape[k][1] + s.substring(j + 1);
					return unescapeHTML(s, f); // recursive call
				}
			}
		}
		return s;
	}

	/**
	 * Convert an XML string to a MediaList string
	 * 
	 * @param message
	 *          the string to convert
	 * @return the string converted
	 */
	public static String string2MediaList(final String message) {
		try {
			XMLReader parser = XMLReaderFactory.createXMLReader();
			String2MediaListSAXParser dmsp = new String2MediaListSAXParser();
			parser.setContentHandler(dmsp);
			parser.setErrorHandler(dmsp);
			parser.parse(new InputSource(new StringReader(message)));
			return dmsp.getMediaList().toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/** Action to do when the manager is closed. */
	public void finalize() {
		if (controller != null)
			controller.stop();
	}

	/**
	 * Startup the media controller using a certain media server and renderer
	 * 
	 * @param mediaServer
	 *          the media server to use
	 * @param mediaRenderer
	 *          the media renderer to use
	 */
	public final void activateMediaController(final Device mediaServer,
			final Device mediaRenderer) {
		if (controller == null) {
			controller = MediaController.getInstance(mediaServer, mediaRenderer);
			controller.start();
		}
		controller.setMediaServer(mediaServer);
		controller.setMediaRenderer(mediaRenderer);
	}

}
