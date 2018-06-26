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
 *  File: $RCSfile: MediaRendererDevice.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.ArrayList;
import java.util.logging.Logger;

// added by Dario Russo
import java.util.Iterator;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;

import org.cybergarage.xml.XML;
import org.cybergarage.http.HTTPServerList;
import org.cybergarage.upnp.*;
import org.cybergarage.upnp.event.EventListener;
import org.cybergarage.upnp.device.InvalidDescriptionException;

import com.cidero.util.*;
import com.cidero.http.*;
import com.cidero.upnp.*;
import com.cidero.control.*;

import domoML.domoMessage.*;

/**
 * Class used by control app to hold info for a MediaRenderer device Handles
 * outgoing UPnP requests, and receives incoming UPnP events
 */
public class MediaRendererDevice extends MediaDevice implements ActionListener {
	private final static Logger logger = Logger.getLogger("com.cidero.control");

	// These helper classes for each service are instantiated here
	// for the benefit of their service-specific errorToString() methods
	// TODO: It would be cool to integrate the helper classes into the
	// Cybergarage stuff at some point
	CtrlRendererConnectionManager connectionManager;
	CtrlAVTransport avTransport;
	CtrlRenderingControl renderingControl;

	// RendererControlWindow controlWindow = null;

	String connectionIds = "";

	// Model containing renderer state info (volume,mute,currTrack)
	RendererStateModel stateModel;

	// State model that reflects 'pseudo' state of device. This is used
	// primarily useful for handling of picture slideshows, where the
	// slideshow may be considered to be in a continual 'PLAYING' state
	// even though the device itself is reporting a 'STOPPED' state between
	// slides.
	RendererStateModel pseudoStateModel;

	AVConnectionInfo connectionInfo = new AVConnectionInfo(0, 0, 0);
	AVConnectionInfo defaultConnectionInfo = new AVConnectionInfo(0, 0, 0);

	// Flag to help handle devices that don't behave well when sent UTF-8
	// multi-byte char sequences.
	boolean suppressUTF8MultiByteChars = false;

	boolean avTransportEventsSupported = true;

	boolean setNextAVTransportURIEnabled = true;

	// Some devices actually support more protocols than specified in their
	// source/sink protocol info strings
	String extraSinkProtocolInfo = null;

	// Threshold at which to prefer transcoded PCM over WMA (presumably
	// lossless) if device doesn't support lossless. Default it to assume
	// lossless *is* supported by device, so threshold is disabled.
	int losslessWMATranscodeThresh = -1;

	// Play queues - these are the data models for the audio/image/video queue
	// panels at bottom of renderer controller window
	PlayQueue playQueue;

	//
	// List of recently used play queues. This is used to restore play queues
	// to devices after a controller stop/restart
	//
	static ArrayList savedPlayQueueList = new ArrayList();

	ImageQueue imageQueue = new ImageQueue();

	AppPreferences pref;

	// Playback thread objects
	PlayThread playThread; // For Audio/Video playback
	ImagePlayThread imagePlayThread; // For Image playback

	// Each renderer can act as a 'master' for a number of other renderers.
	// The play controls of the master affect all the slaves
	ArrayList slaveRendererList = new ArrayList();

	// Back reference to 'master' for slave renderers. null if no master
	MediaRendererDevice masterRenderer = null;

	/**
	 * Constructor
	 */
	public MediaRendererDevice(MediaController controller, Device device) throws InvalidDescriptionException {
		super(controller, device);

		processPreferences();

		stateModel = new RendererStateModel();
		pseudoStateModel = new RendererStateModel();

		// Instantiate service 'helper' objects for this device type
		avTransport = new CtrlAVTransport(this, device);
		connectionManager = new CtrlRendererConnectionManager(this, device);
		renderingControl = new CtrlRenderingControl(this, device);

		// playQueue = getMatchingSavedPlayQueue( getFriendlyName() );
		if (playQueue == null)
			playQueue = new PlayQueue(getFriendlyName());

		// Create playback thread objects (underlying threads created/destroyed
		// as needed using these objects)
		playThread = new PlayThread(this);
		// imagePlayThread = new ImagePlayThread( this );
	}

	public void destroy() {
		/*
		 * if( controlWindow != null ) { controlWindow.dispose(); controlWindow
		 * = null; }
		 */
		savedPlayQueueList.add(playQueue);
	}

	/**
	 * TODO: Should be a UUID version of this routine, not only a friendly-name
	 * version.
	 */
	public PlayQueue getMatchingSavedPlayQueue(String friendlyName) {
		for (int n = 0; n < savedPlayQueueList.size(); n++) {
			PlayQueue savedQueue = (PlayQueue) savedPlayQueueList.get(n);
			if (savedQueue.getFriendlyName().equals(friendlyName)) {
				savedPlayQueueList.remove(savedQueue);
				return savedQueue;
			}
		}
		return null;
	}

	public void processPreferences() {
		super.processPreferences(); // Process common pref for server/renderer

		pref = MediaController.getPreferences();
		if (pref == null) {
			logger.warning("No pref file found!");
			return;
		}

		//
		// Check for device-specific properties. If device has them, the
		// either the property 'renderer.knownDevice<N>.friendlyNameMatch'
		// or 'renderer.knownDevice<N>.modelNameMatch' will be defined
		//

		String friendlyName = getFriendlyName().toLowerCase();
		String modelName = getModelName();
		if (modelName != null)
			modelName = modelName.toLowerCase();

		String propFilePrefix = getDeviceTypeString().toLowerCase();

		int n;
		String baseName = null;

		for (n = 0; n < MAX_DEVICES; n++) {
			baseName = propFilePrefix + ".knownDevice" + n + ".";

			// First try using modelName;
			String nameMatch = pref.get(baseName + "modelNameMatch");
			if (nameMatch != null) {
				nameMatch = nameMatch.toLowerCase();
				if ((modelName != null) && modelName.indexOf(nameMatch) >= 0)
					break; // match found
			}

			nameMatch = pref.get(baseName + "friendlyNameMatch");
			if (nameMatch != null) {
				nameMatch = nameMatch.toLowerCase();
				if (friendlyName.indexOf(nameMatch) >= 0)
					break; // match found
			}

			// TODO: Add device IPAddr matching logic here to handle
			// multiple-device case ?
			// String ipAddrMatch = pref.get( baseName + "ipAddrMatch" );
		}

		if (n < MAX_DEVICES) // match found?
		{
			String p = pref.get(baseName + "defaultConnectionID");
			if (p != null)
				defaultConnectionInfo.setConnectionID(Integer.parseInt(p));

			p = pref.get(baseName + "defaultAVTransportID");
			if (p != null)
				defaultConnectionInfo.setAVTransportID(Integer.parseInt(p));

			p = pref.get(baseName + "defaultRenderingControlID");
			if (p != null)
				defaultConnectionInfo.setRenderingControlID(Integer.parseInt(p));

			suppressUTF8MultiByteChars = pref.getBoolean(baseName + "suppressUTF8MultiByteChars", false);

			avTransportEventsSupported = pref.getBoolean(baseName + "avTransportEventsSupported", true);

			setNextAVTransportURIEnabled = pref.getBoolean(baseName + "setNextAVTransportURIEnabled", true);

			p = pref.get(baseName + "extraSinkProtocolInfo");
			if (p != null) {
				extraSinkProtocolInfo = p;
				logger.fine("Extra SinkProtocolInfo = " + extraSinkProtocolInfo);
			}

			losslessWMATranscodeThresh = pref.getInt(baseName + "losslessWMATranscodeThreshBitsPerSec", -1);

			logger.fine("losslessWMATranscodeThresh = " + losslessWMATranscodeThresh);
		}
	}

	public void addSlaveRenderer(MediaRendererDevice slaveRenderer) {
		logger.fine("Adding slave renderer '" + slaveRenderer.getFriendlyName() + "' to parent '" + getFriendlyName());

		// Check to make sure it's not already on list
		for (int n = 0; n < slaveRendererList.size(); n++) {
			MediaRendererDevice tmpRenderer = (MediaRendererDevice) slaveRendererList.get(n);

			if (tmpRenderer.getFriendlyName().equals(slaveRenderer.getFriendlyName())) {
				logger.fine("Slave '" + slaveRenderer.getFriendlyName() + "' already on list");
				return;
			}
		}

		slaveRendererList.add(slaveRenderer);
		slaveRenderer.setMasterRenderer(this);

		if (slaveRendererList.size() == 1) {
			// Now a master - select link button
			// getRendererControlWindow().getControlPanel().update();
		}
	}

	public void removeSlaveRenderer(MediaRendererDevice slaveRenderer) {
		logger.fine(
				"Removing slave renderer '" + slaveRenderer.getFriendlyName() + "' from parent '" + getFriendlyName());

		slaveRendererList.remove(slaveRenderer);
		slaveRenderer.setMasterRenderer(null);

		if (slaveRendererList.size() == 0) {
			// No longer a master for any renderers - deselect link button
			// getRendererControlWindow().getControlPanel().update();
		}

	}

	public ArrayList getSlaveRendererList() {
		return slaveRendererList;
	}

	public void setMasterRenderer(MediaRendererDevice masterRenderer) {
		this.masterRenderer = masterRenderer;
	}

	public MediaRendererDevice getMasterRenderer() {
		return masterRenderer;
	}

	public boolean isMaster() {
		if (slaveRendererList.size() >= 1)
			return true;
		else
			return false;
	}

	public boolean isLinked() {
		if ((masterRenderer != null) || (slaveRendererList.size() > 0))
			return true;
		else
			return false;
	}

	/**
	 * Test whether the renderer supports a particular UPnP action. Useful when
	 * building the interface (eliminate non-functional controls)
	 */
	public boolean supportsAction(String serviceType, String actionName) {
		// First check properties for suppressed actions for this particular
		// device (known problematic implementations) TODO: Add support code

		if (avTransport.getServiceType().indexOf(serviceType) >= 0) {
			if (avTransport.getAction(actionName) != null)
				return true;
		} else if (connectionManager.getServiceType().indexOf(serviceType) >= 0) {
			if (connectionManager.getAction(actionName) != null)
				return true;
		} else if (renderingControl.getServiceType().indexOf(serviceType) >= 0) {
			if (renderingControl.getAction(actionName) != null)
				return true;
		}

		return false;
	}

	public int getDeviceType() {
		return MediaDevice.RENDERER;
	}

	public String getDeviceTypeString() {
		return "renderer";
	}

	public void setPlayQueueSelectedIndex(int index) {
		// controlWindow.setPlayQueueSelectedIndex( index );
		playQueue.setCurrentPosition(index);
	}

	public int getPlayQueueSelectedIndex() {
		// return controlWindow.getPlayQueueSelectedIndex();
		return playQueue.getCurrentPosition();
	}

	/*
	 * public void clearPlayQueueSelection() {
	 * controlWindow.clearPlayQueueSelection(); }
	 * 
	 * public int getImageQueueSelectedIndex() { return
	 * controlWindow.getImageQueueSelectedIndex(); } public void
	 * clearImageQueueSelection() { controlWindow.clearImageQueueSelection(); }
	 */
	public RendererStateModel getStateModel() {
		return stateModel;
	}

	public RendererStateModel getPseudoStateModel() {
		return pseudoStateModel;
	}

	public PlayQueue getPlayQueue() {
		return playQueue;
	}

	public ImageQueue getImageQueue() {
		return imageQueue;
	}

	/**
	 * Add a CDS object to the play queue. Normally the object will be subclass
	 * of CDSItem, but there may be some cases (M3U,PLS playlists) where it is a
	 * subclass of CDSContainer
	 */
	public void addToPlayQueue(CDSObject obj) {
		playQueue.insertItem(obj);
		// controlWindow.setPlayQueueView(
		// RendererControlWindow.AUDIO_QUEUE_VIEW );

		// If this was 1st item in previously empty queue, make sure play queue
		// panel on renderer is visible (TODO: make this optional?)
		// if( playQueue.size() == 1 )
		// controlWindow.expandPlayQueuePanel();
	}

	/**
	 * Add a CDSImageItem object to the image queue viewed by the renderer
	 * window.
	 */
	public void addToImageQueue(CDSImageItem obj) {
		imageQueue.add(obj);

		// controlWindow.setPlayQueueView(
		// RendererControlWindow.IMAGE_QUEUE_VIEW );

		// If this was 1st item in previously empty queue, make sure play queue
		// panel on renderer is visible (TODO: make this optional?)

		// if( imageQueue.size() == 1 )
		// controlWindow.expandPlayQueuePanel();
	}

	/**
	 * Handler for button press (select) on a specific Media Renderer If no
	 * renderer control window exists for this renderer, create one. Otherwise,
	 * just bring the existing one to the front.
	 */
	public void actionPerformed(ActionEvent e) {
		logger.finer("BUTTON CLICK - device " + getFriendlyName());
		// getRendererControlWindow().setVisible(true);
	}

	/**
	 * Get current connection IDs. Connection IDs are returned as a
	 * comma-separated list.
	 */
	public synchronized boolean actionGetCurrentConnectionIDs() {
		Action action = connectionManager.getAction("GetCurrentConnectionIDs");
		if (action == null) {
			logger.fine("Action not supported - ignoring");
			return false;
		}

		if (postControlAction(action, connectionManager)) {
			logger.fine("postControlAction (GetCurrentConnectionIDs): Success");

			connectionIds = action.getArgumentValue("ConnectionIDs");

			// Check for broken OmniFi implementation - returns 'Sample String'
			// if( (connectionIds != null) && connectionIds.indexOf("Sample") >=
			// 0 )
			// return false;

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get supported protocol info for device
	 */
	public synchronized boolean actionGetProtocolInfo() {
		Action action = connectionManager.getAction("GetProtocolInfo");
		if (action == null) {
			logger.fine("'GetProtocolInfo' Action not supported - ignoring");
			return false;
		}

		if (postControlAction(action, connectionManager)) {
			logger.fine("postControlAction (GetCurrentConnectionIDs): Success");

			String sinkProtocolInfo = action.getArgumentValue("Sink");

			if (sinkProtocolInfo != null) {
				if (extraSinkProtocolInfo != null)
					sinkProtocolInfo = sinkProtocolInfo + "," + extraSinkProtocolInfo;

				logger.fine("Final SINKPROTO = " + sinkProtocolInfo);

				setProtocolInfoList(sinkProtocolInfo);
			}

			return true;
		} else {
			return false;
		}
	}

	public String getExtraSinkProtocolInfo() {
		return extraSinkProtocolInfo;
	}

	public int getLosslessWMATranscodeThresh() {
		return losslessWMATranscodeThresh;
	}

	public boolean isSetNextAVTransportURIEnabled() {
		return setNextAVTransportURIEnabled;
	}

	/**
	 * Get current connection info for specified connection ID.
	 *
	 */
	public synchronized boolean actionGetCurrentConnectionInfo(int connectionID, AVConnectionInfo connectionInfo) {
		Action action = connectionManager.getAction("GetCurrentConnectionInfo");

		if (action == null) {
			logger.fine("Action not supported - ignoring");
			return false;
		}

		action.setArgumentValue("ConnectionID", Integer.toString(connectionID));

		if (postControlAction(action, connectionManager)) {
			logger.fine("postControlAction (GetCurrentConnectionIDs): Success");

			connectionInfo.setConnectionID(connectionID);
			connectionInfo.setAVTransportID(action.getArgumentIntegerValue("AVTransportID"));
			connectionInfo.setRenderingControlID(action.getArgumentIntegerValue("RcsID"));

			connectionInfo.setProtocolInfo(action.getArgumentValue("ProtocolInfo"));

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check for existence of UPNP PrepareForConnection action. If it exists,
	 * invoke it and get InstanceId for future reference
	 *
	 * Note: Intel NMPR2.0 Guidelines says that device shouldn't implement this,
	 * and 0 should be a valid connectionID. Not the case with the Philips
	 * Streamium devices yet...
	 */
	public synchronized boolean actionPrepareForConnection(String mimeType, AVConnectionInfo connectionInfo) {
		logger.fine("prepareForConnection: Entered, mimeType = " + mimeType);

		Action action = connectionManager.getAction("PrepareForConnection");
		if (action == null) {
			logger.fine("Action not supported - ignoring");
			return false;
		}

		// Set input args

		action.setArgumentValue("RemoteProtocolInfo", mimeType);
		action.setArgumentValue("PeerConnectionManager", "");
		action.setArgumentValue("PeerConnectionID", "-1");
		action.setArgumentValue("Direction", "Input");

		if (postControlAction(action, connectionManager)) {
			logger.fine("postControlAction (prepareForConnection): Success");

			connectionInfo.setProtocolInfo(mimeType);

			connectionInfo.setConnectionID(action.getArgumentIntegerValue("ConnectionID"));
			connectionInfo.setAVTransportID(action.getArgumentIntegerValue("AVTransportID"));
			connectionInfo.setRenderingControlID(action.getArgumentIntegerValue("RcsID"));

			return true;
		} else {
			logger.warning("postControlAction (perpareForConnection): Failure!");
			return false;
		}

	}

	/**
	 * Check for existence of UPNP ConnectionComplete action. If it exists,
	 * invoke it and get InstanceId for future reference
	 *
	 * Note: Intel NMPR2.0 Guidelines says that device shouldn't implement this,
	 * and 0 should be a valid connectionID. Not the case with the Philips
	 * Streamium devices yet...(maybe a *good* thing)
	 */
	public synchronized boolean actionConnectionComplete(int connectionId) {
		Action action = connectionManager.getAction("ConnectionComplete");
		if (action == null) {
			logger.fine("Action not supported - ignoring");
			return false;
		}

		// Set input args
		action.setArgumentValue("ConnectionID", connectionId);

		if (postControlAction(action, connectionManager)) {
			logger.fine("postControlAction (ConnectionComplete): Success");
			return true;
		} else {
			logger.fine("postControlAction (ConnectionComplete): Failure!");
			return false;
		}
	}

	/**
	 * Invoke devices UPNP SetAVTransportURI action
	 */
	public synchronized boolean actionSetAVTransportURI(String uri, String metadata) {
		logger.fine("uri = " + uri);

		if (slaveRendererList.size() > 0) {
			for (int n = 0; n < slaveRendererList.size(); n++) {
				MediaRendererDevice slaveRenderer = (MediaRendererDevice) slaveRendererList.get(n);
				if (!slaveRenderer.actionSetAVTransportURI(uri, metadata)) {
					logger.warning("Failure excuting method for linked slave device");
					return false;
				}
			}
		}

		Action action = avTransport.getAction("SetAVTransportURI");
		if (action == null) {
			logger.fine("couldn't find SetAVTransportURI");
			return false;
		}

		// Set input arguments.
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());
		action.setArgumentValue("CurrentURI", uri);

		if (suppressUTF8MultiByteChars == true)
			action.setArgumentValue("CurrentURIMetaData", UTF8Util.replaceMultiByteChars(metadata, '?'));
		else
			action.setArgumentValue("CurrentURIMetaData", metadata);

		// No returned args for setAVTransportURI
		return postControlAction(action, avTransport);
	}

	/**
	 * Invoke device's UPNP SetNextAVTransportURI action. If the action is not
	 * supported by device, set flag so that subsequent calls are non-op
	 */

	boolean deviceSupportsNextAVTransportURI = true;

	public boolean supportsNextAVTransportURI() {
		return deviceSupportsNextAVTransportURI;
	}

	public synchronized boolean actionSetNextAVTransportURI(String uri, String metadata) {
		/*
		 * logger.info("Setting nextURI disabled! : " + uri + "\n" + metadata );
		 * return false;
		 */

		if (!deviceSupportsNextAVTransportURI)
			return false;

		logger.fine("uri: " + uri + " metadata: " + metadata);

		if (slaveRendererList.size() > 0) {
			for (int n = 0; n < slaveRendererList.size(); n++) {
				MediaRendererDevice slaveRenderer = (MediaRendererDevice) slaveRendererList.get(n);
				if (!slaveRenderer.actionSetNextAVTransportURI(uri, metadata)) {
					logger.warning("Failure excuting method for linked slave device");
					return false;
				}
			}
		}

		Action action = avTransport.getAction("SetNextAVTransportURI");
		if (action == null) {
			logger.warning("Device doesn't support SetNextAVTransportURI " + " (suppressing)");
			deviceSupportsNextAVTransportURI = false;
			return false;
		}

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());
		action.setArgumentValue("NextURI", uri);
		action.setArgumentValue("NextURIMetaData", metadata);

		// logger.info("Setting nextURI: " + uri + "\n" + metadata );

		// No returned args for setNextAVTransportURI
		return postControlAction(action, avTransport);
	}

	/**
	 * Invoke device's UPnP Play action
	 */
	public synchronized boolean actionPlay(String speed) {
		Action action = avTransport.getAction("Play");

		if (action == null) {
			logger.fine("couldn't find Play action");
			return false;
		}

		if (slaveRendererList.size() > 0) {
			for (int n = 0; n < slaveRendererList.size(); n++) {
				MediaRendererDevice slaveRenderer = (MediaRendererDevice) slaveRendererList.get(n);
				if (!slaveRenderer.actionPlay(speed)) {
					logger.warning("Failure excuting method for linked slave device");
					return false;
				}
			}
		}

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());
		action.setArgumentValue("Speed", "1");

		boolean status = postControlAction(action, avTransport);
		if (status == true) {
			if (!avTransportEventsSupported) {
				// simulate transport state event if eventing not supported
				avTransport.eventTransportState(AVTransport.STATE_PLAYING);
				avTransport.eventLastChangeEnd();
			}
		}
		return status;
	}

	public void stopProxySessions() {
		String ipAddr = MediaController.getProxyIPAddress();
		AppPreferences pref = MediaController.getPreferences();
		int port = pref.getInt("syncProxyPort", 18081);

		HTTPConnection connection = new HTTPConnection();
		try {
			URL url = new URL("http://" + ipAddr + ":" + port + "/SyncProxyStop");
			HTTPRequest request = new HTTPRequest(HTTP.GET, url);

			// Override use of keep-alive connection with 'close' header for now
			request.setHeader("Connection", "close");

			HTTPResponse response = connection.sendRequest(request, false);

			if ((response == null) || (response.getStatusCode() != HTTPStatus.OK)) {
				logger.warning("Error stopping proxy sessions");
				return;
			} else {
				logger.info("Success stopping proxy sessions!");
				return;
			}
		} catch (MalformedURLException e) {
			return;
		} catch (IOException e) {
			return;
		}

	}

	/**
	 * Invoke device's UPnP Stop action
	 */
	public synchronized void actionStop() {
		logger.fine("actionStop: Entered");

		Action action = avTransport.getAction("Stop");
		if (action == null) {
			logger.warning("couldn't find Stop action");
			return;
		}

		if (slaveRendererList.size() > 0) {
			// Rokus don't yet close connections when they are stopped, and seem
			// to wait for write attempt on their socket, or broken socket
			// connection
			// before they actually complete the stop command. Stop proxy
			// manually
			// here to make things work - Doesn't yet work quite right
			stopProxySessions();

			logger.fine("Stopping slave devices");
			for (int n = 0; n < slaveRendererList.size(); n++) {
				MediaRendererDevice slaveRenderer = (MediaRendererDevice) slaveRendererList.get(n);
				slaveRenderer.actionStop();
			}

			logger.fine("Stopping parent device");
		}

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());

		boolean status = postControlAction(action, avTransport);

		if (status == true) {
			// logger.info("!!!!!!!!!! Stopped parent device ok");

			if (!avTransportEventsSupported) {
				// simulate transport state event if eventing not supported
				// (temporary hack for SB)
				avTransport.eventTransportState(AVTransport.STATE_STOPPED);
				avTransport.eventLastChangeEnd();
			}
		}

		// No returned args for stop command
	}

	/**
	 * Invoke device's UPnP Pause action
	 */
	public synchronized void actionPause() {
		Action action = avTransport.getAction("Pause");
		if (action == null) {
			logger.warning("couldn't find Pause action");
			return;
		}

		if (slaveRendererList.size() > 0) {
			for (int n = 0; n < slaveRendererList.size(); n++) {
				MediaRendererDevice slaveRenderer = (MediaRendererDevice) slaveRendererList.get(n);
				slaveRenderer.actionPause();
			}
		}

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());

		boolean status = postControlAction(action, avTransport);

		if (status == true) {
			if (!avTransportEventsSupported) {
				// simulate transport state event if eventing not supported
				avTransport.eventTransportState(AVTransport.STATE_PAUSED_PLAYBACK);
				avTransport.eventLastChangeEnd();
			}
		}

		// No returned args for pause command
	}

	/**
	 * Invoke device's UPnP Next action
	 */
	public synchronized void actionNext() {
		Action action = avTransport.getAction("Next");
		if (action == null) {
			logger.warning("couldn't find Next action");
			return;
		}

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());

		postControlAction(action, avTransport);

		// No returned args for pause command
	}

	/**
	 * Invoke device's UPnP Previous action
	 */
	public synchronized void actionPrevious() {
		Action action = avTransport.getAction("Previous");
		if (action == null) {
			logger.warning("couldn't find Previous action");
			return;
		}

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());

		postControlAction(action, avTransport);

		// No returned args for pause command
	}

	/**
	 * Invoke device's UPNP SetPlayMode action
	 */
	public synchronized void actionSetPlayMode(String playMode) {
		Action action = avTransport.getAction("SetPlayMode");
		if (action == null) {
			logger.warning("couldn't find SetPlayMode action");
			return;
		}

		logger.fine("setPlayMode: playMode = " + playMode);

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());
		action.setArgumentValue("NewPlayMode", playMode);

		postControlAction(action, avTransport);
		// No returned args for command
	}

	/**
	 * Invoke device's UPnP SetVolume action
	 */
	public synchronized void actionSetVolume(int volumePercent) {
		Action action = renderingControl.getAction("SetVolume");
		if (action == null) {
			logger.warning("couldn't find SetVolume action");
			return;
		}

		logger.fine("setVolume: volPercent = " + volumePercent);

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getRenderingControlID());
		action.setArgumentValue("Channel", "Master");
		action.setArgumentValue("DesiredVolume", Integer.toString(volumePercent));

		// No returned args for command
		postControlAction(action, renderingControl);
	}

	/**
	 * Invoke device's UPnP SetMute action
	 */
	public synchronized void actionSetMute(boolean mute) {
		Action action = renderingControl.getAction("SetMute");
		if (action == null) {
			logger.warning("couldn't find SetMute action");
			return;
		}

		logger.fine("setMute: mute = " + mute);

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getRenderingControlID());
		action.setArgumentValue("Channel", "Master");
		if (mute == true)
			action.setArgumentValue("DesiredMute", "1");
		else
			action.setArgumentValue("DesiredMute", "0");

		postControlAction(action, renderingControl);

		// No returned args for command
	}

	/**
	 * Invoke device's UPnP X_Rotate action. This action is not in UPnP spec,
	 * but is supported by Philips 300i/400i as of 2/8/2005 to rotate pictures.
	 * Maybe it will become industry standard? (Probably not without adding a
	 * rendering instance ID to the arg list to match up with other actions)
	 *
	 * @param rotation
	 *            '0', '+90', '180', '+270'
	 *
	 */
	public synchronized boolean actionX_Rotate(String rotation) {
		Action action = renderingControl.getAction("X_Rotate");
		if (action == null) {
			logger.warning("couldn't find X_Rotate action");
			return false;
		}

		logger.info("X_Rotate: rotation = " + rotation);

		// Set input arguments
		action.setArgumentValue("Rotation", rotation);

		boolean status = postControlAction(action, renderingControl);
		if (status == true) {
			logger.info("SourceRotated = " + action.getArgumentValue("SourceRotated"));
			return true;
		} else {
			return false;
		}
	}

	long getPositionInfoTimeStampMillis = 0;
	long trackMotionTimeStampMillis = 0;

	public void resetTrackMotionTimeStamps() {
		getPositionInfoTimeStampMillis = System.currentTimeMillis();
		trackMotionTimeStampMillis = getPositionInfoTimeStampMillis;
	}

	public long timeSinceLastTrackMotionMillis() {
		/*
		 * System.out.println("TimeSince: " + (getPositionInfoTimeStampMillis %
		 * 1000000) + "  " + (trackMotionTimeStampMillis % 1000000) );
		 */
		return getPositionInfoTimeStampMillis - trackMotionTimeStampMillis;
	}

	/**
	 * Get current connection info for specified connection ID. Modify the local
	 * renderer state model using the returned information
	 *
	 * @return true if action suceeds, false if not
	 */
	public synchronized boolean actionGetPositionInfo() {
		Action action = avTransport.getAction("GetPositionInfo");
		if (action == null) {
			logger.warning("couldn't find GetPositionInfo action");
			return false;
		}

		// Set input arguments
		action.setArgumentValue("InstanceID", connectionInfo.getAVTransportID());

		boolean status = postControlAction(action);
		/*
		 * DebugAction debugAction = null; if( DebugAction.getEnabled() )
		 * debugAction = new DebugAction( action, avTransport );
		 */
		if (status == true) {
			// Store time of last successful action invocation
			getPositionInfoTimeStampMillis = System.currentTimeMillis();

			// Access return arguments
			String trackNum = action.getArgumentValue("Track");
			String trackDuration = action.getArgumentValue("TrackDuration");
			String trackMetaData = action.getArgumentValue("TrackMetaData");
			String trackURI = action.getArgumentValue("TrackURI");
			String trackRelTime = action.getArgumentValue("RelTime");
			String trackAbsTime = action.getArgumentValue("AbsTime");

			// logger.finest("TrackDuration: " + trackDuration +
			// " RelTime: " + trackRelTime +
			// " TrackURI: " + trackURI +
			// " TrackMetadata: " + trackMetaData );

			stateModel.setTrackNum(trackNum);

			// Check for presence of position args - flag as error if missing
			// (should be done by lower UPnP layer - check why this is not
			// always the case - TODO)
			if (trackDuration == null || trackRelTime == null) {
				/*
				 * if( DebugAction.getEnabled() ) { debugAction.setStatus(
				 * DebugObj.STATUS_WARNING ); debugAction.setErrorCode(
				 * UPnPStatus.ACTION_FAILED ); }
				 */
			} else {
				// Trim off millisec here for convenience
				stateModel.setTrackDurationNoMillisec(trackDuration);

				// Update 'stuck playback' track motion time stamp if track
				// position is updating, or if transport state not playing
				// (don't
				// want to falsely declare a stuck condition when paused)
				// System.out.println("relTime = " + trackRelTime +
				// "oldRelTIme = " + stateModel.getTrackRelTime() );
				if ((!trackRelTime.equals(stateModel.getTrackRelTime()))
						|| (!stateModel.getTransportState().equals(AVTransport.STATE_PLAYING))) {
					trackMotionTimeStampMillis = getPositionInfoTimeStampMillis;
				}

				stateModel.setTrackRelTime(trackRelTime);
			}

			stateModel.setTrackURI(trackURI);
			stateModel.setTrackMetaData(trackMetaData);

			//
			// Some renderers (NOXON) don't respond with valid trackMetaData
			// field, so need to check for that
			//
			if (trackMetaData != null) {
				try {
					// Some renderers escape the XML prolog (<DIDL-Lite>
					// envelope).
					// The Apache XML parser complains about badly formed XML
					// and
					// stops parsing. Hack around this for now, but set warning
					// flag in debug object (assume Apache complaint is valid)
					if (trackMetaData.startsWith("&lt;")) {
						trackMetaData = trackMetaData.replaceAll("&lt;", "<");
						trackMetaData = trackMetaData.replaceAll("&gt;", ">");

						stateModel.setTrackMetaData(trackMetaData);
						/*
						 * if( DebugAction.getEnabled() ) {
						 * debugAction.setStatus( DebugObj.STATUS_WARNING );
						 * debugAction.setErrorCode( AVTransport.BAD_METADATA );
						 * }
						 */
					}

					// Some renderers don't properly escape the '&' character
					// in XML metadata - catch the instances with a following
					// space here (no XML entity references have a space after
					// the '&')
					//
					// Note: both this patch and the one above are quick hacks -
					// make them more robust (TODO)

					int origLength = trackMetaData.length();
					trackMetaData = XMLUtil.escapeNonEntityAmpersands(trackMetaData);
					if (trackMetaData.length() != origLength) {
						stateModel.setTrackMetaData(trackMetaData);
						// if( trackMetaData.indexOf("& ") > 0 )
						// {
						// trackMetaData = trackMetaData.replaceAll("&",
						// "&amp;");
						/*
						 * if( DebugAction.getEnabled() ) {
						 * debugAction.setStatus( DebugObj.STATUS_WARNING );
						 * debugAction.setErrorCode( AVTransport.BAD_METADATA );
						 * }
						 */
					}

					// DSM 520 metadata is missing closing brace in <res>
					// element, i.e.:
					// <res ... size=12345555 http://resourceURL</res>
					// ^ missing '>'
					origLength = trackMetaData.length();
					trackMetaData = trackMetaData.replaceAll(" size=\"[0-9]*\" http://", ">http://");

					if (trackMetaData.length() != origLength) {
						// trackMetaData =
						// trackMetaData.replaceAll("resolution=\"\"", "" );
						stateModel.setTrackMetaData(trackMetaData);
						/*
						 * if( DebugAction.getEnabled() ) {
						 * debugAction.setStatus( DebugObj.STATUS_WARNING );
						 * debugAction.setErrorCode( AVTransport.BAD_METADATA );
						 * }
						 */
					}

					// System.out.println("trackMetaData = " + trackMetaData );
					// This should only return a single object in this case. If
					// more,
					// print warning and default to 1st obj in list
					CDSObjectList objList = new CDSObjectList(trackMetaData);
					if (objList.size() >= 1) {
						if (objList.size() > 1)
							logger.warning("TrackMetadata had multiple objects!\n");

						CDSObject obj = objList.getObject(0);

						//
						// Only overwrite local state model if there is really
						// data,
						// and device properly round-trips UTF-8 characters sent
						// via
						// the setAVTransportURI() action
						//
						boolean preferLocalInfo = false;
						if (suppressUTF8MultiByteChars && playThread.isRunning())
							preferLocalInfo = true;

						// If CDSMusicTrack, try using 'Artist' element before
						// 'creator'
						if (obj instanceof CDSMusicTrack) {
							CDSMusicTrack musicTrack = (CDSMusicTrack) obj;

							if ((musicTrack.getArtist() != null) && !musicTrack.getArtist().equals("")
									&& !preferLocalInfo) {
								stateModel.setTrackArtist(musicTrack.getArtist());
							} else if (obj.getCreator() != null && !obj.getCreator().equals("") && !preferLocalInfo) {
								stateModel.setTrackArtist(obj.getCreator());
							}
						} else {
							if (obj.getCreator() != null && !obj.getCreator().equals("") && !preferLocalInfo)
								stateModel.setTrackArtist(obj.getCreator());
						}

						// Only overwrite local state model if there is really
						// data
						if ((obj.getTitle() != null) && !obj.getTitle().equals("") && !preferLocalInfo)
							stateModel.setTrackTitle(obj.getTitle());
					}
				} catch (UPnPException e) {
					logger.warning("Exception processing track MetaData" + e);
					/*
					 * if( DebugAction.getEnabled() ) { debugAction.setStatus(
					 * DebugObj.STATUS_ERROR ); debugAction.setErrorCode(
					 * AVTransport.BAD_METADATA ); }
					 */
				}
			}

			/*
			 * System.out.println("Before notify Artist, title = " +
			 * stateModel.getTrackArtist() + " " + stateModel.getTrackTitle() +
			 * "\nMetadata: " + trackMetaData );
			 */

			stateModel.notifyObservers();
			/*
			 * if( DebugAction.getEnabled() ) getParentController().addDebugObj(
			 * debugAction );
			 */
		} else {
			logger.warning("postControlAction (GetPositionInfo): Failure!");
			/*
			 * if( DebugAction.getEnabled() ) { debugAction.setStatus(
			 * DebugObj.STATUS_ERROR ); debugAction.setErrorCode(
			 * action.getControlStatus().getCode() );
			 * getParentController().addDebugObj( debugAction ); }
			 */
			return false;
		}

		return true;
	}

	/**
	 * Set service instance ID's to 0's (normally all 0's for NMPR-compliant
	 * device)
	 */
	public void setConnectionInfoToDefault() {
		connectionInfo.setConnectionID(defaultConnectionInfo.getConnectionID());
		connectionInfo.setAVTransportID(defaultConnectionInfo.getAVTransportID());
		connectionInfo.setRenderingControlID(defaultConnectionInfo.getRenderingControlID());
	}

	/**
	 * Do basic connection setup. Logic differs on different renderers, with the
	 * main difference relating to whether device implements the
	 * PrepareForConnection and ConnectionComplete actions
	 */
	public void setupConnection(String protocolInfo) {
		//
		// Check for current connections. If one exists, get the connection
		// info for it and if the protocolInfo (MIME-type) matches, 'share'
		// it. If not, and the PrepareForConnection action is supported,
		// invoke it to set up the connection. If the PrepareForConnection
		// action *doesn't* exist, assume this is an NMPR-compliant device
		// and use all 0's for the ID's.
		//
		if (actionGetCurrentConnectionIDs() == false) {
			logger.warning("GetCurrentConnectionIDs action failed - ids: " + connectionIds);

			// try using default ids (normally all 0's for NMPR-compliant
			// device)
			setConnectionInfoToDefault();

		} else if ((connectionIds == null) || connectionIds.trim().equals("")) {
			// No existing connections. Try to create one if
			// PrepareForConnection
			// action is supported. If not supported, or if PrepareForConnection
			// fails, use default connection ID's of all 0's
			if (actionPrepareForConnection(protocolInfo, connectionInfo) == false) {
				logger.fine("PrepareForConnection failed or unsupported - assuming default id's");
				setConnectionInfoToDefault();
			}
		} else {
			// Connection(s) exist. Get id of first one (most devices support
			// only
			// one).
			String[] tmp = connectionIds.split(",");

			int connectionId;

			if (tmp.length > 0)
				connectionId = Integer.parseInt(tmp[0]);
			else
				connectionId = 0;

			if (actionGetCurrentConnectionInfo(connectionId, connectionInfo) == true) {
				// If connection MIME-type doesn't match current resource, and
				// PrepareForConnection/ConnectionComplete actions are
				// supported,
				// get a new connection for the current MIME-type

				if ((connectionInfo.getProtocolInfo() == null)
						|| !connectionInfo.getProtocolInfo().equals(protocolInfo)) {
					if (actionConnectionComplete(connectionId) == true) {
						if (actionPrepareForConnection(protocolInfo, connectionInfo) == false) {
							setConnectionInfoToDefault();
						}
					}
				}
			} else {
				logger.warning("Couldn't get connectionInfo for id " + connectionId);
				setConnectionInfoToDefault();
			}
		}
	}

	/**
	 * If this is a container object, with child objects that are item objects,
	 * generate a dynamic M3U resource with a special address on the control
	 * point's HTTP server. When the HTTP server sees the dynamic M3U resource,
	 * it generates the playlist on the fly.
	 */
	public boolean addPlaylistResource(CDSObject obj) {
		if (obj.isContainer() && ((CDSContainer) obj).getChildCount() > 0) {
			// Make sure renderer device actually supports M3U mime type! (TODO)

			// Need to add a resource for each IP address served (if running
			// control point on host with multiple network interfaces)
			MediaController controller = getParentController();
			HTTPServerList serverList = controller.getHTTPServerList();

			MediaServerDevice mediaServer = controller.getMediaServer();

			for (int n = 0; n < serverList.size(); n++) {
				CDSResource res = new CDSResource();
				res.setProtocolInfo("http-get:*:audio/x-mpegurl:*");
				res.setName("http://" + serverList.getHostAddress(n) + ":" + serverList.getBindPort(n)
						+ "/dynamic/playlist/" + getUUID() + "/" + mediaServer.getUUID() + "/$" + obj.getId() + ".m3u");

				logger.info("Obj has no M3U resources - adding " + res.getName());
				obj.addResource(res);
			}
		} else {
			if (!obj.isContainer())
				logger.info("Obj is not container");
			else if (((CDSContainer) obj).getChildCount() <= 0)
				logger.info("Obj has no child items");

			return false;
		}

		return true;
	}

	public void setNextAVTransportURI(CDSObject obj) {
		if (obj == null) {
			actionSetNextAVTransportURI("", "");
		} else {
			CDSResource res = obj.getBestMatchingResource(getProtocolInfoList(), null, null,
					losslessWMATranscodeThresh);
			if (res == null) {
				logger.warning("No matching resource for server/renderer combo");
			} else {
				//
				// If resource's network segment is the same as one of the local
				// host's
				// interfaces, but *different* than the renderer's network,
				// substitute the correct interface into the resource.
				//
				String resourceHost = URLUtil.getHost(res.getName());

				if (resourceHost != null) {
					if (NetUtil.isLocalAddr(resourceHost) && !NetUtil.onSameSubnet(resourceHost, getHost())) {
						String subnetHostAddr = NetUtil.findInterfaceAddrOnSameSubnetAs(getHost());

						if (subnetHostAddr != null) {
							CDSResource cloneResource = (CDSResource) res.clone();
							cloneResource.setName(URLUtil.replaceHostAddr(res.getName(), subnetHostAddr));
							res = cloneResource;
						}
					}
				}

				// If acting as master for multiple renderers, use proxy
				// address for resource that points to synchronous server proxy
				if (isMaster()) {
					CDSResource cloneResource = (CDSResource) res.clone();

					String optString = "opt:bitRate=" + res.getEstimatedBitRate();

					cloneResource.setName(URLUtil.urlToProxy(res.getName(), MediaController.getProxyIPAddress(),
							pref.getInt("syncProxyPort", 18081), optString));
					res = cloneResource;
				}

				actionSetNextAVTransportURI(res.getName(), CDS.toDIDL(obj, "*", res));
			}
		}
	}

	/**
	 * Some media renderers (e.g. Intel's MediaRenderer sample) may not properly
	 * echo back the artist/title info when getPositionInfo action is invoked,
	 * even if that info was passed to it via the metadata arg of
	 * SetAVTransportURI() (above). Latch info here for backup display in media
	 * controller's renderer control window
	 */
	public void latchArtistTitleInfo(CDSObject obj) {
		//
		// If object is a music track, look in it's Artist field before
		// the Creator field
		if (obj instanceof CDSMusicTrack) {
			CDSMusicTrack musicTrack = (CDSMusicTrack) obj;
			if (musicTrack.getArtist() != null)
				stateModel.setTrackArtist(musicTrack.getArtist());
			else
				stateModel.setTrackArtist(obj.getCreator());
		} else {
			stateModel.setTrackArtist(obj.getCreator());
		}

		stateModel.setTrackTitle(obj.getTitle());

		logger.finer("Obj: " + obj.toString());

		logger.finer("In setURI, stateModel Artist, title = " + stateModel.getTrackArtist() + " "
				+ stateModel.getTrackTitle());
	}

	/**
	 * Monitor thread routines. Whenever a TransportState event with state
	 * "PLAYING" is received, a monitoring thread is started if one is not
	 * already running. Likewise when a "STOPPED" state is received, the stop
	 * routine is invoked
	 *
	 * The monitoring thread is currently suppressed when in image playback
	 * mode, since there is no notion of 'image position' corresponding to that
	 * for audio and video playback
	 */
	RendererMonitorThread monitorThread = null;

	public void startMonitorThread() {
		// only start monitoring thread if control window has been created
		// for this device (user clicked at least once on renderer icon)
		// (Disabled this test for now since I kind've like the window to pop up
		// if the device is already playing when I start the controller TODO -
		// make this an option)
		//
		/*
		 * if( controlWindow == null ) { logger.fine(
		 * "Not creating monitoring thread (control window inactive)"); return;
		 * }
		 */

		/*
		 * if( getRendererControlWindow().getPlayQueueView() ==
		 * RendererControlWindow.IMAGE_QUEUE_VIEW ) { logger.fine(
		 * "Not creating monitoring thread in image viewing mode"); return; }
		 */
		if (monitorThread == null) {
			monitorThread = new RendererMonitorThread(this, pref.getInt("renderer.monitorIntervalMs", 3000));
			monitorThread.start();
		} else {
			logger.fine("Monitor thread already running!");
		}
	}

	public void stopMonitorThread() {
		if (monitorThread != null) {
			monitorThread.stop();
			monitorThread = null;
		} else {
			logger.fine("Monitor thread already stopped!");
		}
	}

	public boolean isRunning() {
		if ((playThread != null) && playThread.isRunning())
			return true;
		else
			return false;
	}

	/**
	 * Playback routines. When the control point is an active participant in the
	 * scheduling of playback items (when in Jukebox mode, or for renderers that
	 * don't have the capability of playing back playlists on their own) a
	 * separate thread is used for managing the playback queue
	 *
	 * Separate threads are used for audio-video vs image playback since the
	 * logic is a bit different in each case.
	 */

	public PlayThread getPlayThread() {
		return playThread;
	}

	public void startPlayback() {
		if (playThread.isRunning()) {
			// The one case where a play command is issued when the play thread
			// is still running is when a playback is paused. Anything else -
			// print warning (but still issue the command, since the device
			// should be smart enough to reject it if transition to PLAYING is
			// impossible in current state)
			if (!stateModel.getTransportState().equals(AVTransport.STATE_PAUSED_PLAYBACK)) {
				logger.warning("startPlayThread: Play thread already running, and device state not PAUSED ("
						+ stateModel.getTransportState() + ")");
			}

			if (actionPlay("1.0") == false) {
				logger.warning("Error occurred starting item playback - " + " Stopping playback session");
			}
		} else {
			playThread.start();
		}
	}

	public void stopPlayback() {
		// Clear any high-priority flags in audio play queue since they are
		// only needed for inserting prioritized tracks into *running*
		// play queue
		playQueue.clearHighPriorityFlags();

		if (playThread.isRunning()) {
			playThread.stop();
		} else {
			// This can happen if stop issued on Ctrl point that isn't managing
			// the play queue - make message a 'fine' debug later TODO
			logger.info("stopPlayThread called but Play thread not running!");
		}

		actionStop(); // Issue UPnP stop action
	}

	public void pausePlayback() {
		actionPause(); // Just issue UPnP pause action (play action resumes)
	}

	/**
	 * Move to next/prev track. If the control point is operating in 'normal'
	 * mode, then all that needs to be done is to send the UPnP 'Next' or 'Prev'
	 * action to the Media Renderer. If the control point is actively managing
	 * the run queue, then issue a UPnP Stop action. Play thread will see the
	 * transportState change to STOPPED and can change to desired track based on
	 * flags set in these routines.
	 */
	public void next() {
		if (playThread.isRunning()) {
			playThread.setNextFlag(true);
			actionStop();
			// Sleep a bit to slow down multiple buttone-click behaviour
			MrUtil.sleep(1000);
		} else {
			actionNext(); // Just the UPnP action
		}
	}

	public void prev() {
		if (playThread.isRunning()) {
			playThread.setPrevFlag(true);
			actionStop();
			MrUtil.sleep(1000);
		} else {
			actionPrevious(); // Just the UPnP action
		}
	}

	/**
	 * Image playback routines. State transitions are a bit different for image
	 * playback, so separate methods used to avoid tangling up the code.
	 */
	public void startImagePlayback(String playMode, int queueStartIndex) {
		imagePlayThread.start(playMode, queueStartIndex);
	}

	public void stopImagePlayback() {
		logger.fine("Entered:");
		imagePlayThread.stop();
	}

	public void pauseImagePlayback() {
		logger.fine("Entered:");
		imagePlayThread.setPauseFlag(true);
	}

	public ImagePlayThread getImagePlayThread() {
		return imagePlayThread;
	}

	/**
	 * Set play mode. If operating in 'detachable controller' mode, just send
	 * UPnP action to renderer (assume renderer supports it for now). If
	 * operating in 'active controller' mode (repeat mode logic being completely
	 * handled on controller side), set the mode of the control point play
	 * queue.
	 */
	public void setPlayMode(String playMode) {
		// TODO: Right now this is quite hacked up, since the controller is
		// always operating in 'active controller' mode. For REPEAT_ALL,
		// set the play queue mode and never send the action to the device.
		// For REPEAT_ONE, send the command to the device - this is done
		// to enable testing of the SetPlayAction for devices, and will
		// end up having the desired effect if the mode is supported by the
		// device.
		if (playMode.equalsIgnoreCase(AVTransport.PLAY_MODE_REPEAT_ALL)) {
			// If current play mode on device is REPEAT_ONE, reset it to
			// NORMAL so it doesn't clash with media controller queue management
			if (stateModel.getPlayMode().equals(AVTransport.PLAY_MODE_REPEAT_ONE)) {
				logger.info("Kicking device out of REPEAT_ONE playMode");

				actionSetPlayMode(AVTransport.PLAY_MODE_NORMAL);

				//
				// Wait up to 4 sec for playMode to change due to event feedback
				// before setting local play mode (otherwise it will be
				// overridden
				// by incoming playMode event)
				//
				int waitSec = 0;
				while (waitSec++ < 8) {
					try {
						Thread.sleep(500);
					} catch (Exception e) {
					}

					if (stateModel.getPlayMode().equals(AVTransport.PLAY_MODE_NORMAL)) {
						logger.info("Device moved out of REPEAT_ONE playMode");
						break;
					}
				}
				if (waitSec == 8)
					logger.warning("Timeout waiting for NORMAL playMode");
			}

			stateModel.setPlayMode(playMode);
			stateModel.notifyObservers();
			playQueue.setPlayMode(playMode);
			// imageQueue.setPlayMode( playMode );
		} else if (playMode.equalsIgnoreCase(AVTransport.PLAY_MODE_REPEAT_ONE)) {
			actionSetPlayMode(playMode);
		} else if (playMode.equalsIgnoreCase(AVTransport.PLAY_MODE_NORMAL)) {
			stateModel.setPlayMode(playMode);
			stateModel.notifyObservers();
			playQueue.setPlayMode(playMode);
			// imageQueue.setPlayMode( playMode );
			actionSetPlayMode(playMode);
		}
	}

	private ArrayList protocolInfoList = new ArrayList();

	public void setProtocolInfoList(String protocolInfoString) {
		String[] tmp = protocolInfoString.split(",");
		for (int n = 0; n < tmp.length; n++)
			protocolInfoList.add(tmp[n].trim());
	}

	public int getProtocolInfoCount() {
		// If count is 0, try to refresh from device
		if (protocolInfoList.size() == 0) {
			actionGetProtocolInfo(); // Invokes setProtocolInfoList
		}

		return protocolInfoList.size();
	}

	public String getProtocolInfo(int index) {
		return (String) protocolInfoList.get(index);
	}

	public ArrayList getProtocolInfoList() {
		return protocolInfoList;
	}

	/**
	 * Convert protocolInfoList back to comma-separated list string
	 */
	public String protocolInfoListToString() {
		StringBuffer buf = new StringBuffer();

		for (int n = 0; n < protocolInfoList.size(); n++) {
			if (n == 0)
				buf.append((String) protocolInfoList.get(n));
			else
				buf.append("," + (String) protocolInfoList.get(n));
		}
		return buf.toString();
	}

	public ArgumentList execute(DomoMessage domoMessage) {
		String actionName = domoMessage.getMessage();
		Action action = null;
		action = avTransport.getAction(domoMessage.getMessage());
		if (action == null)
			action = connectionManager.getAction(domoMessage.getMessage());
		if (action == null)
			action = renderingControl.getAction(domoMessage.getMessage());
		if (action != null) {
			// setting up arguments from domoMessage
			Iterator inputParameterElements = domoMessage.getInputParameterElements().iterator();
			while (inputParameterElements.hasNext()) {
				DomoMessageInput messageInput = (DomoMessageInput) inputParameterElements.next();
				try {
					action.setArgumentValue(messageInput.getName(), messageInput.getValue());
					// unescapeHTML(messageInput.getValue()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (postControlAction(action, connectionManager)) {
				// operation executed successfully.
				// getting output argument list
				return action.getOutputArgumentList();
			}
		}
		return null;
	}
}
