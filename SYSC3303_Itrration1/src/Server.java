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
	PacketManager packetManager;
	IOManager ioManager;
	int readWrite;
	static boolean serverRuning = true;

	public Server(){
		packetManager = new PacketManager();
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
			
			try {
				socket = new DatagramSocket();
				packetType = packetManager.validateRequest(p.getData());
			} catch (SocketException e){
				error("Socket Exception");
				e.printStackTrace();
			} catch (IOException e){
				error("IOException");
				e.printStackTrace();
			}
			
			if(packetType != -1){
				this.start();
			} else {
				error("Request type could not be verified. Thread exiting");
			}
		}
		
		public void run(){
			
			if (packetType == 1){
				/* READ Request */
				try{
					handleReadReq();
				} catch(IOException e) {
					error("IOException on read request");
				}
			} 
			else if(packetType == 2){
				/* WRITE Request */
				
			} 
			else if(packetType == 3){
				/* DATA Request */
				
			}
			else if(packetType == 4){
				/* ACK Request */
				
			}
			else if(packetType == 5){
				/* ERROR packet */
				
			}
		}
		
		private void handleReadReq() throws IOException {
			/* Currently only handles NETASCII */
			byte[] data = new byte[512];
			int offs = 0;
			String filename = new String(PacketManager.getFilename(packet.getData()));
			
			do{
				data = ioManager.read(filename, offs);
				offs += data.length;
				
				packet = new DatagramPacket(packetManager.createData(data),
											packetManager.createData(data).length,
											clientAddr,
											port);
				socket.send(packet);
				
				socket.receive(packet);
			} while(packetManager.lastPacket(data));
		}
	}
}