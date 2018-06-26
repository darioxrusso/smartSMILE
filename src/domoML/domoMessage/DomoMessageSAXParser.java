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

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

//import domoML.domoDevice.*;
import domoML.*;

/**
 * Implements a SAX parser for reading an xml input stream from which take a
 * DomoMessage.
 */
public class DomoMessageSAXParser extends DefaultHandler {

	/** Temporany domoMessage for store parsing values. */
	private DomoMessage domoMessage;

	/**
	 * Constructor that simply calls the super constructor of the
	 * DefaultHandler.
	 */
	public DomoMessageSAXParser(DomoMessage domoMessage) {
		super();
		this.domoMessage = domoMessage;
	}

	/** Handler at the beginning of a tag. */
	public final void startElement(final String uri, final String name, final String qName, final Attributes atts) {
		if (name.equalsIgnoreCase("message")) {
			// Create a new domomessage builded using the domoML
			domoMessage.setSenderURL(atts.getValue("senderURL"));
			domoMessage.setSenderId(atts.getValue("senderId"));
			domoMessage.setReceiverURL(atts.getValue("receiverURL"));
			domoMessage.setReceiverId(atts.getValue("receiverId"));
			domoMessage.setMessage(atts.getValue("message"));
			domoMessage.setMessageType(DomoMessage.MessageType.valueOf(atts.getValue("messageType")));
			String outputDataType = atts.getValue("output");
			if (outputDataType != null) {
				// an output attribute is set
				domoMessage.setOutput(DomoMLDocument.DataType.valueOf(outputDataType));
				String outputName = atts.getValue("outputName");
				if (outputName != null)
					domoMessage.setOutputName(outputName);
			}
		}
		// Add a service
		else if (name.equalsIgnoreCase("input")) {
			domoMessage.addInput(atts.getValue("name"), atts.getValue("value"),
					(DomoMLDocument.DataType) DomoMLDocument.DataType.valueOf(atts.getValue("type")));
		}
	}

	/**
	 * Return the domoMessage that should contains the result of the parser.
	 *
	 * @return The result of the parser.
	 */
	public final DomoMessage getDomoMessage() {
		return domoMessage;
	}
}
