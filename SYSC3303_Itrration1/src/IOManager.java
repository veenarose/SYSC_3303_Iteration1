import java.io.*;

public class IOManager {
	private final int bufferSize = 512;
	
	public IOManager(){}
	
	/**
	 * Reads a given file from a specified starting point and returns a byte[] for the data read
	 * @param filename File to be read
	 * @param offset offset at which to start reading
	 * @return byte[] of the data read
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public synchronized byte[] read(String fn, int offset) throws FileNotFoundException, IOException{
		File filename = new File(fn);
		File f2 = filename.getCanonicalFile();
		String s = f2.getAbsolutePath();
		
		FileInputStream fis = new FileInputStream(s);
		
		byte[] data = new byte[bufferSize];
		BufferedInputStream reader = new BufferedInputStream(fis);

		/* Read 512 bytes from file starting at the offset*/
		reader.read(data, offset, bufferSize);
		
		/* Close reader and return */
		reader.close();
		return data;
	}
	
	/**
	 * Writes the given data to the file with the given filename
	 * @param file name of file to be written to
	 * @param data information of 512 bytes to be written
	 * @throws IOException
	 */
	public synchronized void write(String file, byte[] data) throws IOException{
		String s = new String(data);
		
		BufferedWriter w = new BufferedWriter(new FileWriter(file,true));
		
		w.write(s);
		w.close();
	}
	
	public synchronized int getBufferSize() {
		return bufferSize;
	}
}