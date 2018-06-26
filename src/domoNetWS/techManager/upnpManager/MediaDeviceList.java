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
 *  File: $RCSfile: MediaDeviceList.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.*;
import java.util.logging.Logger;

import org.cybergarage.upnp.*;
//import org.cybergarage.upnp.device.*;

import com.cidero.control.*;

/**
 * List class for media devices currently known by controller
 */
public class MediaDeviceList extends Observable {
	private final static Logger logger = Logger.getLogger("com.cidero.control");

	Vector devList = new Vector();

	public MediaDeviceList() {
	}

	/**
	 * Add device to list, and notify all obervers
	 */
	public void add(MediaDevice dev) {
		devList.add(dev);
		setChanged();
		notifyObservers();
	}

	/**
	 * Remove device from list, and notify all obervers
	 */
	public void remove(MediaDevice dev) {
		devList.remove(dev);
		setChanged();
		notifyObservers();
	}

	public MediaDevice getMediaDevice(int n) {
		return (MediaDevice) devList.get(n);
	}

	public int size() {
		return devList.size();
	}

	/**
	 * Find high-level media device that contains the specified lower-level UPNP
	 * device, based on matching friendly name.
	 *
	 * TODO: Add USN field to match criteria
	 *
	 * @return Media device with matching friendly name, or null if no such
	 *         device is present in list
	 */
	public MediaDevice getMediaDevice(Device device) {
		MediaDevice mediaDevice;

		for (int n = 0; n < size(); n++) {
			mediaDevice = getMediaDevice(n);

			Device dev = mediaDevice.getDevice();

			//
			// Use friendly name as match criteria for now - will need refining
			// (TODO)
			//
			if (dev.getFriendlyName().equals(device.getFriendlyName())) {
				logger.fine("MediaDeviceList.get: Found match for " + device.getFriendlyName());

				return mediaDevice;
			}
		}

		logger.fine("MediaDeviceList.get: No match for " + device.getFriendlyName() + "!!!");
		return null;
	}

	/**
	 * TODO: Add checking for device type here...
	 */
	public MediaServerDevice getMediaServerByUUID(String uuid) {
		return (MediaServerDevice) getMediaDeviceByUUID(uuid);
	}

	public MediaRendererDevice getMediaRendererByUUID(String uuid) {
		return (MediaRendererDevice) getMediaDeviceByUUID(uuid);
	}

	public MediaDevice getMediaDeviceByUUID(String uuid) {
		MediaDevice mediaDevice;

		for (int n = 0; n < size(); n++) {
			mediaDevice = getMediaDevice(n);

			Device dev = mediaDevice.getDevice();

			//
			// Use friendly name as match criteria for now - will need refining
			// (TODO)
			//
			if (uuid.equalsIgnoreCase(dev.getUUID())) {
				logger.fine("MediaDeviceList.get: Found match for UUID: " + uuid);
				return mediaDevice;
			}
		}

		logger.fine("MediaDeviceList.get: No match for UUID: " + uuid);

		return null;
	}

	/**
	 * Get a list of active media renderers.
	 *
	 */
	public ArrayList getActiveMediaRenderers() {
		ArrayList rendererList = new ArrayList();
		MediaDevice mediaDevice;
		for (int n = 0; n < size(); n++) {
			mediaDevice = getMediaDevice(n);

			if ((mediaDevice.getDeviceType() == MediaDevice.RENDERER) && mediaDevice.isEnabled()) {
				rendererList.add(mediaDevice);
			}
		}
		return rendererList;
	}

}
