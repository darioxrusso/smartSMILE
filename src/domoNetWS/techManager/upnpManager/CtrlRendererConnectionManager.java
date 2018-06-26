/*
 *  Copyright (C) 2004 Cidero, Inc.
 *
 *  Permission is hereby granted to any person obtaining a copy of 
 *  this software to use, copy, modify, merge, publish, and distribute
 *  the software for any non-commercial purpose, subject to the
 *  following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 *  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY IN CONNECTION WITH THE SOFTWARE.
 * 
 *  File: $RCSfile: CtrlRendererConnectionManager.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.device.InvalidDescriptionException;

import com.cidero.upnp.ConnectionManager;

/**
 * 
 */
public class CtrlRendererConnectionManager extends ConnectionManager {
	private static Logger logger = Logger.getLogger("com.cidero.control");

	MediaRendererDevice mediaRenderer;

	/**
	 * Constructor
	 */
	public CtrlRendererConnectionManager(MediaRendererDevice mediaRenderer, Device device)
			throws InvalidDescriptionException {
		super(device);
		logger.fine("Entered CtrlRendererConnectionManager constructor");
		this.mediaRenderer = mediaRenderer;
	}

	/**
	 * Process change events
	 */
	public void eventSourceProtocolInfo(String value) {
		logger.fine("eventSourceProtocolInfo: Entered - value = " + value);
	}

	public void eventSinkProtocolInfo(String value) {
		logger.fine("eventSinkProtocolInfo: Entered - value = " + value);

		if (mediaRenderer.getExtraSinkProtocolInfo() != null)
			value = value + "," + mediaRenderer.getExtraSinkProtocolInfo();

		// logger.info("!!!!!!!!!!!!eventSinkProtocolInfo: Entered - final value
		// = " + value );

		// Parse the comma-separated list of protocols and store in
		// renderer object for repeated use
		mediaRenderer.setProtocolInfoList(value);
	}

}
