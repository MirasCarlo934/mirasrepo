/**
 * DBObject documentation:
 * 		Serves as a container of all the values and their subsequent column mapping of the entries retrieved from DB. The DBObject is usually
 * 	initialized after a Module executes a 'select' query. Each entry from the result of the 'select' query is then converted into a
 * 	DBObject which is then used to encode the entry into a JSONObject which will be published to the MQTT broker as part of the response.
 */
package main.objects.DB;

import java.util.HashMap;

import org.json.JSONObject;

public class DBObject extends HashMap<String, Object>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2701508256017110959L;
	//private HashMap<String, Object> map = new HashMap<String, Object>(1);

	public DBObject() {
		super(1);
	}	
	
	public JSONObject toJSONObject() {
		JSONObject json = new JSONObject();
		for(int i = 0; i < this.size(); i++) {
			String[] keys = new String[this.size()];
			this.keySet().toArray(keys);
			json.put(keys[i], this.get(keys[i]));
		}
		return json;
	}
}
