package bm.modules;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttException;

import bm.ComponentRepository;
import bm.TTTIEngine;
import bm.TransactionEngine;
import bm.modules.parents.Module;
import main.components.Component;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.InsertTransactionRequest;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import mqtt.MQTTHandler;

public class DMPModule extends Module {
	private static final String request_type = "message";
	private static final String msg_obj_type = "message";
	private static final String msgname_req_param = "msg_name"; //the message name, used by the receiving component to identify the kind of message sent
	private static final String msg_req_param = "msg"; //the message contents
	private static final String receiver_req_param = "to"; //the ssid of the receiving component
	private static final String[] request_params = {msg_req_param, msgname_req_param, receiver_req_param};
	
	private static final String msg_name_col_name = "msg_name";
	private static final String msg_col_name = "message";
	private static final String receiver_col_name = "receiver";
	private static final String from_col_name = "sender";
	
	private static final String success_res_param = "success"; //true if message delivery is successful
	private static final String from_res_param = "from";
	
	private static final String msg_response_id = "msg"; //signifies that the response is a message

	private ComponentRepository cr;
	private TransactionEngine te;
	private TTTIEngine tttie;
	
	public DMPModule(MQTTHandler mqttHandler, ComponentRepository componentRepository, TransactionEngine transactionEngine, 
			TTTIEngine tttiEngine) {
		super("BM/DMPModule", request_type, request_params, mqttHandler);
		cr = componentRepository;
		te = transactionEngine;
		tttie = tttiEngine;
	}

	@Override
	protected void process(Request request) throws Exception {
		logger.info("Processing message...");
		String msg = request.getString(msg_req_param);
		String msg_name = request.getString(msgname_req_param);
		String receiver = request.getString(receiver_req_param); //the ssid of the receiving component
		
		/*
		 * Sends message to receiving component
		 */
		boolean b = true;
		if(cr.containsComponent(receiver)) {
			tttie.message(receiver, request.getComponentID(), msg_name, msg);
		}
		else { //error 1: Invalid component ID
			handleError(request, "Invalid component ID");
			b = false;
		}
		
		/*
		 * Sends success response to requesting component
		 */
		Response r = new Response(request.getRequestID(), request.getTopic());
		if(b){
			r.put(success_res_param, true);
		} else {
			r.put(success_res_param, false);
		}
		getMqttHandler().publish(r);
	}
	
	private void handleError(Request request, String error) {
		logger.error(error + "!");
		ErrorResponse e = new ErrorResponse(request.getRequestID(), request.getTopic(), error);
		try {
			getMqttHandler().publish(e);
		} catch (MqttException e1) {
			logger.error("Cannot publish error message!", e1);
		}
	}
}
