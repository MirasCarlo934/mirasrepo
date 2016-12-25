package main.engines.requests.DBEngine;

import main.engines.DBEngine;
import main.engines.Engine;
import main.engines.requests.EngineRequest;

public abstract class DBEngineRequest extends EngineRequest {
	private QueryType type;

	public DBEngineRequest(String id, QueryType type) {
		super(id, DBEngine.class.toString());
		this.type = (type);
	}

	/**
	 * Returns the QueryType of this DBEngineRequest. Used to determine the kind of query to be used by 
	 * the DBEngine (select, delete, update, insert, raw query string)
	 * @return the QueryType enum
	 */
	public QueryType getQueryType() {
		return type;
	}
}
