import java.util.Arrays;

public class PacketManager {
	private static byte[] RRQ = {0,1};
	private static byte[] WRQ = {0,2};
	private static byte validRead[] = {0,3,0,1};
	private static byte validWrite[] = {0,4,0,0};
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
	
	public byte[] createData(){
		return null;
		
	}
	
	@SuppressWarnings("unused")
	static private boolean hasValidMode(String mode) {
		int minModeLen = Integer.min(validModes[0].length(), validModes[1].length());
		
		return false;
	}
	
	public static void main(String[] args) {
		String n = "filename.txt";
		String m = "ocTet";
		
		System.out.println("readPack  "+Arrays.toString(createRead(n,m)));
		System.out.println("writePack "+Arrays.toString(createWrite(n,m)));

	}

}
