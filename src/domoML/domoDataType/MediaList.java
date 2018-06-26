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

package domoML.domoDataType;

import java.util.*;
import java.lang.reflect.*;

/** Collects a list of containers and contents */
public class MediaList {

	/** The list containing the sub containers */
	private HashMap<String, MediaContainer> containerList = new HashMap<String, MediaContainer>();

	/** The list containing the media contents */
	private HashMap<String, MediaContent> contentList = new HashMap<String, MediaContent>();

	/**
	 * Adds a media container in the media list
	 * 
	 * @param name
	 *            the name of the new mediaContainer
	 * @param prettyName
	 *            the prettyName of the new mediaContainer
	 * @param description
	 *            the description of the new mediaContainer
	 * @param parent
	 *            the parent of the new mediaContainer
	 */
	public final void addMediaContainer(final String name, final String prettyName, final String description,
			final String parent) {
		containerList.put(name, new MediaContainer(name, prettyName, description, parent));
	}

	/**
	 * Adds a media container in the media list
	 * 
	 * @param path
	 *            the path of the container where to add the mediaContainer
	 * @param name
	 *            the name of the new mediaContainer
	 * @param prettyName
	 *            the prettyName of the new mediaContainer
	 * @param description
	 *            the description of the new mediaContainer
	 * @param parent
	 *            the parent of the new mediaContainer
	 */
	public final void addMediaContainer(final String path, final String name, final String prettyName,
			final String description, final String parent) throws NotExistingMediaContainer {
		if (!path.isEmpty()) {
			String[] pathSplit = path.split("/");
			int pathSplitSize = Array.getLength(pathSplit);
			try {
				MediaContainer currentMediaContainer = getMediaContainer(pathSplit[0]);
				for (int i = 1; i < pathSplitSize; i++)
					currentMediaContainer = currentMediaContainer.getMediaContainer(pathSplit[i]);
				currentMediaContainer.addMediaContainer(name, prettyName, description, parent);
			} catch (NotExistingMediaContainer e) {
				throw new NotExistingMediaContainer();
			}
		} else
			addMediaContainer(name, prettyName, description, parent);
	}

	/**
	 * Gets an existing <code>MediaContainer</code>
	 * 
	 * @param name
	 *            the name of the <code>MediaContainer</code> to get
	 * @return the <code>MediaContainer</code> requested
	 */
	public final MediaContainer getMediaContainer(final String name) throws NotExistingMediaContainer {
		MediaContainer mediaContainer = containerList.get(name);
		if (mediaContainer == null)
			throw new NotExistingMediaContainer();
		return mediaContainer;
	}

	/**
	 * Adds a media content in the media list
	 * 
	 * @param name
	 *            the name of the new mediaContent
	 * @param title
	 *            the title of the new mediaContainer
	 * @param type
	 *            the type of the new mediaContainer
	 * @param author
	 *            the author of the new mediaContainer
	 * @param genre
	 *            the genre of the new mediaContainer
	 * @param description
	 *            the description of the new mediaContainer
	 */
	public final void addMediaContent(final String name, final String title, final String type, final String author,
			final String genre, final String description, final String parent) {
		contentList.put(name, new MediaContent(name, title, type, author, genre, description, parent));
	}

	/**
	 * Adds a media content in the media list
	 * 
	 * @param path
	 *            the path of the content where to add the mediaContent
	 * @param name
	 *            the name of the new mediaContent
	 * @param prettyName
	 *            the prettyName of the new mediaContent
	 * @param description
	 *            the description of the new mediaContent
	 * @param parent
	 *            the parent of the new mediaContent
	 */
	public final void addMediaContent(final String path, final String name, final String title, final String type,
			final String author, final String genre, final String description, final String parent)
					throws NotExistingMediaContainer {
		if (!path.isEmpty()) {
			try {
				MediaContainer mediaContainer = getMediaContainer(path);
				mediaContainer.addMediaContent(name, title, type, author, genre, description, parent);
			} catch (NotExistingMediaContainer e) {
				throw new NotExistingMediaContainer();
			}
		} else
			addMediaContent(name, title, type, author, genre, description, parent);
	}

	/**
	 * Gets an existing mediaContent
	 * 
	 * @param name
	 *            the name of the <code>MediaContent</code> to get
	 * @return the <code>MediaContent</code> requested
	 */
	private final MediaContent getMediaContent(final String title) throws NotExistingMediaContent {
		MediaContent mediaContent = contentList.get(title);
		if (mediaContent == null)
			throw new NotExistingMediaContent();
		return mediaContent;
	}

	public final String toString() {
		StringBuffer returnString = new StringBuffer("<mediaList>");
		Iterator<MediaContainer> mediaContainerIterator = containerList.values().iterator();
		while (mediaContainerIterator.hasNext()) {
			returnString.append(mediaContainerIterator.next().toString());
		}
		Iterator<MediaContent> mediaContentIterator = contentList.values().iterator();
		while (mediaContentIterator.hasNext()) {
			returnString.append(mediaContentIterator.next().toString());
		}
		returnString.append("</mediaList>");
		return returnString.toString();
	}

	/**
	 * Gets the parent name of a <code>MediaContainer</code>
	 * 
	 * @param mediaContainerPath
	 *            the path of the container
	 * @return the name of the parent
	 */
	public final String getMediaContainerParentName(String mediaContainerPath) throws NotExistingMediaContainer {
		if (!mediaContainerPath.isEmpty()) {
			try {
				return getMediaContainer(mediaContainerPath).getParent();
			} catch (NotExistingMediaContainer e) {
				throw new NotExistingMediaContainer();
			}
		} else
			return "";
	}
}

/** The container of media contents (as a draw) */
class MediaContainer {

	/** The list containing the sub containers */
	private HashMap<String, MediaContainer> containerList = new HashMap<String, MediaContainer>();

	/** The list containing the media contents */
	private HashMap<String, MediaContent> contentList = new HashMap<String, MediaContent>();

	/** The name of the container */
	private String name;

	/** A pretty name for the container */
	private String prettyName;

	/** A description of the container */
	private String description;

	/** The parent name of the container (if any) */
	private String parent;

	/**
	 * Creates a media container
	 * 
	 * @param name
	 *            the name of the new mediaContainer
	 * @param prettyName
	 *            the prettyName of the new mediaContainer
	 * @param description
	 *            the description of the new mediaContainer
	 * @param parent
	 *            the parent of the new mediaContainer
	 */
	public MediaContainer(final String name, final String prettyName, final String description, final String parent) {
		setName(name);
		setPrettyName(prettyName);
		setDescription(description);
		setParent(parent);
	}

	/**
	 * Adds a media container
	 * 
	 * @param name
	 *            the name of the new mediaContainer
	 * @param prettyName
	 *            the prettyName of the new mediaContainer
	 * @param description
	 *            the description of the new mediaContainer
	 * @param parent
	 *            the parent of the new mediaContainer
	 */
	public final void addMediaContainer(final String name, final String prettyName, final String description,
			final String parent) {
		containerList.put(name, new MediaContainer(name, prettyName, description, parent));
	}

	/**
	 * Gets an existing mediaContainer
	 * 
	 * @param name
	 *            the name of the <code>MediaContainer</code> to get
	 * @return the <code>MediaContainer</code> requested
	 */
	public final MediaContainer getMediaContainer(final String name) throws NotExistingMediaContainer {
		MediaContainer mediaContainer = containerList.get(name);
		if (mediaContainer == null)
			throw new NotExistingMediaContainer();
		return mediaContainer;
	}

	/**
	 * Adds a media content in the media list
	 * 
	 * @param name
	 *            the name of the new mediaContent
	 * @param title
	 *            the title of the new mediaContent
	 * @param type
	 *            the type of the new mediaContent
	 * @param author
	 *            the author of the new mediaContent
	 * @param genre
	 *            the genre of the new mediaContent
	 * @param description
	 *            the description of the new mediaContent
	 */
	public final void addMediaContent(final String name, final String title, final String type, final String author,
			final String genre, final String description, final String parent) {
		contentList.put(name, new MediaContent(name, title, type, author, genre, description, parent));
	}

	/**
	 * Gets an existing mediaContent
	 * 
	 * @param name
	 *            the name of the <code>MediaContent</code> to get
	 * @return the <code>MediaContent</code> requested
	 */
	public final MediaContent getMediaContent(final String title) throws NotExistingMediaContent {
		MediaContent mediaContent = contentList.get(title);
		if (mediaContent == null)
			throw new NotExistingMediaContent();
		return mediaContent;
	}

	/**
	 * Gets the value of name
	 *
	 * @return the value of name
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * Sets the value of name
	 *
	 * @param argName
	 *            Value to assign to this.name
	 */
	public final void setName(final String argName) {
		this.name = argName;
	}

	/**
	 * Gets the value of prettName
	 *
	 * @return the value of prettName
	 */
	public final String getPrettyName() {
		return this.prettyName;
	}

	/**
	 * Sets the value of prettName
	 *
	 * @param argPrettName
	 *            Value to assign to this.prettName
	 */
	public final void setPrettyName(final String argPrettName) {
		this.prettyName = argPrettName;
	}

	/**
	 * Gets the value of parent
	 *
	 * @return the value of parent
	 */
	public final String getParent() {
		return this.parent;
	}

	/**
	 * Sets the value of parent
	 *
	 * @param argParent
	 *            Value to assign to this.parent
	 */
	public final void setParent(final String argParent) {
		this.parent = argParent;
	}

	/**
	 * Gets the value of description
	 *
	 * @return the value of description
	 */
	public final String getDescription() {
		return this.description;
	}

	/**
	 * Sets the value of description
	 *
	 * @param argDescription
	 *            Value to assign to this.description
	 */
	public final void setDescription(final String argDescription) {
		this.description = argDescription;
	}

	public final String toString() {
		return "<mediaContainer name=\"" + getName() + "\" prettyName=\"" + getPrettyName() + "\" description=\""
				+ getDescription() + "\" parent=\"" + getParent() + "\" />";
	}
}

/** The media content */
class MediaContent {

	/** The name of the media */
	private String name;

	/** The title of the media */
	private String title;

	/** The class of the media */
	private String type;

	/** The author of the media */
	private String author;

	/** The genre of the media */
	private String genre;

	/** A description for the media */
	private String description;

	/** The parent for the media */
	private String parent;

	/**
	 * Creates a media content
	 * 
	 * @param title
	 *            the title of the new mediaContainer
	 * @param type
	 *            the type of the new mediaContainer
	 * @param author
	 *            the author of the new mediaContainer
	 * @param genre
	 *            the genre of the new mediaContainer
	 * @param description
	 *            the description of the new mediaContainer
	 */
	public MediaContent(final String name, final String title, final String type, final String author,
			final String genre, final String description, final String parent) {
		setName(name);
		setTitle(title);
		setType(type);
		setAuthor(author);
		setGenre(genre);
		setDescription(description);
		setParent(parent);
	}

	/**
	 * Gets the value of name
	 *
	 * @return the value of name
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * Sets the value of name
	 *
	 * @param argName
	 *            Value to assign to this.name
	 */
	public final void setName(final String argName) {
		this.name = argName;
	}

	/**
	 * Gets the value of title
	 *
	 * @return the value of title
	 */
	public final String getTitle() {
		return this.title;
	}

	/**
	 * Sets the value of title
	 *
	 * @param argTitle
	 *            Value to assign to this.title
	 */
	public final void setTitle(final String argTitle) {
		this.title = argTitle;
	}

	/**
	 * Gets the value of type
	 *
	 * @return the value of type
	 */
	public final String getType() {
		return this.type;
	}

	/**
	 * Sets the value of type
	 *
	 * @param argType
	 *            Value to assign to this.type
	 */
	public final void setType(final String argType) {
		this.type = argType;
	}

	/**
	 * Gets the value of author
	 *
	 * @return the value of author
	 */
	public final String getAuthor() {
		return this.author;
	}

	/**
	 * Sets the value of author
	 *
	 * @param argAuthor
	 *            Value to assign to this.author
	 */
	public final void setAuthor(final String argAuthor) {
		this.author = argAuthor;
	}

	/**
	 * Gets the value of genre
	 *
	 * @return the value of genre
	 */
	public final String getGenre() {
		return this.genre;
	}

	/**
	 * Sets the value of genre
	 *
	 * @param argGenre
	 *            Value to assign to this.genre
	 */
	public final void setGenre(final String argGenre) {
		this.genre = argGenre;
	}

	/**
	 * Gets the value of description
	 *
	 * @return the value of description
	 */
	public final String getDescription() {
		return this.description;
	}

	/**
	 * Sets the value of description
	 *
	 * @param argDescription
	 *            Value to assign to this.description
	 */
	public final void setDescription(final String argDescription) {
		this.description = argDescription;
	}

	/**
	 * Gets the value of parent
	 *
	 * @return the value of parent
	 */
	public final String getParent() {
		return this.parent;
	}

	/**
	 * Sets the value of parent
	 *
	 * @param argParent
	 *            Value to assign to this.parent
	 */
	public final void setParent(final String argParent) {
		this.parent = argParent;
	}

	public final String toString() {
		return "<mediaContent name=\"" + getName() + "\" title=\"" + getTitle() + "\" type=\"" + getType()
				+ "\" author=\"" + getAuthor() + "\" genre=\"" + getGenre() + "\" description=\"" + getDescription()
				+ "\" parent=\"" + getParent() + "\" />";
	}
}
