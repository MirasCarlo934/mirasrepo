package json.RRP;

import org.json.JSONObject;

public class ReqPOOP extends AbstRequest {
	public String propSSID;
	public Object propValue;

	public ReqPOOP(JSONObject json, String propIDParam, String propValParam) {
		super(json);
		propSSID = json.getString(propIDParam);
		propValue = json.get(propValParam);
	}
	
	public ReqPOOP(ReqRequest request, String propIDParam, String propValParam) {
		super(request.getJSON());
		propSSID = request.getJSON().getString(propIDParam);
		propValue = request.getJSON().get(propValParam);
	}
}
