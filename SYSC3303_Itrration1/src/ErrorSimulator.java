import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

public class ErrorSimulator {
	DatagramSocket receiveSocket;
	private static ProfileData pd = new ProfileData();
	private IOManager ioman; 
	static PacketManager packetManager = new PacketManager(); // The object that controls all the packets transferred
	private int errorSelected = -1;				 			//to store in user choice for error code 4
	private int errorSelected2 = -1;
	private int modeSelected = -1;
	private int errorHost = -1;					
	private int errorBlkNum = -1;				 			//to store the user selected block number
	private int delayedPack = -1;

	public ErrorSimulator() {
		ioman = new IOManager();
		// Socket used for receiving packets from client
		try {
			receiveSocket = new DatagramSocket(pd.getIntermediatePort());
		} catch (SocketException e) {
			e.printStackTrace();
		}
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
			byte[] data = PacketManager.handler(packet,2);
			packet.setData(data);
			System.out.println("Containing: "+new String(packet.getData()));
			PacketManager.printTFTPPacketData(packet.getData());
			System.out.println();
		}
		else if (errorSelected == 4){			//setting a missing filename error
			System.out.println("\nCreated a missing filename packet.");
			byte[] data = PacketManager.handler(packet,1);
			packet.setData(data);
			System.out.println("Containing: "+new String(packet.getData()));
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
		if(errorSelected2 == 1 && errorBlkNum==PacketManager.getBlockNum(data)){				//create a too larger packet
			System.out.println("\nCreated a very large packet.");
			byte[] oldReq = packet.getData();
			byte[] newReq = new byte[1000];
			System.arraycopy(oldReq, 0, newReq, 0, oldReq.length);
			newReq[newReq.length - 1] = 1;
			packet.setData(newReq);
			System.out.println("Containing: "+Arrays.toString(packet.getData()));
		} 
		else if(errorSelected2 == 2 && errorBlkNum==PacketManager.getBlockNum(data)){			//creates an invalid opcode
			System.out.println("\nCreated an invalid opcode packet.");
			byte[] inValid = packet.getData();
			inValid[1] = 8;
			packet.setData(inValid);
			System.out.println("Containing: "+Arrays.toString(packet.getData()));
		} 
		else if(errorSelected2 == 3 && errorBlkNum==PacketManager.getBlockNum(data)){			//creates an invalid block number on DATA/ACK packet
			System.out.println("\nCreated an invalid block number packet.");
			byte[] blkNum = packet.getData();
			byte lsb_blockNumber = blkNum[3];
			lsb_blockNumber += 1;
			blkNum[3] = lsb_blockNumber;
			packet.setData(blkNum);
			System.out.println("Containing: "+Arrays.toString(packet.getData()));
		}
		else if(errorSelected2 == 4 && errorBlkNum==PacketManager.getBlockNum(data)){			//creates an Unknown TID
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
		Scanner keyboard = new Scanner(System.in);

		boolean validInput;
		String inputMenu, inputReq, inputData, inputHost, inputNum, typeDelay;
		//get user to select an error code to which an error to be simulated on 
		do
		{
			validInput = true;
			// Iteration 2 Modes 
			System.out.println("\nChoose your preferred mode to simulate an error:");
			System.out.println("	(1) - No error		(Iteration 1)");
			System.out.println("	(2) - Request packets   (Iteration 2)");
			System.out.println("	(3) - DATA/ACK packets  (Iteration 2)");
			System.out.println("	(4) - Unknown TID  	(Iteration 2)");
			System.out.println("	(5) - Delayed packets   (Iteration 3)");
			System.out.println("	(6) - Duplicate packets (Iteration 3)");
			System.out.println("	(7) - Lost packets 	(Iteration 3)");

			inputMenu = keyboard.next();
			modeSelected = Integer.valueOf(inputMenu);
			if(modeSelected >= 1 || modeSelected <8){
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
				inputData = keyboard.next();

				errorSelected2 = Integer.valueOf(inputData);

				if ((errorSelected2 < 1) || (errorSelected2 > 3)){
					System.out.println("Please enter a value from 1 to 3, thank you");
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
		/* If mode selected is 4 - 7 user needs to select the ***block number*** for 
		 * data delayed, duplicate or lost packets and ACK. */

		else if (modeSelected >=4 && modeSelected <= 7){
			do
			{
				validInput = true;
				System.out.println("\nWhich packet is going to cause the simulated error.");
				System.out.println("	(1) - RRQ/WRQ packets");
				System.out.println("	(2) - DATA/ACK packets");
				typeDelay = keyboard.next();

				delayedPack = Integer.valueOf(typeDelay);
				if ((delayedPack < 1) || (delayedPack > 2)){
					System.out.println("Please enter a value from 1 to 2, thank you");
					validInput = false;
				}

			}while (!validInput);

			//************** Get Block number
			if(delayedPack == 2){
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
					validInput = true;
					System.out.println("Enter the block number of the packet you wish to cause the error.");
					inputNum = keyboard.next();

					errorBlkNum = Integer.valueOf(inputNum);// Get block number to mess up from the user 
					if ((errorBlkNum < 0) || (errorBlkNum > 65535)){
						System.out.println("Please enter a value from 0 to 65535, thank you");
						validInput = false;
					}

				}while (!validInput);
			}
		}

		System.out.println("\nError simulator ready..");
		boolean errorSimulation = true;
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
			keyboard.close();
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
		ErrorSimulator h = new ErrorSimulator();
		System.out.println("Welcome to Error Simulator.");
		h.startErr();
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

			// Creates thread corresponding to the opcode received
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

		@SuppressWarnings("resource")
		public void run() {
			try
			{
				System.out.println("\nRead Transfer Handler thread started.");

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
						, clientRequestPacket.getData().length, clientRequestPacket.getAddress(),pd.getServerPort());

				if(errorSimulation)
					createInvalidRequestPacket(serverRequestPacket);
				//request delayed error on RRQ/WRQ
				if(modeSelected == 5 && delayedPack == 1){
					System.out.println("\nClient resends RRQ packet...");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("\nPacket sent to server");
					sendReceiveServerSocket.send(serverRequestPacket);
					System.out.println(new String(serverRequestPacket.getData()));
					System.out.println(Arrays.toString(serverRequestPacket.getData()));

					//get the response back from server
					DatagramPacket repDATA = receivePacket(sendReceiveServerSocket);
					if(repDATA == null) return;

					//DATA packet received from Server
					System.out.println("\nPacket received from server");
					sendReceiveServerSocket.send(repDATA);
					System.out.println(Arrays.toString(repDATA.getData()));

					//create an unknown TID socket
					DatagramSocket TIDsocket = new DatagramSocket();

					DatagramPacket forwardedFirstDataPacket = new DatagramPacket(repDATA.getData(),
							repDATA.getData().length,clientAddressTID, clientPortTID);

					//send the packet received from server on an unknown socket back to client
					System.out.println("\nPacket sent back to client");
					TIDsocket.send(forwardedFirstDataPacket);
					System.out.println(Arrays.toString(forwardedFirstDataPacket.getData()));

					//get unknown TID response error packet
					DatagramPacket UnknownTIDrep = receivePacket(TIDsocket);
					if(UnknownTIDrep == null) return;

					//print the error response packet for unknown TID
					System.out.println("\nPacket received from unknown TID client");
					System.out.println(Arrays.toString(UnknownTIDrep.getData()));

					//creating an unknown TID response error packet to server
					DatagramPacket UnknownTIDreppacket = new DatagramPacket(UnknownTIDrep.getData(),
							UnknownTIDrep.getData().length,repDATA.getAddress(), repDATA.getPort());

					//send the error response packet to server
					System.out.println("\nPacket sent to server");
					sendReceiveServerSocket.send(UnknownTIDreppacket);
					System.out.println(new String(UnknownTIDreppacket.getData()));
					System.out.println(Arrays.toString(UnknownTIDreppacket.getData()));

					//then close the unknown TID socket
					TIDsocket.close();
					errorSimulation = false;
					return;
				}
				//request duplicated error on RRQ/WRQ
				else if(modeSelected == 6 && delayedPack == 1){
					System.out.println("\nPacket sent to server");
					sendReceiveServerSocket.send(serverRequestPacket);
					System.out.println(new String(serverRequestPacket.getData()));
					System.out.println(Arrays.toString(serverRequestPacket.getData()));

					//thread sleeps for few milliseconds and send the same request back to server
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					System.out.println("\nSending duplicated request packet to server..");
					System.out.println("\nPacket sent to server");
					sendReceiveServerSocket.send(serverRequestPacket);
					System.out.println(new String(serverRequestPacket.getData()));
					System.out.println(Arrays.toString(serverRequestPacket.getData()));

					errorSimulation = false;
				}
				//request lost error on RRQ/WRQ
				else if(modeSelected == 7 && delayedPack == 1){
					System.out.println("\nLosing a request from client..");
					errorSimulation = false;
					return;
				}
				else{
					System.out.println("\nPacket sent to server");
					sendReceiveServerSocket.send(serverRequestPacket);
					System.out.println(new String(serverRequestPacket.getData()));
					System.out.println(Arrays.toString(serverRequestPacket.getData()));
				}

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
						if (dataPacket == null) return;

						//end the transfer if received data packet is less than 512
						if (dataPacket.getData().length < 512){
							transferComplete = true;
							return;
						}

						System.out.println("\nPacket received from server");
						System.out.println(Arrays.toString(dataPacket.getData()));

						// Saves server TID on first iteration
						if (firstIteration){
							serverAddressTID = dataPacket.getAddress();
							serverPortTID = dataPacket.getPort();
						}
						// creating a data packet to receive from server
						DatagramPacket forwardedDataPacket = new DatagramPacket(dataPacket.getData(),
								dataPacket.getData().length,clientAddressTID,clientPortTID);

						//if error caused by the server
						if(errorSimulation && errorHost == 2)
							createInvalidDataAckPacket(forwardedDataPacket);

						if(modeSelected == 4 && errorHost == 2){
							createUnknownThread(forwardedDataPacket,clientAddressTID,clientPortTID);
							System.out.println("\nPacket sent back to client");
							sendReceiveClientSocket.send(forwardedDataPacket);
							System.out.println(Arrays.toString(forwardedDataPacket.getData()));
							errorSimulation = false;
						}
						//packet delayed error on DATA packet
						else if(modeSelected == 5 && delayedPack == 2 && errorHost == 2){ 
							if(firstIteration){
								System.out.println("\nClient resends RRQ packet...");
								try {
									Thread.sleep(2100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								//create an unknown TID socket
								DatagramSocket TIDsocket = new DatagramSocket();
								//send the packet received from server on an unknown socket back to client
								System.out.println("\nPacket sent back to client");
								TIDsocket.send(forwardedDataPacket);
								System.out.println(Arrays.toString(forwardedDataPacket.getData()));

								//get the response back on the unknown socket
								DatagramPacket unknownPacket = receivePacket(TIDsocket);
								if(unknownPacket == null) return;

								System.out.println("\nPacket received from unknown TID client");
								System.out.println(new String(unknownPacket.getData()));
								System.out.println(Arrays.toString(unknownPacket.getData()));

								DatagramPacket sendUnknownTIDpacket = new DatagramPacket(unknownPacket.getData(),
										unknownPacket.getData().length,serverAddressTID, serverPortTID);

								//sends an unknown TID packet to server
								System.out.println("\nPacket sent to server");
								sendReceiveServerSocket.send(sendUnknownTIDpacket);
								System.out.println(new String(sendUnknownTIDpacket.getData()));
								System.out.println(Arrays.toString(sendUnknownTIDpacket.getData()));

								//then close the unknown TID socket
								TIDsocket.close();
								errorSimulation = false;
								return;
							}else{
								System.out.println("Server resends DATA packet...\n");
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								//create a packet to resends a DATA packet to client
								DatagramPacket resendDATA = receivePacket(sendReceiveServerSocket);
								if(resendDATA == null) return;

								System.out.println("\nPacket received from server");
								System.out.println(new String(resendDATA.getData()));
								System.out.println(Arrays.toString(resendDATA.getData()));

								//creating a recreated DATA packet to be sent back to client
								DatagramPacket forwardedResendDataPacket = new DatagramPacket(resendDATA.getData(),
										resendDATA.getData().length,clientAddressTID,clientPortTID);

								//send the recreated DATA packet back to client
								System.out.println("\nPacket sent back to client");
								sendReceiveClientSocket.send(forwardedResendDataPacket);
								System.out.println(Arrays.toString(forwardedResendDataPacket.getData()));

								//create a packet to receive an ACK packet
								DatagramPacket ackForFirstSentPacket = receivePacket(sendReceiveClientSocket);
								if(ackForFirstSentPacket == null) return;

								System.out.println("\nPacket received from client");
								System.out.println(new String(ackForFirstSentPacket.getData()));
								System.out.println(Arrays.toString(ackForFirstSentPacket.getData()));

								//creating an ACK packet to be sent to server
								DatagramPacket forwardedACKPacket = new DatagramPacket(ackForFirstSentPacket.getData(),
										ackForFirstSentPacket.getData().length,serverAddressTID,serverPortTID);

								//send the ACK packet to server
								System.out.println("\nPacket sent to server");
								sendReceiveServerSocket.send(forwardedACKPacket);
								System.out.println(new String(forwardedACKPacket.getData()));
								System.out.println(Arrays.toString(forwardedACKPacket.getData()));

								//send the delayed DATA packet to client
								System.out.println("\nSending a delayed DATA packet ");
								System.out.println("Packet sent back to client");
								sendReceiveClientSocket.send(forwardedDataPacket);
								System.out.println(Arrays.toString(forwardedDataPacket.getData()));

								errorSimulation = false;
							}
						}
						//packet duplicate error on DATA packet
						else if(modeSelected == 6 && delayedPack == 2 && errorHost == 2){
							System.out.println("\nPacket sent back to client");
							sendReceiveClientSocket.send(forwardedDataPacket);
							System.out.println(Arrays.toString(forwardedDataPacket.getData()));
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							//receive ACK on the first DATA packet sent
							DatagramPacket firstACK = receivePacket(sendReceiveClientSocket);
							if(firstACK == null) return;

							System.out.println("\nPacket received from client");
							System.out.println(new String(firstACK.getData()));
							System.out.println(Arrays.toString(firstACK.getData()));

							//creating an ACK packet to be sent to server
							DatagramPacket forwardedFirstACK = new DatagramPacket(firstACK.getData(),
									firstACK.getData().length,serverAddressTID,serverPortTID);

							//send the ACK packet to server
							System.out.println("\nPacket sent to server");
							sendReceiveServerSocket.send(forwardedFirstACK);
							System.out.println(new String(forwardedFirstACK.getData()));
							System.out.println(Arrays.toString(forwardedFirstACK.getData()));

							//client sends the same DATA packet
							System.out.println("\nSending a duplicate DATA packet to client");
							System.out.println("Packet sent back to client");
							sendReceiveClientSocket.send(forwardedDataPacket);
							System.out.println(Arrays.toString(forwardedDataPacket.getData()));

							errorSimulation = false;
							return;
						}
						//packet loss error on DATA packet
						else if(modeSelected == 7 && delayedPack == 2 && errorHost == 2){
							System.out.println("\nLosing a request from client..");
							System.out.println("Server resends DATA packet...\n");
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							//receive data packet from server
							DatagramPacket respDATA = receivePacket(sendReceiveServerSocket);
							if(respDATA == null) return;

							System.out.println("\nPacket received from server");
							System.out.println(new String(respDATA.getData()));
							System.out.println(Arrays.toString(respDATA.getData()));

							//creating a packet to send back the resend 
							DatagramPacket forwardedrespDATA = new DatagramPacket(respDATA.getData(),
									respDATA.getData().length,clientAddressTID,clientPortTID);

							//send the recreated DATA packet back to client
							System.out.println("\nPacket sent back to client");
							sendReceiveClientSocket.send(forwardedrespDATA);
							System.out.println(Arrays.toString(forwardedrespDATA.getData()));

							errorSimulation = false;
							return;
						}
						else{
							System.out.println("\nPacket sent back to client");
							sendReceiveClientSocket.send(forwardedDataPacket);
							System.out.println(Arrays.toString(forwardedDataPacket.getData()));
						}
						firstIteration = false;

						//check if it is an error packet from server and break the loop
						int checkOp = PacketManager.getOpCode(forwardedDataPacket.getData());
						if(checkOp == 5) {
							transferComplete = true;
							break;
						}

						DatagramPacket ackPacket = receivePacket(sendReceiveClientSocket);
						if (ackPacket == null){
							transferComplete = true;
							return;
						}
						System.out.println("\nPacket received from client");
						System.out.println(new String(ackPacket.getData()));
						System.out.println(Arrays.toString(ackPacket.getData()));

						DatagramPacket forwardedAckPacket = new DatagramPacket(ackPacket.getData(),
								ackPacket.getData().length,serverAddressTID,serverPortTID);

						//if error is caused by the client
						if(errorSimulation && errorHost == 1)
							createInvalidDataAckPacket(forwardedAckPacket);

						if(modeSelected == 4 && errorHost == 1){
							createUnknownThread(forwardedAckPacket,serverAddressTID,serverPortTID);
							System.out.println("\nPacket sent back to server");
							sendReceiveServerSocket.send(forwardedAckPacket);
							System.out.println(Arrays.toString(forwardedAckPacket.getData()));
							errorSimulation = false;
						}
						//Packet delayed error on ACK packet
						else if(modeSelected == 5 && delayedPack == 2 && errorHost == 1){

							System.out.println("Server Resends Data Packet ");
							// Let the thread sleep
							try{
								Thread.sleep(1000);
							}catch(InterruptedException e){
								e.printStackTrace();
							}
							DatagramPacket dataPack = receivePacket(sendReceiveServerSocket);
							if (dataPack == null) return;	

							System.out.println("\n Packet received from server");
							System.out.println(new String(dataPack.getData()));
							System.out.println(Arrays.toString(dataPack.getData()));

							// Creating a packet 
							DatagramPacket resendPack = new DatagramPacket(dataPack.getData(),
									dataPack.getData().length,clientAddressTID, clientPortTID);

							System.out.println("Sending back to client ");
							sendReceiveClientSocket.send(resendPack);
							System.out.println(Arrays.toString(resendPack.getData()));

							//Create an ACK Packet and delay it
							DatagramPacket ack_Packet = receivePacket(sendReceiveClientSocket);
							if(ack_Packet == null) return;

							System.out.println("\n Packet received from client");
							System.out.println(new String(ack_Packet.getData()));
							System.out.println(Arrays.toString(ack_Packet.getData()));

							//Creating the ACK packet to send to the server
							DatagramPacket forwardAckPack = new DatagramPacket(ack_Packet.getData(),
									ack_Packet.getData().length, serverAddressTID, serverPortTID);

							System.out.println("\n Sending Packet to Server");
							System.out.println(new String(forwardAckPack.getData()));
							System.out.println(Arrays.toString(forwardAckPack.getData()));


							System.out.println("\n Sending delayed ACK packet");
							System.out.println("\n Packet sent back to server");
							sendReceiveServerSocket.send(forwardedAckPacket);
							System.out.println(Arrays.toString(forwardedAckPacket.getData()));

							errorSimulation = false;	
							return;
						}
						//Packet Duplicate error on ACK packet
						else if(modeSelected == 6 && delayedPack == 2 && errorHost == 1){

							System.out.println("\n Packet sent back to server");
							sendReceiveServerSocket.send(forwardedAckPacket);
							System.out.println(Arrays.toString(forwardedAckPacket.getData()));	

							// Let the thread sleep
							try{
								Thread.sleep(1000);
							}catch(InterruptedException e){
								e.printStackTrace();
							}

							System.out.println("Sending duplicate ACK packet to server");
							sendReceiveServerSocket.send(forwardedAckPacket);
							System.out.println(Arrays.toString(forwardedAckPacket.getData()));		

							errorSimulation = false;	
							return;					
						}

						//Lost packet error on ACK packet
						else if(modeSelected == 7 && delayedPack == 2 && errorHost == 1){
							System.out.println("\nLosing ACK packet from server...");
							System.out.println("Server resends DATA packet...\n");
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							//receive data packet from server
							DatagramPacket respDATA = receivePacket(sendReceiveServerSocket);
							if(respDATA == null) return;	

							System.out.println("Receiving the Data from the server");
							System.out.println(new String (respDATA.getData()));
							System.out.println(Arrays.toString(respDATA.getData()));

							DatagramPacket dataPacks = new DatagramPacket(respDATA.getData(),
									respDATA.getData().length, clientAddressTID, clientPortTID);
							System.out.println("Sending the data to the client");
							sendReceiveClientSocket.send(dataPacks);
							System.out.println(Arrays.toString(dataPacks.getData()));

							DatagramPacket receiveACK = receivePacket(sendReceiveClientSocket);
							if(receiveACK == null) return;	
							System.out.println("Receiving the ACK from the client");
							System.out.println(new String (receiveACK.getData()));
							System.out.println(Arrays.toString(receiveACK.getData()));

							DatagramPacket ackPacks = new DatagramPacket(receiveACK.getData(),
									receiveACK.getData().length, serverAddressTID, serverPortTID);

							System.out.println("Sending the ACK to the client");
							sendReceiveClientSocket.send(ackPacks);
							System.out.println(Arrays.toString(ackPacks.getData()));
							
							errorSimulation = false;	
							return;	
						}
						
						else{
							System.out.println("\nPacket sent to server");
							sendReceiveServerSocket.send(forwardedAckPacket);
							System.out.println(new String(forwardedAckPacket.getData()));
							System.out.println(Arrays.toString(forwardedAckPacket.getData()));
						}
						//check if it is an error packet from client and break the loop
						int checkOp1 = PacketManager.getOpCode(forwardedAckPacket.getData());
						if(checkOp1 == 5) {
							transferComplete = true;
							break;
						}
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
				System.out.println("Do you want to start simulating an error again (y/n)? ");
				Scanner in = new Scanner(System.in);
				String user;
				user = in.nextLine();
				try {
					if(user.equals("y") || user.equals("yes")){
						startErr();
					}else if(user.equals("n") || user.equals("n")){
						in.close();
						System.exit(1);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
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
						, clientRequestPacket.getData().length, clientRequestPacket.getAddress(),pd.getServerPort());

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
				System.out.println("Do you want to start simulating an error again (y/n)? ");
				Scanner in = new Scanner(System.in);
				String user;
				user = in.nextLine();
				try {
					if(user.equals("y") || user.equals("yes")){
						startErr();
					}else if(user.equals("n") || user.equals("n")){
						in.close();
						System.exit(1);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
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
				System.out.println("\nUnknown TID Transfer Handler thread started.\n");
				// New socket with a different TID than the currently ongoing transfer
				DatagramSocket socket = new DatagramSocket();

				// Sends the packet to the host using this new TID
				socket.send(packet);

				byte[] data = new byte[ioman.getBufferSize()+4];
				//received packet is an error
				DatagramPacket errPacket = new DatagramPacket(data,data.length);

				boolean err;

				do
				{
					err = false;
					// Receives invalid TID error packets
					socket.receive(errPacket);
					System.out.println(new String(errPacket.getData()));
					System.out.println(Arrays.toString(errPacket.getData()));

					// Check if the address and port of the received packet match the TID
					InetAddress packetAddress = errPacket.getAddress();
					int packetPort = errPacket.getPort();
					if (!(	packetAddress.equals(addressTID) && (packetPort == portTID)	)){
						err = true;
					}
					// Check if error code is not equal to the unknownTID error code (5)
					else if (PacketManager.twoBytesToInt(errPacket.getData()) != 5){
						err = true;
					}

				}while(err);

				socket.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
			finally
			{
				System.out.println("\nUnknownTIDTransferHandler thread terminated.");
			}
		}
	}
}
