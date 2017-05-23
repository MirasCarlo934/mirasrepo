package main.controller;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import json.RRP.ResError;
import mqtt.MQTTHandler;

public class ThreadRejectionHandler implements RejectedExecutionHandler {
	private static final Logger LOG = Logger.getLogger("controller.ThreadRejectionHandler");
	private MQTTHandler mh;
	
	public ThreadRejectionHandler(MQTTHandler mh) {
		this.mh = mh;
	}

	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		LOG.error("Failed to process the received request due to system overload!");
		ResError error = new ResError("Controller", 
				"Failed to process the received request due to system overload!");
		mh.publishToErrorTopic(error);
	}
}
