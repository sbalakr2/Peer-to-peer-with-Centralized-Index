# Peer-to-peer-with-Centralized-Index
CSC 573 - Internet Protocols
Project #1 - Implementation of a centralized P2P system
-------------------------------------------------------

Instructions to compile and execute the code:
---------------------------------------------
The above program requires java environment. The RFC files are placed in the folder “RFC” and they can be registered by the clients. 
When a peer transfers an RFC say. RFC123, it gets saved as RFC123_NEW.txt in the requesting peer.
Note: We have tested the project in Windows OS.

Steps to run the server code:
-----------------------------
1. In command prompt, go to the location of the “src” directory of the project
2. Compile the code by executing the command: javac mypack/serverSocket.java
3. Run the code by executing the command: java mypack.serverSocket #port
   #port - port number at which to start the server

Steps to run the client code:
-----------------------------
1. In command prompt, go to the location of the “src” directory of the project
2. Compile the code by executing the command: javac mypack/clientSocket.java
3. Run the code by executing the command: java mypack.clientSocket #ipaddress #port
   #ipaddress - ip at which the server is running
   #port - port number at which to start the server

Now we can run multiple clients which will act as peers of the centralized P2P system. A peer can ADD, LIST, LOOKUP or GET RFCs. 
Enter QUIT to close the peer connection.

Error Conditions:
---------------------
The details for testing the error conditions are given below:

1. 400 Bad Request
On selecting options from the client other than ADD, LIST, LOOKUP and GET, the server sends 400 BAD REQUEST as a response to the client.

2. 505 P2P-CI Version Not Supported
In clientSocket.java, comment line 23 and uncomment line 24. Run the client and connect to another peer to get an RFC. The 505 P2P-CI 
Version Not Supported error message appears.

3. 404 Not Found
In order to test the 404 NOT FOUND error, add an RFC that does not exist or make a lookup request to the server for an RFC that does not 
exists in the network with any of the client. The server will respond with 404 NOT FOUND status message.

