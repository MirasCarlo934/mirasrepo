/**
 * InsertTransactionRequest documentation:
 * 		This type of TransactionRequest is required by the Insert and Update function in the TransactionEngine. This TransactionRequest has an added
 * 	property named 'values' which is a HashMap which contains the values that must be inserted to the table. The HashMap 'values' must have
 * 	equal number and value of keys as the table where the values are to be inserted to.
 */
package main.objects.request_response;

import java.util.HashMap;

public class InsertTransactionRequest extends TransactionRequest {
	private HashMap<String, Object> values = new HashMap<String, Object>(1);
	
	/**
	 * Use this constructor when creating an InsertTransactionRequest for the Insert function of the TransactionEngine
	 * 
	 * @param request
	 * @param valuesToBeInserted
	 */
	public InsertTransactionRequest(Request request, HashMap<String, Object> valuesToBeInserted) {
		super(request, null);
		this.setValuesToBeInserted(valuesToBeInserted);
	}
	
	/**
	 * Use this constructor when creating an InsertTransactionRequest for the Insert function of the TransactionEngine.
	 * 
	 * <b>This constructor is only used by BM Engines.</b>
	 * 
	 * @param objectType
	 * @param valuesToBeInserted
	 */
	public InsertTransactionRequest(String objectType, HashMap<String, Object> valuesToBeInserted) {
		super(objectType, null);
		this.setValuesToBeInserted(valuesToBeInserted);
	}
	
	/**
	 * Use this constructor when creating an InsertTransactionRequest for the Update function of the TransactionEngine
	 * 
	 * @param request
	 * @param args
	 * @param valuesToBeInserted
	 */
	public InsertTransactionRequest(Request request, HashMap<String, Object> args, 
			HashMap<String, Object> valuesToBeInserted) {
		super(request, args);
		this.setValuesToBeInserted(valuesToBeInserted);
	}
	
	/**
	 * Use this constructor when creating an InsertTransactionRequest for the Update function of the TransactionEngine
	 * 
	 * <b>This constructor is only used by BM Engines.</b>
	 * 
	 * @param objectType
	 * @param args
	 * @param valuesToBeInserted
	 */
	public InsertTransactionRequest(String objectType, HashMap<String, Object> args, 
			HashMap<String, Object> valuesToBeInserted) {
		super(objectType, args);
		this.setValuesToBeInserted(valuesToBeInserted);
	}

	/**
	 * @return the values to be inserted
	 */
	public HashMap<String, Object> getValuesToBeInserted() {
		return values;
	}

	/**
	 * @param values the values to set
	 */
	public void setValuesToBeInserted(HashMap<String, Object> values) {
		this.values = values;
	}
}
