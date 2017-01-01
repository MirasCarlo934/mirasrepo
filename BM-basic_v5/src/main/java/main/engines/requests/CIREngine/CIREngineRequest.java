package main.engines.requests.CIREngine;

import main.engines.CIREngine;
import main.engines.requests.EngineRequest;

public abstract class CIREngineRequest extends EngineRequest {
	private CIRRequestType type;

	public CIREngineRequest(String id, CIRRequestType type) {
		super(id, CIREngine.class.toString());
		this.setType(type);
	}

	public CIRRequestType getType() {
		return type;
	}

	private void setType(CIRRequestType type) {
		this.type = type;
	}
}
