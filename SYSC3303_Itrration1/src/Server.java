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
					r.start();
				} catch(IOException e) {
					e.printStackTrace();         
					System.exit(1);
				}
			}
		} catch(SocketException e) {
			System.err.println("Main: Socket Exception");
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
	
	public void run() {
		
	}

	class Response extends Thread{
		DatagramPacket packet;
		DatagramSocket clientSocket;
		InetAddress clientAddr;
		int port;
		
		
		public void run(){
			int type = -1;
			try{
				type = packerManager.validateRequest(packet.getData());
			} catch(IOException e){
				System.err.println("Packet Type could not be verifed");
				e.printStackTrace();
			}
			
			if (type == 1){//READ request
				try{
					clientSocket.receive(packet);
				} catch(IOException e){
					System.err.println("RUN: IOException");
					e.printStackTrace();
				}
			} 
			else if(type == 2){ //WRITE request
				
			}
		}
		
		public Response(DatagramPacket p){
			packet = p;
			clientAddr = p.getAddress();
			port = p.getPort();
		}
	}
}