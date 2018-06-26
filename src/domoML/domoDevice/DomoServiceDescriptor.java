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

/**
 * This class extends DomoDeviceId with a description property. This class has a
 * toString method that return a description of the device so it's possible to
 * have an more human identification.
 */
public class DomoServiceDescriptor {

	/** The domoDevice that refers the service */
	private DomoDevice domoDevice;

	/** The service to be descripted */
	private DomoDeviceService domoService;

	/**
	 * Constructor of the identification with description.
	 * 
	 * @param url
	 *            The url of the web services.
	 * @param id
	 *            The id of the device inside the web services.
	 * @param description
	 *            The type and description of the device.
	 */
	public DomoServiceDescriptor(DomoDevice domoDevice, DomoDeviceService domoService) {
		this.domoDevice = domoDevice;
		this.domoService = domoService;
	}

	/**
	 * Gets the value of domoDevice
	 *
	 * @return the value of domoDevice
	 */
	public final DomoDevice getDomoDevice() {
		return this.domoDevice;
	}

	/**
	 * Sets the value of domoDevice
	 *
	 * @param argDomoDevice
	 *            Value to assign to this.domoDevice
	 */
	public final void setDomoDevice(final DomoDevice argDomoDevice) {
		this.domoDevice = argDomoDevice;
	}

	/**
	 * Gets the value of domoService
	 *
	 * @return the value of domoService
	 */
	public final DomoDeviceService getDomoService() {
		return this.domoService;
	}

	/**
	 * Sets the value of domoService
	 *
	 * @param argDomoService
	 *            Value to assign to this.domoService
	 */
	public final void setDomoService(final DomoDeviceService argDomoService) {
		this.domoService = argDomoService;
	}

	/** The same of getDescription(). */
	public String toString() {
		return domoService.getPrettyName();
	}
}
