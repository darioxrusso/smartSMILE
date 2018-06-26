package domoNetWS.techManager.bticinoManager;

import domoML.domoDevice.*;
import domoML.domoMessage.*;
import domoML.DomoMLDocument;
import domoNetWS.*;
import domoNetWS.techManager.*;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.SAXException;

import java.io.FileReader;
import java.io.IOException;

import org.w3c.dom.*;

import common.Debug;

import tuwien.auto.eicl.util.EICLException;

import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Iterator;

public class BTICINOManager extends TechManager {

	/**
	 * Map the string that rappresent the datatype used in the upnp xml
	 * description to the corresponding DomoDevice.DataType.
	 */
	protected HashMap<String, DomoDevice.DataType> string2DataType = new HashMap<String, DomoDevice.DataType>();

	protected HashMap<String, String> valueDevice = new HashMap<String, String>();

	CommandSession cmdSes;
	MonitorSession cmdMon;
	String myHostBiticino;
	VideoServer threadVideo = null;
	IntraVideoComunication videoBticino = null;

	// create an empty domoMessage to store the last sent
	String lastSentUpdateMessage = new DomoMessage().toString();

	public BTICINOManager(final String host, final int port, final DomoNetWS domoNetWS) {
		super(host, port, domoNetWS);
		myHostBiticino = host;
		Debug.getInstance().writeln("   Loading " + this.getClass().toString() + " to " + host + ":" + port + "...");
		initDataType();
		Debug.getInstance().writeln("done.");
	}

	/** Perform the connection to the BTicino WebServer. */
	public void performConnection() throws IOException {
		cmdSes = new CommandSession(this.getHost(), this.getPort());
		if (cmdSes.test()) {
			cmdMon = new MonitorSession(this.getHost(), this.getPort(), this);
			cmdMon.start();
		} else
			throw new IOException();

	}

	public final void addDevice(final DomoDevice domoDevice, final String address) {
		// call the addDevice of the DomoNetWS to adds it.
		// The DomoNetWS returns the domoDeviceId so it's possible fill
		// the doubleHash class correctly.
		DomoDeviceId ddid = domoNetWS.addDomoDevice(domoDevice);
		doubleHash.add(ddid, address);
	}

	/**
	 * Execute a domoMessage converting it to a message for the BTICINO bus.
	 * 
	 * @param domoMessage
	 *            the domoMessage to be converted and executed.
	 * @return The domoMessage as response of the one to be executed.
	 */
	public final DomoMessage execute(final DomoMessage domoMessage) {
		String value = "";
		// Debug.getInstance().write("Arriving: "+domoMessage.toString());

		if (domoMessage.getMessageType().equals((DomoMessage.MessageType.COMMAND).toString())) {
			String myId = domoMessage.getReceiverId();
			String myUrl = domoMessage.getReceiverURL();

			String myAddress = doubleHash.getAddress(new DomoDeviceId(myUrl, myId));
			String chi = selectCHI(myAddress);
			String dove = selectDOVE(myAddress);

			List<Node> listInput = domoMessage.getInputParameterElements();
			for (int i = 0; i < listInput.size(); i++) {
				if (i == 0)
					value = listInput.get(i).getAttributes().getNamedItem("value").getNodeValue();
				else
					value = value + "#" + listInput.get(i).getAttributes().getNamedItem("value").getNodeValue();

			}
			// try{
			// Controllo se il messaggio prevede un output di tipo stream
			// if(domoMessage.getOutput().equals((DomoMLDocument.DataType.STREAM))){

			// Comando accensione video
			if (chi.equals("7")) {
				if (threadVideo == null) {
					videoBticino = new IntraVideoComunication(myHostBiticino, 10000);
					threadVideo = new VideoServer(8888);

				}
				if (value.equals("0")) {

					videoBticino.startThread();
					threadVideo.startThread();
					Debug.getInstance().writeln("AVVIATO");

				} else {
					videoBticino.stopThread();
					threadVideo.stopThread();
				}

			}

			// creare messaggio di output
			// return new DomoMessage("", "", "", "",
			// super.getHost()+":"+threadVideo.getInterPort(),
			// DomoMessage.MessageType.SUCCESS);

			// }
			// }catch(domoML.domoMessage.NoAttributeFoundException eNAFE){
			String command = "";

			if (value.startsWith("#")) {
				// Richiesta di stato
				command = "*#" + chi + "*" + dove;
				Debug.getInstance().writeln("COMMAND BITICINO RICHIESTA STATO " + command);

			} else {
				// Comando
				command = "*" + chi + "*" + value + "*" + dove;
				Debug.getInstance().writeln("COMMAND BITICINO " + command);
			}

			try {
				if (cmdSes.executeCommand(command)) {
					// this.getDomoNetWS().searchAndExecuteLinkedServices(this.getDomoNetWS().getDomoDevice(new
					// DomoDeviceId(domoMessage.getReceiverURL(),domoMessage.getReceiverId())).getService(domoMessage.getMessage()),
					// new
					// DomoDeviceId(domoMessage.getReceiverURL(),domoMessage.getReceiverId()),
					// value, DomoDevice.DataType.STRING );
					return new DomoMessage("", "", "", "", "No response value", DomoMessage.MessageType.SUCCESS);
				} else
					return new DomoMessage("", "", "", "", "No response value", DomoMessage.MessageType.FAILURE);

				// }catch(NoElementFoundException e){
				// return new DomoMessage("", "", "", "", "No response value",
				// DomoMessage.MessageType.SUCCESS);}

			} catch (IOException e) {
			}

			// }
		}

		return new DomoMessage("", "", "", "", "No response value", DomoMessage.MessageType.FAILURE);
	}

	// TODO
	public void loadDumpedDomoDevice(DomoDevice domoDevice) {

		addDevice(domoDevice, domoDevice.getSerialNumber());

	}

	public void start() {

		Debug.getInstance().writeln(" Starting  " + this.getClass().toString() + " using " + getHost() + ":" + getPort() + "... ");
		try {
			performConnection();
			Debug.getInstance().writeln("Biticino connection  success ");
		} catch (IOException e) {
			try {
				performConnection();
				Debug.getInstance().writeln("Biticino connection success ");
			} catch (IOException e2) {
				Debug.getInstance().writeln("Biticino connection failed ");
				// e2.printStackTrace();
			}
		}

	}

	/** Finalize the class when the manager ends. */
	public void finalize() {
		cmdMon.close();
	}

	public void monitor(String message) {
		Debug.getInstance().writeln("MONITOR BITICINO " + message);
		DomoDeviceId ddid = null;
		DomoDevice device;

		String cosa = selectCOSAWhitOutOption(message);
		String address = "*" + selectCHI(message) + "**" + selectDOVE(message);

		List<DomoDeviceId> ddidList = doubleHash.getDomoDeviceId(address);
		Debug.getInstance().writeln("MONITOR device: " + address);

		// Control if exist device with this address
		if (ddidList != null) {
			Iterator ddidListIterator = ddidList.iterator();
			if (ddidListIterator.hasNext()) {
				ddid = (DomoDeviceId) ddidListIterator.next();
				Debug.getInstance().writeln("MONITOR id: " + ddid.getId());
			}
		}
		if (ddid != null) {
			device = domoNetWS.getDomoDevice(ddid);
			Iterator serviceListIterator = device.getServices().iterator();
			// Find in all service of this device
			while (serviceListIterator.hasNext()) {
				DomoDeviceService domoDeviceService = (DomoDeviceService) serviceListIterator.next();

				Iterator inputListIterator = domoDeviceService.getInputs().iterator();
				// Find only first input
				if (inputListIterator.hasNext()) {
					DomoDeviceServiceInput firstInput = (DomoDeviceServiceInput) inputListIterator.next();
					Iterator inputAllowedListIterator = firstInput.getAllowed().iterator();

					// Controllo se il value e' uno di quelli permessi
					while (inputAllowedListIterator.hasNext()) {
						if (((DomoDeviceServiceInputAllowed) inputAllowedListIterator.next()).getValue().equals(cosa)
								&& ifNewValue(address, cosa)) {

							DomoMessage updateMessage = new DomoMessage("", ddid.getId(), "", ddid.getId(),
									domoDeviceService.getName(), DomoMessage.MessageType.UPDATE);
							updateMessage.addInput("value", cosa, firstInput.getType());

							if (!lastSentUpdateMessage.equals(updateMessage.toString())) {
								domoNetWS.sendClientsUpdate(updateMessage.toString());
								lastSentUpdateMessage = updateMessage.toString();
							}

							this.getDomoNetWS().searchAndExecuteLinkedServices(domoDeviceService, ddid, cosa,
									firstInput.getType());
							// if it has found one corrispondence exit
							break;
						}
						/*
						 * //Control if for this service exsist a linkedService
						 * Iterator
						 * linkedServiceListIterator=(domoDeviceService.
						 * getLinkedServices()).iterator();
						 * while(linkedServiceListIterator.hasNext()){
						 * DomoDeviceServiceLinkedService
						 * linkedService=(DomoDeviceServiceLinkedService)
						 * linkedServiceListIterator.next(); Iterator
						 * linkedServiceInputListIterator=(linkedService.
						 * getInputs()).iterator(); DomoMessage domoMessage=new
						 * DomoMessage(super.domoNetWS.);
						 * while(linkedServiceInputListIterator.hasNext()){
						 * linkedServiceInputListIterator } } //if it has found
						 * one corrispondence exit break;
						 * 
						 * }
						 * 
						 */
					}

				}
			}

		}
	}

	private boolean ifNewValue(String deviceAddress, String value) {
		if (valueDevice.containsKey(deviceAddress)) {
			if (valueDevice.get(deviceAddress).equals(value)) {
				return false;
			} else {
				valueDevice.put(deviceAddress, value);
				return true;
			}

		} else {
			valueDevice.put(deviceAddress, value);
			return true;

		}

	}

	private String selectCHI(String address) {
		StringTokenizer stringTokenizer = new StringTokenizer(address, "*");
		if (stringTokenizer.hasMoreElements())
			return stringTokenizer.nextToken();
		else
			return null;

	}

	private String selectDOVE(String address) {
		StringTokenizer stringTokenizer = new StringTokenizer(address, "*");
		int countToken = stringTokenizer.countTokens();
		if (countToken == 2) {// If COSA is not specified
			stringTokenizer.nextToken();
			return stringTokenizer.nextToken();
		}
		if (countToken == 3) {// If COSA is specified
			stringTokenizer.nextToken();
			stringTokenizer.nextToken();
			return stringTokenizer.nextToken();
		} else
			return null;
	}

	private String selectCOSA(String address) {
		StringTokenizer stringTokenizer = new StringTokenizer(address, "*");
		int countToken = stringTokenizer.countTokens();
		if (countToken == 2) {// If COSA is not specified
			return "";
		}
		if (countToken == 3) {// If COSA is specified
			stringTokenizer.nextToken();
			return stringTokenizer.nextToken();
		} else
			return null;
	}

	private String selectCOSAWhitOutOption(String string) {
		String cosa = selectCOSA(string);
		if (!cosa.equals("")) {
			StringTokenizer stringTokenizer2 = new StringTokenizer(cosa, "#");
			return stringTokenizer2.nextToken();
		} else
			return "";
	}

	private final void initDataType() {
		string2DataType.put("string", DomoDevice.DataType.STRING);
		string2DataType.put("STREAM", DomoDevice.DataType.STREAM);
	}

	/*
	 * private void loadService(){ serviceChiOne.put("0", "SET_STATUS_ON_OFF");
	 * serviceChiOne.put("1", "SET_STATUS_ON_OFF"); serviceChiOne.put("2",
	 * "SET_ON_INTENSITY"); serviceChiOne.put("3", "SET_ON_INTENSITY");
	 * serviceChiOne.put("4", "SET_ON_INTENSITY"); serviceChiOne.put("5",
	 * "SET_ON_INTENSITY"); serviceChiOne.put("6", "SET_ON_INTENSITY");
	 * serviceChiOne.put("7", "SET_ON_INTENSITY"); serviceChiOne.put("8",
	 * "SET_ON_INTENSITY"); serviceChiOne.put("9", "SET_ON_INTENSITY");
	 * serviceChiOne.put("10", "SET_ON_INTENSITY");
	 * 
	 * }
	 */
}
