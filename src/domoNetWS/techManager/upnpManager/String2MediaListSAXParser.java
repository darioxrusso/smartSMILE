/* This file is part of DOMONET.

Copyright (C) 2006-2007 ISTI-CNR (Dario Russo)

DOMONET is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

DOMONET is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with DOMONET; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA */

package domoNetWS.techManager.upnpManager;

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import domoML.domoDataType.*;

/**
 * Implements a SAX parser for reading an xml input stream from which take the
 * domoML devices description.
 */
public class String2MediaListSAXParser extends DefaultHandler {

	MediaList mediaList = new MediaList();
	String currentPath = "";
	String currentParentName;
	String currentContainerName;
	String mainTag = "";

	/** The name of the container */
	private String containerName = "";

	/** A pretty name for the container */
	private String containerPrettyName = "";

	/** A description of the container */
	private String containerDescription = "";

	/** The parent name of the container (if any) */
	private String containerParent = "";

	/** The name on the media */
	private String contentName = "";

	/** The title on the media */
	private String contentTitle = "";

	/** The class of the media */
	private String contentType = "";

	/** The author of the media */
	private String contentAuthor = "";

	/** The genre of the media */
	private String contentGenre = "";

	/** A description for the media */
	private String contentDescription = "";

	/** The parent for the media */
	private String contentParent = "";

	/**
	 * An enumeration of possible kind of text that can be found during parsing
	 */
	private static enum WaitForText {
		NULL, CONTAINER_TITLE, CONTAINER_DESCRIPTION, CONTENT_TITLE
	};

	private WaitForText waitForText = WaitForText.NULL;

	/** Handler at the beginning of a tag. */
	public final void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		if (name.equalsIgnoreCase("container")) {
			mainTag = name;
			containerName = atts.getValue("id");
			containerParent = atts.getValue("parentID");
		} else if (qName.equalsIgnoreCase("dc:title") && mainTag.equalsIgnoreCase("container"))
			waitForText = WaitForText.CONTAINER_TITLE;
		else if (qName.equalsIgnoreCase("dc:description") && mainTag.equalsIgnoreCase("container"))
			waitForText = WaitForText.CONTAINER_DESCRIPTION;
		else if (name.equalsIgnoreCase("item")) {
			mainTag = name;
			contentName = atts.getValue("id");
			contentParent = atts.getValue("parentID");
		} else if (qName.equalsIgnoreCase("dc:title") && mainTag.equalsIgnoreCase("item")) {
			waitForText = WaitForText.CONTENT_TITLE;
		}
	}

	/** Handler at the ending of a tag. */
	public final void endElement(final String uri, final String localName, final String qName) {
		if (localName.equalsIgnoreCase("container")) {
			try {
				mediaList.addMediaContainer(currentPath, containerName, containerPrettyName, containerDescription,
						containerParent);
				containerName = "";
				containerPrettyName = "";
				containerDescription = "";
				containerParent = "";
				currentPath = mediaList.getMediaContainerParentName(currentPath);
				mainTag = "";
			} catch (NotExistingMediaContainer e) {
				e.printStackTrace();
			}
		} else if (localName.equalsIgnoreCase("item")) {
			try {
				mediaList.addMediaContent(currentPath, contentName, contentTitle, contentType, contentAuthor,
						contentGenre, contentDescription, contentParent);
				System.out.println("COSTRUISCO CON PARENT: " + contentParent);
				contentName = "";
				contentTitle = "";
				contentType = "";
				contentAuthor = "";
				contentGenre = "";
				contentDescription = "";
				contentParent = "";
				mainTag = "";
			} catch (NotExistingMediaContainer e) {
				e.printStackTrace();
			}
		}
	}

	final public void characters(final char[] ch, final int start, final int len) {
		final String text = new String(ch, start, len);
		if (waitForText.equals(WaitForText.CONTAINER_TITLE))
			containerPrettyName = text;
		else if (waitForText.equals(WaitForText.CONTAINER_DESCRIPTION))
			containerDescription = text;
		else if (waitForText.equals(WaitForText.CONTENT_TITLE))
			contentTitle = text;
		waitForText = WaitForText.NULL;
	}

	/**
	 * Gets the result of parser
	 * 
	 * @return the <code>MediaList</code> parsed
	 */
	public final MediaList getMediaList() {
		return mediaList;
	}
}
