package metaDataServer;

import java.util.Properties;

import utilities.UsefulMethods;

public class HandleServers implements Runnable {
	MetadataHandler handler = new MetadataHandler();
	public HandleServers() {
		
	}
	
	@Override
	public void run() {
		try{
			Thread.sleep(5000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		handleServers();
	}

	private void handleServers() {
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		int numOfServers = Integer.parseInt(ServerPort.getProperty("numofservers"));
		boolean status = true;
		while(true) {
			for(int i=1; i< numOfServers+1; i++) {
				status = handler.checkForAvailabilityofServer(i);
				if(status) {
					continue;
				} else {
					Thread t = new Thread(new HandleFailures(i));
					t.start();
				}
			}
		}
	}	
}
