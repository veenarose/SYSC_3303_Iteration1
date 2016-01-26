import java.io.*;

public class IOManager {
	private final int bufferSize = 512;
	
	/**
	 * Reads a given file from a specified starting point and returns a byte[] for the data read
	 * @param filename File to be read
	 * @param offset offset at which to start reading
	 * @return byte[] of the data read
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public  byte[] read(String filename, int offset) throws FileNotFoundException, IOException{
		byte[] data = new byte[bufferSize];
		BufferedInputStream reader = new BufferedInputStream(new FileInputStream(filename));

		/* Read 512 bytes from file starting at the offset*/
		reader.read(data, offset, bufferSize);
		
		/* Close reader and return */
		reader.close();
		return data;
	}
	
	
	public void write(String file, byte[] data) throws IOException{
		String s = new String(data);

		
		BufferedWriter w = null;

		w = new BufferedWriter(new FileWriter(file));

		w.write(s);
		w.close();
		
	}
}
