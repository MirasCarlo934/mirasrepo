package bm;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import bm.modules.ShutdownModule;
import bm.modules.parents.Module;
import main.TransTechSystem;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.Request;
import mqtt.Callback;
import mqtt.MQTTHandler;

public class BM_Controller extends Callback {
	private static final Logger logger = Logger.getLogger(BM_Controller.class);
	private Vector<Module> modules = new Vector<Module>(1,1);
	private RequestGate requestCheckerModule; /**This Module is not included in the Modules collection*/
	private ShutdownModule sm;
	private MQTTHandler mh;
	/*
	 * The following are for easier value manipulation in case they need to be changed.
	 */
	//check system-config.xml for validity
	private static final String shutdown_command = "exit_system"; 
	private static final String error_topic = TransTechSystem.config.getMqttTopicConfig().getErrorTopic();
	
	public BM_Controller(RequestGate requestHandler, ShutdownModule shutdownModule, Vector<Module> modules,
			MQTTHandler mqttHandler) {
		this.modules = modules;
		this.setRequestCheckerModule(requestHandler);
		this.sm = shutdownModule;
		mh = mqttHandler;
	}

	public void processMessage(String topic, MqttMessage message) {
		String msg = message.toString();
		JSONObject json = null;
		logger.info("-----------REQUEST INTERCEPTED!!!-----------");
		try {
			try{ //tries to parse the request into JSON
				json = new JSONObject(msg);
			} catch(JSONException e) {
				logger.error("Error in JSON construction! Ignoring intercepted request.");
				//ErrorResponse er = new ErrorResponse("ERROR", error_topic, "Error in JSON construction!");
				//mh.publish(er);
			}
			Request r = getRequestCheckerModule().checkJSON(json, modules);
			if(r != null) { //true if request is valid
				//Request r = new Request(json);
				if(r.getRequestType().equals(shutdown_command)) {
					sm.shutdown();
				}
				else {
					for(int i = 0; i < modules.size(); i++) {
						Module m = modules.get(i);
						if(m.getRequest_type().equals(r.getRequestType())) {
							logger.info("Invoking " + m.getName() + "!");
							try {
								m.processRequest(r);
							} catch (Exception e) {
								logger.fatal("Cannot run " + m.getName(), e);
								ErrorResponse er = new ErrorResponse(r.getRequestID(), r.getRequestType(), r.getTopic(), e.getLocalizedMessage());
								mh.publish(er);
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.fatal("Request error!", e);
		}
	}
	
	public void addModule(Module newModule) {
		modules.add(newModule);
	}
	
	public RequestGate getRequestCheckerModule() {
		return requestCheckerModule;
	}

	public void setRequestCheckerModule(RequestGate requestCheckerModule) {
		this.requestCheckerModule = requestCheckerModule;
	}
}
