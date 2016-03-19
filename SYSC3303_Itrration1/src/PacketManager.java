import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class is used to handle the read / write packets between the clients and server
 */
public class PacketManager {
	private static final byte[] RRQ = {0,1}; //read opcode
	private static final byte[] WRQ = {0,2}; //write opcode
	private static final byte[] DRQ = {0,3}; //data opcode
	private static final byte[] ARQ = {0,4}; //ack opcode
	private static final byte[] ERC = {0,5}; //error opcode

	private static final byte[] NotDefined = {0,0};
	private static final byte[] FileNotFound = {0,1};
	private static final byte[] AccessViolation = {0,2};
	private static final byte[] DiskFull = {0,3};
	private static final byte[] IllegalTFTPOp = {0,4};
	private static final byte[] UnknownTID = {0,5};
	private static final byte[] FileExists = {0,6};
	
	private static final String[] modes = {"netascii", "octet"};// The modes
	private static final String[] requests = {"read","write"};//
	
	private static final int bufferSize = IOManager.getBufferSize();
	private static final int ackSize = 4;
	private static final int dataSize = bufferSize + ackSize;
	
	
	public static int getAckSize() {
		return ackSize;
	}
	
	public static int getDataSize() {
		return dataSize;
	}
	
	public static int getBufferSize() {
		return bufferSize;
	}
	
	public static String[] modes() {
		return modes;
	}
	
	public static String[] requests() {
		return requests;
	}

	/**
	 * This method is used to create a read request to send to the server
	 *@param message The name of the file
	 *@param mode The valid mode either netascii or octet
	 *@return an array of bytes that sends a request to read a file on the server 
	 */
	public static byte[] createReadPacketData(String message, String mode){

		byte file[] = message.getBytes();
		byte modes[] = mode.getBytes();
		byte arr2[] = {0};
		byte arr3[] = {0};

		byte readPack[] = new byte[RRQ.length+file.length+modes.length+arr2.length+arr3.length];

		System.arraycopy(RRQ, 0, readPack, 0, RRQ.length);
		System.arraycopy(file, 0, readPack, RRQ.length, file.length);
		System.arraycopy(arr2, 0, readPack, file.length+2, arr2.length);
		System.arraycopy(modes, 0, readPack, file.length+2+arr2.length, modes.length);

		return readPack;
	}

	/**
	 * This method is used to create a write request to send to the server
	 *@param message String: The name of the file
	 *@param mode String: The valid mode either netascii or octet
	 *@return an array of bytes that sends a request to write a file on the server 
	 */
	public static byte[] createWritePacketData(String message, String mode){

		byte file[] = message.getBytes();
		byte modes[] = mode.getBytes();
		byte arr2[] = {0};
		byte arr3[] = {0};

		byte readPack[] = new byte[WRQ.length+file.length+modes.length+arr2.length+arr3.length];

		System.arraycopy(WRQ, 0, readPack, 0, WRQ.length);
		System.arraycopy(file, 0, readPack, WRQ.length, file.length);
		System.arraycopy(arr2, 0, readPack, file.length+2, arr2.length);
		System.arraycopy(modes, 0, readPack, file.length+2+arr2.length, modes.length);

		return readPack;

	}

	/**
	 * Creates a packet to send the server or client 
	 * @param data Byte[] The array of data 
	 * @param blockNum block number
	 * @return The byte array containing the opcode to send
	 */
	public static byte[] createData(byte[] data, int blockNum){	
		byte block[] = intToBytes(blockNum);
		byte dataPack[] = new byte[bufferSize + ackSize]; 

		System.arraycopy(DRQ, 0, dataPack, 0, DRQ.length);
		System.arraycopy(block, 0, dataPack, DRQ.length, block.length);
		System.arraycopy(data, 0, dataPack, block.length+DRQ.length, data.length);
		return dataPack;
	}

	/**
	 * This method is used to extract the Ack code from the data received
	 * @param data byte[]: The data that is received
	 * @return A byte array containing the Ack code
	 */
	public static byte[] createAck(byte[] data){
		byte[] ack = new byte[4];
		ack[0] = 0; 
		ack[1] = 4;
		ack[2] = data[2];
		ack[3] = data[3];
		return ack;
	}

	/**
	 * Creates a packet to send the server or client 
	 * @param errCode Byte[] The that contains the Error code 04/05 
	 * @param errMessage String The error message
	 * @return The byte array containing the the error packet
	 */
	public static byte[] createError(byte[] errCode, String errMessage){	
		byte[] data = errMessage.getBytes();
		byte arr[] = {0};
		byte dataPack[] = new byte[ERC.length+errCode.length+data.length+arr.length]; 

		//System.out.println("COPYING ARRAYS");
		System.arraycopy(ERC, 0, dataPack, 0, ERC.length);
		System.arraycopy(errCode, 0, dataPack, ERC.length, errCode.length);
		System.arraycopy(data, 0, dataPack, errCode.length+ERC.length, data.length);
		//System.out.println("COPIED ARRAYS dataPack = " + new String(dataPack));
		return dataPack;
	}
	@SuppressWarnings("unused")
	static private boolean hasValidMode(String mode) {
		int minModeLen = Integer.min(modes[0].length(), modes[1].length());

		return false;
	}

	/** 
	 *  Validates the data of a packet to determine a valid read or write request. 
	 *  
	 * @param data Data to be validated 
	 * @return Int representing the type of message 1 = Read, 2 = Write 
	 * 		   100 = no termination after 0, 200 = no terminating 0 
	 * @throws IOException 
	 */ 
	public static int validateRequest(byte[] data) throws TFTPExceptions.InvalidTFTPRequestException {

		TFTPExceptions.InvalidTFTPRequestException invalid = new TFTPExceptions().new InvalidTFTPRequestException("invalid");
		int zeroCount = 0; 	
		String[] sfn;
		byte[] delimiter = {0};

		//checks leading 0 byte and read/write request byte
		if(data[0] != 0 || (data[1] != 1 && data[1] != 2)) {
			throw invalid;
		} 

		//checks if final byte is 0
		if(data[data.length-1] != 0) { 
			throw invalid;
		}

		//if packet is too large
		if(data.length > 516){
			throw invalid;
		}
		
		//makes sure there is a 0 byte between the filename and mode bytes 
		//checks if bytes are valid ascii values between -127 and 128 inclusive, and that it does not equal 0
		for(int i=2; i< data.length-1; i++) { 
			if(data[i] == 0) { zeroCount++; } 
			if(data[i] >= 128 || data[i] <= -127) { 
				throw invalid;
			} 
			i++; 
		} if(zeroCount <= 1) { throw invalid; } 


		/* Split the string at byte 0 to form 2 pieces: filename, mode*/
		sfn = new String(data).split(new String(delimiter));

		if (sfn.length > 3){
			throw invalid;
		}
		
		String filename, mode;
		filename = getFilename(data);
		mode = getMode(data);
		if(filename.equals("") || !(mode.toLowerCase().equals("octet") || mode.toLowerCase().equals("netascii"))) {
			throw invalid;
		}

		//returns a value which is either a 1 for a read request or a 2 for a write request 
		return data[1]; 
	}
	
	public static void validateDataPacket(byte[] data) throws TFTPExceptions.InvalidTFTPDataException {
		
		TFTPExceptions.InvalidTFTPDataException invalid = 
				new TFTPExceptions().new InvalidTFTPDataException("invalid");
		
		//check the size of the packet
		int size = data.length;
		if(size != dataSize) { 
			throw invalid;
		}
		
		byte[] opcode = new byte[2];
		opcode[0] = data[0];
		opcode[1] = data[1];
		
		//check if the opcode is correct 
		if(opcode[0] != DRQ[0] || opcode[1] != DRQ[1]) {
			throw invalid;
		}
	}

	public static void validateAckPacket(byte[] ack) throws TFTPExceptions.InvalidTFTPAckException {

		TFTPExceptions.InvalidTFTPAckException invalid = 
				new TFTPExceptions().new InvalidTFTPAckException("invalid");

		byte[] opcode = new byte[2];
		opcode[0] = ack[0];
		opcode[1] = ack[1];

		//check if the opcode is correct 
		if(opcode[0] != ARQ[0] || opcode[1] != ARQ[1]) {
			throw invalid;
		}
		
		for(int i = 4; i < ack.length; i++){
			if(ack[i] != 0){
				throw invalid;
			}
		}
	}

	public boolean isDataPacket(byte[] data) {
		if(data[1] != 4) { return false; }
		int bufferSize = 516;
		if(data.length != bufferSize) { return false; }
		return true;
	}

	public static boolean verifyDataPacket(byte[] data, int blockNumber, String[] errorMessage)
	{
		assert((blockNumber >= 0) && (blockNumber <= 65535));


		int dataLength = data.length;

		try
		{
			// Check if the data packet is a valid size
			if (dataLength > 516)
			{
				errorMessage[0] = "Packet too large";
				return false;
			}

			// Check if first byte is 0
			if (data[0] != 0)
			{
				errorMessage[0] = "First byte is not 0";
				return false;
			}

			// Check if next byte is OPCODE_DATA
			if (data[1] != 3)
			{
				errorMessage[0] = "Invalid op code";
				return false;
			}

			return true;
		}
		catch (IndexOutOfBoundsException e)
		{
			errorMessage[0] = "Something is wrong with the packet";
			return false;
		}
	}

	/**
	 * Quick and dirty check of whether a packet is an ACK packet
	 * @param data
	 * @return
	 */
	public static boolean isAckPacket(byte[] data){
		if(data.length == 4){
			if(data[0] == 0 && data[1] == 4){
				return true;
			}
		}
		return false;
	}
	/**
	 * This method is used to check whether it is the last packet 
	 * to be read or written within a current session.
	 * @param data byte[]: The data that is received
	 * @return true if it is the last block of data to be sent false otherwise
	 */
	public static boolean lastPacket(byte [] data){
		boolean result = false;
		for(int i = 0 ; i <data.length; i++){
			if(data[i] == 0){
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * This method is used to extract just the message section of the data received from a DATA Packet
	 * @param data byte[]: The data that is received
	 * @return A byte array containing only the data
	 */
	public static byte[] getData(byte [] data){
		byte[] byteMessage = new byte[data.length-4];
		for(int i = 0 ; i < byteMessage.length; i++){
			byteMessage [i] = data[i+4];			
		}	

		return byteMessage;		
	}

	/**
	 * Returns the filename from a READ or WRITE request
	 * @param data Data portion of the packet
	 * @return String representation of the filename
	 */
	public static String getFilename(byte[] data){
		byte[] byteMessage = new byte[data.length - 2];
		String[] sfn;
		byte[] delimiter = {0};

		/*Get everything past header*/
		for(int i = 0 ; i < byteMessage.length; i++){
			byteMessage [i] = data[i+2];			
		}
		/* Split the string at byte 0 to form 2 pieces: filename, mode*/
		sfn = new String(byteMessage).split(new String(delimiter));

		return sfn[0];
	}

	/**
	 * Returns the mode from a READ or WRITE request
	 * @param data Data portion of the packet
	 * @return String representation of the mode
	 */
	public static String getMode(byte[] data){
		byte[] byteMessage = new byte[data.length - 2];
		String[] sfn;
		byte[] delimiter = {0};

		/*Get everything past header*/
		for(int i = 0 ; i < byteMessage.length; i++){
			byteMessage [i] = data[i+2];			
		}
		/* Split the string at byte 0 to form 2 pieces: filename, mode*/
		sfn = new String(byteMessage).split(new String(delimiter));

		return sfn[1];
	}

	/**
	 * Returns the OP code of a datagram packet as an integer.
	 *
	 * @param packet A TFTP DatagramPacket
	 *
	 * @return the OP code of the TFTP packet
	 */
	public static int getOpCode(byte[] data) {
		byte[] opCodeBytes = new byte[2];

		System.arraycopy(data,0,opCodeBytes,0,2);
		int opCode = (opCodeBytes[1] & 0xFF);
		return opCode;
	}

	/**
	 * This method is used to extract the Block number from the data/ack received
	 * @param data byte[]: The data that is received
	 * @return A byte array containing the Block code
	 */
	public static int getBlockNum(byte[] data){
		byte[] blk = {data[2],data[3]};
		return twoBytesToInt(blk);
	}

	/**
	 * Takes two byte numbers and returns there associated int value
	 * @param leftByte
	 * @param rightByte
	 * @return
	 */
	public static int twoBytesToInt(byte[] b) {
		byte leftByte = b[0];
		byte rightByte = b[1];
		return ((leftByte << 8) & 0x0000ff00) | (rightByte & 0x000000ff);
	}

	/**
	 * Splits an int into two byte values
	 * @param leftByte
	 * @param rightByte
	 * @return
	 */
	public static byte[] intToBytes(int i) {
		byte[] data = new byte[2];

		data[0] = (byte)((i >> 8) & 0xFF);
		data[1] = (byte)(i & 0xFF);

		return data;
	}

	/**
	 * This method is used to display the packet information
	 * 
	 * @param dPacket DatagramPacket: The packet received
	 * @param host String: The host the message is received from
	 * @param isSending boolean: Used to identify if it is a read or write request
	 */
	public void displayPacketInfo(DatagramPacket dPacket, String host, boolean isSending) {
		String direction = isSending ? "sent" : "received";
		String preHost   = isSending ? "To" : "From";

		System.out.println(host + ": Packet " + direction + ":");
		System.out.println(preHost + " host: " + dPacket.getAddress());
		System.out.println("Host port: " + dPacket.getPort());
		int len = dPacket.getLength();
		System.out.println("Length: " + len);
	}

	public static void printTFTPPacketData(byte[] p) {
		int length = p.length;
		System.out.print("[");
		for(int i = 0; i < length; i++) {
			if(i != length-1) {
				System.out.print(p[i] + ", ");
			} else {
				System.out.print(p[i] + "]\n");
			}
		}
	}
	
	//error code 0
	public static DatagramPacket createTimeOutErrorPacket
		(String cs, InetAddress host, int destinationPort) {
		String message = cs + " timed out upon listening for a response.";
		System.out.println(message);
		byte[] errBlock = NotDefined;
		byte[] errData = createError(errBlock, message);
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}

	//error code 1
	public static DatagramPacket createFileAlreadyExistsErrorPacket(String filename, InetAddress host, int destinationPort) {
		String message = "File + " + filename + " already exists.";
		System.out.println(message);
		byte[] errBlock = FileNotFound;
		byte[] errData = createError(errBlock, message);
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}

	//error code 4
	public static DatagramPacket createInvalidBlockErrorPacket(int expected, int found, InetAddress host, int destinationPort) {
		System.out.println("Unexpected block number detected, "
				+ "terminating connection and sending error packet");
		byte[] errBlock = IllegalTFTPOp;
		byte[] errData = createError(errBlock,"Invalid block number detected. Was expecting " 
				+ expected + " but received " + found + ".");
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}

	//error code 4
	public static DatagramPacket createInvalidRequestErrorPacket(byte[] request, InetAddress host, int destinationPort) {
		byte[] errBlock = IllegalTFTPOp;
		byte[] goodRRQ = createReadPacketData("Filename.ext", "octet");
		byte[] goodWRQ = createWritePacketData("Filename.ext", "netascii");
		System.out.println("Invalid request detected.");
		byte[] errData = createError(errBlock,
				"The following are two examples of valid TFTP request operations.\n"
				+ "The first is a write and the second is a read. They both have seperate and all"
				+ "possible values for the mode type.\n"
				+ "Read request example: " + Arrays.toString(goodRRQ) + "\n"
				+ "Write request example:" + Arrays.toString(goodWRQ) + "\n"
				+ "The following is the request packet detected. Notice the difference(s):\n"
				+ "Found request: " + Arrays.toString(request));
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}
	
	//error code 5
	public static DatagramPacket createInvalidTIDErrorPacket(int expected, int found, InetAddress host) {
		System.out.println("Incoming packet deteced to have an unidentifiable TID");
		byte[] errBlock = UnknownTID;
		byte[] errData = createError(errBlock, "Invalid TID detected. Was expecting "
				+ expected + " but found " + found + ".");
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, found);
		return err;
	}
	
	//error code 6
	public static DatagramPacket createFileNotFoundErrorPacket(String filename, InetAddress host, int destinationPort) {
		String message = "File + " + filename + " not found.";
		System.out.println(message);
		byte[] errBlock = FileNotFound;
		byte[] errData = createError(errBlock, message);
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}

	//error packet created when there is an access violation on the files.
	public static DatagramPacket createAccessViolationErrorPacket(String filename, String errMessage, InetAddress host, int destinationPort) {
		System.out.println("Incoming packet detected to have no access violation");
		byte[] errBlock = AccessViolation;
		byte[] errData = createError(errBlock, "Access violation on "+ filename + ". "+errMessage);
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}

	//error packet created when disk is full in the directory.
	public static DatagramPacket createDiskIsFullErrorPacket(String dir, InetAddress host, int destinationPort) {
		System.out.println("Incoming packet detected to be disk full");
		byte[] errBlock = DiskFull;
		byte[] errData = createError(errBlock, "Disk is full "+".");
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}

	//error packet created when received with an Invalid ACK Packet.
	public static DatagramPacket createInvalidAckErrorPacket(byte[] ack, InetAddress host, int destinationPort){
		byte[] expected = new byte[]{0,4,0,4};
		byte[] errBlock = IllegalTFTPOp;
		byte[] errData = createError(errBlock, "Invalid ACK received "+ack+"\nExample of an ACK packet"+expected);
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}

	//error packet created when received with an Invalid DATA Packet.
	public static DatagramPacket createInvalidDataErrorPacket(byte[] data, InetAddress host, int destinationPort){
		byte[] expected = new byte[dataSize + ackSize];
		Arrays.fill(expected, (byte)7);
		expected[0] = 0;
		expected[1] = 3;
		expected[2] = 0;
		expected[3] = 3;
		byte[] errBlock = IllegalTFTPOp;
		byte[] errData = createError(errBlock, "Invalid DATA packet received: "+Arrays.toString(data)+"\n"
				+ "Example of a DATA packet: "+Arrays.toString(expected));
		DatagramPacket err = new DatagramPacket(errData, errData.length, host, destinationPort);
		return err;
	}

	public static String extractMessageFromErrorPacket(byte[] err) {
		byte msg[] = new byte[err.length - 5];
		System.arraycopy(err, 4, msg, 0, err.length - 5);
		return Arrays.toString(msg);
	}

	public static boolean isErrorPacket(byte[] p) {
		return p[1] == 5;
	}
	
	//ERROR HANDLING METHODS
	public static void handleInvalidAckPacket(byte[] data, InetAddress host, int destinationPort, DatagramSocket socket) { //finish?

	}

	public static void handleInvalidDataPacket(byte[] data, InetAddress host, int destinationPort, DatagramSocket socket) { //finish?
		
		//create error packet
		DatagramPacket errorPacket = 
				PacketManager.createInvalidDataErrorPacket(data, host, destinationPort);
		
		//send error packet
		PacketManager.send(errorPacket, socket);
	}
	
	public static void handleInvalidPort(int expected, int found, InetAddress host, DatagramSocket socket) {
		
		//create error packet
		DatagramPacket errorPacket = 
				PacketManager.createInvalidTIDErrorPacket(expected, found, host);
		
		//send error packet
		PacketManager.send(errorPacket, socket);
	}

	public static void handleTimeOut(InetAddress host, int destinationPort, DatagramSocket socket) {
		
		//create error packet
		DatagramPacket errorPacket = 
				PacketManager.createTimeOutErrorPacket("Client", host, destinationPort);
		
		//send error packet
		PacketManager.send(errorPacket, socket);
		
	}
	
	public static void handleInvalidBlockNumber(int expectedBlockNum, int foundBlockNum, InetAddress host, int destinationPort, DatagramSocket socket) {
		
		//create error packet
		DatagramPacket errorPacket = 
				PacketManager.createInvalidBlockErrorPacket(expectedBlockNum, foundBlockNum, host, destinationPort);
		
		//send error packet
		PacketManager.send(errorPacket, socket);
		
	}
	
	public static void handleInvalidRequest(byte[] data, InetAddress host, int destinationPort, DatagramSocket socket) { //finish?

		//create error packet
		DatagramPacket errorPacket = 
				PacketManager.createInvalidDataErrorPacket(data, host, destinationPort);

		//send error packet
		PacketManager.send(errorPacket, socket);
	
	}
	
	public static void handleDiskFull(byte[] data, InetAddress host, int destinationPort, DatagramSocket socket) { //finish?

		//create error packet
		DatagramPacket errorPacket = 
				PacketManager.createInvalidDataErrorPacket(data, host, destinationPort);

		//send error packet
		PacketManager.send(errorPacket, socket);
	
	}
	
	//SENDING AND RECEIVING METHODS
	public static void send(DatagramPacket sendPacket, DatagramSocket socket){ 
		try {
			socket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void receive(DatagramPacket receivePacket, DatagramSocket socket) throws SocketTimeoutException{
		try {
			//block until a datagram is received via sendReceiveSocket  
			socket.receive(receivePacket);
		} catch(SocketTimeoutException e) {
			throw e;
		} catch(IOException e) {
			e.printStackTrace();         
			System.exit(1);
		}
	}
	/*
	 * this is used to create invalid mode and filename errors
	 */
	public static byte[] handler(DatagramPacket p,int pos){
		String[] sfn;
		byte[] delimiter = {0};
		/* Split the string at byte 0 to form 2 pieces: filename, mode*/
		sfn = new String(p.getData()).split(new String(delimiter));
		if(pos == 1){
			sfn[pos] = " ";
		}else if (pos == 2){
			sfn[pos] = "invalidMode";
		}
		//initialize new byte ArrayList
		ArrayList<Byte> temp = new ArrayList<Byte>();
		byte zero = 0;
		//convert sfn[0] and sfn[1] back to bytes
		byte[] left = sfn[1].getBytes();
		byte[] right = sfn[2].getBytes();
		//add the 0 byte to the array
		temp.add(zero);
		//add each byte of left to the array list, this corresponds to the filename
		for(int i = 0; i<left.length; i++) {
			temp.add(left[i]);
		}
		//add 0 to the array list, this corresponds to the 0 byte between the filename and the mode
		temp.add(zero);
		//add each byte of right to the array list, this corresponds to the mode
		for(int j = 0; j<right.length; j++) {
			temp.add(right[j]);
		}
		//add the final zero byte to the array list
		temp.add(zero);
		Byte[] bdata = new Byte[temp.size()];
		byte[] data = new byte[bdata.length];
		bdata = temp.toArray(bdata);
		int k = 0;
		for(Byte b: bdata) {
			data[k++] = b;
		}
		return data;
	}
	
	/**
	 * Checks the path to see if their is enough space to write to disk 
	 * @param path filepath
	 * @return boolean
	 */
	public static boolean diskSpaceCheck(String path, int size){
		File f = new File(path);
		boolean b = f.exists();
		
		if(b){ //if file exists
			long x = f.getFreeSpace();
			if(x > size){
				//if we have enough space to write
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	
	public static int filesize(byte[] file){
		List byteList = Arrays.asList(file);
		
		int x = Collections.frequency(byteList,0);
		return (file.length - x);
	}
}