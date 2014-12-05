package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import utilities.UsefulMethods;

public class HandleFileReadWrite {

	public void createAndWriteToFile(String filename,String serverNumber, String message) {
		String[] chunk = filename.split("\\.");
		String chunkName = chunk[0]+"-1";
		try {
			int serverNum = Integer.parseInt(serverNumber);

			FileWriter fileWritter = new FileWriter("/home/004/s/sm/smm130130/AOSproject2/FileSystem/server"+serverNum+"/"+chunkName, true);
			BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
			bufferWritter.write(message);
			bufferWritter.flush();
			bufferWritter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readFromFile(String chunkName, String ServerNumber, int seekposition, int bytesToRead) {
		try {
			System.out.println(new String(readFromFile("/home/004/s/sm/smm130130/AOSproject2/FileSystem/server"+ServerNumber+"/"+chunkName, seekposition, bytesToRead)));
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private static byte[] readFromFile(String filePath, int position, int size)
			throws IOException {

		RandomAccessFile file = new RandomAccessFile(filePath, "r");
		file.seek(position);
		byte[] bytes = new byte[size];
		file.read(bytes);
		file.close();
		return bytes;
	}

	public void appendToChunk(String chunkName, int serverNumber, String message) {
		
		String[] chunks = chunkName.split("-");
		String fileName = chunks[0];
		int chunk = Integer.parseInt(chunks[1]);
		
		long msgSize = message.length();
		String storedFile = "/home/004/s/sm/smm130130/AOSproject2/FileSystem/server"+serverNumber+"/"+chunkName;
		
		File file = new File(storedFile);
		long fileLength = file.length();
		if(fileLength+msgSize > 8192) {
			RandomAccessFile file1 = null;
			try {
				file1 = new RandomAccessFile(storedFile, "rw");
				file1.setLength(8192);
				char ch = 0;
				
				while (file1.length() < 8192) {
					file1.writeChar(ch);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			chunk = (chunk+1);
			chunkName = fileName+"-"+chunk;
			storedFile = "/home/004/s/sm/smm130130/AOSproject2/FileSystem/server"+serverNumber+"/"+chunkName;
		}
		
		try {
			FileWriter fileWritter = new FileWriter(storedFile, true);
			BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
						
			bufferWritter.write(message);
			bufferWritter.flush();
			bufferWritter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void createAndWriteAfterFailure(String serverNumber) {
		File[] files = new File("/home/004/s/sm/smm130130/AOSproject2/FileSystem/server"+serverNumber).listFiles();
		int servNum = Integer.parseInt(serverNumber);
		showFiles(files, servNum);
	}
	
	private void showFiles(File[] files, int serverNumber) {
	    for (File file : files) {
	        if (file.isDirectory()) {
	            System.out.println("Directory: " + file.getName());
	            showFiles(file.listFiles(), serverNumber); // Calls same method again.
	        } else {
	        	String fileName[] = file.getName().split("\\.");
	            System.out.println("File: " + file.getName() + "File Size is : " + fileName[0]);
	            int copyToServer = UsefulMethods.getUsefulMethodsInstance().randomServer();
	            copyFiles(serverNumber, copyToServer, file.getName());
	        } 
	    }
	}
	
	private void copyFiles(int copyFromServer, int copyToServer, String filename) {
		InputStream inStream = null;
		OutputStream outStream = null;
	 
	    	try{
	 
	    	    File afile =new File("/home/004/s/sm/smm130130/AOSproject2/FileSystem/server"+copyFromServer+"/"+filename);
	    	    File bfile =new File("/home/004/s/sm/smm130130/AOSproject2/FileSystem/server"+copyToServer+"/"+filename);
	 
	    	    inStream = new FileInputStream(afile);
	    	    outStream = new FileOutputStream(bfile);
	 
	    	    byte[] buffer = new byte[1024];
	 
	    	    int length;
	    	    //copy the file content in bytes 
	    	    while ((length = inStream.read(buffer)) > 0){
	 
	    	    	outStream.write(buffer, 0, length);
	 
	    	    }
	 
	    	    inStream.close();
	    	    outStream.close();
	 
	    	    System.out.println("File is copied successful!");
	 
	    	}catch(IOException e){
	    		e.printStackTrace();
	    	}
	}
}
