import java.io.*;

public class IOManager {
	private static final int bufferSize = 512;
	
	public IOManager(){}
	
	/**
	 * Reads a given file from a specified starting point and returns a byte[] for the data read
	 * @param filename File to be read
	 * @param offset offset at which to start reading
	 * @return byte[] of the data read
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
//	public synchronized byte[] read(String filename, int offset) throws FileNotFoundException, IOException{
//		byte[] data = new byte[bufferSize];
//		BufferedInputStream reader = new BufferedInputStream(new FileInputStream(filename));
//
//		/* Read 512 bytes from file starting at the offset*/
//		reader.read(data, offset, bufferSize); //throws IOException
//		
//		/* Close reader and return */
//		reader.close();
//		return data;
//	}
	
	/**
	 * Writes the given data to the file with the given filename
	 * @param file name of file to be written to
	 * @param data information of 512 bytes to be written
	 * @throws IOException
	 */
	public static void write(File file, byte[] data) throws IOException{
		//BufferedWriter w = new BufferedWriter(new FileWriter(file,true));
		BufferedOutputStream w = new BufferedOutputStream(new FileOutputStream(file,true));
		
		w.write(data);
		w.close();
	}
	
	public static int getBufferSize() {
		return bufferSize;
	}
	
	public static byte[] read(BufferedInputStream reader, int buffSize, byte[] data) 
			throws IOException {
		int last = reader.read(data, 0, buffSize);
		if(last < 0) {
			data = null;
		}
		return data;
	}
	
	public static BufferedInputStream getReader(String pathToFileName) throws FileNotFoundException {
		BufferedInputStream reader = new BufferedInputStream(new FileInputStream(pathToFileName));
		return reader;
	}
}
