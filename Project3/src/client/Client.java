package client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

import utilities.UsefulMethods;

public class Client {
	static int readRetry = 0;
	static int appendRetry = 0;
	static int createRetry = 0;
	
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
	        String fileName = (args[0]).toString();		        
		
			BufferedReader reader = new BufferedReader(new FileReader("resource/"+fileName));
			Client client = new Client();
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					readRetry = 0; appendRetry = 0; createRetry = 0;
					String parts[] = line.split("\\|");
					if(parts[0].toUpperCase().equals(("r").toUpperCase())) {
						client.readFromFile(parts[1], parts[2], Integer.parseInt(parts[3]));
					}
					else if(parts[0].toUpperCase().equals(("w").toUpperCase())) {
						String filename = parts[1];
						client.createFile(filename, parts[2]);
					}
					else if(parts[0].toUpperCase().equals(("a").toUpperCase())) {
						String filename = parts[1];
						client.appendToFile(filename, parts[2]);
					}
				}
			} finally {
				reader.close();
			}
		} else {
			System.out.println("Provide an input file");
		}
	}

	private void createFile(String filename, String message) throws IOException {
		// consult m-server and create a file
		boolean status = true;
		System.out.println("create request filename: "+filename);
		String returnedString = (SetMetadataServer("create",filename));
		int masterServerNumber = Integer.parseInt(returnedString.split(":")[1]);
		int secondServerNumber = Integer.parseInt(returnedString.split(":")[2]);
		int thirdServerNumber = Integer.parseInt(returnedString.split(":")[3]);
		for(int i=1; i<4; i++) {
			int servNum = Integer.parseInt(returnedString.split(":")[i]);
			status = new GetServerStatus().getServerStatus(servNum);
			if(!status) break;
		}
		//SetUpNetworking(masterServerNumber, secondServerNumber, thirdServerNumber, filename, message);
		if(status) {
			Thread master = new Thread(new ClientCreateFiles(masterServerNumber, filename, message));
			Thread replica1 = new Thread(new ClientCreateFiles(secondServerNumber, filename, message));
			Thread replica2 = new Thread(new ClientCreateFiles(thirdServerNumber, filename, message));
			master.start();
			replica1.start();
			replica2.start();
		} else {
			if(createRetry < 2) {
				createRetry++;
				System.out.println("create aborted trying again");
				try{
					Thread.sleep(5000);
				} catch(InterruptedException e){
					e.printStackTrace();
				}
				createFile(filename, message);
			}
		}
	}

	private void appendToFile(String filename, String message) {
		System.out.println("append request filename : "+filename);
		boolean status = true;
		String lastChunkInfo = null;
		filename = filename.split("\\.")[0];
		long msgSize = message.length();
		try {
			lastChunkInfo = SetMetadataServer("append", filename+":"+msgSize);
			String[] lastInfos = lastChunkInfo.split(":");
			String chunkName = lastInfos[0];
			int ServerNumber = Integer.parseInt(lastInfos[1]); int replicaServerNumber = 0;
			int firstServerNumber = Integer.parseInt(lastInfos[2]); int firstReplicaServerNumber = 0;
			int secondServerNumber = Integer.parseInt(lastInfos[3]); int secondReplicaServerNumber = 0;
			for(int i=1; i<4; i++) {
				int servNum = Integer.parseInt(lastChunkInfo.split(":")[i]);
				status = new GetServerStatus().getServerStatus(servNum);
				if(!status) break;
			}
			if(status) {
				if(lastInfos.length > 5) {
					replicaServerNumber = Integer.parseInt(lastInfos[5]);
					firstReplicaServerNumber = Integer.parseInt(lastInfos[6]);
					secondReplicaServerNumber = Integer.parseInt(lastInfos[7]);
				}
				Thread master = new Thread(new ClientUpdateFiles(chunkName, ServerNumber, message, replicaServerNumber));
				Thread replica1 = new Thread(new ClientUpdateFiles(chunkName, firstServerNumber, message, firstReplicaServerNumber));
				Thread replica2 = new Thread(new ClientUpdateFiles(chunkName, secondServerNumber, message, secondReplicaServerNumber));
				//SetUpAppendNetworking(chunkName, ServerNumber, message);
				master.start();
				replica1.start();
				replica2.start();
			} else {
				if(appendRetry < 2) {
					appendRetry++;
					System.out.println("append aborted trying again");
					try{
						Thread.sleep(5000);
					} catch(InterruptedException e){
						e.printStackTrace();
					}
					appendToFile(filename, message);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readFromFile(String filename, String offset, int bytesToRead) throws IOException {
		//consult meta data server and read
		System.out.println("read request filename : "+filename+" offset : "+offset+ " bytesToRead : "+ bytesToRead);
		String[] chunks = filename.split("\\.");
		int chunkNumber = (Integer.parseInt(offset)/8192)+1;
		String chunkName = chunks[0]+"-"+chunkNumber;
		int seekPosition = Integer.parseInt(offset) % 8192;
		String returnedString = (SetMetadataServer("read", chunkName));
		String[] parts = returnedString.split(":");
		for(int i=1; i<4 ;i++) {
			int serverNumber = Integer.parseInt(parts[i]);
			boolean success = SetUpReadNetworking(serverNumber, chunkName, seekPosition, bytesToRead);
			if(success) {
				System.out.println("Reading from server "+serverNumber);
				break;
			} else {
				System.out.println("Server"+serverNumber+" down");
				continue;
			}
		}
	}
	
	private boolean SetUpReadNetworking(int serverNumber, String filename, int seekPosition, int bytesToRead) {
		boolean sucess = true;
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		
		String serverName = ServerPort.getProperty("server"+serverNumber);
		String portString = ServerPort.getProperty("server"+serverNumber+"port");
		int port = Integer.parseInt(portString.trim());
		/*System.out.println("Connecting to "+serverName+".... with port ......"+port);*/
		
		Socket client = null;
		
		try {
			client = new Socket(serverName, port);
			PrintWriter out1 = new PrintWriter(client.getOutputStream(), true);
			out1.println("read:"+filename+":"+serverNumber+":"+seekPosition+":"+bytesToRead);
			client.close();out1.close();			
		}
		catch (IOException e) {
			System.out.println("Server Unavailable");
			sucess = false;
		}
		return sucess;
	}
	
	private String SetMetadataServer(String action, String filename) throws IOException {
		
		String serverNumber = null;
		Properties ServerPort = UsefulMethods.getUsefulMethodsInstance().getPropertiesFile("spec.properties");
		
		String serverName = ServerPort.getProperty("metadataserver");
		String portString = ServerPort.getProperty("metadataport");//Integer.parseInt(args[1]);
		int port = Integer.parseInt(portString.trim());
		//System.out.println("Connecting to "+serverName+".... with port ......"+port);
		
		Socket client = null;
		
		try {
			client = new Socket(serverName, port);
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in =new BufferedReader(new InputStreamReader(client.getInputStream()));
			if(action.equalsIgnoreCase("create")) {
				out.println("create"+":"+filename);
				serverNumber = readResponse(client, in);
				return serverNumber;
			}
			else if(action.equalsIgnoreCase("append")) {
				String name = filename.split(":")[0];
				String msgSize = filename.split(":")[1];
				out.println(action+":"+name+":"+msgSize);
				serverNumber = readAppendResponse(client, in);
				return serverNumber;
			}
			else if(action.equalsIgnoreCase("read")) {
				out.println(action+":"+filename);
				serverNumber = readResponse(client, in);
				return serverNumber;
			}
			client.close();out.close();in.close();
		}
		catch (IOException e) {
			System.out.println("MetaDataServer Unavailable");
		} 
		finally {
			//client.close();
		}
		return null;
	}
	
	private String readAppendResponse(Socket client, BufferedReader in) throws IOException {
		String userInput;

		while ((userInput = in.readLine()) != null) {
			System.out.println("Response from Append server:"+userInput+ " Time of Response : "+UsefulMethods.getUsefulMethodsInstance().getTime());
			return userInput;
		}
		return null;
	}

	public String readResponse(Socket client, BufferedReader in) throws IOException {
		String userInput;
		/*BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				client.getInputStream()));*/

		while ((userInput = in.readLine()) != null) {
			System.out.println("Response from server:"+userInput+ " Time of Response : "+UsefulMethods.getUsefulMethodsInstance().getTime());
			//return Integer.parseInt(parts[1]);
			return userInput;
		}
		return null;
	}
}
