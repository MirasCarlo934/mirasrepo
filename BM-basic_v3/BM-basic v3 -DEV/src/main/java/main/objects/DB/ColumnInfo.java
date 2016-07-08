package main.objects.DB;

public class ColumnInfo {
	private String table;
	private String name;
	private String dataType;
	private boolean unique;
	private boolean exported;
	private ForeignKeyRelation foreignKey; //null if Column is not imported
	//private boolean imported;

	/*public ColumnInfo(String name, String dataType, boolean unique, boolean imported) {
		setName(name);
		setDataType(dataType);
		setUnique(unique);
		setImported(imported);
	}*/
	
	/**
	 * For constructing a ColumnInfo with NO foreign key relation.
	 * @param table
	 * @param name
	 * @param dataType
	 * @param unique
	 * @param imported
	 * @param exported
	 */
	public ColumnInfo(String table, String name, String dataType, boolean unique, boolean exported) {
		setTable(table);
		setColumnName(name);
		setDataType(dataType);
		setUnique(unique);
		setForeignKey(null);
		setExported(exported);
	}
	
	/**
	 * For constructing a ColumnInfo with a foreign key relation
	 * @param table
	 * @param name
	 * @param dataType
	 * @param unique
	 * @param imported
	 */
	public ColumnInfo(String table, String name, String dataType, boolean unique, boolean exported, ForeignKeyRelation imported) {
		setTable(table);
		setColumnName(name);
		setDataType(dataType);
		setUnique(unique);
		setForeignKey(imported);
		setExported(exported);
	}

	/**
	 * @return the name
	 */
	public String getColumnName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setColumnName(String name) {
		this.name = name;
	}

	/**
	 * @return the dataType
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * @param dataType the dataType to set
	 */
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	/**
	 * @return the unique
	 */
	public boolean isUnique() {
		return unique;
	}

	/**
	 * @param unique the unique to set
	 */
	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	/**
	 * @return the table
	 */
	public String getTable() {
		return table;
	}

	/**
	 * @param table the table to set
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * @return the foreignKey
	 */
	public ForeignKeyRelation getForeignKey() {
		return foreignKey;
	}

	/**
	 * @param foreignKey the foreignKey to set
	 */
	public void setForeignKey(ForeignKeyRelation foreignKey) {
		this.foreignKey = foreignKey;
	}

	/**
	 * @return the exported
	 */
	public boolean isExported() {
		return exported;
	}

	/**
	 * @param exported the exported to set
	 */
	public void setExported(boolean exported) {
		this.exported = exported;
	}
}
