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
 *  File: $RCSfile: MediaServerDevice.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

//import com.ssttr.xml.XMLUtils;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;

import org.cybergarage.upnp.*;
import org.cybergarage.upnp.device.InvalidDescriptionException;

import com.cidero.util.MrUtil;
// modified by Dario Russo
// import com.cidero.util.AppPreferences;
import com.cidero.upnp.*;
import com.cidero.control.*;

import domoML.domoMessage.*;

/**
 * Class used by control app to hold info for a single MediaServer device
 *
 */
public class MediaServerDevice extends MediaDevice {
	private final static Logger logger = Logger.getLogger("com.cidero.control");

	// Max number of returned items for browse requests
	private final static int MAX_REQUEST_COUNT = 250;

	DefaultTreeModel treeModel;
	AudioItemModel audioModel; // Model holding audio track info for panel
	ImageQueue imageModel; // Model holding image info for panel
	VideoItemModel videoModel; // Model holding video item info for panel

	// These helper classes for each service are instantiated here
	// for the benefit of their service-specific errorToString() methods
	// TODO: It would be cool to integrate the helper classes into the
	// Cybergarage stuff at some point
	CtrlServerConnectionManager connectionManager;
	CtrlContentDirectory contentDirectory;

	MediaNode rootNode;

	public MediaServerDevice(MediaController parentController, Device device) throws InvalidDescriptionException {
		super(parentController, device);

		processPreferences();

		connectionManager = new CtrlServerConnectionManager(this, device);
		contentDirectory = new CtrlContentDirectory(this, device);

		CDSContainer rootObj = new CDSContainer();
		rootObj.setId("0");
		rootObj.setTitle(device.getFriendlyName());

		rootNode = new MediaNode(this, rootObj);

		// Each server has a tree model for all UPnP container objects, and
		// separate models for audio, image, and video item objects
		treeModel = new DefaultTreeModel(rootNode);
		treeModel.setAsksAllowsChildren(true);

		audioModel = new AudioItemModel();
		imageModel = new ImageQueue();
		videoModel = new VideoItemModel();

		// Expand the root node at device discovery time
		// (now waiting for 1st button click)
		// rootNode.expand();

		//
		// If Windows Media Connect, and device location is local address
		// (127.*.*.*), save device location and associated info to file
		// to speed subsequent program startups (patch for WMC discovery bug)
		//
		// @todo Remove this when WMC bug is fixed...
		//

		logger.finest("Checking for WMC or WMP11... ");

		if ((device.getFriendlyName().indexOf("Windows Media Connect") >= 0)
				|| (device.getModelName().indexOf("Windows Media Player") >= 0)) {
			logger.finest("Yup, this server appears to be an instance of WMC/WMP11 ");

			AppPreferences pref = parentController.getPreferences();

			if (pref.getBoolean("wmcDiscoveryBugPatchEnable")) {
				logger.finest("WMC Patch enabled... ");

				// Only need to update the WMC device URL if it is blank
				String devURL = pref.get("wmcDeviceDescriptionURL", "");
				if (devURL.trim().equals("")) {
					logger.finest("No WMC device URL cached in preferences file");
					logger.finest("storing '" + devURL + "'");

					pref.put("wmcDeviceDescriptionURL", device.getLocation());
					parentController.savePreferences();
				}
			}
		}

	}

	public void destroy() {
		// parentController.getMediaBrowserPanel().unsetMediaServer( this );
	}

	public void processPreferences() {
		super.processPreferences(); // Process common server/renderer pref

		// Process server-specific properties

	}

	public int getDeviceType() {
		return MediaDevice.SERVER;
	}

	public String getDeviceTypeString() {
		return "server";
	}

	public AudioItemModel getAudioItemModel() {
		return audioModel;
	}

	public ImageQueue getImageItemModel() {
		return imageModel;
	}

	public VideoItemModel getVideoItemModel() {
		return videoModel;
	}

	public DefaultTreeModel getTreeModel() {
		return treeModel;
	}

	/**
	 * Invoke devices UPNP 'BrowseChildren' action for the given UPnP CDS parent
	 * Id. This routine requests a max of 250 items in any single result set. It
	 * issues multiple browse requests if a parent container has more than 250
	 * children
	 * 
	 * @param id
	 *            CDS parentID
	 *
	 * @return List of CDS objects, or null if no objects found
	 */
	public CDSObjectList browseChildren(String id) {
		logger.fine("browseChildren: Entered, id = " + id);

		Action action = contentDirectory.getAction("Browse");
		if (action != null) {
			logger.fine("found browse action");
		} else {
			logger.fine("couldn't find browse action");
			return null;
		}

		// Set action arguments.
		//
		// Request a max of 250 children for a single request
		// Note - some media servers may have lower limit than 250 children,
		// so logic below needs to take care to do the right thing based on
		// on the returned counts from each browse action
		//
		int startingIndex = 0;
		int requestedCount = MAX_REQUEST_COUNT;

		CDSObjectList objList = null;
		int totalMatches = 0;
		int totalReturnedMatches = 0;

		do {
			action.setArgumentValue("ObjectID", id);
			action.setArgumentValue("BrowseFlag", "BrowseDirectChildren");
			action.setArgumentValue("Filter", "*");
			action.setArgumentValue("StartingIndex", Integer.toString(totalReturnedMatches));
			action.setArgumentValue("RequestedCount", Integer.toString(requestedCount));
			action.setArgumentValue("SortCriteria", "");

			boolean status = postControlAction(action, contentDirectory);
			if (status == false) {
				logger.fine("postControlAction: Failure!");
				return null;
			}

			// Returned args are: Result, NumberReturned, TotalMatches, UpdateId

			logger.fine("postControlAction: Success");

			ArgumentList outArgList = action.getOutputArgumentList();
			Argument resultArg = outArgList.getArgument("Result");
			Argument numberReturnedArg = outArgList.getArgument("NumberReturned");
			Argument totalMatchesArg = outArgList.getArgument("TotalMatches");

			if (resultArg == null || numberReturnedArg == null || totalMatchesArg == null) {
				logger.warning("Error reading browse arguments");
				return null;
			}

			// logger.fine("Result is:\n" + resultArg.getValue() );
			try {
				totalMatches = totalMatchesArg.getIntegerValue();
				totalReturnedMatches += numberReturnedArg.getIntegerValue();
				if (totalMatches == 0) {
					logger.fine("browse - no objects found!!!");
					return objList;
				}

				requestedCount = totalMatches - totalReturnedMatches;
				if (requestedCount > MAX_REQUEST_COUNT)
					requestedCount = MAX_REQUEST_COUNT;

				// Parse the result argument into DOM
				if (objList == null)
					objList = new CDSObjectList(resultArg.getValue());
				else
					objList.addAll(new CDSObjectList(resultArg.getValue()));

				// logger.fine("browse found " + objList.size() + " objects" );
				if (logger.isLoggable(Level.FINEST))
					logger.finest("ObjList: " + objList.toString());
			} catch (UPnPException e) {
				logger.warning(e.toString());
				return null;
			}

		} while (totalReturnedMatches < totalMatches);

		return objList;
	}

	/**
	 * Handler for button press (select) on a specific Media Server. Sets tree
	 * model in controller's browser window. The 'last known' state of the media
	 * tree is displayed, and refreshed if the device has been updated (not yet
	 * impl)
	 */

	public void actionPerformed(ActionEvent e) {
		/*
		 * // Expand the root node at device discovery time if( !
		 * rootNode.expand() ) { // WMC uses 'HOST:Windows Media Connect" if(
		 * getFriendlyName().toLowerCase().indexOf("windows media connect") >= 0
		 * ) { JOptionPane.showMessageDialog(
		 * MediaController.getInstance().getFrame(), "<html>" +
		 * "Server Browse Error - Windows Media Connect requires that each<br>"
		 * +
		 * "control point accessing it be authorized via the Windows Media<br>"
		 * + "Connect user interface. Please authorized the controller and<br>"
		 * + "reselect the WMC server button." ); } else {
		 * JOptionPane.showMessageDialog(
		 * MediaController.getInstance().getFrame(), "<html>" +
		 * "Server Browse Error - Please check server configuration and<br>" +
		 * "basic network connectivity and try again, or select a<br>" +
		 * "different server" ); } } else { logger.finer(
		 * "BUTTON CLICK - device " + getFriendlyName() );
		 * parentController.getMediaBrowserPanel().setMediaServer( this ); }
		 */
	}

	public ArgumentList getMediaContent(String mediaContainerId, String mediaContentId) {
		org.cybergarage.upnp.Action action = contentDirectory.getAction("Search");
		if (action != null) {
			System.out.println("found browse action");
		} else {
			System.out.println("couldn't find browse action");
			return null;
		}

		// Set action arguments.
		//
		// Request a max of 250 children for a single request
		// Note - some media servers may have lower limit than 250 children,
		// so logic below needs to take care to do the right thing based on
		// on the returned counts from each browse action
		//
		int startingIndex = 0;
		// int requestedCount = MAX_REQUEST_COUNT;
		int requestedCount = 1;

		CDSObjectList objList = null;
		int totalMatches = 0;
		int totalReturnedMatches = 0;

		// do
		// {
		action.setArgumentValue("ContainerID", mediaContainerId);
		action.setArgumentValue("SearchCriteria", "@id = \"" + mediaContentId + "\"");

		action.setArgumentValue("Filter", "*");
		action.setArgumentValue("StartingIndex", Integer.toString(totalReturnedMatches));
		action.setArgumentValue("RequestedCount", Integer.toString(requestedCount));
		action.setArgumentValue("SortCriteria", "");

		boolean status = postControlAction(action, contentDirectory);
		if (status == false) {
			System.out.println("postControlAction: Failure!");
			return null;
		}

		// Returned args are: Result, NumberReturned, TotalMatches, UpdateId

		System.out.println("postControlAction: Success");

		return action.getOutputArgumentList();
		/*
		 * Argument resultArg = outArgList.getArgument("Result");
		 * System.out.println("RISULTATO: " + resultArg.getValue()); Argument
		 * numberReturnedArg = outArgList.getArgument("NumberReturned");
		 * Argument totalMatchesArg = outArgList.getArgument("TotalMatches");
		 * 
		 * if( resultArg == null || numberReturnedArg == null || totalMatchesArg
		 * == null ) { System.out.println("Error reading browse arguments" );
		 * return null; }
		 * 
		 * //logger.fine("Result is:\n" + resultArg.getValue() ); try {
		 * totalMatches = totalMatchesArg.getIntegerValue();
		 * totalReturnedMatches += numberReturnedArg.getIntegerValue(); //if(
		 * totalMatches == 0 ) // { // System.out.println(
		 * "browse - no objects found!!!"); // return objList; // }
		 * 
		 * //requestedCount = totalMatches - totalReturnedMatches; //if(
		 * requestedCount > MAX_REQUEST_COUNT ) // requestedCount =
		 * MAX_REQUEST_COUNT;
		 * 
		 * // Parse the result argument into DOM if( objList == null ) objList =
		 * new CDSObjectList( resultArg.getValue() ); else objList.addAll( new
		 * CDSObjectList( resultArg.getValue() ) );
		 * 
		 * //logger.fine("browse found " + objList.size() + " objects" );
		 * System.out.println("ObjList: " + objList.toString() ); } catch(
		 * UPnPException e ) { logger.warning( e.toString() ); return null; }
		 * 
		 * } while( totalReturnedMatches < totalMatches );
		 * 
		 * return objList;
		 */
	}

	public ArgumentList execute(DomoMessage domoMessage) {
		String actionName = domoMessage.getMessage();
		if (actionName.equalsIgnoreCase("SearchForMediaContent")) {
			return searchForMediaContent(domoMessage);
		} else {
			Action action = contentDirectory.getAction(domoMessage.getMessage());
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
			if (postControlAction(action, contentDirectory)) {
				// operation executed successfully.
				// getting output argument list
				return action.getOutputArgumentList();
			} else {
				return null;
			}
		}
	}

	public CDSObject searchForMediaCDSObject(String mediaContainerId, String mediaContentId) {
		Iterator cdsContainerListIt = browseChildren(mediaContainerId).iterator();
		boolean found = false;
		CDSObject contentObject = null;
		while (cdsContainerListIt.hasNext() && !found) {
			contentObject = (CDSObject) cdsContainerListIt.next();
			System.out.println("Confronto: " + contentObject.getId() + " con " + mediaContentId);
			if (contentObject.getId().equals(mediaContentId)) {
				System.out.println("Trovato");
				found = true;
			} else
				System.out.println("Non trovato");
		}
		System.out.println("Ritorno: " + contentObject.toString());
		return contentObject;
	}

	public ArgumentList searchForMediaContent(DomoMessage domoMessage) {
		// gets the paramters
		Iterator inputParameterElements = domoMessage.getInputParameterElements().iterator();
		String mediaContainerId = null;
		String mediaContentId = null;

		while (inputParameterElements.hasNext()) {
			DomoMessageInput messageInput = (DomoMessageInput) inputParameterElements.next();
			if (messageInput.getName().equalsIgnoreCase("mediaContainerId"))
				mediaContainerId = messageInput.getValue();
			else if (messageInput.getName().equalsIgnoreCase("mediaContentId"))
				mediaContentId = messageInput.getValue();
		}

		if (mediaContentId != null && mediaContainerId != null) {
			Iterator cdsContainerListIt = browseChildren(mediaContainerId).iterator();
			boolean found = false;
			CDSObject contentObject = null;
			while (cdsContainerListIt.hasNext() && !found) {
				contentObject = (CDSObject) cdsContainerListIt.next();
				if (contentObject.getId().equals(mediaContentId)) {
					found = true;
				}
			}

			if (found) {
				ArgumentList argumentListResult = new ArgumentList();
				argumentListResult.add(new Argument("Result", contentObject.toDIDL()));
				return argumentListResult;
			}
		}
		return null;
	}

	public static final String escapeHTML(String s) {
		StringBuffer sb = new StringBuffer();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '&':
				sb.append("&amp;");
				break;
			case '"':
				sb.append("&quot;");
				break;
			// be carefull with this one (non-breaking whitee space)
			// case ' ': sb.append("&nbsp;");break;
			case ' ':
				sb.append(" ");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}

	public static String escapeHTML2(String string) {
		StringBuffer sb = new StringBuffer(string.length());
		// true if last char was blank
		boolean lastWasBlankChar = false;
		int len = string.length();
		char c;

		for (int i = 0; i < len; i++) {
			c = string.charAt(i);
			if (c == ' ') {
				// blank gets extra work,
				// this solves the problem you get if you replace all
				// blanks with &nbsp;, if you do that you loss
				// word breaking
				if (lastWasBlankChar) {
					lastWasBlankChar = false;
					// sb.append("&nbsp;");
				} else {
					lastWasBlankChar = true;
					sb.append(' ');
				}
			} else {
				lastWasBlankChar = false;
				//
				// HTML Special Chars
				if (c == '"')
					sb.append("&quot;");
				else if (c == '&')
					sb.append("&amp;");
				else if (c == '<')
					sb.append("&lt;");
				else if (c == '>')
					sb.append("&gt;");
				else if (c == '\n')
					// Handle Newline
					sb.append("&lt;br/&gt;");
				else {
					int ci = 0xffff & c;
					if (ci < 160)
						// nothing special only 7 Bit
						sb.append(c);
					else {
						// Not 7 Bit use the unicode system
						sb.append("&#");
						sb.append(new Integer(ci).toString());
						sb.append(';');
					}
				}
			}
		}
		return sb.toString();
	}

	public static final String unescapeHTML(String s) {
		return unescapeHTML(s, 0);
	}

	public static final String unescapeHTML(String s, int f) {
		String[][] escape = { { "&lt;", "<" }, { "&gt;", ">" }, { "&amp;", "&" }, { "&quot;", "\"" } };
		int i, j, k, l;

		i = s.indexOf("&", f);
		if (i > -1) {
			j = s.indexOf(";", i);
			// --------
			// we don't start from the beginning
			// the next time, to handle the case of
			// the &amp;
			// thanks to Pieter Hertogh for the bug fix!
			f = i + 1;
			// --------
			if (j > i) {
				// ok this is not most optimized way to
				// do it, a StringBuffer would be better,
				// this is left as an exercise to the reader!
				String temp = s.substring(i, j + 1);
				// search in escape[][] if temp is there
				k = 0;
				while (k < escape.length) {
					if (escape[k][0].equals(temp))
						break;
					else
						k++;
				}
				if (k < escape.length) {
					s = s.substring(0, i) + escape[k][1] + s.substring(j + 1);
					return unescapeHTML(s, f); // recursive call
				}
			}
		}
		return s;
	}

}
