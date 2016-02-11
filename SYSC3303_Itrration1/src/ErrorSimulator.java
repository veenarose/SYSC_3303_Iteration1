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
	static PacketManager packetManager; // The object that controls all the packets transferred
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
	}

	//method which: 
	//receives requests from the client and responses from the server
	//sends requests to the server and responses to the client
	public void receiveAndSend() {
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

		//print out client request info
		System.out.println("Intermediate Host: Packet received:");
		System.out.println("From host: " + receiveSendPacket.getAddress());
		clientPort = receiveSendPacket.getPort();
		System.out.println("Host port: " + clientPort);
		int len = receiveSendPacket.getLength();
		System.out.println("Length: " + len);
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

		//print out data from socket to be relayed to the server
		System.out.println("Intermediate Host: Packet being sent to sever:");
		System.out.println("To host: " + receiveSendPacket.getAddress());
		System.out.println("Destination host port: " + receiveSendPacket.getPort());
		System.out.println("Length: " + len);
		System.out.print("Containing: ");
		System.out.println(new String(receiveSendPacket.getData(),0,len));

		//relay the socket to the server
		try {
			sendReceiveSocket.send(receiveSendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Intermediate Host: Packet sent to server.\n");

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

		//print out the received packet's information
		System.out.println("Intermediate Host: Response received:");
		System.out.println("From host: " + responsePacket.getAddress());
		System.out.println("Host port: " + responsePacket.getPort());
		len = responsePacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: ");

		//form a string from the byte array.
		String response = new String(data,0,len);   
		System.out.println(response + "\n");

		//set the response packet's port destination to that of the client's sendReceive socket
		responsePacket.setPort(clientPort);

		//print out response info to be relayed to the client
		System.out.println("Intermediate Host: Response being relayed to client:");
		System.out.println("To host: " + responsePacket.getAddress());
		System.out.println("Destination host port: " + responsePacket.getPort());
		len = responsePacket.getLength();
		System.out.println("Length: " + len);
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
		System.out.println("Response from server relayed to client.\n");

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

		//set the port for the packet to be that of the servers receive socket
		receiveSendPacket.setPort(69);
		System.out.println("Setting up invalid packet");

		String s = "Error Simulator";
		//print out data from socket to be relayed to the server
		packetManager.displayPacketInfo(receiveSendPacket, s, true);

		//relay the socket to the server
		try {
			sendReceiveSocket.send(receiveSendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Invalid Packet Sent");

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
	public static void createInvalidPacket(){
		DatagramPacket receiveSendPacket = receiveClientPacket();
		if (receiveSendPacket.getData()[1] == 1){// Read Request
			receiveSendPacket.getData()[1] = 8; //setting an invalid opcode
			System.out.println("Containing: "+ new String (receiveSendPacket.getData()));
			//receiveServerPacket();
		}else if (receiveSendPacket.getData()[1] == 2){// Write Request
			receiveSendPacket.getData()[1] = 8; //setting an invalid opcode
			System.out.println("Containing: "+ new String (receiveSendPacket.getData()));
			receiveServerPacket();
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

	public static void main( String args[] )
	{
		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
		ErrorSimulator h = new ErrorSimulator();

		System.out.println("Welcome to Error Simulator.");
		System.out.println("\nChoose your mode:");
		System.out.println("	(1) Error Code 4");
		System.out.println("	(2) Error Code 5");
		System.out.println("	(3) No error");
		
		for(int j = 0; j<11; j++) {
			String input = keyboard.next();
			if (input.equals("1")){
				System.out.println("\nYou have chosen error code 4, please select an error from below.");
				System.out.println("	(1)Invalid read request (RRQ)");
				System.out.println("	(2)Invalid write request (WRQ)");
				System.out.println("	(3)Client sends invalid DATA ");
				System.out.println("	(4)Client sends invalid ACK");
				System.out.println("	(5)Server sends invalid DATA");
				System.out.println("	(6)Server sends invalid ACK");
				System.out.println("	Please type X to exit");

				String input1 = keyboard.next();
				switch(input1){
					case "X":{
						break;
					} 
					case "1":{
						System.out.println("You selected Invalid RRQ\n");
						createInvalidPacket();
						break;
					}
					case "2":{
						System.out.println("You selected Invalid WRQ\n");
						createInvalidPacket();
						break;
	
					}case "3":{
						System.out.println("You selected Invalid DATA\n");
						createInvalidDataPacket();
						break;
	
					}case "4":{
						System.out.println("You selected Invalid ACK\n");
						createInvalidAckPacket();
						break;
	
				 	}case "5":{
						System.out.println("You chose Server sends invalid DATA\n");
						createInvalidServerDataPacket();
						break;
	
					}case "6":{
						System.out.println("You chose Server sends invalid ACK\n");
						createInvalidServerAckPacket();
						break;
					}
				}
			}else if(input.equals("3")){
				System.out.println("Error simulator not running..");
				h.receiveAndSend(); //receive and send requests/responses
			}
		}
	}

}