package domoNetWS.techManager.bticinoManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class IntraVideoComunication extends Thread {

	private static byte[] byteImage;// contiene l'immagine jpeg

	byte[] bytes0 = new byte[100000];
	byte[] bytes1 = new byte[100000];

	Socket socket = null;
	DataInputStream data = null;
	PrintWriter outputStream = null;

	private int port;
	private String address;
	private boolean stop;

	public IntraVideoComunication(String address, int port) {
		this.port = port;
		this.address = address;
		this.stop = false;

	}

	public void run() {

		while (!stop) {

			try {
				// Creazione del socket di comunicazione con il Server Bticino
				socket = new Socket(address, port);
				outputStream = new PrintWriter(socket.getOutputStream(), true);
				data = new DataInputStream(socket.getInputStream());
			} catch (UnknownHostException uhe) {
				System.out.println(
						"UnknownHostException IntraVideoComunication nella creazione della socket su porta " + port);
			} catch (IOException ioe) {
				System.out.println("IOException IntraVideoComunication nella creazione della socket su porta " + port);
			}

			try {

				bytes0 = new byte[100000];
				bytes1 = new byte[100000];

				outputStream.write("GET /telecamera.jpg");
				outputStream.flush();

				// Intestazione
				data.read(bytes0);

				int length = 0;
				int temp = 0;

				while (true) {
					temp = data.read();
					if (temp == -1)
						break;
					bytes1[length] = (byte) temp;
					length++;
				}

				setByteImage(bytes1, length);

			} catch (IOException eio) {
				System.out.println("Errore nella lettura in IntraVideoComunication");
			}
		}

	}

	private synchronized void setByteImage(byte[] bytearray, int length) {
		byteImage = new byte[length];
		for (int z = 0; z < length; z++) {
			byteImage[z] = (byte) bytearray[z];
		}
	}

	public static synchronized byte[] getByteImage() {/*
														 * array =new byte
														 * [byteImage.length];
														 * System.out.println(
														 * "Dimenxione "
														 * +array.length);
														 * System.arraycopy(
														 * byteImage, 0, array,
														 * 0, byteImage.length);
														 */
		return byteImage;
	}

	public void stopThread() {
		stop = true;
	}

	public void startThread() {
		stop = false;
		this.start();
	}

}
