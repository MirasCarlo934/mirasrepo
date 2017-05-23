package json.RRP;

import org.json.JSONObject;

public abstract class AbstRequest {
	public String rid;
	public String cid;
	public String rty;

	public AbstRequest(JSONObject json) {
		this.rid = json.getString("RID");
		this.cid = json.getString("CID");
		this.rty = json.getString("RTY");
	}
	
	public AbstRequest(String RID, String CID, String RTY) {
		this.rid = RID;
		this.cid = CID;
		this.rty = RTY;
	}
}
