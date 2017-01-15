package main.modules;

import json.objects.ReqOHCommand;
import json.objects.ReqRequest;
import json.objects.ResBasic;
import json.objects.ResError;
import main.ComponentRepository;
import main.engines.OHEngine;
import main.engines.requests.OHEngine.StartOHEReq;
import main.engines.requests.OHEngine.StopOHEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class OHControlModule extends AbstModule {
	private OHEngine ohe;
	private String commandParam;
	private String start;
	private String stop;
	private String[] commands;
	private IDGenerator idg = new IDGenerator();

	public OHControlModule(String RTY, String commandParam, MQTTHandler mh, ComponentRepository cr, 
			OHEngine ohe, String start, String stop) {
		super("OHControlModule", RTY, new String[]{commandParam}, mh, cr);
		this.ohe = ohe;
		this.start = start;
		this.stop = stop;
		this.commandParam = commandParam;
		commands = new String[]{start, stop};
	}

	@Override
	protected void process(ReqRequest request) {
		LOG.info("Processing command for OpenHAB...");
		ReqOHCommand rohc = new ReqOHCommand(request.getJSON(), commandParam);
		String com = rohc.command;
		Object o = null;
		
		if(com.equalsIgnoreCase(start)) {
			o = ohe.forwardRequest(new StartOHEReq(idg.generateMixedCharID(10)));
		} else if(com.equalsIgnoreCase(stop)) {
			o = ohe.forwardRequest(new StopOHEReq(idg.generateMixedCharID(10)));
		}
		
		if(!o.getClass().equals(ResError.class)) {
			LOG.debug("OpenHAB command processing complete!");
			mh.publish(new ResBasic(rohc, true));
		} else {
			error((ResError) o);
		}
	}

	@Override
	protected boolean additionalRequestChecking(ReqRequest request) {
		boolean b = false;
		ReqOHCommand rohc = new ReqOHCommand(request.getJSON(), commandParam);
		for(int i = 0; i < commands.length; i++) {
			if(commands[i].equalsIgnoreCase(rohc.command)) {
				b = true;
				break;
			}
		}
		return b;
	}

}
