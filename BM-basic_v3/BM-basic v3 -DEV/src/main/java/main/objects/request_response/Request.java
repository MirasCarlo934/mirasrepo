package main.objects.request_response;

import org.json.JSONObject;

import main.TransTechSystem;

public class Request {
	private String requestID;
	private String componentID;
	private String requestType;
	private String topic;
	private JSONObject json;
	
	/**
	 * This constructor is only used in the creation of the request inside the requesting components themselves (eg. WebServer).
	 * 
	 * @param requestID
	 * @param componentID
	 * @param requestType
	 */
	public Request(String requestID, String componentID, String requestType) {
		setRequestID(requestID);
		setComponentID(componentID);
		setRequestType(requestType);
		json = new JSONObject();
		json.put(TransTechSystem.config.getRequestParamConfig().getRequestIdKey(), requestID);
		json.put(TransTechSystem.config.getRequestParamConfig().getRequestTypeKey(), requestType);
		json.put(TransTechSystem.config.getRequestParamConfig().getComponentIdKey(), componentID);
	}
	
	/**
	 * This constructor is used when the Callback object receives the request from the MQTT broker in the form of a JSON text. The Request
	 * object must be constructed using this constructor ONLY after the RequestHandler has approved of the validity of the request.<br>
	 * 
	 * This constructor constructs a Request object without a topic. This kind of Request object is used ONLY IF the request was issued
	 * in a BusinessMachine Module in order to communicate with other Modules.
	 * 
	 * @param request
	 */
	public Request(JSONObject request) {
		json = request;
		setRequestID(request.getString(TransTechSystem.config.getRequestParamConfig().getRequestIdKey()));
		setComponentID(request.getString(TransTechSystem.config.getRequestParamConfig().getComponentIdKey()));
		setRequestType(request.getString(TransTechSystem.config.getRequestParamConfig().getRequestTypeKey()));
	}
	
	/**
	 * This constructor is used when the RequestGate receives the request from the MQTT broker in the form of a JSON text. The Request
	 * object must be constructed using this constructor ONLY after the RequestGate has approved of the validity of the request
	 * and if the Request is NOT a registration request.
	 * 
	 * @param request
	 */
	public Request(JSONObject request, String topic) {
		json = request;
		setRequestID(request.getString(TransTechSystem.config.getRequestParamConfig().getRequestIdKey()));
		setComponentID(request.getString(TransTechSystem.config.getRequestParamConfig().getComponentIdKey()));
		setRequestType(request.getString(TransTechSystem.config.getRequestParamConfig().getRequestTypeKey()));
		setTopic(topic);
	}
	
	/**
	 * This method is only usually used when the Request was constructed without a JSONObject
	 * 
	 * @param key
	 * @param o
	 */
	public void put(String key, Object o) {
		json.put(key, o);
	}
	
	/**
	 * 	This method is invoked when the request is to be persisted into the MQTT broker.
	 * 
	 * @return the JSONObject representation of the Request
	 */
	public JSONObject toJSONObject() {
		return json;
	}
	
	/**
	 * Gets specified parameter from the request.
	 * @param key
	 * @return
	 */
	public Object get(String key) {
		return json.get(key);
	}
	
	public boolean hasKey(String key) {
		return json.has(key);
	}
	
	public String getString(String key) {
		return json.optString(key);
	}
	
	public int getInt(String key) {
		return json.optInt(key);
	}
	
	public boolean getBoolean(String key) {
		return json.optBoolean(key);
	}

	public String getRequestID() {
		return requestID;
	}

	private void setRequestID(String requestID) {
		this.requestID = requestID;
	}

	public String getComponentID() {
		return componentID;
	}

	private void setComponentID(String componentID) {
		this.componentID = componentID;
	}

	public String getRequestType() {
		return requestType;
	}

	private void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	/**
	 * Returns the topic of the component that sent the request. This is automatically filled by the RequestGate after it
	 * has approved of the validity of this request.
	 * 
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Sets the topic where the response to this request will be sent to. This method is ONLY used in the RequestHandler after an
	 * intercepted JSON checks out to be a valid request.
	 * 
	 * @param topic the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}
}
