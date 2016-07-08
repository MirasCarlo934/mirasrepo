package main.objects.request_response;

import org.json.JSONObject;

import main.TransTechSystem;

/**
 * This type of request is used ONLY by BM engines that need to transact with the DB.
 */
public class BMRequest extends Request {

	public BMRequest() {
		super("breq", TransTechSystem.config.getMqttTopicConfig().getBMTopic(), "breq");
		// TODO Auto-generated constructor stub
	}
	
	public BMRequest(String request_type) {
		super("breq", TransTechSystem.config.getMqttTopicConfig().getBMTopic(), request_type);
	}
}
