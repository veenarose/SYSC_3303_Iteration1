import java.util.Set;
import java.io.*;
import java.net.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

/**
 * Client.java
 * a basic client class used for communicating with a server using the TFTP protocol
 **/

public class Client { //the client class 

	private DatagramSocket sendReceiveSocket; //socket which sends and receives UDP packets
	private DatagramPacket sendPacket, receivePacket; //UDP send (request) and receive (acknowledgement) packets

	private static InetAddress serverHost;

	private final static String ClientDirectory =  //"/Volumes/KINGSTON/"; 
			(System.getProperty("user.dir") + "/src/ClientData/");

	private Set<String> fileNames; //java set to store file names
	private static int serverPort = 0;
	private static int request = -1;

	private final static int timeout = ProfileData.getTimeOut(); //milliseconds
	private final static int bufferSize = IOManager.getBufferSize();
	private final static int ackSize = PacketManager.getAckSize();

	/**
	 * client constructor
	 */
	public Client()
	{
		try {
			//construct a datagram socket to be used to send and receive UDP Datagram requests and bind it to any available 
			//port on the local host machine. 
			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.setSoTimeout(timeout);
			//set server host
			//serverHost = InetAddress.getLocalHost();
			//populate fileNames list
			File dir = new File(ClientDirectory);
			fileNames = new HashSet<String>(Arrays.asList(dir.list()));
			System.out.println("\n\nLocal files:");
			for(String s: fileNames) {
				System.out.println(s);
			}
			System.out.println("\n");
		} catch (SocketException se) {   //unable to create socket
			se.printStackTrace();
			System.exit(1);
		} 
	}

	/**
	 * a method which checks if an input string matches the strings "read" or "write" (the valid request types)
	 * returns 1 for a read request match, 2 for a write request match and 0 on invalid input
	 * @param in
	 * @return
	 */
	public static int validateReqInput(String in) {
		if(in.equals(PacketManager.requests()[0])) return 1;
		else if(in.equals(PacketManager.requests()[1])) return 2;
		else return 0;
	}

	/**
	 * a method which checks if an input string matches the strings "netascii" or "octet" (valid UDP read/write request modes)
	 * returns 0 for a netascii match, 1 for an octet match, and -1 on invalid input
	 * @param in
	 * @return
	 */
	public static int validateModeInput(String in) {
		if(in.equals(PacketManager.modes()[0])) return 0;
		else if(in.equals(PacketManager.modes()[1])) return 1;
		else return -1;
	}

	public void handleReadRequest(String filename, String mode) 
			throws TFTPExceptions.FileAlreadyExistsException, 
			SocketTimeoutException,
			TFTPExceptions.InvalidBlockNumberException,
			TFTPExceptions.InvalidTFTPDataException, 
			TFTPExceptions.ErrorReceivedException,
			TFTPExceptions.DiskFullException
	{

		//check if the file already exists locally on the client
		TFTPExceptions.FileAlreadyExistsException fileExists = 
				new TFTPExceptions().new FileAlreadyExistsException
				("File " + filename + " already exists locally. "
						+ "To avoid overwriting data the read request has been denied.");
		if(fileNames.contains(filename)) { throw fileExists; }

		byte[] readPacketData = null;
		readPacketData = PacketManager.createReadPacketData(filename, mode);
		byte[] receivedData = new byte[PacketManager.getDataSize()]; //516 bytes

		if(request == 1){
			serverPort = ProfileData.getServerPort();
		} else if(request == 2){
			serverPort = ProfileData.getErrorPort();
		}

		sendPacket = new DatagramPacket(readPacketData, readPacketData.length, 
				serverHost, serverPort);
		receivePacket = new DatagramPacket(receivedData, receivedData.length);

		//initially expecting the first block of data to be read from the file on the server
		int expectedBlockNumber = 1;

		int tries = ProfileData.getRepeats(); //number of times to relisten
		boolean received = false;
		while(!received) { //repeat until a successful receive
			try {
				PacketManager.send(sendPacket, sendReceiveSocket);
				PacketManager.receive(receivePacket, sendReceiveSocket);
				received = true; //first data packet received
			} catch(SocketTimeoutException e) { //
				if(--tries == 0) { 
					PacketManager.handleTimeOut(serverHost, sendPacket.getPort(), sendReceiveSocket, "Client"); //send error packet to server
					throw e;
				}
			}
		}

		int serverPort = receivePacket.getPort();

		//check for last packet
		if(PacketManager.isLast(receivePacket.getData())) {
			System.out.println("The file to be read from is empty, read request considered complete.");
			return;
		}

		//check for error packet
		if(PacketManager.isErrorPacket(receivedData)) {
			PacketManager.errorPacketPrinter(receivePacket);
			System.out.println("\n\nReceived an error packet with code: " + receivedData[3]
					+ " Exiting connection and terminating file transfer.\n\n");
			byte[] errorMsg = new byte[receivedData.length - 4];
			System.arraycopy(receivedData, 4, errorMsg, 0, errorMsg.length);
			throw new TFTPExceptions().new ErrorReceivedException(new String(errorMsg));
		}

		//check for valid data
		try {
			PacketManager.validateDataPacket(receivedData);
		} catch (TFTPExceptions.InvalidTFTPDataException e) {
			PacketManager.handleInvalidDataPacket(receivedData, serverHost, serverPort, sendReceiveSocket);
			throw e;
		}

		PacketManager.DataPacketPrinter(receivePacket);
		//the port through which the client will communicate with the server
		//upon a successfully established connection
		int blockNumber = PacketManager.getBlockNum(receivedData); 

		//check the block number
		if(blockNumber != expectedBlockNumber) {
			PacketManager.handleInvalidBlockNumber(expectedBlockNumber, blockNumber, serverHost, serverPort, sendReceiveSocket);
			throw new TFTPExceptions().new InvalidBlockNumberException(
					"Invalid block number detected. "
							+ "Expected " + expectedBlockNumber + "." 
							+ "Found " + blockNumber);
		}

		if(!PacketManager.diskSpaceCheck(ClientDirectory, PacketManager.filesize(PacketManager.getData(receivePacket.getData())))){
			//if we dont have enough space to write the next block
			PacketManager.handleDiskFull(ClientDirectory, serverHost, serverPort, sendReceiveSocket);
			throw new TFTPExceptions().new DiskFullException("Not enough space to write to disk");
		}
		File writeTo = new File(ClientDirectory + filename); //file to write to locally
		byte writeToFileData[];
		writeToFileData = PacketManager.getData(receivedData);

		try {
			IOManager.write(writeTo, writeToFileData);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		while(true)	{

			//send ack and receive next data
			byte[] sendingAck = PacketManager.createAck(receivedData);
			receivedData = new byte[PacketManager.getDataSize()];

			sendPacket = new DatagramPacket(sendingAck, sendingAck.length, 
					serverHost, serverPort);
			receivePacket = new DatagramPacket(receivedData, receivedData.length);
			PacketManager.send(sendPacket, sendReceiveSocket);

			//listen for a response
			tries = ProfileData.getRepeats(); //number of times to re-listen
			received = false;
			while(!received) { //repeat until a successful receive
				try {
					//PacketManager.send(sendPacket, sendReceiveSocket);
					PacketManager.receive(receivePacket, sendReceiveSocket);
					received = true;
				} catch(SocketTimeoutException e) {
					if(--tries == 0) {
						PacketManager.handleTimeOut(serverHost, sendPacket.getPort(), sendReceiveSocket, "Client"); //send error packet to server
						throw e;
					}
				}
			}


			//check for PID
			while(receivePacket.getPort() != serverPort) {
				PacketManager.handleInvalidPort(serverPort, receivePacket.getPort(), serverHost, sendReceiveSocket);
				tries = ProfileData.getRepeats(); //number of times to re-listen
				received = false;
				while(!received) { //repeat until a successful receive
					try {
						PacketManager.receive(receivePacket, sendReceiveSocket);
						received = true; 
					} catch(SocketTimeoutException e) { //
						if(--tries == 0) {
							PacketManager.handleTimeOut(serverHost, sendPacket.getPort(), sendReceiveSocket, "Client"); //send error packet to server
							throw e;
						}
					}
				}
			}

			//check for error packet
			if(PacketManager.isErrorPacket(receivedData)) {
				PacketManager.errorPacketPrinter(receivePacket);
				System.out.println("\n\nReceived an error packet with code: " + receivedData[3]
						+ " Exiting connection and terminating file transfer.\n\n");
				byte[] errorMsg = new byte[receivedData.length - 4];
				System.arraycopy(receivedData, 4, errorMsg, 0, errorMsg.length);
				throw new TFTPExceptions().new ErrorReceivedException(new String(errorMsg));
			}

			//check for valid data
			try {
				PacketManager.validateDataPacket(receivedData);
			} catch (TFTPExceptions.InvalidTFTPDataException e) {
				PacketManager.handleInvalidDataPacket(receivedData, serverHost, serverPort, sendReceiveSocket);
				throw e;
			}

			//increase expected block number and get the block number from the received packet
			expectedBlockNumber++;
			blockNumber = PacketManager.getBlockNum(receivedData); 

			//check block number
			if(blockNumber != expectedBlockNumber) {
				PacketManager.handleInvalidBlockNumber(expectedBlockNumber, blockNumber, serverHost, serverPort, sendReceiveSocket);
				throw new TFTPExceptions().new InvalidBlockNumberException(
						"Invalid block number detected. "
								+ "Expected " + expectedBlockNumber + "." 
								+ "Found " + blockNumber);
			}
			if(!PacketManager.diskSpaceCheck(ClientDirectory, PacketManager.filesize(PacketManager.getData(receivePacket.getData())))){
				//if we dont have enough space to write the next block
				PacketManager.handleDiskFull(ClientDirectory, serverHost, serverPort, sendReceiveSocket);
				throw new TFTPExceptions().new DiskFullException("Not enough space to write to disk");
			}

			PacketManager.DataPacketPrinter(receivePacket);
			if (PacketManager.isLast(receivePacket.getData())) { break; }

			if (!PacketManager.isLast(receivedData)) {
				//write the block
				writeToFileData = PacketManager.getData(receivedData);
				try {
					IOManager.write(writeTo, writeToFileData);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

		}

		//print out success
		System.out.println("Read was succesful.");
	}

	public void handleWriteRequest(String filename, String mode) 
			throws SocketTimeoutException, 
			TFTPExceptions.InvalidTFTPAckException, 
			TFTPExceptions.InvalidBlockNumberException, 
			FileNotFoundException, 
			TFTPExceptions.InvalidTFTPDataException, 
			TFTPExceptions.ErrorReceivedException,
			TFTPExceptions.AccessViolationException {

		Path path = FileSystems.getDefault().getPath(ClientDirectory, filename);

		//reader used for local 512 byte block reads
		BufferedInputStream reader = IOManager.getReader(ClientDirectory + filename);

		//readPacketData stores the TFTP WRQ packet data to be inserted 
		//into the sendPacket DatagramPacket
		//receivedData stores the expected TFTP ACK packet to be received
		//into the receivePacket DatagramPacket
		byte[] writePacketData = null;
		writePacketData = PacketManager.createWritePacketData(filename, mode);
		byte[] receivedAck = new byte[ackSize + bufferSize]; //4 bytes

		int p = -1;
		if(request == 1){
			p = ProfileData.getServerPort();
		} else if(request == 2){
			p = ProfileData.getErrorPort();
		}

		sendPacket = new DatagramPacket(writePacketData, writePacketData.length, 
				serverHost, p);
		receivePacket = new DatagramPacket(receivedAck, receivedAck.length);

		//initially expecting the first block of data to be read from the file on the server
		int expectedBlockNumber = 0;



		int tries = new Integer(0); 
		tries = ProfileData.getRepeats(); //number of times to re-listen
		boolean received = false;
		while(!received) { //repeat until a successful receive
			try {
				PacketManager.send(sendPacket, sendReceiveSocket);
				PacketManager.receive(receivePacket, sendReceiveSocket);
				received = true; //first data packet received
			} catch(SocketTimeoutException e) { //
				if(--tries == 0) { 
					PacketManager.handleTimeOut(serverHost, sendPacket.getPort(), sendReceiveSocket, "Client"); //send error packet to server
					throw e;
				}
			}
		}

		int serverPort = receivePacket.getPort();
		if(!Files.isReadable(path)){
			PacketManager.createAccessViolationErrorPacket(filename, "Access Violation", serverHost, serverPort);
			throw new TFTPExceptions().new AccessViolationException("Cannot Read from this file");
		}
		//check for error packet
		if(PacketManager.isErrorPacket(receivedAck)) {
			PacketManager.errorPacketPrinter(receivePacket);
			System.out.println("\n\nReceived an error packet with code: " + receivedAck[3]
					+ " Exiting connection and terminating file transfer.\n\n");
			byte[] errorMsg = new byte[receivedAck.length - 4];
			System.arraycopy(receivedAck, 4, errorMsg, 0, errorMsg.length);
			throw new TFTPExceptions().new ErrorReceivedException(new String(errorMsg));
		}

		//check for valid ack
		try {
			PacketManager.validateAckPacket(receivedAck);
		} catch (TFTPExceptions.InvalidTFTPAckException e) {
			PacketManager.handleInvalidAckPacket(receivedAck, serverHost, serverPort, sendReceiveSocket);
			throw e;
		}

		//the port through which the client will communicate with the server
		//upon a successfully established connection
		int blockNumber = PacketManager.getBlockNum(receivedAck);

		PacketManager.ackPacketPrinter(receivePacket);

		//check the block number
		if(blockNumber != expectedBlockNumber) {
			PacketManager.handleInvalidBlockNumber(expectedBlockNumber, blockNumber, serverHost, serverPort, sendReceiveSocket);
			throw new TFTPExceptions().new InvalidBlockNumberException(
					"Invalid block number detected. "
							+ "Expected " + expectedBlockNumber + "." 
							+ "Found " + blockNumber);
		}

		expectedBlockNumber++;
		//read 512 bytes from local file

		//byte buffer to be filled with 512 bytes of data from local file
		byte readFromFileData[] = new byte[bufferSize]; 

		//byte buffer for write data packets
		byte writeData[] = new byte[bufferSize + ackSize];

		try {
			readFromFileData = IOManager.read(reader, bufferSize, readFromFileData);
			if(readFromFileData == null) { 
				writeData = null;
				writeData = PacketManager.createLast(expectedBlockNumber);
				sendPacket = new DatagramPacket(writeData, writeData.length, 
						serverHost, serverPort);
				PacketManager.send(sendPacket, sendReceiveSocket);
				System.out.println("File empty, write request considered terminated.");
				return;
			}
			writeData = PacketManager.createData(readFromFileData, expectedBlockNumber);
			readFromFileData = new byte[readFromFileData.length];
			//PacketManager.printTFTPPacketData(writeData);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		//send the data
		sendPacket = new DatagramPacket(writeData, writeData.length, 
				serverHost, serverPort); 
		PacketManager.send(sendPacket, sendReceiveSocket);

		while(true) {

			receivedAck = new byte[bufferSize + ackSize]; //4 bytes
			receivePacket = new DatagramPacket(receivedAck, receivedAck.length);

			//receive ack
			tries = ProfileData.getRepeats(); //number of times to relisten
			received = false;
			int x = tries;
			while(!received) { //repeat until a successful receive, resend data
				try {

					if(tries < x){
						PacketManager.send(sendPacket, sendReceiveSocket);
					}
					PacketManager.receive(receivePacket, sendReceiveSocket);
					received = true; //first data packet received
				} catch(SocketTimeoutException e) { //
					if(--tries == 0) { 
						PacketManager.handleTimeOut(serverHost, sendPacket.getPort(), sendReceiveSocket, "Client"); //send error packet to server
						throw e;
					}
				}
			}

			//check for PID
			while(receivePacket.getPort() != serverPort) {
				PacketManager.handleInvalidPort(serverPort, receivePacket.getPort(), serverHost, sendReceiveSocket);
				tries = ProfileData.getRepeats(); //number of times to re-listen
				received = false;
				while(!received) { //repeat until a successful receive
					try {
						PacketManager.receive(receivePacket, sendReceiveSocket);
						received = true; 
					} catch(SocketTimeoutException e) { //
						if(--tries == 0) {
							PacketManager.handleTimeOut(serverHost, sendPacket.getPort(), sendReceiveSocket, "Client"); //send error packet to server
							throw e;
						}
					}
				}
			}

			//check for error packet
			if(PacketManager.isErrorPacket(receivedAck)) {
				PacketManager.errorPacketPrinter(receivePacket);
				System.out.println("\n\nReceived an error packet with code: " + receivedAck[3]
						+ " Exiting connection and terminating file transfer.\n\n");
				byte[] errorMsg = new byte[receivedAck.length - 4];
				System.arraycopy(receivedAck, 4, errorMsg, 0, errorMsg.length);
				throw new TFTPExceptions().new ErrorReceivedException(new String(errorMsg));
			}

			//known not to be an error packet so the receivedAck buffer is now truncated
			receivedAck = PacketManager.createAck(receivedAck);

			//check for valid ack
			try {
				PacketManager.validateAckPacket(receivedAck);
			} catch (TFTPExceptions.InvalidTFTPAckException e) {
				PacketManager.handleInvalidAckPacket(receivedAck, serverHost, serverPort, sendReceiveSocket);
				throw e;
			}

			//increase expected block number and get the block number from the received packet
			blockNumber = PacketManager.getBlockNum(receivedAck); 

			//check block number
			if(blockNumber != expectedBlockNumber) {
				PacketManager.handleInvalidBlockNumber(expectedBlockNumber, blockNumber, serverHost, serverPort, sendReceiveSocket);
				throw new TFTPExceptions().new InvalidBlockNumberException(
						"Invalid block number detected. "
								+ "Expected " + expectedBlockNumber + "." 
								+ "Found " + blockNumber);
			}

			PacketManager.ackPacketPrinter(receivePacket);
			expectedBlockNumber++;

			//read locally
			//byte buffer to be filled with 512 bytes of data from local file
			readFromFileData = new byte[bufferSize]; 

			//byte buffer for write data packets
			writeData = new byte[bufferSize + ackSize];

			try {

				readFromFileData = IOManager.read(reader, bufferSize, readFromFileData);
				if(readFromFileData == null) { 
					writeData = null;
					writeData = PacketManager.createLast(expectedBlockNumber);
					sendPacket = new DatagramPacket(writeData, writeData.length, 
							serverHost, serverPort);
					PacketManager.send(sendPacket, sendReceiveSocket);

					System.out.println("Succesful write completed.");
					sendReceiveSocket.close();
					return; 
				}
				writeData = PacketManager.createData(readFromFileData, expectedBlockNumber);
				readFromFileData = new byte[readFromFileData.length];
				//PacketManager.printTFTPPacketData(writeData);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			//send the data
			sendPacket = new DatagramPacket(writeData, writeData.length, 
					serverHost, serverPort); 
			PacketManager.send(sendPacket, sendReceiveSocket);
		}


	}

	public static void main( String args[] ) throws IOException
	{
		boolean validIP = false;

		System.out.println("Hello and welcome!");
		int req = -1;
		Scanner keyboard = new Scanner(System.in);
		while(req != 1 || req != 2){
			keyboard = new Scanner(System.in);
			System.out.println("Connect to Error Simulator or Server?");
			System.out.println("1: Server, 2: Error Simulator");
			req = keyboard.nextInt();
			if(req == 1 || req == 2){
				request = req;
				break; 
			}
			while(true){
				System.out.println("Enter 1 to connect to server or 2 to connect to Error Sim");
				req = keyboard.nextInt();
				if(req == 1 || req == 2){ 
					request = req;
					break; 
				}
			}
			if(req == 1 || req == 2){
				keyboard.close();
				break; 
			}
			keyboard.close();
		}



		do{
			//Get an IP addr
			System.out.println("\nPlease enter an IP address:");
			String host = keyboard.next();
			try {
				serverHost = InetAddress.getByName(host);
				validIP = true;
			} catch(UnknownHostException e) {
				System.out.println("Invalid host name or IP address. Please try again.\n");
				continue;
			}

			//Get a Port
			System.out.println("\nPlease enter a Port:");
			serverPort = keyboard.nextInt();

		} while(!validIP);

		validIP = false;

		//prompt user to specify if the request they are making is either read or write
		while(true){

			System.out.println("Enter 'read' to read from the server.\n"
					+ "Enter 'write' to write to a file on the server.\n" 
					+ "Enter 'quit' at anytime to exit the program.\n"
					+ "Enter 'server' at any time to change server\n");
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String request = reader.readLine(); //read user input by line
			if(request.equals("quit")) { 
				System.out.println("System quit");
				return;
			} else if (request.equals("server")){
				do{
					//Get an IP addr
					System.out.println("\nPlease enter an IP address:");
					String host = keyboard.next();
					try {
						serverHost = InetAddress.getByName(host);
						validIP = true;
					} catch(UnknownHostException e) {
						System.out.println("Invalid host name or IP address. Please try again.\n");
						continue;
					}

					//Get a Port
					System.out.println("\nPlease enter the Port of the server:");
					serverPort = keyboard.nextInt();

				} while(!validIP);
			}

			if (validIP == true){
				validIP = false;
				continue;
			}

			while(Client.validateReqInput(request) == 0) { //while input is not 'read' or 'write'
				System.out.println("Please enter either 'read' or 'write'.");
				request = reader.readLine();
				if(request.equals("quit")) {
					System.out.println("System quit");
					return;
				}
			}

			//prompt user to specify the mode of the request
			System.out.println("Enter which mode you would like this request to be.\n"
					+ "Enter either 'netascii' or 'octet'.");
			String mode = reader.readLine(); //read user input by line
			if(mode.equals("quit")) { 
				System.out.println("System quit");
				return; 
			}

			while(Client.validateModeInput(mode) == -1) { //while input is not 'netascii' or 'octet'
				System.out.println("Please enter either 'netascii' or 'octet'.");
				mode = reader.readLine();
				if(mode.equals("quit")) {
					System.out.println("System quit");
					return;
				}
			}

			//prompt the user for a filename
			String filename;
			if(Client.validateReqInput(request) == 1) { //read request
				System.out.println("Enter the name of the file you will be reading from "
						+ "(Make sure to include the file's extension!):");
				filename = reader.readLine();
				if(filename.equals("quit")) {
					System.out.println("System quit");
					return;
				}
			} else { //write request
				System.out.println("Enter the name of the file you will be writing to "
						+ "(Make sure to include the file's extension!): ");
				filename = reader.readLine();
				if(filename.equals("quit")) {
					System.out.println("System quit");
					return; 
				}
			}
			System.out.print("Sending your request");

			Client c = new Client(); 

			int rw = Client.validateReqInput(request);

			if(rw == 1) { //read request
				try {
					c.handleReadRequest(filename, mode);
				} catch(TFTPExceptions.FileAlreadyExistsException e) {//file already exists
					System.out.println(e.getMessage() + "\n");
				} catch (TFTPExceptions.InvalidBlockNumberException e) {
					System.out.println(e.getMessage() + "\n");
				} catch (TFTPExceptions.InvalidTFTPDataException e) {
					System.out.println(e.getMessage() + "\n");
				} catch (SocketTimeoutException e) {
					System.out.println(e.getMessage() + "\n");
				} catch (TFTPExceptions.ErrorReceivedException e) {
					System.out.println(e.getMessage() + "\n");
				} catch (TFTPExceptions.DiskFullException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;

			} else { //write request
				try {
					c.handleWriteRequest(filename, mode);
				} catch (FileNotFoundException 
						| TFTPExceptions.InvalidTFTPAckException 
						| TFTPExceptions.InvalidBlockNumberException 
						| TFTPExceptions.InvalidTFTPDataException 
						| TFTPExceptions.ErrorReceivedException
						| TFTPExceptions.AccessViolationException e) {
					System.out.println(e.getMessage() + "\n");
				} catch (SocketTimeoutException e) {
					//e.printStackTrace();

					System.out.println(e.getMessage() + "\n");
				}
				continue;
			}
		}

	} //end of main

} //end of client class
