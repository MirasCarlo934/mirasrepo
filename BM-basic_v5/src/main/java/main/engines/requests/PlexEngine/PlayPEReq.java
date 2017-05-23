package main.engines.requests.PlexEngine;

public class PlayPEReq extends PlexEngineRequest {
	private String clientIdentifier;

	/**
	 * The PlexEngineRequest that requests the PlexEngine to send a play command to a Plex Media Player.
	 * @param id The 10-character alphanumerical ID of this EngineRequest
	 * @param clientIdentifier The clientIdentifier of the Plex Media Player
	 */
	public PlayPEReq(String id, String clientIdentifier) {
		super(id, PlexRequestType.play);
		this.clientIdentifier = clientIdentifier;
	}

	public String getClientIdentifier() {
		return clientIdentifier;
	}
}
