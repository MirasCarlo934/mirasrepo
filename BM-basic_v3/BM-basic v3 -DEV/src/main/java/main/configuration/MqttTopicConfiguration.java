package main.configuration;

public class MqttTopicConfiguration {
	private String BMTopic;
	private String WSTopic;
	private String openhabTopic;
	private String adminTopic;
	private String defaultTopic;
	private String httpLayerTopic;
	private String errorTopic;
	
	/**
	 * @return the bMTopic
	 */
	public String getBMTopic() {
		return BMTopic;
	}
	
	/**
	 * @param bMTopic the bMTopic to set
	 */
	public void setBMTopic(String bMTopic) {
		BMTopic = bMTopic;
	}

	/**
	 * @return the wSTopic
	 */
	public String getWSTopic() {
		return WSTopic;
	}

	/**
	 * @param wSTopic the wSTopic to set
	 */
	public void setWSTopic(String wSTopic) {
		WSTopic = wSTopic;
	}

	/**
	 * @return the adminTopic
	 */
	public String getAdminTopic() {
		return adminTopic;
	}

	/**
	 * @param adminTopic the adminTopic to set
	 */
	public void setAdminTopic(String adminTopic) {
		this.adminTopic = adminTopic;
	}

	/**
	 * @return the defaultTopic
	 */
	public String getDefaultTopic() {
		return defaultTopic;
	}

	/**
	 * @param defaultTopic the defaultTopic to set
	 */
	public void setDefaultTopic(String defaultTopic) {
		this.defaultTopic = defaultTopic;
	}

	/**
	 * @return the httpLayerTopic
	 */
	public String getHttpLayerTopic() {
		return httpLayerTopic;
	}

	/**
	 * @param httpLayerTopic the httpLayerTopic to set
	 */
	public void setHttpLayerTopic(String httpLayerTopic) {
		this.httpLayerTopic = httpLayerTopic;
	}

	/**
	 * @return the errorTopic
	 */
	public String getErrorTopic() {
		return errorTopic;
	}

	/**
	 * @param errorTopic the errorTopic to set
	 */
	public void setErrorTopic(String errorTopic) {
		this.errorTopic = errorTopic;
	}

	/**
	 * @return the openhabTopic
	 */
	public String getOpenhabTopic() {
		return openhabTopic;
	}

	/**
	 * @param openhabTopic the openhabTopic to set
	 */
	public void setOpenhabTopic(String openhabTopic) {
		this.openhabTopic = openhabTopic;
	}

}
