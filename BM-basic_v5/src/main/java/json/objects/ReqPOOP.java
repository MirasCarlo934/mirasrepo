package json.objects;

import org.json.JSONObject;

public class ReqPOOP extends AbstRequest {
	public String propSSID;
	public int propValue;

	public ReqPOOP(JSONObject json, String propIDParam, String propValParam) {
		super(json);
		propSSID = json.getString(propIDParam);
		propValue = json.getInt(propValParam);
	}
	
	public ReqPOOP(ReqRequest request) {
		super(request.getJSON());
		propSSID = request.getJSON().getString("property");
		propValue = request.getJSON().getInt("value");
	}
}
