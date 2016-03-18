import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


//Server.java
//This class is the server for assignment 1.
//
//by: Damjan Markovic

public class Server{

	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	private PacketManager packetManager;
	private IOManager ioManager;
	private Response rep;
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
		System.out.println("Server started.");
		packetManager = new PacketManager();
		ioManager = new IOManager();
		byte[] data = new byte[ioManager.getBufferSize()+4];
		receivePacket = new DatagramPacket(data, data.length);
		printAllFolderContent();
		System.out.println("\nWaiting for packet..");

		try{
			receiveSocket = new DatagramSocket(pd.getServerPort()); //socket listening on port 1025
			while(serverRuning){
				try {
					receiveSocket.receive(receivePacket);//receive the request from the client
					if(receivePacket.getData() == shutdown){
						stopServer();
					}

					print("Packet received from client at " + getTimestamp());

					rep = new Response(receivePacket); //dispatch new thread to handle the response, pass to it the request packet
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

	/*
	 * Prints all the file names from the specified path.
	 */
	public void printAllFolderContent(){
		System.out.println("Server file contents");
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

	private void stopServer(){
		serverRuning = false;
		receiveSocket.close();
		print("Server shutting down");
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
		 * @throws InvalidTFTPRequestException 
		 */
		public Response(DatagramPacket p){
			packet = p;
			clientAddr = p.getAddress();
			clientPort = p.getPort();
			contents = p.getData();


			try {
				socket = new DatagramSocket();

				//Error handling data collection
				try {
					packetType = PacketManager.validateRequest(contents);
				} catch (TFTPExceptions.InvalidTFTPRequestException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mode = PacketManager.getMode(contents);
				filename = PacketManager.getFilename(contents);

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

			if (filename.isEmpty()){
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
			int blockNum = 0;
			String filename = PacketManager.getFilename(contents); 
			print("4");
			print("Attempting to write to file: " + filename);

			File writeTo = new File(ServerDirectory + filename);
			print("writeTo exists?: " +  writeTo.exists());

			byte[] ack = PacketManager.createAck(new byte[]{0,0,0,0});

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
				byte[] err = PacketManager.createError(new byte[]{0, 5}, "Unknown PID.");
				while(packet.getPort() != clientPort) {
					packet = new DatagramPacket(err, err.length,
							packet.getAddress(), packet.getPort());
					socket.send(packet);

					/* TODO:
					 * NEED TO IMPLEMENT: TIMEOUT ON RECEIVING 
					 */
					packet = new DatagramPacket(inboundDatapacket, inboundDatapacket.length);
					socket.receive(packet);
				}

				blockNum++;
				ack = PacketManager.createAck(inboundDatapacket);

				rawData = PacketManager.getData(inboundDatapacket);
				//packetManager.printTFTPPacketData(inboundDatapacket);
				//packetManager.printTFTPPacketData(rawData);
				ioManager.write(writeTo, rawData);

			} while(!(PacketManager.lastPacket(rawData)));

			/* create and send ACK packet to client */
			packet = new DatagramPacket(ack, ack.length, clientAddr, clientPort);
			socket.send(packet);
			blockNum++;

			/* wait for next packet */
			packet = new DatagramPacket(inboundDatapacket, inboundDatapacket.length);
			socket.receive(packet);

			ack = PacketManager.createAck(inboundDatapacket);

			rawData = PacketManager.getData(inboundDatapacket);
			ioManager.write(writeTo, rawData);

			print("write complete!!!");
		}

		private void handleReadReq() throws IOException {
			byte[] data = new byte[ioManager.getBufferSize()+4]; 
			short blockNum = 1;
			String filename = PacketManager.getFilename(contents);
			print("Attempting to read from file: " + filename);
			byte[] ackData = PacketManager.createAck(data);

			BufferedInputStream reader = new BufferedInputStream
					(new FileInputStream(ServerDirectory + filename));

			byte[] block;
			ByteBuffer buffer;

			do{

				System.out.println(blockNum);
				/* Convert block number from short to byte[] */
				buffer = ByteBuffer.allocate(2);
				buffer.putShort(blockNum);
				block = buffer.array();
				System.out.println("The block numer as an array " + 
						Arrays.toString(block));

				data = new byte[ioManager.getBufferSize()+4];
				/* get data from file and configure the offset*/

				reader.read(data, 0, data.length);
				System.out.println("Read from the file");

				/* place data into packet to be sent to client */
				packet = new DatagramPacket(PacketManager.createData(data, block),
						PacketManager.createData(data, block).length,
						clientAddr,
						clientPort);


				/* send packet to client */
				socket.send(packet);
				System.out.println("Sent DATA packet with block number = " 
						+ PacketManager.twoBytesToInt(packet.getData()));


				/* wait to receive ACK confirmation */
				DatagramPacket simPacket = new DatagramPacket(ackData, ackData.length,packet.getAddress(),packet.getPort());
				socket.receive(simPacket);
				System.out.println("Received ACK packet with block number = " 
						+ PacketManager.twoBytesToInt(simPacket.getData()));

				byte[] err = PacketManager.createError(new byte[]{0, 5}, "Unknown PID.");
				while(packet.getPort() != clientPort) {
					packet = new DatagramPacket(err, err.length,
							packet.getAddress(), packet.getPort());
					socket.send(packet);

					/* TODO:
					 * NEED TO IMPLEMENT: TIMEOUT ON RECEIVING 
					 */
					packet = new DatagramPacket(ackData, ackData.length);
					socket.receive(packet);
				}

				/* confirm validity of ACK */
				System.out.println(Arrays.toString(simPacket.getData()));
				if(PacketManager.isAckPacket(simPacket.getData())){
					ByteBuffer b = ByteBuffer.allocate(2);
					b.put(ackData[2]);
					b.put(ackData[3]);

					short bn = b.getShort(0);

					if(bn != blockNum){
						error("ACK packet Block Number does not match current block number");
						break;
					}
				} else {
					handleErrReq(6, clientAddr, clientPort);
				}

				/* iterate server side block number */
				blockNum++;

			} while(!(PacketManager.lastPacket(data)));

			/* Convert block number from short to byte[] */
			buffer = ByteBuffer.allocate(2);
			buffer.putShort(blockNum);
			block = buffer.array();

			/* exit loop for last packet */
			reader.read(data, 0, data.length);
			reader.close();
			packet = new DatagramPacket(PacketManager.createData(data, block),
					PacketManager.createData(data, block).length,
					clientAddr,
					clientPort);
			socket.send(packet);

			socket.receive(packet);
			if(PacketManager.isAckPacket(packet.getData())){
				/* ACK Packet Received */
				ackData = packet.getData();
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

			byte[] err = PacketManager.createError(errCode, errMsg[i]);
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