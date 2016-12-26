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

public abstract class Engine extends TimerTask {
	private String name;
	private String className;
	protected static Logger LOG;
	protected HashMap<String, EngineRequest> reqQueue = new HashMap<String, EngineRequest>(10,10);
	protected HashMap<String, Object> resQueue = new HashMap<String, Object>(10,10);
	private int counter = 1;
	private Timer timer;
	//private SystemTimer systimer;
	
	public Engine(String name, String className) {
		this.name = name;
		this.className = className;
		timer = new Timer(name + "Processor");
		//this.systimer = systimer;
		LOG = Logger.getLogger("ENGINE_LOG." + name);
		timer.schedule(this, 0, 10);
	}
	
	/**
	 * Processes the given EngineRequest.
	 * 
	 * @param engineRequest The EngineRequest
	 * @return the response of the Engine, can be ResError if the Engine encountered an error or 
	 * 		if the EngineRequest is invalid
	 */
	public Object forwardRequest(EngineRequest engineRequest) {
		LOG.debug("Adding " + engineRequest.getClass().toString() + " " + 
				engineRequest.getId() + " to " + name + "!");
		reqQueue.put(engineRequest.getId(), engineRequest);
		/*Thread t = new Thread(this, name + "Process" + counter);
		counter++;
		t.start();*/
		//wait for Engine to process the request
		long processStart = Calendar.getInstance().getTimeInMillis();
		while(!resQueue.containsKey(engineRequest.getId())) {
			long now = Calendar.getInstance().getTimeInMillis();
			if(now - processStart > 10000) break;
		}
		if(resQueue.containsKey(engineRequest.getId())) {
			LOG.info("EngineRequest " + engineRequest.getId() + " processing complete!");
			return resQueue.get(engineRequest.getId());
		} else {
			LOG.error("Processing failure!");
			return new ResError(name, "BM", "Processing failure!");
		}
	}
	
	
	/**
	 * 
	 */
	@Override
	public void run() {
		if(!reqQueue.isEmpty()) {
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
			counter++;
			//notifyAll();
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
}
