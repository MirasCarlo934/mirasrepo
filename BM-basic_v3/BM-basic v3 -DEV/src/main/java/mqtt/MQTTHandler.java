package mqtt;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import main.objects.*;
import main.objects.request_response.Response;

public class MQTTHandler implements MqttCallback {
	private Logger logger;
	private MQTTClient client;
	private MQTTPublisher publisher;
	private Callback callback = null;
	private String component; //defines w/c system component the MQTTHandler belongs to
	/*
	 * The following are for easier value manipulation in case they need to be changed.
	 */
	private static final int default_qos = 1;
	
	/*
	 * Add error handling procedure to exceptionHandler
	 */
	public MQTTHandler(String component) {
		logger = Logger.getLogger(component + "/MQTTHandler");
		this.component = component;
	}
	
	public void start(String mqttURL) throws MqttException { //initializes client, callback and publisher
		logger.trace("Connecting to MQTT Broker in " + mqttURL + "...");
		client = new MQTTClient(mqttURL, component + "-MQTTClient_DEV");
		client.connect();
		if(client.isConnected()) {
			logger.trace("Setting up publisher...");
			publisher = new MQTTPublisher(client, component + "/MQTTPublisher");
			logger.trace("Setting up MQTT callback...");
			client.setCallback(this);
		} else {
			logger.fatal("Client not yet connected!");
		}
	}
	
	/**
	 * The best way to publish using a Response object
	 * 
	 * @param response
	 * @throws MqttException 
	 */
	public void publish(Response response) throws MqttException {
		publish(response.getTopic(), response);
	}
	
	public void publish(String topic, String content) throws MqttException { //for publishing with default QOS=0
    	publisher.publish(topic, content, 0);
    }
    
    public void publish(String topic, String content, int qos) throws MqttException{ //default publish method
        publisher.publish(topic, content, qos);
    }
    
    public void publish(String topic, Response response) throws MqttException {
    	publish(topic, response.toJSONObject().toString());
    }
    
    public void publish(String topic, Response response, int qos) throws MqttException {
    	publish(topic, response.toJSONObject().toString(), qos);
    }
    
    public void publish(String topic, Vector<Response> responses) throws MqttException {
    	for(int i = 0; i < responses.size(); i++) {
    		publish(topic, responses.get(i).toJSONObject().toString());
    	}
    }
    
    public void publish(String topic, Vector<Response> responses, int qos) throws MqttException { //for multiple responses
    	for(int i = 0; i < responses.size(); i++) {
    		publish(topic, responses.get(i).toJSONObject().toString(), qos);
    	}
    }
	
	public void disconnect() throws MqttException {
		client.disconnect();
	}
	
	public void subscribe(String topic, int qos) throws MqttException {
		client.subscribe(topic, qos);
	}
	
	/**
	 * Subscribes to the MQTT topic with the default QOS
	 * 
	 * @param topic
	 * @throws MqttException
	 */
	public void subscribe(String topic) throws MqttException {
		client.subscribe(topic, default_qos);
	}
	
	public void unsubscribe(String topic) throws MqttException {
		client.unsubscribe(topic);
	}
	
	public void connectionLost(Throwable arg0) {
		logger.fatal("Connection is lost with MQTT server!");
		logger.fatal(arg0.fillInStackTrace());
	}

	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		logger.info("Message(" + message.toString() + ") arrived in topic '" + topic + "'");
		if(getCallback() == null) { //true if callback is not yet set in Spring config
			logger.fatal("Callback is not yet set! (Callback object is different from MQTT callback)");
		} else {
			try {
				getCallback().processMessage(topic, message);
			} catch (Exception e) {
				exceptionHandler("Cannot process message!", e);
			}
		}
	}
	
	private void exceptionHandler(String msg, Exception e) {
		logger.error(msg, e);
	}

	/**
	 * @return the callback
	 */
	public Callback getCallback() {
		return callback;
	}

	/**
	 * @param callback the callback to set
	 */
	public void setCallback(Callback callback) {
		this.callback = callback;
	}
}
