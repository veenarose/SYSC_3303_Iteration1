
public class ProfileData {
	private static final int serverPort = 69;
	private static final int errorSimulator = 68;
	private final static int timeOut = 2000;
	private final static int repeats = 5;
	
	public static int getServerPort() {
		return serverPort;
	}
	public static int getErrorPort() {
		return errorSimulator;
	}
	public static int getTimeOut() {
		return timeOut;
	}
	public static int getRepeats() {
		return repeats;
	}
}
