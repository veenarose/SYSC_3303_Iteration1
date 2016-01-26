import java.io.*;
import java.net.*;
import java.util.Arrays;
 
//Server.java
//This class is the server for assignment 1.
//
//by: Damjan Markovic
 
public class Server {
     
    DatagramSocket receiveSocket, sendSocket;
    int readWrite;
     
    public Server()
    {
        try {
             //construct a datagram socket to receive request on port 1025
            //of the localhost
            receiveSocket = new DatagramSocket(1025);
         } catch (SocketException se) {   // Can't create the socket.
             se.printStackTrace();
             System.exit(1);
         }
    }
     
    public void receiveAndReply() {
         
        //construct the packet which will store the request from the client
        byte data[] = new byte[100];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
         
        //receive request from client
        try {  
            receiveSocket.receive(receivePacket);
        } catch(IOException e) {
            e.printStackTrace();         
            System.exit(1);
        }
         
        //print out request packet info
        System.out.println("Server: Packet received:");
        System.out.println("From host: " + receivePacket.getAddress());
        System.out.println("Host port: " + receivePacket.getPort());
        int len = receivePacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: ");
         
        //form a string from the byte array and print out the byte array
        String received = new String(data,0,len);   
        System.out.println(received + "\n");
        System.out.print("As bytes: [");
        for(int i = 0; i < len; i++) {
            if (i == len-1) {
                System.out.print(receivePacket.getData()[i]);
            } else {
                System.out.print(receivePacket.getData()[i] + ",");
            }
        }
        System.out.println("]\n");
         
        //validate the request catch IO exception in the case of an invalid request
        try {
            readWrite = validateRequest(Arrays.copyOfRange(receivePacket.getData(), 0, len));
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        //generate appropriate UDP ack for a read or write request
        byte ack[] = new byte[4];
        if(readWrite == 1) { //read request
            ack[1] = 3;
            ack[3] = 1;
            System.out.println("Server: Read request received sending acknowledgement.");
        } else { //write request
            ack[1] = 4;
            System.out.println("Server: Write request received sending acknowledgement.");
        }
         
        //generate packet to be sent as a response to the clients request
        DatagramPacket respPacket = new DatagramPacket(ack, ack.length, 
                receivePacket.getAddress(), receivePacket.getPort());
         
        //print out response packet info
        System.out.println("Server: Sending response:");
        System.out.println("To host: " + respPacket.getAddress());
        System.out.println("Destination host port: " + respPacket.getPort());
        len = respPacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: ");
        System.out.println(new String(respPacket.getData(),0,len));
         
        //create then close a temporary socket to send the response to the client
        try {
             DatagramSocket respondToClient = new DatagramSocket();
             respondToClient.send(respPacket);
             respondToClient.close();
          
         } catch (IOException e) {
                e.printStackTrace();         
                System.exit(1);
         }
         System.out.println("Response from server relayed to client.\n");
         
    }
     
    //method which parses through a byte array representation of a request
    //and determines whether or not it is a valid UDP read/write request
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
     
    public static void main( String args[] )
    {
       Server h = new Server();
       for(int i = 0; i<11; i++) {
           h.receiveAndReply(); //receive requests and send appropriate replies
       }
    }
}