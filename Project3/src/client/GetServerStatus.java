package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

import utilities.UsefulMethods;

public class GetServerStatus {
	public boolean getServerStatus(int serverNumber) {
		boolean status = true;
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");	
		
		try {
			String serverName = ServerPort.getProperty("server"+serverNumber);
			String portString = ServerPort.getProperty("server"+serverNumber+"port");
			int port = Integer.parseInt(portString.trim());
			/*System.out.println("Connecting to "+serverName+".... with port ......"+port);*/
			
			Socket client = null;
			client = new Socket(serverName, port);
			PrintWriter out1 = new PrintWriter(client.getOutputStream(), true);
			status = true;
			client.close();out1.close();			
		}
		catch (IOException e) {
			status = false;
	}
		return status;
	}
}
