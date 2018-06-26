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

import common.Debug;

/**
 * Implement an exception raised when happend something in the
 * KNXManagerConfigurationSaxParser class that doesn't permit to create a device
 * correctly.
 */
public class ImportDeviceException extends Exception {

	/**
	 * Shows the error
	 * 
	 * @param error
	 *            the error to display
	 */
	public ImportDeviceException(final String error) {
		Debug.getInstance().writeln(error);
	}
}
