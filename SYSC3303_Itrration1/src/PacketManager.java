import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * Class is used to handle the read / write packets between the clients and server
 */
public class PacketManager {
	private static byte[] RRQ = {0,1}; // Read opcode
	private static byte[] WRQ = {0,2}; // Write opcode
	private static byte[] DRQ = {0,3}; //data request

	private static String[] validModes = {"netascii", "octet"};// The modes

	/**
	 * Constructor
	 */
	public PacketManager(){}

	/**
	 * This method is used to create a read request to send to the server
	 *@param message The name of the file
	 *@param mode The valid mode either netascii or octet
	 *@return an array of bytes that sends a request to read a file on the server 
	 */
	public static byte[] createRead(String message, String mode){

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
	public static byte[] createWrite(String message, String mode){

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
	public static byte[] createData(byte[] data, byte[] blockNum){	
		byte block[] = blockNum;
		byte dataPack[] = new byte[DRQ.length+block.length+data.length]; 

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
	 * This method is used to extract the Block number from the data received
	 * @param data byte[]: The data that is received
	 * @return A byte array containing the Block code
	 */
	public static byte[] getBlockNum(byte[] data){
		byte[] blk = {data[2],data[3]};
		return blk;
	}

	/**
	 * This method is used to check whether it is the last packet 
	 * to be read or written within a current session.
	 * @param data byte[]: The data that is received
	 * @return true if it is the last block of data to be sent false otherwise
	 */
	public boolean lastPacket(byte [] data){
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
	 * Quick and dirty check of whether a packet is an ACK packet
	 * @param data
	 * @return
	 */
	public boolean isAckPacket(byte[] data){
		if(data.length == 4){
			return true;
		}
		return false;
	}
	
	/**
	 * This method is used to display the packet information
	 * 
	 * @param dPacket DatagramPacket: The packet received
	 * @param host String: The host the message is received from
	 * @param isSending boolean: Used to identify if it is a read or write request
	 */
	static public void displayPacketInfo(DatagramPacket dPacket, String host, boolean isSending) {
		String direction = isSending ? "sent" : "received";
		String preHost   = isSending ? "To" : "From";

		System.out.println(host + ": Packet " + direction + ":");
		System.out.println(preHost + " host: " + dPacket.getAddress());
		System.out.println("Host port: " + dPacket.getPort());
		int len = dPacket.getLength();
		System.out.println("Length: " + len);
	}

	@SuppressWarnings("unused")
	static private boolean hasValidMode(String mode) {
		int minModeLen = Integer.min(validModes[0].length(), validModes[1].length());

		return false;
	}

	/** 
	 *  Validates the data of a packet to determine a valid read or write request. 
	 *  
	 * @param msg Data to be validated 
	 * @return Int representing the type of message 1 = Read, 2 = Write 
	 * @throws IOException 
	 */ 
	public int validateRequest(byte[] msg) throws IOException {
		IOException invalid = new IOException(); 

		//checks leading 0 byte and read/write request byte
		if(msg[0] != 0 || (msg[1] != 1 && msg[1] != 2)) { 
			throw invalid; 
		} 

		//checks if final byte is 0
		if(msg[msg.length-1] != 0) { 
			throw invalid; 
		} 

		int i = 2; 

		int zeroCount = 0; 

		//makes sure there is a 0 byte between the filename and mode bytes 
		//checks if bytes are valid ascii values between 0 and 128 
		while(i < msg.length-1) { 
			System.out.println(msg[i]); 
			if(msg[i] == 0) { zeroCount++; } 
			if(msg[i] >= 128) { throw invalid; } 
			i++; 
		} if(zeroCount != 1) { throw invalid; } 


		//returns a value which is either a 1 for a read request or a 2 for a write request 
		return msg[1]; 
	}  

}
