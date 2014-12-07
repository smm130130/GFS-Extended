package trail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class Sample1 {
	public static void main(String[] args) {
		Sample1 s = new Sample1();
		ArrayList<Integer> replicaServers = new ArrayList<Integer>();
		int serverNumber = s.loadBalancing(1);
		replicaServers.add(serverNumber);
		System.out.println(replicaServers);
	}
	
	public int loadBalancing(int serverNumber) {
		long dirSize = 1000000000000000000L;
		/*Properties prop = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		
		int noOfServers = Integer.parseInt(prop.getProperty("numofservers"));*/
		
		for(int i=1; i< 6; i++) {
			if(i != serverNumber) {
				long dir = 0L;
				File[] files = new File("C:/Users/Sunil/git/GFS-Extended/Project3/FileSystem/server"+i).listFiles();
					for (File file : files) {
						 	dir = dir + file.length();		            
					 }
					 if(dir < dirSize) {
						 serverNumber = i;
						 dirSize = dir;
					 }
				}
			}	
		return serverNumber;		 
	}
	
	public void hashMapRelated() {
		HashMap<Integer, Long> list =  new HashMap<>();
		list.put(1, System.currentTimeMillis());
		list.put(2, System.currentTimeMillis());
		list.put(3, System.currentTimeMillis());
		System.out.println(list);
		try{
			Thread.sleep(2000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		if(list.get(1) != null) {
			long diff = System.currentTimeMillis() - list.get(1);
			System.out.println("difference : "+ diff);
			list.put(1, System.currentTimeMillis());
		}
		System.out.println(list);
		
		String some = "Sunil:Munavalli:Shwetha";
		String[] chunk = some.split(":");
		if(chunk.length > 2) {
			System.out.println(chunk[2]);
		}
	}
}
