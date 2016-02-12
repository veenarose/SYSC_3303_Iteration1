import java.io.IOException;

public class tester {

	public static void main( String args[] ) throws IOException
    {
		PacketManager pm = new PacketManager();
		byte b1 = 0; byte b2 = -111;
		System.out.println(pm.twoBytesToInt(b1, b2));
		System.out.println(pm.intToBytes(128)[0] + " " + pm.intToBytes(128)[1]);
    }
	
}
