package metaDataServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import utilities.UsefulMethods;

import java.util.concurrent.locks.ReentrantLock;

public class MetadataHandler implements Runnable {
	
	private final ReentrantLock lock = new ReentrantLock();
	UsefulMethods usefulmethods = UsefulMethods.getUsefulMethodsInstance();
	MetadataStorage storage = MetadataStorage.getMetadataStorageInstance();
	PrintWriter writer;
	BufferedReader reader;
	Socket sock;
	public static boolean heartbeatReceived = false;
	public static volatile HashMap<Integer, Long> lastMsgSentTime = new HashMap<>(); 
	
	public MetadataHandler(Socket client) {
		sock = client;
		try {
			InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
			reader = new BufferedReader(isReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public MetadataHandler() {
		
	}

	public void run() {
		String msg;
		try {
			while((msg = reader.readLine()) != null) {
				String[] parts = msg.split(":");
				String action = parts[0];
				
				if(action.equalsIgnoreCase("create")) {
					System.out.println("metadataHandler create operation");
					String filename = parts[1];
					int serverNumber = usefulmethods.randomServer();
					ArrayList<Integer> serverNumbers = generateReplicaServerNumbers(serverNumber);
					if(storage.fileExists(filename)) {
						System.out.println("file exits appending the message at the end");
					}
					else {
						String[] chunks = filename.split("\\.");
						filename = chunks[0];
						storage.buildArraylist(filename);
						storage.createHashMap(filename, serverNumbers);
					}
					sendWelcomeMessage(sock, serverNumbers);
				}
				else if(action.equalsIgnoreCase("append")) {
					System.out.println("metadataHandler append operation");
					
					String lastChunkInfo = storage.getLastChunkInfo(parts[1]);
					long fileSize = Long.parseLong(lastChunkInfo.split(":")[4]);
					long msgSize = Long.parseLong(parts[2]);
					if(fileSize+msgSize > 8192) {
						String chunkName = lastChunkInfo.split(":")[0];
						int fileBit = Integer.parseInt(chunkName.split("-")[1]);
						ArrayList<Integer> servNums = new ArrayList<>();
						servNums.add(Integer.parseInt(lastChunkInfo.split(":")[1]));
						servNums.add(Integer.parseInt(lastChunkInfo.split(":")[2]));
						servNums.add(Integer.parseInt(lastChunkInfo.split(":")[3]));
						storage.createAppendHashMap(parts[1], servNums, fileBit+1, msgSize);
					}
					for(int i=1; i<4 ;i++) {
						int serverNumber = Integer.parseInt(lastChunkInfo.split(":")[i]);
						if(heartbeatReceived && lastMsgSentTime.get(serverNumber) != null) {
							System.out.println("failure detection in progrees....");
							if(checkForAvailabilityofServer(serverNumber)) {
								continue;
							} else{
								lastChunkInfo = composeSendErrorlastChunkInfo(i, lastChunkInfo);
							}
						}
					}
					sendAppendWelcomeMessage(sock, lastChunkInfo);			
				}
				else if(action.equalsIgnoreCase("read")) {
					int serverNumber = 0;
					System.out.println("metadataHandler read operation" + heartbeatReceived);
					String fileName = parts[1].split("-")[0];
					String chunkName = parts[1];
					String serverNumberString = storage.readHashMap(fileName, chunkName);
					
					for(int i=0; i<3 ;i++) {
						serverNumber = Integer.parseInt(serverNumberString.split(":")[i]);
						if(heartbeatReceived && lastMsgSentTime.get(serverNumber) != null) {
							System.out.println("failure detection in progrees....");
							if(checkForAvailabilityofServer(serverNumber)) {
								break;
							} else{
								serverNumber = -1;
								System.out.println("Server"+serverNumber+" down");
								continue;
							}
						}
					}
					sendReadWelcomeMessage(sock, serverNumber);
				}
				else if(action.equalsIgnoreCase("heartbeat")) {
					System.out.println("metadataHandler heartBeat operation"+ UsefulMethods.getUsefulMethodsInstance().getTime()+ " Server Number : "+ parts[1]);
					String serverNumber = parts[1];					
					String fileLength = parts[3];
					int byteSize = Integer.parseInt(fileLength);
					String lastModified = parts[4];
					
					String chunkName = parts[2];
					String[] names = chunkName.split("-");
					String fileName = names[0];
					
					if(storage.fileExists(fileName)) {
						storage.updateHashMap(Integer.parseInt(serverNumber), fileName, chunkName, byteSize, lastModified);
					}

					heartbeatReceived = true;
					updateLastMsgSentTime(Integer.parseInt(serverNumber));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<Integer> generateReplicaServerNumbers(int serverNumber) {
		ArrayList<Integer> serverNumbers = new ArrayList<Integer>();
		serverNumbers.add(serverNumber);
		int secondServerNumber = serverNumber;
		while(secondServerNumber == serverNumber) {
			secondServerNumber = usefulmethods.randomServer();
		}
		serverNumbers.add(secondServerNumber);
		int thirdServerNumber = secondServerNumber;
		while(thirdServerNumber == secondServerNumber || thirdServerNumber == serverNumber) {
			thirdServerNumber = usefulmethods.randomServer();
		}
		serverNumbers.add(thirdServerNumber);
		return serverNumbers;
	}

	private void updateLastMsgSentTime(int serverNumber) {
		lock.lock();
		try {
			lastMsgSentTime.put(serverNumber, System.currentTimeMillis());
		} finally {
			lock.unlock();
		}
		//System.out.println("lastMsgSentTime: "+lastMsgSentTime);
	}
	
	public boolean checkForAvailabilityofServer(int serverNumber) {
		//System.out.println("lastMsgSentTime : " + lastMsgSentTime);
		Long presentTime = System.currentTimeMillis();
		Long serverTime = 0L;
		lock.lock();
		try{
			serverTime = lastMsgSentTime.get(serverNumber);
		}finally {
			lock.unlock();
		}
		Long difference = presentTime - serverTime;
		if(difference > 15000) {
			return false;
		}
		return true;
	}
	
	private void sendWelcomeMessage(Socket client, ArrayList<Integer> serverNumbers) throws IOException {
        try {
        	writer = new PrintWriter(client.getOutputStream(), true);
            writer.println("serverNumber:"+serverNumbers.get(0)+":"+serverNumbers.get(1)+":"+serverNumbers.get(2));
            writer.flush();
        } finally {
            //writer.close();
        }
    }
	
	private void sendReadWelcomeMessage(Socket client, int serverNumber) throws IOException {
		try {
        	writer = new PrintWriter(client.getOutputStream(), true);
            writer.println("serverNumber:"+serverNumber);
            writer.flush();
        } finally {
            //writer.close();
        }
	}
	
	private void sendAppendWelcomeMessage(Socket client, String lastChunkInfo) throws IOException {
		try {
			writer = new PrintWriter(client.getOutputStream(), true);
            writer.println(lastChunkInfo);
            writer.flush();
		} finally {
            //writer.close();
        }
	}

	private String composeSendErrorlastChunkInfo(int i, String lastChunkInfo) {
		String sendErrorlastChunkInfo = null;
		String chunkName = lastChunkInfo.split(":")[0];
		String master = lastChunkInfo.split(":")[1];
		String first = lastChunkInfo.split(":")[2];
		String second = lastChunkInfo.split(":")[3];
		if(i == 1) {
			sendErrorlastChunkInfo = chunkName+":"+-1+":"+first+":"+second;
		} 
		else if(i == 2) {
			sendErrorlastChunkInfo = chunkName+":"+master+":"+-1+":"+second;
		}
		else if(i == 3) {
			sendErrorlastChunkInfo = chunkName+":"+master+":"+first+":"+-1;
		}
		
		return sendErrorlastChunkInfo;
	}
	
	public void closeEverything() {
		try{
			sock.close();
			writer.close();
			reader.close();
		} catch(Exception w) {
			w.printStackTrace();
		}
	}
}
