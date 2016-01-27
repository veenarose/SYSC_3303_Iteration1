import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

public class PacketManager {
	private static byte[] RRQ = {0,1};
	private static byte[] WRQ = {0,2};
	private static byte[] DRQ = {0,3}; //data request
	
	//private static byte validRead[] = {0,3,0,1};
	//private static byte validWrite[] = {0,4,0,0};
	
	private static String[] validModes = {"netascii", "octet"};
	
	public PacketManager(){
		
	}
	
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
	
	public static byte[] createData(byte[] data){
		
		byte dataPack[] = new byte[DRQ.length+data.length]; 
		
		System.arraycopy(DRQ, 0, dataPack, 0, DRQ.length);
		System.arraycopy(data, 0, dataPack, DRQ.length, data.length);
		return dataPack;
		
	}
	
	/* used from tawfic's messageutility class */
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
	72 	 * Validates the data of a packet to determine a valid read or write request. 
	73 	 * @param msg Data to be validated 
	74 	 * @return Int representing the type of message 1 = Read, 2 = Write 
	75 	 * @throws IOException 
	76 	 */ 
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
	 
	public static void main(String[] args) {
		String n = "filename.txt";
		String m = "ocTet";
		
		ByteArrayOutputStream bOutput = new ByteArrayOutputStream(12);
		ByteArrayOutputStream bOutput1 = new ByteArrayOutputStream(12);
		for (int i=1; i<=9; i++){
			bOutput.write(i);
		}
		for (int i=0; i<=9; i++){
			bOutput1.write(i);
		}
		byte block[] = bOutput.toByteArray();
		byte block1[] = bOutput1.toByteArray();
		System.out.println("test" + Arrays.toString(block)+Arrays.toString(block1));
		//System.out.println("test " + Arrays.toString(createData(a)));
		System.out.println("readPack  "+Arrays.toString(createRead(n,m)));
		System.out.println("writePack "+Arrays.toString(createWrite(n,m)));

	}

}