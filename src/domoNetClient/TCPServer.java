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

import java.net.*;

import org.xml.sax.SAXException;

import java.io.*;
import common.*;

public class TCPServer extends Thread {

	Socket socket;
	ServerSocket serverSocket;
	BufferedReader is;

	AppProperties prefs;

	/**
	 * Configuration file where the client takes parameters like TCP server port
	 * to listen messages from the DomoNet framework.
	 */
	private static String CONFIG_FILE = "src/domoNetClient/domoNetClient.preferences";

	public void run() {
		try {
			prefs = AppPropertiesCollector.getInstance().getAppProperties(CONFIG_FILE);
			serverSocket = new ServerSocket(
					new Integer(prefs.getProperty("updateSocketPort", "7777")));
			// Listener server info
			InetAddress indirizzo = serverSocket.getInetAddress();
			String server = indirizzo.getHostAddress();
			int port = serverSocket.getLocalPort();
			Debug.getInstance().writeln("Initializing update socket on " + server + ":" + port);
			// Hearing for clients
			while (true) {
				socket = serverSocket.accept();
				// Client information
				InetAddress address = socket.getInetAddress();
				String client = address.getHostName();
				int porta = socket.getPort();
				Debug.getInstance().writeln("Opened update socket with: " + client + ":" + porta);

				// Byte stream used for the socket communication
				is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String userInput = "";
				try {
					while (userInput != null) {
						userInput = is.readLine();
						Debug.getInstance().writeln("Readed from update socket: " + userInput);
					}
				} catch (SocketException e) {
					Debug.getInstance().writeln(e.getMessage());
					terminate();
				}
			}
		} catch (IOException | SAXException e) {
			Debug.getInstance().writeln(e.getMessage());
		}
	}

	public void terminate() {
		try {
			is.close();
			socket.close();
			serverSocket.close();
			Thread.currentThread().interrupt();
			Debug.getInstance().writeln("Closed update socket");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
