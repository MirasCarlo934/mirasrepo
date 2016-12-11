/*
 * 	Callback documentation:
 * 		The Callback object was created in order to achieve non-abstraction in the MQTTHandler object. This is done so that the MQTTHandler
 * 	object can be mass produced for multiple system components without having to create daughter classes for each. The only abstract part
 * 	of an MQTTHandler is its MqttCallback functionality. Instead of creating daughter classes for every component to define the processes
 * 	the MQTTHandler has to perform in each invocation of the 'messageArrived' function, the MQTTHandler instead forwards the topic and 
 * 	message to a Callback object, where the specific processes are defined. In this sense, only the Callback class needs to have a daughter
 * 	class for each system component.
 * 
 * 		Each system component that uses an MQTTHandler must define its own Callback object by inheriting the 'processMessage' method and
 * 	define the MqttMessage processing inside the said method. The created Callback object is then injected into the MQTTHandler of the
 * 	system component.
 */
package mqtt;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public abstract class Callback {
	
	public abstract void processMessage(String topic, MqttMessage message);
}
