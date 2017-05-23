package json.RRP;

import org.json.JSONObject;

public class ReqOHCommand extends AbstRequest {
	public String command;

	public ReqOHCommand(JSONObject json, String commandParam) {
		super(json);
		command = json.getString(commandParam);
	}
}
