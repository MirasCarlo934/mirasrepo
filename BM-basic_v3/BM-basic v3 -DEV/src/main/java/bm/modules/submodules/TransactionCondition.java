/**
 * TransactionCondition documentation
 * 		Every table in the database has a specific set of conditions in accepting data persistence that is too specific for the
 * 	TransactionEngine to manage. <br><br>
 * 		
 * 		For example, before persisting a reservation object into the database, its time frame must not coincide with the time frame of
 * 	other reservation objects with the same date and room that are already persisted in the database. This condition is too specific
 * 	for the TransactionEngine to keep check when it comes to persisting a reservation from a request. <br><br>
 * 
 * 		This is why the TransactionEngine makes use of TransactionCondtion objects instead. TransactionCondtion objects act like a
 * 	secondary checkpoint for a TransactionRequest to pass before any transaction can be made with the database. TransactionCondition
 * 	objects come into play after the validity of the incoming TransactionRequest is already verified. The TransactionEngine forwards the
 * 	TransactionRequest to the TransactionCondition object for further checking. If the TransactionRequest passes, then the transaction
 * 	can finally be executed. <br><br>
 * 
 * 		<b>For the programmer,</b> any DB table with further transaction conditions other than the basic conditions that are already handled by
 * 	the TransactionEngine must have its own TransactionCondition object. After the conditions are set in the TransactionCondition object,
 * 	this object must then be included in the list of TransactionCondition objects which can be done by adding it in the dependency of
 * 	the TransactionEngine in the Spring configuration XML.
 */
package bm.modules.submodules;

import org.apache.log4j.Logger;

import main.objects.TransactionType;
import main.objects.request_response.TransactionRequest;
import tools.TrafficController;

public abstract class TransactionCondition {
	private Logger logger;
	private String objectType;
	private String error;
	private TrafficController trafficController;

	public TransactionCondition(String name, String objectType, TrafficController trafficController) {
		setLogger(Logger.getLogger(name));
		setObjectType(objectType);
		setTrafficController(trafficController);
	}
	
	public boolean checkRequest(TransactionType type, TransactionRequest treq) {
		if(type.equals(TransactionType.SELECT)) {
			return selectCondition(treq);
		}
		else if(type.equals(TransactionType.INSERT)) {
			return insertCondition(treq);
		}
		else if(type.equals(TransactionType.UPDATE)) {
			return updateCondition(treq);
		}
		else {
			return deleteCondition(treq);
		}
	}
	
	protected abstract boolean selectCondition(TransactionRequest treq);
	
	protected abstract boolean insertCondition(TransactionRequest treq);
	
	protected abstract boolean updateCondition(TransactionRequest treq);
	
	protected abstract boolean deleteCondition(TransactionRequest treq);

	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	protected void setError(String error) {
		this.error = error;
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
	private void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	/**
	 * @return the logger
	 */
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * @param logger the logger to set
	 */
	protected void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * @return the trafficController
	 */
	protected TrafficController getTrafficController() {
		return trafficController;
	}

	/**
	 * @param trafficController the trafficController to set
	 */
	protected void setTrafficController(TrafficController trafficController) {
		this.trafficController = trafficController;
	}
}
