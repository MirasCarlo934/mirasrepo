package main;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import json.objects.ReqRequest;
import main.modules.AbstModule;
import mqtt.MQTTHandler;

/**
 * This class is instantiated by the controller every time an MqttMessage arrives.
 * @author miras
 *
 */
public class ControllerModule implements Runnable {
	private static final Logger LOG = Logger.getLogger("BM_LOG.ControllerModule");
	private MqttMessage message;
	private MQTTHandler mh;
	private ComponentRepository devices;

	public ControllerModule(MqttMessage message, MQTTHandler mh, ComponentRepository devices) {
		this.message = message;
		this.mh = mh;
		this.devices = devices;
	}
	
	@Override
	public void run() {
		if(checkRequestValidity(message)) {
			ReqRequest request = new ReqRequest(new JSONObject(message.toString()));
			String rty = request.getString("RTY");	
			AbstModule m = (AbstModule) BusinessMachine.context.getBean(rty);
			m.processRequest(request);
		}
	}
	
	/**
	 * Checks if the request contains all the required primary parameters
	 * 
	 * @param request The Request object
	 * @return <b><i>True</b></i> if the request is valid, <b><i>false</i></b> if: <br>
	 * 		<ul>
	 * 			<li>The MQTT message is not in JSON format</li>
	 * 			<li>There are missing primary request parameters</li>
	 * 			<li>There are primary request parameters that are null/empty</li>
	 * 			<li>CID does not exist</li>
	 * 			<li>RTY does not exist</li>
	 * 		</ul>
	 */
	private boolean checkRequestValidity(MqttMessage message) {
		LOG.trace("Checking primary request parameters...");
		JSONObject json;
		
		//#1
		try {
			json = new JSONObject(message.toString());
		} catch(JSONException e) {
			sendError("Improper JSON construction!");
			return false;
		}
		
		//#2
		if(!json.keySet().contains("RID") && !json.keySet().contains("CID") && !json.keySet().contains("RTY")) {
			sendError("Request does not contain all primary request parameters!");
			return false;
		}
		
		//#3
		if(json.getString("RID").equals("") || json.getString("RID") == null) {
			sendError("Null RID!");
			return false;
		} else if(json.getString("CID").equals("") || json.getString("CID") == null) {
			sendError("Null CID!");
			return false;
		} else if(json.getString("RTY").equals("") || json.getString("RTY") == null) {
			sendError("Null RTY!");
			return false;
		}
		
		//#4
		if(json.getString("RTY").equals("register"));
		else if(!devices.containsDevice(json.getString("CID"))) {
			sendError("CID does not exist!");
			return false;
		}
		
		//#5
		boolean b = false;
		if(BusinessMachine.context.containsBean(json.getString("RTY"))) {
			b = true;
		}
		/*for(int i = 0; i < modules.length; i++) {
			String rty = modules[i].getRequestType();
			if(rty.equals(json.get("RTY"))) {
				break;
			} else {
				b = false;
			}
		}*/
		
		if(!b) {
			sendError("Invalid RTY!");
			return false;
		}
		else {
			LOG.trace("Primary request parameters good to go!");
			return true;
		}
	}
	
	private void sendError(String message) {
		LOG.error(message);
		mh.publishToErrorTopic(message);
	}
}
