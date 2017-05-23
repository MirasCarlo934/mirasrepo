package main.modules;

import java.util.Vector;

import org.apache.log4j.Logger;

import cir.Statement;
import json.RRP.ReqRequest;
import json.RRP.ResError;
import main.ComponentRepository;
import main.engines.AbstEngine;
import main.engines.requests.EngineRequest;
import mqtt.MQTTHandler;

public abstract class AbstModule {
	protected String logDomain;
	protected Logger mainLOG;
	protected Logger errorLOG;
	private String name;
	protected String requestType;
	private String[] params;
	protected MQTTHandler mh;
	protected ComponentRepository cr;

	public AbstModule(String logDomain, String errorLogDomain, String name, String RTY, String[] 
			params, MQTTHandler mh, ComponentRepository cr) {
		mainLOG = Logger.getLogger(logDomain + "." + name);
		errorLOG = Logger.getLogger(errorLogDomain + "." + name);
		this.logDomain = logDomain;
		this.name = name;
		this.params = params;
		this.mh = mh;
		this.cr = cr;
		requestType = RTY;
	}
	
	public void processRequest(ReqRequest request) {
		mainLOG.debug(name + " request processing started!");
		if(checkSecondaryRequestValidity(request)) {
			mainLOG.trace("Request valid! Proceeding to request processing...");
			process(request);
		} else {
			mainLOG.error("Secondary request params didn't check out. See also the additional request params"
					+ " checking.");
			//mh.publishToErrorTopic("Invalid request. Check secondary request parameters.");
		}
	}
	
	/**
	 * Forwards the supplied EngineRequest to the specified Engine. Handles the thread waiting
	 * procedure and error handling for the Engine response.
	 * 
	 * @param engine The Engine where the EngineRequest will be sent to
	 * @param engineRequest The EngineRequest that will be processed by the Engine
	 * @return The Engine response object. Returns ResError object if the Engine encountered
	 * 		an error during EngineRequest processing.
	 */
	protected Object forwardEngineRequest(AbstEngine engine, EngineRequest engineRequest) {
		engine.processRequest(engineRequest, Thread.currentThread());
		try {
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
		} catch (InterruptedException e) {
			mainLOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		Object o = engine.getResponse(engineRequest.getId());
		if(o.getClass().equals(ResError.class)) {
			ResError error = (ResError) o;
			error(error);
			return error;
		}
		else {
			return o;
		}
	}
	
	/**
	 * Checks if the request contains all the required secondary parameters
	 * 
	 * @param request The Request object
	 * @return <b><i>True</b></i> if the request is valid, <b><i>false</i></b> if: <br>
	 * 		<ul>
	 * 			<li>There are missing secondary request parameters</li>
	 * 			<li>There are secondary request parameters that are null/empty</li>
	 * 			<li>The module-specific request parameter check failed
	 * 			<br><i>Each module can have additional request checks, see individual
	 * 			modules for more details.</i></li>
	 * 		</ul>
	 */
	private boolean checkSecondaryRequestValidity(ReqRequest request) {
		mainLOG.trace("Checking secondary request parameters...");
		boolean b = false; //true if request is valid
		
		if(params.length == 0) //there are no secondary request params
			b = true;
		else {
			for(int i = 0; i < getParams().length; i++) {
				String param = getParams()[i];
				if(request.getParameter(param) != null && !request.getParameter(param).equals("")) 
					b = true;
				else break;
			}
			
			if(b) { //if basic parameter checking is good
				b = additionalRequestChecking(request);
			}
		}
		
		return b;
	}
	
	protected abstract void process(ReqRequest request);
	
	/**
	 * Used in case of additional request parameter checking. <i><b>Must always return true 
	 * if there are no additional request checking</b></i>
	 * 
	 * @param request The Request object
	 * @return <b>True</b> if Request checks out, <b>false</b> otherwise. 
	 */
	protected abstract boolean additionalRequestChecking(ReqRequest request);
	
	
	/**
	 * Used to publish an error message in log, MQTT error topic, and requesting component topic
	 * 
	 * @param message The error message
	 */
	protected void error(ResError error) {
		mainLOG.error(error.message);
		errorLOG.error(error.message);
		mh.publishToErrorTopic(error);
		/*
		 * Do CID checking for this one
		 */
		//mh.publish(error);
	}
	
	/**
	 * Used to publish an exception message in log, MQTT error topic, and requesting component topic
	 * 
	 * @param e The Exception
	 */
	
	protected void error(Exception e) {
		mainLOG.error(e.getMessage(), e);
		errorLOG.error(e.getMessage(), e);
		mh.publishToErrorTopic(e.getMessage());
	}

	/**
	 * @return the params
	 */
	public String[] getParams() {
		return params;
	}

	/**
	 * @param params the params to set
	 */
	public void setParams(String[] params) {
		this.params = params;
	}
}
