package input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class InputReader {
	private static final Logger log = Logger.getLogger("INPUT.InputReader");
	private BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	public InputReader() {
		// TODO Auto-generated constructor stub
	}
	
	public String read() {
		try {
			return br.readLine();
		} catch (IOException e) {
			log.error("Cannot read line!", e);
			e.printStackTrace();
			return null;
		}
	}
}
