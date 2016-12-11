package main.objects.request_response;

import org.json.JSONObject;

import main.TransTechSystem;

public class Response {
	public static final String success_field = "success";
	
	private String requestID;
	private String requestType;
	private String topic;
	private boolean successful;
	protected JSONObject json;

	/**
	 * Assembles an RRP Response with the 'success' field default to true
	 * 
	 * @param requestID
	 * @param topic
	 */
	public Response(String requestID, String RTY, String topic) {
		this.requestID = requestID;
		this.setTopic(topic);
		json = new JSONObject().put(TransTechSystem.config.getRequestParamConfig().getRequestIdKey(), requestID);
		setSuccessful(true);
		setRequestType(RTY);
	}
	
	public Object get(String key) {
		return json.get(key);
	}
	
	/**
	 * Adds the specified object which is bound to a key mapping to the Response.
	 * 
	 * @param key mapping of the object
	 * @param o to be added to the Response
	 */
	public void put(String key, Object o) {
		json.put(key, o);
	}

	/**
	 * @return the requestID
	 */
	public String getRequestID() {
		return requestID;
	}

	/**
	 * @param requestID the requestID to set
	 */
	public void setRequestID(String requestID) {
		this.requestID = requestID;
	}

	/**
	 * 	This method is invoked when the request is to be persisted into the MQTT broker.
	 * 
	 * @return the JSONObject representation of the Response
	 */
	public JSONObject toJSONObject() {
		return json;
	}

	/**
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * @param topic the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * @return the successful
	 */
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * @param successful the successful to set
	 */
	public void setSuccessful(boolean successful) {
		this.successful = successful;
		json.put(Response.success_field, successful);
	}

	/**
	 * @return the requestType
	 */
	public String getRequestType() {
		return requestType;
	}

	/**
	 * @param requestType the requestType to set
	 */
	public void setRequestType(String requestType) {
		json.put(TransTechSystem.config.getRequestParamConfig().getRequestTypeKey(), requestType);
		this.requestType = requestType;
	}
}
