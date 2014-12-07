package utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;

public class UsefulMethods {
	
	private static volatile UsefulMethods instance = null;
	public static volatile boolean lastRequestCompleted = false;
	
	private UsefulMethods() {
		
	}

	public static UsefulMethods getUsefulMethodsInstance() {
		synchronized (UsefulMethods.class) {
			// Double check
			if (instance == null) {
				System.out.println("UsefulMethods : I am being created");
				instance = new UsefulMethods();
			}
		}
		return instance;
	}
	
	public Properties getPropertiesFile(String filename) {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("resource/"+filename+"");
			prop.load(input);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prop;
	}

	
	public int randomServer() {
		Random r = new Random();
		int Low = 1;
		int High = 5; // This should be 15
		int randomNum = r.nextInt((High - Low) + 1) + Low;
		return randomNum;
	}
	
	public String getTime() {
		final Calendar cal = Calendar.getInstance();
    	final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    	return sdf.format(cal.getTime());
	}
	
	public ArrayList<Integer> loadBalancing() {
		ArrayList<Integer> replicaServers = new ArrayList<>();
		TreeMap<Long, Integer> treeMap = new TreeMap<>();
		Properties prop = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		int serverNumber = 0;
		int noOfServers = Integer.parseInt(prop.getProperty("numofservers"));
		
		for(int i=1; i< noOfServers+1; i++) {
			long dir = 0L;
			serverNumber = i;
			File[] files = new File("/home/004/s/sm/smm130130/AOSproject3/FileSystem/server"+i).listFiles();
				for (File file : files) {
					 	dir = dir + file.length();		            
				 }
				 treeMap.put(dir, serverNumber);
		}
		for(Entry<Long, Integer> entry : treeMap.entrySet()) {
			  Integer value = entry.getValue();
			  replicaServers.add(value);
		}
		while(replicaServers.size() < 3) {
			int ranServ = UsefulMethods.getUsefulMethodsInstance().randomServer();
			if(replicaServers.size() == 2) {
				if(replicaServers.get(0) == ranServ || replicaServers.get(1) == ranServ) {
					continue;
				} else {
					replicaServers.add(ranServ);
				}
			} else if(replicaServers.size() == 1) {
				if(replicaServers.get(0) == ranServ) {
					continue;
				} else {
					replicaServers.add(ranServ);
				}
			}
		}
		return replicaServers;
	}
}
