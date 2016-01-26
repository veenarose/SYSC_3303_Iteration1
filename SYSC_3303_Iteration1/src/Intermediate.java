import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
 
/*
        * Tawfic Abdul-Fatah
        * 100720148
        * SYSC 3303 Assignment #1
*/
public class Intermediate {
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket receiveSocket, sendSocket, sendAndReceiveSocket;
    //Used to shutdown the server
    private boolean keepGoing = false;
     
    //Used to save the port of the client so we can send it the server's response.
    private int clientPort = 0;
     
    public Intermediate() {
        try {
            //Used to receive requests.
            receiveSocket = new DatagramSocket(ProfileData.INTERMEDIATE_PORT_NUM);
             
            //Don't bind it to any specific port
            sendAndReceiveSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        keepGoing = true;
    }
     
    public void listen() {
        System.out.println(ProfileData.I_HOST +  " starting.");
        // Keep listening till shutdown. For now, that would be via a Ctrl-C via the console
        while(keepGoing) {
            byte[] data = new byte[ProfileData.PACKET_SIZE];
            receivePacket = new DatagramPacket(data, data.length);
             
            try {
                System.out.println(ProfileData.I_HOST + " Waiting . . . .");
                receiveSocket.receive(receivePacket);
            } catch (IOException e) {
                System.out.println(ProfileData.I_HOST + " Problem while receiving a packet . . . Exiting");
                e.printStackTrace();
                System.exit(1);
            }
 
            //If we get to this point, that means we have received a packet.
            MessageUtility.displayPacketInfo(receivePacket, ProfileData.I_HOST, false);
 
            MessageUtility.displayMsgInfo(receivePacket.getData());
             
            System.out.println(ProfileData.I_HOST + ": data in bytes");
            MessageUtility.displayBytes(receivePacket.getData());
            System.out.println();
             
            //Save the client port number to be used later
            clientPort = receivePacket.getPort();
             
            //Form a packet to send containing exactly what was received
            sendPacket = new DatagramPacket(receivePacket.getData(),
                                            receivePacket.getData().length,
                                            receivePacket.getAddress(),
                                            ProfileData.SERVER_PORT_NUM);
 
            //Print the sendPacket information.
            MessageUtility.displayPacketInfo(sendPacket, ProfileData.I_HOST, true);
 
            MessageUtility.displayMsgInfo(sendPacket.getData());
             
            System.out.println(ProfileData.I_HOST + ": data in bytes");
            MessageUtility.displayBytes(sendPacket.getData());
            System.out.println();
 
            //The host sends this packet on its send/receive socket to port 69
            try {
                sendAndReceiveSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
 
            //Wait to receive a response
            byte[] serverResponseData = new byte[ProfileData.PACKET_SIZE];
            receivePacket = new DatagramPacket(serverResponseData, serverResponseData.length);
 
            try {
                //Block until a datagram is received via sendAndReceiveSocket.  
                sendAndReceiveSocket.receive(receivePacket);
            } catch(IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            //Print the receivePacket information.
            MessageUtility.displayPacketInfo(receivePacket, ProfileData.I_HOST, false);
 
            System.out.println(ProfileData.I_HOST + ": data in bytes");
            MessageUtility.displayBytes(receivePacket.getData());
            System.out.println();
 
            //Form a packet to send back to the client sending the request
            sendPacket = new DatagramPacket(receivePacket.getData(),
                                            receivePacket.getData().length,
                                            receivePacket.getAddress(),
                                            clientPort);
 
            //Print the sendPacket information.
            System.out.println(ProfileData.I_HOST + " sendPacket . . response to Client");
            MessageUtility.displayPacketInfo(sendPacket, ProfileData.I_HOST, true);
 
            System.out.println(ProfileData.I_HOST + ": data in bytes");
            MessageUtility.displayBytes(sendPacket.getData());
            System.out.println();
 
            //Used and closed immediately after to send a response back to the client
            try {
                sendSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
                System.exit(1);
            }
             
            try {
                sendSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
             
            //Done with this socket, for one request
            sendSocket.close();
        }
    }
     
    //Not used currently
    private void shutDown() {
        System.out.println(ProfileData.I_HOST + " quitting . . . ");
        keepGoing = false;
        receiveSocket.close();
        sendAndReceiveSocket.close();
    }
     
    static public void main(String[] args) {
        Intermediate intermediateHost = new Intermediate();
         
        //Start listening
        intermediateHost.listen();
    }
}