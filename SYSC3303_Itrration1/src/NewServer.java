import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewServer {
	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;

	private PacketManager packetManager;
	private IOManager ioManager;
	private ProfileData pd;

	private boolean serverRunning = true;

	private final byte[] shutdown = {1,1};
	private final static String ServerDirectory = (System.getProperty("user.dir") + "/src/ServerData/");
	private final static String[] files = {"originServer1.txt", "originServer2.txt"};

	public static void main( String args[] ){
		new NewServer();
	}

	public NewServer(){
		packetManager = new PacketManager();
		ioManager = new IOManager();
		pd = new ProfileData();
		connect();
	}

	public void connect(){
		byte[] data = new byte[ioManager.getBufferSize()+4];
		receivePacket = new DatagramPacket(data, data.length);
		printAllFolderContent();
		System.out.println("\nWaiting for packet..");

		try{
			receiveSocket = new DatagramSocket(pd.getServerPort()); //socket listening on port 1025
			while(serverRunning){
				try {
					receiveSocket.receive(receivePacket);//receive the request from the client
					if(receivePacket.getData() == shutdown){
						break;
					}

					System.out.println(getTimestamp() +": Packet received from client");		
					new Response(receivePacket); //dispatch new thread to handle the response, pass to it the request packet

				} catch(IOException e) {
					e.printStackTrace();         
					System.exit(1);
				}
			}
		} catch(SocketException e) {
			System.err.println("Socket Exception");
			e.printStackTrace();
		}
	}

	/**
	 * Makes time stamp of current time as YYYY.MM.DD.HH.MM.SS
	 * @return String representation of time stamp
	 */
	public String getTimestamp(){
		return new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
	}


	/**
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

	class Response extends Thread{

		protected DatagramSocket socket;
		protected DatagramPacket packet;

		protected InetAddress clientAddr;
		protected int clientPort;

		protected String mode = "";
		protected String filename = "";
		protected int reqType = 0;

		public Response(DatagramPacket p){

			clientAddr = p.getAddress();
			clientPort = p.getPort();

			try { //init socket
				socket = new DatagramSocket();
				socket.setSoTimeout(1000);
			} catch (SocketException e1) {
				e1.printStackTrace();
			}

			//if packet is validated and all fields are extracted properly then start
			if(extractPacketData(p.getData())){
				this.start();
			} 
		}

		/**
		 * Extracts data from the packet and places them in the correct fields
		 * @param data packet.getdata()
		 * @return boolean if packet is valid and all fields are correctly filled
		 */
		public boolean extractPacketData(byte[] data){

			try{ //validate request packet
				reqType = PacketManager.validateRequest(data);

			} catch(TFTPExceptions.InvalidTFTPRequestException e){
				//Send error packet to client if invalid packet
				byte[] IllegalTFTPOp = {0,4};
				byte[] err = PacketManager.createError(IllegalTFTPOp, "Invalid TFTP Request");

				try { //send packet
					socket.send(new DatagramPacket(err, err.length, clientAddr, clientPort));
				} catch (IOException e1) {
					System.err.println("Error packet not sent");
					e1.printStackTrace();
				}

				return false;
			}
			//extract additional fields 
			mode = PacketManager.getMode(data);
			filename = PacketManager.getFilename(data);

			//check if all data is valid
			if(reqType == 1 || reqType == 2){
				if(mode.toLowerCase().equals("octet") || mode.toLowerCase().equals("netascii")){
					if(filename != ""){
						return true;
					} else {
						//if filename error
						byte[] fnf = {0,1};
						byte[] err = PacketManager.createError(fnf, "File not found");

						try { //send packet
							socket.send(new DatagramPacket(err, err.length, clientAddr, clientPort));
						} catch (IOException e1) {
							System.err.println("Error packet not sent");
							e1.printStackTrace();
						}
					}
				} else {
					//if invalid mode
					byte[] invalidmode = {0,1};
					byte[] err = PacketManager.createError(invalidmode, "Invalid mode");
					try{ //send packet
						socket.send(new DatagramPacket(err, err.length, clientAddr, clientPort));
					} catch (IOException e1) {
						System.err.println("Error packet not sent");
						e1.printStackTrace();
					}
				}
			} else {
				//if invalid request
				byte[] invalidreq = {0,0};
				byte[] err = PacketManager.createError(invalidreq, "Invalid mode");
				try{ //send packet
					socket.send(new DatagramPacket(err, err.length, clientAddr, clientPort));
				} catch (IOException e1) {
					System.err.println("Error packet not sent");
					e1.printStackTrace();
				}
			}
			return false;
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

		public void run(){
			switch (reqType){
			case 1: 
				handleReadReq();
				break;
			case 2:
				handleWriteReq();
				break;
			default:
				break;
			}
		}

		public void handleReadReq(){
			byte[] data = new byte[ioManager.getBufferSize()+4];
			byte[] ack = new byte[4];
			int blockNum = 1;
			File f = new File(ServerDirectory + filename);
			BufferedInputStream reader = null;
			DatagramPacket recievedPacket = new DatagramPacket(ack, ack.length);
			int retryAttempts = 0;

			if(!f.exists() || !f.isDirectory()) { 
				//if file does not exist send error packet
				data = PacketManager.createError(new byte[]{0,1}, "File not found");
				try {
					socket.send(new DatagramPacket(data, data.length, clientAddr, clientPort));
				} catch (IOException e) {
					e.printStackTrace();
				}
				//end communication
				return;
			}

			try {
				reader = new BufferedInputStream(new FileInputStream(f));
			} catch (FileNotFoundException e) {
				System.err.println("File not found");
				e.printStackTrace();
			}

			//If file does exist send contents
			do {
				retryAttempts = 0;

				//Read data from file
				data = new byte[ioManager.getBufferSize()+4];
				try {
					System.out.println("Reading from the file for block " + blockNum);
					reader.read(data, 0, data.length);
				} catch (IOException e) {
					e.printStackTrace();
				}
				//create data block using PacketManager
				byte[] blocknumber = PacketManager.intToBytes(blockNum);
				byte[] datapacket = PacketManager.createData(data, blocknumber);

				//form packet to be sent
				packet = new DatagramPacket(datapacket, 
						datapacket.length, clientAddr, clientPort);

				//Only try to send packet 5 times in case of timeouts
				//if 5 timeouts occur then send error and terminate connection
				while(retryAttempts < 5){

					try { //send packet
						socket.send(packet);
					} catch (IOException e) {
						e.printStackTrace();
					}

					try { //wait for ACK
						socket.receive(recievedPacket);

						//once we receive an ACK verify if client is the same
						if(!verifyClient(recievedPacket)){
							//if client is different send error and listen again
							DatagramPacket x = PacketManager.createInvalidTIDErrorPacket(clientPort, recievedPacket.getPort());
							x.setAddress(recievedPacket.getAddress());
							x.setPort(recievedPacket.getPort());

							try {
								socket.send(x);
							} catch (IOException e) {
								e.printStackTrace();
							}
							continue;
						} else {
							//if client is the same break out of loop
							break;
						}

					} catch (SocketTimeoutException e) {
						retryAttempts++;

						//if we have failed to get an ACK 5 times
						if(retryAttempts >= 5){
							data = PacketManager.createError(new byte[]{0,0}, "No ACK recieved");
							packet = new DatagramPacket(data, data.length, clientAddr, clientPort);

							try { //send error packet to client
								socket.send(packet);
							} catch (IOException e3) {
								e3.printStackTrace();
							}
							//end connection
							return;
						} else {
							//If no ACK arrives before timeout and we have not yet attempted 5 times
							System.out.println("No ACK recieved trying to send packet again");
							continue;
						}
					} catch(IOException e){
						e.printStackTrace();
					}
				}

				//Once ACK received from correct client
				if(PacketManager.isAckPacket(recievedPacket.getData())){

					//if valid ACK, verify block number
					if(PacketManager.getBlockNum(recievedPacket.getData())== blockNum){
						//if valid ACK and block numbers matches
						blockNum++;
					} else {
						//If Block number has error, send error to client and end connection
						DatagramPacket x = PacketManager.createInvalidBlockErrorPacket(blockNum,
								PacketManager.getBlockNum(recievedPacket.getData()));
						x.setAddress(recievedPacket.getAddress());
						x.setPort(recievedPacket.getPort());

						try {
							socket.send(x);
						} catch (IOException e) {
							e.printStackTrace();
						}

						//end connection
						return;
					}
				} else {
					//if ACK error, send error to client and end connection
					DatagramPacket x = PacketManager.createInvalidAckErrorPacket(recievedPacket.getData());
					x.setAddress(recievedPacket.getAddress());
					x.setPort(recievedPacket.getPort());

					try {
						socket.send(x);
					} catch (IOException e) {
						e.printStackTrace();
					}

					//end connection
					return;
				}
			} while(!(PacketManager.lastPacket(data)));

			//exit loop for last data block
			retryAttempts = 0;

			//Read data from file
			data = new byte[ioManager.getBufferSize()+4];
			try {
				System.out.println("Reading from the file for block " + blockNum);
				reader.read(data, 0, data.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//create data block using PacketManager
			byte[] blocknumber = PacketManager.intToBytes(blockNum);
			byte[] datapacket = PacketManager.createData(data, blocknumber);

			//form packet to be sent
			packet = new DatagramPacket(datapacket, 
					datapacket.length, clientAddr, clientPort);

			//Only try to send packet 5 times in case of timeouts
			//if 5 timeouts occur then send error and terminate connection
			while(retryAttempts < 5){

				try { //send packet
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}

				try { //wait for ACK
					socket.receive(recievedPacket);

					//once we receive an ACK verify if client is the same
					if(!verifyClient(recievedPacket)){
						//if client is different send error and listen again
						DatagramPacket x = PacketManager.createInvalidTIDErrorPacket(clientPort, recievedPacket.getPort());
						x.setAddress(recievedPacket.getAddress());
						x.setPort(recievedPacket.getPort());

						try {
							socket.send(x);
						} catch (IOException e) {
							e.printStackTrace();
						}
						continue;
					} else {
						//if client is the same break out of loop
						break;
					}

				} catch (SocketTimeoutException e) {
					retryAttempts++;

					//if we have failed to get an ACK 5 times
					if(retryAttempts >= 5){
						data = PacketManager.createError(new byte[]{0,0}, "No ACK recieved");
						packet = new DatagramPacket(data, data.length, clientAddr, clientPort);

						try { //send error packet to client
							socket.send(packet);
						} catch (IOException e3) {
							e3.printStackTrace();
						}
						//end connection
						return;
					} else {
						//If no ACK arrives before timeout and we have not yet attempted 5 times
						System.out.println("No ACK recieved trying to send packet again");
						continue;
					}
				} catch(IOException e){
					e.printStackTrace();
				}
			}

			//Once ACK received from correct client
			if(PacketManager.isAckPacket(recievedPacket.getData())){

				//if valid ACK, verify block number
				if(PacketManager.getBlockNum(recievedPacket.getData())== blockNum){
					//if valid ACK and block numbers matches
					blockNum++;
				} else {
					//If Block number has error, send error to client and end connection
					DatagramPacket x = PacketManager.createInvalidBlockErrorPacket(blockNum,
							PacketManager.getBlockNum(recievedPacket.getData()));
					x.setAddress(recievedPacket.getAddress());
					x.setPort(recievedPacket.getPort());

					try {
						socket.send(x);
					} catch (IOException e) {
						e.printStackTrace();
					}

					//end connection
					return;
				}
			} else {
				//if ACK error, send error to client and end connection
				DatagramPacket x = PacketManager.createInvalidAckErrorPacket(recievedPacket.getData());
				x.setAddress(recievedPacket.getAddress());
				x.setPort(recievedPacket.getPort());

				try {
					socket.send(x);
				} catch (IOException e) {
					e.printStackTrace();
				}

				//end connection
				return;
			}

		}

		public void handleWriteReq(){
			byte[] data = new byte[ioManager.getBufferSize()+4];
			byte[] ack = new byte[4];
			int blockNum = 0;
			File f = new File(ServerDirectory + filename);
			DatagramPacket recievedPacket = new DatagramPacket(data, data.length);
			int retryAttempts = 0;

			if(f.exists() || f.isDirectory()) { 
				//if file name exists send error packet
				data = PacketManager.createError(new byte[]{0,6}, "Filename used");
				try {
					socket.send(new DatagramPacket(data, data.length, clientAddr, clientPort));
				} catch (IOException e) {
					e.printStackTrace();
				}
				//end communication
				return;
			}
			
			//send ACK 0 packet
			byte[] block = PacketManager.intToBytes(blockNum);
			byte[] a = PacketManager.createAck(block);
			
			DatagramPacket ackpacket = new DatagramPacket(a, a.length, clientAddr, clientPort);
			
			try{
				socket.send(ackpacket);
			} catch(IOException e){
				e.printStackTrace();
			}
			
			do{
				try{
					socket.receive(recievedPacket);
				} catch (SocketTimeoutException e){
					//if no packet received before timeout
					data = PacketManager.createError(new byte[]{0,0}, "No data recieved");
					try {
						socket.send(new DatagramPacket(data, data.length, clientAddr, clientPort));
					} catch (IOException e2) {
						e2.printStackTrace();
					}
					//end communication
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if(!verifyClient(recievedPacket)){
					//if client is different send error and listen again
					DatagramPacket x = PacketManager.createInvalidTIDErrorPacket(clientPort, recievedPacket.getPort());
					x.setAddress(recievedPacket.getAddress());
					x.setPort(recievedPacket.getPort());

					try {
						socket.send(x);
					} catch (IOException e) {
						e.printStackTrace();
					}
					continue;
				}

				if(PacketManager.isErrorPacket(recievedPacket.getData())){
					//if we receive an error packet
					String x = PacketManager.extractMessageFromErrorPacket(recievedPacket.getData());
					System.out.println(getTimestamp() + " : Error packet received from client");
					System.out.println(x);
					//end connection
					return;
				}

				if(packetManager.isDataPacket(recievedPacket.getData())){
					if(PacketManager.getBlockNum(recievedPacket.getData())==blockNum+1){
						//if valid data packet and block numbers match
						byte[] dataToWrite = PacketManager.getData(recievedPacket.getData());
						
						try { //write to file
							ioManager.write(f, dataToWrite);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						//send ack packet
						byte[] byteblock = PacketManager.intToBytes(blockNum);
						byte[] q = PacketManager.createAck(byteblock);
						
						DatagramPacket ap = new DatagramPacket(q, q.length, clientAddr, clientPort);
						
						try{
							socket.send(ap);
						} catch(IOException e){
							e.printStackTrace();
						}
						
						//increment block number
						blockNum++;
						
					} else {
						//If Block number has error, send error to client and end connection
						DatagramPacket x = PacketManager.createInvalidBlockErrorPacket(blockNum,
								PacketManager.getBlockNum(recievedPacket.getData()));
						x.setAddress(recievedPacket.getAddress());
						x.setPort(recievedPacket.getPort());

						try {
							socket.send(x);
						} catch (IOException e) {
							e.printStackTrace();
						}

						//end connection
						return;
					}
				} else {
					//if data packet is invalid
					DatagramPacket x = PacketManager.createInvalidDataErrorPacket(recievedPacket.getData());
					x.setAddress(recievedPacket.getAddress());
					x.setPort(recievedPacket.getPort());

					try {
						socket.send(x);
					} catch (IOException e) {
						e.printStackTrace();
					}

					//end connection
					return;
				}
				
			} while(!(packetManager.lastPacket(data)));


		}
	}
}
