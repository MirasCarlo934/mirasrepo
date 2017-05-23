package json.RRP;

import org.json.JSONObject;

public class ReqPlex extends AbstRequest {
	public String command;

	public ReqPlex(JSONObject json, String plexCommandParam) {
		super(json);
		command = json.getString(plexCommandParam);
	}
}
