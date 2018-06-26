package domoNetWS.techManager.domoMLTCPManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.xml.sax.SAXException;

import common.AppProperties;
import common.AppPropertiesCollector;
import common.Debug;
import domoML.domoDevice.DomoDevice;
import domoML.domoDevice.DomoDeviceId;
import domoML.domoMessage.DomoMessage;
import domoML.domoMessage.DomoMessage.MessageType;
import domoNetWS.techManager.domoMLTCPManager.ClientServiceThread;
import domoNetWS.techManager.domoMLTCPManager.DomoMLTCPManager;

public class DomoMLTCPTCPServer extends Thread {

	/**
	 * Configuration file where the manager takes parameters like TCP server port
	 * to listen messages of type DomoML.
	 */
	private static String CONFIG_FILE = "src/domoNetWS/techManager/domoMLTCPManager/domoMLTCPManager.preferences";

	AppProperties prefs;

	ServerSocket myServerSocket;
	boolean ServerOn = true;
	DomoMLTCPManager manager;

	public DomoMLTCPTCPServer(DomoMLTCPManager manager) {
		this.manager = manager;
	}

	public void run() {
		try {
			prefs = AppPropertiesCollector.getInstance()
					.getAppProperties(CONFIG_FILE);

			myServerSocket = new ServerSocket(
					new Integer(prefs.getProperty("socketPort", "7779")));
		} catch (IOException | SAXException ioe) {
			System.out
					.println("Could not create server socket on port 7779. Quitting.");
			System.exit(-1);
		}

		// Successfully created Server Socket. Now wait for connections.
		while (ServerOn) {
			try {
				// Accept incoming connections.
				Socket socket = myServerSocket.accept();

				// accept() will block until a client connects to the server.
				// If execution reaches this point, then it means that a client
				// socket has been accepted.

				// For each client, we will start a service thread to
				// service the client requests. This is to demonstrate a
				// Multi-Threaded server. Starting a thread also lets our
				// MultiThreadedSocketServer accept multiple connections
				// simultaneously.

				// Start a Service thread
				ClientServiceThread cliThread = new ClientServiceThread(socket,
						manager);
				cliThread.start();

			} catch (IOException ioe) {
				System.out.println(
						"Exception encountered on accept. Ignoring. Stack Trace :");
				ioe.printStackTrace();
			}
		}
		try {
			myServerSocket.close();
			System.out.println("Server Stopped");
		} catch (Exception ioe) {
			System.out.println("Problem stopping server socket");
			System.exit(-1);
		}
	}
}

class ClientServiceThread extends Thread {
	Socket myClientSocket;
	boolean m_bRunThread = true;
	boolean ServerOn = true;
	DomoMLTCPManager manager;

	public ClientServiceThread() {
		super();
	}

	ClientServiceThread(Socket s, DomoMLTCPManager manager) {
		myClientSocket = s;
		this.manager = manager;
	}

	public void run() {
		// Obtain the input stream and the output stream for the socket
		// A good practice is to encapsulate them with a BufferedReader
		// and a PrintWriter as shown below.
		BufferedReader in = null;
		PrintWriter out = null;

		// Print out details of this connection
		// Listener server info
		InetAddress indirizzo = myClientSocket.getInetAddress();
		String server = indirizzo.getHostAddress();
		int port = myClientSocket.getLocalPort();
		Debug.getInstance().writeln("Initializing DomoML socket on " + server + ":" + port);

		try {
			in = new BufferedReader(
					new InputStreamReader(myClientSocket.getInputStream()));
			out = new PrintWriter(
					new OutputStreamWriter(myClientSocket.getOutputStream()));

			// At this point, we can read for input and reply with appropriate
			// output.

			// Run in a loop until m_bRunThread is set to false
			while (m_bRunThread) {
				// read incoming stream
				String clientCommand = in.readLine();
				if (clientCommand == null || clientCommand.equalsIgnoreCase("close")) {
					m_bRunThread = false;
					System.out.println(
							"Close connection by client: " + server + ":" + port + "... ");
				} else {
					try {
						System.out.println("Readed from DomoML socket: " + clientCommand);
						if (clientCommand.startsWith("<devices"))
							manager.addListOfDevices(clientCommand);
						else if (clientCommand.startsWith("<device")) {
							Debug.getInstance().writeln("Adding device " + clientCommand);
							DomoDevice domoDevice = new DomoDevice(clientCommand);
							manager.addDevice(domoDevice, domoDevice.getSerialNumber());
							DomoDeviceId id = manager
									.getDomoDeviceId(domoDevice.getSerialNumber());
							out.println(new DomoMessage(id.getUrl(), id.getId(), "", "", "",
									MessageType.SUCCESS));
							out.flush();
							System.out.println("Reply: " + new DomoMessage(id.getUrl(),
									id.getId(), "", "", "", MessageType.SUCCESS).toString());
						} else if (clientCommand.startsWith("<message")) {
							DomoMessage message = new DomoMessage(clientCommand);
							if (message.getMessageType().equals("UPDATE")) {
								manager.searchAndExecuteLinkedServices(
										new DomoMessage(clientCommand));
								out.println(new DomoMessage(message.getSenderURL(),
										message.getSenderId(), "", "", message.getMessage(),
										MessageType.SUCCESS));
								out.flush();
								System.out
										.println("Reply: " + new DomoMessage(message.getSenderURL(),
												message.getSenderId(), "", "", message.getMessage(),
												MessageType.SUCCESS).toString());
							} else if (message.getMessageType().equals("REMOVE"))
								manager.removeDevice(message);
							else if (message.getMessageType().equals("EXISTS")) {
								DomoDeviceId id = null;
								message = new DomoMessage(clientCommand);
								if ((id = manager
										.getDomoDeviceId(message.getMessage())) != null) {
									out.println(new DomoMessage(id.getUrl(), id.getId(), "", "",
											message.getMessage(), MessageType.SUCCESS).toString());
									out.flush();
									System.out.println(
											"Reply: " + new DomoMessage(id.getUrl(), id.getId(), "",
													"", message.getMessage(), MessageType.SUCCESS)
															.toString());
								} else {
									out.println(new DomoMessage("", "", "", "",
											message.getMessage(), MessageType.FAILURE).toString());
									out.flush();
									System.out.println("Reply: " + new DomoMessage("", "", "", "",
											message.getMessage(), MessageType.FAILURE).toString());
								}
							}
						} else {
							// Process it
							out.println("Message not supported: " + clientCommand);
							out.flush();
							System.out.println("Message not supported: " + clientCommand);
						}
					} catch (Exception e) {
						System.out.println("Error processing message: ");
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			System.out
					.println("Exception socket with client: " + server + ":" + port);
			e.printStackTrace();
		} finally {
			// Clean up
			try {
				in.close();
				out.close();
				myClientSocket.close();
				System.out.println("Closed socket with client: " + server + ":" + port);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
