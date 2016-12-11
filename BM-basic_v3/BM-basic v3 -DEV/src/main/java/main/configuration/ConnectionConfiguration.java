package main.configuration;

public class ConnectionConfiguration {
	private String dbURL;
	private String dbUser;
	private String dbPass;
	private String mqttURL;
	
	private UserConfig uc;
	
	public ConnectionConfiguration(UserConfig userConfig) {
		uc = userConfig;
		setDbURL(uc.getProperty("derby"));
		setDbUser(uc.getProperty("derby_user"));
		setDbPass(uc.getProperty("derby_pwd"));
		setMqttURL(uc.getProperty("mqtt"));
	}
	
	/**
	 * @return the dbURL
	 */
	public String getDbURL() {
		return dbURL;
	}
	
	/**
	 * @param dbURL the dbURL to set
	 */
	public void setDbURL(String dbURL) {
		this.dbURL = dbURL;
	}

	/**
	 * @return the dbUser
	 */
	public String getDbUser() {
		return dbUser;
	}

	/**
	 * @param dbUser the dbUser to set
	 */
	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	/**
	 * @return the dbPass
	 */
	public String getDbPass() {
		return dbPass;
	}

	/**
	 * @param dbPass the dbPass to set
	 */
	public void setDbPass(String dbPass) {
		this.dbPass = dbPass;
	}

	/**
	 * @return the mqttURL
	 */
	public String getMqttURL() {
		return mqttURL;
	}

	/**
	 * @param mqttURL the mqttURL to set
	 */
	public void setMqttURL(String mqttURL) {
		this.mqttURL = mqttURL;
	}

}
