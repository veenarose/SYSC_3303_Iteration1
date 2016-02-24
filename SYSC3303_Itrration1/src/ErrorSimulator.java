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
	private int errorData;					 //
	private int errorAck;				 	 //
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
		errorData = -1;
		errorAck = -1;
	}

	/* Method which: 
	 * Receives requests from the client and responses from the server
	 * Sends requests to the server and responses to the client
	 */	
	public static void receiveAndSend() {
		byte data[] = new byte[100];
		//int clientPort; //port from which receiving client packet came from
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
		po.setPort(clientPort);;
		//relay response packet to client
		try {
			DatagramSocket relayToClient = new DatagramSocket();
			relayToClient.send(po);
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
	private void createInvalidPacket(){
		DatagramPacket receivePacket = receiveClientPacket();
		clientPort = receivePacket.getPort();
		setClientIP(receivePacket.getAddress());
		//setting an invalid opcode error
		if (errorSelected == 1){
			System.out.println("Created an invalid opcode packet.");
			receivePacket.getData()[1] = 8;
			System.out.print("Containing: ");
			packetManager.printTFTPPacketData(receivePacket.getData());
			System.out.println("Invalid opcode packet sent.");
			sendPacket(receivePacket);
		}
		//setting an invalid mode error
		if(errorSelected == 2){
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
		//setting a missing filename error
		if (errorSelected == 3){
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
		//setting a no termination error
		if (errorSelected == 4){
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
		//setting a no termination error
		if (errorSelected == 5){
			System.out.println("Created a no ending packet.");
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
	 * Creates an invalid Client and Server DATA packet 
	 */
	private void createInvalidDataPacket(){
		DatagramPacket receivePacket = receiveClientPacket();
		if (errorData == 1 || errorData == 2){
			receivePacket.getData()[2] = 9;
			System.out.println("Containing: "+ new String (receivePacket.getData()));
			sendPacket(receivePacket);
		}
	}

	/*
	 * Creates an invalid Client and Server ACK packet 
	 */
	private void createInvalidAckPacket(){
		DatagramPacket receiveSendPacket = receiveClientPacket();
		if (errorAck == 1 || errorAck == 2){
			receiveSendPacket.getData()[1] = 7;
			System.out.println("Containing: "+ new String (receiveSendPacket.getData()));
			sendPacket(receiveSendPacket);
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
		Scanner keyboard = new Scanner(System.in);
		boolean validInput;
		String inputMenu, inputCode, inputData, inputAck;
		System.out.println("Welcome to Error Simulator.");
		//get user to simulate error or without errors
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

		//simulate error code 4
		if (inputMenu.equals("1")){
			do
			{
				System.out.println("\nYou have chosen error code 4, please select an error from below.");
				System.out.println("	(1) - Invalid opcode");
				System.out.println("	(2) - Invalid mode");
				System.out.println("	(3) - Missing filename");
				System.out.println("	(4) - No termination after ending 0");
				System.out.println("	(5) - No ending 0");
				System.out.println("	(6) - Invalid DATA");
				System.out.println("	(7) - Invalid ACK");
				inputCode = keyboard.next();

				errorSelected = Integer.valueOf(inputCode);

				if ((errorSelected < 1) || (errorSelected > 7)){
					System.out.println("Please enter a value from 1 to 5, thank you");
					validInput = false;
				}
				switch(errorSelected){
				case 1:{
					System.out.println("You selected, invalid opcode error.\n");
					createInvalidPacket();
					serverResponse();
					break;
				}
				case 2:{
					System.out.println("You selected, invalid mode error.\n");
					createInvalidPacket();
					serverResponse();
					break;
				}
				case 3:{
					System.out.println("Missing filename error.\n");
					createInvalidPacket();
					serverResponse();
					break;
				}
				case 4:{
					System.out.println("No termination on packet error.\n");
					createInvalidPacket();
					serverResponse();
					break;
				}
				case 5:{
					System.out.println("Invalid ending on packet.\n");
					createInvalidPacket();
					serverResponse();
					break;
				}
				}
			}while (!validInput);
			if (errorSelected == 6){
				do
				{
					System.out.println("\nWhich host is going to send invalid DATA.");
					System.out.println("	(1) - Client sends DATA");
					System.out.println("	(2) - Server sends DATA");
					inputData = keyboard.next();

					errorData = Integer.valueOf(inputData);
					if ((errorData < 1) || (errorData > 2)){
						System.out.println("Please enter a value from 1 to 2, thank you");
						validInput = false;
					}
					switch(errorData){
					case 1:{
						System.out.println("Client sending invalid DATA..\n");
						createInvalidDataPacket();
						serverResponse();
						break;
					}
					case 2:{
						System.out.println("Server sending invalid DATA..\n");
						createInvalidDataPacket();
						serverResponse();
						break;
					}
					}
				}while (!validInput);
			}
			if (errorSelected == 7){
				do
				{
					System.out.println("\nWhich host is going to send invalid ACK.");
					System.out.println("	(1) - Client sends ACK");
					System.out.println("	(2) - Server sends ACK");
					inputAck = keyboard.next();

					errorAck = Integer.valueOf(inputAck);
					if ((errorAck < 1) || (errorAck > 2)){
						System.out.println("Please enter a value from 1 to 2, thank you");
						validInput = false;
					}
					switch(errorAck){
					case 1:{
						System.out.println("Client sending invalid ACK..\n");
						createInvalidAckPacket();
						serverResponse();
						break;
					}
					case 2:{
						System.out.println("Server sending invalid ACK..\n");
						createInvalidAckPacket();
						serverResponse();
						break;
					}
					}
				}while (!validInput);
			}	
		}
		keyboard.close();
	}

	/*
	 * The Main Method
	 */
	public static void main( String args[] ) throws IOException
	{
		ErrorSimulator h = new ErrorSimulator();
		h.startErr();
	}

	public static InetAddress getClientIP() {
		return clientIP;
	}

	public static void setClientIP(InetAddress clientIP) {
		ErrorSimulator.clientIP = clientIP;
	}
}