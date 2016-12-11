package main.modules;

import json.objects.ReqRequest;
import main.ComponentRepository;
import main.engines.DBEngine;
import mqtt.MQTTHandler;

public class DetachmentModule extends AbstModule {
	private String propsTable;
	private String comsTable;
	private DBEngine dbe;

	public DetachmentModule(String RTY, MQTTHandler mh, ComponentRepository cr, DBEngine dbe) {
		super("DetachmentModule", RTY, new String[0], mh, cr);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void process(ReqRequest request) {
		String cid = request.cid;
		LOG.info("Deleting component " + cid + " from system...");
		
		LOG.debug("Deleting component from CR...");
		cr.removeComponent(cid);
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
