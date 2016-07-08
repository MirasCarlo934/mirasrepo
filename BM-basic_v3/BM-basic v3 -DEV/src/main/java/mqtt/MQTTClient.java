package mqtt;

import java.util.Timer;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MQTTClient extends MqttClient {
	private final Logger logger;

	public MQTTClient(String serverURI, String name) throws MqttException { //before Config object implementation
		super(serverURI, name);
		logger = Logger.getLogger(name);
	}
	
	public void connect() throws MqttException{ //before Config object implementation
    	//connect to MQTT broker
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setKeepAliveInterval(0);
        super.connect(connOpts);
        logger.info("Connected to MQTTbroker!");
    }
    
    public void subscribe(String topic, int qos) throws MqttException {
    	//subscribe to a certain topic
    	super.subscribe(topic, qos);
    	logger.trace("Subscribed to '" + topic + "'");
    }
    
    public void disconnect() throws MqttException {
        //disconnect from MQTT broker
        super.disconnect();
        logger.info("Disconnected from MQTTbroker!");
    }
}
