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
 *  File: $RCSfile: MediaController.java,v $
 *
 */

// modified by Dario Russo
// package com.cidero.control;
package domoNetWS.techManager.upnpManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Properties;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.cybergarage.http.*;
import org.cybergarage.upnp.*;
import org.cybergarage.upnp.device.*;
import org.cybergarage.upnp.event.*;
import org.cybergarage.upnp.ssdp.SSDPPacket;

import com.ha.common.windows.*;

// added by Dario Russo
import com.cidero.control.*;
import com.cidero.proxy.*;
import com.cidero.util.MrUtil;
import com.cidero.util.NetUtil;
import com.cidero.util.URLUtil;
// modified by Dario Russo
// import com.cidero.util.AppPreferences;
import com.cidero.upnp.*;

// added by Dario Russo
import domoML.domoMessage.*;
import domoML.domoDevice.*;

/**
 * Main UPnP Media Controller class. Discovers UPnP MediaServer and
 * MediaRenderer devices on a network and interacts with them via a Swing-based
 * GUI
 */
public class MediaController extends ControlPoint implements NotifyListener, EventListener,
		// commented by Dario Russo
		// SearchResponseListener,
		// DeviceChangeListener,
		SubscriptionChangeListener, StandByRequestListener {
	// deleted by Dario Russo
	// private final static Logger logger =
	// Logger.getLogger("com.cidero.control");

	// private final static String TITLE = "Cidero UPnP A/V Controller";
	// public static int DEFAULT_WIDTH = 820;
	// public final static int DEFAULT_HEIGHT = 480;

	// Use 180 seconds subcription duration for NMPR compatibility
	public static int REQUESTED_SUBSCRIPTION_PERIOD_SEC = 180;

	// Commented by Dario Russo
	// private JFrame frame;
	// private MenuBar menuBar;

	// JPanel avServerPanel;
	// JPanel avRendererPanel;

	// JButton mediaServerButton;

	// List of servers/renderers currently active
	static MediaDeviceList mediaDeviceList;

	// Commented by Dario Russo
	// JPanel serverDevicePanel;
	// RendererDevicePanel rendererDevicePanel;
	// MediaBrowserPanel mediaBrowserPanel;

	// DebugWindow debugWindow;

	// /**
	// * List model for set of debug objects (actions,events,notifications)
	// * in debug window
	// */
	// DefaultListModel debugListModel = new DefaultListModel();
	// static int debugHistorySize = 100;
	// boolean debugAutoFormatXML = true;

	static AppPreferences preferences;

	/**
	 * Proxy server for synchronous playback
	 */
	HTTPProxyServer proxyServer = null;

	/**
	 * Detector for windows standby events (uses JNI and DLL library)
	 */
	StandByDetector standByDetector;

	static MediaController controller = null;

	// Added by Dario Russo
	/** The Media Server to use */
	MediaServerDevice mediaServerDevice = null;

	/** The Media Renderer to use */
	MediaRendererDevice mediaRendererDevice = null;

	public static MediaController getInstance(Device mediaServer, Device mediaRenderer) {
		if (controller == null)
			// modified by Dario Russo
			// controller = new MediaController();
			controller = new MediaController(mediaServer, mediaRenderer);

		return controller;
	}

	/**
	 * Constructor. Private for singleton object
	 */
	private MediaController(Device mediaServer, Device mediaRenderer) {
		loadPreferences();

		createDefaultPlaylistDirs();

		mediaDeviceList = new MediaDeviceList();

		addNotifyListener(this);
		// addSearchResponseListener( this );
		addEventListener(this);
		// addDeviceChangeListener( this );
		addSubscriptionChangeListener(this);

		// Install optional listener for non-notify HTTP requests
		// Use separate listener class here (as opposed to 'this') since
		// base ControlPoint class already implements HTTPRequestListener,
		// and we don't want to override it (TODO may want to change base
		// class instead)
		addHttpRequestListener(new ControllerHTTPRequestListener(this));

		setSubscriptionPeriodSec(preferences.getInt("subscriptionPeriodSec", REQUESTED_SUBSCRIPTION_PERIOD_SEC));

		// Use NMPR mode (enables auto-resubscribe in CyberLink)
		setNMPRMode(true);

		// If configured to run a synchronized proxy server in the same
		// JVM as the controller, start it up.
		startLocalProxyServer();

		// If windows, run standby detector
		// Also run simple monitoring thread that detects the waking up
		// condition
		// by looking for time jumps
		//

		if (isRunningOnWindows() && mediaRendererDevice != null && mediaServerDevice != null) {
			System.out.println("Installing Windows standby mode handler");
			standByDetector = new StandByDetector(this);
			standByDetector.setAllowStandby(true);

			WakeupMonitorThread wakeupMonitorThread = new WakeupMonitorThread(this);
			wakeupMonitorThread.start();
		}

		// setting up Devices
		try {
			if (mediaServer != null) {
				deviceAdded(mediaServer);
				mediaServerDevice = new MediaServerDevice(this, mediaServer);
			}

			if (mediaRenderer != null) {
				deviceAdded(mediaRenderer);
				mediaRendererDevice = new MediaRendererDevice(this, mediaRenderer);
			}
		} catch (InvalidDescriptionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load MediaController preferences. Preference information comes from 2
	 * sources - the default set of preferences that come with the program, and
	 * the (optional) user-specific set stored in the user's home directory. The
	 * default set is located in the Java classpath under the 'properties'
	 * subdirectory. The user-specific set is stored in the user's home
	 * directory, under the '.cidero' subdirectory
	 */
	public static void loadPreferences() {
		// Load shared & user-specific preferences for this application
		// modified by Dario Russo
		// preferences = new AppPreferences(".cidero");
		preferences = new AppPreferences("src/domoNetWS/techManager/upnpManager/cidero");

		if (!preferences.load("MediaController", "MediaController")) {
			System.out.println("Missing preferences file - exiting");
			System.exit(-1);
		}
	}

	public void savePreferences() {
		preferences.saveUserPreferences("MediaController", "MediaController");
	}

	public static AppPreferences getPreferences() {
		if (preferences == null)
			loadPreferences();

		return preferences;
	}

	public void createDefaultPlaylistDirs() {
		String userHome = System.getProperty("user.home");
		File xmlDir = new File(userHome + "/.cidero/MediaController/playlists/xml");
		if (!xmlDir.exists()) {
			if (!xmlDir.mkdirs()) {
				System.out.println("Error creating default playlist directory" + xmlDir.getName());
			}
		}

		File m3uDir = new File(userHome + "/.cidero/MediaController/playlists/m3u");
		if (!m3uDir.exists()) {
			if (!m3uDir.mkdirs()) {
				System.out.println("Error creating default playlist directory" + m3uDir.getName());
			}
		}
	}

	public void startLocalProxyServer() {
		if (proxyServer != null) {
			proxyServer.stop();
			proxyServer = null;
		}

		String syncProxyType = preferences.get("syncProxyType", "local");

		System.out.println("Proxy server type is '" + syncProxyType + "'");

		if (syncProxyType.equals("local")) {
			int syncProxyPort = preferences.getInt("syncProxyPort", 18081);
			int syncProxyWaitMs = preferences.getInt("syncProxyWaitMs", 2000);

			System.out.println("(Re)Starting sync proxy server on local host, port: " + syncProxyPort
					+ " syncProxyWaitMs: " + syncProxyWaitMs);
			try {
				// Start set of server threads (one per network interface)
				proxyServer = new HTTPProxyServer(syncProxyPort, syncProxyWaitMs);
				proxyServer.start();
			} catch (IOException e) {
				System.out.println("Failure starting synchronous proxy server: " + e);
			}
		}
	}

	public void stopLocalProxyServer() {
		if (proxyServer != null) {
			System.out.println("Stopping local proxy server");
			proxyServer.stop();
			proxyServer = null;
		}
	}

	public static MediaDeviceList getMediaDeviceList() {
		return mediaDeviceList;
	}

	/**
	 * This callback is invoked whenever CLink discovers a new device
	 *
	 */
	public void deviceAdded(final Device dev) {
		System.out.println("Adding device - Type: " + dev.getDeviceType() + " Name: " + dev.getFriendlyName()
				+ " Location: " + dev.getLocation() + " UUID: " + dev.getUDN());

		// One early Sony device advertised itself as a 'ContentDirectory'...

		if ((dev.getDeviceType().indexOf("device:MediaServer") >= 0)
				|| (dev.getDeviceType().indexOf("device:ContentDirectory") >= 0)) {
			addMediaServer(this, dev);
		} else if (dev.getDeviceType().indexOf("device:MediaRenderer") >= 0) {
			addMediaRenderer(this, dev);
		}

		// Check embedded devices
		DeviceList devList = dev.getDeviceList();
		int devCnt = devList.size();

		for (int n = 0; n < devCnt; n++) {
			Device embeddedDev = devList.getDevice(n);

			System.out.println("  Checking embedded device " + n);
			System.out.println(" deviceType: " + embeddedDev.getDeviceType());
			System.out.println(" friendlyName: " + embeddedDev.getFriendlyName());

			// Set location field in embedded device for benefit of URLBase
			// logic
			embeddedDev.setLocation(dev.getLocation());

			if (embeddedDev.getDeviceType().indexOf("MediaServer") >= 0) {
				addMediaServer(this, embeddedDev);
			} else if (embeddedDev.getDeviceType().indexOf("MediaRenderer") >= 0) {
				addMediaRenderer(this, embeddedDev);
			}
		}

		System.out.println(" --------- deviceAdded callback (Leaving) ----------");
		System.out.println("UDN trovato: " + dev.getUDN());
		/*
		 * try { if(mediaServerUDN.equals(dev.getUDN())) { mediaServerDevice =
		 * new MediaServerDevice(this, dev);
		 * System.out.println("------------------------------------");System.out
		 * .println("TROVATO SERVER!"
		 * );System.out.println("------------------------------------"); }
		 * if(mediaRendererUDN.equals(dev.getUDN())) { mediaRendererDevice = new
		 * MediaRendererDevice(this,
		 * dev);System.out.println("------------------------------------");
		 * System.out.println("TROVATO RENDERER!"
		 * );System.out.println("------------------------------------");} }
		 * catch (InvalidDescriptionException e) { e.printStackTrace(); }
		 * if(mediaServerDevice != null && mediaRendererDevice != null) {
		 * System.out.println("------------------------------------");
		 * System.out.println("TROVATI MEDIA RENDERER E SERVER!!!!");
		 * System.out.println("------------------------------------");
		 * mediaRendererDevice.addToPlayQueue(((CDSObject)mediaServerDevice.
		 * searchForMedia(containerId, mediaId).get(0)));
		 * mediaRendererDevice.startPlayback(); }
		 */
	}

	public void addMediaServer(final MediaController controller, final Device dev) {
		System.out.println(" Adding Media Server! ");
		System.out.println(" URLBase: " + dev.getURLBase());

		// Check for content directory service

		ServiceList serviceList = dev.getServiceList();

		Service service;

		int serviceCnt = serviceList.size();

		for (int n = 0; n < serviceCnt; n++) {
			service = serviceList.getService(n);

			System.out.println(" Service Type: " + service.getServiceType());
			System.out.println(" Service Id: " + service.getServiceID());
			System.out.println(" Service Description URL: " + service.getDescriptionURL());
			System.out.println(" Service SCPD URL: " + service.getSCPDURL());
			System.out.println(" Service Control URL: " + service.getControlURL());

			// service.setControlURL("/PhotoServer/" + service.getControlURL()
			// );

			if (service.getServiceType().indexOf("ContentDirectory") >= 0)
				System.out.println(" Matched content director service!!!!");
			else
				System.out.println(" Didn't match content directory service!!!!");
		}
	}

	public void addMediaRenderer(MediaController controller, Device dev) {
		System.out.println(" Adding Media Renderer! ");
		System.out.println(" URLBase: " + dev.getURLBase());

		//
		// Instantiate new MediaRendererDevice. Pass controller parent and
		// low-level device as args. Note that constructor sets up event
		// listeners, so subscribe command (below) shouldn't be issued until
		// *after* this constructor has executed, otherwise some events may
		// arrive too early
		//
		final MediaRendererDevice mediaRenderer;
		try {
			mediaRenderer = new MediaRendererDevice(controller, dev);
			mediaDeviceList.add(mediaRenderer);
		} catch (InvalidDescriptionException e) {
			System.out.println("Invalid device description " + e);
			return;
		}

		// Subscribe to known MediaRenderer services ConnectionManager,
		// AVTransport, and RenderingControl. Some devices may have other
		// vendor-specific services - skip over those
		ServiceList serviceList = dev.getServiceList();
		Service service;
		int serviceCnt = serviceList.size();
		for (int n = 0; n < serviceCnt; n++) {
			service = serviceList.getService(n);

			System.out.println(" Service Type: " + service.getServiceType());
			System.out.println(" Service Id: " + service.getServiceID());
			System.out.println(" Service Description URL: " + service.getDescriptionURL());
			System.out.println(" Service SCPD URL: " + service.getSCPDURL());
			System.out.println(" Service Control URL: " + service.getControlURL());

			if (service.getServiceType().indexOf("ConnectionManager") >= 0
					|| service.getServiceType().indexOf("AVTransport") >= 0
					|| service.getServiceType().indexOf("RenderingControl") >= 0) {
				// OJN - changed this to request a non-infinte duration
				// subscription
				// 180 sec is NMPR 120-sec renew rate + 60 sec leeway
				if (!subscribe(service, getSubscriptionPeriodSec()))
					System.out.println("Error subscribing to service");
			}
		}

	}

	/**
	 * Device removal handler. This method is invoked by CLink whenever CLink
	 * removes a device from it's list of devices, either due to receiving a
	 * 'bye-bye' notificaton from the device, or due to a device timeout.
	 *
	 * In order that the application can clean up, this method is invoked prior
	 * to the destruction of the underlying CLink Device object, so all Device
	 * methods may be invoked here.
	 *
	 * @param dev
	 *            Underlying CLink device
	 */
	public void deviceRemoved(final Device dev) {
		System.out.println("Removing device - Type: " + dev.getDeviceType() + " Name: " + dev.getFriendlyName());

		// If debug enabled, save action in debug object list
		/*
		 * if ( DebugMsg.getEnabled() ) { DebugMsg debugMsg = new DebugMsg(
		 * "CyberLink Event: Device Removed: " + " Type: " + dev.getDeviceType()
		 * + " Name: " + dev.getFriendlyName(), "" ); addDebugObj( debugMsg ); }
		 * 
		 */
		if (dev.getDeviceType().indexOf("MediaServer") >= 0) {
			System.out.println(" Removing Media Server! ");

			// Find server device in list
			MediaDevice mediaServerDevice = mediaDeviceList.getMediaDevice(dev);

			// Remove device from list
			mediaDeviceList.remove(mediaServerDevice);

			mediaServerDevice.destroy();
		} else if (dev.getDeviceType().indexOf("MediaRenderer") >= 0) {
			MediaRendererDevice mediaRenderer = (MediaRendererDevice) mediaDeviceList.getMediaDevice(dev);

			// Remove device from list
			mediaDeviceList.remove(mediaRenderer);

			mediaRenderer.destroy();

			/*
			 * // Update the GUI asynchronously Runnable swingUpdateThread = new
			 * Runnable() { public void run() { // Find renderer device in list
			 * MediaRendererDevice mediaRenderer =
			 * (MediaRendererDevice)mediaDeviceList.getMediaDevice( dev );
			 * 
			 * if( mediaRenderer != null ) rendererDevicePanel.remove(
			 * mediaRenderer );
			 * 
			 * // Remove device from list mediaDeviceList.remove( mediaRenderer
			 * );
			 * 
			 * mediaRenderer.destroy();
			 * 
			 * System.out.println("RUNNING GUI UPDATE!!!!!"); frame.pack(); } };
			 * 
			 * SwingUtilities.invokeLater( swingUpdateThread );
			 */
		}
	}

	/**
	 * Handle an incoming UPnP event. Events for *all* services are funneled to
	 * this routine at the moment. From here they are passed to service-
	 * specific event listener routines (CLink customization)
	 *
	 * A better implementation might be to install EventListener's on a
	 * per-service basis (further Clink mod - TODO)
	 *
	 * @param uuid
	 * @param seq
	 * @param name
	 * @param value
	 */
	public void eventNotifyReceived(String uuid, long seq, String name, String value) {
		// System.out.println("--------------------Event---------------------------");
		// System.out.println("event notify : uuid = " + uuid + ", seq = " +
		// seq + ", name = " + name + ", value =" + value);

		//
		// Dispatch event to service-specific listener (OJN CLink mod).
		//
		Service service = getSubscriberService(uuid);
		if (service == null) {
			System.out.println("Can't find service for event");
		} else {
			System.out.println("Service SID = " + service.getSID());
			service.performEventListener(uuid, seq, name, value);

		}

		// If debug enabled, save action in debug object list
		/*
		 * if ( DebugEvent.getEnabled() ) { DebugEvent debugEvent = new
		 * DebugEvent( service, uuid, seq, name, value ); addDebugObj(
		 * debugEvent ); }
		 */
		// System.out.println("--------------------Event
		// end-----------------------");

	}

	/**
	 * Handle an incoming device notification. This routine is actually invoked
	 * for all packets arriving on the SSDP multicast port. Packets come in
	 * three flavors:
	 *
	 * - Search requests (ssdp:discover)
	 *
	 * - Announce/Alive packets (ssdp:alive)
	 *
	 * - ByeBye packets (ssdp:bye-bye)
	 *
	 */
	public void deviceNotifyReceived(SSDPPacket packet) {
		/*
		 * DebugObj debugObj;
		 * 
		 * // Filter out message based on user settings if ( packet.isDiscover()
		 * ) { if ( ! DebugSearchRequest.getEnabled() ) return;
		 * 
		 * debugObj = new DebugSearchRequest( packet ); addDebugObj( debugObj );
		 * } else if ( packet.isByeBye() ) { if ( !
		 * DebugNotifyMsg.getByeByeEnabled() ) return;
		 * 
		 * debugObj = new DebugNotifyMsg( packet ); addDebugObj( debugObj ); }
		 * else if ( packet.isAlive() ) { if ( !
		 * DebugNotifyMsg.getAliveEnabled() ) return;
		 * 
		 * debugObj = new DebugNotifyMsg( packet ); addDebugObj( debugObj ); }
		 */
	}

	/**
	 * Handle an incoming response to a search request. These responses are sent
	 * to the unicast SSDP address, not the multicast address used by the above
	 * messages (hence the use of a separate callback)
	 */
	public void deviceSearchResponseReceived(SSDPPacket packet) {
		/*
		 * DebugSearchResponse response = new DebugSearchResponse( packet );
		 * addDebugObj( response );
		 */
	}

	/**
	 * Handle subscription requests/responses. These callbacks are only intended
	 * to be used to snoop (debug) the underlying subscription activity, which
	 * is managed internally by the CLink UPnP library.
	 * 
	 * In this application, simply create debug objects out of the subscription
	 * request/response packets.
	 */
	public void subscriptionRequestSent(Service service, SubscriptionRequest subReq) {
		/*
		 * DebugSubscriptionRequest request = new DebugSubscriptionRequest(
		 * service, subReq ); addDebugObj( request );
		 */
	}

	public void subscriptionResponseReceived(Service service, SubscriptionResponse subRes) {
		/*
		 * DebugSubscriptionResponse response = new DebugSubscriptionResponse(
		 * service, subRes ); addDebugObj( response );
		 */
	}

	public ArrayList getActiveMediaRenderers() {
		return mediaDeviceList.getActiveMediaRenderers();
	}

	/**
	 * Close down controller.
	 */
	public void close() {
		stop(); // Does a remove of all subscribed services
		System.exit(-1);
	}

	/**
	 * If OS issues a standby request (Windows-only at the moment), check to see
	 * if the controller is actively controlling any playback sessions. If not,
	 * allow the system to go into standby. If controller is 'active', refuse to
	 * be put into standby.
	 *
	 * In standby mode, the UPnP 'alive' messages and service resubscription
	 * messages no longer flow, so it is best to issue a 'stop' to the control
	 * point so other UPnP devices aren't trying to pass events to a control
	 * point that is no longer listening.
	 */
	public void standByRequested() {
		System.out.println("Standby requested");

		if (isActivelyControllingRenderer()) {
			System.out.println("Not allowing standby, controller in use");
			standByDetector.setAllowStandby(false);
		} else {
			System.out.println("Allowing Standby after stopping controller");
			stop();
			standByDetector.setAllowStandby(true);
		}
		MrUtil.sleep(2000);
	}

	public boolean isActivelyControllingRenderer() {
		ArrayList mediaRendererList = getActiveMediaRenderers();

		for (int dev = 0; dev < mediaRendererList.size(); dev++) {
			MediaRendererDevice mediaRenderer = (MediaRendererDevice) mediaRendererList.get(dev);

			if (mediaRenderer.isRunning())
				return true;
		}

		return false;
	}

	static String os = null;

	public static boolean isRunningOnMacOSX() {
		if (os == null) {
			os = System.getProperty("os.name");
		}

		System.out.println("OS = " + os);
		if (os.toLowerCase().indexOf("os x") >= 0)
			return true;

		return false;
	}

	public static boolean isRunningOnWindows() {
		if (os == null) {
			os = System.getProperty("os.name");
		}

		System.out.println("OS = " + os);
		if (os.toLowerCase().indexOf("window") >= 0)
			return true;

		return false;
	}

	public static String getProxyIPAddress() {
		if (preferences.get("syncProxyType", "local").equals("remote")) {
			return preferences.get("syncProxyIPAddress", NetUtil.getDefaultLocalIPAddress());
		} else {
			String addr = NetUtil.getDefaultLocalIPAddress();
			System.out.println("************Returning default local IP of " + addr);
			return addr;
		}
	}

	public MediaServerDevice getMediaServer() {
		return mediaServerDevice;
	}

	public MediaRendererDevice getMediaRenderer() {
		return mediaRendererDevice;
	}

	public void setMediaServer(Device mediaServer) {
		try {
			if (mediaServer != null) {
				deviceAdded(mediaServer);
				mediaServerDevice = new MediaServerDevice(this, mediaServer);
			}
		} catch (InvalidDescriptionException e) {
			e.printStackTrace();
		}
	}

	public void setMediaRenderer(Device mediaRenderer) {
		try {
			if (mediaRenderer != null) {
				deviceAdded(mediaRenderer);
				mediaRendererDevice = new MediaRendererDevice(this, mediaRenderer);
			}
		} catch (InvalidDescriptionException e) {
			e.printStackTrace();
		}
	}

	public ArgumentList execute(DomoMessage domoMessage, Device device) {
		if (domoMessage.getMessage().equals("Play")) {
			return playMediaContent(domoMessage);
		} else if (domoMessage.getMessage().equals("Stop")) {
			return stopMediaContent(domoMessage);
		} else if (domoMessage.getMessage().equals("SetVolume")) {
			return setVolumePercent(domoMessage);
		} else if (domoMessage.getMessage().equals("SetMute")) {
			return setMute(domoMessage);
		} else if (device.getDeviceType().indexOf("device:MediaServer") >= 0) {
			if (domoMessage.getMessage().equals("GetVolume")) {
				return getVolumePercent(domoMessage);
			} else if (domoMessage.getMessage().equals("GetMute")) {
				return getMute(domoMessage);
			} else {
				try {
					MediaServerDevice serverDevice = new MediaServerDevice(controller, device);
					return serverDevice.execute(domoMessage);
				} catch (InvalidDescriptionException e) {
					e.printStackTrace();
				}
			}
		} else if (device.getDeviceType().indexOf("device:MediaRenderer") >= 0) {
			try {
				MediaRendererDevice rendererDevice = new MediaRendererDevice(controller, device);
				return rendererDevice.execute(domoMessage);
			} catch (InvalidDescriptionException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public ArgumentList playMediaContent(DomoMessage domoMessage) {
		ArgumentList argList = new ArgumentList();
		try {
			CDSObject cdsObject = mediaServerDevice.searchForMediaCDSObject(
					domoMessage.getInput("mediaContainerId").getValue(),
					domoMessage.getInput("mediaContentId").getValue());
			// add the media to play
			mediaRendererDevice.addToPlayQueue(cdsObject);
			if (mediaRendererDevice.getPlayQueue() != null && !mediaRendererDevice.getPlayQueue().isEmpty()) {
				mediaRendererDevice.startPlayback();
				argList.add(new Argument("State", "Play"));
				return argList;
			}
		} catch (NoElementFoundException e) {
			e.printStackTrace();
		}
		argList.add(new Argument("State", "FAILURE"));
		return argList;
	}

	public ArgumentList stopMediaContent(DomoMessage domoMessage) {
		mediaRendererDevice.stopPlayback();
		ArgumentList argList = new ArgumentList();
		argList.add(new Argument("State", "Stop"));
		return argList;
	}

	public ArgumentList setVolumePercent(DomoMessage domoMessage) {
		try {
			int volume = new Integer(domoMessage.getInput("DesideredVolume").getValue());
			mediaRendererDevice.actionSetVolume(volume);
			// update the state model
			mediaRendererDevice.getStateModel().setVolume(volume);
		} catch (NoElementFoundException e) {
			e.printStackTrace();
		}
		return getVolumePercent();
	}

	public ArgumentList getVolumePercent(DomoMessage domoMessage) {
		return getVolumePercent();
	}

	public ArgumentList getVolumePercent() {
		int volumePercent = getMediaRenderer().getStateModel().getVolume();
		ArgumentList argList = new ArgumentList();
		argList.add(new Argument("volumePercent", Integer.toString(volumePercent)));
		return argList;
	}

	public ArgumentList setMute(DomoMessage domoMessage) {
		try {
			int mute = new Integer(domoMessage.getInput("DesideredMute").getValue());
			boolean isMute;
			if (mute == 1)
				isMute = true;
			else
				isMute = false;
			mediaRendererDevice.actionSetMute(isMute);
			// update the state model
			mediaRendererDevice.getStateModel().setMute(isMute);
		} catch (NoElementFoundException e) {
			e.printStackTrace();
		}
		return getMute();
	}

	public ArgumentList getMute(DomoMessage domoMessage) {
		return getMute();
	}

	public ArgumentList getMute() {
		boolean isMute = getMediaRenderer().getStateModel().getMute();
		int mute;
		if (isMute)
			mute = 1;
		else
			mute = 0;
		ArgumentList argList = new ArgumentList();
		argList.add(new Argument("mute", Integer.toString(mute)));
		return argList;
	}
}
