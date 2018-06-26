package domoNetWS.techManager.domoMLTCPManager;

import java.io.*;
import java.net.*;

class DomoMLTCPManagerTest {
    public static void main(String argv[]) throws Exception {
	BufferedReader is = new BufferedReader(
		new InputStreamReader(System.in));
	Socket clientSocket = new Socket("localhost", 7779);
	DataOutputStream outToServer = new DataOutputStream(
		clientSocket.getOutputStream());
	BufferedReader inFromServer = new BufferedReader(
			new InputStreamReader(clientSocket.getInputStream()));
	String outStream;
	/*
	while (!(outStream = is.readLine()).equals("exit")) {
	    System.out.println(outStream + '\n');
	    outToServer.writeBytes(outStream + '\n');
	    if(outStream.startsWith("<device") || 
	    		(outStream.startsWith("<message") && outStream.contains("EXISTS"))) {
				String inStream = inFromServer.readLine();
	      System.out.println("Received from server: " + inStream);
	    }
	}
	clientSocket.close();
    }
    */
	outStream = is.readLine();
	clientSocket.close();
    }
}