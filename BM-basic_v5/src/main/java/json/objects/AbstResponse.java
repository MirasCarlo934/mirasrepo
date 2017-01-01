package json.objects;

import org.json.JSONObject;

public abstract class AbstResponse {
	private JSONObject json = new JSONObject();
	public String rid;
	public String cid;
	public String rty;
	public boolean success;

	/**
	 * The constructor in case papa requests for it
	 * @param rid
	 * @param cid
	 * @param rty
	 
	public AbstResponse(String rid, String cid, String rty, boolean success) {
		this.rid = rid;
		this.cid = cid;
		this.rty = rty;
		this.success = success;
		json.put("RID", rid);
		json.put("RTY", rty);
		json.put("success", success);
	}*/
	
	/**
	 * Not the default, most intuitive, and most logical constructor
	 * @param rid
	 * @param cid
	 */
	public AbstResponse(String rid, String cid, String rty, boolean success) {
		this.rid = rid;
		this.cid = cid;
		this.rty = rty;
		this.success = success;
		json.put("RID", rid);
		json.put("RTY", rty);
		json.put("success", success);
	}
	
	/**
	 * The default, most intuitive, and most logical constructor
	 * @param rid
	 * @param cid
	 */
	public AbstResponse(AbstRequest request, boolean success) {
		this.rid = request.rid;
		this.cid = request.cid;
		this.rty = request.rty;
		this.success = success;
		json.put("RID", rid);
		json.put("RTY", rty);
		json.put("success", success);
	}
	
	protected void addParameter(String name, Object value) {
		json.put(name, value);
	}
	
	public String toString() {
		return json.toString();
	}
}
