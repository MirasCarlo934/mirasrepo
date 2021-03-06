package main.modules;

import java.sql.SQLException;
import java.util.HashMap;

import json.RRP.ReqRequest;
import json.RRP.ResError;
import main.ComponentRepository;
import main.engines.DBEngine;
import main.engines.OHEngine;
import main.engines.requests.DBEngine.DeleteDBEReq;
import main.engines.requests.OHEngine.RemoveItemOHEReq;
import main.engines.requests.OHEngine.UpdateOHEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class DetachmentModule extends AbstModule {
	private String propsTable;
	private String comsTable;
	private DBEngine dbe;
	private OHEngine ohe;
	private IDGenerator idg = new IDGenerator();

	public DetachmentModule(String logDomain, String errorLogDomain, String RTY, MQTTHandler mh, 
			ComponentRepository cr, DBEngine dbe, OHEngine ohe) {
		super(logDomain, errorLogDomain, "DetachmentModule", RTY, new String[0], mh, cr);
		this.dbe = dbe;
		this.ohe = ohe;
	}

	@Override
	protected void process(ReqRequest request) {
		String cid = request.cid;
		mainLOG.info("Detaching component " + cid + " from system...");
		
		mainLOG.debug("Deleting component from DB...");
		HashMap<String, Object> vals1 = new HashMap<String, Object>(1,1);
		vals1.put("com_id", cid);
		HashMap<String, Object> vals2 = new HashMap<String, Object>(1,1);
		vals2.put("ssid", cid);
		
		//delete component properties from DB
		forwardEngineRequest(dbe, new DeleteDBEReq(idg.generateMixedCharID(10), propsTable, vals1));

		//delete component from DB
		forwardEngineRequest(dbe, new DeleteDBEReq(idg.generateMixedCharID(10), comsTable, vals2));

		//update OH of changes
		//forwardEngineRequest(ohe, new UpdateOHEReq(idg.generateMixedCharID(10)));
		mainLOG.debug("Deleting component from OH DB...");
		//forwardEngineRequest(ohe, new RemoveItemOHEReq(idg.generateMixedCharID(10), cid));
		
		mainLOG.debug("Deleting component from CR...");
		cr.removeComponent(cid);
		
		mainLOG.debug("Deleting component from OH config files...");
		forwardEngineRequest(ohe, new UpdateOHEReq(idg.generateMixedCharID(10)));
		
		mainLOG.info("Detachment complete!");
	}

	@Override
	protected boolean additionalRequestChecking(ReqRequest request) {
		// TODO Auto-generated method stub
		return true;
	}

	public void setPropsTable(String propsTable) {
		this.propsTable = propsTable;
	}

	public void setComsTable(String comsTable) {
		this.comsTable = comsTable;
	}

}
