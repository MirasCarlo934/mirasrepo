package main.modules;

import components.Component;
import json.RRP.ReqPlex;
import json.RRP.ReqRequest;
import main.ComponentRepository;
import main.engines.DBEngine;
import main.engines.OHEngine;
import main.engines.PlexEngine;
import main.engines.requests.PlexEngine.PlayPEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class PlexModule extends AbstModule {
	private String commandParam;
	private String playerProdSSID; //the SSID of the Plex Media Player product in the DB
	private DBEngine dbe;
	private OHEngine ohe;
	private PlexEngine pe;
	private IDGenerator idg = new IDGenerator();

	public PlexModule(String logDomain, String errorLogDomain, String RTY, String commandParam, MQTTHandler mh,
			ComponentRepository cr, DBEngine dbe, OHEngine ohe, PlexEngine pe, String playerProdSSID) {
		super(logDomain, errorLogDomain, "PlexModule", RTY, new String[]{commandParam}, mh, cr);
		this.commandParam = commandParam;
		this.dbe = dbe;
		this.ohe = ohe;
		this.pe = pe;
	}

	@Override
	protected void process(ReqRequest request) {
		ReqPlex req = new ReqPlex(request.getJSON(), commandParam);
		if(req.command.equals("play")) {
			Component player = cr.getComponent(req.cid);
			forwardEngineRequest(pe, new PlayPEReq(idg.generateMixedCharID(10), player.getMAC()));
		}
	}

	/**
	 * <ul>
	 * <li>Checks if request CID is a valid CID of a Plex Media Player</li>
	 * </ul>
	 */
	@Override
	protected boolean additionalRequestChecking(ReqRequest request) {
		// TODO Auto-generated method stub
		return true;
	}
}
