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

	private DatagramSocket receiveSocket;
	private PacketManager packetManager;
	private IOManager ioManager;
	private static boolean serverRuning = true;
	private ProfileData pd = new ProfileData();
	private final byte[] shutdown = {1,1};
	private final static String ServerDirectory =  
			(System.getProperty("user.dir") + "/src/ServerData/");//the directory path of the server files
	private final static String[] files = 
		{"originServer1.txt", "originServer2.txt"}; //names of files local to the client
	//private Set<String> fileNames = new HashSet<String>(); //java set to store file names

	/*
	 * Server constructor. Creates new PacketManager and IOManager classes to 
	 * handle UDP packets and writing to files respectively. Receive client requests
	 * and pass them onto to new response handler threads.
	 */
	public Server(){
		System.out.println("Server Started...");
		packetManager = new PacketManager();
		ioManager = new IOManager();
		byte[] data = new byte[512];
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		printAllFolderContent();

		try{
			receiveSocket = new DatagramSocket(pd.getServerPort()); //socket listening on port 1025
			while(serverRuning){
				try {
					receiveSocket.receive(receivePacket);//receive the request from the client
					if(receivePacket.getData() == shutdown){
						stopServer();
					}

					print("Packet recieved from client at " + getTimestamp());

					Response r = new Response(receivePacket); //dispatch new thread to handle the response, pass to it the request packet
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

	private void stopServer(){
		serverRuning = false;
		receiveSocket.close();
		print("Server shutting down");
	}
	
	/*
	 * Prints all the file names from the specified path.
	 */
	public void printAllFolderContent(){
		File folder = new File(ServerDirectory);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("File " +(i+1) +": "+ listOfFiles[i].getName());
			} else if (listOfFiles[i].isDirectory()) {
				System.out.println("Directory " + listOfFiles[i].getName());
			}
		}
	}
	
	public static void main( String args[] ){
		new Server(); //instantiate the server
	}

	/**
	 * Makes time stamp of current time as YYYY.MM.DD.HH.MM.SS
	 * @return String representation of time stamp
	 */
	public String getTimestamp(){
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

	/*
	 * Response class. Extends thread, handles requests coming into the server
	 * and generates an appropriate response. Each Response class continues
	 * communication with a client
	 *
	 */
	class Response extends Thread{

		protected DatagramSocket socket;
		protected DatagramPacket packet;
		protected DatagramSocket clientSocket;
		protected InetAddress clientAddr;
		protected int clientPort;
		protected int packetType = -1;
		protected String mode;
		protected byte[] contents;
		protected String filename = "";

		/**
		 * Takes a DatagramPacket as input. Opens a new Socket for communication with client
		 * Verifies the packet as a legitimate packet. If the packet is a legitimate packet the thread runs this.start();
		 * Otherwise the thread prints an error message and closes.
		 * @param p DatagramPacket
		 */
		public Response(DatagramPacket p){
			packet = p;
			clientAddr = p.getAddress();
			clientPort = p.getPort();
			contents = p.getData();

			try {
				socket = new DatagramSocket();

				//Error handling data collection
				packetType = packetManager.validateRequest(contents);
				mode = packetManager.getMode(contents);
				filename = packetManager.getFilename(contents);

			} catch (SocketException e){
				error("Socket Exception");
				e.printStackTrace();
			} catch (IOException e){
				error("IOException");
				e.printStackTrace();
			}

			if(packetType != 0){
				System.out.println("packet "+new String(contents));
				print("Packet type verified as " + packetType + ". Starting thread. " + getTimestamp());
				this.start();
			} else {
				error("Request type could not be verified. Thread exiting " + getTimestamp());
			}
		}

		public void run(){
			if (!mode.equals("octet") && !mode.equals("netascii")){
				handleErrReq(0 ,clientAddr, clientPort);
			}

			if (filename == ""){
				handleErrReq(1 ,clientAddr, clientPort);
			}

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
					handleWriteReq();
				} catch(IOException e) {
					error("IOException on write request");
				}
			} 
			else if(packetType == 100) {
				//no termination after 0
				handleErrReq(3 ,clientAddr, clientPort);
			} 
			else if(packetType == 200){
				//Final byte is not 0
				handleErrReq(4 ,clientAddr, clientPort);
			} else {
				// Not read or write request do nothing
				handleErrReq(2 ,clientAddr, clientPort);
			}
		}

		/**
		 * Takes the DatagramPacket received for a write request and handles
		 * the operations necessary to finish the write request
		 * @throws IOException
		 */

		private void handleWriteReq()throws IOException{
			print("1");
			byte[] inboundDatapacket = new byte[ioManager.getBufferSize() + 4];
			byte[] rawData; 
			int blockNum = 1;
			String filename = packetManager.getFilename(contents); 
			print("4");
			print("Attempting to read from file: " + filename);

			File writeTo = new File(ServerDirectory + filename);
			print("writeTo exists?: " +  writeTo.exists());

			byte[] ack = packetManager.createAck(new byte[]{0,0,0,0});

			do{
				rawData = new byte[ioManager.getBufferSize()];

				/* create and send ACK packet to client */
				packet = new DatagramPacket(ack, ack.length, clientAddr, clientPort);
				socket.send(packet);

				print("9");
				/*  for next packet */
				packet = new DatagramPacket(inboundDatapacket, inboundDatapacket.length);
				socket.receive(packet);



				//If Packet arrives from an unknown TID
				if(verifyClient(packet) == false){
					handleErrReq(5 , packet.getAddress(), packet.getPort());
					continue; //go back and wait for client packet
				}

				blockNum++;
				ack = packetManager.createAck(inboundDatapacket);

				rawData = packetManager.getData(inboundDatapacket);
				//packetManager.printTFTPPacketData(inboundDatapacket);
				//packetManager.printTFTPPacketData(rawData);
				ioManager.write(writeTo, rawData);

			} while(!(packetManager.lastPacket(rawData)));

			/* create and send ACK packet to client */
			packet = new DatagramPacket(ack, ack.length, clientAddr, clientPort);
			socket.send(packet);
			blockNum++;

			print("10");
			/* wait for next packet */
			packet = new DatagramPacket(inboundDatapacket, inboundDatapacket.length);
			socket.receive(packet);

			ack = packetManager.createAck(inboundDatapacket);

			rawData = packetManager.getData(inboundDatapacket);
			ioManager.write(writeTo, rawData);

			print("write complete!!!");
		}

		private void handleReadReq() throws IOException {
			byte[] data; 
			short blockNum = 1; print(" 3 ");
			String filename = packetManager.getFilename(contents); print(" 4 ");
			print("Attempting to read from file: " + filename);

			BufferedInputStream reader = new BufferedInputStream
					(new FileInputStream(ServerDirectory + filename));

			print(" 5 ");
			/* Convert block number from short to byte[] */
			ByteBuffer buffer = ByteBuffer.allocate(2);
			buffer.putShort(blockNum);
			byte[] block = buffer.array();

			do{
				data = new byte[512];
				print(" 6 ");
				/* get data from file and configure the offset*/

				reader.read(data, 0, data.length);

				print(" 7 ");
				/* place data into packet to be sent to client */
				packet = new DatagramPacket(packetManager.createData(data, block),
						packetManager.createData(data, block).length,
						clientAddr,
						clientPort);

				print(" 8 ");
				/* send packet to client */
				socket.send(packet);

				print(" 9 ");
				/* iterate server side block number */
				blockNum++;

				print(" 10 ");
				/* wait to receive ACK confirmation */
				socket.receive(packet);

				if(verifyClient(packet) == false){
					handleErrReq(5, packet.getAddress(), packet.getPort());
				}

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
				} else {
					handleErrReq(6, clientAddr, clientPort);
				}
			} while(!(packetManager.lastPacket(data)));

			/* exit loop for last packet */
			reader.read(data, 0, data.length);
			reader.close();
			packet = new DatagramPacket(packetManager.createData(data, block),
					packetManager.createData(data, block).length,
					clientAddr,
					clientPort);
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
			} else {
				handleErrReq(6, clientAddr, clientPort);
			}
			print("File read Succesfuly");
		}

		/**
		 * Sends an Error Packet to the given Address
		 * @param i	Error message ID
		 * @param addr Address to send the error
		 * @param port Port
		 */
		private void handleErrReq(int i, InetAddress addr, int port){
			byte[] errCode = {0,4};
			String[] errMsg = {	"Invalid Mode", 
					"Filename not found", 
					"Invalid Request Type", 
					"Packet message continues after terminating 0",
					"Packet did not terminate with 0",
					"Error has occoured",
					"Invalid ACK recieved"
			};

			byte[] err = packetManager.createError(errCode, errMsg[i]);
			packet = new DatagramPacket(err,err.length,addr,port);

			print(getTimestamp() + ": " + errMsg[i] + " thread exiting");
			try {
				socket.send(packet);
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		/**
		 * Verifies that that client is the original client for which the thread was made
		 * @param p Packet received thats is to be verified
		 * @return boolean
		 */
		private boolean verifyClient(DatagramPacket p){
			if(p.getAddress() == clientAddr && p.getPort() == clientPort){
				return true;
			}
			return false;
		}

	}
}