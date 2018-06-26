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

package domoNetWS;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServlet;

import domoML.DomoMLElement;
import domoML.domoDevice.*;
import domoML.domoMessage.*;
import domoNetWS.techManager.*;
import domoNetWS.techManager.knxManager.*;
import domoNetWS.techManager.domoMLTCPManager.DomoMLTCPManager;
import domoNetWS.techManager.upnpManager.*;
import domoNetWS.techManager.bticinoManager.*;
import domoNetWS.techManager.x10Manager.*;
import common.*;
import com.ha.common.windows.*;

/**
 * Implements the main Web Services class for domoNet network service.
 * 
 * @author Dario Russo
 * @version 1.0
 */
public class DomoNetWS {
	/**
	 * Takes a list of the executed linked services in order to avoid loops
	 */
	private List<DomoDeviceService> executedServices = new LinkedList<DomoDeviceService>();

	/** Counts how many linked services must be already executed */
	private static int aptendedServices = 0;

	/**
	 * A list of all domoDevice currently in use. Using an hash-map in order to
	 * grant an unique identifier for each DomoDevice.
	 */
	public HashMap<DomoDeviceId, DomoDevice> deviceList = new HashMap<DomoDeviceId, DomoDevice>();

	/**
	 * A list of domoManager modules able to connect to the devices. It's possible
	 * to call the interested manager indexing the hash map.
	 */
	private HashMap<DomoDevice.DomoTech, TechManager> managerList = new HashMap<DomoDevice.DomoTech, TechManager>();

	/** A list of client IP to update */
	private ConcurrentHashMap<Socket, DataOutputStream> socketToUpdate = new ConcurrentHashMap<Socket, DataOutputStream>();

	/**
	 * Take a count of the device inserted. This number is used as key in
	 * domoDeviceList in order to grant that for each device inserted correspond
	 * an unique key. This value will be always incremented and must not be
	 * decremented.
	 */
	private static int deviceCount = 0;

	private String TomcatShutdownCommand;

	/**
	 * Detector for windows standby events (uses JNI and DLL library)
	 */
	StandByDetector standByDetector;

	/** Base path for file I/O. */
	public final String basePath;

	/** Constructor that loads all modules and devices for this web service. */
	public DomoNetWS() {
		Debug.getInstance().writeln("Initializing web service...");
		// If windows, run standby detector
		// Also run simple monitoring thread that detects the waking up
		// condition
		// by looking for time jumps
		//
		if (isRunningOnWindows()) {
			System.out.println("Installing Windows standby mode handler");
			standByDetector = new StandByDetector(new StandByRequestListener() {
				public void standByRequested() {
					System.out.println("standby requested");
				}
			});
			standByDetector.setAllowStandby(false);
		}

		HttpServlet servlet = (HttpServlet) MessageContext.getCurrentContext()
				.getProperty(HTTPConstants.MC_HTTP_SERVLET);
		basePath = servlet.getServletContext().getRealPath(".");

		AppProperties prefs;
		try {
			prefs = AppPropertiesCollector.getInstance()
					.getAppProperties(basePath + "/WEB-INF/domoNetWS.properties");
			TomcatShutdownCommand = prefs.getProperty("TomcatShutdownCommand", "");
			printLicenceNote();
			// adding manager modules. The parameter "this" refer to this class
			// and not to the KNXManager class.
			if (prefs.getProperty("KNXEnable", "true").equals("true")) {
				managerList.put(DomoDevice.DomoTech.KNX,
						new KNXManager(prefs.getProperty("KNXServerIP", "192.168.1.4"),
								new Integer(prefs.getProperty("KNXServerPort", "3671")), this));
			}

			// adding UPnP manager
			if (prefs.getProperty("UPNPEnable", "true").equals("true")) {
				managerList.put(DomoDevice.DomoTech.UPNP, new UPNPManager(this));
			}
			// adding BTicino manager
			if (prefs.getProperty("BTicinoEnable", "true").equals("true")) {
				managerList.put(DomoDevice.DomoTech.BTICINO,
						new BTICINOManager(
								prefs.getProperty("BTicinoServerIP", "192.168.1.35"),
								new Integer(prefs.getProperty("BTicinoServerPort", "20000")),
								this));
			}
			// Initialize x10 manager
			if (prefs.getProperty("X10Enable", "true").equals("true")) {
				String test = System.getProperty("os.name");
				test = test.substring(0, 7);
				if (test.compareToIgnoreCase("Windows") == 0)
					managerList.put(DomoDevice.DomoTech.X10,
							new x10Manager("CM11A", "COM1", this));
				else
					managerList.put(DomoDevice.DomoTech.X10,
							new x10Manager("CM11A", "/dev/ttyS0", this));
			}
			// Initialize DomoMLTCP manager
			if (prefs.getProperty("DomoMLTCPEnable", "true").equals("true")) {
				managerList.put(DomoDevice.DomoTech.DOMOML,
						new DomoMLTCPManager(
								prefs.getProperty("DomoMLTCPServerIP", "localhost"),
								new Integer(prefs.getProperty("DomoMLTCPServerPort", "20000")),
								this));
			}
		} catch (SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// load dump file containing a snapshot of DomoDevices
		loadDumpedDomoDevices(basePath + "/xml/domoDevices.xml");
		// now can activate the techManager in order to load the connected
		// devices but not the ones already loaded before.
		Iterator<TechManager> managerListIterator = managerList.values().iterator();
		while (managerListIterator.hasNext()) {
			managerListIterator.next().start();
		}
		// loads dumped client sockets
		Debug.getInstance().writeln("Searching for socket list to update in " + basePath
				+ "/xml/dumpSockets.xml ...");
		loadSocketsToUpdate(basePath + "/xml/dumpSockets.xml");
		Debug.getInstance().writeln("done.");
		Debug.getInstance().writeln("Initialization of the web service terminated.");
	}

	/**
	 * Describe <code>loadDumpedDomoDevices</code> method here.
	 *
	 * @param xmlFile
	 *          a <code>String</code> value
	 */
	public final void loadDumpedDomoDevices(final String xmlFile) {
		// preparing for parsing XML configuration file.
		// checking if a file is empty
		if (xmlFile.trim().equalsIgnoreCase("")) {
			Debug.getInstance().writeln("No previous domoDevice configuration file found.");
		} else {
			File configFile = new File(xmlFile);
			// check if file exists and it's readable
			if (!configFile.canRead()) {
				Debug.getInstance().writeln(xmlFile + " can't be readed."
						+ " Check if file exists or the permission flags.");
			} else {
				List<DomoDevice> configuratedDomoDeviceList = new LinkedList<DomoDevice>();
				try {
					configuratedDomoDeviceList = DomoDevice
							.getDomoDeviceList(readFileAsString(xmlFile));
				} catch (IOException e) {
					e.printStackTrace();
				}
				// have the domoDeviceList. Now must be loaded inside domoNet
				Iterator<DomoDevice> configuratedDomoDeviceListIterator = configuratedDomoDeviceList
						.iterator();
				while (configuratedDomoDeviceListIterator.hasNext()) {
					DomoDevice domoDevice = configuratedDomoDeviceListIterator.next();
					// searching for the correct techManager
					DomoDevice.DomoTech domoTechType = DomoDevice.DomoTech
							.valueOf(domoDevice.getTech());
					TechManager techManager = managerList.get(domoTechType);
					if (techManager != null) {
						// calling the correct loadDomoDevice method
						techManager.loadDumpedDomoDevice(domoDevice);
					}
					// update counter for internal domoAddress id
					if (((Integer) deviceCount) <= new Integer(domoDevice.getId()))
						deviceCount = new Integer(domoDevice.getId()) + 1;
				}
			}
		}
	}

	/**
	 * Add a domo device to this web service in order to make it available.
	 *
	 * @param domoDevice
	 *          The device to add.
	 * @return The domoDeviceId for that device.
	 */
	public final DomoDeviceId addDomoDevice(final DomoDevice domoDevice) {
		String id; // store the identifier of the DomoDevice
		if (domoDevice.getId() != "") {
			// the DomoDevice has already an id
			if (((Integer) deviceCount) < new Integer(domoDevice.getId()))
				deviceCount = new Integer(domoDevice.getId());
			id = Integer.toString(deviceCount);
			deviceCount++;
			Debug.getInstance().write("Adding dumped domoDevice: ");
		} else {
			// convert deviceCount to strings
			id = ((Integer) deviceCount++).toString();
			// need to update id of the device
			domoDevice.setId(id);
			Debug.getInstance().write("Adding new domoDevice: ");
		}
		if (domoDevice.getUrl() == "") {
			// TODO: manage the url
		}
		final DomoDeviceId domoDeviceId = new DomoDeviceId(domoDevice.getUrl(),
				domoDevice.getId());
		deviceList.put(domoDeviceId, domoDevice);
		Debug.getInstance().writeln(deviceList.get(domoDeviceId).toString());
		// sends the update message to clients
		sendClientsUpdate(domoDevice.toString());
		return domoDeviceId;
	}

	/**
	 * Get a DomoDevice of this web service.
	 *
	 * @param domoDeviceId
	 *          The identifier of the domoDevice.
	 * @return The domoDevice requested.
	 */
	public final DomoDevice getDomoDevice(final DomoDeviceId domoDeviceId) {
		return deviceList.get(domoDeviceId);
	}

	/**
	 * Remove a device from this web service.
	 * 
	 * @param domoDeviceId
	 *          a <code>DomoDeviceId</code> value
	 */
	public final void removeDomoDevice(DomoDeviceId domoDeviceId) {
		Debug.getInstance().writeln("Removing domoDevice: " + deviceList.get(domoDeviceId));
		deviceList.remove(domoDeviceId);
		sendClientsUpdate(new DomoMessage("", domoDeviceId.getId(), "",
				domoDeviceId.getId(), "", DomoMessage.MessageType.REMOVE).toString());
	}

	/**
	 * Execute a domoML command.
	 *
	 * @param messageString
	 *          The message in DomoML string.
	 * @return The response message in DomoML.
	 */
	public final String execute(final String messageString) {
		Debug.getInstance().writeln("Executing on domonet: " + messageString);
		// initialize a domoMessage to be returned at the end of the method
		String responseMessage = "";
		// preparing for parsing passed DomoDevice list configuration.
		try {
			// create the domoMessage
			DomoMessage domoMessage = new DomoMessage(messageString);
			// searching for the domomessage receiver device in order to
			// invoke the correct techmanager
			DomoDevice.DomoTech domoTechType = DomoDevice.DomoTech
					.valueOf(deviceList.get(new DomoDeviceId(domoMessage.getReceiverURL(),
							domoMessage.getReceiverId())).getTech());
			if (domoTechType != null)
				// calling the correct execute method
				responseMessage = (managerList.get(domoTechType)).execute(domoMessage)
						.toString();
		} catch (Exception e) {
			responseMessage = new DomoMessage("", "", "", "", "",
					DomoMessage.MessageType.FAILURE).toString();
		}
		Debug.getInstance().writeln("Returning: " + responseMessage);
		return responseMessage;
	}

	/**
	 * Search for &quot;linkedService&quot; tags for the provided. If found, it's
	 * generated and executed the corresponding domoMessage.
	 * 
	 * @param service
	 *          The service to be checked in order to find
	 *          &quot;linkedService&quot; tags.
	 * @param sourceDevice
	 *          The url and id of the domoDevice that has the service.
	 * @param stringValue
	 *          The input to be used in the new domoMessage.
	 * @param dataType
	 *          The datatype of the input.
	 */
	public final void searchAndExecuteLinkedServices(
			final DomoDeviceService service, final DomoDeviceId sourceDevice,
			final String stringValue, DomoDevice.DataType dataType) {
		// reset value
		boolean serviceAlreadyExecuted = false;
		/*
		 * System.out.println("Chiamo searchAndExecute con aptendedServices: " +
		 * aptendedServices + " servizio: " + service.toString());
		 */
		// if not waiting for the message of invoked linked service
		// can clear the list of invoked linked services
		// if(aptendedServices == 0)
		// executedServices.clear();

		Iterator<DomoDeviceService> executedServicesIt = executedServices
				.iterator();
		// System.out.println("Cerco se il servizio e' in lista di attesa: ");
		while (executedServicesIt.hasNext() /* && !serviceAlreadyExecuted */) {
			DomoDeviceService executedService = executedServicesIt.next();
			// System.out.println("Servizio in lista: " +
			// executedService.toString());
			if (executedService.toString().equalsIgnoreCase(service.toString())) {
				// the service called was in the list so it was aptended and
				// now will be executed
				serviceAlreadyExecuted = true;
			}
		}

		if (aptendedServices != 0)
			// waiting for executed services.
			// If this is a call derived by an executed linked service
			// must decrement the counter of the aptendedServices
			if (serviceAlreadyExecuted) {
			aptendedServices--;
			// System.out.println("Decremento aptendedServices: ora sta a "
			// + aptendedServices);
			}

		if (!serviceAlreadyExecuted) {
			// searches for linked services
			// System.out.println("Searching for service: " + service.toString()
			// +
			// " with value " + stringValue + " and dataType " +
			// dataType.toString());

			// searching for linkedServiceToCheck tag
			Iterator<DomoDeviceServiceLinkedService> linkedServiceIterator = service
					.getLinkedServices().iterator();
			while (linkedServiceIterator.hasNext()) {
				DomoDeviceServiceLinkedService ddsls = linkedServiceIterator.next();
				// tests if condition for the execution is required
				String ifInput = ddsls.getIfInput();
				String hasValue = ddsls.getHasValue();
				boolean executeLinkedService = true;
				if (ifInput.compareTo("") != 0 && hasValue.compareTo("") != 0
						&& hasValue.compareTo(stringValue) != 0)
					executeLinkedService = false;
				// if (ddsls.getUrl().equalsIgnoreCase("")) {
				// the linked device is on the same web serviceToCheck.
				// getting the device which serviceToCheck has linked to
				// System.out.println("Execute linked service? " +
				// executeLinkedService);
				if (executeLinkedService) {
					DomoDevice linkedDevice = getDomoDevice(
							new DomoDeviceId("", ddsls.getId()));
					// System.out.println("Ho riconosciuto l'id del domoDevice
					// destinatario del linkedService: "
					// + ddsls.getId());
					// getting the service linked
					try {
						Iterator<DomoDeviceService> linkedServiceIt = linkedDevice
								.getService(ddsls.getService()).iterator();
						while (linkedServiceIt.hasNext()) {

							DomoDeviceService linkedService = linkedServiceIt.next();
							// check if the linkedService is already on the list
							// of executed
							// linkedServices
							Iterator<DomoDeviceService> executedServicesIt2 = executedServices
									.iterator();
							boolean found = false;
							while (executedServicesIt2.hasNext()/* && !found */) {
								DomoDeviceService executedService = executedServicesIt2.next();
								// System.out.println("Confronto servizio in
								// lista: " + executedService.toString() +
								// " con il linked service: " +
								// linkedService.toString());
								if (executedService.toString()
										.equalsIgnoreCase(linkedService.toString())) {
									// the service called was in the list so it
									// was attended and
									// now will be executed
									found = true;
								}
							}

							// System.out.println("Trovato linkedService tra
							// quelli in lista? " + found);
							/* if(!found) { */
							// System.out.println("Servizio mai invocato!");
							// build the domoMessage to be executed
							DomoMessage linkedDomoMessage = new DomoMessage(
									sourceDevice.getUrl(), sourceDevice.getId(), ddsls.getUrl(),
									ddsls.getId(), linkedService.getName(),
									DomoMessage.MessageType.COMMAND);
							/* Dirty trick to fix */
							if (linkedDevice.getTech().equalsIgnoreCase("KNX"))
								dataType = DomoDevice.DataType.BOOLEAN;
							// add input tags considering the linked values
							Iterator<DomoDeviceServiceLinkedServiceInput> ddslsii = ddsls
									.getInputs().iterator();
							while (ddslsii.hasNext()) {
								DomoDeviceServiceLinkedServiceInput ddslsi = (DomoDeviceServiceLinkedServiceInput) ddslsii
										.next();
								if (ddslsi.getFrom().isEmpty())
									// constant was set inside the
									// linkedInput
									linkedDomoMessage.addInput(ddslsi.getTo(), ddslsi.getValue(),
											dataType);
								else
									linkedDomoMessage.addInput(ddslsi.getTo(), stringValue,
											dataType);
								/* } */
								Debug.getInstance().writeln(
										"Executing linkedService: " + linkedDomoMessage.toString());
								// the service was not yet executed
								// executedServices.add(linkedService);
								// System.out.println("Messo in lista: " +
								// linkedService);
								// the number of aptendedServices depends by how
								// many services with the same name has the
								// device
								aptendedServices = aptendedServices + linkedDevice
										.getNamedServices(linkedService.getName()).size();
								// System.out.println("aptendedServices
								// incrementato: " + aptendedServices);
								execute(linkedDomoMessage.toString());
								// System.out.println("FINE ESECUZIONE LINKED
								// SERVICE!");
							} // else do nothing. The service was just called
								// once so loop occurs
							/*
							 * else { //System.out.println( "Servizio gia' invocato!"); found
							 * = false; }
							 */
							// } // to do: linked ServiceToCheck from different
							// web serviceToChecks
						}
					} catch (NoElementFoundException nefe) {
						Debug.getInstance().writeln("Not found any domoDevice with that service!");
					}
				}
			}
		} else {
			// reset value
			serviceAlreadyExecuted = false;
		}
	}

	/**
	 * Get an XML list of the devices (empty too) connected to this web services.
	 *
	 * @return The XML string that represent the devices connected to this web
	 *         services.
	 */
	public final String getAllDomoDeviceList() {
		// the return string initialized as empty
		StringBuffer domoDeviceList = new StringBuffer();
		// preamble for concatenate each device of the web services
		domoDeviceList = domoDeviceList.append("<devices>");
		Iterator<DomoDevice> deviceListIt = deviceList.values().iterator();
		while (deviceListIt.hasNext()) {
			DomoDevice currentDomoDevice = deviceListIt.next();
			DomoMLElement domoMLElement = ((DomoMLElement) currentDomoDevice
					.getDocumentElement());
			try {
				// concatenate each device of the web services to the return string
				// without the document preamble.
				domoDeviceList.append(domoMLElement.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// end for the preamble
		domoDeviceList = domoDeviceList.append("</devices>");
		Debug.getInstance().writeln("Give the list of devices: " + domoDeviceList);
		return domoDeviceList.toString();
	}

	/**
	 * Get an XML list of the devices connected to this web services.
	 *
	 * @return The XML string that represent the devices connected to this web
	 *         services.
	 */
	public final String getDomoDeviceList() {
		// the return string initialized as empty
		StringBuffer domoDeviceList = new StringBuffer();
		// preamble for concatenate each device of the web services
		domoDeviceList = domoDeviceList.append("<devices>");
		Iterator<DomoDevice> deviceListIt = deviceList.values().iterator();
		while (deviceListIt.hasNext()) {
			DomoDevice currentDomoDevice = deviceListIt.next();
			// not interested to domoDevice without services
			if (currentDomoDevice.getServices().size() != 0 &&
			// not interested to BTICINO devices... for the moment
					!currentDomoDevice.getTech().equalsIgnoreCase("BTICINO")) {
				DomoMLElement domoMLElement = ((DomoMLElement) currentDomoDevice
						.getDocumentElement());
				try {
					// concatenate each device of the web services to the return
					// string without the document preamble.
					domoDeviceList.append(domoMLElement.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		// end for the preamble
		domoDeviceList = domoDeviceList.append("</devices>");
		Debug.getInstance().writeln("Give the list of devices: " + domoDeviceList);
		return domoDeviceList.toString();
	}

	/**
	 * Register an IP with port in order to receive updates regarding the state of
	 * domoDevices.
	 * 
	 * @param ip
	 *          the IP to register
	 * @param port
	 *          the port number
	 */
	public final Socket registerToClientsUpdate(String ip, int port) {
		// Create the new socket
		Debug.getInstance().write("Trying to create socket " + ip + ":" + port + "... ");
		Socket socket = null;
		DataOutputStream os = null;
		try {
			socket = new Socket(ip, port);
			os = new DataOutputStream(socket.getOutputStream());
			Debug.getInstance().writeln("Success.");
		} catch (UnknownHostException e) {
			Debug.getInstance().writeln("Unknow host");
			try {
				if (socket != null)
					socket.close();
			} catch (IOException ioe1) {
				ioe1.printStackTrace();
			}
		} catch (IOException ioe2) {
			Debug.getInstance().writeln("Can't create socket.");
			try {
				if (socket != null)
					socket.close();
			} catch (IOException ioe3) {
				ioe3.printStackTrace();
			}
		}
		if (socket != null && os != null) {
			// The new socket is valid.
			// Check if IP and port were already registered.
			// If so, old socket is removed and in any case, the new socket is
			// inserted.
			Iterator<Socket> socketIterator = socketToUpdate.keySet().iterator();
			boolean found = false;
			while (socketIterator.hasNext() && !found) {
				Socket currentSocket = socketIterator.next();
				if (new String(
						currentSocket.getInetAddress() + ":" + currentSocket.getPort())
								.equals(new String("/" + ip + ":" + port))) {
					found = true;
					Debug.getInstance().writeln("Removing socket " + currentSocket.getInetAddress()
							+ ":" + currentSocket.getPort()
							+ " from the list of clients to update.");
					try {
						socketToUpdate.get(currentSocket).close();
						currentSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					socketToUpdate.remove(currentSocket);
				}
			}
			Debug.getInstance().writeln("Inserting socket " + socket.getInetAddress() + ":"
					+ socket.getPort() + " to the list of clients to update.");
			socketToUpdate.put(socket, os);
		}
		return socket;
	}

	public final void registerToClientsUpdatePort(String port) {
		MessageContext curContext = MessageContext.getCurrentContext();

		String remoteIp = "Unknown";
		if (curContext != null) {
			Object ipProperty = curContext.getProperty(Constants.MC_REMOTE_ADDR);
			remoteIp = ipProperty.toString();
		}
		registerToClientsUpdate(remoteIp, new Integer(port));
	}

	/**
	 * Sends update to all registered clients
	 * 
	 * @param domoMessage
	 *          the message to send to all registered clients
	 */
	public final void sendClientsUpdate(String domoMessage) {
		// TODO: REMOVE THIS LINE: HACK FOR SOCIALIZE
		loadSocketsToUpdate(basePath + "/xml/dumpSockets.xml");
		Debug.getInstance().write("Sending update message: " + domoMessage.toString() + " to ");
		Iterator<Socket> socketIterator = socketToUpdate.keySet().iterator();
		while (socketIterator.hasNext()) {
			Socket currentSocket = socketIterator.next();
			Debug.getInstance().write(
					currentSocket.getInetAddress() + ":" + currentSocket.getPort() + " ");
			try {
				DataOutputStream os = socketToUpdate.get(currentSocket);
				os.writeBytes(domoMessage + '\n');
				os.flush();
				os.close();
				// TODO: REMOVE THIS LINE: HACK FOR SOCIALIZE
				socketToUpdate.remove(currentSocket);
			} catch (IOException ioe1) {
				Debug.getInstance().writeln(
						"... can't connect. Trying to establish a new connection.");
				currentSocket = registerToClientsUpdate(
						currentSocket.getInetAddress().toString().substring(1),
						currentSocket.getPort());
				if (currentSocket != null) {
					DataOutputStream os = socketToUpdate.get(currentSocket);
					try {
						os.writeBytes(domoMessage + '\n');
						os.flush();
					} catch (IOException ioe2) {
						Debug.getInstance().writeln("... error on socket.");
					}
				} else {
					Debug.getInstance().writeln("Skipping this time.");
				}
			}
		}
	}
	/*
	 * public final void sendClientsUpdate(String domoMessage) { Debug.getInstance().write(
	 * "Sending update message: " + domoMessage.toString() + " to ");
	 * Iterator<Socket> socketIterator = socketToUpdate.keySet().iterator(); while
	 * (socketIterator.hasNext()) { Socket currentSocket = socketIterator.next();
	 * PrintWriter os = socketToUpdate.get(currentSocket);
	 * os.println(domoMessage.toString()); os.flush(); Debug.getInstance().write(
	 * currentSocket.getInetAddress() + ":" + currentSocket.getPort() + " "); } }
	 */

	/**
	 * Updates domoDevices.
	 * 
	 * @param domoDevices
	 *          The xml string that rappresent the new configuration of
	 *          domoDevices
	 */
	public final void domoDevicesUpdate(String domoDevices) {
		Debug.getInstance().writeln("Updating domoDevice list: " + domoDevices);
		List<DomoDevice> configuratedDomoDeviceList = new LinkedList<DomoDevice>();
		configuratedDomoDeviceList = DomoDevice.getDomoDeviceList(domoDevices);
		// have the new domoDeviceList updated.
		// Now must update the current configuration
		Iterator<DomoDevice> configuratedDomoDeviceListIterator = configuratedDomoDeviceList
				.iterator();
		while (configuratedDomoDeviceListIterator.hasNext()) {
			DomoDevice domoDevice = configuratedDomoDeviceListIterator.next();
			domoDevice.setUrl("");
			DomoDevice originalDomoDevice = deviceList
					.get(new DomoDeviceId("", domoDevice.getId()));
			if (originalDomoDevice != null && !domoDevice.toString()
					.equalsIgnoreCase(originalDomoDevice.toString())) {
				Debug.getInstance().writeln("Updating " + originalDomoDevice + " with " + domoDevice);
				deviceList.put(new DomoDeviceId("", domoDevice.getId()), domoDevice);
			}
		}

		// dumping new configuration to file
		String toDump = getAllDomoDeviceList();
		File f = new File("xml/domoDevices.xml");
		try {
			FileOutputStream fos = new FileOutputStream(f);
			PrintStream ps = new PrintStream(fos);
			ps.println(toDump);
			ps.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/** Write to the Standard output the license of DomoNet. */
	private final void printLicenceNote() {
		System.out.println("************************************************");
		System.out.println("** DomoNet                                    **");
		System.out.println("**                                            **");
		System.out.println("** Copyright(C) 2006 Dario Russo              **");
		System.out.println("** DomoNet comes with ABSOLUTELY NO WARRANTY. **");
		System.out.println("** This is free software, and you are welcome **");
		System.out.println("** to redistribuite it under certain          **");
		System.out.println("** conditions. For details see COPYING file.  **");
		System.out.println("************************************************");
	}

	/** Finalize the class when web service ends. */
	public final void finalize() {
		Debug.getInstance().writeln("Called finalize");

		Debug.getInstance().write("Dumping socket connections...");
		HttpServlet servlet = (HttpServlet) MessageContext.getCurrentContext()
				.getProperty(HTTPConstants.MC_HTTP_SERVLET);
		String basePath = servlet.getServletContext().getRealPath(".");
		dumpSocketsToUpdate(basePath + "/xml/dumpSockets.xml");

		Debug.getInstance().write("Shutting down socket connections...");
		Iterator<Socket> socketIterator = socketToUpdate.keySet().iterator();
		while (socketIterator.hasNext()) {
			Socket currentSocket = socketIterator.next();
			try {
				socketToUpdate.get(currentSocket).close();
				currentSocket.close();
				socketToUpdate.remove(currentSocket);
			} catch (IOException e) {
				socketToUpdate.remove(currentSocket);
			}
		}
		Debug.getInstance().writeln("done.");

		Debug.getInstance().write("Shutting down DomoNet WebService modules...");
		Iterator<TechManager> managerListIterator = managerList.values().iterator();
		while (managerListIterator.hasNext())
			managerListIterator.next().finalize();
		Debug.getInstance().writeln("done.");

		Debug.getInstance().write("Shutting down Tomcat modules...");
		try {
			Runtime.getRuntime().exec(TomcatShutdownCommand);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Read a file and return a string containing the file content.
	 * 
	 * @param filePath
	 *          The name of the file to open. Not sure if it can accept URLs or
	 *          just filenames. Path handling could be better, and buffer sizes
	 *          are hard-coded.
	 * @return The string.
	 */
	private static String readFileAsString(String filePath)
			throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

	/**
	 * Dumps current clients ip and port number to file
	 * 
	 * @param filePath
	 *          the file where to dump the informations
	 */
	public final void dumpSocketsToUpdate(String filePath) {
		AppProperties appPro;
		try {
			appPro = AppPropertiesCollector.getInstance()
					.getAppProperties(filePath);

			int i = 0;
			Iterator<Socket> socketIterator = socketToUpdate.keySet().iterator();
			while (socketIterator.hasNext()) {
				Socket currentSocket = socketIterator.next();
				appPro.setProperty("socket" + i++,
						currentSocket.getInetAddress().toString().substring(1) + ":"
								+ currentSocket.getPort());
			}
			appPro.dumpPropertiesToFile();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Loads the clients ip and port number stored in a file
	 * 
	 * @param filePath
	 *          the file where load the informations
	 */
	public final void loadSocketsToUpdate(String filePath) {
		AppProperties appPro;
		try {
			appPro = AppPropertiesCollector.getInstance().getAppProperties(filePath);

			Iterator<?> appProIt = appPro.getPropertiesKeySet().iterator();

			while (appProIt.hasNext()) {
				String key = ((String) appProIt.next());
				String url;
				url = appPro.getProperty(key);

				registerToClientsUpdate(url.substring(0, url.indexOf(':')),
						new Integer(url.substring(url.indexOf(':') + 1)));
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
