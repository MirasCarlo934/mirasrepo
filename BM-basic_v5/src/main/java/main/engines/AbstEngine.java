package main.engines;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import json.objects.ResError;
import main.Controller;
import main.engines.requests.EngineRequest;
import tools.SystemTimer;

public abstract class AbstEngine extends TimerTask {
	protected String name;
	private String className;
	protected static Logger LOG;
	protected LinkedList<EngineRequest> reqQueue = new LinkedList<EngineRequest>();
	protected HashMap<String, Thread> threads = new HashMap<String, Thread>(10, 1);
	protected HashMap<String, Object> responses = new HashMap<String, Object>(10, 1);
	private Timer timer;
	private int counter = 0;

	/**
	 * The current EngineRequest being processed by this Engine. Changes every time the <i>run()</i>
	 * method is invoked.
	 */
	protected EngineRequest currentRequest = null;
	//private SystemTimer systimer;
	
	public AbstEngine(String name, String className) {
		this.name = name;
		this.className = className;
		LOG = Logger.getLogger(name);
		timer = new Timer(name);
		timer.schedule(this, 0, 10);
	}
	
	/**
	 * Processes the given EngineRequest.
	 * 
	 * @param engineRequest The EngineRequest
	 * @return the response of the Engine, can be ResError if the Engine encountered an error or 
	 * 		if the EngineRequest is invalid
	 */
	public void forwardRequest(EngineRequest engineRequest, Thread t) {
		LOG.debug("Adding " + engineRequest.getClass().getName() + " " + 
				engineRequest.getId() + " to " + name + "!");
		reqQueue.add(engineRequest);
		threads.put(engineRequest.getId(), t);
		//LOG.fatal("Thread1: " + t.getName());
	}
	
	/**
	 * Retrieves the response from the specified EngineRequest
	 * @param engineRequestID The ID of the EngineRequest
	 * @return the response Object
	 */
	public Object getResponse(String engineRequestID) {
		Object o = responses.remove(engineRequestID);
		return o;
	}
	
	@Override
	public void run() {
		if(!reqQueue.isEmpty()) {
			counter++;
			EngineRequest er = reqQueue.getFirst();
			Thread t = threads.remove(er.getId());
			//LOG.fatal("ERID:" + er.getId());
			Thread process = new Thread(new EngineProcessor(t), 
					t.getName());
			process.start();
			//LOG.fatal("Thread2: " + t.getName());
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
				EngineRequest er = reqQueue.removeFirst();
				boolean b = checkEngineRequest(er);
				if(b) {
					Object res = processRequest(er);
					responses.put(er.getId(), res);
					//LOG.fatal("Response:" + er.getId());
				} else {
					responses.put(er.getId(), new ResError(name, "BM", "N/A", 
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
