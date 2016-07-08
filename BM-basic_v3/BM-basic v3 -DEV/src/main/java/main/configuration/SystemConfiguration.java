package main.configuration;

import org.apache.log4j.Logger;

public class SystemConfiguration {
	private static final Logger logger = Logger.getLogger("SystemConfiguration");
	/*
	 * System information
	 */
	private String systemMacAddress;
	/*
	 * System commands
	 */
	private String[] systemCommands;
	
	private UserConfig uc;
	
	public SystemConfiguration() {
		
	}
	
	public SystemConfiguration(UserConfig userConfig) {
		uc = userConfig;
		setSystemMacAddress(uc.getProperty("mac"));
	}
	
	/**
	 * @return the systemMacAddress
	 */
	public String getSystemMacAddress() {
		return systemMacAddress;
	}
	
	/**
	 * @param systemMacAddress the systemMacAddress to set
	 */
	private void setSystemMacAddress(String mac) {
		/*
		 * Retrieves MAC address automatically
		 
		try {
			InetAddress ip = InetAddress.getLocalHost();
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
			}
			systemMacAddress = sb.toString();
		} catch (Exception e) {
			logger.fatal("Error in obtaining MAC address!", e);
			System.exit(0);
		}*/
		
	}

	/**
	 * @return the systemCommands
	 */
	public String[] getSystemCommands() {
		return systemCommands;
	}

	/**
	 * @param systemCommands the systemCommands to set
	 */
	public void setSystemCommands(String[] systemCommands) {
		this.systemCommands = systemCommands;
	}
}
