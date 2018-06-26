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
 *  File: $RCSfile: PlayQueue.java,v $
 *
 */
package domoNetWS.techManager.upnpManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Vector;
import java.util.Collections;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;

import com.cidero.upnp.*;
import com.cidero.util.TwonkyPlaylistConverter;
import com.cidero.control.*;

/**
 * Dynamic Play Queue class. Supports jukebox-style playlist control on the
 * control point. Items can be added to the play queue on a 'Play Once' or 'Play
 * Repeatedly' basis, and methods are included to add multiple items in one
 * shot. The queue can be shuffled, sorted, etc...
 *
 * This class implements it's own shuffle and repeat logic since the current
 * state of support for these features is patchy on the different UPnP-compliant
 * media renderers (as of 11/10/2004). The idea is to provide the most
 * consistent behaviour possible across a range of devices.
 *
 * This object implements the ListModel interface so it can be displayed in a
 * Swing JList container
 *
 */
public class PlayQueue extends AbstractListModel {
	private static Logger logger = Logger.getLogger("com.cidero.control");

	Vector itemList = new Vector();

	String playMode = AVTransport.PLAY_MODE_NORMAL;

	static int MEMBERSHIP_PERMANENT = 1;
	static int MEMBERSHIP_TRANSIENT = 2; // One-time play
	int insertItemMembershipType = MEMBERSHIP_PERMANENT;

	public final static int MODE_BUILDING_LIST = 0;
	public final static int MODE_JUKEBOX = 1;
	int insertItemMode = MODE_BUILDING_LIST;

	public final static int PRIORITY_NORMAL = 0;
	public final static int PRIORITY_HIGH = 1;

	// Index of current track
	int currIndex = 0;

	// Reference to item obj for current track
	// PlayQueueElem currItem;

	String friendlyName = "None";

	/**
	 * Basic constructor.
	 */
	public PlayQueue() {
	}

	public PlayQueue(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	Vector activeItemListenerList = new Vector();

	public void addActiveItemListener(ActiveItemListener listener) {
		activeItemListenerList.add(listener);
	}

	public void fireActiveItemChanged() {
		for (int n = 0; n < activeItemListenerList.size(); n++) {
			ActiveItemListener listener = (ActiveItemListener) activeItemListenerList.get(n);
			listener.activeItemChanged(currIndex);
		}
	}

	/**
	 * Set the play mode for the queue.
	 *
	 * @param playMode
	 *            AVTransport.PLAY_MODE_NORMAL or
	 *            AVTransport.PLAY_MODE_REPEAT_ONE or
	 *            AVTransport.PLAY_MODE_REPEAT_ALL
	 *
	 *            Note: Some MediaRenderer devices support these play modes,
	 *            some don't. Hence the support for it here to provide
	 *            consistent behavior across multiple devices
	 */
	public void setPlayMode(String playMode) {
		this.playMode = playMode;
	}

	/**
	 * Set the position for added items.
	 *
	 * @param insertItemMode
	 *            Play queue insertion mode. Values are: BUILDING_LIST
	 *            Insertions are made at end of queue JUKEBOX Insertions are
	 *            made w/respect to current queue position. If multiple items
	 *            are added in JUKEBOX mode, earlier items are played first
	 *            (FIFO scheduling).
	 */
	public void setInsertItemMode(int mode) {
		this.insertItemMode = mode;
	}

	/**
	 * Set the membership type for added items. This routine should be called
	 * prior to the addItem() call to set the play type that is associated with
	 * each subsequently added item.
	 *
	 * @param membershipType
	 *            PERMANENT (default) or TRANSIENT (play once then remove from
	 *            list
	 */
	public void setInsertItemMembershipType(int membershipType) {
		this.insertItemMembershipType = membershipType;
	}

	/**
	 * Add a single item to the play queue, using current playQueue values of
	 * insertMode and membershipType . The location where the item is added to
	 * depends on the
	 */
	public synchronized void insertItem(CDSObject item) {
		insertItem(item, insertItemMode, insertItemMembershipType);
	}

	/**
	 * Add a single item to the play queue. The location where the item is added
	 * to depends on the
	 *
	 * @param membershipType
	 *            MEMBERSHIP_PERMANENT (default) or MEMBERSHIP_TRANSIENT (play
	 *            once then remove from list)
	 * @param mode
	 *            Play queue insertion mode. Values are: BUILDING_LIST
	 *            Insertions are made at end of queue JUKEBOX Insertions are
	 *            made w/respect to current queue position. If multiple items
	 *            are added in JUKEBOX mode, earlier items are played first
	 *            (FIFO scheduling).
	 */
	public synchronized void insertItem(CDSObject item, int mode, int membershipType) {
		PlayQueueElem qItem = new PlayQueueElem(item);

		// PERMANENT or TRANSIENT (one-time-play)
		qItem.setMembershipType(membershipType);

		if (mode == MODE_BUILDING_LIST) {
			qItem.setPriority(PRIORITY_NORMAL);

			// Append to end of queue
			itemList.add(qItem);
			fireIntervalAdded(this, itemList.size() - 1, itemList.size() - 1);

			// If need FRONT OF QUEUE Mode, use this
			// case FRONT_OF_QUEUE:
			// itemList.add( 0, item );
			// fireIntervalAdded( this, 0, 0 );

		} else if (mode == MODE_JUKEBOX) {
			//
			// Jukebox mode assumes that user will probably be adding tracks
			// while lower-priority 'base' playlist is running (and wants them
			// to play with higher priority)
			//
			qItem.setPriority(PRIORITY_HIGH);

			PlayQueueElem tmp = null;
			int index = currIndex;

			if (index < 0) {
				// Playback not underway and nothing selected - insert at
				// start of queue
				itemList.add(0, qItem);
			} else {
				// Starting at current position, search for next item of low
				// priority, and insert just before that (HIGH_PRIORITY items
				// that
				// have already been cued up will play before this new item).
				int n;
				for (n = 0; n < itemList.size(); n++) {
					tmp = (PlayQueueElem) itemList.get(index);

					// If currItem, and it's playing, skip over it (don't
					// preempt it)
					if ((index == currIndex) && tmp.isBusy()) {
						logger.fine("Skipping busy current item");
					} else if (tmp.getPriority() != PRIORITY_HIGH) {
						break;
					}

					index++;
					if (index >= itemList.size())
						index = 0;
				}

				// Special case - single item playlist with currently playing
				// track.
				// Insert *after* current track
				if ((itemList.size() == 1) && tmp.isBusy())
					itemList.add(1, qItem);
				else
					itemList.add(index, qItem);
			}

			fireIntervalAdded(this, index, index);
		} else {
			logger.warning("Error - bad mode in insertItem");
			qItem.setPriority(PRIORITY_NORMAL);
		}

	}

	/**
	 * Remove single item from play queue
	 */
	public synchronized void remove(int index) {
		itemList.remove(index);

		fireContentsChanged(this, 0, itemList.size() - 1);
	}

	/**
	 * Move an item in the list
	 */
	public synchronized void move(int fromIndex, int toIndex) {
		if (fromIndex == toIndex)
			return;

		if (fromIndex < 0 || toIndex < 0 || fromIndex >= itemList.size() || toIndex > itemList.size()) {
			return;
		}

		PlayQueueElem qItem = (PlayQueueElem) itemList.remove(fromIndex);

		if (fromIndex > toIndex) {
			itemList.insertElementAt(qItem, toIndex);
		} else {
			itemList.insertElementAt(qItem, toIndex - 1);
		}

		// Adjust now playing index (slightly tricky since curr track is
		// allowed to move in list while it's playing)
		if ((fromIndex < currIndex) && (toIndex > currIndex))
			currIndex--;
		else if ((fromIndex > currIndex) && (toIndex <= currIndex))
			currIndex++;
		else if ((fromIndex == currIndex) && (toIndex < currIndex))
			currIndex = toIndex;
		else if (fromIndex == currIndex)
			currIndex = toIndex - 1;

		fireContentsChanged(this, 0, itemList.size() - 1);
	}

	/**
	 * Clear all items from play queue
	 */
	public synchronized void clear() {
		int size = itemList.size();
		itemList.clear();
		if (size > 0)
			fireIntervalRemoved(this, 0, size - 1);
		currIndex = 0;
	}

	public synchronized boolean isEmpty() {
		return (itemList.size() == 0);
	}

	int markIndex = -1;

	public void setMarkPosition(int index) {
		markIndex = index;
	}

	public int getMarkPosition() {
		return markIndex;
	}

	/**
	 * Set the current queue position.
	 */
	public synchronized void setCurrentPosition(int index) {
		if ((index < 0) || (index >= itemList.size()))
			logger.warning("index argument (" + index + ") out of bounds");
		else
			currIndex = index;

		// fireActiveItemChanged(); selected is not same as active (busy)
	}

	/**
	 * Return the current queue position
	 */
	public synchronized int getCurrentPosition() {
		return currIndex;
	}

	/**
	 * Return size of play queue
	 */
	public synchronized int size() {
		return itemList.size();
	}

	/**
	 * Shuffle the list of tracks in the queue. If there is a item marked as
	 * busy (currently playing), the queue position is updated to still point to
	 * it. If there are any PRIORITY_HIGH items (added in Jukebox mode), their
	 * priority is lowered to PRIORITY_NORMAL (Jukebox sequencing is lost after
	 * shuffle, so the PRIORITY tag is useless)
	 */
	public synchronized void shuffle() {
		Collections.shuffle(itemList);

		currIndex = 0;
		for (int n = 0; n < itemList.size(); n++) {
			PlayQueueElem qItem = (PlayQueueElem) itemList.get(n);
			if (qItem.isBusy())
				currIndex = n;

			qItem.setPriority(PRIORITY_NORMAL);
		}

		fireContentsChanged(this, 0, itemList.size() - 1);
		fireActiveItemChanged();
	}

	public synchronized void albumShuffle() {
		// Make an CDSObjectList - object supports album shuffle
		CDSObjectList objList = new CDSObjectList();
		for (int n = 0; n < itemList.size(); n++) {
			PlayQueueElem qItem = (PlayQueueElem) itemList.get(n);
			CDSObject obj = qItem.getCDSObject();
			objList.add(obj);
		}

		objList.shuffleByAlbum();

		// Can be less items in post-shuffled list if there were non-music
		// tracks in queue - reset play queue item list

		CDSObject busyObj = null;
		if (currIndex >= 0) {
			PlayQueueElem qItem = (PlayQueueElem) itemList.get(currIndex);
			if (qItem.isBusy())
				busyObj = qItem.getCDSObject();
		}

		itemList.clear();
		for (int n = 0; n < objList.size(); n++) {
			CDSObject obj = objList.getObject(n);
			insertItem(objList.getObject(n));

			// Restore busy flag in item if track was playing during shuffle
			if (obj == busyObj) {
				PlayQueueElem qItem = (PlayQueueElem) itemList.get(n);
				qItem.setBusy(true);
				currIndex = n;
			}
		}

		fireContentsChanged(this, 0, itemList.size() - 1);
		fireActiveItemChanged();
	}

	/**
	 * Clear the high priority flags for all items in the queue. This is done
	 * when the user issues a stop request since Jukebox-mode high priority
	 * sequencing flags are only needed when songs are inserted into a currently
	 * playing queue. (with the current logic at least)
	 */
	public synchronized void clearHighPriorityFlags() {
		for (int n = 0; n < itemList.size(); n++) {
			PlayQueueElem qItem = (PlayQueueElem) itemList.get(n);
			qItem.setPriority(PRIORITY_NORMAL);
		}

		fireContentsChanged(this, 0, itemList.size() - 1);
		fireActiveItemChanged();
	}

	/**
	 * Set the play queue position to the first item (item at current queue
	 * pointer position)
	 *
	 * @return CDSObject reference, or null if empty queue or invalid position
	 */
	public synchronized CDSObject getFirstItem() {
		if (itemList.size() > 0) {
			if (currIndex < 0 || currIndex >= itemList.size()) {
				logger.warning("Invalid queue index");
				return null;
			}

			PlayQueueElem firstItem = (PlayQueueElem) itemList.get(currIndex);

			// Mark as busy and let listeners know the list model changed
			firstItem.setBusy(true);
			fireActiveItemChanged();
			fireContentsChanged(this, 0, itemList.size() - 1);

			return firstItem.getCDSObject();
		}
		return null;
	}

	/**
	 * Get next item in play queue. If item at current position is of type
	 * PLAY_ONCE, it is removed, UNLESS the current play mode is REPEAT_ONE, in
	 * which case it is left on the queue and repeated.
	 */
	public synchronized CDSObject getNextItem() {
		PlayQueueElem currItem;
		CDSObject cdsObj = null;

		if ((currIndex < 0) || (currIndex >= itemList.size()))
			return null;

		int lastCurrIndex = currIndex;

		logger.fine("getNextItem: Entered, currIndex = " + currIndex + " playMode = " + playMode);

		currItem = (PlayQueueElem) itemList.get(currIndex);

		if (playMode.equals(AVTransport.PLAY_MODE_REPEAT_ONE)) {
			return currItem.getCDSObject();
		}

		// Remove PLAY_ONCE items once they have played once
		if (currItem.getMembershipType() == MEMBERSHIP_TRANSIENT)
			itemList.remove(currIndex);
		else {
			currItem.setBusy(false); // No longer busy

			// If item is high priority, lower priority back to normal now that
			// it has been visited
			if (currItem.getPriority() == PRIORITY_HIGH)
				currItem.setPriority(PRIORITY_NORMAL);
		}

		if (++currIndex == itemList.size()) {
			if (playMode.equals(AVTransport.PLAY_MODE_REPEAT_ALL))
				currIndex = 0;
			else
				currIndex = -1;
		}

		if (currIndex == -1) {
			fireContentsChanged(this, 0, itemList.size() - 1);
			return null;
		}

		currItem = (PlayQueueElem) itemList.get(currIndex);

		if (currItem == null) {
			fireContentsChanged(this, 0, itemList.size() - 1);
			return null;
		}

		// Mark as busy and let listeners know the list model changed
		currItem.setBusy(true);
		fireContentsChanged(this, lastCurrIndex, currIndex);
		fireActiveItemChanged();

		return currItem.getCDSObject();
	}

	/**
	 * Get next item in play queue. If item at current position is of type
	 * PLAY_ONCE, it is removed, UNLESS the current play mode is REPEAT_ONE, in
	 * which case it is left on the queue and repeated.
	 */
	public synchronized CDSObject getPrevItem() {
		PlayQueueElem currItem;
		CDSObject cdsObj = null;

		if ((currIndex < 0) || (currIndex >= itemList.size()))
			return null;

		int lastCurrIndex = currIndex;

		logger.fine("getNextItem: Entered, currIndex = " + currIndex + " playMode = " + playMode);

		currItem = (PlayQueueElem) itemList.get(currIndex);

		if (playMode.equals(AVTransport.PLAY_MODE_REPEAT_ONE)) {
			return currItem.getCDSObject();
		}

		currItem.setBusy(false); // No longer busy

		// If item is high priority, lower priority back to normal now that
		// it has been visited
		if (currItem.getPriority() == PRIORITY_HIGH)
			currItem.setPriority(PRIORITY_NORMAL);

		if (--currIndex == 0) {
			if (playMode.equals(AVTransport.PLAY_MODE_REPEAT_ALL))
				currIndex = itemList.size() - 1;
			else
				currIndex = -1;
		}

		if (currIndex == -1) {
			fireContentsChanged(this, 0, itemList.size() - 1);
			return null;
		}

		currItem = (PlayQueueElem) itemList.get(currIndex);

		if (currItem == null) {
			fireContentsChanged(this, 0, itemList.size() - 1);
			return null;
		}

		// Mark as busy and let listeners know the list model changed
		currItem.setBusy(true);
		fireContentsChanged(this, lastCurrIndex, currIndex);
		fireActiveItemChanged();

		return currItem.getCDSObject();
	}

	/**
	 * Clear the busy flag for the current item. This is used when a playback is
	 * halted by the user, where we want to leave the current item pointer in
	 * the same place but no longer want it to show up in the 'now playing'
	 * color in the play queue UI.
	 */
	public synchronized void clearCurrentItemBusyFlag() {
		/*
		 * Just clear busy flag for all items now (trying to fix race condition)
		 *
		 * @todo Revert to more efficient implementation below at some point
		 */
		for (int n = 0; n < itemList.size(); n++) {
			PlayQueueElem item = (PlayQueueElem) itemList.get(n);
			item.setBusy(false);
		}

		fireContentsChanged(this, 0, itemList.size() - 1);

		/* Older, more efficient implementation (not looking at whole list) */

		/*
		 * if( (currIndex < 0) || (currIndex >= itemList.size()) ) return;
		 * 
		 * PlayQueueElem currItem = (PlayQueueElem)itemList.get( currIndex );
		 * 
		 * currItem.setBusy(false); fireContentsChanged( this, currIndex,
		 * currIndex );
		 */
	}

	/**
	 * Find next item in queue. This is similar to getNextItem, but the queue
	 * pointer is not updated.
	 */
	public synchronized CDSObject findNextItem() {
		PlayQueueElem nextItem;
		CDSObject cdsObj = null;

		// Check for REPEAT_ONE play mode special case

		if (currIndex == -1) {
			logger.fine("findNextItem: End of play queue found (returning null)");
			return null;
		}

		nextItem = (PlayQueueElem) itemList.get(currIndex);
		if (playMode.equals(AVTransport.PLAY_MODE_REPEAT_ONE))
			return nextItem.getCDSObject();

		int nextIndex = currIndex;
		if (++nextIndex >= itemList.size()) {
			if (playMode.equals(AVTransport.PLAY_MODE_REPEAT_ALL))
				nextIndex = 0;
			else
				nextIndex = -1;
		}

		if (nextIndex == -1)
			return null;

		// logger.info("currIndex, nextIndex = " + currIndex + ", " + nextIndex
		// );

		nextItem = (PlayQueueElem) itemList.get(nextIndex);

		if (nextItem == null) // Should be impossible...trying to track bug
		{
			logger.warning("findNextItem: Logic error!");
			return null;
		}

		return nextItem.getCDSObject();
	}

	/**
	 * Check if next song is start of an album. To qualify, the next 4 songs
	 * have to have the same album name, different from the current song
	 */
	public boolean isNextSongStartOfAlbum() {
		PlayQueueElem currItem;
		PlayQueueElem nextItem;
		CDSObject cdsObj = null;

		currItem = (PlayQueueElem) itemList.get(currIndex);
		cdsObj = currItem.getCDSObject();
		if (!(cdsObj instanceof CDSMusicTrack)) {
			logger.info("Not music track - no match!");
			return false;
		}
		CDSMusicTrack currTrack = (CDSMusicTrack) cdsObj;
		String currAlbum = currTrack.getAlbum();

		int nextIndex = currIndex;

		CDSMusicTrack[] nextTracks = new CDSMusicTrack[4];
		int n;

		for (n = 0; n < 4; n++) {
			if (++nextIndex >= itemList.size())
				break;

			nextItem = (PlayQueueElem) itemList.get(nextIndex);
			cdsObj = nextItem.getCDSObject();
			if (!(cdsObj instanceof CDSMusicTrack)) {
				logger.info("One of next 4 items not music track - no match!");
				return false;
			}

			nextTracks[n] = (CDSMusicTrack) cdsObj;
			if (n == 0) {
				if (nextTracks[n].getAlbum().equals(currAlbum)) {
					logger.info("No - next track same album as current one");
					return false;
				}
			} else if (n > 0) {
				if (!nextTracks[n].getAlbum().equals(nextTracks[0].getAlbum())) {
					logger.info("Album mismatch - next track not track 1 of 4");
					return false;
				}
			}
		}

		if (n == 4) {
			logger.info("Next track is 1st track of album");
			return true;
		}

		return false;
	}

	/**
	 * Methods for ListModel interface
	 */
	public synchronized Object getElementAt(int index) {
		return itemList.get(index);
	}

	public synchronized int getSize() {
		return itemList.size();
	}

	public void saveToDidlLite(File file) {
		try {
			if (!file.getCanonicalPath().endsWith(".xml"))
				file = new File(file.getCanonicalPath() + ".xml");

			logger.info("Saving queue to DIDL-Lite file " + file.getCanonicalPath());

			FileOutputStream fileOutputStream = new FileOutputStream(file);

			PrintWriter writer = new PrintWriter(new BufferedOutputStream(fileOutputStream));

			writer.println(
					"<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">");

			CDSFilter filter = new CDSFilter("*");

			for (int n = 0; n < itemList.size(); n++) {
				PlayQueueElem qItem = (PlayQueueElem) itemList.get(n);

				CDSObject obj = qItem.getCDSObject();

				writer.println(obj.toXML(filter));
			}

			writer.append("</DIDL-Lite>\n");
			writer.flush();
			writer.close();
		} catch (Exception e) {
			logger.warning("Exception saving play queue to file");
		}

	}

	public void loadFromDidlLite(File file) {
		try {
			BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(file));

			CDSObjectList objList = new CDSObjectList(inStream);
			if (objList != null) {
				for (int n = 0; n < objList.size(); n++) {
					insertItem(objList.getObject(n));
				}
			}

			inStream.close();
		} catch (Exception e) {
			logger.warning("Exception loading play queue from file");
		}
	}

	/**
	 * Export a play queue to an M3U file, using the Twonky Db file to translate
	 * the virtual resources to real pathnames in the M3U file
	 * 
	 * @return true on success, false on failure
	 */
	public boolean exportToM3U(String twonkyDbFile, File m3uFile) {
		try {
			if (!m3uFile.getCanonicalPath().toLowerCase().endsWith(".m3u"))
				m3uFile = new File(m3uFile.getCanonicalPath() + ".m3u");

			CDSObjectList objList = new CDSObjectList();

			for (int n = 0; n < itemList.size(); n++) {
				PlayQueueElem qItem = (PlayQueueElem) itemList.get(n);
				CDSObject obj = qItem.getCDSObject();
				objList.add(obj);
			}

			TwonkyPlaylistConverter converter = new TwonkyPlaylistConverter(twonkyDbFile);

			converter.objListToPlaylist(objList, m3uFile);
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	/**
	 * Convert to the string representation
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("Dynamic playlist:\n");

		for (int n = 0; n < itemList.size(); n++) {
			PlayQueueElem item = (PlayQueueElem) itemList.get(n);
			CDSObject obj = item.getCDSObject();
			buf.append(obj.getTitle() + " Pri:" + item.getPriority() + "\n");
		}

		return buf.toString();
	}

	/**
	 * Simple test program
	 */
	public static void main(String[] args) {
		try {
			PlayQueue playQueue = new PlayQueue();

			for (int n = 0; n < 4; n++) {
				CDSObject obj = new CDSItem();
				obj.setTitle("Item_" + n);
				playQueue.insertItem(obj);
			}

			System.out.println("PlayQueue:\n" + playQueue.toString());

			playQueue.shuffle();
			System.out.println("PlayQueue after shuffle:\n" + playQueue.toString());

			playQueue.setInsertItemMode(PlayQueue.MODE_JUKEBOX);

			playQueue.getFirstItem();
			playQueue.getNextItem();

			for (int n = 0; n < 2; n++) {
				CDSObject obj = new CDSItem();
				obj.setTitle("JukeItem_" + n);
				playQueue.insertItem(obj);
			}

			System.out.println("PlayQueue after JukeAdd:\n" + playQueue.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
