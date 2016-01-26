import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
 
/**
 *  Tawfic Abdul-Fatah
 *  100720148
 *  SYSC 3303 Assignment #1
 */
public class Server {
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket sendSocket, receiveSocket;
 
    //Used to shutdown the server
    private boolean keepGoing = false;
     
    public Server() {
        try {
            //This socket will be used to receive and handle client datagram requests.
            receiveSocket   = new DatagramSocket(ProfileData.SERVER_PORT_NUM);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        keepGoing = true;
    }
     
    public void listen() throws Exception {
        System.out.println(ProfileData.S_HOST + " starting.");
        // Keep listening till shutdown. For now, that would be via a Ctrl-C via the console
        while(keepGoing) {
            byte[] data = new byte[ProfileData.PACKET_SIZE];
            receivePacket = new DatagramPacket(data, data.length);
             
            try {
                System.out.println(ProfileData.S_HOST + " Waiting . . . . .");
                receiveSocket.receive(receivePacket);
            } catch (IOException e) {
                System.out.println("Problem while receiving a packet . . . Exiting");
                e.printStackTrace();
                System.exit(1);
            }
             
            //If we get to this point, that means we have received a packet.
            MessageUtility.displayPacketInfo(receivePacket, ProfileData.S_HOST, false);
 
            MessageUtility.displayMsgInfo(receivePacket.getData());
             
            System.out.println("Server: data in bytes");
            MessageUtility.displayBytes(receivePacket.getData());
            System.out.println();
             
            //Now we need to
            //1) Validate it
            if(MessageUtility.validate(data)) {
                System.out.println("Packet is valid");
            } else {
                shutDown();
                System.out.println("Invalid Packet . . . . ");
                throw new Exception("Invalid Packet received. Shutting down.");
            }
            //2) Create a response packet as follows:
            //  Packet is a valid read request  ==> send back 0 3 0 1 (exactly four bytes)
            byte[] responseData = new byte[4];
             
            if (MessageUtility.isReadRequest(data)) {
                System.out.println("Server: Creating a response to a READ request");
                responseData[1] = 3;
                responseData[3] = 1;
            //  Packet is a valid write request ==> send back 0 4 0 0 (exactly four bytes)
            } else if (MessageUtility.isWriteRequest(data)) {
                System.out.println("Server: Creating a response to a WRITE request");
                responseData[1] = 4;
            }
 
            //Create a response packet
            sendPacket = new DatagramPacket(responseData,
                                            responseData.length,
                                            receivePacket.getAddress(),
                                            receivePacket.getPort());
 
            //Send the response packet to the originating caller.
            sendSocket = new DatagramSocket();
             
            try {
                sendSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
 
            MessageUtility.displayPacketInfo(sendPacket, ProfileData.S_HOST, true);
             
            System.out.println("Server: data in bytes");
            MessageUtility.displayBytes(sendPacket.getData());
            System.out.println();
             
            //As per the spec, socket used to send back a response will be closed each time.
            sendSocket.close();
        }
    }
 
    //Not used currently
    private void shutDown() {
        System.out.println("Server quitting . . . ");
        keepGoing = false;
        receiveSocket.close();
    }
     
    static public void main(String[] args) throws Exception {
        Server fileServer = new Server();
         
        //Start listening
        fileServer.listen();
    }
}