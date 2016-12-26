package main.engines.requests.DBEngine;

import java.util.HashMap;

public class UpdateDBEReq extends DBEngineRequest {

	/**
	 * Creates an UpdateDBEReq which the DBEngine uses to create an Update statement to the DB.
	 * 
	 * @param id The ID of this EngineRequest
	 * @param table The table name for this Select statement
	 * @param vals HashMap containing names of the columns to be updated along with the new values 
	 * 		for each.
	 * @param args HashMap containing the column name and the required column value for each. This is used
	 * 		in the where statement.
	 */
	public UpdateDBEReq(String id, String table, HashMap<String, Object> vals, 
			HashMap<String, Object> args) {
		super(id, QueryType.UPDATE, table);
		String q = "update " + table;
    	
    	//constructs set clause
		String set = " SET "; // where clause that ignores case
		Object[] valscols = vals.keySet().toArray();
		for(int i = 0; i < valscols.length; i++) {
			String col = (String) valscols[i];
			Object value = vals.get(col);
			
			if(value.getClass().equals(String.class)) { //if value is a string
				set = set + col + "='" + value + "'";
			} else {
				set = set + col + " = " + value + "";
			}
			set += ", ";
		}
		set = set.substring(0, set.length() - 2); //cuts last comma and space in set String
		
		//constructs where clause
		String where = " WHERE "; // where clause that ignores case
		Object[] argscols = args.keySet().toArray();
		for(int i = 0; i < argscols.length; i++) {
			String col = (String) argscols[i];
			Object value = args.get(col);
			
			if(value.getClass().equals(String.class)) { //if value is a string
				where = where + "UPPER(" + col + ") LIKE UPPER('" + value + "')";
			} else {
				where = where + col + " = " + value + "";
			}
			where += " AND ";
		}
		where = where.substring(0, where.length() - 4); //cuts last AND in where String
		
		q += set + where;
		query = q;
	}

}
