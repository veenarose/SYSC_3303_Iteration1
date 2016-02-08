import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

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
					print("Packet recieved from client at " + getTimestamp());
					print("packet info " + new String(receivePacket.getData()));
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

	public void print(String s){
		System.out.println(s);
	}
	
	class Response extends Thread{
		private DatagramSocket socket;
		private final DatagramPacket packet;
		private DatagramSocket clientSocket;
		private InetAddress clientAddr;
		private int port;
		private int packetType = -1;


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

			print("p " + new String(packet.getData()));
			
			try {
				socket = new DatagramSocket();
				print("socket created");
				packetType = packetManager.validateRequest(p.getData());
				print("req validated");
			} catch (SocketException e){
				error("Socket Exception");
				e.printStackTrace();
			} catch (IOException e){
				error("IOException");
				e.printStackTrace();
			}

			if(packetType != 0){
				print("Packet type verified as " + packetType + ". Starting thread. " + getTimestamp());
				this.start();
			} else {
				error("Request type could not be verified. Thread exiting " + getTimestamp());
			}
		}

		public void run(){

			if (packetType == 1){
				/* READ Request */
				try{
					print("calling read handler");
					handleReadReq();
				} catch(IOException e) {
					error("IOException on read request");
				}
			} 
			else if(packetType == 2){
				/* WRITE Request */
				try{
					print("p2 "+ packet.getData().toString());
					handleWriteReq(packet);
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
		public void handleWriteReq(DatagramPacket p)throws IOException{
			print("1");
			//print("packet info " + new String(packet.getData()));
			print(p.getData().toString());
			byte[] data;
			short blockNum = 0;
			String filename = packetManager.getFilename(packet.getData());
			print("2");
			do{
				print("3");
				/* get data from packet */
				data = packet.getData();
				
				print("4");
				/* Convert server side block number from short to byte[] */
				ByteBuffer buffer = ByteBuffer.allocate(2);
				buffer.putShort(blockNum);
				byte[] block = buffer.array();
				
				print("5");
				/* create ACK Packet */
				byte[] ack = packetManager.createAck(block);
				
				print("6");
				/* Write data received from client to file */
				ioManager.write(filename, data);
				
				print("7");
				/* create and send ACK packet to client */
				DatagramPacket pp = new DatagramPacket(ack, ack.length, clientAddr, port);
				socket.send(pp);
				
				print("8");
				/* iterate server side blockNumber */
				blockNum++;
				
				print("9");
				/* wait for next packet */
				socket.receive(packet);
				
				print("10");
				/* get block number from client and convert to short */
				ByteBuffer b = ByteBuffer.allocate(2);
				b.put(packetManager.getBlockNum(packet.getData()));
				short bn = b.getShort(0);
				
				print("11");
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
			DatagramPacket p3= new DatagramPacket(ack, ack.length, clientAddr, port);
			socket.send(p3);
			
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
				print("data written to " + filename + " succesfully");
			}
		}
		
		private void handleReadReq() throws IOException {
			/* Currently assumes requests are NETASCII */
			byte[] data = new byte[512]; print(" 1 ");
			int offs = 0; print(" 2 ");
			short blockNum = 1; print(" 3 ");
			String filename = new String("test.txt"); print(" 4 ");

			
			print(" 5 ");
			/* Convert block number from short to byte[] */
			ByteBuffer buffer = ByteBuffer.allocate(2);
			buffer.putShort(blockNum);
			byte[] block = buffer.array();

			do{
				
				print(" 6 ");
				/* get data from file and configure the offset*/
				data = ioManager.read(filename, offs);
				offs += data.length;

				print(" 7 ");
				/* place data into packet to be sent to client */
				DatagramPacket p1 = new DatagramPacket(packetManager.createData(data, block),
											packetManager.createData(data, block).length,
											clientAddr,
											port);
				
				print(" 8 ");
				/* send packet to client */
				socket.send(p1);
				
				print(" 9 ");
				/* iterate server side block number */
				blockNum++;

				print(" 10 ");
				/* wait to receive ACK confirmation */
				socket.receive(packet);
				
				print(" 11 ");
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

			DatagramPacket p = new DatagramPacket(packetManager.createData(data, block),
										packetManager.createData(data, block).length,
										clientAddr,
										port);
			socket.send(p);
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
			print("File read Succesfuly");
		}
	}
	
}