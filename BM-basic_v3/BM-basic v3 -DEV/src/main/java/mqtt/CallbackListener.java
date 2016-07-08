/*
 * CallbackListener:
 * 		1. Listens to MQTTCallback for instructions from MQTTBroker and also for OutboundMessages from 
 */

package mqtt;

import java.util.TimerTask;

import org.apache.log4j.Logger;

public abstract class CallbackListener extends TimerTask {
	protected Logger logger;
	private String name;
	
	public CallbackListener(String name) {
		this.setName(name);
		logger = Logger.getLogger(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
