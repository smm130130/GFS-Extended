package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

import utilities.UsefulMethods;

public class ClientUpdateFiles implements Runnable {
	
	String chunkName = null;
	int serverNumber = 0;
	String message = null;

	public ClientUpdateFiles(String chunkName, int serverNumber, String message) {
		this.chunkName = chunkName;
		this.serverNumber = serverNumber;
		this.message = message;
	}
	
	@Override
	public void run() {
		SetUpAppendNetworking(chunkName, serverNumber, message);
	}

	private void SetUpAppendNetworking(String chunkName, int serverNumber, String message) {
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		
		String serverName = ServerPort.getProperty("server"+serverNumber);
		String portString = ServerPort.getProperty("server"+serverNumber+"port");
		int port = Integer.parseInt(portString.trim());
		/*System.out.println("Connecting to "+serverName+".... with port ......"+port);*/
		
		Socket client = null;
		
		try {
			client = new Socket(serverName, port);
			PrintWriter out1 = new PrintWriter(client.getOutputStream(), true);
			out1.println("append:"+chunkName+":"+serverNumber+":"+message);
			client.close();out1.close();			
		}
		catch (IOException e) {
			System.out.println("Server Unavailable");
		}
	}
	
}
