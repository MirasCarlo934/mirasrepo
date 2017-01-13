package main.engines.requests.OHEngine;

public class UpdateOHEReq extends OHEngineRequest {

	public UpdateOHEReq(String id) {
		super(id, OHRequestType.update);
	}
}
