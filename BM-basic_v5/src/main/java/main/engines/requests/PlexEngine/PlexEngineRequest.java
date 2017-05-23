package main.engines.requests.PlexEngine;

import main.engines.PlexEngine;
import main.engines.requests.EngineRequest;

public class PlexEngineRequest extends EngineRequest {
	private PlexRequestType type;

	public PlexEngineRequest(String id, PlexRequestType type) {
		super(id, PlexEngine.class.toString());
		this.setType(type);
	}

	public PlexRequestType getType() {
		return type;
	}

	private void setType(PlexRequestType type) {
		this.type = type;
	}
}
