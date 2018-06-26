package domoNetWS.techManager.domoMLTCPManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

class DomoMLTCPManagerServerTest2 implements Runnable {

	Socket csocket;

	DomoMLTCPManagerServerTest2(Socket csocket) {
		this.csocket = csocket;
	}

	public static void main(String args[]) throws Exception {
		ServerSocket ssock = new ServerSocket(6788);
		System.out.println("Listening");
		while (true) {
			Socket sock = ssock.accept();
			System.out.println("Connected");
			new Thread(new DomoMLTCPManagerServerTest2(sock)).start();
		}
	}

	public void run() {
		try {
			BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(csocket.getInputStream()));
			String clientSentence = "";
			while (clientSentence != null) {
				clientSentence = inFromClient.readLine();
				System.out.println("Received: " + clientSentence);
			}
			inFromClient.close();
			csocket.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}