import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class TFTPServer implements Runnable{
	private DatagramSocket socket;
	private PacketManager pacman;
	private IOManager ioman;

	public TFTPServer(){
		pacman = new PacketManager();
		ioman = new IOManager();
	}

	public void run() {
		print("Server created");
		byte[] buf = new byte[512];
		DatagramPacket p = new DatagramPacket(buf, buf.length);

		try {
			socket = new DatagramSocket(1024);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		print("listening");
		while(true){

			try {
				socket.receive(p);
			} catch (IOException e) {
				e.printStackTrace();
			}
			print("starting thread");
			Thread t = new Thread(new Transfer(p), "Client");
			t.start();

			print("thread started");
		}
	}

	public static void main(String[] args){
		TFTPServer t = new TFTPServer();
		t.run();
	}

	/**
	 * Convenience function to not have to type System.err.println();
	 * @param s String to be printed
	 */
	public void error(String s){
		System.err.println(s);
	}

	public void print(String s){
		System.out.println(s);
	}

	class Transfer extends Thread{
		private DatagramPacket packet;
		private byte[] data;
		private final int port;
		private final InetAddress addr;

		public Transfer(DatagramPacket p){
			packet = p;
			data = p.getData();
			port = p.getPort();
			addr = p.getAddress();
		}

		public void run(){

			print("thread run");
			byte[] buf = new byte[512];
			DatagramPacket send = new DatagramPacket(buf, buf.length);
			int req = -1;

			print("req val");
			try {
				req = pacman.validateRequest(data);
			} catch (IOException e) {
				e.printStackTrace();
			}

			print("req value: " + req);
			if(req == 1){	//read

				int offs = 0;
				short blockNum = 1;

				print("getfilename");
				String filename = "C:/a";
				
				//converts block number from short to byte
				ByteBuffer buffer = ByteBuffer.allocate(2);
				buffer.putShort(blockNum);
				byte[] block = buffer.array();

				do{
					print(" 6 ");

					try {
						data = ioman.read(filename, offs);
					} catch (IOException e) {
						e.printStackTrace();
					}
					offs += data.length;

					print(" 7 ");

					DatagramPacket p = new DatagramPacket(pacman.createData(data, block),
							pacman.createData(data, block).length,
							addr,
							port);

					try {
						socket.send(p);
					} catch (IOException e) {
						e.printStackTrace();
					}

					blockNum++;

					try {
						socket.receive(packet);
						update(packet);
					} catch (IOException e) {
						e.printStackTrace();
					}

					print(" 11 ");

					if(pacman.isAckPacket(data)){
						byte[] ackData = data;
						ByteBuffer b = ByteBuffer.allocate(2);
						b.put(ackData[3]);
						b.put(ackData[4]);

						short bn = b.getShort(0);

						if(bn != blockNum){
							error("ACK packet Block Number does not match current block number");
							break;
						}
					} else
						try {
							if(pacman.validateRequest(data) == 5){
								break;
							} else {
								error("HandleReadReq: incoming packet could not be verified as ACK or ERR");
								break;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
				} while(!(pacman.lastPacket(data)));


				try {
					data = ioman.read(filename, offs);
				} catch (IOException e) {
					e.printStackTrace();
				}

				offs += data.length;

				DatagramPacket p = new DatagramPacket(pacman.createData(data, block),
						pacman.createData(data, block).length,
						addr,
						port);

				try {
					socket.send(p);
				} catch (IOException e) {
					e.printStackTrace();
				}
				blockNum++;

				try {
					socket.receive(packet);
					update(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(pacman.isAckPacket(data)){

					byte[] ackData = data;
					ByteBuffer b = ByteBuffer.allocate(2);
					b.put(ackData[3]);
					b.put(ackData[4]);

					short bn = b.getShort(0);

					if(bn != blockNum){
						error("ACK packet Block Number does not match current block number");
					}
				}
				print("File read Succesfuly");

			}
			if(req == 2){	//write

			} 
		}
		private void update(DatagramPacket p){
			packet = p;
			data = p.getData();
		}
	}

}
