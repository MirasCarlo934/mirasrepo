package main.engines.requests.OHEngine;

public class StopOHEReq extends OHEngineRequest {

	public StopOHEReq(String id) {
		super(id, OHRequestType.stop);
	}
}
