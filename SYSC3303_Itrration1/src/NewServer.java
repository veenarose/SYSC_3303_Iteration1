import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class NewServer implements Runnable{

	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;

	private final static String ServerDirectory =  
			(System.getProperty("user.dir") + "/src/ServerData/");

	private Set<String> fileNames; //java set to store file names

	private final static int timeout = ProfileData.getTimeOut(); //milliseconds
	private static final int bufferSize = IOManager.getBufferSize();
	private static final int ackSize = 4;
	private static final int dataSize = bufferSize + ackSize;

	public static boolean isRunning = true;

	public NewServer(int lp) {
		try {
			receiveSocket = new DatagramSocket(lp);
			System.out.println(receiveSocket.getInetAddress());
			populateFileNames();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void populateFileNames() {
		//populate fileNames list
		File dir = new File(ServerDirectory);
		fileNames = new HashSet<String>(Arrays.asList(dir.list()));
		System.out.print("Local files:\n");
		for(String s: fileNames) {
			System.out.print(s + "\n");
		}
		System.out.print("\n\n");
	}
	
	public InetAddress getIP() {
		return receiveSocket.getInetAddress();
	}

	//server's run method
	public void run() {
		//Thread response;
		byte[] requestData = new byte[dataSize];
		receivePacket = new DatagramPacket(requestData, requestData.length);
		int clientPort;
		InetAddress clientAddr;
		Thread fileHandl = new Thread(new fileUpdater(this));
		//fileHandl.start();
		while(isRunning) {
			//receive incoming request and pass it onto a new thread
			try {
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			clientAddr = receivePacket.getAddress();
			clientPort = receivePacket.getPort();

			Thread response = new Thread(new ResponseHandler(requestData, clientAddr, clientPort));
			response.start();
		}
	}

	public static void main(String args[]) throws IOException {

		NewServer s = new NewServer(ProfileData.getServerPort());
		System.out.println(s.getIP());
		Thread server = new Thread(s);
		server.start();
		System.out.print("Server running...accepting incoming read or write requests.\n");
		String userInput;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			System.out.print("\nEnter 'quit' to shutdown the server.");
			System.out.print("\nEnter 'files' to list the files stored in on the server.");
			userInput = reader.readLine();
			if(userInput.equals("quit"))  {
				server.interrupt();
				System.out.print("Server has been shut down. No longer accepting incoming requests.\n");
				isRunning = false;
				break;
			} else if(userInput.equals("files")) {
				s.populateFileNames();
			}
		}
		System.exit(0);
	}

	private class fileUpdater implements Runnable{

		private NewServer server;
		private int updateCount = 0;

		public fileUpdater(NewServer ns) {
			server = ns;

		}

		public void run() {
			while(true) {
				try {
					Thread.sleep(3000);                 //1000 milliseconds is one second.
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				server.populateFileNames();
			}
		}
	}

	private class ResponseHandler implements Runnable {

		private byte[] requestData;
		private InetAddress clientHost;
		private int clientPort;
		private DatagramSocket sendReceiveSocket;
		private DatagramPacket receivePacket, sendPacket;

		public ResponseHandler(byte[] rp, InetAddress h, int p) {
			requestData = rp;
			clientHost = h;
			clientPort = p;
			try {
				sendReceiveSocket = new DatagramSocket();
				sendReceiveSocket.setSoTimeout(timeout);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			//check for valid request
			try {
				int request = PacketManager.validateRequest(requestData);
				if(request == 1) {
					try {
						handleReadRequest(PacketManager.getFilename(requestData));
					} catch (SocketTimeoutException 
							| FileNotFoundException 
							| TFTPExceptions.InvalidTFTPAckException
							| TFTPExceptions.InvalidBlockNumberException 
							| TFTPExceptions.InvalidTFTPDataException 
							| TFTPExceptions.ErrorReceivedException 
							| TFTPExceptions.AccessViolationException e) {
						System.out.print(e + "\n");
					}
				} else if (request == 2) {
					try {
						handleWriteRequest(PacketManager.getFilename(requestData));
					} catch (SocketTimeoutException 
							| TFTPExceptions.FileAlreadyExistsException 
							| TFTPExceptions.InvalidBlockNumberException
							| TFTPExceptions.InvalidTFTPDataException 
							| TFTPExceptions.ErrorReceivedException
							| TFTPExceptions.DiskFullException e) {
						System.out.print(e + "\n");
					}
				}
			} catch (TFTPExceptions.InvalidTFTPRequestException e) {
				PacketManager.handleInvalidRequest
				(requestData, clientHost, clientPort, sendReceiveSocket);
			}
			sendReceiveSocket.close();
		}

		public void handleReadRequest(String filename) throws 
		SocketTimeoutException, 
		TFTPExceptions.InvalidTFTPAckException, 
		TFTPExceptions.InvalidBlockNumberException, 
		FileNotFoundException, 
		TFTPExceptions.InvalidTFTPDataException, 
		TFTPExceptions.ErrorReceivedException, 
		TFTPExceptions.AccessViolationException {

			Path path = FileSystems.getDefault().getPath(ServerDirectory, filename);
			if(!Files.isReadable(path)){
				PacketManager.createAccessViolationErrorPacket(filename, "Access Violation", clientHost, clientPort);
				throw new TFTPExceptions().new AccessViolationException("Cannot Read from this file");
			}

			//reader used for local 512 byte block reads
			BufferedInputStream reader = IOManager.getReader(ServerDirectory + filename);
			int blockNumber = 1;

			//byte buffer to be filled with 512 bytes of data from local file
			byte readFromFileData[] = new byte[bufferSize]; 

			//byte buffer for write data packets
			byte readData[] = new byte[bufferSize + ackSize];
			byte receivedAck[] = new byte[ackSize + bufferSize]; 

			try {
				readFromFileData = IOManager.read(reader, bufferSize, readFromFileData);
				readData = PacketManager.createData(readFromFileData, blockNumber);
				readFromFileData = new byte[readFromFileData.length];
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			sendPacket = new DatagramPacket(readData, readData.length,clientHost, clientPort);
			receivePacket = new DatagramPacket(receivedAck, receivedAck.length);

			int tries = ProfileData.getRepeats(); //number of times to re-listen
			boolean received = false;
			while(!received) { //repeat until a successful receive
				try {
					PacketManager.send(sendPacket, sendReceiveSocket);
					PacketManager.receive(receivePacket, sendReceiveSocket);
					received = true; //first data packet received
				} catch(SocketTimeoutException e) { //
					if(--tries == 0) {
						PacketManager.handleTimeOut(clientHost, sendPacket.getPort(), sendReceiveSocket, "Server"); //send error packet to server
						throw e;
					}
				}
			}

			//check for PID
			while(receivePacket.getPort() != clientPort) {
				PacketManager.handleInvalidPort(clientPort, receivePacket.getPort(), clientHost, sendReceiveSocket);
				tries = ProfileData.getRepeats(); //number of times to re-listen
				received = false;
				while(!received) { //repeat until a successful receive
					try {
						PacketManager.receive(receivePacket, sendReceiveSocket);
						received = true; 
					} catch(SocketTimeoutException e) { //
						if(--tries == 0) { 
							PacketManager.handleTimeOut(clientHost, sendPacket.getPort(), sendReceiveSocket, "Server"); //send error packet to server
							throw e;
						}
					}
				}
			}

			//check for error packet
			if(PacketManager.isErrorPacket(receivedAck)) {
				PacketManager.errorPacketPrinter(receivePacket);
				System.out.print("Received an error packet with code: " + receivedAck[3]
						+ " Exiting connection and terminating file transfer.\n");
				byte[] errorMsg = new byte[receivedAck.length - 4];
				System.arraycopy(receivedAck, 4, errorMsg, 0, errorMsg.length);
				System.out.print("Error message: " + new String(errorMsg) + "\n");
				throw new TFTPExceptions().new ErrorReceivedException(new String(errorMsg));
			}

			//known not to be an error packet so the receivedAck buffer is now truncated
			receivedAck = PacketManager.createAck(receivedAck);

			//check for valid ack
			try {
				PacketManager.validateAckPacket(receivedAck);
			} catch (TFTPExceptions.InvalidTFTPAckException e) {
				PacketManager.handleInvalidAckPacket(receivedAck, clientHost, clientPort, sendReceiveSocket);
				throw e;
			}

			//check block number
			if(blockNumber != PacketManager.getBlockNum(receivedAck)) {
				PacketManager.handleInvalidBlockNumber(blockNumber, PacketManager.getBlockNum(receivedAck), clientHost, clientPort, sendReceiveSocket);
				throw new TFTPExceptions().new InvalidBlockNumberException(
						"Invalid block number detected. "
								+ "Expected " + blockNumber + "." 
								+ "Found " + PacketManager.getBlockNum(receivedAck));
			}

			blockNumber++;

			//while(!PacketManager.lastPacket(PacketManager.getData(readData))) { 
			while(true) {
				
				System.out.print("Received:\n");
				System.out.print(Arrays.toString(receivedAck) + "\n");

				//byte buffer for write data packets
				readData = new byte[bufferSize + ackSize];
				receivedAck = new byte[ackSize + bufferSize]; //4 bytes

				try {
					readFromFileData = IOManager.read(reader, bufferSize, readFromFileData);
					if(readFromFileData == null) break;
					readData = PacketManager.createData(readFromFileData, blockNumber);
					readFromFileData = new byte[readFromFileData.length];
					//PacketManager.printTFTPPacketData(writeData);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				sendPacket = new DatagramPacket(readData, readData.length, 
						clientHost, clientPort);
				receivePacket = new DatagramPacket(receivedAck, receivedAck.length);

				tries = ProfileData.getRepeats(); //number of times to re-listen
				received = false;
				while(!received) { //repeat until a successful receive
					try {
						PacketManager.send(sendPacket, sendReceiveSocket);
						PacketManager.receive(receivePacket, sendReceiveSocket);
						received = true; //first data packet received
					} catch(SocketTimeoutException e) { //
						if(--tries == 0){
							PacketManager.handleTimeOut(clientHost, sendPacket.getPort(), sendReceiveSocket, "Server"); //send error packet to server
							throw e;
						}
					}
				}

				//check for PID
				while(receivePacket.getPort() != clientPort) {
					PacketManager.handleInvalidPort(clientPort, receivePacket.getPort(), clientHost, sendReceiveSocket);
					tries = ProfileData.getRepeats(); //number of times to re-listen
					received = false;
					while(!received) { //repeat until a successful receive
						try {
							PacketManager.receive(receivePacket, sendReceiveSocket);
							received = true; 
						} catch(SocketTimeoutException e) { //
							if(--tries == 0) {
								PacketManager.handleTimeOut(clientHost, sendPacket.getPort(), sendReceiveSocket, "Server"); //send error packet to server
								throw e;
							}
						}
					}
				}

				//check for error packet
				if(PacketManager.isErrorPacket(receivedAck)) {
					PacketManager.errorPacketPrinter(receivePacket);
					System.out.print("Received an error packet with code: " + receivedAck[3]
							+ " Exiting connection and terminating file transfer.\n");
					byte[] errorMsg = new byte[receivedAck.length - 4];
					System.arraycopy(receivedAck, 4, errorMsg, 0, errorMsg.length);
					System.out.print("Error message: " + new String(errorMsg) + "\n");
					throw new TFTPExceptions().new ErrorReceivedException(new String(errorMsg));
				}

				//known not to be an error packet so the receivedAck buffer is now truncated
				receivedAck = PacketManager.createAck(receivedAck);

				//check for valid ack
				try {
					PacketManager.validateAckPacket(receivedAck);
				} catch (TFTPExceptions.InvalidTFTPAckException e) {
					PacketManager.handleInvalidAckPacket(receivedAck, clientHost, clientPort, sendReceiveSocket);
					throw e;
				}

				//check block number
				if(blockNumber != PacketManager.getBlockNum(receivedAck)) {
					PacketManager.handleInvalidBlockNumber(blockNumber, PacketManager.getBlockNum(receivedAck), clientHost, clientPort, sendReceiveSocket);
					throw new TFTPExceptions().new InvalidBlockNumberException(
							"Invalid block number detected. "
									+ "Expected " + blockNumber + "." 
									+ "Found " + PacketManager.getBlockNum(receivedAck));
				}
				
				PacketManager.ackPacketPrinter(receivePacket);
				blockNumber++;

			}

			//send lastPacket packet

			//blockNumber++; 
			readData = null;
			readData = PacketManager.createLast(blockNumber++);
			sendPacket = new DatagramPacket(readData, readData.length, 
					clientHost, clientPort);


			PacketManager.send(sendPacket, sendReceiveSocket);

			System.out.print("Read completed succesfully.\n");

		}

		public void handleWriteRequest(String filename) throws 
		TFTPExceptions.FileAlreadyExistsException, 
		SocketTimeoutException,
		TFTPExceptions.InvalidBlockNumberException,
		TFTPExceptions.InvalidTFTPDataException, 
		TFTPExceptions.ErrorReceivedException,
		TFTPExceptions.DiskFullException {

			//check if the file already exists locally on the client
			TFTPExceptions.FileAlreadyExistsException fileExists = 
					new TFTPExceptions().new FileAlreadyExistsException
					("File " + filename + " already exists locally. "
							+ "To avoid overwriting data the read request has been denied.");
			if(fileNames.contains(filename)) { 
				//TODO handleFileExists
				throw fileExists; 
			}

			byte receivedData[] = new byte[ackSize + bufferSize]; 
			int blockNumber = 0;
			byte ackToBeSent[] = new byte[4];

			ackToBeSent = PacketManager.createAck(receivedData);

			sendPacket = new DatagramPacket(ackToBeSent, ackToBeSent.length,clientHost, clientPort);
			receivePacket = new DatagramPacket(receivedData, receivedData.length);

			//send ACK 0
			PacketManager.send(sendPacket, sendReceiveSocket);
			PacketManager.ackPacketPrinter(sendPacket);
			blockNumber++;

			int tries = ProfileData.getRepeats(); //number of times to relisten
			boolean received = false;
			while(!received) { //repeat until a successful receive
				try {
					PacketManager.receive(receivePacket, sendReceiveSocket);
					received = true; //first data packet received
				} catch(SocketTimeoutException e) { //
					if(--tries == 0) {
						PacketManager.handleTimeOut(clientHost, sendPacket.getPort(), sendReceiveSocket, "Server"); //send error packet to server
						throw e;
					}
				}
			}

			PacketManager.DataPacketPrinter(receivePacket);
			//check if last packet
			if(PacketManager.isLast(receivePacket.getData())) {
				System.out.print("The file from which the write data comes from is empty, write request considered complete.");
				return;
			}

			//check for error packet
			if(PacketManager.isErrorPacket(receivedData)) {
				System.out.print("Received an error packet with code: " + receivedData[3]
						+ " Exiting connection and terminating file transfer.\n");
				byte[] errorMsg = new byte[receivedData.length - 4];
				System.arraycopy(receivedData, 4, errorMsg, 0, errorMsg.length);
				System.out.print("Error message: " + new String(errorMsg) + "\n");
				throw new TFTPExceptions().new ErrorReceivedException(new String(errorMsg));
			}

			//check for valid data
			try {
				PacketManager.validateDataPacket(receivedData);
			} catch (TFTPExceptions.InvalidTFTPDataException e) {
				PacketManager.handleInvalidDataPacket(receivedData, clientHost, clientPort, sendReceiveSocket);
				throw e;
			}

			//check block number
			if(blockNumber != PacketManager.getBlockNum(receivedData)) {
				PacketManager.handleInvalidBlockNumber(blockNumber, PacketManager.getBlockNum(receivedData), clientHost, clientPort, sendReceiveSocket);
				throw new TFTPExceptions().new InvalidBlockNumberException(
						"Invalid block number detected. "
								+ "Expected " + blockNumber + "." 
								+ "Found " + PacketManager.getBlockNum(receivedData));
			}

			System.out.print("Packet Block Number: " + PacketManager.getBlockNum(receivedData) + "\n");

			if(!PacketManager.diskSpaceCheck(ServerDirectory, PacketManager.filesize(PacketManager.getData(receivePacket.getData())))){
				//if we dont have enough space to write the next block
				PacketManager.handleDiskFull(ServerDirectory, clientHost, clientPort, sendReceiveSocket);
				//throw new TFTPExceptions().new DiskFullException("Not enough space to write to disk");
				throw new TFTPExceptions().new DiskFullException("Not enough space to write to disk");
			}

			File writeTo = new File(ServerDirectory + filename); //file to write to locally
			byte writeToFileData[] = new byte[0];
			writeToFileData = PacketManager.getData(receivedData);
			try {
				IOManager.write(writeTo, writeToFileData);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			while(true) {

				System.out.print("Received:\n");
				System.out.print(Arrays.toString(PacketManager.getData(receivedData))  + "\n");

				ackToBeSent = PacketManager.createAck(receivedData);
				receivedData = new byte[ackSize + bufferSize];

				//send ACK
				sendPacket = new DatagramPacket(ackToBeSent, ackToBeSent.length, 
						clientHost, clientPort);
				receivePacket = new DatagramPacket(receivedData, receivedData.length);

				PacketManager.send(sendPacket, sendReceiveSocket);
				blockNumber++;

				tries = ProfileData.getRepeats(); //number of times to relisten
				received = false;
				while(!received) { //repeat until a successful receive
					try {
						PacketManager.receive(receivePacket, sendReceiveSocket);
						received = true; //first data packet received
					} catch(SocketTimeoutException e) { //
						if(--tries == 0) {
							PacketManager.handleTimeOut(clientHost, sendPacket.getPort(), sendReceiveSocket, "Server"); //send error packet to server
							throw e;
						}
					}
				}


				//check for error packet
				if(PacketManager.isErrorPacket(receivedData)) {
					System.out.print("Received an error packet with code: " + receivedData[3]
							+ " Exiting connection and terminating file transfer.\n");
					byte[] errorMsg = new byte[receivedData.length - 4];
					System.arraycopy(receivedData, 4, errorMsg, 0, errorMsg.length);
					System.out.print("Error message: " + new String(errorMsg) + "\n");
					throw new TFTPExceptions().new ErrorReceivedException(new String(errorMsg));
				}

				//check for valid data
				try {
					PacketManager.validateDataPacket(receivedData);
				} catch (TFTPExceptions.InvalidTFTPDataException e) {
					PacketManager.handleInvalidDataPacket(receivedData, clientHost, clientPort, sendReceiveSocket);
					throw e;
				}

				//check block number
				if(blockNumber != PacketManager.getBlockNum(receivedData)) {
					PacketManager.handleInvalidBlockNumber(blockNumber, PacketManager.getBlockNum(receivedData), clientHost, clientPort, sendReceiveSocket);
					throw new TFTPExceptions().new InvalidBlockNumberException(
							"Invalid block number detected. "
									+ "Expected " + blockNumber + "." 
									+ "Found " + PacketManager.getBlockNum(receivedData));
				}

				PacketManager.DataPacketPrinter(receivePacket);
				if(PacketManager.isLast(receivePacket.getData())) { break; }
				
				if(!PacketManager.diskSpaceCheck(ServerDirectory, PacketManager.filesize(PacketManager.getData(receivePacket.getData())))){
					//if we dont have enough space to write the next block
					PacketManager.handleDiskFull(ServerDirectory, clientHost, clientPort, sendReceiveSocket);
					throw new TFTPExceptions().new DiskFullException("Not enough space to write to disk");

				}

				writeToFileData = PacketManager.getData(receivedData);
				try {
					IOManager.write(writeTo, writeToFileData);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

			}

			System.out.print("Write complete.\n");
		}
	}
}
