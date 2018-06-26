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
 *  File: $RCSfile: MediaNode.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.awt.Cursor;
import javax.swing.tree.DefaultMutableTreeNode;

// modified by Dario Russo
// import com.cidero.util.AppPreferences;
import com.cidero.util.MrUtil;
import com.cidero.upnp.*;
import com.cidero.swing.MySwingUtil;

/**
 * Media node class. There is a media node for every container object in the
 * ContentDirectory that has been visited (browsed)
 */
public class MediaNode extends DefaultMutableTreeNode {
	private final static Logger logger = Logger.getLogger("com.cidero.control");

	MediaServerDevice mediaDevice; // 'Parent' device
	boolean needsRefresh;

	// List of child objects for this node
	CDSObjectList itemList;

	/**
	 * Constructor
	 */
	public MediaNode(MediaServerDevice device, CDSObject upnpObj) {
		// userObject's toString() method is used in tree rendering,
		// so set it here (don't implement as simple member)
		setUserObject(upnpObj);
		this.mediaDevice = device;
		needsRefresh = true;
		itemList = new CDSObjectList();
	}

	public boolean getAllowsChildren() {
		CDSObject obj = (CDSObject) getUserObject();
		return obj.isContainer();
	}

	public MediaServerDevice getMediaDevice() {
		return mediaDevice;
	}

	public boolean needsRefresh() {
		return needsRefresh;
	}

	public void setNeedsRefresh(boolean flag) {
		needsRefresh = flag;
	}

	public void removeAllItems() {
		itemList.clear();
	}

	public void addItem(CDSObject obj) {
		itemList.add(obj);
	}

	public CDSObjectList getItemList() {
		return itemList;
	}

	/**
	 * Expand node if node is in need of refresh (browse for children)
	 *
	 * @return true on success, false on failure
	 *
	 *         TODO: Clean up error handling here...don't just deal with browse
	 *         error for root node (OJN)
	 */
	public boolean expand() {
		logger.fine("expand!!");

		if (needsRefresh) {
			// logger.fine("refreshing node, device = " +
			// mediaDevice.getDevice().getFriendlyName() );
			//
			// remove all current children of this node
			//
			removeAllChildren();
			removeAllItems();

			//
			// Invoke UPNP browse action, and add all the 'new' children
			// to the node tree
			//
			//
			// Handle the special case of a playlist that exists as a
			// playlist item (MusicMatch) - in such a case the child
			// items must be retrieved via an HTTP request for the M3U file
			//

			CDSObject upnpObj = (CDSObject) getUserObject();

			// Enable wait cursor for large containers
			// TODO: Really need to multi-thread potentially long operations
			int childCount = 0;

			// Object should *always* be the a CDSContainer here
			if (upnpObj instanceof CDSContainer) {
				childCount = ((CDSContainer) upnpObj).getChildCount();
				/*
				 * if( childCount > 250 ) {
				 * 
				 * MediaController.getInstance().getMediaBrowserPanel().
				 * setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
				 * }
				 */
			} else {
				logger.warning("Object not of container type!");
				return false;
			}

			CDSObjectList objList;
			if (upnpObj instanceof CDSPlaylistContainer) {
				CDSPlaylistContainer playlist = (CDSPlaylistContainer) upnpObj;
				if ((playlist.getChildCount() == 0) && (playlist.getResourceCount() > 0)) {
					objList = playlist.browseChildrenUsingHTTP();
				} else {
					objList = mediaDevice.browseChildren(upnpObj.getId());
				}
			} else {
				objList = mediaDevice.browseChildren(upnpObj.getId());
			}

			// Restore cursor
			/*
			 * if( childCount > 250 ) {
			 * MediaController.getInstance().getMediaBrowserPanel(). setCursor(
			 * Cursor.getDefaultCursor() ); }
			 */
			// Treat as serious error if no children of root node
			if ((objList == null) && (upnpObj.getId().equals("0"))) {
				return false;
			}

			//
			// If container is a set of audio broadcasts, and the items have
			// no 'creator' element (used to hold 'artist-like' info in case
			// of Rhapsody stations), set the creator to the title of the
			// parent object (TODO: May not be best strategy in all cases -
			// this is a test for the Rhapsody case - OJN
			//

			if (objList != null) {
				for (int n = 0; n < objList.size(); n++) {
					CDSObject obj = objList.getObject(n);
					if ((obj instanceof CDSAudioBroadcast) && (obj.getCreator() == null)) {
						obj.setCreator(upnpObj.getTitle());
					}
				}
			}

			//
			// Add container objects to tree. Item objects are added to
			// separate list of item objects for node, to keep them
			// invisible to tree model (they are not displayed in tree,
			// but in a separate item panel)
			//
			// If there are more than a given number (nominally 500) of
			// sub-containers in this container, and 'auto-insert-alphabetical-
			// nodes' is enabled, create special intermediate alphabetical nodes
			// to speed up browsing
			//

			ArrayList intermediateNodeList = null;

			AppPreferences pref = MediaController.getPreferences();
			boolean alphaFoldersEnabled = pref.getBoolean("autoAlphaFoldersEnabled", true);
			int alphaFoldersThresh = pref.getInt("autoAlphaFoldersThresh", 500);

			if (objList != null) {
				if (alphaFoldersEnabled && (objList.getContainerCount() > alphaFoldersThresh)) {
					// Create intermediate nodes and add them to current node
					intermediateNodeList = createIntermediateNodes();
				}

				for (int n = 0; n < objList.size(); n++) {
					CDSObject obj = objList.getObject(n);

					MediaNode parentNode = null;
					if (intermediateNodeList != null)
						parentNode = findIntermediateNode(intermediateNodeList, obj.getTitle());
					if (parentNode == null)
						parentNode = this;

					//
					// Playlist items are a special case - they are really
					// containers, logically. This code converts these items to
					// equivalent containers, and supports fetching their
					// contents via an HTTP-get request.
					//
					// MusicMatch's UPnP playlist items have UPnP class of
					// 'object.item' as of 10/10/2004, so there's a bit of
					// logic required to check the item to see if it's a
					// playlist
					//
					// Intel NMPR requirements now mandate that playlists be
					// represented by UPnP playlist containers, so the playlist
					// items can be more properly represented as UPnP items
					// within
					// those containers, and metadata can be retrieved via the
					// normal browse mechanism.
					//
					if (obj.isContainer()) {
						MediaNode childNode = new MediaNode(mediaDevice, obj);
						// logger.fine("Adding child container node");
						parentNode.add(childNode);
					} else if (obj.getClass().equals("object.item.playlistItem")) {
						CDSPlaylistItem playlistItem = (CDSPlaylistItem) obj;

						CDSPlaylistContainer playlistContainer = playlistItem.convertToPlaylistContainer();

						MediaNode childNode = new MediaNode(mediaDevice, playlistContainer);
						// logger.fine("Adding child container node");
						parentNode.add(childNode);
					} else {
						CDSItem item = (CDSItem) obj;

						//
						// If item's resource is a MPEGURL, also do conversion
						// to playlist
						// container (MusicMatch patch). Don't do this for audio
						// broadcasts TODO: Make this hack more
						// MusicMatch-specific!
						//
						CDSResource itemResource = item.getResource(0);
						if ((itemResource != null) && (!(item instanceof CDSAudioBroadcast))
								&& itemResource.getProtocolInfo().equals("http-get:*:audio/mpegurl:*")) {
							CDSPlaylistContainer playlistContainer = item.convertToPlaylistContainer();

							MediaNode childNode = new MediaNode(mediaDevice, playlistContainer);
							// logger.fine("Adding child container node");
							parentNode.add(childNode);
						} else {
							// logger.fine("Adding item");
							parentNode.addItem(item);
						}
					}

					//
					// If current object doesn't have a creator, and one of the
					// child
					// items does have a creator/artist resource, set the
					// creator
					// of the current node from it
					//
					if (upnpObj.getCreator() == null) {
						if (obj.getCreator() != null)
							upnpObj.setCreator(obj.getCreator());
					}

				} // end for child objs

			} // end if( objList != null )

			// Update intermediate node titles with item counts
			if (intermediateNodeList != null)
				updateIntermediateNodeTitles(intermediateNodeList);

			needsRefresh = false;
		} else {
			logger.fine("Using cached node info");
		}

		return true;
	}

	public void expandPlaylistItem() {

	}

	static String[] intermediateNodeNames = { "1-9...", "A...", "B...", "C...", "D...", "E...", "F...", "G...", "H...",
			"I...", "J...", "K...", "L...", "M...", "N...", "O...", "P...", "Q...", "R...", "S...", "T...", "U...",
			"V...", "W...", "X...", "Y...", "Z...", "Other..." };

	/**
	 * Create set of intermediate nodes for current node. Return a list of them.
	 */
	public ArrayList createIntermediateNodes() {
		ArrayList nodeList = new ArrayList();

		for (int n = 0; n < intermediateNodeNames.length; n++) {
			CDSStorageFolder folder = new CDSStorageFolder();
			folder.setTitle(intermediateNodeNames[n]);

			MediaNode childNode = new MediaNode(mediaDevice, folder);
			add(childNode);
			childNode.setNeedsRefresh(false);

			nodeList.add(childNode);
		}

		return nodeList;
	}

	/**
	 * Set the title for all intermediate nodes. The number of objects contained
	 * by the node is appended to the node name. Ends up looking like:
	 *
	 * 1-9...(20 items) A... (208 items) B... (302 items)
	 *
	 */
	public void updateIntermediateNodeTitles(ArrayList intermediateNodeList) {
		for (int n = 0; n < intermediateNodeList.size(); n++) {
			MediaNode node = (MediaNode) intermediateNodeList.get(n);
			CDSObject obj = (CDSObject) node.getUserObject();
			obj.setTitle(obj.getTitle() + " ( " + node.getChildCount() + " )");
		}
	}

	public MediaNode findIntermediateNode(ArrayList intermediateNodeList, String match) {
		char[] matchChars = match.toUpperCase().trim().toCharArray();
		if (Character.isDigit(matchChars[0])) // Begins with digit?
		{
			return (MediaNode) intermediateNodeList.get(0);
		} else {
			int offset = (int) (matchChars[0] - 'A');
			if ((offset >= 0) && (offset < 26)) // Begins with char?
			{
				return (MediaNode) intermediateNodeList.get(offset + 1);
			} else {
				// Not digit or character - assign to 'Other' category. Under
				// the Twonkyvision server, for example, under 'Albums' there is
				// an '- All -' entry that ends up getting put in the 'Other'
				// category due to the leading '-'
				return (MediaNode) intermediateNodeList.get(27);
			}
		}
	}

}
