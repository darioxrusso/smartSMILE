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
 *  File: $RCSfile: WakeupMonitorThread.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.logging.Logger;

import com.cidero.util.MrUtil;

/**
 * Thread to periodically monitor a running renderer. An instance of this thread
 * is started every time the TransportState of a renderer transitions to PLAYING
 * (as reported by the UPnP eventing mechanism). The thread is shut down when
 * the state transitions to STOPPED.
 *
 */
public class WakeupMonitorThread implements Runnable {
	private static Logger logger = Logger.getLogger("com.cidero.control");

	private final static int DEFAULT_WAKEUP_MONITORING_PERIOD = 2000;

	MediaController mediaController;
	int monitorPeriodMillisec = DEFAULT_WAKEUP_MONITORING_PERIOD;

	/**
	 * Constructor
	 *
	 * @param monitorPeriodMillisec
	 *            How often to get status
	 */
	public WakeupMonitorThread(MediaController mediaController) {
		this.mediaController = mediaController;
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
		logger.info("WakeupMonitorThread: Running...");

		Thread thisThread = Thread.currentThread();

		long lastTime = System.currentTimeMillis();

		while (monitorThread == thisThread) {
			long currTime = System.currentTimeMillis();

			if ((currTime - lastTime) > (monitorPeriodMillisec + 60000)) {
				logger.info("WakeupMonitorThread: System waking up... restarting");
				logger.info("after 10 sec wait for network connectivity");

				MrUtil.sleep(10000);
				mediaController.start();
			}

			lastTime = currTime;

			MrUtil.sleep(monitorPeriodMillisec);
		}

		logger.fine("WakeupMonitorThread: Shutting down... ");
	}

}
