import java.io.*;
import java.net.*;

/**
 * Client.java
 * a basic client class used for communicating with a server using the TFTP protocol
**/

public class Client { //the client class 
     
    private DatagramSocket sendReceiveSocket; //socket which sends and receives UDP packets
    private DatagramPacket sendPacket, receivePacket; //UDP send (request) and receive (acknowledgement) packets
    
    private PacketManager packMan = new PacketManager(); //instance of the PacketManager class, used to handle read/write packets
    private IOManager ioMan = new IOManager(); //instance of the IOManager class, used to read/write to files
    
    private final static String[] requests = {"read","write"};  //the valid requests which can be made by the server
    private final static String[] modes = {"netascii","octet"}; //the valid modes for the corresponding requests
    
    /**
     * client constructor
     */
    public Client()
    {
         try {
             //construct a datagram socket to be used to send and receive UDP Datagram requests and bind it to any available 
             //port on the local host machine. 
             sendReceiveSocket = new DatagramSocket();
         } catch (SocketException se) {   //unable to create socket
             se.printStackTrace();
             System.exit(1);
         }
    }
    
    /**
     * private method used by the client to send a packet over a socket
     * @param sendPacket
     * @param socket
     */
    private void sendPacket(DatagramPacket sendPacket, DatagramSocket socket){ 
    	try {
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * private method used by the client to receive packets on a socket
     * @param receivePacket
     * @param socket
     */
    private void receivePacket(DatagramPacket receivePacket, DatagramSocket socket){
    	try {
    		//block until a datagram is received via sendReceiveSocket  
    		socket.receive(receivePacket);
        } catch(IOException e) {
        	e.printStackTrace();         
        	System.exit(1);
        }
    }
    
    /**
     * client's method for sending and receiving requests
     * @param req 
     * can take value of either 1 (for a read request) or 2 (for a write request)
     * @param mode
     * can take value of either 'netascii' or 'octet'
     * @param filename
     * name of the file to be either read from or written to
     */
    public void sendAndReceive(int req, int mode, String filename)
    {
        
    	byte request[]; //the request 
    	
    	//HANDLING THE REQUEST
    	
    	if(req == 1) { //a read request
        	request = packMan.createRead(filename, modes[mode]);
        } else { //a write request
        	request = packMan.createWrite(filename, modes[mode]);
        }
        
    	//SENDING THE REQUEST
    	
    	//create packet to be sent to server on port 1024 
    	//containing the request
        try {
            sendPacket = new DatagramPacket(request, request.length,
                                                 InetAddress.getLocalHost(), 1024);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
         
        //send the request packet to the server
        sendPacket(sendPacket, sendReceiveSocket);
        System.out.println("Client: Request sent.\n");
        
        //RECEIVING THE REQUEST AND SUBSEQUENTLY HANDLING IT
        
        int serverPort; //variable to store the port of the sever
        InetAddress serverHost; //variable to store the address of the server's host
        
        if (req == 1) { //a read request
        	
        	//variable to store the read requests data, 2 bytes for the opcode, 2 for the block number, 512 for the raw data
        	byte readData[] = new byte[ioMan.getBufferSize() + 4]; 
        	
        	//receive data from the server
        	receivePacket = new DatagramPacket(readData, readData.length);
        	receivePacket(receivePacket, sendReceiveSocket);
        	
        	//write the data to local file with name filename
        	byte writeToFileData[];
        	writeToFileData = packMan.getData(readData);
        	try {
				ioMan.write(filename, writeToFileData);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	serverHost = receivePacket.getAddress(); //get the host address from the server
        	serverPort = receivePacket.getPort(); //get the port id
        	
        	//send ack packets and receive data until last packet is reached
        	while (!packMan.lastPacket(readData)) { 
        		
        		//create ack
        		byte[] ack = packMan.createAck(readData);
        		
        		//send the ack packet
        		sendPacket = new DatagramPacket(ack, ack.length,
				                                     serverHost, serverPort);
        		sendPacket(sendPacket, sendReceiveSocket);
        		
        		//receive data from server
        		receivePacket = new DatagramPacket(readData, readData.length);
        		receivePacket(receivePacket, sendReceiveSocket);
            	
            	//write the data to the file
            	writeToFileData = packMan.getData(readData);
            	try {
					ioMan.write(filename, writeToFileData);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		
        	} //end read request loop
        	
        	
        } else { //a write request
        	
        	//byte buffer for incoming ack packets
        	byte[] ackData = new byte[4];
        	
        	//to be incremented by 512 for each read of the file
        	int bufferOffset = 0; 
        	
        	//receive the ack packet from the server
        	receivePacket = new DatagramPacket(ackData, ackData.length);
        	receivePacket(receivePacket, sendReceiveSocket);
        	
        	//byte buffer to be filled with data from local file
        	byte readFromFileData[]; 
        	
        	//byte buffer for write requests
        	byte writeData[] = new byte[516]; 
        	
        	//block number 1, to be used in sent udp write packet
        	byte[] block1 = new byte[2]; 
        	block1[0] = 0;
        	block1[1] = 1; //block number 1
        	serverHost = receivePacket.getAddress(); //get the host address from the server
        	serverPort = receivePacket.getPort(); //get the port id
        	
        	//read 512 bytes from local file and create a data packet to send these to-be-written bytes to the server
        	try {
				readFromFileData = ioMan.read(filename, bufferOffset); //read 512 bytes from file
				writeData = packMan.createData(readFromFileData, block1); //creates proper write data udp packet
				
        	} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	//increment the buffer read offset by 512
        	bufferOffset += ioMan.getBufferSize(); 
        	
        	//send the packet containing the data to be written to the server
			sendPacket = new DatagramPacket(writeData, writeData.length, serverHost, serverPort); 
        	sendPacket(sendPacket, sendReceiveSocket);
        	
        	//receive ack packets and send data until there is no more data to send
			while (!packMan.lastPacket(writeData)) { 
        		
        		//wait to receive the acknowledgement just sent
        		//wait for a response from the server
        		receivePacket = new DatagramPacket(ackData, ackData.length);
        		receivePacket(receivePacket, sendReceiveSocket);
        		
        		//get the two-byte block number from the ack and convert it to an int
            	int blockNumAsInt = packMan.twoBytesToInt(ackData[2], ackData[3]); 
        		blockNumAsInt++; //increment the block number
        		//convert int back to two bytes
        		byte blockNum[] = packMan.intToBytes(blockNumAsInt);
        		
        		//read 512 bytes from local file and create a data packet to send these to-be-written bytes to the server
        		try {
    				readFromFileData = ioMan.read(filename, bufferOffset); //read 512 bytes from file
    				writeData = packMan.createData(readFromFileData, blockNum); //creates proper write data udp packet
            	} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        		
        		//increment the buffer read offset by 512
        		bufferOffset += ioMan.getBufferSize();
        		
        		//send data to-be-written on the server
        		sendPacket = new DatagramPacket(writeData, writeData.length, serverHost, serverPort); //the packet to be sent
        		sendPacket(sendPacket, sendReceiveSocket);
        		
        	} //end write request loop
        
        }//end handle else write request
    
    }//end of sendAndReceive
    
    /**
     * a method which checks if an input string matches the strings "read" or "write" (the valid request types)
     * returns 1 for a read request match, 2 for a write request match and 0 on invalid input
     * @param in
     * @return
     */
    public static int validReqInput(String in) {
    	if(in.equals(requests[0])) return 1;
    	else if(in.equals(requests[1])) return 2;
    	else return 0;
    }
    
    /**
     * a method which checks if an input string matches the strings "netascii" or "octet" (valid UDP read/write request modes)
     * returns 0 for a netascii match, 1 for an octet match, and -1 on invalid input
     * @param in
     * @return
     */
    public static int validModeInput(String in) {
    	if(in.equals(modes[0])) return 0;
    	else if(in.equals(modes[1])) return 1;
    	else return -1;
    }
    
    public static void main( String args[] ) throws IOException
    {
    	
    	System.out.println("Hello and welcome!");
    	//prompt user to specify if the request they are making is either read or write
    	System.out.println("Enter 'read' to read from the server.\n"
    			+ "Enter 'write' to write to a file on the server.");
    	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    	String request = reader.readLine(); //read user input by line
    	
    	while(Client.validReqInput(request) == 0) { //while input is not 'read' or 'write'
    		System.out.println("Please enter either 'read' or 'write'.");
    		request = reader.readLine();
    	}
    	
    	//prompt user to specify the mode of the request
    	System.out.println("Enter which mode you would like this request to be.\n"
    			+ "Enter either 'netascii' or 'octet'.");
    	String mode = reader.readLine(); //read user input by line
    	
    	while(Client.validModeInput(mode) == -1) { //while input is not 'netascii' or 'octet'
    		System.out.println("Please enter either 'netascii' or 'octet'.");
    		mode = reader.readLine();
    	}
    	
    	//prompt the user for a filename
    	String restOfMsg = (Client.validReqInput(request) == 1) ? "reading from:" : "writing to:";
    	System.out.println("Enter the name of the file you will be " + restOfMsg);
    	System.out.println("Make sure to include the file's extension!");
    	String filename = reader.readLine();
    	System.out.print("Sending your request");
    	
    	Client c = new Client(); 
        c.sendAndReceive(Client.validReqInput(request), Client.validModeInput(mode), filename);
    
    } //end of main
 
} //end of client class