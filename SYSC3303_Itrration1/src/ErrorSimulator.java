import java.io.*;
import java.net.*;
import java.util.Scanner;

/* ErrorSimulator.java
 * This class is the intermediate host between the client and server
 * for assignment 1.
 * 
 * by: Damjan Markovic
 */

public class ErrorSimulator {

	static DatagramSocket receiveSocket;	 //socket which receives requests from client on port 1024
	static DatagramSocket sendReceiveSocket; //socket which sends and receives UDP packets from the server
	static DatagramSocket unknownSocket;	 //socket which sends unknown packets to client
	DatagramPacket sendPacket;				 //packet which relays request from client to server
	private int errorSelected;				 //to store in user choice for error code 4
	private int errorSelected2;
	private int packetDelay;
	private int errorHost;					 //
	private int errorBlkNum;				 //to store the user selected block number
	static PacketManager packetManager = new PacketManager(); // The object that controls all the packets transferred
	private static int clientPort;
	private static InetAddress clientIP;
	private static ProfileData pd = new ProfileData();
	public ErrorSimulator() {
		try {
			//construct a datagram socket and bind it to any available port on the local machine
			sendReceiveSocket = new DatagramSocket();
			//construct a datagram socket and bind it to port 68 on the local machine
			receiveSocket = new DatagramSocket(pd.getIntermediatePort());
			//construct a datagram socket and bind it to any available port on the local machine
			unknownSocket = new DatagramSocket();
		} catch (SocketException se) {   //can't create a socket.
			se.printStackTrace();
			System.exit(1);
		}
		errorSelected = -1;
		errorSelected2 = -1;
		packetDelay = -1;
		errorHost = -1;
		errorBlkNum = -1;
	}

	/* Method which: 
	 * Receives requests from the client and responses from the server
	 * Sends requests to the server and responses to the client
	 */	
	public static void receiveAndSend() {
		byte data[] = new byte[100];
		DatagramPacket receiveSendPacket = new DatagramPacket(data, data.length);
		try {
			//block until a datagram is received via sendReceiveSocket.  
			receiveSocket.receive(receiveSendPacket);
		} catch(IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		clientPort = receiveSendPacket.getPort();
		setClientIP(receiveSendPacket.getAddress());
		int len = receiveSendPacket.getLength();

		String host = "Error Simulator";
		//display packet received info from the Client to the console
		packetManager.displayPacketInfo(receiveSendPacket, host, false);
		System.out.print("Containing: ");
		packetManager.printTFTPPacketData(receiveSendPacket.getData());
		System.out.println();
		//set the port for the packet to be that of the servers receive socket
		receiveSendPacket.setPort(pd.getServerPort());
		//display packet info being sent to Server to the console

		packetManager.displayPacketInfo(receiveSendPacket, host, true);
		System.out.print("Containing: ");
		System.out.println(new String(receiveSendPacket.getData(),0,len));

		//relay the socket to the server
		try {
			sendReceiveSocket.send(receiveSendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		//create packet in which to store server response
		byte respData[] = new byte[100];
		DatagramPacket responsePacket = new DatagramPacket(respData, respData.length);

		try {
			//block until a packet is received via sendReceiveSocket from server  
			sendReceiveSocket.receive(responsePacket);
		} catch(IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		System.out.println();
		len = responsePacket.getLength();
		packetManager.displayPacketInfo(responsePacket, host, false);
		//form a string from the byte array.
		String response = new String(data,0,len);   
		System.out.println(response+"\n");
		//set the response packet's port destination to that of the client's sendReceive socket
		responsePacket.setPort(clientPort);
		len = responsePacket.getLength();
		packetManager.displayPacketInfo(responsePacket, host, true);
		System.out.print("Containing: ");
		System.out.println(new String(responsePacket.getData(),0,len));

		//relay response packet to client
		try {
			DatagramSocket relayToClient = new DatagramSocket();
			relayToClient.send(responsePacket);
			relayToClient.close();

		} catch (IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}

	}

	/*
	 * Receives the data packet from the client
	 * @return The DatagramPacket received 
	 */
	private static DatagramPacket receiveClientPacket(){
		byte data[] = new byte[100];

		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		clientPort = receivePacket.getPort();
		clientIP = receivePacket.getAddress();
		try {
			//block until a datagram is received via sendReceiveSocket.  
			receiveSocket.receive(receivePacket);
		} catch(IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		return receivePacket;
	}

	/*
	 * Send the DatagramPacket to the Server
	 * @return the DatagramPacket we are sending 
	 */
	private static DatagramPacket sendPacket(DatagramPacket po){
		//set the port for the packet to be that of the servers receive socket
		po.setPort(pd.getServerPort());

		//relay the socket to the server
		try {
			sendReceiveSocket.send(po);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return po;
	} 

	/*
	 * Receives the data packet from the client
	 * @return The DatagramPacket received
	 */
	private static DatagramPacket receiveServerPacket(){
		//create packet in which to store server response
		byte respData[] = new byte[100];
		DatagramPacket lo = new DatagramPacket(respData, respData.length);

		try {
			//block until a packet is received via sendReceiveSocket from server  
			sendReceiveSocket.receive(lo);
		} catch(IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		System.out.println("Response received:");
		System.out.println("Contains: " + new String(lo.getData()));
		return lo;
	}

	/*
	 * Send the DatagramPacket to the client 
	 * @return the DatagramPacket we are sending 
	 */
	private static DatagramPacket sendPacketToClient(DatagramPacket po){
		//relay response packet to client
		DatagramPacket p = new DatagramPacket(po.getData(),po.getData().length, clientIP, clientPort);
		try {
			DatagramSocket relayToClient = new DatagramSocket();
			relayToClient.send(p);
			relayToClient.close();

		} catch (IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		System.out.println("Response sent to Client:");
		return po;
	}

	/*
	 * Creates an invalid read/write request packet
	 */
	private void createInvalidRequestPacket(){
		DatagramPacket receivePacket = receiveClientPacket();
		clientPort = receivePacket.getPort();
		setClientIP(receivePacket.getAddress());
		//setting an invalid opcode error
		if (errorSelected == 1){
			System.out.println("Created an invalid opcode packet.");
			byte[] inValid = receivePacket.getData();
			inValid[1] = 8;
			receivePacket.setData(inValid);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid opcode packet sent.");
			sendPacket(receivePacket);
		}
		else if(errorSelected == 2){			//setting an invalid mode error
			System.out.println("Created an invalid mode packet.");
			String invalidMode = "invalidMode";
			byte[] req = packetManager.createRead(packetManager.getFilename(receivePacket.getData()), invalidMode);
			receivePacket.setData(req);
			System.out.println("Containing: "+new String(receivePacket.getData()));
			System.out.print("In Bytes: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid mode packet sent.");
			sendPacket(receivePacket);
		}
		else if (errorSelected == 3){			//setting a missing filename error
			System.out.println("Created a missing filename packet.");
			String missFile = " ";
			byte[] req = packetManager.createRead(missFile, packetManager.getMode(receivePacket.getData()));
			receivePacket.setData(req);
			System.out.println("Containing: "+new String(receivePacket.getData()));
			System.out.print("In Bytes: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Missing filename packet sent.");
			sendPacket(receivePacket);
		}
		else if (errorSelected == 4){			//setting a no termination error
			System.out.println("Created a no termination packet.");
			byte[] oldReq = receivePacket.getData();
			byte[] newReq = new byte[oldReq.length+1];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length);
			receivePacket.setData(newReq);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid termination packet sent.");
			sendPacket(receivePacket);
		}
		else if (errorSelected == 5){			//setting a no termination error
			System.out.println("Created a no ending zero packet.");
			byte[] oldReq = receivePacket.getData();
			byte[] newReq = new byte[oldReq.length-1];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length-1);
			receivePacket.setData(newReq);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid ending packet sent.");
			sendPacket(receivePacket);
		}
	}

	/*
	 * Waits for the response from Server and relay back to Client
	 */
	private void serverResponse(){
		System.out.println("\nWaiting for a response..");
		DatagramPacket serverPacket = receiveServerPacket();
		serverPacket.setPort(clientPort);
		sendPacketToClient(serverPacket);
	}

	/*
	 * Creates an invalid DATA\ACK packet according to the selected Host 
	 */
	private void createInvalidDataAckPacket(){
		DatagramPacket receivePacket = receiveClientPacket();  //receive DATA/ACK packet from client
		DatagramPacket receivePacket2 = receiveServerPacket(); //receive DATA/ACK packet from server
		clientPort = receivePacket.getPort();
		setClientIP(receivePacket.getAddress());
		//setting an invalid mode error
		if(errorSelected2 == 1 && errorHost == 1){					//if client sends an invalid opcode
			System.out.println("Created an invalid opcode packet.");
			byte[] inValid = receivePacket.getData();
			inValid[1] = 8;
			receivePacket.setData(inValid);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid opcode packet sent.");
			sendPacket(receivePacket);
		}
		else if (errorSelected2 == 1 && errorHost == 2){			//if server sends an invalid opcode
			System.out.println("Created an invalid opcode packet.");
			byte[] inValid = receivePacket2.getData();
			inValid[1] = 8;
			receivePacket2.setData(inValid);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket2.getData());
			System.out.println("Invalid opcode packet sent.");
			receivePacket2.setPort(clientPort);
			sendPacketToClient(receivePacket2);
		}
		//setting an block number error
		if(errorSelected2 == 2 && errorHost == 1){					//if client sends an invalid block number on DATA/ACK packet
			System.out.println("Created an invalid block number packet.");
			byte[] blkNum = receivePacket.getData();
			byte lsb_blockNumber = blkNum[3];
			lsb_blockNumber += 1;
			blkNum[3] = lsb_blockNumber;
			receivePacket.setData(blkNum);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid Block Number packet sent.");
			sendPacket(receivePacket);
		}
		else if (errorSelected2 == 2 && errorHost == 2){			//if server sends an invalid block number on DATA/ACK packet
			System.out.println("Created an invalid block number packet.");
			byte[] blkNum = receivePacket2.getData();
			byte lsb_blockNumber = blkNum[3];
			lsb_blockNumber += 1;
			blkNum[3] = lsb_blockNumber;
			receivePacket2.setData(blkNum);
			packetManager.printTFTPPacketData(receivePacket2.getData());
			System.out.println("Invalid Block Number packet sent.");
			receivePacket2.setPort(clientPort);
			sendPacketToClient(receivePacket2);
		}
		//setting a delayed DATA/ACK packet
		if(errorSelected2 == 3 && errorHost == 1){					//if client sends a delayed DATA/ACK packet
			
		}
		else if(errorSelected2 == 3 && errorHost == 2){				//if server sends a delayed DATA/ACK packet
			
		}
	}

	/*
	 * Creates an Unknown Transfer ID
	 */
	private static void unknownTID(){
		DatagramPacket receiveSendPacket = receiveServerPacket();
		try {
			//block until a datagram is received via unknownSocket.  
			unknownSocket.receive(receiveSendPacket);
		} catch(IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		receiveClientPacket();
	}

	/*
	 * Attempting to shutdown the server
	 */
	private void shutDown(){
		byte respData[] = new byte[10];
		DatagramPacket sendPackets = new DatagramPacket(respData,respData.length);
		byte[] arr = {1,1};
		sendPackets.setData(arr);
		sendPacket(sendPackets);
	}
	/*
	 * Error Simulation
	 */
	private void startErr() throws IOException
	{
		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
		boolean validInput;
		String inputMenu, inputType, inputCode, inputHost, inputNum, inputDelay;
		//get user to select an error code to which an error to be simulated on 
		do
		{
			System.out.println("\nChoose your mode:");
			System.out.println("	(1) - Error Code 4");
			System.out.println("	(2) - Error Code 5");
			System.out.println("	(3) - No error");
			System.out.println("	(4) - Shutdown Server");
			inputMenu = keyboard.next();

			if(inputMenu.equals("1") || inputMenu.equals("2") || inputMenu.equals("3")|| inputMenu.equals("4")){
				validInput = true;
			}else{
				System.out.println("Please enter a value from 1 to 3, thank you");
				validInput = false;
			}

			if (inputMenu.equals("2")){
				System.out.println("Error code 5 (UNKNOWN TID) simulated.");
				unknownTID();
			}
			if(inputMenu.equals("3")){
				System.out.println("Error simulator not running..\n");
				receiveAndSend(); //receive and send requests/responses
			}
			if(inputMenu.equals("4")){
				System.out.println("Attempting to shutdown server..\n");
				shutDown(); //Attempting to shutdown server
			}
		} while (!validInput);
		//get user input to select the type of packet the error to be simulated on
		do
		{
			System.out.println("\nPlease select the type of packet, that you want to simulate errors for:");
			System.out.println("	(1) - RRQ/WRQ packets");
			System.out.println("	(2) - DATA/ACK packets");
			inputType = keyboard.next();

			if(inputType.equals("1") || inputType.equals("2")){
				validInput = true;
			}else{
				System.out.println("Please enter a value between 1 and 2, thank you");
				validInput = false;
			}
		}while (!validInput);
		//simulate error code 4 on request packets
		if (inputType.equals("1")){
			do
			{
				System.out.println("\nPlease select the type of error from below.");
				System.out.println("	(1) - Invalid opcode");
				System.out.println("	(2) - Invalid mode");
				System.out.println("	(3) - Missing filename");
				System.out.println("	(4) - No termination after ending 0");
				System.out.println("	(5) - No ending 0");
				inputCode = keyboard.next();

				errorSelected = Integer.valueOf(inputCode);

				if ((errorSelected < 1) || (errorSelected > 5)){
					System.out.println("Please enter a value from 1 to 5, thank you");
					validInput = false;
				}
				System.out.println("Error packet simulator ready..");
				createInvalidRequestPacket();
				serverResponse();
			}while (!validInput);
		}
		else if (inputType.equals("2")){
			do
			{
				System.out.println("\nPlease select the type of error from below.");
				System.out.println("	(1) - Invalid opcode");
				System.out.println("	(2) - Invalid block number");
				System.out.println("	(3) - DATA/ACK delay");
				System.out.println("	(4) - DATA/ACK duplicate");
				System.out.println("	(5) - DATA/ACK loss");
				inputCode = keyboard.next();

				errorSelected2 = Integer.valueOf(inputCode);

				if ((errorSelected2 < 1) || (errorSelected2 > 5)){
					System.out.println("Please enter a value from 1 to 5, thank you");
					validInput = false;
				}
				if (errorSelected2 == 3){
					do
					{
						validInput = true;
						System.out.println("Enter the delay amount in (ms) ");
						inputDelay = keyboard.next();

						try
						{
							packetDelay = Integer.valueOf(inputDelay);
						}
						catch (NumberFormatException e)
						{
							System.out.println("Please enter a value from " + 0 + " to " + 999999 );
							validInput = false;
						}
						if ((packetDelay < 0) || (packetDelay > 999999))
						{
							System.out.println("Please enter a value from " + 0 + " to " + 999999 );
							validInput = false;
						}
					}while (!validInput);
				}
			}while (!validInput);
			do
			{
				validInput = true;
				System.out.println("\nWhich host is going to cause the simulated error.");
				System.out.println("	(1) - Client");
				System.out.println("	(2) - Server");
				inputHost = keyboard.next();

				errorHost = Integer.valueOf(inputHost);
				if ((errorHost < 1) || (errorHost > 2)){
					System.out.println("Please enter a value from 1 to 2, thank you");
					validInput = false;
				}

			}while (!validInput);
			do
			{
				validInput = true;
				System.out.println("Enter the block number of the packet you wish to cause the error.");
				inputNum = keyboard.next();

				errorBlkNum = Integer.valueOf(inputNum);
				if ((errorBlkNum < 0) || (errorBlkNum > 65535)){
					System.out.println("Please enter a value from 0 to 65535, thank you");
					validInput = false;
				}
				System.out.println("\nError simulator ready..");
				createInvalidDataAckPacket();
				serverResponse();
			}while (!validInput);
			
		}
		//keyboard.close();
	}

	/*
	 * The Main Method
	 */
	public static void main( String args[] ) throws IOException
	{
		ErrorSimulator h = new ErrorSimulator();
		System.out.println("Welcome to Error Simulator.");
		h.startErr();
		@SuppressWarnings("resource")
		Scanner loop = new Scanner(System.in);
		while(true){
			System.out.println("Do you want to simulate an error again (yes/no)? ");
			String s = loop.next();
			if(s.equals("yes"))
			{
				h.startErr();
			}
			else if(s.equals("no")){
				System.out.println("Bye.");
				break;
			}
		}
	}

	public static InetAddress getClientIP() {
		return clientIP;
	}

	public static void setClientIP(InetAddress clientIP) {
		ErrorSimulator.clientIP = clientIP;
	}
}