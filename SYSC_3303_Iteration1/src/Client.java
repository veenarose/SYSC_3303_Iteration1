import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
 
/**
 * 
 *  Tawfic Abdul-Fatah
 *  100720148
 *  SYSC 3303 Assignment #1
 *
 *  Algorithms:
 *      Create a DatagramPacket packet (Read or Write)
 *      Print the packet information
 *      Send the packet to a known port on the intermediate host.
 *      Wait on the DatagramSocket
 *      Receive a DatagramPacket
 *      Print the packet information
 */
public class Client {
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket socket;
     
    //File names used for read/write requests
    private static String[] fileNames = {   "c:\\fileNameOne",
            "d:\\dirOne\\fileNameOne",
            "c:\fileNameTwo",
            "d:\\dirOne\\fileNameTwo",
            "c:\\fileNameThree",
            "d:\\dirOne\\fileNameThree",
            "c:\\fileNameFour",
            "d:\\dirOne\\fileNameFour",
            "c:\\fileNameFive",
            "d:\\dirOne\\fileNameFive"
        };
 
    public Client() {
        try {
            //Creating a datagram socket and binding it to any available port.
            //The socket will be used to send and receive datagram packets.
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
     
    public void sendAndReceive(byte[] message) {
        try {
            sendPacket = 
                    new DatagramPacket(message, message.length, InetAddress.getLocalHost(),
                            ProfileData.INTERMEDIATE_PORT_NUM);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        System.out.println("Client. Before sending packet");
        MessageUtility.displayPacketInfo(sendPacket, ProfileData.C_HOST, true);
 
        MessageUtility.displayMsgInfo(sendPacket.getData());
 
        System.out.println("Client:  data in bytes");
        MessageUtility.displayBytes(sendPacket.getData());
        System.out.println();
 
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        System.out.println("Client: Packet sent.\n");
         
        //Now we wait for a response from the the server
        // Construct a DatagramPacket for receiving packets up MessageUtility.PACKET_SIZE
        byte serverResponseData[] = new byte[ProfileData.PACKET_SIZE];
        receivePacket = new DatagramPacket(serverResponseData, serverResponseData.length);
 
        try {
            //Block until a datagram is received via socket.  
             socket.receive(receivePacket);
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
 
        System.out.println("Client: received response.\n");
        MessageUtility.displayPacketInfo(receivePacket, ProfileData.C_HOST, false);
        System.out.println("Containing: ");
         
        MessageUtility.displayBytes(receivePacket.getData());
        System.out.println();
    }
     
    private void quit() {
        System.out.println("Client quitting . . . ");
        socket.close();
    }
     
    public static void main(String[] args) {
        //Toggle between read/write request. 10 times
        //Request #11 is an invalid one
         
        Client fileClient = new Client();
        //Send 10 valid requests to the intermediate host toggling between write/read request
        //using different filenames.
        byte[] message = null;
 
        //starting with a read request and then toggling between read and write
        boolean isReadRequest = true;
        //toggling modes
        String mode = MessageUtility.getOctMode();
         
        for (String fileName : fileNames) {
            message = MessageUtility.create(isReadRequest, mode, fileName);
            fileClient.sendAndReceive(message);
             
            //Let's toggle
            isReadRequest = !isReadRequest;
            if (mode.equalsIgnoreCase(MessageUtility.getOctMode()))
                mode = MessageUtility.getNetMode();
            else
                mode = MessageUtility.getOctMode();
            //Wait two seconds between requests
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
         
        //For the eleven'th request, we want to test an invalid request code E.g. 04
        //using the last message
        message[1] = 4;
        fileClient.sendAndReceive(message);
        //The client will freeze after this request since the Server will throw an exception.
        //As per the instructor's note, we're not supposed to deal with it at this point.  
        fileClient.quit();
    }
}