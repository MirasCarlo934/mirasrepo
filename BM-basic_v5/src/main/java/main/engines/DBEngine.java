package main.engines;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;

import main.engines.requests.EngineRequest;
import main.engines.requests.DBEngine.DBEngineRequest;
import main.engines.requests.DBEngine.QueryType;
import main.engines.requests.DBEngine.RawDBEReq;
import tools.SystemTimer;

public class DBEngine extends Engine {
	//private static final Logger LOG = Logger.getLogger("DB_LOG.TrafficController");
	private String dbURL;
    private Connection conn;
    private String dbusr;
    private String dbpwd;
    
    /**
     * Primitive instantiation of DBEngine. Automatically connects itself to DB.
     * @param name
     * @param dbURL
     * @param dbusr
     * @param dbpwd
     */
    public DBEngine(String dbURL, String dbusr, String dbpwd) {
    	//super(systimer, "DBEngine", DBEngine.class.toString());
    	super("DBEngine", DBEngine.class.toString());
    	LOG.info("DBEngine construct start! url=" + dbURL);
    	this.dbURL = dbURL;
    	this.dbusr = dbusr;
    	this.dbpwd = dbpwd;
    	try {
			createConnection(dbURL, dbusr, dbpwd);
		} catch (SQLException e) {
			LOG.error("Cannot connect to DB!", e);
		}
    }
    
    public void createConnection(String dbURL, String dbusr, String dbpwd) throws SQLException {
    	this.dbURL = dbURL;
    	this.dbusr = dbusr;
    	this.dbpwd = dbpwd;
    	setConn(DriverManager.getConnection(dbURL, dbusr, dbpwd));
        LOG.info("Connected to Derby DB!");
    }
    
    public void closeConnection() throws SQLException {
    	getConn().close();
    	LOG.info("Disconnected from Deby DB!");
    }
    

	@Override
	protected Object processRequest(EngineRequest er) {
		DBEngineRequest dber = (DBEngineRequest) er;
		try {
			return executeQuery(dber.getQuery());
			/*if(dber.getQueryType() == QueryType.RAW) {
				RawDBEReq r  = (RawDBEReq) dber;
				return executeQuery(r.getQuery());
			}*/
		} catch (SQLException e) {
			LOG.error("SQLException!", e);
			e.printStackTrace();
		}
		return null;
	}
    
    public ResultSet executeQuery(String query) throws SQLException {
    	Statement stmt = null;
    	try{
    		stmt = getConn().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        	//logger.debug(conn.toString());
        	LOG.trace("Executing " + query + " ...");
        	stmt.execute(query);
        	LOG.trace("Query executed successfully!");
        	return stmt.getResultSet();
    	} catch(NullPointerException e) {
    		LOG.fatal("Connection not yet established!");
    		return null;
    	}
    }
    
    /*public ResultSet selectQuery(String cols, String table) throws SQLException {
    	String q = "select " + cols + " from " + table;
    	return executeQuery(q);
    }
    
    public ResultSet selectQuery(String cols, String table, String where) throws SQLException {
    	String q = "select " + cols + " from " + table + " where " + where;
    	return executeQuery(q);
    }*/
    
    /**
     * Ideal method for handling select query. Method uses HashMap args to construct the WHERE clause of the query.
     * <br><br>
     * <i><b>NOTE:</b> The WHERE clause is constructed by separating the arguments with AND.</i>
     * 
     * @param columns
     * @param table
     * @param args The arguments used to construct the 'where' clause. Supplied from the getArgs() method of the TransactionRequest object
     * @return
     * @throws SQLException
     */
    /*public ResultSet selectQuery(String columns, String table, HashMap<String, Object> args) throws SQLException {
    	String q = "select " + columns + " from " + table;
    	
    	//constructs where clause
		String where = " WHERE "; // where clause that ignores case
		Object[] cols = args.keySet().toArray();
		for(int i = 0; i < cols.length; i++) {
			String col = (String) cols[i];
			Object value = args.get(col);
			
			if(value.getClass().equals(String.class)) { //if value is a string
				where = where + "UPPER(" + col + ") LIKE UPPER('" + value + "')";
			} else {
				where = where + col + " = " + value + "";
			}
			where += " AND ";
		}
		where = where.substring(0, where.length() - 4); //cuts last AND in where String
		q += where;
    	return executeQuery(q);
    }*/
    
    /**
     * Select query for 'where' clause with single comparison only
     * @param columns
     * @param table
     * @param colParam
     * @param val
     * @return
     * @throws SQLException
     */
    public ResultSet selectQuery(String[] columns, String table, String colParam, Object val) throws SQLException {
    	String cols = "";
    	for(int i = 0; i < columns.length; i++) {
    		cols = cols + columns[i] + ",";
    	}
    	cols = cols.substring(0, cols.length() - 1); //cuts off last comma
    	
    	String v = "";
    	if(val.getClass().equals(String.class)) { //if true, encloses the value in single quotes
    		v = "'" + val + "'";
    	} else {
    		v = val.toString();
    	}
    	String q = "select " + cols + " from " + table + " where " + colParam + " = " + v;
    	return executeQuery(q);
    }
    
    /**
     * Select query for 'where' clause with multiple comparisons. Assumes that the lengths of colParams and vals are equal. To reduce
     * system error likelihood, check if both array lengths are equal first before calling this method.
     * 
     * @param columns
     * @param table
     * @param colParams
     * @param vals
     * @return
     * @throws SQLException
     */
    public ResultSet selectQuery(String[] columns, String table, String[] colParams, Object[] vals) throws SQLException {
    	//translates columns to String
    	String cols = "";
    	for(int i = 0; i < columns.length; i++) {
    		cols = cols + columns[i] + ",";
    	}
    	cols = cols.substring(0, cols.length() - 1); //cuts off last comma
    	
    	//translates colParams and vals to SQL WHERE clause
    	String where = "";
    	for(int i = 0; i < vals.length; i++) {
    		String val = "";
        	if(vals[i].getClass().equals(String.class)) { //if true, encloses the value in single quotes
        		val = "'" + vals[i] + "'";
        	} else {
        		val = vals[i].toString();
        	}
        	
    		where = where +  colParams[i] + "=" + val + " AND ";
    	}
    	where = where.substring(0, where.length() - 5); //cuts off last ' AND '
    	String q = "select " + cols + " from " + table + " where " + where;
    	return executeQuery(q);
    }
    
    /*public void insertQuery(String values, String table) throws SQLException {
    	String q = "insert into " + table + " values(" + values + ")";
    	executeQuery(q);
    }
    
    public void insertQuery(Object[] values, String table) throws SQLException {
    	String vals = "";
    	for(int i = 0; i < values.length; i++) {
    		Object val = values[i];
    		if(val.getClass().equals(String.class)) { //if true, encloses the value in single quotes
    			vals = vals + "'" + val + "',";
    		} else {
    			vals = vals + val + ",";
    		}
    	}
    	vals = vals.substring(0, vals.length() - 1); //cuts off last comma
    	String q = "insert into " + table + " values(" + vals + ")";
    	executeQuery(q);
    }*/
    
    /**
     * @param cols Array of the column names
     * @param values Array of the values (must be in the same order as @param cols)
     * @param table SQL table name
     * @throws SQLException
     */
    /*public void insertQuery(String[] cols, Object[] values, String table) throws SQLException {
    	String scols = "";
    	String vals = "";
    	for(int i = 0; i < cols.length; i++) {
    		scols = scols + cols[i] + ",";
    	}
    	for(int i = 0; i < values.length; i++) {
    		Object val = values[i];
    		if(val.getClass().equals(String.class)) { //if true, encloses the value in single quotes
    			vals = vals + "'" + val + "',";
    		} else {
    			vals = vals + val + ",";
    		}
    	}
    	scols = scols.substring(0, scols.length() - 1); //cuts off last comma
    	vals = vals.substring(0, vals.length() - 1); //cuts off last comma
    	String q = "insert into " + table + "(" + scols + ") values(" + vals + ")";
    	executeQuery(q);
    }
    
    public void insertQuery(String table, HashMap<String, Object> values) throws SQLException {
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
    	
    	String q = "insert into " + table + "(" + scols + ") values(" + vals + ")";
    	executeQuery(q);
    }*/
    
    /*public ResultSet deleteQuery(String table, HashMap<String, Object> args) throws SQLException {
    	String q = "DELETE FROM " + table;
    	
    	//constructs where clause
    	if(!args.isEmpty()) { //to prevent deletion of entire table contents
	    	String where = " WHERE "; // where clause that ignores case
			Object[] argscols = args.keySet().toArray();
			for(int i = 0; i < argscols.length; i++) {
				String col = (String) argscols[i];
				Object value = args.get(col);
				
				if(value.getClass().equals(String.class)) { //if value is a string
					//where = where + "UPPER(" + col + ") LIKE UPPER('" + value + "')";
					where = where + col + " = '" + value + "'";
				} else {
					where = where + col + " = " + value + "";
				}
				where += " AND ";
			}
			where = where.substring(0, where.length() - 4);
			return executeQuery(q + where);
    	} else {
    		LOG.warn("Empty 'WHERE' statement in DELETE query not allowed in DBEngine!");
    		throw new SQLException("Empty 'WHERE' statement in DELETE query not allowed in DBEngine!");
    	}
    }*/
    
    /**
     * Ideal method for handling update query. Method uses HashMap vals to construct the 'set' clause and the HashMap args to construct 
     * the 'where' clause of the query.
     * 
     * @param table
     * @param args The arguments used to construct the 'where' clause. Supplied from the getArgs() method of the TransactionRequest object
     * @return 
     * @throws SQLException 
     */
    /*public ResultSet updateQuery(String table, HashMap<String, Object> args, HashMap<String, Object> vals) throws SQLException {
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
    	return executeQuery(q);
    }*/
    
    /*public ResultSet updateQuery(String table, String args, HashMap<String, Object> vals) throws SQLException {
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
		String where = " WHERE " + args; // where clause that ignores case
		
		q += set + where;
    	return executeQuery(q);
    }*/
    
    public int getLastId (String idname, String table) throws SQLException {
    	ResultSet rs = executeQuery("select max(" + idname + ") from " + table);
    	int id = -1;
    	if(rs.next()) {
    		id = rs.getInt(1);
    	}
    	return id;
    }

	public String getDbURL() {
		return dbURL;
	}

	/**
	 * @return the conn
	 */
	public Connection getConn() {
		return conn;
	}

	/**
	 * @param conn the conn to set
	 */
	public void setConn(Connection conn) {
		this.conn = conn;
	}
}
