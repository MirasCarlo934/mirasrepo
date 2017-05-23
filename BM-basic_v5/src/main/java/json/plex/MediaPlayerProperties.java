package json.plex;

import org.json.JSONObject;

/**
 * The properies of the Plex media player and the media it is currently playing/displaying. 
 * 
 * @author Carlo
 */
public class MediaPlayerProperties {
	private JSONObject mediaProperties = new JSONObject();
	private JSONObject playerProperties = new JSONObject();

	public MediaPlayerProperties(String playerURL, String clientIdentifier, String playerState, 
			MediaType mediaType, String mediaTitle) {
		playerProperties.put("URL", playerURL);
		playerProperties.put("clientIdentifier", clientIdentifier);
		playerProperties.put("state", playerState);
		mediaProperties.put("type", mediaType);
		mediaProperties.put("title", mediaTitle);
	}
	
	/**
	 * Creates a blank MediaPlayerProperties, to be filled by the PlexEngine. <i>This is the default
	 * constructor used</i>
	 */
	public MediaPlayerProperties() {
		
	}
	
	public String getPlayerURL() {
		return playerProperties.getString("URL");
	}
	
	public void setPlayerURL(String playerURL) {
		playerProperties.put("URL", playerURL);
	}
	
	public String getPlayerClientIdentifier() {
		return playerProperties.getString("clientIdentifier");
	}
	
	public void setPlayerClientIdentifier(String clientIdentifier) {
		playerProperties.put("clientIdentifier", clientIdentifier);
	}
	
	public PlayerState getPlayerState() {
		return PlayerState.parseString(playerProperties.getString("state"));
	}
	
	public void setPlayerState(PlayerState playerState) {
		playerProperties.put("state", playerState);
	}
	
	public MediaType getMediaType() {
		return MediaType.parseString(mediaProperties.getString("type"));
	}
	
	public void setMediaType(MediaType mediaType) {
		mediaProperties.put("type", mediaType.toString());
	}
	
	public String getMediaTitle() {
		return mediaProperties.getString("title");
	}
	
	public void setMediaTitle(String mediaTitle) {
		mediaProperties.put("title", mediaTitle);
	}
	
	public enum MediaType {
		episode, movie;
		
		/**
		 * Parses the specified String to determine what MediaType it is.
		 * 
		 * @param s The String
		 * @return The MediaType of the specified String, <i>null</i> if String does not match a 
		 * 		MediaType
		 */
		public static MediaType parseString(String s) {
			if(s.equals(episode.toString())) {
				return episode;
			}
			else if(s.equals(movie.toString())) {
				return movie;
			}
			else {
				return null;
			}
		}
	}
	
	public enum PlayerState {
		playing, paused, stopped, buffering;
		
		/**
		 * Parses the specified String to determine what PlayerState it is.
		 * 
		 * @param s The String
		 * @return The MediaType of the specified String, <i>null</i> if String does not match a 
		 * 		MediaType
		 */
		public static PlayerState parseString(String s) {
			if(s.equals(playing.toString())) {
				return playing;
			}
			else if(s.equals(paused.toString())) {
				return paused;
			}
			else if(s.equals(stopped.toString())) {
				return stopped;
			}
			else if(s.equals(buffering.toString())) {
				return buffering;
			}
			else {
				return null;
			}
		}
	}
}
