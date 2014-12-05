package metaDataServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Queue;

import utilities.UsefulMethods;

public class MetaDataServer {
	ServerSocket serverSock;
	int processNumber;
	Queue<Socket> requestQueue = new PriorityQueue<Socket>();
	
	public static void main(String[] args) {
		MetaDataServer mds = new MetaDataServer();
		mds.run();
	}

	public void run() {
		Thread th = new Thread(new HandleServers());
		th.start();
		
		Properties MetadataServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		String portString = MetadataServerPort.getProperty("metadataport");//Integer.parseInt(args[1]);
		int port = Integer.parseInt(portString);
		System.out.println("Metadata Server port is " + port);
		try{
			serverSock = new ServerSocket(port);
			while(true) {
				Socket client = serverSock.accept();
				requestQueue.add(client);
				try{
					if(!requestQueue.isEmpty()) {
						System.out.println("Requesting the queue..............................");
						Thread t = new Thread(new MetadataHandler(requestQueue.poll()));
						t.start();

						Thread.sleep(2000);		
					}			
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
