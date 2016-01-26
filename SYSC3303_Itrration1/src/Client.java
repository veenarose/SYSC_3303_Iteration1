import java.io.*;
import java.net.*;
 
//EchoClient.java
//This class is the client for assignment 1.
//
//by: Damjan Markovic
 
public class Client {
     
    DatagramSocket sendReceiveSocket; //socket which sends and receives UDP packets
    DatagramPacket sendPacket, receivePacket; //UDP send (request) and receive (acknowledgement) packets
     
    public Client()
    {
         try {
             // Construct a datagram socket and bind it to any available 
             // port on the local host machine. This socket will be used to
             // send and receive UDP Datagram packets.
             sendReceiveSocket = new DatagramSocket();
         } catch (SocketException se) {   // Can't create the socket.
             se.printStackTrace();
             System.exit(1);
         }
    }
     
    //method which sends requests to the server. Type true is a read request
    //while type false is a write request.
    public void send(Boolean type)
    {
        //Specify filename and initialize UDP read/write packet
        String filename = "generic.txt";
        byte msg[] = new byte[4 + "octet".length() + filename.length()];
         
        if (type) {
            msg[1] = 1;
            System.out.println("Client: sending a request to read " + filename + "\n");
            } 
        else {
            msg[1] = 2;
            System.out.println("Client: sending a request to write to " + filename);
            }
        int i = 2;
        byte fn[] = filename.getBytes();
        while (i < 2 + fn.length) {
            msg[i] = fn[i-2];
            i++;
        }
        byte mode[] = "octet".getBytes();
        i++;
        int j = i;
        while (i < j + mode.length) {
            msg[i] = mode[i-j];
            i++;
        }
         
        //Create packet to be sent to server on port 1024 containing the request
        try {
            sendPacket = new DatagramPacket(msg, msg.length,
                                                 InetAddress.getLocalHost(), 1024);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        //Print out request packet info
        System.out.println("Client: Sending packet:");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        int len = sendPacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: ");
        System.out.println(new String(sendPacket.getData(),0,len));
         
        //Send the request packet to the server
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Client: Packet sent.\n");
    }
     
    //method which receives a reply from the server
    public void receive() {
          
         //construct a DatagramPacket for receiving packets up 
         //to 100 bytes long (the length of the byte array)
         byte data[] = new byte[100];
         receivePacket = new DatagramPacket(data, data.length);
 
         try {
            //block until a datagram is received via sendReceiveSocket  
            sendReceiveSocket.receive(receivePacket);
         } catch(IOException e) {
            e.printStackTrace();         
            System.exit(1);
         }
          
         //process the received datagram and print out data as bytes
         System.out.println("Client: Response packet received:");
         System.out.println("From host: " + receivePacket.getAddress());
         System.out.println("Host port: " + receivePacket.getPort());
         int len = receivePacket.getLength();
         System.out.println("Length: " + len);
         System.out.print("Containing: ");
         for(int i = 0; i < len; i++) {
             System.out.print(data[i]);
         }
         System.out.println("\n");
    }
     
    //method which sends error request to the server
    public void sendError() {
         
        //Generate error packet which is neither a write or read request
        System.out.println("ERROR SENT");
        try {
            sendPacket = new DatagramPacket(new byte[100], 100,
                                                 InetAddress.getLocalHost(), 1024);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        //Print out packet info
        System.out.println("Client: Sending packet:");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        int len = sendPacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: ");
        System.out.println(new String(sendPacket.getData(),0,len));
         
    }
     
    public static void main( String args[] )
    {
       Client c = new Client(); //create client
       for(int i = 0; i<5; i++) { //send and receive a response for 5 read and 5 write requests
           c.send(true); //send a read request
           c.receive(); //listen for the response
           c.send(false); //send a write request
           c.receive(); //listen for the response
       }
       c.sendError(); //send an error message
       c.receive(); //listen for the response to the error
    }
 
}