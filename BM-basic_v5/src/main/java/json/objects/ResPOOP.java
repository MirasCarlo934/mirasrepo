package json.objects;

public class ResPOOP extends AbstResponse {
	private String propSSID;
	private int propVal;

	public ResPOOP(String rid, String cid, String propSSID, int propVal) {
		super(rid, cid, true);
		setPropSSID(propSSID);
		setPropVal(propVal);
	}
	
	public ResPOOP(AbstRequest request, String propSSID, int propVal) {
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

	public int getPropVal() {
		return propVal;
	}

	public void setPropVal(int propVal) {
		this.propVal = propVal;
		super.addParameter("value", propVal);
	}
}
