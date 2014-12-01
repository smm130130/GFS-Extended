package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

import utilities.UsefulMethods;

public class ClientCreateFiles implements Runnable {

	private int createRetry = 0;
	private int serverNumber = 0;
	private String filename = null;
	private String message = null;
	
	public ClientCreateFiles(int serverNumber, String filename, String message) {
		createRetry = 0;
		this.serverNumber = serverNumber;
		this.filename = filename;
		this.message = message;
	}
	
	@Override
	public void run() {
		SetUpNetworking(serverNumber, filename, message);
	}
	
	private void SetUpNetworking(int serverNumber, String filename, String message) {
		
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");	
		
		try {
			String serverName = ServerPort.getProperty("server"+serverNumber);
			String portString = ServerPort.getProperty("server"+serverNumber+"port");
			int port = Integer.parseInt(portString.trim());
			/*System.out.println("Connecting to "+serverName+".... with port ......"+port);*/
			
			Socket client = null;
			client = new Socket(serverName, port);
			PrintWriter out1 = new PrintWriter(client.getOutputStream(), true);
			out1.println("write:"+filename+":"+serverNumber+":"+message);
			client.close();out1.close();			
		}
		catch (IOException e) {
			try{
				Thread.sleep(2000);
				if(createRetry < 2) {
					System.out.println("Server was not up so trying "+createRetry+" again to create a file");
					createRetry++;
					SetUpNetworking(serverNumber, filename, message);
				}
			} catch(InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}

}
