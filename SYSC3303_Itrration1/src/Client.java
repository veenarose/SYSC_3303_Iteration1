import java.io.*;
import java.net.*;
 
//EchoClient.java
//This class is the client for assignment 1.
//
//by: Damjan Markovic



public class Client {
     
    private DatagramSocket sendReceiveSocket; //socket which sends and receives UDP packets
    private DatagramPacket sendPacket, receivePacket; //UDP send (request) and receive (acknowledgement) packets
    //private PacketManager packMan;
    private static String[] requests = {"read","write"};
    private static String[] modes = {"netascii","octet"};
    
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
         //packMan = new PacketManager();
    }
     
    //method which sends requests to the server. Type true is a read request
    //while type false is a write request.
    public void sendAndReceive(int type, int mode, String filename)
    {
        
    	byte msg[] = new byte[100];
        //Create packet to be sent to server on port 1024 containing the request
        try {
            sendPacket = new DatagramPacket(msg, msg.length,
                                                 InetAddress.getLocalHost(), 1024);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        //Print out request packet info
        /*System.out.println("Client: Sending packet:");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        int len = sendPacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: ");
        System.out.println(new String(sendPacket.getData(),0,len));*/
         
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
    
    public static int validReqInput(String in) {
    	if(in.equals(requests[0])) return 1;
    	else if(in.equals(requests[1])) return 2;
    	else return 0;
    }
    
    public static int validModeInput(String in) {
    	if(in.equals(modes[0])) return 1;
    	else if(in.equals(modes[1])) return 2;
    	else return 0;
    }
    
    public static void main( String args[] ) throws IOException
    {
    	boolean invalidInput = true;
    	System.out.println("Hello and welcome!");
    	//prompt user to specify if the request they are making is either read or write
    	System.out.println("Enter 'read' to read from the server.\n"
    			+ "Enter 'write' to write to a file on the server.");
    	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    	String request = reader.readLine();
    	while(Client.validReqInput(request) == 0) {
    		System.out.println("Please enter either 'read' or 'write'.");
    		request = reader.readLine();
    	}
    	//prompt user to specify the mode which this request will be
    	System.out.println("Enter which mode you would like this request to be.\n"
    			+ "Enter either 'netascii' or 'octet'.");
    	String mode = reader.readLine();
    	while(Client.validModeInput(mode) == 0) {
    		System.out.println("Please enter either 'netascii' or 'octet'.");
    		mode = reader.readLine();
    	}
    	//prompt the user for a filename
    	String restOfMsg = (Client.validReqInput(request) == 1) ? "reading from:" : "writing to:";
    	System.out.println("Enter the name of the file you will be " + restOfMsg);
    	System.out.println("Make sure to include the file's extension!");
    	String filename = reader.readLine();
    	System.out.print("Sending your request");
    	for(int i = 0; i<3; i++) {
    		try {
    		    Thread.sleep(1000);  
    		    System.out.print(".");
    		} catch(InterruptedException ex) {
    		    Thread.currentThread().interrupt();
    		}
    	}
        Client c = new Client(); //create client
        c.sendAndReceive(Client.validReqInput(request), Client.validModeInput(mode), filename);
    }
 
}
