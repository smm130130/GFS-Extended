package metaDataServer;

import java.util.ArrayList;
import java.util.Properties;

import utilities.UsefulMethods;

public class HandleServers implements Runnable {
	
	MetadataHandler handler = new MetadataHandler();
	public static volatile ArrayList<Integer> unavailServers = new ArrayList<>();
	private static volatile HandleServers instance = null;
	
	public HandleServers() {
		
	}
	
	public static HandleServers getHandleServersInstance() {
		synchronized (UsefulMethods.class) {
			// Double check
			if (instance == null) {
				System.out.println("HandleServers : I am being created");
				instance = new HandleServers();
			}
		}
		return instance;
	}
	
	@Override
	public void run() {
		sleepSomeSeconds(25000);
		handleServers();
	}
	
	private void sleepSomeSeconds(int some) {
		try{
			Thread.sleep(some);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handleServers() {
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		int numOfServers = Integer.parseInt(ServerPort.getProperty("numofservers"));
		boolean status = true;
		while(true) {
			sleepSomeSeconds(2000);
			for(int i=1; i< numOfServers+1; i++) {
				if(checkUnavilServers(i)) {
					status = handler.checkForAvailabilityofServer(i);
					if(status) {
						continue;
					} else {
						System.out.println("We have a failure at server : " + i);
						unavailServers.add(i);
						Thread t = new Thread(new HandleFailures(i));
						t.start();
					}
				}
			}
		}
	}
	
	public boolean checkUnavilServers(int serverNumber) {
		for(int i=0; i< unavailServers.size(); i++) {
			int unServ = unavailServers.get(i);
			if(serverNumber == unServ) {
				return false;
			}
		}
		return true;
	}
}
