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

package domoML.domoMessage;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.StringReader;
import java.util.*;

import domoML.*;
import domoML.domoDevice.*;

/**
 * A generic message used in the web service. Every information is stored in a
 * DOM tree with documentElement &quot;message&quot;.
 */
public class DomoMessage extends DomoMLDocument implements Document {

	/** Enumeration of types for messages supported. */
	public static enum MessageType {
		COMMAND, SUCCESS, FAILURE, UPDATE, REMOVE, EXISTS
	};

	/** Create a new Document with tag &quot;message&quot;. */
	public DomoMessage() {
		appendChild(createElement("message"));
	}

	/**
	 * Create a new Document from a DomoML description.
	 *
	 * @param domoMessageString
	 *            The description of the message as DomoML string.
	 */
	public DomoMessage(final String domoMessageString) {
		this();
		// check if empty domoMLMessage
		if (!domoMessageString.trim().equalsIgnoreCase(""))
			try {
				XMLReader parser = XMLReaderFactory.createXMLReader();
				DomoMessageSAXParser dmsp = new DomoMessageSAXParser((DomoMessage) this);
				parser.setContentHandler(dmsp);
				parser.setErrorHandler(dmsp);
				parser.parse(new InputSource(new StringReader(domoMessageString)));
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	/**
	 * Build a domoMessage.
	 *
	 * @param senderURL
	 *            The URL of the sender of the message.
	 * @param senderId
	 *            The id of the sender of the message.
	 * @param destURL
	 *            The URL of the receiver of the message.
	 * @param destId
	 *            The id of the receiver of the message.
	 * @param message
	 *            The message to be trasmitted.
	 * @param messageType
	 *            The type of the message.
	 */
	public DomoMessage(final String senderURL, final String senderId, final String destURL, final String destId,
			final String message, final MessageType messageType) {
		// create the root of the DOM
		this();
		// set values
		setSenderURL(senderURL);
		setSenderId(senderId);
		setReceiverURL(destURL);
		setReceiverId(destId);
		setMessage(message);
		setMessageType(messageType);
	}

	/**
	 * Build a domoMessage.
	 *
	 * @param senderURL
	 *            The URL of the sender of the message.
	 * @param senderId
	 *            The id of the sender of the message.
	 * @param destURL
	 *            The URL of the receiver of the message.
	 * @param destId
	 *            The id of the receiver of the message.
	 * @param message
	 *            The message to be trasmitted.
	 * @param messageType
	 *            The type of the message.
	 * @param output
	 *            The type of the output expected from the execution of the
	 *            message.
	 */
	public DomoMessage(final String senderURL, final String senderId, final String destURL, final String destId,
			final String message, final MessageType messageType, final DomoDevice.DataType output) {
		// create the root of the DOM
		this(senderURL, senderId, destURL, destId, message, messageType);
		if (output != null)
			setOutput(output);
	}

	/**
	 * Build a domoMessage.
	 *
	 * @param senderURL
	 *            The URL of the sender of the message.
	 * @param senderId
	 *            The id of the sender of the message.
	 * @param destURL
	 *            The URL of the receiver of the message.
	 * @param destId
	 *            The id of the receiver of the message.
	 * @param message
	 *            The message to be trasmitted.
	 * @param messageType
	 *            The type of the message.
	 * @param output
	 *            The type of the output expected from the execution of the
	 *            message.
	 * @param outputName
	 *            The name of the output expected from the execution of the
	 *            message.
	 */
	public DomoMessage(final String senderURL, final String senderId, final String destURL, final String destId,
			final String message, final MessageType messageType, final DomoDevice.DataType output,
			final String outputName) {
		// create the root of the DOM
		this(senderURL, senderId, destURL, destId, message, messageType);
		if (output != null)
			setOutput(output);
		if (outputName != null)
			setOutputName(outputName);
	}

	/**
	 * Set the new senderURL for the message.
	 *
	 * @param senderURL
	 *            The new senderURL to be assigned.
	 */
	public final void setSenderURL(final String senderURL) {
		// Take the first and unique child named "device".
		((Element) getFirstChild()).setAttribute("senderURL", senderURL);
	}

	/**
	 * Get the senderURL assigned.
	 *
	 * @return the senderURL of the message.
	 */
	public final String getSenderURL() {
		// Take the first and unique child named "device".
		return ((Element) getFirstChild()).getAttribute("senderURL");
	}

	/**
	 * Set the new senderId for the message.
	 *
	 * @param senderId
	 *            The new senderId to be assigned.
	 */
	public final void setSenderId(final String senderId) {
		// Take the first and unique child named "device".
		((Element) getFirstChild()).setAttribute("senderId", senderId);
	}

	/**
	 * Get the senderId assigned.
	 *
	 * @return the senderId of the message.
	 */
	public final String getSenderId() {
		// Take the first and unique child named "device".
		return ((Element) getFirstChild()).getAttribute("senderId");
	}

	/**
	 * Set the new receiverURL for the message.
	 *
	 * @param receiverURL
	 *            The new receiverURL to be assigned.
	 */
	public final void setReceiverURL(final String receiverURL) {
		// Take the first and unique child named "device".
		((Element) getFirstChild()).setAttribute("receiverURL", receiverURL);
	}

	/**
	 * Get the receiverURL assigned.
	 *
	 * @return the receiverURL of the message.
	 */
	public final String getReceiverURL() {
		// Take the first and unique child named "device".
		return ((Element) getFirstChild()).getAttribute("receiverURL");
	}

	/**
	 * Set the new senderId for the message.
	 *
	 * @param senderId
	 *            The new senderId to be assigned.
	 */
	public final void setReceiverId(final String senderId) {
		// Take the first and unique child named "device".
		((Element) getFirstChild()).setAttribute("receiverId", senderId);
	}

	/**
	 * Get the senderId assigned.
	 *
	 * @return the senderId of the message.
	 */
	public final String getReceiverId() {
		// Take the first and unique child named "device".
		return ((Element) getFirstChild()).getAttribute("receiverId");
	}

	/**
	 * Set the new Message for the message.
	 *
	 * @param message
	 *            The new Message to be assigned.
	 */
	public final void setMessage(final String message) {
		// Take the first and unique child named "device".
		((Element) getFirstChild()).setAttribute("message", message);
	}

	/**
	 * Get the Message assigned.
	 *
	 * @return the Message of the message.
	 */
	public final String getMessage() {
		// Take the first and unique child named "device".
		return ((Element) getFirstChild()).getAttribute("message");
	}

	/**
	 * Set the new message Type for the Type.
	 *
	 * @param messageType
	 *            The new messageType to be assigned.
	 */
	public final void setMessageType(final MessageType messageType) {
		// Take the first and unique child named "device".
		((Element) getFirstChild()).setAttribute("messageType", messageType.toString());
	}

	/**
	 * Get the message type assigned.
	 *
	 * @return the Type of the message.
	 */
	public final String getMessageType() {
		// Take the first and unique child named "device".
		return ((Element) getFirstChild()).getAttribute("messageType");
	}

	/**
	 * Add an input field for the message.
	 *
	 * @param name
	 *            The name of the input.
	 *
	 * @param value
	 *            The value to be used as input.
	 * @param type
	 *            The domoML.DomoMLDocument.DataType
	 * @return The domoML.domoMessage.DomoMessageInput rappresenting the element
	 *         added.
	 */
	public final DomoMessageInput addInput(final String name, final String value, final DomoDevice.DataType type) {
		DomoMessageInput inputElement = new DomoMessageInput(this, name, value, type);
		getDocumentElement().appendChild(inputElement);
		// return the input Element
		return inputElement;
	}

	/**
	 * Set the output field for the message.
	 *
	 * @param type
	 *            The DomoDevice.DataType value to be used as output.
	 */
	public final void setOutput(final DomoDevice.DataType type) {
		((Element) getFirstChild()).setAttribute("output", type.toString());
	}

	/**
	 * Set the outputName field for the message.
	 *
	 * @param outputName
	 *            The outputName to be used as output.
	 */
	public final void setOutputName(final String outputName) {
		((Element) getFirstChild()).setAttribute("outputName", outputName);
	}

	/**
	 * Get the output field for the message.
	 *
	 * @return type The element representing the output field.
	 * @throws domoML.domoDevice.NoElementFoundException
	 *             if no &quot;output&quot; attribute is found.
	 */
	public final DomoDevice.DataType getOutput() throws NoAttributeFoundException {
		if (!getDocumentElement().getAttribute("output").equalsIgnoreCase(""))
			// a value for output is set
			return DomoMLDocument.DataType.valueOf(getDocumentElement().getAttribute("output"));
		else
			throw new NoAttributeFoundException();
	}

	/**
	 * Get the output field for the message.
	 *
	 * @return type The element representing the output field.
	 * @throws domoML.domoDevice.NoElementFoundException
	 *             if no &quot;output&quot; attribute is found.
	 */
	public final String getOutputName() throws NoAttributeFoundException {
		if (!getDocumentElement().getAttribute("outputName").equalsIgnoreCase(""))
			// a value for output is set
			return getDocumentElement().getAttribute("outputName");
		else
			throw new NoAttributeFoundException();
	}

	/**
	 * Get a list of node which tag names are &quot;input&quot;
	 *
	 * @return The list of Node.
	 */
	public final List<Node> getInputParameterElements() {
		List<Node> inputParameterList = new LinkedList<Node>();
		NodeList rootElementChildren = getDocumentElement().getChildNodes();
		for (int i = 0; i < rootElementChildren.getLength(); i++) {
			if (((Element) rootElementChildren.item(i)).getTagName().matches("input"))
				inputParameterList.add(rootElementChildren.item(i));
		}
		return inputParameterList;
	}

	/**
	 * Get an Element with tag &quot;input&quot; and attribute &quot;name&quot;
	 * with the specified. If element was not found it throws NoElementFoudn
	 * exception.
	 *
	 * @param inputName
	 *            The value of the attribute &quot;name&quot;.
	 * @return The element if found.
	 * @throws NoElementFoundException
	 *             If Element was not found.
	 */
	public final DomoMessageInput getInput(final String inputName) throws NoElementFoundException {
		NodeList messageChildNodes = getDocumentElement().getChildNodes();
		boolean found = false;
		int i = 0;
		// searching for Element
		for (i = 0; (i < messageChildNodes.getLength()) && (!found); i++)
			if (((Element) messageChildNodes.item(i)).getAttribute("name").matches(inputName))
				found = true;
		if (found)
			return (DomoMessageInput) messageChildNodes.item(--i);
		else
			throw new NoElementFoundException();
	}
}
