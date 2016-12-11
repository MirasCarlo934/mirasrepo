/**
 * TransactionEngine documentation
 * 		In an effort to standardize the operation of interacting with the database, the TransactionEngine is born. This BusinessMachine-embedded
 * 	component has two main purposes: provide a standard way of database interaction and handle the possible errors that may occur in this
 * 	interaction. This process of database interaction and error handling is called a <b><i>transaction</i></b>. <br><br>
 * 
 * 		One of the sole purposes of the TransactionEngine is to standardize database interaction. In the first versions of the BusinessMachine,
 * 	database interaction is handled by the individual Modules. This created a scenario of confusing processes where one Module's process of 
 * 	handling data from the database is very different from the other Modules although both basically produce the same result. 
 * 	The TransactionEngine addresses this problem by already providing the processes of handling data which the Modules can simply make use
 * 	of. <br><br>
 * 
 * 		The second purpose of the TransactionEngine is to streamline error handling in database interaction. Most, if not all cases of
 * 	database interaction involve the same conditions that need to be met before any transaction can be made. Some of these conditions
 * 	are making sure that foreign key constraints are met and the values of an object must be complete before it can be persisted into
 * 	the database.
 */
package bm;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import bm.modules.submodules.TransactionCondition;
import main.TransTechSystem;
import main.objects.SSIDType;
import main.objects.TransactionType;
import main.objects.DB.ColumnInfo;
import main.objects.DB.DBObject;
import main.objects.DB.DBTableConfig;
import main.objects.DB.ForeignKeyRelation;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.InsertTransactionRequest;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import main.objects.request_response.TransactionRequest;
import main.objects.request_response.TransactionResponse;
import tools.IDGenerator;
import tools.TrafficController;

public class TransactionEngine {
	private static final String ssid_col_name = TransTechSystem.config.getDatabaseConfig().getSsidColName(); //this is in all caps
	public static final String objtype_param_name = "obj_type";
	
	private static final Logger logger = Logger.getLogger(TransactionEngine.class);
	private TransactionCondition[] transactionConditions;
	private TrafficController trafficController;
	private IDGenerator idg;
	private String error = "none"; //contains the error in the transaction if there is any

	/**
	 * @param name
	 * @param trafficController
	 */
	public TransactionEngine(TrafficController trafficController, IDGenerator idGenerator, TransactionCondition[] transactionConditions) {
		this.setTrafficController(trafficController);
		this.idg = idGenerator;
		this.transactionConditions = transactionConditions;
	}
	
	/**
	 * Handles insertion of values into DB. Requires InsertTransactionRequest over the normal TransactionRequest.
	 * 
	 * @param treq The InsertTransactionRequest (may or may not include the SSID of the new entry, if not, the method
	 * 		will automatically generate the SSID)
	 * @return a TransactionResponse with the SSID of the newly inserted entry
	 * @throws SQLException
	 */
	public TransactionResponse insert(InsertTransactionRequest treq) throws SQLException {
		TransactionResponse tres = null;
		
		if(checkInsertRequestValidity(treq)) {
			if(checkTransactionConditions(TransactionType.INSERT, treq)) {
				logger.trace("Inserting values to DB...");
				String id = null;
				DBTableConfig table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(treq.getObjectType());
				
				//logger.debug(treq.getValuesToBeInserted().get(ssid_col_name));
				if(treq.getValuesToBeInserted().containsKey(ssid_col_name.toLowerCase())) { //true if SSID is already specified in request values to be inserted
					id = (String) treq.getValuesToBeInserted().get(ssid_col_name.toLowerCase());
				} 
				else { //generates SSID
					//checks for redundancies in ID generation
					logger.trace("Generating object id...");
					ResultSet rs = getTrafficController().selectQuery(TransTechSystem.config.getDatabaseConfig().getSsidColName(), 
							table.getTableName());
					String[] ids = new String[0];
					int i = 0;
					if(rs.last()) {
						ids = new String[rs.getRow()];
						rs.beforeFirst();
					}
					while(rs.next()) {
						String ssid = rs.getString(ssid_col_name);
						ids[i] = ssid;
						i++;
					}
					
					if(table.getSSIDType().equals(SSIDType.INT))
						id = idg.generateIntID(TransTechSystem.config.getDatabaseConfig().getSsidLength(), ids);
					else if(table.getSSIDType().equals(SSIDType.MIXEDCHAR)) {
						id = idg.generateMixedCharID(TransTechSystem.config.getDatabaseConfig().getSsidLength(), ids);
					}
					treq.getValuesToBeInserted().put(ssid_col_name, id);
				}
				
				//inserts request to DB
				logger.trace("Persisting object...");
				getTrafficController().insertQuery(table.getTableName(), treq.getValuesToBeInserted());
				logger.trace("Request persisted into DB!");
				tres = new TransactionResponse(treq.getRequestID(), treq.getRequestType(), treq.getTopic(), true, null);
				
				//adds newly generated ssid of inserted object to the transaction response
				tres.put(ssid_col_name.toLowerCase(), id);
			}
			else {
				tres = new TransactionResponse(treq.getRequestID(), treq.getRequestType(), treq.getTopic(), false, error);
			}
		}
		else {
			tres = new TransactionResponse(treq.getRequestID(), treq.getRequestType(), treq.getTopic(), false, error);
		}
		
		return tres;
	}
	
	/**
	 * Deletes specific entries from an SQL table using the 'where' clause constructed from the TransactionRequest args property. <br><br>
	 * 
	 * <b>Current functionality only supports deletion based on SSID</b>
	 * 
	 * @param treq
	 * @return
	 * @throws SQLException
	 */
	public TransactionResponse delete(TransactionRequest treq) throws SQLException {
		TransactionResponse response = null;
		
		if(checkDeleteRequestValidity(treq)) {
			logger.trace("Deleting entry from DB...");
			DBTableConfig table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(treq.getObjectType());
			String rid = treq.getRequestID();
			String topic = treq.getTopic();
			
			//deletes entry from table
			try { //checks if entry to be deleted is an exported key with dependencies
				response = new TransactionResponse(rid, treq.getRequestType(), topic, true, null);
				getTrafficController().deleteQuery(table.getTableName(), treq.getArgs());
				logger.trace("Entry with SSID:" + treq.getArgs().get(TransTechSystem.config.getDatabaseConfig().getSsidColName().toLowerCase()) + 
						" deleted from table '" + table.getTableName() + "'!");
			} catch (SQLException e) {
				//checks if error was caused by a foreign key constraint violation
				if(e.getErrorCode() == 30000) { //error 2: Deletion of a foreign key with existing dependencies
					response = new TransactionResponse(rid, treq.getRequestType(), topic, false, error);
					logger.error("Deletion of a foreign key with existing dependencies!", e);
					setError("2022");
				}
			}
		} else {
			response = new TransactionResponse(treq.getRequestID(), treq.getRequestType(), treq.getTopic(), false, error);
		}
		
		return response;
	}
	
	public TransactionResponse update(InsertTransactionRequest treq) throws SQLException {
		TransactionResponse tres = null;
		
		if(checkUpdateRequestValidity(treq)) {
			if(checkTransactionConditions(TransactionType.UPDATE, treq)) {
				DBTableConfig table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(treq.getObjectType());
				String ssid = (String) treq.getArgs().get(ssid_col_name.toLowerCase());
				
				//updates entry in DB
				//logger.trace("Updating object with ssid '" + ssid + "' in DB...");
				logger.trace("Updating object in DB...");
				//getTrafficController().updateQuery(table.getTableName(), ssid, treq.getValuesToBeInserted());
				getTrafficController().updateQuery(table.getTableName(), treq.getArgs(), treq.getValuesToBeInserted());
				logger.trace("Object updated!");
				tres = new TransactionResponse(treq.getRequestID(), treq.getRequestType(), treq.getTopic(), true, null);
			}
			else {
				tres = new TransactionResponse(treq.getRequestID(), treq.getRequestType(), treq.getTopic(), false, error);
			}
		}
		else {
			tres = new TransactionResponse(treq.getRequestID(), treq.getRequestType(), treq.getTopic(), false, error);
		}
		
		return tres;
	}
	
	/**
	 * Selects specific entries from an SQL Table using the 'where' clause constructed from the TransactionRequest args property.
	 * @param treq
	 * @return
	 * @throws SQLException
	 */
	public TransactionResponse selectSpecific(TransactionRequest treq) throws SQLException {
		TransactionResponse response = null;
		String RID = treq.getRequestID();
		String topic = treq.getTopic();
		DBTableConfig table = null;
		String objName = treq.getObjectType();
		
		if(checkRequestValidity(treq)) {
			response = new TransactionResponse(RID, treq.getRequestType(), topic, true, null);
			logger.trace("Retrieving objects from DB...");
			table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(objName);
			response.addDBObjects(select(getTrafficController().selectQuery("*", table.getTableName(), treq.getArgs())));
			logger.trace(response.getObjects().size() + " object/s retrieved!");
		} else {
			response = new TransactionResponse(RID, treq.getRequestType(), topic, false, error);
		}
		
		return response;
	}
	
	/**
	 * Selects all entries from an SQL table. TransactionRequest args property can be null.
	 * @param treq
	 * @return
	 * @throws SQLException
	 */
	public TransactionResponse selectAll(TransactionRequest treq) throws SQLException {
		TransactionResponse response = null;
		
		String RID = treq.getRequestID();
		String topic = treq.getTopic();
		DBTableConfig table = null;
		String objName = treq.getObjectType();
		
		if(checkRequestValidity(treq)) {
			response = new TransactionResponse(RID, treq.getRequestType(), topic, true, null);
			logger.trace("Retrieving objects from DB...");
			table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(objName);
			response.addDBObjects(select(getTrafficController().selectQuery("*", table.getTableName())));
			logger.trace(response.getObjects().size() + " object/s retrieved!");
		} else {
			response = new TransactionResponse(RID, treq.getRequestType(), topic, false, error);
		}
		
		return response;
	}
	
	private Vector<DBObject> select(ResultSet rs) throws SQLException {
		Vector<DBObject> entries = new Vector<DBObject>(1,1);
		
		//retrieve entries from DB
		while(rs.next()) {
			DBObject dbo = new DBObject();
			ResultSetMetaData keys = rs.getMetaData();
			for(int i = 0; i < keys.getColumnCount(); i++) {
				String col = keys.getColumnName(i + 1);
				Object val = rs.getObject(col);
				//logger.debug(val);
				dbo.put(col.toLowerCase(), val);
			}
			entries.add(dbo);
		}
		//logger.debug(entries.isEmpty());
		return entries;
	}
	
	/**
	 * Checks the validity of a TransactionRequest to be processed by the Delete function. <br><br>
	 * 
	 * Checks if an entry with the specified SSID exists in the specified table of the database.
	 * 
	 * @param tr The TransactionRequest
	 * @return
	 * @throws SQLException 
	 */
	private boolean checkDeleteRequestValidity(TransactionRequest tr) throws SQLException {
		boolean b = checkRequestValidity(tr);
		
		/*
		 * Continues from basic checking
		 */
		if(b) {
			DBTableConfig table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(tr.getObjectType());
			ResultSet rs = getTrafficController().selectQuery("*", table.getTableName(), tr.getArgs());
			
			//checks if entry with specified ssid exists in DB
			if(!rs.next()) { //error 1: entry does not exist in DB
				b = false;
				logger.error("Entry does not exist in DB!");
				setError("2021");
			}
		}
		logger.trace("Checking complete! Error: '" + error + "'");
		return b;
	}
	
	/**
	 * Checks the validity of an InsertTransactionRequest. <br><br>
	 * 
	 * Checks if the HashMap 'values' property of the InsertTransactionRequest have the same amount and name of columns as the SQL table
	 * the request pertains to in the DB.
	 * 
	 * Also checks Constraint Violations that may occur in the persistence of the InsertTransactionRequest
	 *  -Primary Key duplication violation
	 * 	-Foreign Key nonexistence violation
	 * 
	 * @param itr The InsertTransactionRequest
	 * @return
	 * @throws SQLException 
	 */
	private boolean checkInsertRequestValidity(InsertTransactionRequest itr) throws SQLException {
		boolean b = checkRequestValidity(itr); //checks basic T-Req properties
		
		/*
		 * Continues from basic checking
		 */
		if(b) { 
			HashMap<String, Object> values = itr.getValuesToBeInserted(); //all keys MUST be lowercase according to the secondary parameter request
			Vector<String> columns = new Vector<String>(1,1);
			DBTableConfig table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(itr.getObjectType());
			
			//puts table columns into vector. NOTE: SSID is excluded in checking
			for(int i = 0; i < table.getColumns().length; i++) {
				columns.add(table.getColumns()[i].getColumnName());
			}
			columns.remove(TransTechSystem.config.getDatabaseConfig().getSsidColName());
			
			//checks if values have the same number and name of columns
			if(values != null) { //values CANNOT be null
				//logger.debug(values.size());
				//logger.debug(columns.size());
				/*
				 * Checks if values specified an ssid
				 * If specified, remove it from the hashmap to proceed with the checking of number of columns
				 * ssid can be specified in values, but MUST BE in lowercase
				 * ssid is re-included in values AFTER checking the number of columns
				 */
				String ssid = null;
				if(values.containsKey(ssid_col_name.toLowerCase())) {
					ssid = (String) values.remove(ssid_col_name.toLowerCase());
				}
				
				if(values.size() == columns.size()) { //values and columns MUST have the same size
					Object[] keys = values.keySet().toArray();
					for(int i = 0; i < keys.length; i++) {
						String key = (String) keys[i];
						for(int j = 0; j < columns.size(); j++){
							if(columns.get(j).equalsIgnoreCase(key)) {
								b = true;
								break;
							} else {
								b = false;
							}
						}
						
						if(!b) { //error 5: Insert column not a valid column name!
							logger.error("Insert column not a valid column name! Column:'" + key + "'");
							setError("2012");
							break;
						}
					}
				} else { //error 4: Number of keys inserted is not equal to number of columns in table!
					b = false;
					logger.error("Number of keys inserted is not equal to number of columns in table!");
					setError("2011");
				}
				
				/*
				 * re-includes ssid to values (if there was an ssid specified before checking)
				 */
				if(ssid != null) {
					values.put(ssid_col_name.toLowerCase(), ssid);
				}
			} else { //error 3: No values to be inserted!
				b = false;
				logger.error("No values to be inserted!");
				setError("2010");
			}
			
			
			/*
			 * starts checking on Constraint Violations GIVEN THAT the request passes the basic test
			 */
			if(b) {
				Vector<ColumnInfo> uniqKeys = new Vector<ColumnInfo>(1,1); //all values within are uppercase
				HashMap<String, ForeignKeyRelation> impKeys = new HashMap<String, ForeignKeyRelation>(1); //all values within are uppercase
				
				//gets unique and imported keys
				for(int i = 0; i < table.getColumns().length; i++) {
					ColumnInfo c = table.getColumns()[i];
					if(c.isUnique()) {
						uniqKeys.add(c);
					}
					if(c.getForeignKey() != null) {
						impKeys.put(c.getColumnName(), c.getForeignKey());
					}
				}
				
				/*
				 * checks for duplicates in unique keys in the same table NOTE: IGNORES CASE
				 */
				for(int i = 0; i < uniqKeys.size(); i++) {
					ColumnInfo uniqKey = uniqKeys.get(i);
					String val = (String) values.get(uniqKey.getColumnName().toLowerCase());
					if(values.containsKey(uniqKey.getColumnName().toLowerCase())) {
						ResultSet rs = null;
						
						if(uniqKey.getDataType().equalsIgnoreCase("VARCHAR")) {
							rs = getTrafficController().selectQuery("*", table.getTableName(), 
									"UPPER(" + uniqKey.getColumnName() + ") LIKE UPPER('" + val + "')");
						}
						else {
							rs = getTrafficController().selectQuery("*", table.getTableName(), uniqKey.getColumnName() + " = " + val);
						}
						
						if(rs.next()) { //error 6: Value to be inserted to [colname] already exists!
							b = false;
							//setError("value '" + val.toUpperCase() + "' already exists in " + uniqKey.getColumnName().toUpperCase() + 
									//" column!");
							setError("2013");
							break;
						}
					}
				}
				
				/*
				 * checks if value of the FK column is valid NOTE: IGNORES CASE
				 */
				Object[] reqCols = values.keySet().toArray(); //column names of the values to be inserted
				for(int j = 0; j < reqCols.length; j++) {
					String reqCol = (String) reqCols[j]; //column name
					Object val = values.get(reqCol); //value to be inserted under the column name
					
					//checks if value exists in the FOREIGN key of the FOREIGN table
					if(impKeys.containsKey(reqCol.toUpperCase())) {
						ForeignKeyRelation fkr = impKeys.get(reqCol.toUpperCase());
						ResultSet rs = null;
						/*logger.debug(fkr.getColumnName());
						logger.debug(fkr.getTableName());
						logger.debug(reqCol);
						logger.debug(val);*/
						
						if(val.getClass().equals(String.class)) {
							rs = getTrafficController().selectQuery("*", fkr.getTableName(), 
									"UPPER(" + fkr.getColumnName() + ") LIKE UPPER('" + val + "')");
						} else {
							rs = getTrafficController().selectQuery("*", fkr.getTableName(), fkr.getColumnName() + " = " + val);
						}
						
						if(rs.next()) { //Foreign key value exists!
							b = true;
						} else { //error 7: Foreign key value [value] does NOT exist!
							b = false;
							logger.error("Foreign key value '" + ((String) val).toUpperCase() + "' does not exist!");
							setError("2014");
							break;
							//setError("Foreign key value '" + val.toUpperCase() + "' does not exist!");
						}
					}
				}	
			}
		}
		logger.trace("Checking complete! Error: '" + error + "'");
		return b;
	}
	
	/**
	 * Checks the validity of an UpdateTransactionRequest. <br><br>
	 * 
	 * Checks Constraint Violations that may occur in updating an entry in an SQL table.
	 * 	-Primary Key duplication violation
	 * 	-Foreign Key nonexistence violation
	 * 
	 * @param itr The InsertTransactionRequest
	 * @return
	 * @throws SQLException 
	 */
	private boolean checkUpdateRequestValidity(InsertTransactionRequest itr) throws SQLException {
		boolean b = checkRequestValidity(itr); //checks basic T-Req properties
		
		/*
		 * Continues from basic checking
		 */
		if(b) { 
			HashMap<String, Object> values = itr.getValuesToBeInserted(); //all keys MUST be lowercase according to the secondary parameter request
			Vector<String> columns = new Vector<String>(1,1);
			DBTableConfig table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(itr.getObjectType());
			
			//puts table columns into vector. NOTE: SSID is excluded in checking
			for(int i = 0; i < table.getColumns().length; i++) {
				columns.add(table.getColumns()[i].getColumnName());
			}
			columns.remove(TransTechSystem.config.getDatabaseConfig().getSsidColName());
			
			//checks if values have the same number and name of columns
			if(values == null) { //error 3: No values to be inserted!
				b = false;
				logger.error("No values to be inserted!");
				setError("2010");
			}
			
			/*
			 * checks if keys of the HashMap vals are valid columns
			 */
			HashMap<String, Object> vals = itr.getValuesToBeInserted();
			if(table != null) {
				if(vals != null) { //vals cannot be null!
					String[] colnames = new String[table.getColumns().length];
					for(int i = 0; i < colnames.length; i++) {
						colnames[i] = table.getColumns()[i].getColumnName();
					}
					Object[] keys = vals.keySet().toArray();
					
					for(int i = 0; i < keys.length; i++) {
						String key = (String) keys[i];
						for(int j = 0; j < colnames.length; j++){
							if(colnames[j].equalsIgnoreCase(key)) {
								b = true;
								break;
							} else {
								b = false;
							}
						}
						
						if(!b) { //error 2: Key not a valid column name!
							logger.error("Key in 'vals' not a valid column name! Key:" + key);
							setError("2001");
							break;
						}
					}
				} else {
					b = false;
					logger.error("No values to be inserted!");
					setError("2010");
				}
			}
			
			
			/*
			 * starts checking on Constraint Violations GIVEN THAT the request passes the basic test
			 */
			if(b) {
				Vector<ColumnInfo> uniqKeys = new Vector<ColumnInfo>(1,1); //all values within are uppercase
				HashMap<String, ForeignKeyRelation> impKeys = new HashMap<String, ForeignKeyRelation>(1); //all values within are uppercase
				
				//gets unique and imported keys
				for(int i = 0; i < table.getColumns().length; i++) {
					ColumnInfo c = table.getColumns()[i];
					if(c.isUnique()) {
						uniqKeys.add(c);
					}
					if(c.getForeignKey() != null) {
						impKeys.put(c.getColumnName(), c.getForeignKey());
					}
				}
				
				/*
				 * checks for duplicates in unique keys in the same table NOTE: IGNORES CASE
				 */
				for(int i = 0; i < uniqKeys.size(); i++) {
					ColumnInfo uniqKey = uniqKeys.get(i);
					String val = (String) values.get(uniqKey.getColumnName().toLowerCase());
					if(values.containsKey(uniqKey.getColumnName().toLowerCase())) {
						ResultSet rs = null;
						
						if(uniqKey.getDataType().equalsIgnoreCase("VARCHAR")) {
							rs = getTrafficController().selectQuery("*", table.getTableName(), 
									"UPPER(" + uniqKey.getColumnName() + ") LIKE UPPER('" + val + "')");
						}
						else {
							rs = getTrafficController().selectQuery("*", table.getTableName(), uniqKey.getColumnName() + " = " + val);
						}
						
						if(rs.next()) { //error 4: Value to be inserted to [colname] already exists!
							b = false;
							//setError("value '" + val.toUpperCase() + "' already exists in " + uniqKey.getColumnName().toUpperCase() + 
									//" column!");
							setError("2013");
							break;
						}
					}
				}
				
				/*
				 * checks if value of the FK column is valid NOTE: IGNORES CASE
				 */
				Object[] reqCols = values.keySet().toArray(); //column names of the values to be inserted
				for(int j = 0; j < reqCols.length; j++) {
					String reqCol = (String) reqCols[j]; //column name
					Object val = values.get(reqCol); //value to be inserted under the column name
					
					//checks if value exists in the FOREIGN key of the FOREIGN table
					if(impKeys.containsKey(reqCol.toUpperCase())) {
						ForeignKeyRelation fkr = impKeys.get(reqCol.toUpperCase());
						ResultSet rs = null;
						
						if(val.getClass().equals(String.class)) {
							rs = getTrafficController().selectQuery("*", fkr.getTableName(), 
									"UPPER(" + fkr.getColumnName() + ") LIKE UPPER('" + val + "')");
						} else {
							rs = getTrafficController().selectQuery("*", fkr.getTableName(), fkr.getColumnName() + " = " + val);
						}
						
						if(rs.next()) { //Foreign key value exists!
							b = true;
						} else { //error 5: Foreign key value [value] does NOT exist!
							b = false;
							setError("2014");
							break;
							//setError("Foreign key value '" + val.toUpperCase() + "' does not exist!");
						}
					}
				}	
			}
		}
		logger.trace("Checking complete! Error: '" + error + "'");
		return b;
	}
	
	/**
	 * For basic checking of TransactionRequest validity. <b>This method is only invoked by the other T-Req validity-checking methods and not
	 * by the CRUDS methods themselves.</b><br><br>
	 * 
	 * Checks on: <br>
	 * 1. Validity of object type. <br>
	 * 2. Keys of T-Req HashMap 'args' are valid columns. <br>
	 * @param request
	 * @return
	 */
	private boolean checkRequestValidity(TransactionRequest request) {
		error = "none"; //resets error
		logger.trace("TransactionEngine invoked!");
		logger.trace("Checking validity of TransactionRequest...");
		boolean b = true; //true if request is valid
		String objName = request.getObjectType();
		DBTableConfig table = null;
		
		/*
		 * checks if object type exists
		 */
		if(TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().containsKey(objName)){
			b = true;
			table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(objName);
		}
		else { //error 1: object type does not exist
			b = false;
			logger.error("Object type '" + objName + "' does not exist!");
			setError("2000");
		}
		
		/*
		 * checks if keys of the HashMap args are valid columns
		 */
		HashMap<String, Object> args = request.getArgs();
		if(table != null) {
			if(args != null) { //args can be null if [conditions] = "all" OR if request is Insert
				String[] colnames = new String[table.getColumns().length];
				for(int i = 0; i < colnames.length; i++) {
					colnames[i] = table.getColumns()[i].getColumnName();
				}
				Object[] keys = args.keySet().toArray();
				
				for(int i = 0; i < keys.length; i++) {
					String key = (String) keys[i];
					for(int j = 0; j < colnames.length; j++){
						if(colnames[j].equalsIgnoreCase(key)) {
							b = true;
							break;
						} else {
							b = false;
						}
					}
					
					if(!b) { //error 2: Key not a valid column name!
						logger.error("Key '" + key + "' not a valid column name!");
						setError("2001");
						break;
					}
				}
			}
		}
		
		return b;
	}
	
	private boolean checkTransactionConditions(TransactionType tt, TransactionRequest r) {
		logger.trace("Checking transaction conditions...");
		boolean b = true;
		String objectType = (String) r.get(objtype_param_name);
		
		for(int i = 0; i < transactionConditions.length; i++) {
			TransactionCondition tc = transactionConditions[i];
			if(tc.getObjectType().equals(objectType)) {
				b = tc.checkRequest(tt, r);
				if(!b) {
					logger.trace("Request failed a transaction condition!");
					setError(tc.getError());
				}
			}
		}
		
		if(b) {
			logger.trace("Request passed all transaction conditions!");
		}
		
		return b;
	}

	/**
	 * @return the trafficController
	 */
	public TrafficController getTrafficController() {
		return trafficController;
	}

	/**
	 * @param trafficController the trafficController to set
	 */
	public void setTrafficController(TrafficController trafficController) {
		this.trafficController = trafficController;
	}

	/**
	 * Returns the error from the transaction. Is only invoked when transaction is not successful.
	 * 
	 * @return the error
	 */
	public String getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}

}
