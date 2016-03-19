package DirectoryFiller;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class Fill {

	public static void main( String args[] ) throws IOException
	{
		System.out.println("Enter (in bytes) the size of the file you would like to create.");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String request = reader.readLine(); 
		int sizeInBytes = Integer.parseInt(request);
		File file = new File(System.getProperty("user.dir") + "/file.txt");
		BufferedWriter w = new BufferedWriter(new FileWriter(file,true));
		byte b = 1;
		
		for(int i = 0; i < sizeInBytes; i++) {
			w.write(b);
		}
		
		w.close();
	}
}
