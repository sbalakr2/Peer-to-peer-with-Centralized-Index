package mypack;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;

public class serverSocket implements Runnable{
	private static final String version = "P2P-CI/1.0";
	public static List<Object> list = new ArrayList<Object>();
	public static List<Object> rfcLists = Collections.synchronizedList(list);
	public static List<Object> peerLists = Collections.synchronizedList(new ArrayList<Object>());
	public ServerSocket sock;
	
	/*
	 * Constructor to create a socket
	 */
	public serverSocket(int port) throws IOException {
		sock = new ServerSocket(port);
      	System.out.println("Centralized Server running at "+ InetAddress.getLocalHost().getHostAddress()+ " on port " + sock.getLocalPort());
		System.out.println("OS: " + System.getProperty("os.name"));
		System.out.println("Version: " + version);
		new Thread(this).start();
	}

	public static void main(String args[]) throws IOException {
		int port = 0;
		
		if(args.length == 1){
			port = Integer.parseInt(args[0]);
			new serverSocket(port);
		} else{
			System.out.println("Run as java mypack.serverSocket #port");
		}
	}
	
	@Override
	public void run() throws NullPointerException{
		
		Socket s = null;
		ObjectInputStream  ois = null; 
	    ObjectOutputStream oos = null;
	    String hostname = null;
	    int clientPort = 0;
		try {
		    s = sock.accept();
	        new Thread(this).start();
	        clientPort = s.getPort();
			System.out.println("Connection Established with client @ port "+ clientPort);
			ois = new ObjectInputStream (s.getInputStream());
		    oos = new ObjectOutputStream(s.getOutputStream());
		    hostname = (ois.readObject()).toString();
			peerList peerObj = new peerList(hostname, Integer.toString(clientPort));
			peerLists.add(peerObj);
			System.out.println("Peer Added successfully");
		    
		} catch(Exception e) {
			System.out.println("Connection failure at start " + e); 
	        if (s.isConnected())
	        {
	        	removePeer(clientPort, false);
	        	try {s.close();}catch(IOException ioe){} 
            }
	        return;
		}
		 
	   try { 
		   while (true)
	       {  
			   	String request = (String) ois.readObject();
			 	System.out.println(request);
			 	String[] result = request.trim().split("\\s");
			 	executeRequest(result[0], ois, oos);
	        }       
	  	} catch (Exception e) {
	  		removePeer(clientPort, true);
	  		System.out.println("Connection with client " + hostname +" @ " + clientPort + " closed");
	  	}
	}

	/*
	 * Method to remove RFC and Peers
	 * @param: clientPort, port of the client
	 * @param: flag, removes RFC object if true
	 */
	private void removePeer(int clientPort, boolean flag) {
		if(flag){
			rfcList listOne = null;
			int index = 0;
			
			while(index < rfcLists.size()) {
			    listOne = (rfcList) rfcLists.get(index);
			    if(clientPort == listOne.cport) {
			    	rfcLists.remove(index);
			        index = 0;
			        continue;
			    }
			    index += 1;
			}
		}
		try {
			ListIterator<Object> piterator = peerLists.listIterator();
			peerList peerOne = null;
			while((piterator.hasNext()))                                    
			{
				peerOne = (peerList) piterator.next();
				if(Integer.toString(clientPort).equals(peerOne.port)){
					peerLists.remove(peerOne);
				}
			}
		} catch (ConcurrentModificationException e) {}
	}
	
	/*
	 * Method to process client request
	 */
	 private void executeRequest(String str, ObjectInputStream ois, ObjectOutputStream oos) throws ClassNotFoundException, IOException {
			switch(str.trim()){
				case "ADD":{
					addNewRFC(ois, oos);	
					break;
				}
				case "LIST":{
					listAllRFCs(ois, oos);
					break;
				}
				case "LOOKUP":{
					lookUpAnRFC(ois, oos);
					break;
				}
				case "default":{
					break;
				}
			}
		}

	 /*
	  * Method to lookup an RFC
	  */
		private void lookUpAnRFC(ObjectInputStream ois, ObjectOutputStream oos) {
			try {
				String rfcName = (String) ois.readObject();
				String rfcTitle = (String) ois.readObject();
				ListIterator<Object> iterator = rfcLists.listIterator();
				rfcList listOne = null;
				boolean isExist = false;
				while((iterator.hasNext()))                                    
			    {
			    	listOne = (rfcList) iterator.next();
			    	if(rfcName.equals(listOne.rfc)){
			    		isExist = true;
			    	}
			    }
				if(isExist){
					oos.writeObject(version + " 200 OK\n");
				    while((iterator.hasNext()))                                    
				    {
				    	listOne = (rfcList) iterator.next();
				    	if(rfcName.equals(listOne.rfc)){
				    		oos.writeObject(rfcName + " " + rfcTitle + " " + listOne.host + " " + listOne.port + "\n");
				    	}
				    }
				    oos.writeObject("\n");
				} else{
					oos.writeObject("404 Not Found");
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}

		/*
		 * Method to list all RFCs
		 */
		private void listAllRFCs(ObjectInputStream ois, ObjectOutputStream oos) throws ClassNotFoundException, IOException {
			try{
				oos.writeObject(version + " 200 OK\n");
				ListIterator<Object> iterator = rfcLists.listIterator();
				rfcList listOne = null;

			    while((iterator.hasNext()))                                    
			    {
			    	listOne = (rfcList) iterator.next();
			    	oos.writeObject(listOne.rfc + " " + listOne.title + " " + listOne.host + " " + listOne.port + "\n");
			    }
			    oos.writeObject("end");
			} catch(Exception e){
				
			}
		}
		
	/*
	 * Method to add a new RFC
	 */
	private void addNewRFC(ObjectInputStream ois, ObjectOutputStream oos) throws ClassNotFoundException, IOException, NumberFormatException {
		int clientPort = 0;
		try {
				String rfc = (String) ois.readObject();
				String hostName = (String) ois.readObject();
				String port = (String) ois.readObject();
				String title = (String) ois.readObject();
				clientPort = Integer.parseInt((String) ois.readObject());
				rfcList rfcObj = new rfcList(rfc, hostName, port, title, clientPort);
				rfcLists.add(rfcObj);
				System.out.println("RFC Added successfully");
				oos.writeObject("RFC Added successfully");
			} catch (NumberFormatException e) {
				removePeer(clientPort, false);    // remove peer if adding it's RFC is failed
				System.out.println(e);
			}
	}
}
