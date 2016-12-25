package main.engines.requests.DBEngine;

public class RawDBEReq extends DBEngineRequest {
	private String query;

	public RawDBEReq(String id, String query) {
		super(id, QueryType.RAW);
		this.setQuery(query);
	}

	/**
	 * 
	 * @return
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * 
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}
}
