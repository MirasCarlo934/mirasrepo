package bm.modules;

import java.util.HashMap;

import org.apache.log4j.Logger;

import bm.ComponentRepository;
import bm.OpenhabHandlingEngine;
import bm.TransactionEngine;
import bm.modules.parents.Module;
import main.TransTechSystem;
import main.components.Component;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import main.objects.request_response.TransactionRequest;
import mqtt.MQTTHandler;

public class DetachmentModule extends Module {
	private static final Logger logger = Logger.getLogger(DetachmentModule.class);
	public static final String request_type = "detach_com";
	
	private TransactionEngine te;
	private ComponentRepository cr;
	private MQTTHandler mh;
	private OpenhabHandlingEngine ohe;
	
	public DetachmentModule(MQTTHandler mqttHandler, TransactionEngine transactionEngine, 
			ComponentRepository componentRepository, OpenhabHandlingEngine openhabHandlingEngine) {
		super("DetachmentModule", request_type, null, mqttHandler);
		te = transactionEngine;
		cr = componentRepository;
		mh = getMqttHandler();
		ohe = openhabHandlingEngine;
	}

	@Override
	protected void process(Request request) throws Exception {
		String cid = request.getComponentID();
		Component com = cr.getComponent(cid);
		
		logger.info("Detaching component '" + cid + "' from system...");
		if(cr.detachComponent(cid)) {
			//updates OpenHab
			ohe.updateOpenhabRecords(cr.getAllRooms(), cr.getAllComponents());
			
			Response r1 = new Response(com.getMacAddress(),  request.getRequestType(), TransTechSystem.config.getMqttTopicConfig().getDefaultTopic());
			r1.put("detached", com.getId());
			mh.publish(r1);
			logger.info("Component detachment successful!");
		} else  { //ERROR: Component not yet registered
			logger.warn("No component '" + cid + "' was found! Detachment process ended");
			ErrorResponse er = new ErrorResponse("dtch",  request.getRequestType(), TransTechSystem.config.getMqttTopicConfig().getErrorTopic(), 
					"Component '" + cid + "' not yet registered!");
			mh.publish(er);
		}
		com = null;
	}
}
