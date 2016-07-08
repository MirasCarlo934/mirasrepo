package main.configuration;

import java.util.HashMap;

import main.objects.DB.DBTableConfig;

public class DatabaseConfiguration {
	private String ssidColName;
	private int ssidLength;
	private HashMap<String, DBTableConfig> dbTableConfigs = new HashMap<String, DBTableConfig>(1);
	
	/**
	 * @return the ssidColName
	 */
	public String getSsidColName() {
		return ssidColName;
	}
	
	/**
	 * @param ssidColName the ssidColName to set
	 */
	public void setSsidColName(String ssidColName) {
		this.ssidColName = ssidColName;
	}

	/**
	 * @return the ssidLength
	 */
	public int getSsidLength() {
		return ssidLength;
	}

	/**
	 * @param ssidLength the ssidLength to set
	 */
	public void setSsidLength(int ssidLength) {
		this.ssidLength = ssidLength;
	}

	/**
	 * @return the dbTableConfigs
	 */
	public HashMap<String, DBTableConfig> getDbTableConfigs() {
		return dbTableConfigs;
	}

	/**
	 * @param dbTableConfigs the dbTableConfigs to set
	 */
	public void setDbTableConfigs(HashMap<String, DBTableConfig> dbTableConfigs) {
		this.dbTableConfigs = dbTableConfigs;
	}
}
