package main.engines.requests.CIREngine;

public class UpdateCIREReq extends CIREngineRequest {

	public UpdateCIREReq(String id) {
		super(id, CIRRequestType.update);
	}
}
