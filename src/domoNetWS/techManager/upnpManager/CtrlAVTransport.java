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
 *  File: $RCSfile: CtrlAVTransport.java,v $
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

import com.cidero.upnp.AVTransport;

/**
 * 
 */
public class CtrlAVTransport extends AVTransport {
	private static Logger logger = Logger.getLogger("com.cidero.control");

	MediaRendererDevice mediaRenderer;

	/**
	 * Constructor
	 *
	 */
	public CtrlAVTransport(MediaRendererDevice mediaRenderer, Device device) throws InvalidDescriptionException {
		super(device);
		logger.fine("Entered ControllerAVTransport constructor");
		this.mediaRenderer = mediaRenderer;
	}

	/**
	 * Process change event
	 *
	 */
	public void eventTransportState(String value) {
		logger.fine("eventTransportState: Entered - value = " + value);

		if (value.equals("PLAYING"))
			mediaRenderer.startMonitorThread(); // No-op if already running
		else if (value.equals("STOPPED"))
			mediaRenderer.stopMonitorThread(); // No-op if already stopped

		mediaRenderer.getStateModel().setTransportState(value);
	}

	/**
	 * Handle TransportStatus event
	 *
	 * Note - after display of an image, DLink DSM-320 reports TransportStatus
	 * of 'STATUS_MEDIA_END'. The TransportState never switches to 'STOPPED'.
	 */
	public void eventTransportStatus(String value) {
		logger.fine("eventTransportStatus Entered - value = " + value);

		mediaRenderer.getStateModel().setTransportStatus(value);
	}

	public void eventCurrentPlayMode(String value) {
		logger.fine("eventCurrentPlayMode: Entered - value = " + value);
		mediaRenderer.getStateModel().setPlayMode(value);
	}

	/**
	 * Special event that is fired at end of all sub-events contained in a
	 * LastChange event. Notify all observers of state model that something has
	 * changed
	 */
	public void eventLastChangeEnd() {
		mediaRenderer.getStateModel().notifyObservers();
	}

}
