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
 *  File: $RCSfile: DeviceSearchThread.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.logging.Logger;

import com.cidero.util.MrUtil;

/**
 * Thread to periodically issue UPnP root device search requests. This is done
 * to monitor a running renderer. An instance of this thread is started every
 * time the TransportState of a renderer transitions to PLAYING (as reported by
 * the UPnP eventing mechanism). The thread is shut down when the state
 * transitions to STOPPED.
 *
 */
public class DeviceSearchThread implements Runnable {
	private static Logger logger = Logger.getLogger("com.cidero.control");

	MediaController mediaController;
	int searchPeriodSec;
	int nRequestsPerPeriod;

	/**
	 * Constructor
	 */
	public DeviceSearchThread(MediaController mediaController, int searchPeriodSec, int nRequestsPerPeriod) {
		this.mediaController = mediaController;
		this.searchPeriodSec = searchPeriodSec;
		this.nRequestsPerPeriod = nRequestsPerPeriod;
	}

	private Thread searchThread = null; // for clean shutdown via stop()

	public void start() {
		searchThread = new Thread(this);
		searchThread.start();
	}

	public void stop() {
		searchThread = null;
	}

	public void run() {
		logger.fine("DeviceSearchThread: Running...");

		Thread thisThread = Thread.currentThread();

		while (searchThread == thisThread) {
			for (int n = 0; n < nRequestsPerPeriod; n++) {
				mediaController.search();

				// Issue search requests spaced 4 seconds apart.
				// The devices are given 3 seconds to respond (MX=3), so the 4
				// seconds should be enough to prevent overlapping responses
				// (though that wouldn't necessarily break anything)
				//
				MrUtil.sleep(4000);
			}

			MrUtil.sleep(searchPeriodSec * 1000 - 4000 * nRequestsPerPeriod);
		}

		logger.fine("DeviceSearchThread: Shutting down... ");
	}

}
