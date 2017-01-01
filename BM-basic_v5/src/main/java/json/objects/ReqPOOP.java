package json.objects;

import org.json.JSONObject;

public class ReqPOOP extends AbstRequest {
	public String propSSID;
	public int propValue;

	public ReqPOOP(JSONObject json) {
		super(json);
		propSSID = json.getString("property");
		propValue = json.getInt("value");
	}
	
	public ReqPOOP(ReqRequest request) {
		super(request.getJSON());
		propSSID = request.getJSON().getString("property");
		propValue = request.getJSON().getInt("value");
	}
}
