package mqtt;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public abstract class MQTTCallback implements MqttCallback {
	protected final Logger logger; 
	//private Vector<OutboundMessage> outboundMessages = new Vector<OutboundMessage>(1, 1);
	
	public MQTTCallback(String name) {
		logger = Logger.getLogger(name);
	}

	public void connectionLost(Throwable arg0) {
		logger.fatal("Connection is lost with MQTT server!");
		logger.fatal(arg0.fillInStackTrace());
	}

	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		logger.debug("Message(" + message.toString() + ") arrived in topic(" + topic + ")");
		try {
			processMessage(topic, message);
		} catch (Exception e) {
			logger.fatal("Cannot process message!");
			logger.trace(e);
		}
	}
	
	public abstract void processMessage(String topic, MqttMessage message) throws Exception; //for processing arrived message
	
	/*public void addOutboundMessages(Vector<OutboundMessage> vom) {
		outboundMessages.addAll(vom);
	}
	
	public void addOutboundMessage(String topic, String message) {
		OutboundMessage om = new OutboundMessage(topic, message);
		outboundMessages.add(om);
	}
	
	public void addOutboundMessage(String topic, String message, int qos) {
		OutboundMessage om = new OutboundMessage(topic, message, qos);
		outboundMessages.add(om);
	}
	
	public OutboundMessage getOutboundMessage() {
		//gets outbound message at the first position in the vector
		return outboundMessages.remove(0);
	}
	
	public boolean hasOutboundMessages() {
		return !outboundMessages.isEmpty();
	}*/
}
