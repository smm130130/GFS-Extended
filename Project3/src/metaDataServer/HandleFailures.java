package metaDataServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

import utilities.UsefulMethods;

public class HandleFailures implements Runnable {

	int serverNumber = 0;
	MetadataStorage storage = MetadataStorage.getMetadataStorageInstance();
	public HandleFailures(int serverNumber) {
		this.serverNumber = serverNumber;
	}
	
	@Override
	public void run() {
		storage.changeAfterFailure(serverNumber);
		//Now copy the files
		copyFilesAfterFailures(serverNumber);
	}

	private void copyFilesAfterFailures(int serverNumber) {
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		
		String serverName = ServerPort.getProperty("server"+serverNumber);
		String portString = ServerPort.getProperty("server"+serverNumber+"port");
		int port = Integer.parseInt(portString.trim());
		
		Socket client = null;
		
		try {
			client = new Socket(serverName, port);
			PrintWriter out1 = new PrintWriter(client.getOutputStream(), true);
			out1.println("failure:"+serverNumber);
			client.close();out1.close();			
		}
		catch (IOException e) {
			System.out.println("Server Unavailable");
		}
		
	}
	
}
