package main.controller;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import json.objects.ReqRequest;
import main.BusinessMachine;
import main.ComponentRepository;
import main.modules.AbstModule;
import mqtt.MQTTHandler;

/**
 * This class is instantiated by the controller every time an MqttMessage arrives.
 * @author miras
 *
 */
public class ControllerModule implements Runnable {
	private static final Logger LOG = Logger.getLogger("BM_LOG.ControllerModule");
	//private int assignedThread;
	private int processNumber;
	private MqttMessage message;
	private MQTTHandler mh;
	private ComponentRepository cr;

	/**
	 * @param processNumber The process number assigned to this <i>ControllerModule</i> by
	 * 			the <i>Controller</i>
	 * @param message The <i>MqttMessage</i> that contains the request sent by an external
	 * 			component
	 * @param mh The <i>MQTTHandler</i> of this BusinessMachine
	 * @param cr The <i>ComponentRepository</i> of this BusinessMachine
	 */
	public ControllerModule(int processNumber, MqttMessage message, MQTTHandler mh, 
			ComponentRepository cr) {
		this.processNumber = processNumber;
		this.message = message;
		this.mh = mh;
		this.cr = cr;
	}
	
	@Override
	public void run() {
		//renameThread();
		if(checkPrimaryRequestValidity(message)) {
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
	private boolean checkPrimaryRequestValidity(MqttMessage message) {
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
		if(!json.keySet().contains("RID") || !json.keySet().contains("CID") || !json.keySet().contains("RTY")) {
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
		else if(!cr.containsComponent(json.getString("CID"))) {
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
	
	/**
	 * Returns the process number assigned to this <i>ControllerModule</i> by the <i>Controller</i>
	 * @return The process number
	 */
	public int getProcessNumber() {
		return processNumber;
	}
	
	/**
	 * Returns the thread number of the thread assigned to handle this <i>ControllerModule</i> 
	 * by the <i>ThreadPoolExecutor</i> in <i>Controller</i>
	 * 
	 * @return The assigned thread number
	
	public int getAssignedThread() {
		return assignedThread;
	} */
}
