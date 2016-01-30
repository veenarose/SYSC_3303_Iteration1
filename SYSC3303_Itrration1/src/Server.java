import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
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
			receiveSocket = new DatagramSocket(1024);
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

			if(packetType != 0){
				System.out.println("Packet type verified as " + packetType + ". Starting thread.");
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
				try{
					handleWriteReq();
				} catch(IOException e) {
					error("IOException on write request");
				}
			} 
		}

		/**
		 * Takes the DatagramPacket received for a write request and handles
		 * the operations necessary to finish the write request
		 * @throws IOException
		 */
		private void handleWriteReq()throws IOException{
			byte[] data;
			short blockNum = 0;
			String filename = packetManager.getFilename(packetManager.getData(packet.getData()));
			
			do{
				/* get data from packet */
				data = packetManager.getData(packet.getData());
				
				/* Convert server side block number from short to byte[] */
				ByteBuffer buffer = ByteBuffer.allocate(2);
				buffer.putShort(blockNum);
				byte[] block = buffer.array();
				
				/* create ACK Packet */
				byte[] ack = packetManager.createAck(block);
				
				/* Write data received from client to file */
				ioManager.write(filename, data);
				
				/* create and send ACK packet to client */
				packet = new DatagramPacket(ack, ack.length, clientAddr, port);
				socket.send(packet);
				
				/* iterate server side blockNumber */
				blockNum++;
				
				/* wait for next packet */
				socket.receive(packet);
				
				/* get block number from client and convert to short */
				ByteBuffer b = ByteBuffer.allocate(2);
				b.put(packetManager.getBlockNum(packet.getData()));
				short bn = b.getShort(0);
				
				if(bn != blockNum){
					/* if client block number and server block number do not match */
					error("HandleWriteRequest: Block numbers between client and server do not match");
					break;
				}
				
			} while(!(packetManager.lastPacket(data)));
			
			/* exit loop for last packet */
			data = packetManager.getData(packet.getData());
			
			/* Convert server side block number from short to byte[] */
			ByteBuffer buffer = ByteBuffer.allocate(2);
			buffer.putShort(blockNum);
			byte[] block = buffer.array();
			
			/* create ACK Packet */
			byte[] ack = packetManager.createAck(block);
			
			/* Write data received from client to file */
			ioManager.write(filename, data);
			
			/* create and send ACK packet to client */
			packet = new DatagramPacket(ack, ack.length, clientAddr, port);
			socket.send(packet);
			
			/* wait for next packet */
			socket.receive(packet);
			
			/* get block number from client and convert to short */
			ByteBuffer b = ByteBuffer.allocate(2);
			b.put(packetManager.getBlockNum(packet.getData()));
			short bn = b.getShort(0);
			
			if(bn != blockNum){
				/* if client block number and server block number do not match */
				error("HandleWriteRequest: Block numbers between client and server do not match");
			} else {
				System.out.println("data written to " + filename + " succesfully");
			}
		}
		
		private void handleReadReq() throws IOException {
			/* Currently assumes requests are NETASCII */
			byte[] data = new byte[512];
			int offs = 0;
			short blockNum = 1;
			String filename = new String(PacketManager.getFilename(packet.getData()));

			/* Convert block number from short to byte[] */
			ByteBuffer buffer = ByteBuffer.allocate(2);
			buffer.putShort(blockNum);
			byte[] block = buffer.array();

			do{
				/* get data from file and configure the offset*/
				data = ioManager.read(filename, offs);
				offs += data.length;

				/* place data into packet to be sent to client */
				packet = new DatagramPacket(packetManager.createData(data, block),
											packetManager.createData(data, block).length,
											clientAddr,
											port);
				
				/* send packet to client */
				socket.send(packet);
				
				/* iterate server side block number */
				blockNum++;

				/* wait to receive ACK confirmation */
				socket.receive(packet);
				
				/* confirm validity of ACK */
				if(packetManager.isAckPacket(packet.getData())){
					byte[] ackData = packet.getData();
					ByteBuffer b = ByteBuffer.allocate(2);
					b.put(ackData[3]);
					b.put(ackData[4]);

					short bn = b.getShort(0);

					if(bn != blockNum){
						error("ACK packet Block Number does not match current block number");
						break;
					}
				}
				else if(packetManager.validateRequest(packet.getData()) == 5){
					/* ERROR packet received */
					//TODO handle error packets
					break;
				} else {
					error("HandleReadReq: incoming packet could not be verified as ACK or ERR");
					break;
				}
			} while(!(packetManager.lastPacket(data)));

			/* exit loop for last packet */
			data = ioManager.read(filename, offs);
			offs += data.length;

			packet = new DatagramPacket(packetManager.createData(data, block),
										packetManager.createData(data, block).length,
										clientAddr,
										port);
			socket.send(packet);
			blockNum++;

			socket.receive(packet);
			if(packetManager.isAckPacket(packet.getData())){
				/* ACK Packet Received */
				byte[] ackData = packet.getData();
				ByteBuffer b = ByteBuffer.allocate(2);
				b.put(ackData[3]);
				b.put(ackData[4]);

				short bn = b.getShort(0);

				if(bn != blockNum){
					error("ACK packet Block Number does not match current block number");
				}
			}
			System.out.println("File read Succesfuly");
		}
	}
	
}