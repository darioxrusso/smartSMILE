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
 *  File: $RCSfile: CtrlRenderingControl.java,v $
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

import com.cidero.upnp.RenderingControl;

/**
 * 
 */
public class CtrlRenderingControl extends RenderingControl {
	private static Logger logger = Logger.getLogger("com.cidero.control");

	MediaRendererDevice mediaRenderer;

	/**
	 * Constructor
	 *
	 */
	public CtrlRenderingControl(MediaRendererDevice mediaRenderer, Device device) throws InvalidDescriptionException {
		super(device);
		logger.fine("Entered CtrlRenderingControl constructor");
		this.mediaRenderer = mediaRenderer;
	}

	/**
	 * Process volume change event
	 *
	 */
	public void eventVolume(String value) {
		logger.fine("eventVolume: Entered - Volume = " + value);
		int volume = Integer.parseInt(value);
		mediaRenderer.getStateModel().setVolume(volume);
	}

	public void eventMute(String value) {
		logger.fine("eventMute: Entered - value = " + value);
		mediaRenderer.getStateModel().setMute(value);
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
