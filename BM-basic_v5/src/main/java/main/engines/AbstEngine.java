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
import main.controller.Controller;
import main.engines.requests.EngineRequest;
import tools.SystemTimer;

public abstract class AbstEngine extends TimerTask {
	protected Logger LOG;
	protected String name;
	private String className;
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
		timer = new Timer(name);
		timer.schedule(this, 0, 100);
		LOG = Logger.getLogger("BM_LOG." + name);
	}
	
	/**
	 * Processes the given EngineRequest.
	 * 
	 * @param engineRequest The EngineRequest
	 * @return the response of the Engine, can be ResError if the Engine encountered an error or 
	 * 		if the EngineRequest is invalid
	 */
	public void processRequest(EngineRequest engineRequest, Thread t) {
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
			LOG.debug("New EngineRequest detected! Processing...");
			counter++;
			EngineRequest er = reqQueue.removeFirst();
			Thread t = threads.remove(er.getId());
			//LOG.fatal("ERID:" + er.getId());
			Thread process = new Thread(new EngineProcessor(er, t), 
					t.getName());
			process.start();
			//LOG.fatal("Thread2: " + t.getName());
		}
	}
	
	protected abstract Object processRequest(EngineRequest er);
	
	/**
	 * The processing thread of the Engine. This object is instantiated every time an Engine
	 * needs to process an ERQS request (See ERQS document for request handling procedure).
	 * <br><br>
	 * This thread is actually not necessary, provided that the processing can take place
	 * within the TimerTask of the Engine. But for the sake of unclogging the Engine thread
	 * with multiple requests arriving at the same time, this was created so that the queuing
	 * thread (Timer thread) will not have to handle the processing which takes considerable
	 * amount of time. Instead, the processing takes place here, on a separate thread.
	 */
	private class EngineProcessor implements Runnable {
		EngineRequest er;
		Thread parent;
		
		/**
		 * The processing thread of the Engine. This object is instantiated every time an Engine
		 * needs to process an ERQS request (See ERQS document for request handling procedure).
		 * <br><br>
		 * This thread is actually not necessary, provided that the processing can take place
		 * within the TimerTask of the Engine. But for the sake of unclogging the Engine thread
		 * with multiple requests arriving at the same time, this was created so that the queuing
		 * thread (Timer thread) will not have to handle the processing which takes considerable
		 * amount of time. Instead, the processing takes place here, on a separate thread.
		 */
		private EngineProcessor(EngineRequest er, Thread parent) {
			this.er = er;
			this.parent = parent;
		}
		
		@Override
		public void run() {
			//checks if EngineRequest is valid for this Engine
			boolean b = checkEngineRequest(er);
			if(b) {
				Object res = processRequest(er);
				responses.put(er.getId(), res);
			} else {
				responses.put(er.getId(), new ResError(name, "BM", "N/A", 
						"Invalid EngineRequest for " + name));
			}

			synchronized (parent) {
				parent.notify();
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
	}
}
