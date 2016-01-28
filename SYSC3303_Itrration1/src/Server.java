import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

//Server.java
//This class is the server for assignment 1.
//
//by: Damjan Markovic

public class Server{

	DatagramSocket receiveSocket, sendSocket;
	PacketManager packerManager;
	IOManager ioManager;
	int readWrite;
	static boolean serverRuning = true;

	public Server(){
		packerManager = new PacketManager();
		ioManager = new IOManager();
		byte[] data = new byte[512];
		
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		
		try{
			receiveSocket = new DatagramSocket(4488);
			while(serverRuning){
				try {
					receiveSocket.receive(receivePacket);
					System.out.println("Packet recieved from client at " + getTimestamp());
					Response r = new Response(receivePacket);
				} catch(IOException e) {
					e.printStackTrace();         
					System.exit(1);
				}
			}
		} catch(SocketException e) {
			error("Main: Socket Exception");
			e.printStackTrace();
		}
	}

	public static void main( String args[] ){
		new Server();
	}

	/**
	 * Makes time stamp of current time as YYYY.MM.DD.HH.MM.SS
	 * @return String representation of time stamp
	 */
	public static String getTimestamp(){
		return new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
	}
	
	/**
	 * Convenience function to not have to type System.err.println();
	 * @param s String to be printed
	 */
	public void error(String s){
		System.err.println(s);
	}

	class Response extends Thread{
		protected DatagramSocket socket;
		protected DatagramPacket packet;
		protected DatagramSocket clientSocket;
		protected InetAddress clientAddr;
		protected int port;
		protected int packetType = -1;
		
		public void run(){
			int type = -1;
			try{
				type = packerManager.validateRequest(packet.getData());
			} catch(IOException e){
				error("Packet Type could not be verifed");
				e.printStackTrace();
			}
			
			if (type == 1){//READ request
				try{
					clientSocket.receive(packet);
				} catch(IOException e){
					error("RUN: IOException");
					e.printStackTrace();
				}
			} 
			else if(type == 2){ //WRITE request
				
			}
		}
		
		/**
		 * Takes a DatagramPacket as input. Opens a new Socket for communication with client
		 * Verifies the packet as a legitimate packet. If the packet is a legitimate packet the thread runs this.start();
		 * Otherwise the thread prints an error message and closes.
		 * @param p DatagramPacket
		 */
		public Response(DatagramPacket p){
			packet = p;
			clientAddr = p.getAddress();
			port = p.getPort();
			int validReq = -1;
			
			try {
				socket = new DatagramSocket();
				validReq = packerManager.validateRequest(p.getData());
			} catch (SocketException e){
				error("Socket Exception");
				e.printStackTrace();
			} catch (IOException e){
				error("IOException");
				e.printStackTrace();
			}
			
			if(validReq != -1){
				this.start();
			} else {
				error("Request type could not be verified. Thread exiting");
			}
		}
	}
}