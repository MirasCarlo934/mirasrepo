package bm.modules.parents;

import org.eclipse.paho.client.mqttv3.MqttException;

import bm.TransactionEngine;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import main.objects.request_response.TransactionResponse;
import mqtt.MQTTHandler;

public abstract class TransactionModule extends Module {
	/*
	 * System constants. Change when necessary
	 */
	private static final String obj_response_param = "obj"; //param name for Response of DBObject
	private static final String success_response_param = "success"; //param name for response of boolean isSuccessful()
	
	private TransactionEngine transactionEngine;
	
	public TransactionModule(String name, String request_type, String[] request_params, MQTTHandler mqttHandler, 
			TransactionEngine transactionEngine) {
		super(name, request_type, request_params, mqttHandler);
		this.transactionEngine = transactionEngine;
	}

	/**
	 * Handles publishing of TransactionResponse.
	 * @param tres
	 * @throws MqttException
	 */
	protected void publishTransRes(TransactionResponse tres) throws MqttException {
		/*String RID = tres.getRequestID();
		String topic = tres.getTopic();
		
		if(tres.isSuccessful()) {
			Response r = new Response(RID, topic);
			if(tres.getObjects().isEmpty()) { //no objects were retrieved
				r.put(obj_response_param, "none");
				r.put(success_response_param, true);
				getMqttHandler().publish(topic, r);
			}
			else {
				while(!tres.getObjects().isEmpty()) { //creates new response for every object retrieved
					r = new Response(RID, topic);
					r.put(obj_response_param, tres.getObjects().remove(0));
					getMqttHandler().publish(r);
				}
			}
		} else {
			ErrorResponse r = new ErrorResponse(RID, topic, transactionEngine.getError());
			r.put(success_response_param, false);
			getMqttHandler().publish(topic, r);
		}*/
		getMqttHandler().publish(tres);
	}

	/**
	 * @return the transactionEngine
	 */
	public TransactionEngine getTransactionEngine() {
		return transactionEngine;
	}

	/**
	 * @param transactionEngine the transactionEngine to set
	 */
	public void setTransactionEngine(TransactionEngine transactionEngine) {
		this.transactionEngine = transactionEngine;
	}
}
