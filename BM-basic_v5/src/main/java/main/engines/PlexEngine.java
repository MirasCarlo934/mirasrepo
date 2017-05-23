package main.engines;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import components.Component;
import json.RRP.ResError;
import json.plex.MediaPlayerProperties;
import json.plex.MediaPlayerProperties.MediaType;
import json.plex.MediaPlayerProperties.PlayerState;
import main.ComponentRepository;
import main.engines.requests.EngineRequest;
import main.engines.requests.PlexEngine.PlayPEReq;
import main.engines.requests.PlexEngine.PlexEngineRequest;
import main.engines.requests.PlexEngine.PlexRequestType;

public class PlexEngine extends AbstEngine {
	private ComponentRepository cr;
	private SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
	private SAXParser saxParser;
	private XMLParser xmlParser = new XMLParser();
	private URL sessionsURL;
	private String plexURL;
	private String URLPropSSID;
	private String playerProdSSID;
	private PlexEngineRequest per = null; //the PlexEngineRequest being processed, is replaced after every EngineRequest processing
	
	//the property SSIDs of the Plex Media Player component
	private String playerURLPropID;
	private String playerStatePropID;
	private String playPropID;
	private String pausePropID;
	private String stopPropID;
	private String progressPropID;
	
	public PlexEngine(String logDomain, String errorLogDomain, ComponentRepository cr, String plexURL, 
			String playerProdSSID, String URLPropSSID, 
			String playerURLPropID, String playerStatePropID, String playPropID, String pausePropID,
			String stopPropID, String progressPropID) {
		super(logDomain, errorLogDomain, PlexEngine.class.getName(), PlexEngine.class.toString());
		this.cr = cr;
		this.plexURL = plexURL;
		this.URLPropSSID = URLPropSSID;
		this.playerProdSSID = playerProdSSID;
		this.playerURLPropID = playerURLPropID;
		this.playerStatePropID = playerStatePropID;
		this.playPropID = playPropID;
		this.pausePropID = pausePropID;
		this.stopPropID = stopPropID;
		this.progressPropID = progressPropID;
		try {
			saxParser = saxParserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			LOG.fatal("ParserConfigurationException!", e);
			e.printStackTrace();
		} catch (SAXException e) {
			LOG.fatal("SAXException!", e);
			e.printStackTrace();
		}
		try {
			sessionsURL = new URL(plexURL + "/status/sessions/");
		} catch (MalformedURLException e) {
			LOG.fatal("Malformed URL!", e);
			e.printStackTrace();
		}
	}

	@Override
	protected Object processRequest(EngineRequest er) {
		per = (PlexEngineRequest) er;
		
		if(per.getType() == PlexRequestType.play) {
			PlayPEReq pper = (PlayPEReq) per;
			playMediaPlayer(pper.getClientIdentifier());
		}
		
		return per.getResponse();
	}
	
	/**
	 * Updates the properties of all Plex media players registered.
	 */
	private boolean updatePlayerProperties() {
		LOG.debug("Updating all media player properties...");
		LOG.trace("Fetching Plex sessions XML...");
		HttpURLConnection conn = null;
		MediaPlayerProperties[] playerProps = null;
		try {
			conn = (HttpURLConnection) sessionsURL.openConnection();
			conn.setRequestMethod("GET");
		} catch (IOException e) {
			LOG.error("Cannot open connection to Plex sessions!", e);
			e.printStackTrace();
			return false;
		}
		LOG.trace("Parsing Plex sessions XML...");
		try {
			saxParser.parse(conn.getInputStream(), xmlParser);
		} catch (SAXException e) {
			LOG.error("Error parsing XML!", e);
			e.printStackTrace();
		} catch (IOException e) {
			LOG.error("Error reading XML!", e);
			e.printStackTrace();
		}
		LOG.trace("Updating player properties within system...");
		playerProps = xmlParser.getRetrievedPlayerProperties();
		for(int i = 0; i < playerProps.length; i++) {
			MediaPlayerProperties props = playerProps[i];
			Component player = cr.getComponent(props.getPlayerClientIdentifier());
			//player.setPropertyValue(playerURLPropID, props.getPlayerURL());
			//player.setPropertyValue(prop_ssid, value);
		}
		
		return false;
	}
	
	private void playMediaPlayer(String clientIdentifier) {
		Component player = cr.getComponent(clientIdentifier);
		if(!updatePlayerProperties()) { //update failed
			per.setResponse(new ResError("PlexEngine", "Update media player properties failed!"));
			return;
		}
		/*try {
			URL url = new URL("");
		} catch (MalformedURLException e) {
			LOG.error("Malformed URL!", e);
			e.printStackTrace();
		}*/
	}
	
	/**
	 * Retrieves all the media players registered in this BM
	 * @return An array containing all media players retrieved
	 */
	private Component[] getAllMediaPlayers() {
		LOG.debug("Retrieving all registered media players...");
		Vector<Component> players = new Vector<Component>(1, 1);
		Component[] allComs = cr.getAllComponents();
		
		for(int i = 0; i < allComs.length; i++) {
			Component c = allComs[i];
			if(c.getProduct().getSSID().equals(playerProdSSID)) {
				players.add(c);
			}
		}
		
		LOG.debug("Retrieval complete! " + players.size() + " media players retrieved!");
		return players.toArray(new Component[players.size()]);
	}
	
	private class XMLParser extends DefaultHandler {
		private Vector<MediaPlayerProperties> playerProps = new Vector<MediaPlayerProperties>(1,1);
		private MediaPlayerProperties playerProp = new MediaPlayerProperties(); //changes after every Video element in XML
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) 
				throws SAXException {
			if(qName.equals("Video")) {
				playerProp = new MediaPlayerProperties();
				playerProp.setMediaType(MediaType.parseString(attributes.getValue("type")));
				if(playerProp.getMediaType().equals(MediaType.episode)) { //for TV shows
					playerProp.setMediaTitle(attributes.getValue("grandparentTitle") + " " + 
						attributes.getValue("parentTitle") + " - " + attributes.getValue("title"));
				}
			} else if(qName.equals("Player")) {//for movies
				playerProp.setPlayerClientIdentifier(attributes.getValue("machineIdentifier"));
				playerProp.setPlayerURL(attributes.getValue("address"));
				playerProp.setPlayerState(PlayerState.parseString(attributes.getValue("state")));
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(qName.equals("Video")) {
				LOG.trace("Adding data from XML for media player " + 
						cr.getComponent(playerProp.getPlayerClientIdentifier()).getSSID());
				playerProps.add(playerProp);
			}
		}
		
		/**
		 * Returns the media player propeties retrieved from the Plex sessions XML
		 * @return
		 */
		public MediaPlayerProperties[] getRetrievedPlayerProperties() {
			return playerProps.toArray(new MediaPlayerProperties[playerProps.size()]);
		}
	}
}
