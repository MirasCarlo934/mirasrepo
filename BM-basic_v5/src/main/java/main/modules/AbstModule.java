package main.modules;

import org.apache.log4j.Logger;

import json.objects.ReqRequest;
import json.objects.ResError;
import main.ComponentRepository;
import mqtt.MQTTHandler;

public abstract class AbstModule {
	private String name;
	private String requestType;
	private String[] params;
	protected static Logger LOG;
	protected MQTTHandler mh;
	protected ComponentRepository cr;

	public AbstModule(String name, String RTY, String[] params, MQTTHandler mh, ComponentRepository cr) {
		LOG = Logger.getLogger("BM_LOG." + name);
		this.name = name;
		this.setParams(params);
		this.mh = mh;
		this.cr = cr;
		requestType = RTY;
	}
	
	public void processRequest(ReqRequest request) {
		LOG.debug(name + " request processing started!");
		if(checkRequestValidity(request)) {
			LOG.trace("Request valid! Proceeding to request processing...");
			process(request);
		} else {
			LOG.error("Secondary request params didn't check out. See also the additional request params"
					+ " checking.");
			//mh.publishToErrorTopic("Invalid request. Check secondary request parameters.");
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
	 * 		</ul>
	 */
	private boolean checkRequestValidity(ReqRequest request) {
		LOG.trace("Checking secondary request parameters...");
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
	 * Used in case of additional request parameter checking. <i>Must always return true if there are no additional request checking</i>
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
		LOG.error(error.message);
		mh.publishToErrorTopic(error);
		mh.publish(error);
	}

	/**
	 * @return the requestType
	 */
	public String getRequestType() {
		return requestType;
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
