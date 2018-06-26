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

package domoNetWS.techManager;

import java.util.*;

import domoML.domoDevice.*;

/**
 * Translate a domoDeviceId to a tech address (represented as String) and a
 * tech address to a domoDeviceId. This is implemented with two hash table in
 * order to use the key and the value of one as the value and key of the other.
 */
public class DoubleHash {

	/**
	 * Index a domoDeviceId for getting the corresponding address (string).
	 * For each DomoDeviceId correspond an unique device.
	 */
	private static HashMap<DomoDeviceId, String> domoDeviceId2String = new HashMap<DomoDeviceId, String>();

	/**
	 * Index an address for getting the corresponding domoDeviceIds (if more
	 * than one). For each address can correspond more domoDeviceId.
	 */
	private static HashMap<String, List<DomoDeviceId>> string2DomoDeviceId = new HashMap<String, List<DomoDeviceId>>();

	/** The constructor. */
	DoubleHash() {
		// do nothing.
	}

	/**
	 * Add an entry.
	 *
	 * @param domoDeviceId
	 *            The id of the domoDevice (domoML identifier).
	 * @param address
	 *            The real address of the device for the technology.
	 * @param overwrite
	 *            If must be overwritten the address if domoDeviceId already
	 *            exists.
	 */
	public final void add(final DomoDeviceId domoDeviceId, final String address, final boolean overwrite) {
		// write value only if not exists the key and overwrite option
		// is turned false
		if (!(domoDeviceId2String.containsKey(domoDeviceId) && overwrite == false))
			// add the entry to the domoDeviceId2String
			domoDeviceId2String.put(domoDeviceId, address);
		// add the entry to the string2DomoDeviceId. If the string key
		// already
		// exists will be added the domoDeviceId to the list of the
		// corresponding
		// values else new entry will be created.
		List<DomoDeviceId> domoDeviceIdList = string2DomoDeviceId.get(address);
		if (domoDeviceIdList == null)
			// no address already exists
			domoDeviceIdList = new LinkedList<DomoDeviceId>();
		// add the domoDeviceId to the new or to the existing domoDeviceIdList
		domoDeviceIdList.add(domoDeviceId);
		// store values
		string2DomoDeviceId.put(address, domoDeviceIdList);
	}

	public final void add(final DomoDeviceId domoDeviceId, final String address) {
		add(domoDeviceId, address, false);
	}

	/**
	 * Get an address giving a DomoDeviceId
	 *
	 * @param domoDeviceId
	 *            The domoDeviceId.
	 * @return The address of the tech as String.
	 */
	public final String getAddress(final DomoDeviceId domoDeviceId) {
		return domoDeviceId2String.get(domoDeviceId);
	}

	/**
	 * Get a list of DomoDeviceId giving an address
	 *
	 * @param address
	 *            The address of the tech as String.
	 * @return The list of the corresponding DomoDeviceId.
	 */
	public final List<DomoDeviceId> getDomoDeviceId(final String address) {
		return string2DomoDeviceId.get(address);
	}

	/**
	 * Get a set of address contained inside.
	 *
	 * @return The set of addresses.
	 */
	public final Set getAddresses() {
		return string2DomoDeviceId.keySet();
	}

	/** Get a set of domoML.domoDevice.DomoDeviceId contained inside. */
	public final Set getDomoDeviceIds() {
		return domoDeviceId2String.keySet();
	}

	/**
	 * Remove a DomoDeviceId from the doubleHash.
	 * 
	 * @param domoDeviceId
	 *            The domoDeviceId to be removed
	 */
	public final void removeDomoDeviceId(final DomoDeviceId domoDeviceId) {
		domoDeviceId2String.remove(domoDeviceId);
		Iterator listOfDomoDeviceIdsIterator = string2DomoDeviceId.values().iterator();
		while (listOfDomoDeviceIdsIterator.hasNext()) {
			List<DomoDeviceId> domoDeviceIds = ((List<DomoDeviceId>) listOfDomoDeviceIdsIterator.next());
			if (domoDeviceIds.contains(domoDeviceId))
				domoDeviceIds.remove(domoDeviceId);
		}
	}

	/**
	 * Remove an Address from the doubleHash.
	 * 
	 * @param address
	 *            The address to be removed
	 */
	public final void removeAddress(final String address) {
		Iterator domoDeviceIdIterator = string2DomoDeviceId.get(address).iterator();
		string2DomoDeviceId.remove(address);
		while (domoDeviceIdIterator.hasNext())
			domoDeviceId2String.remove(((DomoDeviceId) domoDeviceIdIterator.next()));
	}
}
