package main.engines.requests.OHEngine;

public class StartOHEReq extends OHEngineRequest {

	public StartOHEReq(String id) {
		super(id, OHRequestType.start);
	}
}
