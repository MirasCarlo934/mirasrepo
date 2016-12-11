/*
 * Module documentation:
 * 		Implementations of Module objects are used as the main processing entities behind a BusinessMachine. They are called by an
 * 	MQTTCallback which forwards specific MqttMessages for the Module to process. Modules are also used by the BusinessMachine to handle
 * 	database interactions and are the only BusinessMachine components that can do so.
 * 
 * 		Each implementation of this Module object handles a specific MqttMessage that needs to be processed. This is determined by the
 * 	'request_type' parameter of an MqttMessage. One request_type calls for a respective Module implementation. Responses to these
 * 	'requests' are stored in a Vector object and are collected by the MQTTCallback, which are then collected by the MQTTGateway which
 * 	publishes these responses to the MQTTBroker.
 * 		
 * 		A Module object is dependent on the BM TrafficController for database interactions.
 */

package bm.modules.parents;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import bm.ComponentRepository;
import main.TransTechSystem;
import main.objects.*;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import mqtt.MQTTHandler;
import tools.TrafficController;

public abstract class Module implements Runnable {
	private static final Logger logger = Logger.getLogger(Module.class);
	private MQTTHandler mqttHandler;
	private String name;
	private String request_type;
	private String[] request_params = null; //contains all the required request parameters, null if Module does not require additional request parameters
	private Request request = null; //changes with every call on an implementation of this Module
	private Vector<Response> responses = new Vector<Response>(1,1); //each implementation of this Module will put their responses to this vector
	
	public Module(String name, String request_type, String[] request_params, MQTTHandler mqttHandler) {
		this.mqttHandler = mqttHandler;
		this.name = name;
		this.setRequest_type(request_type);
		this.request_params = request_params;
	}
	
	/**
	 * Instructs the Module implementation to start processing the supplied request.
	 * @param request The supplied request
	 * @throws Exception
	 */
	public void processRequest(Request request) throws Exception {
		this.request = request;
		Thread t = new Thread(this, name);
		t.start();
	}
	
	protected abstract void process(Request request) throws Exception;
	
	public void run() {
		logger.info("Starting request processing...");
		try {
			process(request);
			publishResponses();
			logger.info("Request processing finished!");
		} catch (Exception e) {
			logger.error("Cannot process request!", e);
		}
	}
	
	private void publishResponses() throws MqttException {
		if(!responses.isEmpty()) {
			while(!responses.isEmpty()) {
				Response r = responses.remove(0);
				mqttHandler.publish(r.getTopic(), r.toJSONObject().toString());
			}
		}
	}
	
	/**
	 * Checks whether the request complies with the secondary-parameter requirements of this Module. /n/n
	 * 
	 * ErrorCode: 10000
	 * @param json The JSONObject representation of the request
	 * @return
	 */
	public boolean checkParameterNames(JSONObject json) {
		boolean b = true; //true if json checks okay, false if it is incomplete
		if(request_params != null){ //returns true if request_params contains keys
			for(int i = 0; i < request_params.length; i++) {
				String param = request_params[i];
				if(!json.has(param)) {
					//logger.info("Request has missing parameter '" + param + "'!");
					b = false;
					break;
				}
			}
		}
		return b;
	}
	
	/**
	 * Checks whether the request secondary-parameters have values or are empty /n/n
	 * 
	 * ErrorCode: 10001 
	 * @param json The JSONObject representation of the request
	 * @return
	 */
	public boolean checkParameterValues(JSONObject json) {
		boolean b = true; //true if json checks okay, false if it has null parameters
		Object[] keys = json.keySet().toArray();
		for(int i = 0; i < keys.length; i++) {
			String key = (String) keys[i];
			if(json.get(key).equals("") || json.get(key) == null ) {
				b = false;
			}
		}
		return b;
	}
	
	protected void addResponse(Response r) {
		responses.add(r);
	}
	
	protected void addResponses(Vector<Response> r) {
		responses.addAll(r);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRequest_type() {
		return request_type;
	}

	public void setRequest_type(String request_type) {
		this.request_type = request_type;
	}

	public String[] getRequest_params() {
		return request_params;
	}

	public void setRequest_params(String[] request_params) {
		this.request_params = request_params;
	}

	protected MQTTHandler getMqttHandler() {
		return mqttHandler;
	}

	protected void setMqttHandler(MQTTHandler handler) {
		this.mqttHandler = handler;
	}
}
