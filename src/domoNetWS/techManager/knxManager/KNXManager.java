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

package domoNetWS.techManager.knxManager;

import java.net.*;
import java.io.*;
import java.util.*;

import tuwien.auto.eicl.*;
import tuwien.auto.eicl.util.*;
import tuwien.auto.eibxlator.*;
import tuwien.auto.eicl.struct.cemi.*;
import tuwien.auto.eicl.event.*;

import domoML.domoDevice.*;
import domoML.domoMessage.*;
import domoNetWS.*;
import domoNetWS.techManager.*;
import common.Debug;

import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Extends the DomoManager class in order to implement a manager for the KNX
 * protocol.
 */
public class KNXManager extends TechManager {

    /**
     * The connection tunnel through the socket to the server that manage the
     * KNX devices.
     */
    protected CEMI_Connection tunnel;

    /**
     * Map the DomoDevice.DataType into a &quot;Major&quot; string in order to
     * represent the message inside the bus correctly.
     */
    private HashMap<DomoDevice.DataType, String> dataType2Major = new HashMap<DomoDevice.DataType, String>();

    /** Maps group addresses to DomoML data-types */
    public HashMap<String, domoML.DomoMLDocument.DataType> groupAddress2DataTypeList = new HashMap<String, domoML.DomoMLDocument.DataType>();

    /**
     * Map the DomoDevice.DataType into a &quot;Minor&quot;. It's not important
     * for my implementation but it's requested by the KNX library used.
     */
    private HashMap<DomoDevice.DataType, String> dataType2Minor = new HashMap<DomoDevice.DataType, String>();

    protected HashMap<String, DomoDevice.DataType> string2DataType = new HashMap<String, DomoDevice.DataType>();

    protected HashMap<DomoDevice.DataType, String> dataType2String = new HashMap<DomoDevice.DataType, String>();

    protected HashMap<String, String> dumpedGroupAddressValues = new HashMap<String, String>();

    /**
     * The listener for the KNX bus. Check every message that travels on the KNX
     * bus.
     */
    private FrameActionListener frameActionListener = new FrameActionListener(
	    this);

    /** The configuration file to be imported by the manager. */
    private String configXMLFile = new String("/xml/KNXConfiguration.xml");

    /**
     * Map the serial of a DomoDevice with its DomoDeviceId as configured
     * before.
     */
    private HashMap<String, DomoDeviceId> dumpedDomoDeviceSerials = new HashMap<String, DomoDeviceId>();

    /**
     * Build the KNXManager.
     * 
     * @param host
     *            The host where the KNX server is.
     * @param port
     *            The port of the host of the KNX server.
     * @param domoNetWS
     *            The domoNetWS class that links this module.
     */
    public KNXManager(final String host, final int port,
	    final DomoNetWS domoNetWS) {
	super(host, port, domoNetWS);
	Debug.getInstance().write(" Loading " + this.getClass().toString() + "... ");
	initDataType();
	Debug.getInstance().writeln("done.");
    }

    /** Start the service. */
    public void start() {
	Debug.getInstance().writeln("Starting " + this.getClass().toString() + " using "
		+ getHost() + ":" + getPort() + "... ");
	// set configuration file (an ETS 3 XML file)
	// to do: load XML file from a path that can be reached from the
	// DomoNetWS jar.
	// connect to the server
	try {
	    performConnection();
	    loadConnectedDevices();
	} catch (EICLException e) {
	    try {
		tryReconnection(20, 30000);
		loadConnectedDevices();
	    } catch (EICLException e2) {
		e2.printStackTrace();
	    }
	}
	Debug.getInstance().writeln("done.");
    }

    /**
     * Add a domoML device to the list of domoDevice in the DomoNetWS.
     * 
     * @param domoDevice
     *            The string representation of the device to add.
     * @param address
     *            The real address in the manager.
     */
    public final void addDevice(final DomoDevice domoDevice,
	    final String address) {
	// call the addDevice of the DomoNetWS to adds it.
	// The DomoNetWS returns the domoDeviceId so it's possible fill
	// the doubleHash class correctly.
	DomoDeviceId ddid = domoNetWS.addDomoDevice(domoDevice);
	doubleHash.add(ddid, address, true);
	// store the domoDeviceId for each group address too.
	Iterator<DomoDeviceService> domoServicesIterator = domoDevice
		.getServices().iterator();
	while (domoServicesIterator.hasNext()) {
	    doubleHash.add(ddid,
		    ((DomoDeviceService) domoServicesIterator.next())
			    .getName());
	}
    }

    /** Perform the connection to the KNX server. */
    private final void performConnection() throws EICLException {
	boolean connectionFailed = false;
	Debug.getInstance().write(" Connecting to " + getHost() + ":" + getPort() + "... ");
	if (tunnel == null) {
	    try {
		// creating the socket
		InetSocketAddress isa = new InetSocketAddress(getHost(),
			getPort());
		// try the connection using the "Calimero" library
		tunnel = new CEMI_Connection(isa,
			new TunnellingConnectionType());
		/*
		 * if(isRunningOnWindows()) { Debug.getInstance().writeln(
		 * "Receiver buffer size: " + tunnel.getReceiveBufferSize());
		 * Debug.getInstance().writeln("Send buffer size: " +
		 * tunnel.getSendBufferSize()); }
		 */
	    } catch (EICLException e) {
		connectionFailed = true;
		/*
		 * } catch (SocketException e2) { e2.printStackTrace();
		 */
	    }
	}
	if (tunnel != null) {
	    Debug.getInstance().write("Creating listener for knx bus... ");
	    // create a listener for the KNX bus
	    tunnel.addFrameListener(frameActionListener);
	    Debug.getInstance().write("done. ");
	    // frame listener created
	}
	if (connectionFailed) {
	    Debug.getInstance().writeln("failure.");
	    throw new EICLException("");
	}
	Debug.getInstance().writeln("success.");
    }

    /**
     * Reconnect for lost connection to the KNX bus.
     * 
     * @param maxTrials
     *            The number of reconnection trials to do.
     * @param waitFor
     *            The milliseconds to wait from a trial to another.
     * @return if the reconnection fails or not.
     */
    protected final void tryReconnection(int maxTrials, int waitFor)
	    throws EICLException {
	// set if must try connection
	boolean tryAgain = true;
	// number of trials
	int currentTrial = 0;
	while (tryAgain) {
	    try {
		// do not wait only for the first trial
		if (currentTrial != 0)
		    Thread.sleep(waitFor);
		performConnection();
		tryAgain = false;
	    } catch (EICLException e) {
		tryAgain = (currentTrial++ < maxTrials);
	    } catch (InterruptedException e) {
		tryAgain = false;
	    } catch (Exception e) {
		tryAgain = (currentTrial++ < maxTrials);
	    }
	}
	if (currentTrial == maxTrials) {
	    tryAgain = false;
	    throw new EICLException("Reconnection failure");
	}
    }

    /**
     * Get the configuration file created with ETS3 and parse it in order to
     * find devices attached to the KNX bus.
     */
    private final void loadConnectedDevices() {
	List<DomoDevice> deviceList = new LinkedList<DomoDevice>();
	try {
	    XMLReader parser = XMLReaderFactory.createXMLReader();
	    KNXManagerConfigurationSAXParser kmcsp = new KNXManagerConfigurationSAXParser(
		    this);
	    parser.setContentHandler(kmcsp);
	    parser.setErrorHandler(kmcsp);
	    FileReader fileReader = new FileReader(basePath + configXMLFile);
	    parser.parse(new InputSource(fileReader));
	    // assign returned value to the property of this class.
	    deviceList = kmcsp.getDomoDevices();
	    groupAddress2DataTypeList = kmcsp.getGroupAddress2DataTypes();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	// than add the devices
	Iterator<DomoDevice> deviceListIterator = deviceList.iterator();
	while (deviceListIterator.hasNext()) {
	    // get the domoDevice
	    DomoDevice domoDevice = deviceListIterator.next();
	    if (!dumpedDomoDeviceSerials
		    .containsKey((String) domoDevice.getSerialNumber()))
		// The physical address was stored in domoDevices.serial field.
		addDevice(domoDevice, domoDevice.getSerialNumber());
	}
    }

    /** Load a DomoDevice from a previous configuration. */
    public final void loadDumpedDomoDevice(final DomoDevice domoDevice) {
	dumpedDomoDeviceSerials.put(domoDevice.getSerialNumber(),
		new DomoDeviceId(domoDevice.getUrl(), domoDevice.getId()));
	addDevice(domoDevice, domoDevice.getSerialNumber());
    }

    /**
     * Execute a domoMessage converting it to a message for the KNX bus
     * 
     * @param domoMessage
     *            the domoMessage to be converted and executed
     * @param tryAgainIfFails
     *            if must try again if fails execution
     * @return The domoMessage as response of the one to be executed
     */
    public final DomoMessage execute(final DomoMessage domoMessage) {
	// getting the real address of the receivers of the message.
	// It was stored in the message attribute of the domoMessage.
	final String receiverAddress = domoMessage.getMessage();
	Debug.getInstance().writeln("Executing on KNX Bus: " + receiverAddress + " - "
		+ domoMessage.toString());
	// getting the type of the parameter. For this tech surely it's
	// requested only one parameter so I get only it.
	try {
	    // test if output attribute is set. If set, it'a read message
	    DomoDevice.DataType serviceType = domoMessage.getOutput();
	    // the address of the device that will response to the execution
	    String senderResponseAddress = null;
	    // the domoDevice is managed by this techManager so do not
	    // specify the URL
	    senderResponseAddress = doubleHash.getAddress(
		    new DomoDeviceId("", domoMessage.getReceiverId()));
	    // send a read command to the bus. Wait for a response.
	    String response = null;
	    int numberOfTries = 0;
	    boolean readed = false;
	    // tries to see if the state of the requested value was cached
	    response = dumpedGroupAddressValues.get(domoMessage.getMessage());
	    if (response == null) {
		Debug.getInstance().writeln("Reading value from the device");
		// the response was not cached and must be asked to the device

		while (numberOfTries < 2 && !readed) {
		    try {
			response = sendReadDataFrame(serviceType,
				new EIB_Address(domoMessage.getMessage()));
			readed = true;
		    } catch (InterruptedException e) {
			readed = true;
			e.printStackTrace();
		    } catch (EICLException e) {
			e.printStackTrace();
			response = null;
			try {
			    tunnel.disconnect("client request");
			    tunnel = null;
			    numberOfTries++;
			    tryReconnection(20, 30000);
			} catch (EICLException e2) {
			    readed = true;
			    response = null;
			}
		    }
		}
	    }

	    if (response != null) {
		// a valid response was received
		return new DomoMessage(domoMessage.getReceiverURL(),
			domoMessage.getReceiverId(),
			domoMessage.getReceiverURL(),
			domoMessage.getReceiverId(), response,
			DomoMessage.MessageType.SUCCESS);
	    }
	} catch (domoML.domoMessage.NoAttributeFoundException nafe) {
	    // take only the first input field because KNX may have only one
	    // parameter at time
	    DomoMessageInput inputParameter = ((DomoMessageInput) domoMessage
		    .getInputParameterElements().get(0));
	    // getting the data-type of the parameter
	    DomoDevice.DataType serviceType = inputParameter.getType();
	    // send a write command to the bus. No response expected.
	    boolean tryAgain = true;
	    while (tryAgain) {
		try {
		    sendWriteDataFrame(serviceType,
			    inputParameter.getAttribute("value"),
			    new EIB_Address(receiverAddress));
		    tryAgain = false;
		    /*
		     * Think don't need this
		     * dumpedGroupAddressValues.put(receiverAddress,
		     * inputParameter.getAttribute("value"));
		     */
		    return new DomoMessage(domoMessage.getReceiverURL(),
			    domoMessage.getReceiverId(),
			    domoMessage.getReceiverURL(),
			    domoMessage.getReceiverId(), "",
			    DomoMessage.MessageType.SUCCESS);
		} catch (EICLException e) {
		    e.printStackTrace();
		    try {
			tunnel.disconnect("client request");
			tunnel = null;
			tryReconnection(20, 30000);
		    } catch (EICLException e2) {
			tryAgain = false;
			e2.printStackTrace();
		    }
		}
	    }
	}
	// an error occurs
	return new DomoMessage(domoMessage.getReceiverURL(),
		domoMessage.getReceiverId(), domoMessage.getReceiverURL(),
		domoMessage.getReceiverId(), "",
		DomoMessage.MessageType.FAILURE);
    }

    /**
     * Send the write frame to the KNX bus.
     * 
     * @param dataType
     *            The DomoDevice.DataType value.
     * @param value
     *            The value of the message to send.
     * @param receiverAddress
     *            The real address of the receiver device.
     * @throws EICLException
     *             If send fails.
     */
    private final void sendWriteDataFrame(final DomoDevice.DataType dataType,
	    String value, final EIB_Address receiverAddress)
		    throws EICLException {
	try {

	    CEMI_L_DATA data = new CEMI_L_DATA((byte) CEMI_L_DATA.MC_L_DATAREQ,
		    new EIB_Address(), receiverAddress,
		    string2PointPDUXlator(value, dataType).getAPDUByteArray());
	    tunnel.sendFrame(data, CEMI_Connection.WAIT_FOR_CONFIRM);
	    // searches for linked services
	} catch (EICLException e) {
	    throw new EICLException("Executing write data frame.");
	}
    }

    /**
     * Send the read frame to the KNX bus and wait for response.
     * 
     * @param dataType
     *            The DomoDevice.DataType value.
     * @param value
     *            The value of the message to send.
     * @param receiverAddress
     *            The real address of the receiver device.
     * @throws EICLException
     *             If send fails.
     */
    private final String sendReadDataFrame(final DomoDevice.DataType dataType,
	    final EIB_Address receiverAddress)
		    throws EICLException, InterruptedException {
	// getting info of the message
	final String majorType = dataType2Major.get(dataType);
	final String minorType = dataType2Minor.get(dataType);
	if (majorType != null && minorType != null) {
	    // major and minor Type correctly taken
	    try {
		PointPDUXlator xlator = PDUXlatorList
			.getPointPDUXlator(majorType, minorType);
		xlator.setServiceType(PointPDUXlator.A_GROUPVALUE_READ);
		CEMI_L_DATA data = new CEMI_L_DATA(
			(byte) CEMI_L_DATA.MC_L_DATAREQ, new EIB_Address(),
			receiverAddress, xlator.getAPDUByteArray());
		tunnel.sendFrame(data, CEMI_Connection.WAIT_FOR_CONFIRM);
		byte[] dataByte = null;
		dataByte = waitForResponse(receiverAddress);
		return byte2String(dataByte, dataType);
	    } catch (EICLException e) {
		throw new EICLException("Reading value");
	    }
	}
	// something did not go well...
	return null;
    }

    /**
     * Convert byte data received from KNX bus as string.
     * 
     * @param dataByte
     *            The value read from bus
     * @param dataType
     *            The domoML.domoDevice.DataType to be used to convert the
     *            value.
     * @return The value as string.
     * @throws EICLException
     *             When error occurs.
     */
    public final String byte2String(byte[] dataByte,
	    DomoDevice.DataType dataType) throws EICLException {
	String dataString = null;
	try {
	    PointPDUXlator ppx = PDUXlatorList.getPointPDUXlator(
		    dataType2Major.get(dataType), dataType2Minor.get(dataType));
	    ppx.setAPDUByteArray(dataByte);
	    dataString = ppx.getASDUasString();
	} catch (EICLException eicle) {
	    throw new EICLException("EICLException converting " + dataByte
		    + " byte data value to String.");
	}
	if (dataString != null) {
	    // Conversion with success. Now patch the result.
	    // Patch the response value if it's of type BOOLEAN
	    // in order to convert the "true" string as "1" and
	    // the "false" string to "0". The "Calimero" library
	    // doesn't provide this kind of "minor" but it's
	    // useful when must implement cooperation
	    // (UPnP uses that values)
	    if (dataType.equals(DomoDevice.DataType.BOOLEAN))
		if (dataString.equalsIgnoreCase("true"))
		    dataString = "1";
		else
		    dataString = "0";
	    // if is not a BOOLEAN and is not a STRING, it's needed only the
	    // value of the result and not the unit
	    else if (!dataType.equals(DomoDevice.DataType.STRING))
		dataString = dataString.substring(0, dataString.indexOf(" "));
	}
	return dataString;
    }

    public final PointPDUXlator string2PointPDUXlator(String value,
	    DomoDevice.DataType dataType) throws EICLException {
	String majorType = dataType2Major.get(dataType);
	String minorType = dataType2Minor.get(dataType);
	// patch the response value if it's of type BOOLEAN
	// in order to convert the "true" string as "1" and
	// the "false" string to "0". The "Calimero" library
	// doesn't provide this kind of "minor" but it's
	// useful when must implement cooperation
	// (UPnP uses that values)
	if (dataType.equals(DomoDevice.DataType.BOOLEAN))
	    if (value.equalsIgnoreCase("0"))
		value = "False";
	    else
		value = "True";
	try {
	    PointPDUXlator xlator = PDUXlatorList.getPointPDUXlator(majorType,
		    minorType);
	    xlator.setServiceType(PointPDUXlator.A_GROUPVALUE_WRITE);
	    xlator.setASDUfromString(value);
	    return xlator;
	} catch (EICLException eicle) {
	    throw new EICLException("EICLException converting " + value
		    + " to PointPDUXlator.");
	}
    }

    /**
     * Execute all linked service regarding the write domoMessage.
     * 
     * @param data
     *            The data to be translated in order to search
     *            &quot;linkedServices&quot;.
     */
    public final void executeLinkedServices(final CEMI_L_DATA data) {
	// gets the domoDevice involved with the message taking its address
	List<DomoDeviceId> sourceIds = doubleHash
		.getDomoDeviceId(data.getSourceAddress().toString());
	// if not possible to recognize the sender of the message
	// (e.g. the sender is the ip interface), must try to research
	// using the destination address.
	if (!(sourceIds != null && sourceIds.size() == 1
		&& domoNetWS.getDomoDevice(sourceIds.get(0)).getServices()
			.size() != 0))
	    // try to recognize the service thought the address group
	    sourceIds = doubleHash
		    .getDomoDeviceId(data.getDestinationAddress().toString());
	if (sourceIds != null) {
	    Iterator<DomoDeviceId> sourceIdsIterator = sourceIds.iterator();
	    // try to get the only input sub-tag in order to know the
	    // data-type of the message.
	    DomoDevice.DataType inputDataType = null;
	    // remember the last domoDevice in order to try to repeat
	    // searches of linked service already done.
	    // The value is initialize at -1 because I'm sure that any domoDevice
	    // may have that value
	    int lastInteractedId = -1;
	    while (sourceIdsIterator.hasNext()) {
		DomoDeviceId sourceId = sourceIdsIterator.next();
		if (lastInteractedId != new Integer(sourceId.getId())) {
		    lastInteractedId = new Integer(sourceId.getId());
		    // not yet visited that device
		    try {
			// the DomoDevice represented by the source address
			// taken
			// from the message exists.
			DomoDevice sourceDevice = domoNetWS
				.getDomoDevice(sourceId);
			// the service name is stored in the destination
			// address:
			// the service name is the group address. Each
			// domoDevice
			// may have much services with the same name
			Iterator<DomoDeviceService> serviceIt = sourceDevice
				.getNamedServices(
					data.getDestinationAddress().toString())
				.iterator();
			// create an empty domoMessage to store the last sent
			String lastSentUpdateMessage = new DomoMessage()
				.toString();
			while (serviceIt.hasNext()) {
			    DomoDeviceService service = (DomoDeviceService) serviceIt
				    .next();
			    if (service.getInputs().size() != 0) {
				// input fields found so take the first
				// (and should be the only on KNX)
				DomoDeviceServiceInput ddsi = service
					.getInputs().get(0);
				// the unique input tag was found than taking
				// the data-type
				// of the input
				inputDataType = ddsi.getType();
			    } else {
				// not found any input fields so search for the
				// output
				try {
				    inputDataType = service.getOutput();
				} catch (domoML.domoDevice.NoAttributeFoundException e) {
				    e.printStackTrace();
				}
			    }
			    String stringValue = byte2String(data.getData(),
				    inputDataType);
			    DomoMessage updateMessage = new DomoMessage("",
				    sourceId.getId(), "", sourceId.getId(),
				    service.getName(),
				    DomoMessage.MessageType.UPDATE);
			    updateMessage.addInput("value", stringValue,
				    inputDataType);

			    if (!lastSentUpdateMessage
				    .equals(updateMessage.toString())) {
				domoNetWS.sendClientsUpdate(
					updateMessage.toString());
				lastSentUpdateMessage = updateMessage
					.toString();
			    }

			    domoNetWS.searchAndExecuteLinkedServices(service,
				    sourceId, stringValue, inputDataType);
			}
		    } catch (NoElementFoundException nefe) {
			Debug.getInstance().writeln(
				"Not found any Konnex domoDevice with that "
					+ "service!");
		    } catch (EICLException eicle) {
			eicle.printStackTrace();
		    }
		}
	    }
	}
    }

    /**
     * Find and launch linked services provided with private void
     * executeLinkedServices(String receiverURL(), String receiverId() { }
     * 
     * /** Initialize the HashMaps to convert DomoDevece.DataType to
     * &quot;major&quot; and &quot;minor&quot; in order to rappresent the
     * message inside the bus correctly.
     */
    private final void initDataType() {
	// BOOLEAN
	dataType2Major.put(DomoDevice.DataType.BOOLEAN, "0x01");
	dataType2Minor.put(DomoDevice.DataType.BOOLEAN, "1.002");

	string2DataType.put("1 bit", DomoDevice.DataType.BOOLEAN);
	dataType2String.put(DomoDevice.DataType.BOOLEAN, "1 bit");

	// STRING
	dataType2Major.put(DomoDevice.DataType.STRING, "0x13");
	dataType2Minor.put(DomoDevice.DataType.STRING, "16.000");

	// INT
	dataType2Major.put(DomoDevice.DataType.INT, "0x09");
	dataType2Minor.put(DomoDevice.DataType.INT, "5.010");

	// FLOAT
	dataType2Major.put(DomoDevice.DataType.FLOAT, "0x09");
	dataType2Minor.put(DomoDevice.DataType.FLOAT, "5.010");

	// TWOBIT
	dataType2Major.put(DomoDevice.DataType.TWOBIT, null);
	dataType2Minor.put(DomoDevice.DataType.TWOBIT, null);

	string2DataType.put("2 bit", DomoDevice.DataType.TWOBIT);
	dataType2String.put(DomoDevice.DataType.TWOBIT, "2 bit");

	// THREEBIT
	dataType2Major.put(DomoDevice.DataType.THREEBIT, "0x03");
	dataType2Minor.put(DomoDevice.DataType.THREEBIT, "1.007");

	string2DataType.put("3 bit", DomoDevice.DataType.THREEBIT);
	dataType2String.put(DomoDevice.DataType.THREEBIT, "3 bit");

	// FOURBIT
	dataType2Major.put(DomoDevice.DataType.FOURBIT, null);
	dataType2Minor.put(DomoDevice.DataType.FOURBIT, null);

	string2DataType.put("4 bit", DomoDevice.DataType.FOURBIT);
	dataType2String.put(DomoDevice.DataType.FOURBIT, "4 bit");

	// ONEBYTE
	dataType2Major.put(DomoDevice.DataType.ONEBYTE, "0x05");
	dataType2Minor.put(DomoDevice.DataType.ONEBYTE, "5.010");

	string2DataType.put("1 Byte", DomoDevice.DataType.ONEBYTE);
	dataType2String.put(DomoDevice.DataType.ONEBYTE, "1 Byte");

	// TWOBYTE
	dataType2Major.put(DomoDevice.DataType.TWOBYTE, "0x10");
	dataType2Minor.put(DomoDevice.DataType.TWOBYTE, "5.003");

	string2DataType.put("2 Byte", DomoDevice.DataType.TWOBYTE);
	dataType2String.put(DomoDevice.DataType.TWOBYTE, "2 Byte");

	// FOURTEENBYTE
	dataType2Major.put(DomoDevice.DataType.STRING, "0x17");
	dataType2Minor.put(DomoDevice.DataType.STRING, "16.000");

	string2DataType.put("14 Byte", DomoDevice.DataType.STRING);
	dataType2String.put(DomoDevice.DataType.STRING, "14 Byte");
    }

    public DomoDevice getDomoDeviceFromKNXAddress(String address) {
	return null;
    }

    /** Finalize the class when the manager ends. */
    public void finalize() {
	try {
	    if (tunnel != null)
		tunnel.disconnect("client request");
	} catch (EICLException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Wait until a message from a device is received.
     * 
     * @param receiverAddress
     *            The address of the device.
     * @return The data of the message.
     * @throws InterruptedException
     */
    public synchronized byte[] waitForResponse(EIB_Address receiverAddress) {
	frameActionListener.waitForResponse = true;
	frameActionListener.pendingReadRequests.put(receiverAddress.toString(),
		null);
	boolean found = false;
	byte[] data = null;
	boolean firstFound = false;
	int i = 0;
	Timer timer = new Timer(5000, frameActionListener.available);
	timer.start();
	while (!found && i < 1) {
	    try {
		frameActionListener.available.P();
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	    data = frameActionListener.pendingReadRequests
		    .get(receiverAddress.toString());
	    if (data != null) {
		if (firstFound) {
		    found = true;
		} else {
		    firstFound = true;
		    data = null;
		}
	    } else
		i++;
	}
	timer.stop();
	frameActionListener.waitForResponse = false;
	frameActionListener.pendingReadRequests
		.remove(receiverAddress.toString());
	return data;
    }
}

/** Listener for take messages from the KNX bus. */
class FrameActionListener implements EICLEventListener {

    /** Requests that needs response. */
    public HashMap<String, byte[]> pendingReadRequests = new HashMap<String, byte[]>();

    public BinarySemaphore available;

    /** If waiting for a response. */
    public boolean waitForResponse = false;

    /** The manager that control this listener. */
    private KNXManager knxManager;

    /** If a conflict on the bus is found. */
    private boolean busyBus = false;

    /**
     * Initialize the class.
     * 
     * @param knxManager
     *            The KNX Manager that controls this listener.
     */
    public FrameActionListener(KNXManager knxManager) {
	this.knxManager = knxManager;
	available = new BinarySemaphore(0);
    }

    /**
     * Called when a new KNX message is received
     * 
     * @param e
     *            The event.
     */
    public synchronized void newFrameReceived(FrameEvent e) {
	CEMI_L_DATA data = (CEMI_L_DATA) e.getPacket();
	if (busyBus)
	    Debug.getInstance().writeln("Conflict on KNX bus for frame: "
		    + data.getDestinationAddress().toString());
	busyBus = true;
	try {
	    String stringValue = knxManager.byte2String(data.getData(),
		    knxManager.groupAddress2DataTypeList
			    .get(data.getDestinationAddress().toString()));
	    Debug.getInstance().writeln("Received data on Konnex bus: " + " from "
		    + data.getSourceAddress().toString() + " to "
		    + data.getDestinationAddress() + " value " + stringValue);
	    knxManager.dumpedGroupAddressValues
		    .put(data.getDestinationAddress().toString(), stringValue);
	} catch (EICLException e2) {
	    e2.printStackTrace();
	}
	// output the resulting message take from the net
	if (waitForResponse) {
	    pendingReadRequests.put(data.getDestinationAddress().toString(),
		    data.getData());
	    available.V();
	}
	// prints data received and find linked services
	knxManager.executeLinkedServices(data);
	busyBus = false;
    }

    /**
     * A disconnect event.
     * 
     * @param e
     *            The event.
     */
    public void serverDisconnected(DisconnectEvent e) {
	Debug.getInstance().writeln("Lost KNX bus connection: " + e.getDisconnectMessage());
	// tries to connect to KNX bus again
	try {
	    knxManager.tunnel.disconnect("client request");
	    knxManager.tunnel = null;
	    knxManager.tryReconnection(20, 30000);
	} catch (EICLException eicle) {
	    eicle.printStackTrace();
	}
    }
}

/** Implements a semaphore with 2 values. */
class BinarySemaphore {

    /** The value of the semaphore. */
    private int value;

    /**
     * Build the semaphore with a initial value.
     * 
     * @param initial
     *            The initial value.
     */
    public BinarySemaphore(int initial) {
	value = initial;
    }

    /** Increment the semaphore variable. */
    synchronized public void V() {
	value = 1; // implement a binary semaphore. No increment needed
	notify();
    }

    /** Decrement the semaphore variable. */
    synchronized public void P() throws InterruptedException {
	while (value == 0)
	    wait();
	value = 0;
    }
}

class Timer extends Thread {
    /** Rate at which timer is checked */
    protected int m_rate = 100;

    /** Length of timeout */
    private int m_length;

    /** Time elapsed */
    private int m_elapsed;

    private BinarySemaphore semaphore;

    /**
     * Creates a timer of a specified length
     * 
     * @param length
     *            Length of time before timeout occurs
     */
    public Timer(int length, BinarySemaphore semaphore) {
	// Assign to member variable
	m_length = length;

	// Set time elapsed
	m_elapsed = 0;

	this.semaphore = semaphore;
    }

    /** Resets the timer back to zero */
    public synchronized void reset() {
	m_elapsed = 0;
    }

    /** Performs timer specific code */
    public void run() {
	// Keep looping
	for (;;) {
	    // Put the timer to sleep
	    try {
		Thread.sleep(m_rate);
	    } catch (InterruptedException ioe) {
		continue;
	    }

	    // Use 'synchronized' to prevent conflicts
	    synchronized (this) {
		// Increment time remaining
		m_elapsed += m_rate;

		// Check to see if the time has been exceeded
		if (m_elapsed > m_length) {
		    // Trigger a timeout
		    timeout();
		}
	    }

	}
    }

    // Override this to provide custom functionality
    public void timeout() {
	Debug.getInstance().writeln("Network timeout occurred... terminating");
	semaphore.V();
    }

    public static boolean isRunningOnWindows() {
	String os = null;
	if (os == null) {
	    os = System.getProperty("os.name");
	}

	System.out.println("OS = " + os);
	if (os.toLowerCase().indexOf("window") >= 0)
	    return true;

	return false;
    }
}
