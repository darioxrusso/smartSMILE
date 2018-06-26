/* This file is part of DOMONET.

 Copyright (C) 2007 Mattia Ferrari

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

package domoNetWS.techManager.x10Manager;

import java.util.*;

import domoML.*;
import domoML.domoDevice.*;
import domoML.domoMessage.*;
import domoNetWS.DomoNetWS;
import domoNetWS.techManager.TechManager;

import x10.*;

/**
 * Extends the DomoManager class in order to implement a manager for the X10
 * protocol.
 */
public class x10Manager extends TechManager {
	protected x10.Controller controller;
	protected x10Listener listen;

	/**
	 * Constructor.
	 * 
	 * @param model
	 *            The model of the x10 serial Controller (CM11 or CM17A).
	 * @param port
	 *            The port where x10 serial Controller was connect. Ie. On a
	 *            Windows based PC usually COMM1.
	 * @param domoNetWS
	 *            The reference of the domoNetWS that links the module.
	 */
	public x10Manager(final String model, final String port, final DomoNetWS domoNetWS) {
		super(domoNetWS);
		System.out.println("   Loading " + this.getClass().toString() + " using " + model + " as Serial Controller and "
				+ port + " as comport.");
		try {
			if (model.compareTo("CM17A") == 0)
				this.controller = new x10.CM17ASerialController(port);
			else
				this.controller = new x10.CM11ASerialController(port);
			listen = new x10Listener(this);
			controller.addUnitListener(listen);
			System.out.println("X10 Controller initialized");
		} catch (java.io.IOException e) {
			System.out.println("Failure to init Controller");
		}
	}

	/**
	 * Add a domoML device to the list of domoDevice in the DomoNetWS.
	 * 
	 * @param domoDevice
	 *            The string rappresentation of the device to add.
	 * @param address
	 *            The real address in the manager.
	 */
	public void addDevice(final DomoDevice domoDevice, String address) {
		// call the addDevice of the DomoNetWS to adds it.
		// The DomoNetWS returns the domoDeviceId so it's possible fill
		// the doubleHash class correctly.
		doubleHash.add(domoNetWS.addDomoDevice(domoDevice), address);
		if (domoDevice.getType().equalsIgnoreCase("LIGHT"))
			address = address + "L";
		listen.addDevice(address);
	}

	/**
	 * Execute a domoMessage converting it to a message for the X10 bus.
	 * 
	 * @param domoMessage
	 *            the domoMessage to be converted and executed.
	 * @return The domoMessage as response of the one to be executed.
	 * 
	 * @throws Exception
	 */
	public final DomoMessage execute(final DomoMessage domoMessage) throws Exception {
		DomoDeviceId id = new DomoDeviceId(domoMessage.getReceiverURL(), domoMessage.getReceiverId());
		DomoDevice device = super.domoNetWS.getDomoDevice(id);
		String dest = doubleHash.getAddress(id);
		byte function = convert(domoMessage.getMessage());
		if (function == -1) {
			System.out.println("Error: Command not known");
			throw new Exception("Comand not found");
		}
		String response;
		if (function == 15) {
			System.out.println("Executing Command: Status request on: " + dest);
			int status;
			if (device.getType().equalsIgnoreCase("LIGHT"))
				status = listen.getStauts(dest + "L");
			else
				status = listen.getStauts(dest);
			if (status == -1)
				response = "UnKonwn";
			else
				response = Integer.toString(status);
		} else {
			if (function == x10.Command.DIM) {
				controller.addCommand(new Command(dest, function, 1));
			} else {
				if (function == x10.Command.BRIGHT) {
					controller.addCommand(new Command(dest, function, 1));
				} else {
					controller.addCommand(new Command(dest, function));
				}
			}
			System.out.println("Executing Command: " + domoMessage.getMessage() + " on: " + dest);
			response = domoMessage.getMessage();
		}
		return (new DomoMessage(domoMessage.getReceiverURL(), domoMessage.getReceiverId(), domoMessage.getSenderURL(),
				domoMessage.getSenderId(), response, DomoMessage.MessageType.SUCCESS));
	}

	/** Action to do when the manager is closed. */
	public void finalize() {
		controller.addCommand(new x10.Command("A1", x10.Command.ALL_UNITS_OFF));
		listen.clear();
	}

	/**
	 * Make the x10 Tech Manager active
	 * 
	 */
	public void start() {
		// do nothing.
	}

	/**
	 * Load a domoDevice already defined.
	 * 
	 * @param domoDevice
	 *            The string rappresentation of the device to add.
	 */
	public void loadDumpedDomoDevice(final DomoDevice domoDevice) {
		this.addDevice(domoDevice, domoDevice.getSerialNumber());
	}

	/**
	 * Launch the linkedService of the Service named fun of all x10 Device
	 * having the adress adress
	 * 
	 * @param address
	 *            The serialNumber of the Device
	 * 
	 * @param type
	 *            The type of the Device
	 * 
	 * @param fun
	 *            The name of the Service
	 */
	public void launchLinkedService(String address, String type, String fun) {
		System.out.println("Codice: " + address + " tipo: " + type + " funzione: " + fun + " end");
		Object[] dev = doubleHash.getDomoDeviceId(address).toArray();
		for (int i = 0; i < dev.length; i++) {
			DomoDevice device = super.domoNetWS.getDomoDevice((DomoDeviceId) dev[i]);
			try {
				List<DomoDeviceService> service = device.getService(fun);
				System.out.println(service.get(0).getName() + "servizio usato" + service.get(0).getPrettyName() + "\n");
				super.domoNetWS.searchAndExecuteLinkedServices(service.get(0), (DomoDeviceId) dev[i], fun,
						DomoMLDocument.DataType.STRING);
			} catch (NoElementFoundException ecc) {
			}
		}
	}

	/**
	 * Convert message to byte to give to the x10 interface.
	 *
	 * @param fun
	 *            the string to be converted to byte code.
	 */
	private final byte convert(String fun) {
		if (fun.equalsIgnoreCase("all units off"))
			return x10.Command.ALL_UNITS_OFF;
		if (fun.equalsIgnoreCase("all lights on"))
			return x10.Command.ALL_LIGHTS_ON;
		if (fun.equalsIgnoreCase("on"))
			return x10.Command.ON;
		if (fun.equalsIgnoreCase("off"))
			return x10.Command.OFF;
		if (fun.equalsIgnoreCase("dim"))
			return x10.Command.DIM;
		if (fun.equalsIgnoreCase("bright"))
			return x10.Command.BRIGHT;
		if (fun.equalsIgnoreCase("all lights off"))
			return x10.Command.ALL_LIGHTS_OFF;
		if (fun.equalsIgnoreCase("status"))
			return 15;
		return -1;
	}

}
