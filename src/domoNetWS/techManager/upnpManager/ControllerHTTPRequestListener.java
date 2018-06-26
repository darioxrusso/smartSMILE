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
 *  File: $RCSfile: ControllerHTTPRequestListener.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

/*
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
*/

import java.util.Properties;
import java.util.ArrayList;
import java.util.logging.Logger;
/*
import java.io.FileInputStream;
import java.io.FileOutputStream;
*/
import java.io.IOException;

import javax.swing.JOptionPane;
//import javax.swing.border.TitledBorder;

import org.cybergarage.http.*;
import org.cybergarage.upnp.*;
import org.cybergarage.upnp.device.*;
import org.cybergarage.upnp.event.*;
//import org.cybergarage.upnp.ssdp.SSDPPacket;

import com.cidero.proxy.*;
//import com.cidero.util.MrUtil;
//import com.cidero.util.NetUtil;
import com.cidero.util.URLUtil;
//import com.cidero.util.AppPreferences;
import com.cidero.upnp.*;
//import com.cidero.control.*;

/**
 * Handle incoming HTTP request. These are requests other than the event
 * notifications, which are handled/dispatched by the base class.
 *
 * Currently, the only supported protocol/action is an HTTP get of a dynamic
 * playlist, which the control point can generate if a UPnP server doesn't
 * generate M3U playlist resources for a container.
 */
class ControllerHTTPRequestListener implements HTTPRequestListener {
	private final static Logger logger = Logger.getLogger("com.cidero.control");

	MediaController controller;

	public ControllerHTTPRequestListener(MediaController controller) {
		this.controller = controller;
	}

	public void httpRequestReceived(HTTPRequest httpReq) {
		logger.fine("Got HTTP request!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
		httpReq.print();

		if (!httpReq.isGetRequest()) {
			logger.warning("Unsupport HTTP request type (not a GET)");
			return;
		}

		// Dynamic playlist URI's are of the form:
		//
		// http://addr:port/dynamic/playlist/parentId_<id>/id_<id>.m3u
		//
		// The id's are the parent & object id for the container that
		// is used as the source for generating the playlist
		//
		// Firefox URLs are '/firefoxRadioURL' or /firefoxTrackURL'
		//
		if (httpReq.getURI().startsWith("/dynamic")) {
			createAndSendDynamicContent(httpReq);
			return;
		} else if (httpReq.getURI().startsWith("/remoteURL")) {
			processFirefoxRequest(httpReq);
			return;
		}
	}

	public void processFirefoxRequest(HTTPRequest httpReq) {
		String uri = httpReq.getURI();
		int index;
		logger.info("Firefox URI = " + uri);

		String webURLString;

		boolean isAudioBroadcast = true;

		// Incoming URI looks like
		// Reconstruct original web resource URL by stripping of the
		// 'firefoxURL'
		webURLString = "http:" + uri.replaceFirst("/[^/]*", "/");

		if (uri.indexOf("remoteURL-file") > 0) {
			isAudioBroadcast = false;
		} else {
			isAudioBroadcast = true;
		}

		// logger.info("Setting audiobroadcast to music track for streamium");
		// isAudioBroadcast = false;

		logger.info("Web URL String = " + webURLString);
		String webSite = webURLString.substring(7);
		index = webSite.indexOf("/");
		if (index > 0)
			webSite = webSite.substring(0, index);

		HTTPResponse response = new HTTPResponse();
		response.setStatusCode(HTTPStatus.OK);
		// response.setContentType("audio/x-mpegurl");

		CDSResource resource = new CDSResource();
		CDSObjectList objList;

		resource.setName(webURLString);

		if (webURLString.toLowerCase().endsWith(".m3u")) {
			resource.setProtocolInfo("http-get:*:audio/mpegurl:*");
			objList = resource.getPlaylistItems(isAudioBroadcast);
		} else if (webURLString.toLowerCase().endsWith(".pls")) {
			resource.setProtocolInfo("http-get:*:audio/x-scpls:*");
			objList = resource.getPlaylistItems(isAudioBroadcast);
		} else if (webURLString.toLowerCase().endsWith(".mp3")) {
			logger.info("Adding single MPEG resource: " + webURLString);
			resource.setProtocolInfo("http-get:*:audio/mpeg:*");
			CDSMusicTrack musicTrack = new CDSMusicTrack();
			// musicTrack.setArtist( "Internet" );
			musicTrack.setTitle(URLUtil.getPathTail(webURLString));
			musicTrack.addResource(resource);
			objList = new CDSObjectList();
			objList.add(musicTrack);
		} else if (webURLString.toLowerCase().endsWith(".mpg")) {
			logger.info("Adding single MPEG resource: " + webURLString);
			resource.setProtocolInfo("http-get:*:video/mpeg:*");
			CDSVideoItem video = new CDSVideoItem();
			// musicTrack.setArtist( "Internet" );
			video.setTitle(URLUtil.getPathTail(webURLString));
			video.addResource(resource);
			objList = new CDSObjectList();
			objList.add(video);
		} else if (webURLString.toLowerCase().endsWith(".jpg")) {
			logger.info("Adding single JPEG resource: " + webURLString);
			resource.setProtocolInfo("http-get:*:image/jpeg:*");
			CDSImageItem image = new CDSImageItem();
			// musicTrack.setArtist( "Internet" );
			image.setTitle(URLUtil.getPathTail(webURLString));
			image.addResource(resource);
			objList = new CDSObjectList();
			objList.add(image);
		} else if (webURLString.toLowerCase().endsWith(".wmv")) {
			logger.info("Adding single MPEG resource: " + webURLString);
			resource.setProtocolInfo("http-get:*:video/x-ms-wmv:*");
			CDSVideoItem video = new CDSVideoItem();
			// musicTrack.setArtist( "Internet" );
			video.setTitle(URLUtil.getPathTail(webURLString));
			video.addResource(resource);
			objList = new CDSObjectList();
			objList.add(video);
		} else if (webURLString.toLowerCase().indexOf("asx") > 0) {
			logger.info("Found ASX playlist: " + webURLString);

			CDSPlaylistContainer playlist = new CDSPlaylistContainer();
			playlist.setTitle("Web resource");

			resource.setProtocolInfo("http-get:*:audio/x-ms-asx:*");
			playlist.addResource(resource);
			objList = new CDSObjectList();
			objList.add(playlist);
		} else {
			logger.warning("Unknown resource type - trying M3U: " + webURLString);
			resource.setProtocolInfo("http-get:*:audio/mpegurl:*");
			objList = resource.getPlaylistItems(isAudioBroadcast);
			// response.setStatusCode( HTTPStatus.OK );
			// httpReq.post( response );
			// return;
		}

		// If playlist didn't have artist info, fill in with website
		for (int n = 0; n < objList.size(); n++) {
			CDSObject obj = (CDSObject) objList.get(n);
			if (obj instanceof CDSMusicTrack) {
				CDSMusicTrack trk = (CDSMusicTrack) obj;
				if (trk.getArtist() == null)
					trk.setArtist(webSite);
			} else {
				if (obj.getCreator() == null)
					obj.setCreator(webSite);
			}

			// Try adding fake duration, size for Streamium

			// resource = obj.getResource(0);
			// resource.setSize( 4000000 );
			// resource.setDurationSecs( 360 );
		}

		//
		// Add playlist items to all renderers that are visible, except
		// those that are under control of another 'master' renderer
		// Put up info dialog if none are visible
		//
		if ((objList != null) && (objList.size() > 0)) {
			ArrayList mediaRendererList = controller.getActiveMediaRenderers();
			if (mediaRendererList.size() == 0) {
				logger.warning("No active media renderers (select one to bring up window)");
				httpReq.post(response);
				return;
			}

			// int visibleRendererCount = 0;
			for (int dev = 0; dev < mediaRendererList.size(); dev++) {
				MediaRendererDevice mediaRenderer = (MediaRendererDevice) mediaRendererList.get(dev);

				if (mediaRenderer.getMasterRenderer() != null)
					continue;

				// visibleRendererCount++;

				for (int n = 0; n < objList.size(); n++) {
					CDSObject obj = (CDSObject) objList.get(n);
					if (obj instanceof CDSImageItem)
						mediaRenderer.addToImageQueue((CDSImageItem) obj);
					else
						mediaRenderer.addToPlayQueue(obj);

					// Mark the first added entry
					if (n == 0) {
						PlayQueue playQueue = mediaRenderer.getPlayQueue();
						logger.info("Setting markPos to " + (playQueue.size() - 1));
						playQueue.setMarkPosition(playQueue.size() - 1);
					}
				}
			}

			if (!webURLString.toLowerCase().endsWith(".jpg")) {
				//
				// If configured to do so, move to 1st new added item and start
				// renderer(s), if another track is not already being played
				//
				for (int dev = 0; dev < mediaRendererList.size(); dev++) {
					MediaRendererDevice mediaRenderer = (MediaRendererDevice) mediaRendererList.get(dev);

					if ((mediaRenderer.getMasterRenderer() != null) || mediaRenderer.isRunning()) {
						continue;
					}

					int firstAddedPos = mediaRenderer.getPlayQueue().getMarkPosition();

					logger.info("firstAddedPos = " + firstAddedPos);
					mediaRenderer.setPlayQueueSelectedIndex(firstAddedPos);
					mediaRenderer.startPlayback();
				}
			}
		}

		httpReq.post(response);

	}

	private void createAndSendDynamicContent(HTTPRequest httpReq) {
		String uri = httpReq.getURI();
		int index;

		logger.fine("CreateDynamic URI = " + uri);

		String[] splitURI = uri.split("/");

		// for( int n = 0 ; n < splitURI.length ; n++ )
		// System.out.println("uri = " + n + " " + splitURI[n] );

		if (splitURI[2].equals("playlist")) {
			// Dynamic playlist URI's are of the form:
			//
			// http://<addr>:<port>/dynamic/playlist/<rendererUUID>/<serverUUID>$<objId>.m3u
			//
			// The server UUID is needed to unambiguously search for the object
			// with the specified parentId/Id when there is more than one server
			// present.
			// The renderer UUID is needed so the playlist can be customized
			// to the protocols that the renderer supports (may be multiple
			// resources available for some items)
			//

			MediaDeviceList mediaDeviceList = MediaController.getMediaDeviceList();

			MediaRendererDevice rendererDevice = mediaDeviceList.getMediaRendererByUUID(splitURI[3]);
			if (rendererDevice == null)
				logger.warning("No renderer device matching UUID " + splitURI[3]);

			MediaServerDevice serverDevice = mediaDeviceList.getMediaServerByUUID(splitURI[4]);
			if (serverDevice == null)
				logger.warning("No server device matching UUID " + splitURI[4]);

			//
			// Some objIds may be constructed as a path (contain '/'s). Hence
			// the need for a different separator here. TODO: Use '#" instead?
			//
			String objId = null;

			index = uri.indexOf("$");
			if (index > 0) {
				// Strip .m3u suffix
				objId = uri.substring(index + 1);

				index = objId.indexOf(".");
				if (index >= 0)
					objId = objId.substring(0, index);
			}

			System.out.println("objId = " + objId);

			if ((rendererDevice != null) && (serverDevice != null) && (objId != null)) {
				// Get the child objects that are items (not sub-containers) and
				// make a playlist out of 'em
				CDSObjectList objList = serverDevice.browseChildren(objId);

				// Process the object list and produce one that has only
				// the best matching resources to the media renderer's supported
				// protocols
				objList = objList.getBestMatchingResource(rendererDevice.getProtocolInfoList(),
						rendererDevice.getLosslessWMATranscodeThresh());

				if (objList == null || objList.size() > 0) {
					M3UPlaylist playlist = new M3UPlaylist(objList);

					HTTPResponse response = new HTTPResponse();
					response.setStatusCode(HTTPStatus.OK);
					response.setContentType("audio/x-mpegurl");

					response.setContent(playlist.toString());
					httpReq.post(response);
					return;
				} else {
					logger.warning("Empty object list for dynamic playlist: " + uri);
				}
			} else {
				logger.warning("Dynamic playlist uri syntax error - uri: " + uri);
			}
		} else {
			logger.warning("Error - unknown dynamic content request");
		}

		httpReq.returnBadRequest();
	}
}
