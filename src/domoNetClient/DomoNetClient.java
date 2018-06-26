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

package domoNetClient;

import java.util.*;

import javax.xml.namespace.*;
// explicit declaration of javax.xml.rpc components in order to don't
// specify every time that I declare a Call that is a Call
// (exists a javax.xml.rpc.Call too)
import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.*;

import domoML.domoDevice.*;
import domoML.domoMessage.*;
import common.*;

/** Client for the domoNet Web services. */
public class DomoNetClient {

    /** An hashmap containing the list of web services in use. */
    private static HashMap<String, String> webServicesList = new HashMap<String, String>();

    /**
     * A list of all domoDevice currently in use. Using an hashmap in order to
     * grant an unique identifier for each domodevice.
     */
    public HashMap<DomoDeviceId, DomoDevice> domoDeviceList = new HashMap<DomoDeviceId, DomoDevice>();

    // /** Use an unique call to invoke remote methods. */
    private static Call call;

    /** The server that listen for TCP messages becoming from domonet */
    TCPServer tcpServer = null;

    AppProperties prefs;

    /** Empty constructor. */
    public DomoNetClient() throws Exception {
	// init params
	prefs = new AppProperties(
		"src/domoNetClient/domoNetClient.preferences");
	// init call
	try {
	    call = (org.apache.axis.client.Call) (new org.apache.axis.client.Service())
		    .createCall();
	} catch (Exception e) {
	    throw new Exception();
	}
    }

    /**
     * Perform the connection to a web services in order to get informations on
     * it.
     *
     * @param URL
     *            The URL of the web services to connect to.
     * @param description
     *            The description of the web services.
     */
    public final void connectToWebServices(final String URL,
	    final String description) throws Exception {
	// check if connection is estabilished with success
	boolean connectionSuccessed = false;
	// The string that will contains the list of devices
	String domoDevicesString = new String("<devices />");
	// trying to connect
	Debug.getInstance().writeln("Connecting to " + URL + " - " + description);
	try {
	    if (tcpServer == null) {
		// tcpServer for incoming messages not yet initialized
		tcpServer = new TCPServer();

		tcpServer.start();
		Object[] param = {
			prefs.getProperty("updateSocketPort", "7777") };
		domoDevicesString = (String) callWebServicesMethod(URL,
			javax.xml.rpc.encoding.XMLType.XSD_STRING,
			"registerToClientsUpdatePort", param);
	    }
	    domoDevicesString = (String) callWebServicesMethod(URL,
		    javax.xml.rpc.encoding.XMLType.XSD_STRING,
		    "getDomoDeviceList", null);
	    connectionSuccessed = true;
	} catch (Exception e) {
	    Debug.getInstance().writeln(
		    "Cannot connect to " + URL + ". The site may be down.");
	    throw new Exception();
	}
	// if connection successed load the devices
	if (connectionSuccessed) {
	    // removing existing domoDevices
	    domoDeviceList.clear();
	    Debug.getInstance().writeln("Loading devices... ");
	    // preparing for parsing passed domo device list config.
	    try {
		// assign returned value to a temporary variable in order to
		// store values into the domoDeviceList.
		List<DomoDevice> importedDevices = DomoDevice
			.getDomoDeviceList(domoDevicesString);
		Iterator<DomoDevice> importedDevicesIterator = importedDevices
			.iterator();
		while (importedDevicesIterator.hasNext()) {
		    // get device
		    DomoDevice domoDevice = (DomoDevice) importedDevicesIterator
			    .next();
		    // setting correct url if has no one
		    if (domoDevice.getUrl().length() == 0)
			domoDevice.setUrl(URL);
		    // create a domoDevice identificator
		    DomoDeviceId deviceId = new DomoDeviceId(
			    domoDevice.getUrl(), domoDevice.getId());
		    // add device to the hashmap
		    domoDeviceList.put(deviceId, domoDevice);
		    // debug message
		    Debug.getInstance().writeln("Added [" + deviceId.getId() + "@"
			    + deviceId.getUrl() + "] " + domoDevice.getType()
			    + " (" + domoDevice.getDescription() + ")");
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    // adding web services in use to the list of web services in use
	    webServicesList.put(URL, description);
	    Debug.getInstance().writeln(" Client has now " + domoDeviceList.size()
		    + " domo devices.");
	}
    }

    /**
     * Perform the shutdown of a web services in order to finalize it.
     *
     * @param URL
     *            The URL of the web services to connect to.
     * @param description
     *            The description of the web services.
     */
    public final void shutdownWebServices(final String URL,
	    final String description) throws Exception {
	Debug.getInstance().writeln("Shutting down " + URL + "... ");
	callWebServicesMethod(URL, null, "finalize", null);
	disconnectUpdateSocket();

    }

    /**
     * Perform the disconnection of the update socket.
     */
    public final void disconnectUpdateSocket() {
	Debug.getInstance().write("Disconnecting update socket... ");
	tcpServer.terminate();
	Debug.getInstance().writeln("done.");
    }

    /**
     * Call a method to remote web services.
     *
     * @param URL
     *            The URL of the web services.
     * @param returnType
     *            The type to be returned as javax.xml.namespace.QName.
     * @param method
     *            The method name.
     * @param params
     *            The parameters to be passed to the remote method. For this
     *            purpose it can be an empty array of Object (must be passed
     *            null) or an array of one element of type String so it's
     *            implemented to run only in those case.
     * @return an Object representing the result of the call. Null if call
     *         fails.
     */
    private final Object callWebServicesMethod(final String URL,
	    final QName returnType, final String method, Object[] params)
		    throws Exception {
	// initializes return type to null so if failure the method return null
	// else a generic object to be casted from the caller of the method.
	Object object = null;
	try {
	    call = (org.apache.axis.client.Call) (new org.apache.axis.client.Service())
		    .createCall();
	    // set address for the call
	    call.setTargetEndpointAddress(new java.net.URL(URL));
	    // set the operation name
	    call.setOperationName(new javax.xml.namespace.QName(method));
	    // set the return type
	    if (returnType != null)
		call.setReturnType(returnType);
	    // if "params" is null so it's not needed to pass arguments to the
	    // remote call
	    if (params == null)
		params = new Object[0];
	    // else it's implemented only to pass only one argument of type
	    // string
	    else
		call.addParameter("param",
			javax.xml.rpc.encoding.XMLType.XSD_STRING,
			ParameterMode.IN);
	    // invoke remote method.
	    object = call.invoke(params);
	} catch (Exception e) {
	    throw new Exception();
	}
	return object;
    }

    public String execute(DomoMessage domoMessage) throws Exception {
	// calling for executing command
	Debug.getInstance().write("Calling: " + domoMessage.toString() + "...");
	// Now I'll do a little dirty hack: I change the URL of the receiver
	// as empty because I'm sure that the interested device is on the
	// web service that I will
	// contact. In this way, when I'll search for the device on the web
	// service using the key with empty URL and the ID because on
	// the web service, the local devices are stored in that way.
	// Before resetting receiver url I must store it in order to know
	// where send the request.
	final String receiverURL = domoMessage.getReceiverURL();
	domoMessage.setReceiverURL("");
	// put the domoMessage as the parameter of the command to invoke
	Object[] param = { domoMessage.toString() };
	try {
	    // calling execute method
	    String ret = (String) callWebServicesMethod(receiverURL,
		    javax.xml.rpc.encoding.XMLType.XSD_STRING, "execute",
		    param);
	    Debug.getInstance().write("Receiving: " + ret + "...");
	    // convert the result to a DomoMessage
	    DomoMessage responseDomoMessage = new DomoMessage(ret);
	    if (responseDomoMessage.getMessageType().equals("SUCCESS")) {
		Debug.getInstance().writeln("Execution successed.");
		return responseDomoMessage.getMessage();
	    } else {
		Debug.getInstance().writeln("Execution failure.");
		return "FAILURE";
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    Debug.getInstance().writeln("Execution failure.");
	    throw new Exception();
	}
    }
}
