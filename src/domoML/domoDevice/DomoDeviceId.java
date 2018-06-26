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

/** Identify an unique domoDevice by url and a number. */
public class DomoDeviceId {

	/** The url of the domo device. */
	private String url;

	/** The id of the device for that url. */
	private String id;

	/**
	 * Constructor of the identification.
	 * 
	 * @param url
	 *            The url of the web services.
	 * @param id
	 *            The id of the device inside the web services.
	 */
	public DomoDeviceId(final String url, final String id) {
		setUrl(url);
		setId(id);
	}

	/**
	 * Get the url of the device.
	 *
	 * @return Returns the url.
	 */
	public final String getUrl() {
		return url;
	}

	/**
	 * Set the url of the device.
	 *
	 * @param url
	 *            The url to set.
	 */
	public final void setUrl(final String url) {
		this.url = url;
	}

	/**
	 * Get the id of the device for that url.
	 *
	 * @return Returns the id.
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Set the id of the device for that url.
	 *
	 * @param id
	 *            The id to set.
	 */
	public final void setId(final String id) {
		this.id = id;
	}

	/**
	 * /override Reimplemented method.
	 * 
	 * @param obj
	 *            The object to check.
	 * @return true if url and id are equals, false otherwise.
	 */
	public final boolean equals(final Object obj) {
		if (obj != null && (obj.getClass().equals(this.getClass()))) {
			DomoDeviceId d = (DomoDeviceId) obj;
			return (getUrl().equals(d.getUrl())) && (getId().equals(d.getId()));
		}
		return false;
	}

	/**
	 * /override Calculate the hash code.
	 * 
	 * @return the hash code.
	 */
	public final int hashCode() {
		int hash = 1;
		hash = hash * 31 + getId().hashCode();
		hash = hash * 31 + (getUrl() == null ? 0 : getId().hashCode());
		return hash;
	}

	public String toString() {
		return getUrl() + "@" + getId();
	}
}
