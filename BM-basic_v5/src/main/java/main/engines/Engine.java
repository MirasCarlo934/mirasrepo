package main.engines;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.log4j.Logger;

import json.objects.ResError;
import main.Controller;
import main.engines.requests.EngineRequest;

public abstract class Engine implements Runnable {
	private String name;
	private String className;
	protected static Logger LOG;
	protected HashMap<String, EngineRequest> reqQueue = new HashMap<String, EngineRequest>(10,10);
	protected HashMap<String, Object> resQueue = new HashMap<String, Object>(10,10);
	
	public Engine(String name, String className) {
		this.name = name;
		this.className = className;
		LOG = Logger.getLogger("Engine_LOG." + name);
		//Timer timer = new Timer("EngineProcessingTimer");
	}
	
	/**
	 * Processes the given EngineRequest.
	 * 
	 * @param engineRequest The EngineRequest
	 * @return the response of the Engine, can be ResError if the Engine encountered an error or 
	 * 		if the EngineRequest is invalid
	 */
	public Object forwardRequest(EngineRequest engineRequest) {
		LOG.debug("Adding EngineRequest " + engineRequest.getId() + " to " + name + "!");
		reqQueue.put(engineRequest.getId(), engineRequest);
		Thread t = new Thread(this, "DBEProcess" + (Controller.processCounter - 1));
		t.start();
		int i = 0;
		while(!resQueue.containsKey(engineRequest.getId())) {
			i++;
			//if(i > 10000) break;
		}
		if(resQueue.containsKey(engineRequest.getId())) {
			return resQueue.get(engineRequest.getId());
		} else {
			LOG.error("Processing failure!");
			return new ResError(name, "BM", "Processing failure!");
		}
	}
	
	@Override
	public void run() {
		//checks if EngineRequest is valid for this Engine
		EngineRequest er = reqQueue.values().iterator().next();
		reqQueue.remove(er.getId());
		boolean b = checkEngineRequest(er);
		
		if(b) {
			Object res = processRequest(er);
			resQueue.put(er.getId(), res);
			//return processRequest(engineRequest);
		} else {
			resQueue.put(er.getId(), new ResError(name, "BM", "Invalid EngineRequest for " + name));
			//return new ResError(name, "BM", "Invalid EngineRequest for " + name);
		}
		//notifyAll();
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
}
