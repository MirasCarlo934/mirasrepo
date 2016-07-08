package mqtt;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public class MQTTPublisher {
	private final Logger logger;
    private MQTTClient client;
    
    public MQTTPublisher(MQTTClient client, String name) {
    	this.client = client;
    	logger = Logger.getLogger(name);
    }
    
    public void publish(String topic, String content) throws MqttException { //for publishing with default QOS=0
    	publish(topic, content, 0);
    }
    
    public void publish(String topic, String content, int qos) throws MqttException{
        //publish content to topic
        logger.trace("Publishing message: "+content+ " to topic '" + topic + "'");
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        client.publish(topic, message);
        logger.trace("Message published!");
    }
    
    public void publish(String topic, MqttMessage message, int qos) throws MqttPersistenceException, MqttException {
    	message.setQos(qos);
    	client.publish(topic, message);
    }
    
    public void publish(String topic, MqttMessage message) throws MqttException { //for publishing with default QOS=0
    	publish(topic, message, 0);
    }
}