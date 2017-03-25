package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * <b>FileHandler</b><br><br>
 * 
 * The FileHandler is an outsourced process which came from multiple objects that needed to handle files. This
 * object was created to outsource the process of accessing a file within the system. The FileHandler has two main
 * functions: <b>
 * <ul>
 * 	<li>Accessing a specified file.
 * 		<ul>
 * 			<li>Provide the Reader and Writer to a specified file.</li>
 * 			<li>Provide a method for saving the a Properties object to a file.</li>
 * 		</ul>
 * 	</li>
 * 	<!--<li>Manage the existing InputStreams and OutputStreams:
 * 		<ul>
 * 			<li>Closing unused InputStreams and OutputStreams</li>
 * 		</ul>
 * 	</li>-->
 * 	<li>Handle exceptions in accessing the specified file.
 * 		<ul>
 * 			<li>Invalid file path.</li>
 * 		</ul>
 * 	</li>
 * </ul>
 * @author carlo
 *
 */
public class FileHandler {
	private static final Logger logger = Logger.getLogger(FileHandler.class);
	private File file;
	
	public FileHandler(String filepath) throws FileNotFoundException {
		file = new File(filepath);
		if(!file.exists()) {
			throw new FileNotFoundException();
		}
	}
	
	/**
	 * Reads one line from the file specified. A line is considered to be terminated 
	 * by any one of a line feed ('\n'), a carriage return ('\r'), or a carriage return 
	 * followed immediately by a linefeed.
	 * <br><br>
	 * <b>NOTE:</b> Always reads the next line every time this method is called.
	 * 
	 * @return String A String containing the contents of the line, not including any 
	 * line-termination characters, or null if the end of the stream has been reached
	 * @throws IOException
	 */
	public String readLine() throws IOException {
		logger.trace("Reading one line from '" + file.getName() + "...");
		BufferedReader reader = getFileReader();
		InputStream in = new FileInputStream(file);
		
		return reader.readLine();
	}
	
	public String[] readAllLines() throws IOException {
		logger.trace("Reading one line from '" + file.getName() + "...");
		BufferedReader reader = getFileReader();
		InputStream in = new FileInputStream(file);
		Vector<String> lines = new Vector<String>(10, 10);
		
		while(reader.ready()) {
			lines.add(reader.readLine());
		}
		return lines.toArray(new String[lines.size()]);
	}
	
	/**
	 * Accesses the file and returns a BufferedReader for the file. <br><br>
	 * 
	 * @param filepath The path of the file. Can be absolute or relative.
	 * @return a BufferedReader of the file that is specified by the <b>filepath</b>.
	 * @throws FileNotFoundException 
	 */
	public BufferedReader getFileReader() throws FileNotFoundException {
		logger.trace("Getting BufferedReader for '" + file.getName() + "...");
		InputStream in = new FileInputStream(file);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		return reader;
	}
	
	/**
	 * Creates a BufferedWriter for the file.
	 * 
	 * @param filepath
	 * @return a BufferedWriter of the file that is specified by the <b>filepath</b>.
	 * @throws IOException
	 */
	public BufferedWriter getFileWriter() throws IOException { 
		logger.trace("Getting BufferedWriter for the file...");
		FileWriter fw = new FileWriter(file.getAbsolutePath());
		BufferedWriter writer = new BufferedWriter(fw);
		
		return writer;
	}
	
	/**
	 * Appends the specified String to the file. Adds a new line first before appending the String.
	 * 
	 * @param str The String to be appended
	 * @throws IOException 
	 */
	public void appendToFile(String str) throws IOException {
		logger.trace("Appending ''" + str + "'' to " + file.getName());
		FileWriter fw = new FileWriter(file.getAbsolutePath(), true);
		BufferedWriter writer = new BufferedWriter(fw);
		writer.newLine();
		writer.write(str);
		writer.close();
		logger.trace("Append successful!");
	}
	
	/**
	 * Writes the specified String to the file. Overwrites all pre-existing contents of the file.
	 * @param str The String to be written
	 * @throws IOException
	 */
	public void writeToFile(String str) throws IOException {
		logger.trace("Writing ''" + str + "'' to " + file.getName());
		FileWriter fw = new FileWriter(file.getAbsolutePath(), false);
		BufferedWriter writer = new BufferedWriter(fw);
		writer.write(str);
		writer.close();
		logger.trace("Write successful!");
	}
	/**
	 * Checks the if the file extension is the same with the extension specified.
	 * 
	 * @param extension The specified file extension. <b>MUST NOT</b> contain the period before the extension.
	 * @return <b>True</b> if the file has the same extension specified. <b>False</b> if not.
	 */
	public boolean checkExtension(String extension) {
		logger.trace("Checking extension...");
		try {
			return file.getCanonicalPath().split("\\.")[file.getCanonicalPath().split("\\.").length - 1]
					.equalsIgnoreCase(extension);
		} catch (IOException e) {
			logger.error("Cannot check extension!", e);
			return false;
		}
	}
	
	/**
	 * Stores the Properties object into the file
	 * 
	 * @param properties The Properties object
	 * @param comments Header comments that will also be put in the file. Can be null
	 */
	public void saveProperties(Properties properties, String comments) {
		logger.trace("Saving Properties object...");
		try {
			properties.store(getFileWriter(), comments);
		} catch (IOException e) {
			logger.error("Cannot save the Properties object!", e);
		}
	}
	
	/**
	 * Returns the File this FileHandler handles.
	 * 
	 * @return The file;
	 */
	public File getFile() {
		return file;
	}
}