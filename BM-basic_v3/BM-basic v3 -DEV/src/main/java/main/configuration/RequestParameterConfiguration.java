package main.configuration;

public class RequestParameterConfiguration {
	private String requestIdKey;
	private String requestTypeKey;
	private String componentIdKey;
	private String errorKey;
	private String errorMsgKey;
	private String commandKey;
	
	/**
	 * @return the requestIdKey
	 */
	public String getRequestIdKey() {
		return requestIdKey;
	}
	
	/**
	 * @param requestIdKey the requestIdKey to set
	 */
	public void setRequestIdKey(String requestIdKey) {
		this.requestIdKey = requestIdKey;
	}

	/**
	 * @return the requestTypeKey
	 */
	public String getRequestTypeKey() {
		return requestTypeKey;
	}

	/**
	 * @param requestTypeKey the requestTypeKey to set
	 */
	public void setRequestTypeKey(String requestTypeKey) {
		this.requestTypeKey = requestTypeKey;
	}

	/**
	 * @return the componentIdKey
	 */
	public String getComponentIdKey() {
		return componentIdKey;
	}

	/**
	 * @param componentIdKey the componentIdKey to set
	 */
	public void setComponentIdKey(String componentIdKey) {
		this.componentIdKey = componentIdKey;
	}

	/**
	 * @return the errorKey
	 */
	public String getErrorKey() {
		return errorKey;
	}

	/**
	 * @param errorKey the errorKey to set
	 */
	public void setErrorKey(String errorKey) {
		this.errorKey = errorKey;
	}

	/**
	 * @return the errorMsgKey
	 */
	public String getErrorMsgKey() {
		return errorMsgKey;
	}

	/**
	 * @param errorMsgKey the errorMsgKey to set
	 */
	public void setErrorMsgKey(String errorMsgKey) {
		this.errorMsgKey = errorMsgKey;
	}

	/**
	 * @return the commandKey
	 */
	public String getCommandKey() {
		return commandKey;
	}

	/**
	 * @param commandKey the commandKey to set
	 */
	public void setCommandKey(String commandKey) {
		this.commandKey = commandKey;
	}

}
