package main.engines;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import json.objects.ResError;
import main.Controller;
import main.engines.requests.EngineRequest;
import tools.SystemTimer;

public abstract class Engine {
	protected String name;
	private String className;
	protected static Logger LOG;
	protected HashMap<String, EngineRequest> reqQueue = new HashMap<String, EngineRequest>(10,10);
	protected HashMap<String, Object> resQueue = new HashMap<String, Object>(10,10);

	/**
	 * The current EngineRequest being processed by this Engine. Changes every time the <i>run()</i>
	 * method is invoked.
	 */
	protected EngineRequest currentRequest = null;
	//private SystemTimer systimer;
	
	public Engine(String name, String className) {
		this.name = name;
		this.className = className;
		LOG = Logger.getLogger(name);
	}
	
	/**
	 * Processes the given EngineRequest.
	 * 
	 * @param engineRequest The EngineRequest
	 * @return the response of the Engine, can be ResError if the Engine encountered an error or 
	 * 		if the EngineRequest is invalid
	 */
	public Object forwardRequest(EngineRequest engineRequest) {
		LOG.trace("Adding " + engineRequest.getClass().toString() + " " + 
				engineRequest.getId() + " to " + name + "!");
		reqQueue.put(engineRequest.getId(), engineRequest);
		Thread parent = Thread.currentThread();
		Thread process = new Thread(new EngineProcessor(parent), "Process" + Controller.processCounter);
		process.start();
		
		synchronized (parent) {
			try {
				parent.wait();
			} catch (InterruptedException e) {
				LOG.error("Thread interrupted!");
				e.printStackTrace();
			}
		}
		if(resQueue.containsKey(engineRequest.getId())) {
			LOG.trace("EngineRequest " + engineRequest.getId() + " processing complete!");
			return resQueue.get(engineRequest.getId());
		} else {
			LOG.error("EngineRequest " + engineRequest.getId() + " processing failure!");
			return new ResError(name, "BM", "N/A", "Processing failure!");
		}
	}
	
	/**
	 * Checks if the EngineRequest is valid. Returns false if: <br>
	 * <ul>
	 * 		<li>This Engine cannot accommodate the given EngineRequest. (Wrong EngineRequest given)</li>
	 * </ul>
	 * @param er The EngineRequest
	 * @return <b><i>True</b></i> if the EngineRequest checks out, <b><i>False</b></i> otherwise.
	 */
	private boolean checkEngineRequest(EngineRequest er) {
		boolean b = false;
		
		if(er.getEngineType().equals(className)) {
			b = true;
		}
		
		return b;
	}
	
	protected abstract Object processRequest(EngineRequest er);
	
	private class EngineProcessor implements Runnable {
		Thread parent;
		
		private EngineProcessor(Thread parent) {
			this.parent = parent;
		}
		/**
		 * 
		 */
		@Override
		public void run() {
			if(!reqQueue.isEmpty()) {
				//checks if EngineRequest is valid for this Engine
				EngineRequest er = reqQueue.values().iterator().next();
				currentRequest = er;
				reqQueue.remove(er.getId());
				boolean b = checkEngineRequest(er);
				
				if(b) {
					Object res = processRequest(er);
					resQueue.put(er.getId(), res);
					//return processRequest(engineRequest);
				} else {
					resQueue.put(er.getId(), new ResError(name, "BM", "N/A", 
							"Invalid EngineRequest for " + name));
					//return new ResError(name, "BM", "Invalid EngineRequest for " + name);
				}

				synchronized (parent) {
					parent.notify();
				}
			}
		}
	}
}
