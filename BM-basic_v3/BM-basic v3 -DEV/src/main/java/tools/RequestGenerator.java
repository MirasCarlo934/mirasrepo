package tools;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import main.TransTechSystem;

public class RequestGenerator {
	private static final Logger logger = Logger.getLogger("RequestGenerator");
	private IDGenerator idGenerator;
	
	public RequestGenerator(IDGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}
	
	public JSONObject buildRequest(String request_type, String com_id, Map<String, Object> params) { //returns json to be published to MQTT broker
		JSONObject json = new JSONObject();
		json.put(TransTechSystem.config.getRequestParamConfig().getRequestIdKey(), idGenerator.generateMixedCharID(5));
		json.put(TransTechSystem.config.getRequestParamConfig().getRequestTypeKey(), request_type);
		json.put(TransTechSystem.config.getRequestParamConfig().getComponentIdKey(), com_id); //for determining which topic the response will be published in
		String[] params_keys = new String[params.size()];
		params.keySet().toArray(params_keys);
		for(int i = 0; i < params_keys.length; i++) { //adds params to json
			String key = params_keys[i];
			json.put(key, params.get(key));
		}
		//logger.debug(json.toString());
		return json;
	}
	
	public JSONObject buildRequest(String com_id, Map<String, Object> params) { //IF request_type is inside parameters
		return buildRequest((String)params.get(TransTechSystem.config.getRequestParamConfig().getRequestTypeKey()), com_id, params);
	}
	
	public JSONObject buildRequest(String com_id, String request_type) { //for command requests only (requests with no params)
		Map<String, Object> map = new HashMap<String, Object>(1);
		map.put(TransTechSystem.config.getRequestParamConfig().getRequestTypeKey(), request_type);
		return buildRequest(com_id, map);
	}
}
