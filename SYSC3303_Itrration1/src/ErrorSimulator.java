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
	private static int modeSelected;
	private int packetDelay;
	private int errorHost;					 //
	private int errorBlkNum;				 //to store the user selected block number
	static PacketManager packetManager = new PacketManager(); // The object that controls all the packets transferred
	private static int clientPort;
	//private static int serverPort;
	private static InetAddress clientIP;
	private static InetAddress serverIP;
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
		modeSelected = -1;
		packetDelay = -1;
		errorHost = -1;
		errorBlkNum = -1;
	}

	/* Method which: 
	 * Receives requests from the client and responses from the server
	 * Sends requests to the server and responses to the client
	 */	
	public void receiveAndSend() {
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
	 * Check for user selection
	 */
	private void modeSelection(){
		// Iteration2
		if(modeSelected == 3){
			System.out.println("Error simulator not running..\n");
			receiveAndSend();
		}
		if (modeSelected == 2){
			System.out.println("Error code 5 (UNKNOWN TID) simulated.");
			unknownTID();
		}
		//**********************************************************************

		//Iteration3
		// Delayed Modes
		if(modeSelected == 4){
			System.out.println("You have selected Delayed Packets...\n");
			// TODO: Delayed Packets
		}		
		if(modeSelected == 5){
			System.out.println("You have selected Delayed ACK...\n");
			// TODO: Delayed ACK
		}

		// Lost Modes
		if(modeSelected == 6){
			System.out.println("You have selected Lost Packets...\n");
			// TODO: Lost Packets
		}		
		if(modeSelected == 7){
			System.out.println("You have selected Lost ACK...\n");
			// TODO: Lost ACK
		}

		// Duplicate Modes
		if(modeSelected == 8){
			System.out.println("You have selected Duplicate Packets...\n");
			// TODO: Duplicate Packets
		}		
		if(modeSelected == 9){
			System.out.println("You have selected Duplicate ACK...\n");
			// TODO: Duplicate ACK
		}

		//**********************************************************************
		// Shutdown
		if(modeSelected == 10){
			System.out.println("Attempting to shutdown server..\n");
			shutDown(); //Attempting to shutdown server
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
	private static DatagramPacket sendPacketToServer(DatagramPacket po){
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
		//		serverPort = lo.getPort();
		serverIP = lo.getAddress();
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

		if (errorSelected == 1){								//setting a large packet
			System.out.println("Created a very large packet.");
			byte[] oldReq = receivePacket.getData();
			byte[] newReq = new byte[1000];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length);
			newReq[newReq.length - 1] = 1;
			receivePacket.setData(newReq);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid packet size sent.");
			sendPacketToServer(receivePacket);
		}
		else if (errorSelected == 2){							//setting an invalid opcode error
			System.out.println("Created an invalid opcode packet.");
			byte[] inValid = receivePacket.getData();
			inValid[1] = 8;
			receivePacket.setData(inValid);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid opcode packet sent.");
			sendPacketToServer(receivePacket);
		}
		else if(errorSelected == 3){			//setting an invalid mode error
			System.out.println("Created an invalid mode packet.");
			String invalidMode = "invalidMode";
			byte[] req = packetManager.createRead(packetManager.getFilename(receivePacket.getData()), invalidMode);
			receivePacket.setData(req);
			System.out.println("Containing: "+new String(receivePacket.getData()));
			System.out.print("In Bytes: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid mode packet sent.");
			sendPacketToServer(receivePacket);
		}
		else if (errorSelected == 4){			//setting a missing filename error
			System.out.println("Created a missing filename packet.");
			String missFile = "";
			byte[] req = packetManager.createRead(missFile, packetManager.getMode(receivePacket.getData()));
			receivePacket.setData(req);
			System.out.println("Containing: "+new String(receivePacket.getData()));
			System.out.print("In Bytes: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Missing filename packet sent.");
			sendPacketToServer(receivePacket);
		}
		else if (errorSelected == 5){			//setting a no termination error
			System.out.println("Created a no termination packet.");
			byte[] oldReq = receivePacket.getData();
			byte[] newReq = new byte[oldReq.length+1];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length);
			receivePacket.setData(newReq);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid termination packet sent.");
			sendPacketToServer(receivePacket);
		}
		else if (errorSelected == 6){			//setting a no termination error
			System.out.println("Created a no ending zero packet.");
			byte[] oldReq = receivePacket.getData();
			byte[] newReq = new byte[oldReq.length-1];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length-1);
			receivePacket.setData(newReq);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid ending packet sent.");
			sendPacketToServer(receivePacket);
		}
	}

	/*
	 * Waits for the response from Server and reply back to Client
	 */
	private void serverResponse(){
		System.out.println("\nWaiting for a response..");
		DatagramPacket serverPacket = receiveServerPacket();
		serverPacket.setPort(clientPort);
		sendPacketToClient(serverPacket);
	}

	/*
	 * Sends an invalid DATA\ACK packet to the server
	 */
	private void sendInvalidDataAckPacket(){
		receiveAndSend();
		DatagramPacket receivePacket1 = receiveClientPacket();  //receive DATA/ACK packet from client
		clientPort = receivePacket1.getPort();
		setClientIP(receivePacket1.getAddress());
		//modeSelected=3;
		receiveAndSend();
		if(errorSelected2 == 1 && errorHost == 1){				//if client sends too larger packet
			System.out.println("Created a very large packet.");
			byte[] oldReq = receivePacket1.getData();
			byte[] newReq = new byte[1000];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length);
			newReq[newReq.length - 1] = 1;
			receivePacket1.setData(newReq);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket1.getData());
			System.out.println("Invalid packet size sent.");
			sendPacketToServer(receivePacket1);
		} 
		else if(errorSelected2 == 2 && errorHost == 1){			//if client sends an invalid opcode
			System.out.println("Created an invalid opcode packet.");
			byte[] inValid = receivePacket1.getData();
			inValid[1] = 8;
			receivePacket1.setData(inValid);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket1.getData());
			System.out.println("Invalid opcode packet sent.");
			sendPacketToServer(receivePacket1);
		} 
		else if(errorSelected2 == 3 && errorHost == 1){			//if client sends an invalid block number on DATA/ACK packet
			System.out.println("Created an invalid block number packet.");
			byte[] blkNum = receivePacket1.getData();
			byte lsb_blockNumber = blkNum[3];
			lsb_blockNumber += 1;
			blkNum[3] = lsb_blockNumber;
			receivePacket1.setData(blkNum);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket1.getData());
			System.out.println("Invalid Block Number packet sent.");
			sendPacketToServer(receivePacket1);
		}
		else if(errorSelected2 == 3 && errorHost == 1){			//if client sends a delayed DATA/ACK packet

		}
	}

	/*
	 * Receives an invalid DATA\ACK packet to the server
	 */
	private void receiveInvalidDataAckPacket(){
		DatagramPacket receivePacket2 = receiveServerPacket(); //receive DATA/ACK packet from server
		receivePacket2.setPort(clientPort);

		if(errorSelected2 == 1 && errorHost == 2){			//if server sends too larger packet
			System.out.println("Created a very large packet.");
			byte[] oldReq = receivePacket2.getData();
			byte[] newReq = new byte[oldReq.length+10];
			System.arraycopy(oldReq, 0, newReq, 0, newReq.length-1);
			newReq[newReq.length - 1] = 1;
			receivePacket2.setData(newReq);
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket2.getData());
			System.out.println("Invalid packet size sent.");
			sendPacketToClient(receivePacket2);

		}
		else if (errorSelected2 == 2 && errorHost == 2){	//if server sends an invalid opcode
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
		else if (errorSelected2 == 3 && errorHost == 2){	//if server sends an invalid block number on DATA/ACK packet
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
		else if(errorSelected2 == 3 && errorHost == 2){		//if server sends a delayed DATA/ACK packet

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
		sendPacketToServer(sendPackets);
	}


	/*
	 * Error Simulation
	 */
	private void startErr() throws IOException
	{
		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
		boolean validInput;
		String inputMenu, inputType, inputReq, inputDa, inputHost, inputNum, inputDelay;
		//get user to select an error code to which an error to be simulated on 
		do
		{
			validInput = true;
			// Iteration 2 Modes 
			System.out.println("\nChoose your mode:");
			System.out.println("	(1) - Error Code 4");
			System.out.println("	(2) - Error Code 5");
			System.out.println("	(3) - No error");

			// Iteration 3 Modes 
			System.out.println("	(4) - Delayed Data Packets");
			System.out.println("	(5) - Delayed ACK");
			System.out.println("	(6) - Lost Data Packets");
			System.out.println("	(7) - Lost ACK");
			System.out.println("	(8) - Duplicate Packets");
			System.out.println("	(9) - Duplicate ACK");
			System.out.println();

			// Shutdown server 
			System.out.println("	(10) - Shutdown SERVER");
			inputMenu = keyboard.next();
			modeSelected = Integer.valueOf(inputMenu);
			if(inputMenu.equals("1") || inputMenu.equals("2") || inputMenu.equals("3")|| inputMenu.equals("4")
					|| inputMenu.equals("5")|| inputMenu.equals("6")|| inputMenu.equals("7")|| inputMenu.equals("8")
					|| inputMenu.equals("9")|| inputMenu.equals("10")){
				validInput = true;
			}else{
				System.out.println("Please enter a value from 1 to 10, thank you");
				validInput = false;
			}
			modeSelection();
		} while (!validInput);
		//get user input to select the type of packet the error to be simulated on
		if(modeSelected == 1){
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
					System.out.println("	(1) - Packet too large");
					System.out.println("	(2) - Invalid opcode");
					System.out.println("	(3) - Invalid mode");
					System.out.println("	(4) - Missing filename");
					System.out.println("	(5) - No termination after ending 0");
					System.out.println("	(6) - No ending 0");
					inputReq = keyboard.next();

					errorSelected = Integer.valueOf(inputReq);

					if ((errorSelected < 1) || (errorSelected > 6)){
						System.out.println("Please enter a value from 1 to 6, thank you");
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
					System.out.println("	(1) - Packet too large");
					System.out.println("	(2) - Invalid opcode");
					System.out.println("	(3) - Invalid block number");


					inputDa = keyboard.next();

					errorSelected2 = Integer.valueOf(inputDa);

					if ((errorSelected2 < 1) || (errorSelected2 > 3)){
						System.out.println("Please enter a value from 1 to 3, thank you");
						validInput = false;
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

				}while (!validInput);
				System.out.println("\nError simulator ready..");
				sendInvalidDataAckPacket();
				receiveInvalidDataAckPacket();
			}
		}

		/* If mode selected is 4 - 9 user needs to select the ***block number*** for delay and 
		 the ***time in milliseconds** for duplicate or lost packets and ACK. */

		if (modeSelected >= 4 && modeSelected <= 9){

			//************** Get Block number
			do
			{
				validInput = true;
				System.out.println("Enter the block number of the packet you wish to cause the error.");
				inputNum = keyboard.next();

				errorBlkNum = Integer.valueOf(inputNum);// Get block number to mess up from the user 
				if ((errorBlkNum < 0) || (errorBlkNum > 65535)){
					System.out.println("Please enter a value from 0 to 65535, thank you");
					validInput = false;
				}

			}while (!validInput);

			//******************** Get delay time to 
			do{
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
		while(true && modeSelected == 1){
			System.out.println("\nDo you want to simulate an error (yes/no)? ");
			String s = loop.next();
			if(s.equals("yes") || s.equals("y"))
			{
				h.startErr();
			}
			else if(s.equals("no")|| s.equals("n")){
				System.out.println("Bye.");
				break;
			}
		}
	}

	public static InetAddress getClientIP() {
		return clientIP;
	}

	public static InetAddress getServerIP() {
		return serverIP;
	}

	public static void setClientIP(InetAddress clientIP) {
		ErrorSimulator.clientIP = clientIP;
	}
	public static void setServerIP(InetAddress serverIP) {
		ErrorSimulator.serverIP = serverIP;
	}
}