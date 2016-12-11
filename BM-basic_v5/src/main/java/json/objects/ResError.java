package json.objects;

public class ResError extends AbstResponse {
	public String message;

	public ResError(String rid, String cid, String message) {
		super(rid, cid, false);
		super.addParameter("message", message);
		this.message = message;
	}
}
