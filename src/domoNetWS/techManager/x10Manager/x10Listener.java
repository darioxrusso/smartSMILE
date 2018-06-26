/*
 * x10Listener.java
 * 
 * Created on Jul 15, 2007, 1:54:30 AM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package domoNetWS.techManager.x10Manager;

import x10.*;
import java.util.*;

/**
 *
 * @author mattia
 */
public class x10Listener implements x10.UnitListener {

	private java.util.HashMap<String, Integer> devices;
	private x10Manager tech;

	public x10Listener(x10Manager Tech) {
		devices = new java.util.HashMap<String, Integer>();
		tech = Tech;
	}

	public void addDevice(String address) {
		devices.put(address, -1);
	}

	public int getStauts(String address) {
		return devices.get(address);
	}

	public void put(String address, int value) {
		devices.put(address, value);
	}

	public void clear() {
		devices.clear();
	}

	public void allLightsOff(UnitEvent event) {
		Object[] list = devices.keySet().toArray();
		for (int i = 0; i < list.length; i++) {
			String casa = list[i].toString();
			try {
				if (casa.charAt(2) == 'L') {
					devices.put(casa, 0);
					tech.launchLinkedService(String.valueOf(casa.charAt(0)) + casa.charAt(1), "LIGHT",
							"ALL LIGHTS OFF");
				}
			} catch (IndexOutOfBoundsException ecc) {
			}
		}
	}

	public void allLightsOn(UnitEvent event) {
		Object[] list = devices.keySet().toArray();
		for (int i = 0; i < list.length; i++) {
			String casa = list[i].toString();
			try {
				if (casa.charAt(2) == 'L') {
					devices.put(casa, 100);
					tech.launchLinkedService(String.valueOf(casa.charAt(0)) + casa.charAt(1), "LIGHT", "ALL LIGHTS ON");
				}
			} catch (IndexOutOfBoundsException ecc) {
			}
		}
	}

	public void allUnitsOff(UnitEvent event) {
		Object[] list = devices.keySet().toArray();
		for (int i = 0; i < list.length; i++) {
			devices.put(list[i].toString(), 0);
			tech.launchLinkedService(list[1].toString(), "ALL", "ALL UNITS OFF");
		}
	}

	public void unitBright(UnitEvent event) {
		String address = x10.Command.getAddress((byte) event.getCommand().getAddress()) + "L";
		int value = devices.get(address);
		if (value == 0 || value == 100)
			value = 100;
		else
			value++;
		devices.put(x10.Command.getAddress((byte) event.getCommand().getAddress()) + "L", value);
		tech.launchLinkedService(String.valueOf(address.charAt(0)) + address.charAt(1), "LIGHT", "BRIGHT");
	}

	public void unitDim(UnitEvent event) {
		String address = x10.Command.getAddress((byte) event.getCommand().getAddress()) + "L";
		int value = devices.get(address);
		if (value != 0)
			value--;
		else
			value = 100;
		devices.put(x10.Command.getAddress((byte) event.getCommand().getAddress()) + "L", value);
		tech.launchLinkedService(String.valueOf(address.charAt(0)) + address.charAt(1), "LIGHT", "DIM");
	}

	public void unitOff(UnitEvent event) {
		String address = x10.Command.getAddress((byte) event.getCommand().getAddress());
		devices.put(address, 0);
		devices.put(address + "L", 0);
		tech.launchLinkedService(String.valueOf(address.charAt(0)) + address.charAt(1), "ALL", "OFF");

	}

	public void unitOn(UnitEvent event) {
		String address = x10.Command.getAddress((byte) event.getCommand().getAddress());
		devices.put(address, 100);
		devices.put(address + "L", 100);
		tech.launchLinkedService(String.valueOf(address.charAt(0)) + address.charAt(1), "ALL", "ON");
	}

}
