import java.io.*;
import java.net.*;
import java.util.Scanner;

//ErrorSimulator.java
//This class is the intermediate host between the client and server
//for assignment 1.
//
//by: Damjan Markovic

public class ErrorSimulator {

	static DatagramSocket receiveSocket; //socket which receives requests from client on port 1024
	static DatagramSocket sendReceiveSocket; //socket which sends and receives UDP packets from the server
	DatagramPacket sendPacket; //packet which relays request from client to server
	private int errorSelected;
	private int errorData;
	private int errorAck;
	static PacketManager packetManager = new PacketManager(); // The object that controls all the packets transferred
	public ErrorSimulator() {
		try {
			//construct a datagram socket and bind it to any available port on the local machine
			sendReceiveSocket = new DatagramSocket();
			//construct a datagram socket and bind it to port 1024 on the local machine
			receiveSocket = new DatagramSocket(68);
		} catch (SocketException se) {   //can't create a socket.
			se.printStackTrace();
			System.exit(1);
		}
		errorSelected = -1;
		errorData = -1;
		errorAck = -1;
	}

	//method which: 
	//receives requests from the client and responses from the server
	//sends requests to the server and responses to the client
	public static void receiveAndSend() {
		byte data[] = new byte[100];
		int clientPort; //port from which receiving client packet came from
		DatagramPacket receiveSendPacket = new DatagramPacket(data, data.length);
		try {
			//block until a datagram is received via sendReceiveSocket.  
			receiveSocket.receive(receiveSendPacket);
		} catch(IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		clientPort = receiveSendPacket.getPort();
		int len = receiveSendPacket.getLength();

		String host = "Error Simulator";
		//display packet received info from the Client to the console
		packetManager.displayPacketInfo(receiveSendPacket, host, false);
		System.out.print("Containing: ");

		//form a string from the byte array and print out the byte array
		String received = new String(data,0,len);   
		System.out.println(received);
		System.out.print("As bytes: [");
		for(int i = 0; i < len; i++) {
			if (i == len-1) {
				System.out.print(data[i]);
			} else {
				System.out.print(data[i] + ",");
			}
		}
		System.out.println("]\n");

		//set the port for the packet to be that of the servers receive socket
		receiveSendPacket.setPort(69);
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
		len = responsePacket.getLength();
		packetManager.displayPacketInfo(responsePacket, host, false);
		//form a string from the byte array.
		String response = new String(data,0,len);   
		System.out.println(response + "\n");

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

	private static DatagramPacket receiveClientPacket(){
		byte data[] = new byte[100];

		DatagramPacket receiveSendPacket = new DatagramPacket(data, data.length);
		try {
			//block until a datagram is received via sendReceiveSocket.  
			receiveSocket.receive(receiveSendPacket);
		} catch(IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		int len = receiveSendPacket.getLength();
		System.out.println("Setting up invalid packet..");

		String s = "Error Simulator";
		//print out data from socket to be relayed to the server
		packetManager.displayPacketInfo(receiveSendPacket, s, true);
		System.out.print("Containing: ");
		String received = new String(data,0,len);   
		System.out.println(received);
		System.out.print("As bytes: [");
		for(int i = 0; i < len; i++) {
			if (i == len-1) {
				System.out.print(data[i]);
			} else {
				System.out.print(data[i] + ",");
			}
		}
		System.out.println("]");
		//set the port for the packet to be that of the servers receive socket
		receiveSendPacket.setPort(69);

		//relay the socket to the server
		try {
			sendReceiveSocket.send(receiveSendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("\nInvalid Packet Sent");

		return receiveSendPacket;
	} 

	/*
	 * 
	 */
	private static DatagramPacket receiveServerPacket(){
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
		System.out.println("Response received:");
		String s = "Error Simulator";
		//print out data from socket to be relayed to the server
		packetManager.displayPacketInfo(responsePacket, s, false);

		//relay response packet to client
		try {
			DatagramSocket relayToClient = new DatagramSocket();
			relayToClient.send(responsePacket);
			relayToClient.close();

		} catch (IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
		System.out.println("Response from server relayed to client.\n");
		return responsePacket;
	}
	/*
	 * Creates an invalid read/write request packet
	 */
	public void createInvalidPacket(){
		DatagramPacket receiveSendPacket = receiveClientPacket();
		if (errorSelected == 1){
			System.out.println("Creating an invalid opcode Packet.");
			receiveSendPacket.getData()[1] = 8; //setting an invalid opcode
			System.out.println("Containing: "+ new String (receiveSendPacket.getData()));
			receiveServerPacket();
		}
		if(errorSelected == 2){
			System.out.println("Creating an invalid mode Packet.");
			//String invalidMode = "invalidMode";
			//byte[] mode = invalidMode.getBytes();
		}
		if (errorSelected == 3){

		}
	}

	/*
	 * Creates an invalid Client DATA packet 
	 */
	public static void createInvalidDataPacket(){
		DatagramPacket receiveSendPacket = receiveClientPacket();
		if (receiveSendPacket.getData()[1] == 3){
			receiveSendPacket.getData()[1] = 9;
			System.out.println("Containing: "+ new String (receiveSendPacket.getData()));
			receiveServerPacket();
		}
	}

	/*
	 * Creates an invalid Client ACK packet 
	 */
	public static void createInvalidAckPacket(){
		DatagramPacket receiveSendPacket = receiveClientPacket();
		if (receiveSendPacket.getData()[1] == 4){
			receiveSendPacket.getData()[1] = 7;
			System.out.println("Containing: "+ new String (receiveSendPacket.getData()));
			receiveServerPacket();
		}
	}

	/*
	 * Creates an invalid Server DATA packet 
	 */
	public static void createInvalidServerDataPacket(){
		DatagramPacket receiveSendPacket = receiveServerPacket();
		if (receiveSendPacket.getData()[1] == 3){
			receiveSendPacket.getData()[1] = 7;
			System.out.println("Containing: "+ new String (receiveSendPacket.getData()));
			receiveClientPacket();
		}
	}

	/*
	 * Creates an invalid Server DATA packet 
	 */
	public static void createInvalidServerAckPacket(){
		DatagramPacket receiveSendPacket = receiveServerPacket();
		if (receiveSendPacket.getData()[1] == 4){
			receiveSendPacket.getData()[1] = 7;
			System.out.println("Containing: "+ new String (receiveSendPacket.getData()));
			receiveClientPacket();
		}
	}

	public void startErr() throws IOException
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
			inputMenu = keyboard.next();

			if(inputMenu.equals("1") || inputMenu.equals("2") || inputMenu.equals("3")){
				validInput = true;
			}else{
				System.out.println("Please enter a value from 1 to 3, thank you");
				validInput = false;
			}
			if (inputMenu.equals("2")){
				System.out.println("Error code 5 (UNKNOWN TID) simulated.\n");
			}
			if(inputMenu.equals("3")){
				System.out.println("Error simulator not running..");
				receiveAndSend(); //receive and send requests/responses
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
					System.out.println("Sending invalid opcode.\n");
					createInvalidPacket();
					break;
				}
				case 2:{
					System.out.println("Sending invalid mode.\n");
					createInvalidPacket();
					break;
				}
				case 3:{
					System.out.println("Missing filename error.\n");
					createInvalidPacket();
					break;
				}
				case 4:{
					System.out.println("No termination on packet error.\n");
					createInvalidPacket();
					break;
				}
				case 5:{
					System.out.println("Invalid ending on packet.\n");
					createInvalidPacket();
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
						break;
					}
					case 2:{
						System.out.println("Server sending invalid DATA..\n");
						createInvalidServerDataPacket();
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
						break;
					}
					case 2:{
						System.out.println("Server sending invalid ACK..\n");
						createInvalidServerAckPacket();
						break;
					}
					}
				}while (!validInput);
			}	
		}
		keyboard.close();
	}

	public static void main( String args[] ) throws IOException
	{
		ErrorSimulator h = new ErrorSimulator();
		h.startErr();
	}
}