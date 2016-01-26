import java.net.DatagramPacket;
 
/**
 * 
 * @author tawficabdulfatah
 * Purpose: 
 *      Create valid messages to be used as the data part in a Data gram.
 *      Validate messages.
 *
 */
public class MessageUtility {
    // No constructor is needed.
    private static byte[] RRQ = {0,1};
    private static byte[] WRQ = {0, 2};
    // Number of 0 bytes used in the message
    private static int numberOfZeroBytes = 2;
     
    private static String[] validModes = {"netascii", "octet"};
     
    /**
     * Create a valid read or write message
     * Read request format:
     *      First two bytes are 0 and 1
     *      Then, filename in bytes
     *      Then  0 byte
     *      Then a mode (netascii or octet, any mix of cases, e.g. ocTEt) in bytes
     *      Finally another 0 byte (and nothing else after that!)
     *      
     * Write request format:
     *      Like a read request except:
     *      First two bytes are 0 2
     */
     
    static public byte[] create(boolean isReadMessage, String mode, String fileName) {
        byte[] fileNameInBytes  = fileName.getBytes();
        byte[] modeInBytes      = mode.toLowerCase().getBytes();
        int totalBytes          =   RRQ.length              +
                                    numberOfZeroBytes       +
                                    fileNameInBytes.length  +
                                    modeInBytes.length;
         
        byte[] requestType;
        if (isReadMessage)
            requestType = RRQ;
        else
            requestType = WRQ;
         
        byte[] message = new byte[totalBytes];
         
        //Setting the request bytes
        message[0] = requestType[0];
        message[1] = requestType[1];
         
        //Setting the file name
        System.arraycopy(fileNameInBytes, 0, message, 2, fileNameInBytes.length);
         
        //Setting the mode
        int modeStartIndex = RRQ.length + numberOfZeroBytes - 1 + fileNameInBytes.length; 
        System.arraycopy(modeInBytes, 0, message, modeStartIndex, modeInBytes.length);
         
        return message;
         
    }
    /**
     * Validate message length. This check is needed in many methods.
     */
    static private boolean isValidLength(byte[] message) {
        //message should be at-least 10 bytes.
        //2-0 bytes + mode(min 5) + 2-request type + file name(min 1 byte)
        boolean validateResult = true;
         
        if (message == null || message.length < 10) {
            validateResult = false;
        }
         
        return validateResult;
    }
 
    /**
     * Validate a datagram message
     */
    static public boolean validate(byte[] message) {
        if(!isValidLength(message)) {
            return false;
        }
         
        boolean isValidMessage =    isValidRequestType(message);
         
        //TODO
        //Validate mode
        //Validate fileName position
        //Validate 0 bytes
        return isValidMessage;
    }
    /**
     * Helper/Convenience getters
     */
    public static String getOctMode() {
        return validModes[1];
    }
     
    public static String getNetMode() {
        return validModes[0];
    }
     
    /**
     */
    static private boolean isValidRequestType(byte[] message) {
        return (message[0] == RRQ[0] && message[1] == RRQ[1]) ||
                (message[0] == WRQ[0] && message[1] == WRQ[1]);
    }
     
    /** 
     * Check if Read request
     */
    static public boolean isReadRequest(byte[] message) {
        if(!isValidLength(message)) {
            return false;
        }
 
        return (message[0] == RRQ[0] && message[1] == RRQ[1]);
    }
     
    /** 
     * Check if Write request
     */
    static public boolean isWriteRequest(byte[] message) {
        if(!isValidLength(message)) {
            return false;
        }
 
        return (message[0] == WRQ[0] && message[1] == WRQ[1]);
    }
     
    /**
     * Check if the mode is valid
     * TODO
     */
    static private boolean hasValidMode(String mode) {
        //First, let's check if a mode exists
        int minModeLen = Integer.min(validModes[0].length(), validModes[1].length());
         
        return false;
    }
     
    /**
     * Helper to display the datagram header information
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
     
    /**
     * Helper to display the data part of a datagram
     * @param message
     */
    static public void displayMsgInfo(byte[] message) {
        if (!validate(message)) {
            System.out.println("Invalid message. Cannont display any information");
            return;
        }
         
        int messageLength = message.length;
        String  mode;
        String  fileName;
        int     fileNameStartIndex  = RRQ.length;
        int     fileNameEndIndex    = 0;
        int     modeStartIndex      = RRQ.length;
        int     modeEndIndex        = 0;
 
        System.out.println("Containing: " );
 
        System.out.println("Message Length: " + message.length);
        //Where does the file name end ?
        for (int msgIndex = 2; msgIndex < messageLength; msgIndex++) {
            if (message[msgIndex] == 0) {
                fileNameEndIndex = msgIndex - 1;
                break;
            }
        }
 
        //Where does the mode end ?
        modeStartIndex = fileNameEndIndex + 2;
        for (int msgIndex = modeStartIndex; msgIndex < messageLength; msgIndex++) {
            if (message[msgIndex] == 0) {
                modeEndIndex = msgIndex;
                break;
            }
        }
         
        //System.out.println("fileNameEndIndex: " + fileNameEndIndex);
        byte[] fileNameInBytes  = new byte[fileNameEndIndex - fileNameStartIndex + 1];
        byte[] modeInBytes      = new byte[modeEndIndex - modeStartIndex + 1];
         
        System.arraycopy(message, fileNameStartIndex, fileNameInBytes, 0, fileNameEndIndex - fileNameStartIndex + 1);
         
        System.arraycopy(message, modeStartIndex, modeInBytes, 0, modeEndIndex - modeStartIndex + 1);
         
        fileName = new String(fileNameInBytes);
        mode     = new String(modeInBytes);
         
        System.out.println("First two bytes: " + message[0] + message[1]);
        System.out.println("File Name: " + fileName);
        System.out.println("Mode: " + mode);
    }
     
    static public void displayBytes(byte[] bytes) {
        for (int index = 0; index < bytes.length; index++)
                System.out.print(bytes[index]);
    }
}