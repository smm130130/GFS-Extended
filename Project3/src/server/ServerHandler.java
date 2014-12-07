package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerHandler implements Runnable {
	
	Socket sock;
	BufferedReader reader;
	HandleFileReadWrite hfrw = new HandleFileReadWrite();
	
	public ServerHandler(Socket client) throws IOException {
		sock = client;
		InputStreamReader in = new InputStreamReader(sock.getInputStream());
		reader = new BufferedReader(in);
	}

	@Override
	public void run() {
		String msg = null;
		try {
			while((msg = reader.readLine()) != null) {
				String parts[] = msg.split(":");
				System.out.println("Received "+parts[0]+" request on the server side");
				if(parts[0].equalsIgnoreCase("write")) {
					hfrw.createAndWriteToFile(parts[1], parts[2], parts[3]);
				}
				else if(parts[0].equalsIgnoreCase("read")) {
					hfrw.readFromFile(parts[1], parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
				}
				else if(parts[0].equalsIgnoreCase("append")) {
					int replicaServerNumber = Integer.parseInt(parts[4]);
					hfrw.appendToChunk(parts[1], Integer.parseInt(parts[2]), parts[3], replicaServerNumber);
				}
				else if(parts[0].equalsIgnoreCase("failure")) {
					System.out.println("The failure got is: "+ msg);
					hfrw.copyFiles(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parts[3]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
