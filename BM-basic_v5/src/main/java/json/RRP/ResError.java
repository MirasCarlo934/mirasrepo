package json.RRP;

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
	
	/**
	 * Creates an instance of ResError SOLELY for the use of internal ERQS error handling.
	 * @param source the source of the error, preferrably the name of the object that issued
	 * 		the error
	 * @param message the error message
	 */
	public ResError(String source, String message) {
		super(source, "BM", "N/A", false);
		super.addParameter("message", message);
		this.message = message;
	}
}
