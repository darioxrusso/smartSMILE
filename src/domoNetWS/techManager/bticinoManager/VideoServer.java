package domoNetWS.techManager.bticinoManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import common.Debug;

/**
 * Description: Gestisce tramite un thread la ricezione delle immagini dal
 * webserver
 * 
 */
public class VideoServer extends Thread {

	Socket socket;
	DataOutputStream os;
	ServerSocket serverSocket;
	private int port;
	private boolean stop;

	/**
	 * Costruttore
	 * 
	 * @param ipAddress
	 *            Indirizzo ip del webserver dal quale catturare le immagini
	 */
	public VideoServer(int port) {
		this.port = port;
		this.stop = false;
		Debug.getInstance().writeln("CREATO");
	}

	/**
	 * Avvia il Thread per il refresh dell'immagine della videocamera
	 */
	public void run() {

		try {

			serverSocket = new ServerSocket(port);

		} catch (UnknownHostException e2) {
			System.out.println("UnknownHostException VideoServer nella creazione della socket su porta " + port);
		}

		catch (IOException e2) {
			System.out.println("eccezione2 VideoThread nella creazione della socket su porta " + port);

		}

		while (!stop) {

			try {
				socket = serverSocket.accept();
				Debug.getInstance().writeln("Connessione accettata");
			} catch (IOException ioe) {
				System.out.println("IOException VideoServer " + ioe);

			}

			try {
				os = new DataOutputStream(socket.getOutputStream());

				byte[] image;

				image = IntraVideoComunication.getByteImage();
				os.writeInt(image.length);
				os.write(image);

				os.close();
				socket.close();

			} catch (IOException ioe) {
				System.out.println("Eccezione VideoServer" + ioe);
			}

		}

	}

	public int getInterPort() {

		return port;
	}

	public void stopThread() {
		stop = true;
	}

	public void startThread() {
		stop = false;
		this.start();
	}

}
