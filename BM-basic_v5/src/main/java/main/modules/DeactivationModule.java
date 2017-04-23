package main.modules;

import java.util.HashMap;

import components.Component;
import json.objects.ReqRequest;
import json.objects.ResError;
import main.ComponentRepository;
import main.engines.*;
import main.engines.requests.DBEngine.UpdateDBEReq;
import main.engines.requests.OHEngine.UpdateOHEReq;
import mqtt.*;
import tools.IDGenerator;

public class DeactivationModule extends AbstModule {
	private DBEngine dbe;
	private OHEngine ohe;
	private String comstable;
	
	public DeactivationModule(String RTY, MQTTHandler mh, ComponentRepository cr, DBEngine dbe, 
			OHEngine ohe, String comstable) {
		super("ByeModule", RTY, new String[0], mh, cr);
		this.dbe = dbe;
		this.ohe = ohe;
		this.comstable = comstable;
	}

	@Override
	protected void process(ReqRequest request) {
		IDGenerator idg = new IDGenerator();
		Component c = cr.getComponent(request.cid);
		
		mainLOG.info("Deactivating component " + c.getSSID() + " (MAC:" + c.getMAC() + ")");
		HashMap<String, Object> args = new HashMap<String, Object>(1,1);
		args.put("mac", request.cid);
		HashMap<String, Object> vals = new HashMap<String, Object>(1,1);
		vals.put("active", false);
		
		mainLOG.debug("Updating DB...");
		UpdateDBEReq updateActive = new UpdateDBEReq(idg.generateMixedCharID(10), comstable, 
				vals, args);
		Object o1 = forwardEngineRequest(dbe, updateActive);
		if(o1.getClass().equals(ResError.class)) {
			ResError error = (ResError) o1;
			error(error);
			return;
		}
		
		mainLOG.debug("Updating OH...");
		UpdateOHEReq updateOH = new UpdateOHEReq(idg.generateMixedCharID(10));
		Object o2 = forwardEngineRequest(ohe, updateOH);
		if(o2.getClass().equals(ResError.class)) {
			ResError error = (ResError) o2;
			error(error);
			return;
		}
		
		mainLOG.info("Component deactivated!");
	}

	@Override
	protected boolean additionalRequestChecking(ReqRequest request) {
		return true;
	}
}
