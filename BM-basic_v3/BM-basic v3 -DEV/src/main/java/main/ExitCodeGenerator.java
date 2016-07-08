package main;

import org.apache.log4j.Logger;

public class ExitCodeGenerator implements org.springframework.boot.ExitCodeGenerator {
	//private static final Logger logger = Logger.getLogger("ExitCodeGenerator");

	public int getExitCode() {
		//logger.info("System shutdown complete!");
		return 0;
	}
}
