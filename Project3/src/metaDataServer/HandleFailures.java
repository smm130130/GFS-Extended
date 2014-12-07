package metaDataServer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;

import utilities.UsefulMethods;

public class HandleFailures implements Runnable {

	int serverNumber = 0;
	MetadataStorage storage = MetadataStorage.getMetadataStorageInstance();
	public HandleFailures(int serverNumber) {
		this.serverNumber = serverNumber;
	}
	
	@Override
	public void run() {
		iterateThroughFileSystem();
	}
	
	public void iterateThroughFileSystem() {
		File[] files = new File("/home/004/s/sm/smm130130/AOSproject3/FileSystem/server"+serverNumber).listFiles();
		for (File file : files) {
			String chunkName = file.getName();
			String fileName = chunkName.split("-")[0];
			ArrayList<Integer> replicaServers = MetadataStorage.getMetadataStorageInstance().getFailureReplicas(serverNumber, fileName, chunkName);
			int copyToServerNumber = getCopyToServerNumber(replicaServers);
			MetadataStorage.getMetadataStorageInstance().changeAfterFailure(this.serverNumber, copyToServerNumber, fileName, chunkName);
			System.out.println("copying "+ chunkName + " from server " + serverNumber + " to server " + copyToServerNumber);
			copyFilesAfterFailures(this.serverNumber, copyToServerNumber, chunkName);
	    }
	}
	
	private int getCopyToServerNumber(ArrayList<Integer> replicaServers) {
		ArrayList<Integer> repServers = new ArrayList<>();
		TreeMap<Long, Integer> treeMap = new TreeMap<>();
		Properties prop = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		int serverNum = 0;
		int noOfServers = Integer.parseInt(prop.getProperty("numofservers"));
		
		for(int i=1; i< noOfServers+1; i++) {
			if(replicaServers.get(0) == i || replicaServers.get(1) == i || i == this.serverNumber) {
				continue;
			} else {
				long dir = 0L;
				serverNum = i;
				File[] files = new File("/home/004/s/sm/smm130130/AOSproject3/FileSystem/server"+i).listFiles();
					for (File file : files) {
						 	dir = dir + file.length();		            
					 }
					 treeMap.put(dir, serverNum);
			}
		}
		for(Entry<Long, Integer> entry : treeMap.entrySet()) {
			  Integer value = entry.getValue();
			  repServers.add(value);
		}
		return repServers.get(0);
	}
	
	private void copyFilesAfterFailures(int copyFrom, int copyTo, String chunkName) {
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		
		String serverName = ServerPort.getProperty("server"+copyTo);
		String portString = ServerPort.getProperty("server"+copyTo+"port");
		int port = Integer.parseInt(portString.trim());
		
		Socket client = null;
		
		try {
			client = new Socket(serverName, port);
			PrintWriter out1 = new PrintWriter(client.getOutputStream(), true);
			out1.println("failure"+":"+copyFrom+":"+copyTo+":"+chunkName);
			client.close();out1.close();			
		}
		catch (IOException e) {
			System.out.println("Server Unavailable");
		}
		
	}
	
}
