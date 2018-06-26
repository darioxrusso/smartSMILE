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

import javax.servlet.http.HttpServlet;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;
import domoML.domoDevice.*;
import domoML.domoMessage.*;
import domoNetWS.*;

/**
 * Generic manager to implement specific techmanager. It was not implemented
 * this class as interface (as it shold be) for performaces reasons.
 */
public abstract class TechManager {

	/** Contains a mapping from domoDeviceId to real device addresses. */
	protected DoubleHash doubleHash = new DoubleHash();

	/** The host of the server that control the tech. */
	private String host;

	/** The port of the server that control the tech. */
	private int port;

	/** The DomoNetWS that control the module. */
	public DomoNetWS domoNetWS;

	/** Base path of the web application. */
	protected final String basePath;

	/**
	 * Constructor used when the other has no sense because is not needed to
	 * configure a techmanager through url and port.
	 *
	 * @param domoNetWS
	 *            The reference of the domoNetWS that links the module.
	 */
	protected TechManager(final DomoNetWS domoNetWS) {
		// set init params
		this("localhost", 0, domoNetWS);
	}

	/**
	 * Constructor.
	 *
	 * @param url
	 *            The URL of the DomoManager.
	 * @param port
	 *            The port of the DomoManager.
	 * @param domoNetWS
	 *            The reference of the domoNetWS that links the module.
	 */
	protected TechManager(final String url, final int port, final DomoNetWS domoNetWS) {
		// set init params
		setHost(url);
		setPort(port);
		setDomoNetWS(domoNetWS);

		HttpServlet servlet = (HttpServlet) MessageContext.getCurrentContext()
				.getProperty(HTTPConstants.MC_HTTP_SERVLET);
		basePath = servlet.getServletContext().getRealPath(".");
	}

	/**
	 * Get the host that control the tech.
	 *
	 * @return Returns the host.
	 */
	public final String getHost() {
		return host;
	}

	/**
	 * Set the host that control the tech.
	 *
	 * @param host
	 *            The host to set.
	 */
	public final void setHost(final String host) {
		this.host = host;
	}

	/**
	 * Get the port of the host that control the tech.
	 *
	 * @return Returns the port.
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * Set the DomoNetWS that controls the module.
	 *
	 * @param domoNetWS
	 *            The DomoNetWS that controls the module.
	 */
	public final void setDomoNetWS(DomoNetWS domoNetWS) {
		this.domoNetWS = domoNetWS;
	}

	/**
	 * Get the DomoNetWS that controls the module.
	 *
	 * @return The DomoNetWS that controls the module.
	 */
	public final DomoNetWS getDomoNetWS() {
		return domoNetWS;
	}

	/**
	 * Set the port of the host that control the tech.
	 *
	 * @param port
	 *            The port to set.
	 */
	public final void setPort(final int port) {
		this.port = port;
	}

	/**
	 * Write a message on the debug window.
	 *
	 * @param message
	 *            The message to display.
	 * @param retCarr
	 *            true if new line is requested, false otherwise.
	 */
	private final void writeToDebug(final String message, final boolean retCarr) {
		System.out.print(message);
		if (retCarr)
			System.out.print("\n");
	}

	/**
	 * Write a message on the debug window.
	 *
	 * @param message
	 *            The message to display followed by new line.
	 */
	private final void writeToDebug(final String message) {
		writeToDebug(message, true);
	}

	/**
	 * Add a new domoDevice to the list of domoDevice in the DomoNetWS.
	 *
	 * @param domoDevice
	 *            The string rappresentation of the device to add.
	 * @param address
	 *            The real address in the manager.
	 */
	public abstract void addDevice(final DomoDevice domoDevice, String address);

	/**
	 * Load a domoDevice already defined.
	 * 
	 * @param domoDevice
	 *            The string rappresentation of the device to add.
	 */
	public abstract void loadDumpedDomoDevice(final DomoDevice domoDevice);

	/** Make the techManager active. */
	public abstract void start();

	/**
	 * Execute a domoML.domoMessage.DomoMessage.
	 *
	 * @param domoMessage
	 *            The message to be executed.
	 *
	 * @return The resulting domoML.DomoMessage.DomoMessage after the execution.
	 */
	public abstract DomoMessage execute(final DomoMessage domoMessage) throws Exception;

	/** Actions to do when the manager is closed. */
	public abstract void finalize();
}
