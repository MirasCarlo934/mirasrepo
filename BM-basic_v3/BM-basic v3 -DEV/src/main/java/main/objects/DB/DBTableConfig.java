/**
 * DBTableConfig documentation
 * 		Contains all the necessary information of an SQL table in the DB.
 */
package main.objects.DB;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import main.TransTechSystem;
import main.objects.SSIDType;
import tools.TrafficController;

public class DBTableConfig {
	private Logger logger;
	private String objectName; //the object type that is contained in the DB table (eg. User, Reservation)
	private String tableName;
	private SSIDType ssidType; //MIXEDCHAR or INT ssid type
	private ColumnInfo[] columns;

	public DBTableConfig(String objectName, String tableName, SSIDType ssidType) {
		setObjectName(objectName.toUpperCase());
		setTableName(tableName.toUpperCase());
		setSSIDType(ssidType);
		logger = Logger.getLogger(this.tableName + "TableConfig");
	}
	
	/**
	 * Retrieves relevant table info from DB, specifically column information. Column information is used in 
	 * the processing of TransactionRequests in the TransactionEngine's CRUDS functionality.<br><br>
	 * 
	 * <i>This method is invoked in the startup stage.</i>
	 */
	public void populateConfig(TrafficController tc) {
		try {	
			DatabaseMetaData DBdata = tc.getConn().getMetaData();
			Vector<String> uniqCols = new Vector<String>(1,1); //contains column names of unique keys
			Vector<String> expCols = new Vector<String>(1,1); 
			HashMap<String, ForeignKeyRelation> impCols = new HashMap<String, ForeignKeyRelation>(1); //contains names of imported columns in table and their foreign key relation
			
			//retrieves unique columns from table
			ResultSet rs1 = DBdata.getIndexInfo(null, null, tableName, true, true);
			while(rs1.next()) {
				uniqCols.add(rs1.getString("COLUMN_NAME"));
			}
			
			//retrieves imported columns from table
			ResultSet rs2 = DBdata.getImportedKeys(null, null, tableName);
			while(rs2.next()) {
				ForeignKeyRelation fk = new ForeignKeyRelation(rs2.getString("PKTABLE_NAME"), rs2.getString("PKCOLUMN_NAME"));
				impCols.put(rs2.getString("FKCOLUMN_NAME"), fk);
			}
			
			//retrieves exported columns from table
			ResultSet rs3 = DBdata.getExportedKeys(null, null, tableName);
			while(rs3.next()) {
				expCols.add(rs3.getString("PKCOLUMN_NAME"));
			}
			
			//retrieves column names from table
			ResultSet rs = tc.selectQuery("*", tableName);
			ResultSetMetaData cols = rs.getMetaData();
			columns = new ColumnInfo[cols.getColumnCount()];
			for(int i = 0; i < cols.getColumnCount(); i++) {
				ColumnInfo col = null;
				String colname = cols.getColumnName(i + 1);
				String dataType = cols.getColumnTypeName(i + 1);
				boolean unique = false;
				boolean exported = false;
				ForeignKeyRelation fkr = null;
				
				//checks if column is unique
				if(uniqCols.contains(colname)) {
					unique = true;
				}
				
				//checks if column is imported
				if(impCols.containsKey(colname)) {
					fkr = impCols.get(colname);
				}
				
				//checks if column is exported
				if(expCols.contains(colname)) {
					exported = true;
				}
				
				col = new ColumnInfo(tableName, colname, dataType, unique, exported, fkr);
				
				/* uncomment for debugging purposes
				logger.debug(colname + ":::");
				if(col.getForeignKey() != (null)) {
					logger.debug(col.getForeignKey().getTableName());
					logger.debug(col.getForeignKey().getColumnName());
				}*/
				columns[i] = col;
			}
		} catch (Exception e) {
			logger.fatal("Cannot populate DB table configuration!", e);
		}		
	}

	/**
	 * @return the name
	 */
	public String getObjectName() {
		return objectName;
	}

	/**
	 * @param name the name to set
	 */
	public void setObjectName(String name) {
		this.objectName = name;
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @param tableName the tableName to set
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * @return the sSIDType
	 */
	public SSIDType getSSIDType() {
		return ssidType;
	}

	/**
	 * @param sSIDType the sSIDType to set
	 */
	public void setSSIDType(SSIDType sSIDType) {
		ssidType = sSIDType;
	}

	/**
	 * Retrieves the column names of the SQL table. /n/n
	 * 
	 * Note: Populate DBTableConfig first to get the column names from the DB
	 * @return the columns
	 */
	public ColumnInfo[] getColumns() {
		return columns;
	}

	/**
	 * @param columns the columns to set
	 */
	public void setColumns(ColumnInfo[] columns) {
		this.columns = columns;
	}
}
