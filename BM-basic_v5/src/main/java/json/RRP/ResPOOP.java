package json.RRP;

public class ResPOOP extends AbstResponse {
	private String propSSID;
	private Object propVal;

	public ResPOOP(String rid, String cid, String rty, String propSSID, Object propVal) {
		super(rid, cid, rty, true);
		setPropSSID(propSSID);
		setPropVal(propVal);
	}
	
	public ResPOOP(AbstRequest request, String propSSID, Object propVal) {
		super(request, true);
		setPropSSID(propSSID);
		setPropVal(propVal);
	}

	public String getPropSSID() {
		return propSSID;
	}

	public void setPropSSID(String propSSID) {
		this.propSSID = propSSID;
		super.addParameter("property", propSSID);
	}

	public Object getPropVal() {
		return propVal;
	}

	public void setPropVal(Object propVal) {
		this.propVal = propVal;
		super.addParameter("value", propVal);
	}
}
