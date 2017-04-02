package main.engines.requests.CIREngine;

import json.objects.ReqPOOP;

public class GetSpecificExecBlocksCIREReq extends CIREngineRequest {
	private ReqPOOP poop;

	/**
	 * Retrieves all CIR statements related with the specified component and property.
	 * 
	 * @param id
	 * @param component The specified Component
	 * @param property The specified Property
	 */
	public GetSpecificExecBlocksCIREReq(String id, ReqPOOP poop) {
		super(id, CIRRequestType.getSpecificExecBlocks);
		this.poop = poop;
	}

	public ReqPOOP getPoop() {
		return poop;
	}

	public void setPoop(ReqPOOP poop) {
		this.poop = poop;
	}
}
