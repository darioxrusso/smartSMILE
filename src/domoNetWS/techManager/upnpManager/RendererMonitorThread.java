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
 *  File: $RCSfile: RendererMonitorThread.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.logging.Logger;

/**
 * Thread to periodically monitor a running renderer. An instance of this thread
 * is started every time the TransportState of a renderer transitions to PLAYING
 * (as reported by the UPnP eventing mechanism). The thread is shut down when
 * the state transitions to STOPPED.
 *
 */
public class RendererMonitorThread implements Runnable {
	private static Logger logger = Logger.getLogger("com.cidero.control");

	private final static int STATUS_REQUEST_TIMEOUT = 2000;

	MediaRendererDevice mediaRenderer; // parent
	int monitorPeriodMillisec;

	/**
	 * Constructor
	 *
	 * @param monitorPeriodMillisec
	 *            How often to get status
	 */
	public RendererMonitorThread(MediaRendererDevice mediaRenderer, int monitorPeriodMillisec) {
		this.mediaRenderer = mediaRenderer;
		this.monitorPeriodMillisec = monitorPeriodMillisec;
	}

	private Thread monitorThread = null; // for clean shutdown via stop()

	public void start() {
		monitorThread = new Thread(this);
		monitorThread.start();
	}

	public void stop() {
		monitorThread = null;
	}

	public void run() {
		logger.fine("RendererMonitorThread: Running...");

		Thread thisThread = Thread.currentThread();

		int consecFailCount = 0;

		while (monitorThread == thisThread) {
			logger.fine("RendererMonitorThread: Calling GetPositionInfo ");

			// Periodically call GetPositionInfo to update the playback time
			// If it fails 10 times in a row, bail out
			if (mediaRenderer.actionGetPositionInfo() == false)
				consecFailCount++;
			else
				consecFailCount = 0;

			if (consecFailCount >= 10) {
				logger.warning(
						"Number of consecutive failures > 10 - stopping monitor thread (wireless connectivity problem?)");
				break;
			}

			try {
				Thread.sleep(monitorPeriodMillisec);
			} catch (InterruptedException e) {
			}
		}

		logger.fine("RendererMonitorThread: Shutting down... ");
	}

}
