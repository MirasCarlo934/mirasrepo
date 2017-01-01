package mqtt;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.springframework.beans.factory.annotation.Autowired;

import json.objects.AbstResponse;
import main.ComponentRepository;
import main.Controller;

public class MQTTHandler implements MqttCallback{
	private static final Logger logger = Logger.getLogger("BM_LOG.mqtt");
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

	public MQTTHandler(String brokerURL, String clientID) {
		this.brokerURL = brokerURL;
		this.clientID = clientID;
		try {
			client = new MqttClient(brokerURL, clientID);
		} catch (MqttException e) {
			logger.fatal("Cannot create MQTTClient!", e);
		}
	}
	
	public void connectToMQTT() {
		try {
			client.connect();
			logger.debug("Connected to MQTT!");
			client.subscribe(BM_topic, 0);
			logger.debug("Subscribed to BM topic!");
			client.setCallback(this);
			logger.debug("Callback set!");
		} catch (MqttSecurityException e) {
			logger.fatal("Cannot connect to MQTT server!", e);
		} catch (MqttException e) {
			logger.fatal("Cannot connect to MQTT server!", e);
		}
	}
	
	/**
	 * Publish to MQTT with String message
	 * @param destination The topic/cid to publish to
	 * @param message The message
	 */
	public void publish(String destination, String message) {
		String topic = destination;
		if(cr.getComponent(destination) != null) {
			topic = cr.getComponent(destination).getTopic();
		}
		logger.trace("Publishing message:" + message + " to topic:" + topic);
		MqttMessage m = new MqttMessage(message.getBytes());
		m.setQos(0);
		try {
			client.publish(destination, m);
		} catch (MqttPersistenceException e) {
			logger.error("Cannot publish message:" + message + " to topic:" + destination + "!", e);
		} catch (MqttException e) {
			logger.error("Cannot publish message:" + message + " to topic:" + destination + "!", e);
		}
	}
	
	/**
	 * Publish to MQTT with a specific destination
	 * @param destination The topic/cid to publish to
	 * @param message The message
	 */
	public void publish(String destination, AbstResponse response) {
		String topic = destination;
		if(cr.getComponent(destination) != null) {
			topic = cr.getComponent(destination).getTopic();
		}
		logger.trace("Publishing message:" + response.toString() + " to topic:" + topic);
		MqttMessage m = new MqttMessage(response.toString().getBytes());
		m.setQos(0);
		try {
			client.publish(topic, m);
		} catch (MqttPersistenceException e) {
			logger.error("Cannot publish message:" + response.toString() + " to topic:" + topic + "!", e);
		} catch (MqttException e) {
			logger.error("Cannot publish message:" + response.toString() + " to topic:" + topic + "!", e);
		}
	}
	
	/**
	 * Publish to MQTT with the CID of the response
	 * @param destination The topic/cid to publish to
	 * @param message The message
	 */
	public void publish(AbstResponse response) {
		String topic = cr.getComponent(response.cid).getTopic();
		logger.trace("Publishing message:" + response.toString() + " to topic:" + topic);
		MqttMessage m = new MqttMessage(response.toString().getBytes());
		m.setQos(0);
		try {
			client.publish(topic, m);
		} catch (MqttPersistenceException e) {
			logger.error("Cannot publish message:" + response.toString() + " to topic:" + topic + "!", e);
		} catch (MqttException e) {
			logger.error("Cannot publish message:" + response.toString() + " to topic:" + topic + "!", e);
		}
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

	public void connectionLost(Throwable arg0) {
		logger.fatal("Connection lost with MQTT server!");
	}

	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		System.out.println("\n\n");
		logger.debug("Message arrived at topic " + topic);
		logger.debug("Message is: " + msg.toString());
		controller.processMqttMessage(msg);
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
}
