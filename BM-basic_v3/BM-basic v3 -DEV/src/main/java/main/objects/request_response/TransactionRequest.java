package main.objects.request_response;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class TransactionRequest extends Request {
	private static final BMRequest bmreq = new BMRequest();
	private static final String obj_type_request_param = "obj_type";
	
	//private static final Logger logger = Logger.getLogger("TransactionRequest");
	private HashMap<String, Object> args = new HashMap<String, Object>(1);
	private String objectType;
	
	/**
	 * Basic transaction request needed by the Select and Delete functions of the TransactionEngine.
	 * 
	 * @param request The intercepted request.
	 * @param args The HashMap containing the arguments needed to perform the function. The keys must contain the column name and the
	 * 		values must contain the condition in which the transaction response must satisfy. This HashMap is used to build the 'where'
	 * 		clause of the query.
	 */
	public TransactionRequest(Request request, HashMap<String, Object> args) {
		super(request.toJSONObject(), request.getTopic());
		setArgs(args);
		setObjectType(request.getString(obj_type_request_param));
	}
	
	/**
	 * Basic transaction request needed by the Select and Delete functions of the TransactionEngine.
	 * 
	 * This transaction request constructor is only used by BusinessMachine Modules that wish to transact with the database using the
	 * TransactionEngine. 
	 * 
	 * @param request The intercepted request.
	 * @param args The HashMap containing the arguments needed to perform the function. The keys must contain the column name and the
	 * 		values must contain the condition in which the transaction response must satisfy. This HashMap is used to build the 'where'
	 * 		clause of the query. <i>Can be null and just set later with the <b>addArg()</b> method.</i>
	 */
	public TransactionRequest(String objectType, HashMap<String, Object> args) {
		//super(request.toJSONObject(), request.getTopic());
		super(bmreq.toJSONObject(), bmreq.getTopic());
		setArgs(args);
		setObjectType(objectType);
	}
	
	/**
	 * Adds a new argument for this transaction request
	 * @param colname
	 * @param value
	
	public void addArg(String colname, String value) {
		
	} */
	
	/**
	 * Get the conditions in which the TransactionEngine will use to perfrom the necessary process to process the request.
	 * 
	 * @return the args
	 */
	public HashMap<String, Object> getArgs() {
		return args;
	}

	/**
	 * @param args the args to set
	 */
	public void setArgs(HashMap<String, Object> args) {
		this.args = args;
	}

	/**
	 * @return the objectType
	 */
	public String getObjectType() {
		return objectType;
	}

	/**
	 * @param objectType the objectType to set
	 */
	public void setObjectType(String objectType) {
		put(obj_type_request_param, objectType);
		this.objectType = objectType;
	}
}
