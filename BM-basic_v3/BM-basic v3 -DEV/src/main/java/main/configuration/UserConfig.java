package main.configuration;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import main.TransTechSystem;
import tools.FileHandler;

public class UserConfig extends Properties {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4355899199387630170L;
	private static final Logger logger = Logger.getLogger(UserConfig.class);
	private FileHandler fh;

	public UserConfig() {
		super();
		String file = "configuration/bm.properties";
		try {
			fh = new FileHandler(file);
			this.load(fh.getFileReader());
		} catch (IOException e1) {
			logger.fatal("Could not open properties file! The installation files must be mishandled or corrupted!");
			//TransTechSystem.exit();
		}
	}
}
