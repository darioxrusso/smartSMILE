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
 *  File: $RCSfile: PlayThread.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

import com.cidero.util.*;
import com.cidero.upnp.*;
import com.cidero.http.*;

/**
 * Playback thread class. Used for playback of audio & video content. A new
 * instance of the underlying playback thread is created each time the play
 * button is pressed. Audio/Video playback and Image playback state transitions
 * are a bit different, so a separate ImagePlayThread class is used for image
 * playback.
 */
public class PlayThread implements Runnable {
	private static Logger logger = Logger.getLogger("com.cidero.control");

	MediaRendererDevice renderer;
	AppPreferences pref;

	// String playMode;
	// int queueStartIndex;

	/**
	 * Constructor
	 *
	 */
	public PlayThread(MediaRendererDevice renderer) {
		this.renderer = renderer;
	}

	private Thread playThread = null; // for clean shutdown via stop()

	/**
	 * Start the thread
	 * 
	 */
	public void start() {
		if (playThread == null) {
			playThread = new Thread(this);
			playThread.start();
		} else {
			logger.warning("start: play thread already running!");
		}
	}

	public boolean isRunning() {
		return (playThread != null);
	}

	public void stop() {
		if (playThread == null)
			logger.warning("stop: play thread not currently running");

		playThread = null;
	}

	public void run() {
		logger.fine("Audio/Video PlayThread: Running...");

		// shorthand
		PlayQueue playQueue = renderer.getPlayQueue();
		RendererStateModel stateModel = renderer.getStateModel();
		pref = MediaController.getPreferences();

		//
		// If empty play queue, assume device has a CurrentTransportURI value
		// (perhaps received via a different control point) and just issue
		// a play command to (re)start it
		//

		if (playQueue.isEmpty()) {
			logger.fine("Empty play queue - (Re)Starting last URI sent to device");

			// TODO: Add check for CurrentTransportURI here
			if (renderer.actionPlay("1.0") == false)
				logger.warning("Error occurred starting item playback");

			playThread = null;
			return;
		}

		if (stateModel.getTransportState().equals(AVTransport.STATE_PLAYING)
				|| stateModel.getTransportState().equals(AVTransport.STATE_TRANS)
				|| stateModel.getTransportState().equals(AVTransport.STATE_RECORDING)) {
			logger.info("Can't start playback when device is in state '" + stateModel.getTransportState() + "'");
			return;
		}

		int playCount = 0;

		// Get current selection from PlayQueue JList. Defaults to 1st item
		// in list (index 0) if nothing selected
		int selectedIndex = renderer.getPlayQueueSelectedIndex();
		if (selectedIndex >= 0) {
			playQueue.setCurrentPosition(selectedIndex);
			// renderer.clearPlayQueueSelection();
		} else {
			playQueue.setCurrentPosition(0);
		}

		CDSObject obj = playQueue.getFirstItem();

		if (obj == null) {
			logger.warning("No object located in play queue (bug)");
			playThread = null;
			return;
		}

		//
		// If running in sync mode, check for presence of proxy before
		// proceeding.
		// (Sending bogus proxy URL's to renderers can cause them to lock up and
		// need to be rebooted)
		//
		if (renderer.getSlaveRendererList().size() >= 1) {
			if (!syncProxyIsAlive()) {
				logger.warning("Synchronous proxy server not responding");
				/*
				 * JOptionPane.showMessageDialog(
				 * MediaController.getInstance().getFrame(), "<html>" +
				 * "Synchronous proxy server not responding - please check settings<br>"
				 * +
				 * "under 'Options->Synchronization Options' to make sure Proxy IP<br>"
				 * + "address and port are correct</html>");
				 */
				playThread = null;
				return;
			}
		}

		while ((playThread != null) && (obj != null)) {
			//
			// If object has no resources (container with no playlist
			// resource?),
			// try and generate one locally. It gets served by Control-point-
			// resident HTTP server.
			//
			if (obj.getResourceCount() <= 0) {
				logger.fine("Obj has no resources - trying to generate playlist");
				if (!renderer.addPlaylistResource(obj)) {
					logger.warning("No resources for this object (skipping)");
					continue;
				}
			}

			//
			// Get the 'best matching resource' for the given target renderer
			// device. This is based on the preferred/supported protocols of
			// both the source (server) and sink (renderer) devices
			//

			// renderer.getLosslessWMATranscodeThresh();
			// renderer.getHost();

			// Reload the protocol supported info - Added by Dario Russo
			renderer.actionGetProtocolInfo();

			String reqIpAddr = null; // Not yet impl
			CDSResource res = obj.getBestMatchingResource(renderer.getProtocolInfoList(), reqIpAddr, "684x456",
					renderer.getLosslessWMATranscodeThresh());
			if (res == null) {
				logger.warning("No matching resource for this server/renderer combo");
				logger.warning("Supported renderer protocols: " + renderer.protocolInfoListToString());
				break;
			}
			logger.fine("Resource name: " + res.getName());

			//
			// If resource's network segment is the same as one of the local
			// host's
			// interfaces, but *different* than the renderer's network,
			// substitute the correct interface into the resource. The most
			// common problem this solves is when a controller is run on the
			// same host as a server, and the server, seeing a browse request
			// coming from the local host, advertises a resource with the
			// local loopback address in response to the browse command. If
			// this raw URL with the loopback address is passed to a media
			// renderer, it won't work - the media renderer needs an external
			// interface URL
			//
			String resourceHost = URLUtil.getHost(res.getName());

			if (resourceHost != null) {
				if (NetUtil.isLocalAddr(resourceHost) && !NetUtil.onSameSubnet(resourceHost, renderer.getHost())) {
					logger.fine("res is local URL and renderer on diff subnet!");

					// Search for the interface addr on the host that is on the
					// same subnet as the renderer device
					String subnetHostAddr = NetUtil.findInterfaceAddrOnSameSubnetAs(renderer.getHost());

					if (subnetHostAddr != null) {
						logger.fine("Substituting new host interface in resource = " + subnetHostAddr);

						CDSResource cloneResource = (CDSResource) res.clone();
						cloneResource.setName(URLUtil.replaceHostAddr(res.getName(), subnetHostAddr));
						res = cloneResource;
					}
				}
			}

			// Do basic connection setup for this protocol. This is expensive Op
			// (several UPnP calls), so only do 1st time through (TODO: May need
			// to do whenever protocolInfo changes also)
			if (playCount == 0)
				renderer.setupConnection(res.getProtocolInfo());
			playCount++;

			//
			// If object is a music track, and bit rate is set to more than
			// 128Kbytes/sec, assume it's miscoded as bits/sec and divide
			// by 8 to get bytes/sec (yes, UPnP field name is confusing)
			// (MusicMatch is one server that has this problem in version 10)
			//
			// Note: This hack didn't fix DLink seeing bad duration from
			// MusicMatch server ( I think the DLink must be using the HTTP
			// Content-length header or something like that ), so I took it
			// out for now.
			// if( (obj instanceof CDSAudioItem) && (res.getBitRate() > 128000)
			// )
			// res.setBitRate( res.getBitRate()/8 );

			// If acting as master for multiple renderers, use proxy
			// address for resource that points to synchronous server proxy
			//
			// Example:
			//
			// http://64.236.34.196:80/stream/2001
			//
			// gets changed to:
			//
			// http://<proxyIP>:<proxyPort>/opt:bitRate=16384/64.236.34.196:80/stream/2001
			//
			// Bit rate is needed to establish bounds for 'slow start window'.
			// May want to pass window explicitly in future to allow for
			// different
			// windows for different renderers (behaviour is tuned to Roku for
			// now)
			//
			if (renderer.isMaster()) {
				CDSResource cloneResource = (CDSResource) res.clone();

				String optString = "opt:bitRate=" + res.getEstimatedBitRate();

				cloneResource.setName(URLUtil.urlToProxy(res.getName(), MediaController.getProxyIPAddress(),
						pref.getInt("syncProxyPort", 18081), optString));
				res = cloneResource;
			}

			//
			// TODO: filter string here should probably be switched to be the
			// same one used during the browse action, or maybe to one that
			// suppresses properties that are unused by the renderer (OJN)
			//
			if (renderer.actionSetAVTransportURI(res.getName(), CDS.toDIDL(obj, "*", res)) == false) {
				logger.warning("play:Error returned from setting URI");
				break;
			}

			// Latch Artist/Title info to deal with media renderers (e.g.
			// Intel's MediaRenderer sample) that do not properly echo
			// back the artist/title info from UPnP GetPositionInfo action
			renderer.latchArtistTitleInfo(obj);

			// Reset time stamps used to detect motion timeout for track
			// (handle unexpected renderer lockups)
			renderer.resetTrackMotionTimeStamps();

			//
			// Send play action to renderer. Note that a successful play action
			// should result in an incoming LastChange event with TransportState
			// set to 'PLAYING'. When the event is received, the UI is updated
			// and a monitoring thread is started to issue periodic
			// getPostionInfo
			// actions
			//
			// logger.info("Invoking 'Play'");

			if (renderer.actionPlay("1.0") == false) {
				logger.warning("Error occurred starting item playback - " + " Stopping playback session");
				break;
			}

			// A 'play' operation is considered to be the start of sync
			long lastSyncTimeMillis = System.currentTimeMillis();
			// logger.info("After 'Play'");

			//
			// Wait a short time for things to get going. Default is 6000 ms
			// (6sec),
			// but it's user-configurable. Need to wait here to give
			// transportState
			// time to transition to 'PLAYING', otherwise logic below could
			// see the 'STOPPED' state and think it's time to move to the next
			// track.
			//
			int waitMs = pref.getInt("renderer.playTransitionTimeoutMs", 6000);

			// System.out.println("Waiting for playTransition - millis = " +
			// waitMs );
			MrUtil.sleep(waitMs);

			//
			// For devices that support the SetNextAVTransportURI UPnP action,
			// get the next URI and invoke the action. This enables the device
			// to make a smooth transition between tracks.
			//
			// Note the use of 'findNextItem()' (doesn't alter queue position)
			// as
			// opposed to 'getNextItem()' here
			//
			CDSObject nextObj = playQueue.findNextItem();

			if ((nextObj != null) && renderer.isSetNextAVTransportURIEnabled())
				renderer.setNextAVTransportURI(nextObj);

			//
			// Loop waiting for the device to transition to the STOPPED
			// TransportState, moving on in the play queue when the current
			// track finishes playing. Also support devices that utilize the
			// SetNextAVTransportURI action to implement gapless multi-track
			// playbacks
			//
			String lastTrackURI = null;
			String lastTrackMetaData = null;
			int loopCount = 0;
			boolean sentStopActionToStuckDevice = false;
			int motionTimeoutMs = pref.getInt("renderer.playMotionTimeoutMs", 8000);

			while (playThread != null) {
				//
				// Note: actionGetPositionInfo now being invoked periodically in
				// separate monitoring thread. If the position is being updated
				// (so the device is returning UPnP GetPositionInfo results ok)
				// but the position has been 'stuck' for a while, issue a stop
				// command to attempt to get the device unstuck. This is a hack
				// to deal with a (fairly rare) sticking problem with the DLink
				// DSM320
				// on certain songs - the device gets to within ~1 sec of the
				// end
				// of the song, but never moves to a TransportState of STOPPED
				//
				// if motionTimeoutMs <= 0, stuck renderer detection is disabled
				//
				if ((motionTimeoutMs > 0) && (renderer.timeSinceLastTrackMotionMillis() > motionTimeoutMs)) {
					if (!sentStopActionToStuckDevice) // only send 1 stop
														// request
					{
						logger.warning("STUCK DEVICE!!!!!!!!! - issuing STOP TO UNSTICK");
						renderer.actionStop();
						sentStopActionToStuckDevice = true;
					} else {
						logger.warning(" DEVICE STILL STUCK !!!!!! ");
					}
				}

				//
				// If CurrentTrackURI changes to NextAVTransportURI, then the
				// device
				// moved on to the next track on it's own (must be supporting
				// the
				// SetNextAVTransportURI pipelining scheme, enabling smooth
				// music
				// track transitions). In this case, the TransportState never
				// switched to STOPPED between tracks. Set things up for the
				// *next*
				// track by updating the queue position and invoking
				// SetNextAVTransportURI
				//
				// These tests skipped first time through since lastTrackURI
				// isn't valid
				//
				// Bypass these tests if item is of type audioBroadcast - song
				// data will change but same item in queue (the station) applies
				//

				String currTrackMetaData = stateModel.getTrackMetaData();

				if (renderer.supportsNextAVTransportURI() && renderer.isSetNextAVTransportURIEnabled()
						&& (!(obj instanceof CDSAudioBroadcast))) {
					if (((lastTrackURI != null) && (!stateModel.getTrackURI().equals(lastTrackURI)))
							|| ((lastTrackMetaData != null) && (currTrackMetaData != null)
									&& (!currTrackMetaData.equals(lastTrackMetaData)))) {
						logger.fine("device-driven TrackURI/Metadata change detected");

						// Update currently playing object to reference the
						// right thing
						obj = playQueue.getNextItem(); // Updates queue position

						// Update reference to next obj in list, and send to
						// renderer,
						// unless we're doing a synchronized playback, and it's
						// time
						// to resync
						nextObj = playQueue.findNextItem();

						if (renderer.isMaster() && isGoodTimeForResync(playQueue, lastSyncTimeMillis)) {
							logger.info("Will resync on next song!!!!!!!!!!!");
						} else {
							if (nextObj != null)
								renderer.setNextAVTransportURI(nextObj);
						}
					} else if (playQueue.findNextItem() != nextObj) {
						// Play queue was modified, and the next entry is
						// different
						// than what it was - update the NextAVTransportURI
						// fields
						// Note: current item not consumed, so use find(), not
						// get()
						nextObj = playQueue.findNextItem();

						logger.info("Next item in play queue changed");
						if (renderer.isMaster() && isGoodTimeForResync(playQueue, lastSyncTimeMillis)) {
							logger.info("Will resync on next song!!!!!!!!!!!");
						} else {
							if (nextObj != null)
								renderer.setNextAVTransportURI(nextObj);
						}
					} else {
						logger.finer("stateModel.TrackURI: " + stateModel.getTrackURI());
						logger.finer("stateModel.TrackMetaData: " + stateModel.getTrackMetaData());
					}
				}

				lastTrackURI = stateModel.getTrackURI(); // update
				lastTrackMetaData = stateModel.getTrackMetaData(); // update

				//
				// Block waiting for state change, or 3 sec timeout
				//
				if (stateModel.waitForTransportState(AVTransport.STATE_STOPPED, 3000)) {
					logger.fine("waitForTransportState - STOPPED");
					break;
				} else {
					logger.fine("TransportState not yet STOPPED (condition timeout) ");
				}

				loopCount++;

			} // while ( playThread != null )

			if (playThread != null) {
				// Update queue position. Side effect of getPrev/Next calls is
				// to
				// shift view of playQueue scrolling JList to ensure new item
				// is visible

				if (prevFlag) // was stop caused by request for prev item?
				{
					// If in 1st ~9 sec (3 polling loop cycles) of playback,
					// jump
					// back one song in queue. Otherwise just leave queue
					// position at
					// current position, so current song is restarted from
					// beginning
					// (attempt to emulate most CD players)
					if (loopCount < 3)
						obj = playQueue.getPrevItem();

					prevFlag = false; // Clear flag
				} else {
					obj = playQueue.getNextItem();
				}
			} else {
				// User must have issued stop command - Done with playback so
				// clear
				// the busy flag of the current track so it no longer shows up
				// as
				// green in the UI
				playQueue.clearCurrentItemBusyFlag();
			}

		} // while( (playThread != null) && (obj != null) )

		if (obj == null)
			logger.fine("No more resources in play queue - stopping");

		if (playThread == null)
			logger.fine("Audio/Video play thread stopped by user");

		logger.fine("Audio/Video play thread terminating");

		playThread = null; // Reset for next start() call

	}

	/**
	 * Methods used to help implement next/prev button behavior
	 */
	boolean nextFlag = false;
	boolean prevFlag = false;

	public void setNextFlag(boolean value) {
		nextFlag = value;
	}

	public void setPrevFlag(boolean value) {
		prevFlag = value;
	}

	/**
	 * Check if it is a good time for a resync when running in synchronous mode
	 * 
	 */
	public boolean isGoodTimeForResync(PlayQueue playQueue, long lastSyncTimeMillis) {
		if (pref.getBoolean("resyncOnAlbumTransitions", true)) {
			if (playQueue.isNextSongStartOfAlbum()) {
				return true;
			}
		}

		long timeSinceLastSync = System.currentTimeMillis() - lastSyncTimeMillis;

		logger.info("Time since last resync (min): " + timeSinceLastSync / (1000 * 60) + " Threshold: "
				+ pref.getInt("resyncPeriodMin", 60));

		if (timeSinceLastSync > (pref.getInt("resyncPeriodMin", 60) * 60 * 1000)) {
			return true;
		}

		return false;
	}

	public boolean syncProxyIsAlive() {
		String ipAddr = MediaController.getProxyIPAddress();
		AppPreferences pref = MediaController.getPreferences();
		int port = pref.getInt("syncProxyPort", 18081);

		HTTPConnection connection = new HTTPConnection();
		try {
			URL url = new URL("http://" + ipAddr + ":" + port + "/SyncProxyPing");
			HTTPRequest request = new HTTPRequest(HTTP.GET, url);

			// Override use of keep-alive connection with 'close' header for now
			request.setHeader("Connection", "close");

			HTTPResponse response = connection.sendRequest(request, false);

			if ((response == null) || (response.getStatusCode() != HTTPStatus.OK)) {
				logger.warning("Error pinging proxy");
				return false;
			} else {
				logger.info("Success pinging proxy!");
				return true;
			}
		} catch (MalformedURLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}

}
