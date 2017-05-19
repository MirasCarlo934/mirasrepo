package main.controller;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import main.ComponentRepository;
import mqtt.MQTTHandler;

public class Controller {
	private static Logger LOG;
	private MQTTHandler mh;
	private ComponentRepository devices;
	public static int processCounter = 1;
	private ThreadPool threadPool;
	
	public Controller(String logDomain, MQTTHandler mh, ComponentRepository devices, ThreadPool threadPool) {
		LOG = Logger.getLogger(logDomain + ".Controller");
		this.mh = mh;
		this.devices = devices;
		this.threadPool = threadPool;
		//threadPool = new ThreadPool(3, 10, 5, TimeUnit.SECONDS, 
		//		new ArrayBlockingQueue<Runnable>(10), threadFactory, new ThreadRejectionHandler());
		LOG.info("Controller constructed!");
	}

	public void processMQTTMessage(MqttMessage mqttMessage) {
		LOG.info("Request received!");
		threadPool.execute(new ControllerModule(processCounter, mqttMessage, mh, devices));
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