package json.objects;

import org.json.JSONObject;

public class ReqPOOP extends AbstRequest {
	public String propSSID;
	public int propValue;
	public String propValueStr;

	public ReqPOOP(JSONObject json, String propIDParam, String propValParam) {
		super(json);
		propSSID = json.getString(propIDParam);
		propValueStr = json.getString(propValParam);
		try {
			propValue = Integer.parseInt(propValueStr);
		} catch(NumberFormatException e) {
			propValue = Integer.parseInt(propValueStr.substring(1));
		}
	}
	
	public ReqPOOP(ReqRequest request, String propIDParam, String propValParam) {
		super(request.getJSON());
		propSSID = request.getJSON().getString(propIDParam);
		propValueStr = request.getJSON().getString(propValParam);
		try {
			propValue = Integer.parseInt(propValueStr);
		} catch(NumberFormatException e) {
			propValue = Integer.parseInt(propValueStr.substring(1));
		}
	}
}
