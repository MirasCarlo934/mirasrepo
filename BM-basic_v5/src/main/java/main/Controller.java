package main;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import mqtt.MQTTHandler;

public class Controller {
	private static final Logger LOG = Logger.getLogger("BM_LOG.Controller");
	private MQTTHandler mh;
	private ComponentRepository devices;
	public static int processCounter = 1;
	
	public Controller(MQTTHandler mh, ComponentRepository devices) {
		this.mh = mh;
		this.devices = devices;
		LOG.info("Controller constructed!");
	}

	public void processMqttMessage(MqttMessage mqttMessage) {
		LOG.trace("Controller request processing started");
		Thread t = new Thread(new ControllerModule(mqttMessage, mh, devices), "Process" + processCounter);
		t.start();
		processCounter++;
	}

	/**
	 * @return the devices
	 */
	public ComponentRepository getDevices() {
		return devices;
	}

	/**
	 * @param devices the devices to set
	 */
	public void setDevices(ComponentRepository devices) {
		this.devices = devices;
	}
}
