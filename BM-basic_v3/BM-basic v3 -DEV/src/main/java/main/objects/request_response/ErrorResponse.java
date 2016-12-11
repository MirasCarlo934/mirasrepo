package main.objects.request_response;

import main.TransTechSystem;
import main.objects.ErrorCode;

public class ErrorResponse extends Response {
	private ErrorCode errorCode;
	private String errorMsg = ""; //to be filled from DB

	public ErrorResponse(String requestID, String RTY, String topic, ErrorCode errorCode) {
		super(requestID, RTY, topic);
		setErrorCode(errorCode);
		json.put(TransTechSystem.config.getRequestParamConfig().getErrorKey(), errorCode.getCode());
		json.put(TransTechSystem.config.getRequestParamConfig().getErrorMsgKey(), errorMsg);
		json.put(Response.success_field, false);
	}
	
	/**
	 * Constructs a primitive ErrorResponse. Use only for testing purposes. For a formal and system-adequate ErrorResponse, use the first
	 * constructor.
	 * 
	 * @param requestID of the Request
	 * @param msg - details of the error that occurred
	 */
	public ErrorResponse(String requestID, String RTY, String topic, String msg) { 
		super(requestID, RTY, topic);
		setErrorCode(ErrorCode.NULL);
		setErrorMsg(msg);
		json.put(TransTechSystem.config.getRequestParamConfig().getErrorKey(), errorCode.getCode());
		json.put(TransTechSystem.config.getRequestParamConfig().getErrorMsgKey(), errorMsg);
		setSuccessful(false);
	}

	/**
	 * @return the errorCode
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * @return the errorMsg
	 */
	public String getErrorMsg() {
		return errorMsg;
	}

	/**
	 * @param errorMsg the errorMsg to set
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

}
