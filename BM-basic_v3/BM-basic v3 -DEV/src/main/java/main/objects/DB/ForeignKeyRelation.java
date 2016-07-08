package main.objects.DB;

public class ForeignKeyRelation {
	private String tableName;
	private String columnName;

	/**
	 * For constructing a Foreign Key Relation needed in one of the constructors of a ColumnInfo. Only contains table name and column name
	 * of the foreign key
	 * @param table
	 * @param name
	 */
	public ForeignKeyRelation(String table, String name) {
		setTableName(table);
		setColumnName(name);
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
	 * @return the columnName
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * @param columnName the columnName to set
	 */
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
}
