package domoNetWS.techManager.bticinoManager;

import java.net.*;
import java.io.*;
import common.Debug;

public class MonitorSession extends Thread {

	BTICINOManager bTicinoManager;

	String host;
	int port;
	Socket socket;
	BufferedReader input;
	PrintWriter output;

	final String MSG_OPEN_OK = "*#*1##";
	final String MSG_OPEN_NOT_OK = "*#*0##";
	final String MSG_OPEN_MONITOR = "*99*1##";

	public MonitorSession(String host, int port, BTICINOManager bTicinoManager) {
		this.host = host;
		this.port = port;

		this.bTicinoManager = bTicinoManager;
	}

	public void run() {

		connect(this.host, this.port);
		while (true) {
			// writeToDebug("IN RUN "+readTCP());
			bTicinoManager.monitor(readTCP());

		}

	}

	/**
	 * Tentativo di apertura socket comandi verso il webserver
	 * 
	 * @param ip
	 *            Ip del webserver al quale connettersi
	 * @param port
	 *            Porta sulla quale aprire la connessione
	 * @return true Se la connessione va a buon fine, false altrimenti
	 */
	public boolean connect(String ip, int port) {

		try {
			socket = new Socket(ip, port);
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);

			if (socket != null) {
				if (readTCP().equals(MSG_OPEN_OK)) {
					output.write(MSG_OPEN_MONITOR);
					output.flush();
				} else {
					close();
					return false;
				}
				if (readTCP().equals(MSG_OPEN_OK)) {
					// connect=true;
					return true;
				} else {
					close();
					return false;
				}

			} else {
				close();
				return false;
			}
		} catch (IOException e) {
			close();
			return false;
		}
	}

	/**
	 * Chiude la socket comandi
	 *
	 */
	public void close() {
		if (socket != null) {
			try {
				socket.close();
				socket = null;
				// connect = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Debug.getInstance().writeln("Errore Socket: <GestioneSocketMonitor>");
				e.printStackTrace();
			}
		}
	}

	public String readTCP() {

		char risposta[] = new char[1024];
		int indice = 0;
		char c = ' ';
		int state = 0;
		try {
			while (true) {
				int ch = input.read();
				if (ch == -1) {
					Debug.getInstance().writeln("Problemi nella connessione");
					break;
				} else {
					c = (char) ch;
					if (c == '#' && state == 0) {
						risposta[indice] = c;
						state = 1;
						indice++;
					} else if (c == '#' && state == 1) {
						risposta[indice] = c;

						break;
					} else if (c != '#') {
						risposta[indice] = c;
						state = 0;
						indice++;
					} else
						System.out.println("----------ERRORE-------------");
				}

			}
		} catch (IOException e) {
			System.out.println("Error in ReadTCP");
		}
		String responseString = new String(risposta, 0, indice + 1);
		return responseString;

	}

	/**
	 * Write a message on the debug window.
	 * 
	 * @param message
	 *            The message to display.
	 * @retCarr true if new line is requested, false otherwise.
	 */
	private void writeToDebug(String message, boolean retCarr) {
		if (retCarr)
			Debug.getInstance().writeln(message);
		else
			Debug.getInstance().writeln(message);
	}

}
