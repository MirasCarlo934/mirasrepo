package main.engines.requests.OHEngine;

import main.engines.OHEngine;
import main.engines.requests.EngineRequest;

public abstract class OHEngineRequest extends EngineRequest {
	private OHRequestType type;

	public OHEngineRequest(String id, OHRequestType type) {
		super(id, OHEngine.class.toString());
		this.setType(type);
	}

	public OHRequestType getType() {
		return type;
	}

	private void setType(OHRequestType type) {
		this.type = type;
	}
}
