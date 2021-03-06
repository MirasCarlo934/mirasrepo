package mqtt;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.springframework.beans.factory.annotation.Autowired;

import json.RRP.AbstResponse;
import main.ComponentRepository;
import main.controller.Controller;

/**
 * The MQTTHandler is the object that handles all interactions related to an MQTT server. 
 * Specifically, this object handles the connection of an MqttClient to the MQTT server, 
 * reception of messages into the BM topic, the publishing of messages to different topics, 
 * connection error handling, and the reconnection function for higher availability.
 * 
 * @author User
 *
 */
public class MQTTHandler extends TimerTask implements MqttCallback {
	private static Logger logger;
	@Autowired
	private Controller controller;
	@Autowired
	private ComponentRepository cr;
	private MqttClient client;
	private String brokerURL;
	private String clientID;
	private String BM_topic;
	private String default_topic;
	private String error_topic;
	private LinkedList<MQTTMessage> queue = new LinkedList<MQTTMessage>();
	private Timer publishTimer = new Timer("MQTTHandler");

	public MQTTHandler(String logDomain, String brokerURL, String clientID, String BM_topic, 
			String default_topic, String error_topic) {
		logger = Logger.getLogger(logDomain + ".MQTTHandler");
		this.setBrokerURL(brokerURL);
		this.setClientID(clientID);
		setBM_topic(BM_topic);
		setDefault_topic(default_topic);
		setError_topic(error_topic);
		try {
			client = new MqttClient(brokerURL, clientID);
		} catch (MqttException e) {
			logger.fatal("Cannot create MQTTClient!", e);
		}
		connectToMQTT();
		publishTimer.schedule(this, 0, 50);
	}
	
	/**
	 * Connects this MQTTHandler to the MQTT broker
	 * 
	 * @return <b>True</b> if the MQTTHandler has successfully connected, <b>false</b> otherwise.
	 */
	public boolean connectToMQTT() {
		try {
			client.connect();
			logger.info("Connected to MQTT!");
			client.subscribe(BM_topic, 0);
			logger.debug("Subscribed to BM topic!");
			client.setCallback(this);
			logger.debug("Callback set!");
			return true;
		} catch (MqttSecurityException e) {
			logger.fatal("Cannot connect to MQTT server due to MqttSecurityException!", e);
			logger.info("Attempting to reconnect...");
			connectToMQTT();
			return false;
		} catch (MqttException e) {
			logger.fatal("Cannot connect to MQTT due to MqttException!", e);
			logger.info("Attempting to reconnect...");
			connectToMQTT();
			return false;
		}
	}
	
	/**
	 * Publish to MQTT with String message
	 * @param destination The topic/cid to publish to
	 * @param message The message
	 */
	public void publish(String destination, String message) {
		String topic = destination;
		if(cr.getComponent(destination) != null) { //get Component if destination is a CID
			topic = cr.getComponent(destination).getTopic();
		}
		logger.trace("Adding new MQTTMessage to topic '" + topic + "' to queue...");
		queue.add(new MQTTMessage(topic, message, Thread.currentThread()));
	}
	
	/**
	 * Publish to MQTT with a specific destination
	 * @param destination The topic/cid to publish to
	 * @param message The message
	 */
	public void publish(String destination, AbstResponse response) {
		publish(destination, response.toString());
	}
	
	/**
	 * Publish to MQTT with the CID of the response
	 * @param destination The topic/cid to publish to
	 * @param message The message
	 */
	public void publish(AbstResponse response) {
		String topic = cr.getComponent(response.cid).getTopic();
		publish(topic, response.toString());
	}
	
	public void publishToDefaultTopic(String message) {
		publish(default_topic, message);
	}
	
	public void publishToDefaultTopic(AbstResponse response) {
		publish(default_topic, response.toString());
	}
	
	public void publishToErrorTopic(String message) {
		publish(error_topic, message);
	}
	
	public void publishToErrorTopic(AbstResponse response) {
		publish(error_topic, response.toString());
	}
	
	@Override
	public void run() {
		if(!queue.isEmpty()) {
			Thread t = new Thread(new MQTTPublisher(), queue.getFirst().callerThread.getName());
			t.start();
		}
	}

	public void connectionLost(Throwable arg0) {
		logger.fatal("Connection lost with MQTT server!", arg0);
		logger.info("Attempting to reconnect...");
		connectToMQTT();
	}

	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		System.out.println("\n\n");
		logger.debug("Message arrived at topic " + topic);
		logger.debug("Message is: " + msg.toString());
		controller.processMQTTMessage(msg);
	}

	/**
	 * @return the bM_topic
	 */
	public String getBM_topic() {
		return BM_topic;
	}

	/**
	 * @param bM_topic the bM_topic to set
	 */
	public void setBM_topic(String bM_topic) {
		BM_topic = bM_topic;
	}

	/**
	 * @return the default_topic
	 */
	public String getDefault_topic() {
		return default_topic;
	}

	/**
	 * @param default_topic the default_topic to set
	 */
	public void setDefault_topic(String default_topic) {
		this.default_topic = default_topic;
	}

	/**
	 * @return the error_topic
	 */
	public String getError_topic() {
		return error_topic;
	}

	/**
	 * @param error_topic the error_topic to set
	 */
	public void setError_topic(String error_topic) {
		this.error_topic = error_topic;
	}

	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public String getBrokerURL() {
		return brokerURL;
	}

	public void setBrokerURL(String brokerURL) {
		this.brokerURL = brokerURL;
	}
	
	private final class MQTTMessage {
		private String topic;
		private String message;
		private Thread callerThread;
		
		/**
		 * 
		 * @param topic
		 * @param message
		 * @param callerThread the Thread that called for the publishing of this MQTTMessage
		 */
		private MQTTMessage(String topic, String message, Thread callerThread) {
			this.topic = topic;
			this.message = message;
			this.callerThread = callerThread;
		}
	}
	
	private final class MQTTPublisher implements Runnable {

		@Override
		public void run() {
			MQTTMessage m = queue.removeFirst();
			logger.debug("Publishing message:" + m.message + " to topic:" + m.topic);
			MqttMessage payload = new MqttMessage(m.message.getBytes());
			payload.setQos(0);
			try {
				client.publish(m.topic, payload);
				logger.trace("Message published!");
			} catch (MqttPersistenceException e) {
				logger.error("Cannot publish message:" + m.message + " to topic:" + m.topic + "!", e);
			} catch (MqttException e) {
				logger.error("Cannot publish message:" + m.message + " to topic:" + m.topic + "!", e);
			}
		}
	}
}
