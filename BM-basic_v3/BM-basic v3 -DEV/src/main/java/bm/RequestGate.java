/**
 * RequestCheckerModule documentation:
 * 		An embedded Module. Checks the intercepted JSON from the MQTT broker if it is a valid request or not.
 * 
 * 		Because a JSON can easily be constructed using a String, errors can easily be made. The RequestCheckerModule addresses these errors
 * 	by tightly checking validity the JSON message that arrived from the MQTT broker, serving as the primary wall of defense of the
 * 	BusinessMachine against improper request construction.
 * 
 * 		On an added level of security, the RequestCheckerModule also checks for the validity of the "request_type" and "com_id" parameters
 * 	against the records of the BusinessMachine, given that the two were already provided in the request.
 * 	
 * 		The RequestCheckerModule checks for three cases:
 * 			1. Existence of the three primary request parameters: 
 * 				a. "request_id"
 * 				b. "request_type"
 * 				c. "com_id"
 * 			2. If 1b checks fine, check if a Module that can handle the "request_type" exists
 * 			3. If 1c checks fine, check existence of "com_id" in ComponentRepository
 * 				*skips this part if request calls for the initialization of a component
 * 			4. If 1a checks fine, check if "request_id" is not null
 * 
 * 		If one of the three cases doesn't check out, the RequestCheckerModule dynamically publishes all of the deficiencies of the request
 * 	 to the MQTT broker, where the component can listen in and receive the error message in JSON response format.
 */
package bm;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import bm.modules.parents.Module;
import main.TransTechSystem;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import mqtt.MQTTHandler;
import tools.TrafficController;

public class RequestGate implements Runnable {
	private static final Logger logger = Logger.getLogger(RequestGate.class);
	private static final String reigster_request_type = "register_com";
	//the following variables indicated the parameter key which maps to the request_id, request_type, and com_id respectively
	private static final String request_id_key = TransTechSystem.config.getRequestParamConfig().getRequestIdKey();
	private static final String request_type_key	= TransTechSystem.config.getRequestParamConfig().getRequestTypeKey();
	private static final String com_id_key = TransTechSystem.config.getRequestParamConfig().getComponentIdKey();
	
	private MQTTHandler mh;
	private ComponentRepository cr;
	private Vector<ErrorResponse> errorResponses = new Vector<ErrorResponse>(1,1);

	public RequestGate(TrafficController tc, MQTTHandler mh, ComponentRepository componentRepository) {
		//super("BM/JSONCheckerModule", "request_checker", null, tc, gateway);
		this.mh = mh;
		cr = componentRepository;
	}

	/**
	 * Checks the intercepted JSONObject if it is a valid request.
	 * 
	 * @param json
	 * @param modules
	 * @return The Request object if the intercepted JSONObject is valid, null if not.
	 * @throws Exception
	 */
	public Request checkJSON(JSONObject json, Vector<Module> modules) throws Exception {		
		Request request = null; //resets request value to null
		boolean a1 = true; //boolean indicator for case 1a
		boolean b1 = true; //boolean indicator for case 1b
		boolean c1 = true; //boolean indicator for case 1c
		boolean complete = true; //returns true after checking if request checks out
		boolean inRegistration = false; //returns true if requesting component is in registration
		
		logger.info("Starting request checking...");
		String msg = "error::";
		
		//Checks cases 1-3
		logger.trace("Checking cases 1-3!");
		if(!json.has(request_id_key)) {					//case 1a
			logger.trace("Request failed case 1a!");
			a1 = false;
			msg = msg + ":request_id";
		} else {
			if(json.getString(request_id_key).equals("")) {
				logger.trace("Request failed case 4!");
				a1 = false;
				msg = msg + ":request_id_null";
			}
		}
		
		if(!json.has(request_type_key)) {				//case 1b
			logger.trace("Request failed case 1b!");
			b1 = false;
			msg = msg + ":request_type";
		} else { 										//case 2
			boolean modIsFound = false; //states if a module was found
			for(int i = 0; i < modules.size(); i++) {
				Module m = modules.get(i);
				if(m.getRequest_type().equals(json.getString(request_type_key))) { //true if there is a Module that can handle the specified request_type
					modIsFound = true;
					
					//checks if request checks out on secondary request parameters
					if(m.checkParameterNames(json) && m.checkParameterValues(json)) {
						b1 = true;
					} else { 
						b1 = false;
						msg+=":2nd_param_reqs";
						if(!m.checkParameterNames(json)) {
							logger.error("JSON does not comply with secondary parameter requirements!");
							msg += "=noncompliance";
						} else if(!m.checkParameterValues(json)) {
							logger.error("JSON secondary request parameter/s are empty!");
							msg+= "=null";
						}
					}
					break;
				}
			}
			
			//checks if [request_type] is a system command
			for(int i = 0; i < TransTechSystem.config.getSystemConfig().getSystemCommands().length; i++) {
				if(TransTechSystem.config.getSystemConfig().getSystemCommands()[i].equals(json.getString(request_type_key))) {
					b1 = true;
				}
			}
			if(!modIsFound) { //if no Modules are found
				logger.trace("Request failed case 2!");
				b1 = false;
				msg = msg + ":request_type=invalid";
			}
		}
		
		if(!json.has(com_id_key)) {					//case 1c	
			logger.trace("Request failed case 1c!");
			c1 = false;
			msg = msg + ":com_id";
		} else if (!cr.containsComponent(json.getString(com_id_key))){  //case3
			//component existence checking in ComponentRepository
			if(json.has(request_type_key) && json.getString(request_type_key).equals(reigster_request_type)) { 
				//bypasses component existence checking since component is in registration
				c1 = true;
				inRegistration = true;
			} else {
				logger.trace("Request failed case 3!");
				c1 = false;
				msg = msg + ":com_id=nonexistent";
			}
		}
		
		//logger.debug(a1 + ":" + b1 + ":" + c1);
		//checks if request checks out
		if(a1 && b1 && c1) complete = true;
		else complete = false;
		
		//publishes the error message to the MQTT broker if request doesn't check out
		if(!complete) {
			logger.info("Request denied. Error message: " + msg);
			String topic = TransTechSystem.config.getMqttTopicConfig().getErrorTopic();
			String r_id = "null";
			if(c1 && cr.containsComponent(json.getString(com_id_key))) {
				topic = cr.getComponent(json.getString(com_id_key)).getMqttTopic();
			}
			if(a1) {
				r_id = json.getString(request_id_key);
			}
			ErrorResponse er = new ErrorResponse(r_id, "error", topic, msg);
			addErrorResponse(er);
			publishErrorResponses();
		} else {
			if(inRegistration) {
				request = new Request(json);
			} else {
				request = new Request(json, cr.getComponent(json.getString(TransTechSystem.config.getRequestParamConfig().getComponentIdKey())).getMqttTopic());
			}
			logger.info("Request accepted. Proceeding to request processing...");
		}
		return request;
	}
	
	private void publishErrorResponses() {
		Thread t = new Thread(this, "RequestCheckingProcess");
		t.start();
	}

	private void addErrorResponse(ErrorResponse er) {
		errorResponses.add(er);
	}
	
	/**
	 * Handles publishing to MQTTHandler to avoid thread crashing. Invoked by publishErrorResponses() method.
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(!errorResponses.isEmpty()) {
			try {
				mh.publish(errorResponses.remove(0));
			} catch (MqttException e) {
				logger.error("Cannot publish!", e);
			}
		}
	}
}
