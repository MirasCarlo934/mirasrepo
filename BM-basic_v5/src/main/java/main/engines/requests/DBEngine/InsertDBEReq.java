package main.engines.requests.DBEngine;

import java.util.HashMap;

public class InsertDBEReq extends DBEngineRequest {
	private HashMap<String, Object> values = new HashMap<String, Object>(1,1);

	/**
	 * Creates an InsertDBEReq which the DBEngine uses to create an Insert statement to the DB.
	 * 
	 * @param id The ID of this EngineRequest
	 * @param table The table name for this Insert statement
	 * @param values HashMap containing the column names and the new values of each for the new row. 
	 * 		<b>MUST</b> contain all columns of the table specified, if required. 
	 */
	public InsertDBEReq(String id, String table, HashMap<String, Object> values) {
		super(id, QueryType.INSERT, table);
		this.table = table;
		this.values = values;
		
		String scols = "";
    	String vals = "";
    	Object[] cols = values.keySet().toArray();
    	for(int i = 0; i < values.size(); i++) {
    		//puts all column names into a string for query construction
    		String col = (String) cols[i];
    		scols = scols + col + ",";
    		
    		//puts all values into a string for query construction
    		Object val = values.get(col);
    		if(val.getClass().equals(String.class)) { //if true, encloses the value in single quotes
    			vals = vals + "'" + val + "',";
    		} else {
    			vals = vals + val + ",";
    		}
    	}
    	scols = scols.substring(0, scols.length() - 1); //cuts off last comma
    	vals = vals.substring(0, vals.length() - 1); //cuts off last comma
    	
    	query = "insert into " + table + "(" + scols + ") values(" + vals + ")";
	}
}
