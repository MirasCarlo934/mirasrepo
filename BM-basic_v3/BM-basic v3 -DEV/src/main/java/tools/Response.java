package tools;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Response extends HashMap<String, Object>{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1974144714253195278L;
	private String request_id;
	
	public Response(JSONObject response) {	//for initialization using constructed JSONObject (must contain required parameters)
		super(1);
		String[] keys = new String[response.length()];
		response.keySet().toArray(keys);
		for(int i = 0; i < response.length(); i++) {
			String key = keys[i];
			
			/*boolean b = false; //true if object mapped to key can be split into smaller JSONArrays or JSONObject
			Object o = null;
			//if key maps to a JSONArray or JSONObject, this converts the JSONArray into a Vector or JSONObject into a HashMap
			if(response.optJSONArray(key) != null) { 
				JSONArray array = response.getJSONArray(key);
				b = true;
			}
			if(response.optJSONObject(key) != null) {
				JSONObject object = response.optJSONObject(key);
				b = true;
			}
			
			do{ //splits the Object o into smaller JSONArrays or JSONObjects
				if(o.getClass().equals(JSONArray.class)) {
					
				}
			} while(b);*/
			put(key, response.get(key));
		}
	}
	
	public Response(String request_id) {	//for initializing raw Response object (no parameters)
		super(1);
		this.request_id = request_id;
		put("request_id", request_id);
	}
	
	public Response(String request_id, Map<String, Object> map) {	//for initializing Response object with parameter map
		super(1);
		this.request_id = request_id;
		put("request_id", request_id);
		putAll(map);
	}
	
	public Response(Map<String, Object> map) {	//for initializing Response object with parameter map that includes request id
		putAll(map);
	}
	
	public String getRequest_id() {
		return request_id;
	}
	
	public void setRequest_id(String request_id) {
		this.request_id = request_id;
	}
}
