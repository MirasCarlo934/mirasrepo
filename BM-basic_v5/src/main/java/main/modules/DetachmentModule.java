package main.modules;

import java.sql.SQLException;
import java.util.HashMap;

import json.objects.ReqRequest;
import json.objects.ResError;
import main.ComponentRepository;
import main.engines.DBEngine;
import mqtt.MQTTHandler;

public class DetachmentModule extends AbstModule {
	private String propsTable;
	private String comsTable;
	private DBEngine dbe;

	public DetachmentModule(String RTY, MQTTHandler mh, ComponentRepository cr, DBEngine dbe) {
		super("DetachmentModule", RTY, new String[0], mh, cr);
		this.dbe = dbe;
	}

	@Override
	protected void process(ReqRequest request) {
		String cid = request.cid;
		LOG.info("Detaching component " + cid + " from system...");
		
		LOG.debug("Deleting component from CR...");
		cr.removeComponent(cid);
		
		LOG.debug("Deleting component from DB...");
		HashMap<String, Object> vals1 = new HashMap<String, Object>(1,1);
		vals1.put("com_id", cid);
		HashMap<String, Object> vals2 = new HashMap<String, Object>(1,1);
		vals2.put("ssid", cid);
		//LOG.info(vals1.size());
		try {
			dbe.deleteQuery(propsTable, vals1);
			dbe.deleteQuery(comsTable, vals2);
		} catch (SQLException e) {
			error(new ResError(request, "Cannot delete component from DB!"));
			e.printStackTrace();
		}
		LOG.info("Detachment complete!");
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
