import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Scanner;

public class FixedErrorSimulator {
	private static ProfileData pd = new ProfileData();
	private IOManager ioman; 
	static PacketManager packetManager = new PacketManager(); // The object that controls all the packets transferred
	private int errorSelected = -1;				 //to store in user choice for error code 4
	private int errorSelected2 = -1;
	private int modeSelected = -1;
	private int packetDelay = -1;
	private int errorHost = -1;					 //
	private int errorBlkNum = -1;				 //to store the user selected block number

	public FixedErrorSimulator() {
		ioman = new IOManager();
	}
	
	/*
	 * Creates an invalid read/write request packet
	 */
	private void createInvalidRequestPacket(DatagramPacket packet){

		if (errorSelected == 1){								//setting a large packet
			System.out.println("\nCreated a very large packet.");
			byte[] oldReq = packet.getData();
			byte[] newReq = new byte[1000];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length);
			newReq[newReq.length - 1] = 1;
			packet.setData(newReq);
			System.out.print("Containing: ");
			PacketManager.printTFTPPacketData(packet.getData());
			System.out.println();
		}
		else if (errorSelected == 2){							//setting an invalid opcode error
			System.out.println("\nCreated an invalid opcode packet.");
			byte[] inValid = packet.getData();
			inValid[1] = 8;
			packet.setData(inValid);
			System.out.print("Containing: ");
			PacketManager.printTFTPPacketData(packet.getData());
			System.out.println();
		}
		else if(errorSelected == 3){			//setting an invalid mode error
			System.out.println("\nCreated an invalid mode packet.");
			String invalidMode = "invalidMode";
			String filename = PacketManager.getFilename(packet.getData());
			byte[] f = filename.getBytes();
			byte[] m = invalidMode.getBytes();
			byte[] data = packet.getData();
			byte[] r = new byte[f.length+0+m.length+0];
			data = r;
			packet.setData(data);
			System.out.println("Containing: "+new String(packet.getData()));
			PacketManager.printTFTPPacketData(packet.getData());
			System.out.println();
		}
		else if (errorSelected == 4){			//setting a missing filename error
			System.out.println("\nCreated a missing filename packet.");
			String missFile = "";
			byte[] req = PacketManager.createRead(missFile, PacketManager.getMode(packet.getData()));
			packet.setData(req);
			System.out.println("Containing: "+new String(packet.getData()));
			System.out.print("In Bytes: ");
			PacketManager.printTFTPPacketData(packet.getData());
			System.out.println();
		}
		else if (errorSelected == 5){			//setting a no termination error
			System.out.println("\nCreated a no termination packet.");
			byte[] oldReq = packet.getData();
			byte[] newReq = new byte[oldReq.length+1];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length);
			packet.setData(newReq);
			System.out.print("Containing: ");
			PacketManager.printTFTPPacketData(packet.getData());
			System.out.println();
		}
		else if (errorSelected == 6){			//setting a no termination error
			System.out.println("\nCreated a no ending zero packet.");
			byte[] oldReq = packet.getData();
			byte[] newReq = new byte[oldReq.length-1];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length-1);
			packet.setData(newReq);
			System.out.print("Containing: ");
			PacketManager.printTFTPPacketData(packet.getData());
			System.out.println();
		}
	}
	
	/*
	 * Sends an invalid DATA\ACK packet to the server
	 */
	private void createInvalidDataAckPacket(DatagramPacket packet){
		byte[] data = packet.getData();
		if(errorSelected2 == 1 && errorBlkNum==PacketManager.getBlockNum(data)){				//if client sends too larger packet
			System.out.println("\nCreated a very large packet.");
			byte[] oldReq = packet.getData();
			byte[] newReq = new byte[1000];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length);
			newReq[newReq.length - 1] = 1;
			packet.setData(newReq);
			System.out.println("Containing: "+Arrays.toString(packet.getData()));
		} 
		else if(errorSelected2 == 2 && errorBlkNum==PacketManager.getBlockNum(data)){			//if client sends an invalid opcode
			System.out.println("\nCreated an invalid opcode packet.");
			byte[] inValid = packet.getData();
			inValid[1] = 8;
			packet.setData(inValid);
			System.out.println("Containing: "+Arrays.toString(packet.getData()));
		} 
		else if(errorSelected2 == 3 && errorBlkNum==PacketManager.getBlockNum(data)){			//if client sends an invalid block number on DATA/ACK packet
			System.out.println("\nCreated an invalid block number packet.");
			byte[] blkNum = packet.getData();
			byte lsb_blockNumber = blkNum[3];
			lsb_blockNumber += 1;
			blkNum[3] = lsb_blockNumber;
			packet.setData(blkNum);
			System.out.println("Containing: "+Arrays.toString(packet.getData()));
		}
		else if(errorSelected2 == 4 && errorBlkNum==PacketManager.getBlockNum(data)){			//if client sends an invalid block number on DATA/ACK packet
			System.out.println("\nCreated an unknown TID.");
			createUnknownThread(packet, packet.getAddress(),packet.getPort());
		}
	}
	
	/*
	 * Creates an unknown TID to communicate between client and server
	 */
	private void createUnknownThread(DatagramPacket packet, InetAddress addressTID, int portTID)
	{
		// Thread to handle client request
		Thread unknownTIDThread;

		unknownTIDThread = new Thread(
				new UnknownTIDTransferHandler(packet, addressTID, portTID),
				"Unknown TID Trasfer Handler Thread");

		// Start unknown TID handler thread
		unknownTIDThread.start();
	}
	
	/*
	 * Error Simulation
	 */
	private void startErr() throws IOException
	{
		// Socket used for receiving packets from client
		DatagramSocket receiveSocket = new DatagramSocket(pd.getIntermediatePort());

		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);

		boolean validInput;
		String inputMenu, inputReq, inputData, inputHost, inputNum, inputDelay;
		//get user to select an error code to which an error to be simulated on 
		do
		{
			validInput = true;
			// Iteration 2 Modes 
			System.out.println("\nChoose your preferred mode to simulate an error:");
			System.out.println("	(1) - No error		(Iteration 1)");
			System.out.println("	(2) - Request packets   (Iteration 2)");
			System.out.println("	(3) - DATA/ACK packets  (Iteration 2)");
			System.out.println("	(4) - Delayed packets   (Iteration 3)");
			System.out.println("	(5) - Duplicate packets (Iteration 3)");
			System.out.println("	(6) - Lost packets 	(Iteration 3)");

			inputMenu = keyboard.next();
			modeSelected = Integer.valueOf(inputMenu);
			if(modeSelected >= 1 || modeSelected <7){
				validInput = true;
			}else{
				System.out.println("Please enter a value from 1 to 6, thank you");
				validInput = false;
			}
		} while (!validInput);
		//simulate error code 4 on request packets
		if (modeSelected == 2){
			do
			{
				System.out.println("\nPlease select the type of error from below.");
				System.out.println("	(1) - Packet too large");
				System.out.println("	(2) - Invalid opcode");
				System.out.println("	(3) - Invalid mode");
				System.out.println("	(4) - Missing filename");
				System.out.println("	(5) - No termination after ending 0");
				System.out.println("	(6) - No ending 0");
				inputReq = keyboard.next();

				errorSelected = Integer.valueOf(inputReq);

				if ((errorSelected < 1) || (errorSelected > 6)){
					System.out.println("Please enter a value from 1 to 6, thank you");
					validInput = false;
				}
			}while (!validInput);
		}
		else if (modeSelected == 3){
			do
			{
				validInput = true;
				System.out.println("\nWhich host is going to cause the simulated error.");
				System.out.println("	(1) - Client");
				System.out.println("	(2) - Server");
				inputHost = keyboard.next();

				errorHost = Integer.valueOf(inputHost);
				if ((errorHost < 1) || (errorHost > 2)){
					System.out.println("Please enter a value from 1 to 2, thank you");
					validInput = false;
				}

			}while (!validInput);
			do
			{
				System.out.println("\nPlease select the type of error from below.");
				System.out.println("	(1) - Packet too large");
				System.out.println("	(2) - Invalid opcode");
				System.out.println("	(3) - Invalid block number");
				System.out.println("	(4) - Unknown TID");
				inputData = keyboard.next();

				errorSelected2 = Integer.valueOf(inputData);

				if ((errorSelected2 < 1) || (errorSelected2 > 4)){
					System.out.println("Please enter a value from 1 to 4, thank you");
					validInput = false;
				}

			}while (!validInput);	
			do
			{
				validInput = true;
				System.out.println("Enter the block number of the packet you wish to cause the error.");
				inputNum = keyboard.next();

				errorBlkNum = Integer.valueOf(inputNum);
				if ((errorBlkNum < 0) || (errorBlkNum > 65535)){
					System.out.println("Please enter a value from 0 to 65535, thank you");
					validInput = false;
				}

			}while (!validInput);
		}
		/* If mode selected is 4 - 6user needs to select the ***block number*** for delay and 
		 the ***time in milliseconds** for duplicate or lost packets and ACK. */

		if (modeSelected >= 4 && modeSelected <= 6){

			//************** Get Block number
			do
			{
				validInput = true;
				System.out.println("Enter the block number of the packet you wish to cause the error.");
				inputNum = keyboard.next();

				errorBlkNum = Integer.valueOf(inputNum);// Get block number to mess up from the user 
				if ((errorBlkNum < 0) || (errorBlkNum > 65535)){
					System.out.println("Please enter a value from 0 to 65535, thank you");
					validInput = false;
				}

			}while (!validInput);

			//******************** Get delay time to 
			do{
				validInput = true;
				System.out.println("Enter the delay amount in (ms) ");
				inputDelay = keyboard.next();

				try
				{
					packetDelay = Integer.valueOf(inputDelay);
				}
				catch (NumberFormatException e)
				{
					System.out.println("Please enter a value from " + 0 + " to " + 999999 );
					validInput = false;
				}
				if ((packetDelay < 0) || (packetDelay > 999999))
				{
					System.out.println("Please enter a value from " + 0 + " to " + 999999 );
					validInput = false;
				}
			}while (!validInput);		
		}
		System.out.println("\nError simulator ready..");
		boolean errorSimulation;
		try
		{
			byte[] data = new byte[ioman.getBufferSize()+4];

			while (true)
			{
				// Creates a DatagramPacket to receive request from client
				DatagramPacket clientRequestPacket = new DatagramPacket(data,data.length);

				// Receives response packet through socket
				receiveSocket.receive(clientRequestPacket);
				System.out.println("\nRequest recieved from client");
				System.out.println(new String(clientRequestPacket.getData()));
				System.out.println(Arrays.toString(clientRequestPacket.getData()));

				errorSimulation = (java.lang.Thread.activeCount() == 1);
				// Creates a thread to handle client request
				Thread requestHandlerThread = new Thread(
						new RequestHandler(clientRequestPacket, errorSimulation),
						"Request Handler Thread");

				// Start request handler thread
				requestHandlerThread.start();
			}
		}
		finally
		{
			receiveSocket.close();
		}
	}
	
	/**
	 *	An easy way to handle receive packets from either the server or client
	 */
	public DatagramPacket receivePacket(DatagramSocket socket) throws IOException
	{
		byte[] data = new byte[ioman.getBufferSize()+4];
		DatagramPacket packet = new DatagramPacket(data,data.length);

		do
		{
			try
			{
				socket.receive(packet);
				return packet;
			}
			catch (InterruptedIOException e)
			{
				//do nothing
			}
		} while (true);
	}

	/**
	 * This creates a FixedErrorSimulator object and runs the startErr() method
	 */
	public static void main( String args[] ) throws IOException
	{
		FixedErrorSimulator h = new FixedErrorSimulator();
		System.out.println("Welcome to Error Simulator.");

		h.startErr();
		Scanner loop = new Scanner(System.in);
		while(true){
			System.out.println("\nDo you want to simulate an error (yes/no)? ");
			String s = loop.next();
			if(s.equals("yes") || s.equals("y"))
			{
				h.startErr();
			}
			else if(s.equals("no")|| s.equals("n")){
				System.out.println("Bye.");
				loop.close();
				break;
			}
		}
	}
	
	/**
	 * Thread used to handle client requests
	 */
	class RequestHandler implements Runnable
	{
		private DatagramPacket requestPacket;
		private boolean errorSimulation;

		public RequestHandler(DatagramPacket clientRequestPacket, boolean errorSimulation) {
			this.requestPacket = clientRequestPacket;
			this.errorSimulation = errorSimulation;
		}

		@Override
		public void run() {
			int checkOp = PacketManager.getOpCode(requestPacket.getData());
			// Thread to handle client request
			Thread transferHandlerThread = null;

			// Creates thread corresponding to the operation received
			switch (checkOp)
			{
			case(1):
				transferHandlerThread = new Thread(
						new ReadTransferHandler(requestPacket, errorSimulation),
						"Read Transfer Handler Thread");
				break;
			case(2):
				transferHandlerThread = new Thread(
						new WriteTransferHandler(requestPacket, errorSimulation),
						"Write Transfer Handler Thread");
				break;
			default:
				throw new UnsupportedOperationException();
			}
			// Start request handler thread
			transferHandlerThread.start();
		}

	}
	/**
	 * Thread used to handle client read transfers
	 */
	class ReadTransferHandler implements Runnable
	{
		private DatagramPacket clientRequestPacket;
		private boolean errorSimulation;

		public ReadTransferHandler(DatagramPacket requestPacket, boolean errorSimulation) {
			this.clientRequestPacket = requestPacket;
			this.errorSimulation = errorSimulation;
		}

		public void run() {
			try
			{
				System.out.println("\nRead Transfer Handler thread started.\n");

				// Socket used  sending and receiving packets from server
				DatagramSocket sendReceiveServerSocket = new DatagramSocket();

				// Socket will time out after a couple of seconds
				sendReceiveServerSocket.setSoTimeout(2000);

				// Socket used for sending and receiving packets from client
				DatagramSocket sendReceiveClientSocket = new DatagramSocket();

				// Socket will time out after a couple of seconds
				sendReceiveClientSocket.setSoTimeout(2000);

				// Saves the client TID
				InetAddress clientAddressTID = clientRequestPacket.getAddress();
				int clientPortTID = clientRequestPacket.getPort();

				DatagramPacket serverRequestPacket = new DatagramPacket(clientRequestPacket.getData()
						, clientRequestPacket.getData().length, clientRequestPacket.getAddress(),69);
				
				if(errorSimulation)
					createInvalidRequestPacket(serverRequestPacket);
					
				System.out.println("Packet sent to server");
				sendReceiveServerSocket.send(serverRequestPacket);
				System.out.println(new String(serverRequestPacket.getData()));
				System.out.println(Arrays.toString(serverRequestPacket.getData()));

				// Transfer ID of the server
				InetAddress serverAddressTID = null;
				int serverPortTID = -1;

				// Flag set when transfer is finished
				boolean transferComplete = false;
				// Flag indicating first loop iteration (for setting up TID)
				boolean firstIteration = true;

				try
				{
					while (!transferComplete)
					{
						// Creates a DatagramPacket to receive data packet from server
						// Receives data packet from server
						DatagramPacket dataPacket = receivePacket(sendReceiveServerSocket);
						if (dataPacket == null)
						{
							return;
						}
						System.out.println("\nPacket received from server");
						System.out.println(Arrays.toString(dataPacket.getData()));

						// Saves server TID on first iteration
						if (firstIteration)
						{
							serverAddressTID = dataPacket.getAddress();
							serverPortTID = dataPacket.getPort();
						}
						// Sends data packet to client
						DatagramPacket forwardedDataPacket = new DatagramPacket(dataPacket.getData(),
								dataPacket.getData().length,clientAddressTID,clientPortTID);
						
						if(errorSimulation)
							createInvalidDataAckPacket(forwardedDataPacket);
						
						System.out.println("\nPacket sent back to client");
						sendReceiveClientSocket.send(forwardedDataPacket);
						System.out.println(Arrays.toString(forwardedDataPacket.getData()));

						DatagramPacket ackForFirstSentPacket = receivePacket(sendReceiveClientSocket);
						if (ackForFirstSentPacket == null)
						{
							transferComplete = true;
							return;
						}
						System.out.println("\nPacket received from client");
						System.out.println(new String(ackForFirstSentPacket.getData()));
						System.out.println(Arrays.toString(ackForFirstSentPacket.getData()));
						
						DatagramPacket forwardedAckPacket = new DatagramPacket(ackForFirstSentPacket.getData(),
								ackForFirstSentPacket.getData().length,serverAddressTID,serverPortTID);
						
						System.out.println("\nPacket sent to server");
						sendReceiveServerSocket.send(forwardedAckPacket);
						System.out.println(new String(forwardedAckPacket.getData()));
						System.out.println(Arrays.toString(forwardedAckPacket.getData()));
					}
				}finally
				{
					sendReceiveClientSocket.close();
					sendReceiveServerSocket.close();
				}

			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			finally
			{
				System.out.println("\nRead Transfer Handler thread terminated.\n");
			}
		}
	}
	
	/**
	 * Thread used to handle client write transfers
	 */
	class WriteTransferHandler implements Runnable
	{
		private DatagramPacket clientRequestPacket;
		private boolean errorSimulation;

		public WriteTransferHandler(DatagramPacket requestPacket, boolean errorSimulation) {
			this.clientRequestPacket = requestPacket;
			this.errorSimulation = errorSimulation;
		}

		public void run() {
			try
			{
				System.out.println("\nWrite Thread Handler thread started.\n");

				// Socket used for sending and receiving packets from server
				DatagramSocket sendReceiveServerSocket = new DatagramSocket();

				// Socket will time out after a couple of seconds
				sendReceiveServerSocket.setSoTimeout(2000);

				// Socket used for sending and receiving packets from client
				DatagramSocket sendReceiveClientSocket = new DatagramSocket();

				// Socket will time out after a couple of seconds
				sendReceiveClientSocket.setSoTimeout(2000);

				// Saves the client TID
				InetAddress clientAddressTID = clientRequestPacket.getAddress();
				int clientPortTID = clientRequestPacket.getPort();

				DatagramPacket serverRequestPacket = new DatagramPacket(clientRequestPacket.getData()
						, clientRequestPacket.getData().length, clientRequestPacket.getAddress(),69);
				
				if(errorSimulation)
					createInvalidRequestPacket(serverRequestPacket);
					
				System.out.println("Packet sent to server");
				sendReceiveServerSocket.send(serverRequestPacket);
				System.out.println(new String(serverRequestPacket.getData()));
				System.out.println(Arrays.toString(serverRequestPacket.getData()));

				// Transfer ID of the server
				InetAddress serverAddressTID = null;
				int serverPortTID = -1;

				// Flag set when transfer is finished
				boolean transferComplete = false;
				// Flag indicating first loop iteration (for setting up TID)
				boolean firstIteration = true;

				try
				{
					while (!transferComplete)
					{
						// Creates a DatagramPacket to receive data packet from server
						// Receives data packet from server
						DatagramPacket dataPacket = receivePacket(sendReceiveServerSocket);
						if (dataPacket == null)
						{
							return;
						}
						System.out.println("\nPacket received from server");
						System.out.println(Arrays.toString(dataPacket.getData()));

						// Saves server TID on first iteration
						if (firstIteration)
						{
							serverAddressTID = dataPacket.getAddress();
							serverPortTID = dataPacket.getPort();
						}
						// Sends data packet to client
						DatagramPacket forwardedDataPacket = new DatagramPacket(dataPacket.getData(),
								dataPacket.getData().length,clientAddressTID,clientPortTID);
						
						if(errorSimulation)
							createInvalidDataAckPacket(forwardedDataPacket);
						
						System.out.println("\nPacket sent back to client");
						sendReceiveClientSocket.send(forwardedDataPacket);
						System.out.println(Arrays.toString(forwardedDataPacket.getData()));

						DatagramPacket ackForFirstSentPacket = receivePacket(sendReceiveClientSocket);
						if (ackForFirstSentPacket == null)
						{
							transferComplete = true;
							return;
						}
						System.out.println("\nPacket received from client");
						System.out.println(new String(ackForFirstSentPacket.getData()));
						System.out.println(Arrays.toString(ackForFirstSentPacket.getData()));
						
						DatagramPacket forwardedAckPacket = new DatagramPacket(ackForFirstSentPacket.getData(),
								ackForFirstSentPacket.getData().length,serverAddressTID,serverPortTID);
						
						System.out.println("\nPacket sent to server");
						sendReceiveServerSocket.send(forwardedAckPacket);
						System.out.println(new String(forwardedAckPacket.getData()));
						System.out.println(Arrays.toString(forwardedAckPacket.getData()));
					}
				}finally
				{
					sendReceiveClientSocket.close();
					sendReceiveServerSocket.close();
				}

			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			finally
			{
				System.out.println("\nWrite Thread Handler thread terminated.\n");
			}
		}
		
	}
	class UnknownTIDTransferHandler implements Runnable
	{
		private DatagramPacket packet;
		private InetAddress addressTID;
		private int portTID;
		
		public UnknownTIDTransferHandler(DatagramPacket packet, InetAddress addressTID, int portTID) {
			this.packet = packet;
			this.addressTID = addressTID;
			this.portTID = portTID;
		}

		@Override
		public void run() {
			try
			{
				System.out.println("Unknown TID Transfer Handler thread started.\n");
				// New socket with a different TID than the currently ongoing transfer
				DatagramSocket socket = new DatagramSocket();

				// Sends the packet to the host using this new TID
				socket.send(packet);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
			
		}
		
	}
}
