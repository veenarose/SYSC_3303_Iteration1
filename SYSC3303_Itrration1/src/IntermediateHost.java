import java.io.*;
import java.net.*;

//IntermediateHost.java
//This class is the intermediate host between the client and server
//for assignment 1.
//
//by: Damjan Markovic

public class IntermediateHost {

	DatagramSocket receiveSocket; //socket which receives requests from client on port 1024
	DatagramSocket sendReceiveSocket; //socket which sends and receives UDP packets from the server
	DatagramPacket sendPacket; //packet which relays request from client to server

	public IntermediateHost() {
		try {
			//construct a datagram socket and bind it to any available port on the local machine
			sendReceiveSocket = new DatagramSocket();
			//construct a datagram socket and bind it to port 1024 on the local machine
			receiveSocket = new DatagramSocket(1024);
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
		receiveSendPacket.setPort(1025);

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

	public static void main( String args[] )
	{
		IntermediateHost h = new IntermediateHost();
		for(int i = 0; i<11; i++) {
			h.receiveAndSend(); //receive and send requests/responses
		}
	}
}