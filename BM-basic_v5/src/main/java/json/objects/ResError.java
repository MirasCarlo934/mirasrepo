package json.objects;

public class ResError extends AbstResponse {
	public String message;

	public ResError(String rid, String cid, String rty, String message) {
		super(rid, cid, rty, false);
		super.addParameter("message", message);
		this.message = message;
	}
	
	public ResError(AbstRequest request, String message) {
		super(request, false);
		addParameter("message", message);
		this.message = message;
	}
}
