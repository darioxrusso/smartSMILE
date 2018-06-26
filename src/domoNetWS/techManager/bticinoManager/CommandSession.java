package domoNetWS.techManager.bticinoManager;

import java.net.*;
import java.io.*;

import common.Debug;

public class CommandSession {
	String host;
	int port;
	Socket socket;
	BufferedReader input;
	PrintWriter output;
	// boolean connect;

	final String MSG_OPEN_OK = "*#*1##";
	final String MSG_OPEN_NOT_OK = "*#*0##";
	final String MSG_OPEN_COMMAND = "*99*0##";
	final String MSG_OPEN_SUPER_COMMAND = "*99*9##";

	public CommandSession(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public boolean test() throws IOException {

		socket = new Socket(this.host, this.port);
		input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		if (socket != null) {
			if (readTCP().equals(MSG_OPEN_OK)) {

				input.close();
				socket.close();
				return true;
			} else {

				input.close();
				socket.close();
				return false;
			}
		} else {
			Debug.getInstance().writeln(" Failed Biticino connection test");
			input.close();
			socket.close();
			return false;
		}

	}

	public boolean executeCommand(String command) throws IOException {
		if (connect(this.host, this.port)) {
			output.write(command);
			output.flush();
			String answer = readTCP();
			close();
			if (answer.equals(MSG_OPEN_OK))
				return true;
			else if (answer.equals(MSG_OPEN_NOT_OK))
				return false;
			// Se la risposta e' diversa da un ACK o un NACK
			// e il comando inviato e' una richista di stato, restituisce true
			else if (command.startsWith("*#"))
				return true;
			else
				// Se il comando inviato non e' una richiesta di stato
				return false;
		} else
			return false;

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
					output.write(MSG_OPEN_COMMAND); // super comandi
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

		/*
		 * while(true){
		 * 
		 * String responseLine = readTCP(input);
		 * 
		 * 
		 * if(responseLine != null){ if (stato == 0){ //ho mandato la richiesta
		 * di connessione
		 * 
		 * if (responseLine.equals(MSG_OPEN_OK)) {
		 * output.write(MSG_OPEN_SUPER_COMMAND); //super comandi output.flush();
		 * stato = 1; }else{ //se non mi connetto chiudo la socket this.close();
		 * break; } }else if (stato == 1){ //ho mandato il tipo di servizio
		 * richiesto
		 * 
		 * 
		 * if(responseLine.equals(MSG_OPEN_OK)){ stato = 3; break; } else{ //se
		 * non mi connetto chiudo la socket this.close(); break; } } } }else{
		 * 
		 * this.close(); break;//ramo else di if(responseLine != null) }
		 * }//chiude while(true) }else{
		 * 
		 * }
		 * 
		 * if (stato == 3) return true; else return false; }//chiude connect()
		 * 
		 */
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
				System.out.println("Errore Socket: <GestioneSocketComandi>");
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
					System.out.println("Problemi nella connessione");
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
			System.out.println(message);
		else
			System.out.print(message);
	}

	/**
	 * Write a message on the debug window.
	 * 
	 * @param message
	 *            The message to display followed by new line.
	 */
	private void writeToDebug(String message) {
		writeToDebug(message, true);
	}

}
