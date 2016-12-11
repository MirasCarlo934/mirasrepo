/**
 * TransactionResponse documentation:
 * 		The TransactionResponse object serves as a container of the results of the transaction made by the TransactionEngine. In addition
 * 	to the properties of the Response object which the TransactionResponse object extends, the Response object contains three more
 * 	properties. 
 */
package main.objects.request_response;

import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import main.objects.DB.DBObject;

public class TransactionResponse extends Response {
	private static final String success_response_param = "success";
	private static final String obj_response_param = "obj";
	private static final String error_response_param = "error";
	private String error;
	private boolean successful;
	private Vector<DBObject> objects = new Vector<DBObject>(1,1);

	/**
	 * Instantiates the TransactionResponse.
	 * 
	 * @param requestID
	 * @param topic
	 * @param successful
	 * @param entrySSID The SSID of the object involved in the transaction
	 * @param error The error in the transaction. Set to null if transaction is successful
	 */
	public TransactionResponse(String requestID, String RTY, String topic, boolean successful, String error) {
		super(requestID, RTY, topic);
		setSuccessful(successful);
		setError(error);
	}

	/**
	 * @return the success of the TransactionEngine in processing the request
	 */
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * @param successful the successful to set
	 */
	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}
	
	/**
	 * Adds DBObject to the TransactionResponse's collection. Only invoked in the Select function of the TransactionEngine
	 * @param object
	 */
	public void addDBObject(DBObject object) {
		objects.addElement(object);
	}
	
	/**
	 * Adds multiple DBObjects to the TransactionResponse's collection. Only invoked in the Select function of the TransactionEngine
	 * @param object
	 */
	public void addDBObjects(Vector<DBObject> objects) {
		this.objects.addAll(objects);
	}

	/**
	 * Returns the DBObject processed by the TransactionEngine. Only returns values if a successful Select transaction was made. 
	 * Returns an empty vector otherwise.
	 * @return the object processed by the TransactionEngine
	 */
	public Vector<DBObject> getObjects() {
		return objects;
	}

	/**
	 * @param objects the objects to set
	 */
	public void setObjects(Vector<DBObject> objects) {
		this.objects = objects;
	}
	
	public JSONObject toJSONObject() {
		super.toJSONObject();
		
		json.put(success_response_param, successful);
		
		if(successful) {
			if(objects.isEmpty()) {
				json.put(obj_response_param, "none");
			}
			else {
				json.put(obj_response_param, objects);
			}
		}
		else {
			json.put(error_response_param, error);
		}
		
		return json;
	}

	/**
	 * @return the error
	 */
	protected String getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * The SSID of the object involved in the transaction. This is particularly important in the Insert function of the TransactionEngine
	 * in order to return the randomly generated ID back to the requesting component.
	 * 
	 * @return the entrySSID
	 */
	/*public String getEntrySSID() {
		return entrySSID;
	}*/

	/**
	 * @param entrySSID the entrySSID to set
	 */
	/*public void setEntrySSID(String entrySSID) {
		this.entrySSID = entrySSID;
	}*/
}
