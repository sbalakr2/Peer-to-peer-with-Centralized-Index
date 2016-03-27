package mypack;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class clientSocket implements Runnable{
	private static final String version = "P2P-CI/1.0";
	//private static final String version = "P2P-CI/2.0";
	public static List<Object> list = new ArrayList<Object>();
	public static List<Object> rfcLists = Collections.synchronizedList(list);
	public ServerSocket cserv;
	
	/*
	 * Constructor to create server socket
	 */
	public clientSocket(int port) throws IOException {
		cserv = new ServerSocket(port);
		System.out.println("Client is up at "+ InetAddress.getLocalHost().getHostAddress() + " on port "+ cserv.getLocalPort());
		new Thread(this).start();
	}
	
	public static void main(String args[]) throws IOException, ClassNotFoundException {
		String addr = "localhost";
		int port = 0;
		
		if(args.length == 2){
			addr = args[0];
			port = Integer.parseInt(args[1]);
			String address = addr;
			String hostName = InetAddress.getByName(addr).getHostName();
			Socket csock = null;
			ObjectOutputStream out = null;
			ObjectInputStream oin = null;
			
			Random random = new Random();
	        int csport = random.nextInt(5000) + 5000;
			new clientSocket(csport); // start a server at the client
			int clientPort = 0;
			try {
				csock = new Socket(address, port);
				out = new ObjectOutputStream(csock.getOutputStream());
				oin = new ObjectInputStream(csock.getInputStream());
				out.writeObject(hostName.toString());
				clientPort = csock.getLocalPort();
				System.out.println("Client running at "+ hostName + " on port " + clientPort);
				sendClientRequest(InetAddress.getByName(addr), hostName, out, oin, csport, clientPort);
			} catch (IOException e) {
				System.err.print("IO Exception - Unable to connect to server");
			}
		} else{
			System.out.println("Run as java mypack.clientSocket #ipaddress #port");
		}
	}

	/*
	 * Method to get client request
	 */
	private static void sendClientRequest(InetAddress address, String hostName,
			ObjectOutputStream out, ObjectInputStream oin, int csport, int clientPort) throws ClassNotFoundException {
		BufferedReader br = null;
		System.out.println("\nEnter ADD to add an existing RFC");
		System.out.println("Enter LIST to list all existing RFCs");
		System.out.println("Enter LOOKUP to lookup an existing RFC");
		System.out.println("Enter GET to download an RFC");
		System.out.println("Enter QUIT to shut down client");
		br = new BufferedReader(new InputStreamReader(System.in));

		try {
			forwardClientRequest(hostName, out, oin, csport, clientPort, br);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Socket read Error");
		}
	}

	/*
	 * Method to handle client request
	 */
	private static void forwardClientRequest(String hostName,
			ObjectOutputStream out, ObjectInputStream oin, int csport,
			int clientPort, BufferedReader br) throws IOException,
			ClassNotFoundException {
		String option;
		while (true) {
			if(br != null){
				System.out.println("Enter a command:");  
				option = br.readLine().trim();
				String cservport = Integer.toString(csport);  // port at which client's server is listening
				String cport = Integer.toString(clientPort);  // port at which client is connected to the centralized server
				if (option.length() == 0) {
					continue; 
				}else if (option.equalsIgnoreCase("ADD")) {
					addRFC(oin, out, br, hostName, cservport, cport);
					continue;
				} else if (option.equalsIgnoreCase("LIST")) {
					listAllRFCs(oin, out, hostName, cservport);
					continue;
				}else if (option.equalsIgnoreCase("LOOKUP")) {
					lookupRFC(oin, out, br, hostName, cservport);
					continue;
				} else if (option.equalsIgnoreCase("GET")) {
					downloadRFC(br, hostName);
					continue;
				}else if (option.equalsIgnoreCase("QUIT")) {
					System.exit(1);
					break;
				}else {
					handleErrorMessages("400 Bad Request");
					System.out.println("Enter a valid command");
					continue;
				}
			}
		}
	}

	/*
	 * Method to get an RFC
	 */
	private static void downloadRFC(BufferedReader br, String hostName) throws IOException {
		Socket getsock = null;
		String rfcName = "";
		String rfcHost = "";
		int rfcPort = 0;
		try {
			System.out.println("Enter the RFC:");
			rfcName = br.readLine().trim();
			System.out.println("Enter the host:");
			rfcHost = br.readLine().trim();
			System.out.println("Enter the port:");
			rfcPort = Integer.parseInt(br.readLine().trim());
		} catch (NumberFormatException e1) {
			System.out.println("Error: The values entered are incorrect");
		}
		
		try{
			getsock = new Socket(rfcHost, rfcPort);
			
		} catch(UnknownHostException | NullPointerException e){
			System.out.println("Unable to reach the host");
		}
		fileRequest(hostName, getsock, rfcName, rfcHost, rfcPort);
	}

	/*
	 * Method to lookup an RFC
	 */
	private static void lookupRFC(ObjectInputStream oin, ObjectOutputStream out, BufferedReader br,
			String hostName, String port) throws IOException {
		System.out.println("Enter the RFC to lookup:");
		String rfcName = br.readLine().trim();
		System.out.println("Enter the title of the RFC:");
		String rfcTitle = br.readLine().trim();
		out.writeObject(" LOOKUP RFC " + rfcName + " " + version + "\n HOST: "
				+ hostName + "\n PORT: " + port + "\n TITLE: " + rfcTitle
				+ "\n");
		out.writeObject(rfcName);
		out.writeObject(rfcTitle);
		
		try {
			String resp = ((String) oin.readObject()).trim();
			System.out.println(resp);
			if ((resp.contains("200 OK"))) {
				resp = (String) oin.readObject();
				while (resp.equalsIgnoreCase("\n") == false) {
					System.out.print(resp);
					resp = (String) oin.readObject();
				}
				return;
			} else{
				handleErrorMessages(resp);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Method to list existing RFCs
	 */
	private static void listAllRFCs(ObjectInputStream oin,
			ObjectOutputStream out, String hostName, String port)
			throws IOException {
		out.writeObject(" LIST ALL " + version + "\n HOST: " + hostName
				+ "\n PORT: " + port + "\n");

		try {
			String resp = ((String) oin.readObject()).trim();
			System.out.println(resp);
			if (! resp.startsWith(version)) {
				System.out.println("Error: Peer has different version");
				return;
			}
			if ((resp.contains("200 OK"))) {
				resp = (String) oin.readObject();
				while (resp.equalsIgnoreCase("end") == false) {
					System.out.print(resp);
					resp = (String) oin.readObject();
				}
				return;
			} else{
				handleErrorMessages(resp);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Method to add an RFC
	 */
	private static void addRFC(ObjectInputStream oin, ObjectOutputStream out, BufferedReader br,
			String hostName, String port, String localPort) throws IOException, ClassNotFoundException {
		String filename = "";
		String rfcName = "";
		String rfcTitle = "";
		
		try{
			System.out.println("Enter the RFC to add:");
			rfcName = br.readLine();
			System.out.println("Enter the title of the RFC:");
			rfcTitle = br.readLine();
		} catch(IOException e){
			e.printStackTrace();
		}
		filename = "RFC" + rfcName + ".txt";
		File directory = new File("RFC");
		File file = null;
		
		try {
			file = new File(directory.getCanonicalPath() + "\\" + filename);
			if ((file.exists())) {                       
				out.writeObject(" ADD RFC " + rfcName + " " + version + "\n HOST:"+ hostName + "\n PORT:" + port + "\n TITLE:" + rfcTitle + "\n");
				out.writeObject(rfcName);
				out.writeObject(hostName);
				out.writeObject(port);
				out.writeObject(rfcTitle);
				out.writeObject(localPort);
				System.out.println(oin.readObject());
			} else if((!file.exists())) {
				System.out.println("No such file exists!");
				handleErrorMessages("404 Not Found");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Method to request file from a peer
	 */
	private static void fileRequest(String hostName, Socket getsock,
			String rfcName, String rfcHost, int rfcPort) throws IOException {
		try {
			ObjectOutputStream gos = new ObjectOutputStream(getsock.getOutputStream());
			ObjectInputStream gis = new ObjectInputStream(getsock.getInputStream());
			gos.writeObject(" GET RFC " + rfcName + " " + version + "\n HOST: "+ hostName + "\n OS: " + System.getProperty("os.name") + "\n");
			gos.writeObject(rfcName);
			
			String peer_resp = ((String) gis.readObject()).trim();
			System.out.println(peer_resp);
			if (!peer_resp.startsWith(version)) {
				System.out.println("Error: Peer has different version");
				handleErrorMessages("505 P2P-CI Version Not Supported");
				return;
			}
			if ((peer_resp.contains("200 OK"))) {
				File directory = new File("RFC");
				File newFile = new File(directory.getCanonicalPath() + "\\RFC" + rfcName + "_NEW.txt");
				newFile.createNewFile();
				try {
					byte[] content = (byte[]) gis.readObject();
					Files.write(newFile.toPath(), content);
				} 
				catch (EOFException eof) {
					System.out.println("End of file");
				}
			} else{
				handleErrorMessages(peer_resp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		Socket sget = null;
		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;
		
		try {
			sget = cserv.accept();
			new Thread(this).start(); 
			ois = new ObjectInputStream(sget.getInputStream());
			oos = new ObjectOutputStream(sget.getOutputStream());
			
		} catch (Exception e) {
			System.out.println("Connection failure during: "+ e);
			if (sget.isConnected()) {
				try {
					sget.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			return; 
		}
		try {
			String rsp = (String) ois.readObject();
			System.out.println(rsp);
			if (rsp.contains("GET")) {
				createFile(ois, oos);            
			}
		} catch (Exception e) {
			System.out.println("Unable to send the requested file");
		} finally {
			try {
				oos.close();
				ois.close();
				sget.close();
			} catch (Exception e) {
			}
		}
	}

	/*
	 * Method to create a new file at the peer
	 */
	private void createFile(ObjectInputStream ois, ObjectOutputStream oos)
			throws IOException, ClassNotFoundException {
		String rfcName = (String) ois.readObject();
		String filePathString = "RFC" + rfcName + ".txt";          
		File directory = new File("RFC");
		File file = new File(directory.getCanonicalPath() + "\\" + filePathString);
		
		if (file.exists()) {
			fileResponse(oos, file);
		} else {
			oos.writeObject("P2P-CI/1.0 404 Not Found \n");
			oos.flush();
		}
		oos.close();
	}

	/*
	 * Method to write GET response to peer
	 */
	private void fileResponse(ObjectOutputStream oos, File file)
			throws IOException {
		oos.writeObject(version + " 200 OK \n"+
		"Date: "+ (new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss")).format(new Date(0)) + " GMT\n"+
		"OS: " + System.getProperty("os.name")+ "\n"+
		"Last Modified: " + (new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss")).format(new Date(file.lastModified())) +" GMT \n"+
		"Content-Length: " + file.length() + "\nContent-Type: Text \n");
		byte[] content = Files.readAllBytes(file.toPath());
		oos.writeObject(content);
	}
	
	/*
	 * Method to display error messages
	 */
	private static void handleErrorMessages(String msg) {
		if ((msg.startsWith("400 Bad Request"))) {
			System.out.println("Error: 400 Bad Request");
		} else if ((msg.startsWith("404 Not Found"))) {
			System.out.println("Error: 404 Not Found");
		} else if ((msg.startsWith("505 P2P-CI Version Not Supported"))) {
			System.out.println("Error: 505 P2P-CI Version Not Supported");
		} else {
			System.out.println("Error: Unknown message format\n" + "Only P2P-CI/1.0 is supported");
		}
	}
}