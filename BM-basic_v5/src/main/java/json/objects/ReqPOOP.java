package json.objects;

import org.json.JSONObject;

public class ReqPOOP extends AbstRequest {
	public String propIndex;
	public int propValue;

	public ReqPOOP(JSONObject json) {
		super(json);
		propIndex = json.getString("property");
		propValue = json.getInt("value");
	}
	
	public ReqPOOP(ReqRequest request) {
		super(request.getJSON());
		propIndex = request.getJSON().getString("property");
		propValue = request.getJSON().getInt("value");
	}
}
