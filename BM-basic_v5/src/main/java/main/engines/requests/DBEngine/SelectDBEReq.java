package main.engines.requests.DBEngine;

import java.util.HashMap;

public class SelectDBEReq extends DBEngineRequest {
	private String[] columns;
	private HashMap<String, Object> where = new HashMap<String, Object>(1,1); //colname and specified value

	/**
	 * Creates a SelectDBEReq which the DBEngine uses to create a Select statement to the DB. This
	 * constructor creates a <i>complete</i> Select statement, including the where statement.
	 * 
	 * @param id The ID of this EngineRequest
	 * @param table The table name for this Select statement
	 * @param columns The column names to be retrieved from the specified table.
	 * @param where HashMap containing the column name and the required column value for each. This is used
	 * 		in the where statement
	 */
	public SelectDBEReq(String id, String table, String[] columns, HashMap<String, Object> where) {
		super(id, QueryType.SELECT, table);
		this.columns = columns;
		
		String cols = "";
    	for(int i = 0; i < columns.length; i++) {
    		cols = cols + columns[i] + ",";
    	}
    	
    	String w = "";
    	String[] colParams = where.keySet().toArray(new String[0]);
    	String[] vals = where.values().toArray(new String[0]);
    	for(int i = 0; i < vals.length; i++) {
    		String val = "";
        	if(vals[i].getClass().equals(String.class)) { //if true, encloses the value in single quotes
        		val = "'" + vals[i] + "'";
        	} else {
        		val = vals[i].toString();
        	}
        	
    		w = w +  colParams[i] + "=" + val + " AND ";
    	}
    	cols = cols.substring(0, cols.length() - 1); //cuts off last comma
		query = "select " + cols + " from " + table + " where " + w;
	}
	
	/**
	 * Creates a SelectDBEReq which the DBEngine uses to create a Select statement to the DB. This
	 * constructor retrieves all columns with a where statement.
	 * 
	 * @param id The ID of this EngineRequest
	 * @param table The table name for this Select statement
	 * @param where HashMap containing the column name and the required column value for each. This is used
	 * 		in the where statement
	 */
	public SelectDBEReq(String id, String table, HashMap<String, Object> where) {
		super(id, QueryType.SELECT, table);
    	
    	String w = "";
    	String[] colParams = where.keySet().toArray(new String[0]);
    	String[] vals = where.values().toArray(new String[0]);
    	for(int i = 0; i < vals.length; i++) {
    		String val = "";
        	if(vals[i].getClass().equals(String.class)) { //if true, encloses the value in single quotes
        		val = "'" + vals[i] + "'";
        	} else {
        		val = vals[i].toString();
        	}
        	
    		w = w +  colParams[i] + "=" + val + " AND ";
    	}
		query = "select * from " + table + " where " + w;
	}
	
	/**
	 * Creates a SelectDBEReq which the DBEngine uses to create a Select statement to the DB. This
	 * constructor excludes the where statement
	 * 
	 * @param id The ID of this EngineRequest
	 * @param table The table name for this Select statement
	 * @param columns The column names to be retrieved from the specified table.
	 */
	public SelectDBEReq(String id, String table, String[] columns) {
		super(id, QueryType.SELECT, table);
		this.columns = columns;
		
		String cols = "";
    	for(int i = 0; i < columns.length; i++) {
    		cols = cols + columns[i] + ",";
    	}
    	cols = cols.substring(0, cols.length() - 1); //cuts off last comma
		query = "select " + cols + " from " + table;
	}
	
	/**
	 * Creates a SelectDBEReq which the DBEngine uses to create a Select statement to the DB. This
	 * constructor specifies ALL columns to be retrieved.
	 * 
	 * @param id The ID of this EngineRequest
	 * @param table The table name for this Select statement
	 */
	public SelectDBEReq(String id, String table) {
		super(id, QueryType.SELECT, table);
		this.columns = new String[]{"*"};
		
		query = "select * from " + table;
	}

	/**
	 * Returns all columns specified in this SelectDBEReq. In the query, these columns are the ones to be
	 * retrieved from the DB.
	 * @return the column names in String array
	 */
	public String[] getColumns() {
		return columns;
	}
}
