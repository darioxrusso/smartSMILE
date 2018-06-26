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

package domoNetWS.techManager.domoMLTCPManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import common.Debug;
import domoML.domoDevice.DomoDevice;
import domoML.domoDevice.DomoDeviceId;
import domoML.domoDevice.DomoDeviceService;
import domoML.domoDevice.NoElementFoundException;
import domoML.domoMessage.DomoMessage;
import domoML.domoMessage.DomoMessageInput;
import domoNetWS.DomoNetWS;
import domoNetWS.techManager.TechManager;

/**
 * Implements the Sensor Weaver Manager.
 * 
 * @param host
 *          The host where the KNX server is.
 * @param port
 *          The port of the host of the KNX server.
 * @param domoNetWS
 *          The domoNetWS class that links this module.
 */
public class DomoMLTCPManager extends TechManager {

	/**
	 * The server that listen for TCP messages becoming from Sensor-Weaver
	 * network.
	 */
	private static DomoMLTCPTCPServer tcpServer = null;

	/**
	 * Map the serial of a DomoDevice with its DomoDeviceId as configured before.
	 */
	private HashMap<String, DomoDevice> dumpedDomoDeviceSerials = new HashMap<String, DomoDevice>();

	public DomoMLTCPManager(final String host, final int port,
			final DomoNetWS domoNetWS) {
		super(host, port, domoNetWS);
		Debug.getInstance().write(" Loading " + this.getClass().toString() + "... ");
		Debug.getInstance().writeln("done.");
	}

	@Override
	public void addDevice(DomoDevice domoDevice, String address) {
		DomoDeviceId id = null;
		if ((id = getDomoDeviceId(address)) != null) {
			// the device already exists (the serial number was used)
			doubleHash.removeAddress(address);
			domoNetWS.removeDomoDevice(id);
			domoDevice.setId(id.getId());
			domoDevice.setUrl(id.getUrl());
			doubleHash.add(domoNetWS.addDomoDevice(domoDevice), address);
		} else {
			if (!dumpedDomoDeviceSerials
					.containsKey((String) domoDevice.getSerialNumber()))
				// store the new domoDevice
				doubleHash.add(domoNetWS.addDomoDevice(domoDevice), address);
			else {
				domoDevice = dumpedDomoDeviceSerials.get(domoDevice.getSerialNumber());
				doubleHash.add(
						domoNetWS.addDomoDevice(
								dumpedDomoDeviceSerials.get(domoDevice.getSerialNumber())),
						address);
			}
		}
	}

	public void removeDevice(DomoMessage message) {
		DomoDeviceId domoDeviceId = (DomoDeviceId) doubleHash
				.getDomoDeviceId(message.getMessage()).get(0);
		doubleHash.removeAddress(message.getMessage());
		// gets the domoDeviceId that represent the device
		// removes the domoDevice from the list of domoDevices
		domoNetWS.removeDomoDevice(domoDeviceId);

	}

	@Override
	public void loadDumpedDomoDevice(DomoDevice domoDevice) {
		dumpedDomoDeviceSerials.put(domoDevice.getSerialNumber(), domoDevice);
	}

	public void addListOfDevices(String domoDevicesList) {
		List<DomoDevice> list = DomoDevice.getDomoDeviceList(domoDevicesList);
		Iterator<DomoDevice> iterator = list.iterator();
		while (iterator.hasNext()) {
			DomoDevice device = iterator.next();
			addDevice(device, device.getSerialNumber());
		}
	}

	/**
	 * Execute all linked service regarding the write domoMessage.
	 * 
	 * @param data
	 *          The data to be translated in order to search
	 *          &quot;linkedServices&quot;.
	 */
	public final void searchAndExecuteLinkedServices(DomoMessage message) {
		// gets the domoDevice involved with the message taking its address
		DomoDeviceId sourceId = null;
		// The ideal is case 0:
		// 0. field message contains the name of the service to call and field "id"
		// contains the id of the involved domoDevice.The field "name" of the
		// tag "input" contains the name of involved input.
		// To be backward compatible with already developed software, there is the
		// need to consider two cases of tag "message":
		// 1. field "message" is empty and field "id" contains the id of the
		// involved domoDevice. The field "name" of the tag "input" contains the
		// name of the service and of the involved input;
		// 2. field message contains the serialNumber of the domoDevice. The field
		// "id" is empty. The field "name" of the tag "input" contains the name
		// of the service.

		// specifies the belonging case of the arrived message, as described above.
		// It is initialized to 0 (the ideal case).
		int dirtyHack = 0;
		if (message.getMessage().trim().isEmpty()) {
			sourceId = new DomoDeviceId(message.getSenderURL(),
					message.getSenderId());
			if (sourceId != null)
				dirtyHack = 1;
			// else case should be managed as error and it should never happen.
		} else {
			if (doubleHash.getDomoDeviceId(message.getMessage()) != null) {
				dirtyHack = 2;
				sourceId = doubleHash.getDomoDeviceId(message.getMessage()).get(0);
			} else {
				// case 0
				sourceId = new DomoDeviceId(message.getSenderURL(),
						message.getSenderId());
				// if(sourceId == null) case should be managed as error and it should
				// never happen.
			}
		}
		if (sourceId != null) {
			message.setSenderId(sourceId.getId());
			message.setSenderURL(sourceId.getUrl());
			message.setReceiverId(sourceId.getId());
			message.setReceiverURL(sourceId.getUrl());
			DomoDevice device = domoNetWS.getDomoDevice(sourceId);
			Iterator<Node> domoMessageInputIterator = message
					.getInputParameterElements().iterator();
			while (domoMessageInputIterator.hasNext()) {
				DomoMessageInput domoMessageInput = (DomoMessageInput) domoMessageInputIterator
						.next();
				String inputName = domoMessageInput.getName();
				try {
					// supposes that every service has a different name.
					DomoDeviceService service = null;
					if (dirtyHack != 0)
						service = device.getService(inputName).get(0);
					else
						service = device.getService(message.getMessage()).get(0);
					domoNetWS.searchAndExecuteLinkedServices(service,
							new DomoDeviceId("", device.getId()),
							message.getInput(inputName).getValue(),
							message.getInput(inputName).getType());
					if (dirtyHack != 0)
						// the message field containing the serial number of the device that
						// originates the message, is replaced with the name of the service.
						message.setMessage(service.getName());
				} catch (NoElementFoundException e) {
					e.printStackTrace();
				}
			}
			domoNetWS.sendClientsUpdate(message.toString());
		}
	}

	public DomoDeviceId getDomoDeviceId(String address) {
		DomoDeviceId id = null;
		if (doubleHash.getDomoDeviceId(address) != null)
			id = doubleHash.getDomoDeviceId(address).get(0);
		return id;
	}

	@Override
	public void start() {
		createSocketServer();

	}

	@Override
	public DomoMessage execute(DomoMessage domoMessage) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	private void createSocketServer() {
		Debug.getInstance().writeln("Creating socket for " + this.toString());
		if (tcpServer == null) {
			// tcpServer for incoming messages not yet initialized
			tcpServer = new DomoMLTCPTCPServer(this);
			tcpServer.start();
		}
	}

	@Override
	public void finalize() {
		// TODO Auto-generated method stub

	}

}
