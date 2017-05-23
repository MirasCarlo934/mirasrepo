package main.modules;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import cir.ArgOperator;
import cir.Argument;
import cir.ExecutionBlock;
import cir.Statement;
import components.Component;
import components.properties.AbstProperty;
import components.properties.CommonProperty;
import components.properties.StringProperty;
import json.RRP.ReqPOOP;
import json.RRP.ReqRequest;
import json.RRP.ResError;
import json.RRP.ResPOOP;
import main.ComponentRepository;
import main.engines.CIREngine;
import main.engines.DBEngine;
import main.engines.OHEngine;
import main.engines.requests.CIREngine.GetSpecificExecBlocksCIREReq;
import main.engines.requests.CIREngine.GetStatementsCIREReq;
import main.engines.requests.CIREngine.UpdateCIREReq;
import main.engines.requests.DBEngine.RawDBEReq;
import main.engines.requests.DBEngine.UpdateDBEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class POOPModule extends AbstModule {
	private DBEngine dbe;
	private CIREngine cire;
	private OHEngine ohe;
	private String propIDParam;
	private String propValParam;
	private String propsTable = ""; //PROPERTIES table
	private String oh_topic;
	private IDGenerator idg = new IDGenerator();

	public POOPModule(String logDomain, String errorLogDomain, String RTY, String propIDParam, 
			String propValParam, String oh_topic, MQTTHandler mh, OHEngine ohe, ComponentRepository cr, 
			DBEngine dbe, CIREngine cire) {
		super(logDomain, errorLogDomain, "POOPModule", RTY, 
				new String[]{propIDParam, propValParam}, mh, cr);
		this.dbe = dbe;
		this.cire = cire;
		this.propIDParam = propIDParam;
		this.propValParam = propValParam;
		this.oh_topic = oh_topic;
	}

	/**
	 * Updates the system of the property change of the requesting component and also the property
	 * changes of all the affected components according to CIR.
	 * 
	 * @param request The Request to be processed. <b>Must be</b> a <i>ReqPOOP</i> object.
	 */
	@Override
	protected void process(ReqRequest request) {
		ReqPOOP poop = new ReqPOOP(request, propIDParam, propValParam);
		Component c = cr.getComponent(poop.cid);
		
		mainLOG.info("Changing component " + request.cid + " property " 
				+ poop.propSSID + " to " + poop.propValue + "...");
		if(c.getProperty(poop.propSSID).getValue().equals(poop.propValue)) {
			mainLOG.info("Property is already set to " + poop.propValue + "!");
			mh.publish(new ResPOOP(request, poop.propSSID, poop.propValue));
			mh.publish("openhab/" + c.getTopic(), poop.propSSID + "_" + poop.propValue);
		}
		else {
			
			mainLOG.debug("Updating component property in system...");
			forwardEngineRequest(cire, new UpdateCIREReq(idg.generateMixedCharID(10)));
			try {
				AbstProperty prop = c.getProperty(poop.propSSID);
				prop.setValue(poop.propValue, dbe, propsTable, logDomain);
				prop.publishPropertyValueToMQTT(mh, poop.rid, poop.cid, poop.rty);
			} catch (Exception e) {
				mainLOG.error("Cannot change property " + poop.propSSID + " of component " + 
						poop.cid);
				error(e);
				e.printStackTrace();
			}
			
			mainLOG.info("Updating affected component properties in system...");
			boolean updatedOthers = false;
			GetSpecificExecBlocksCIREReq cirer1 = new GetSpecificExecBlocksCIREReq(idg.generateMixedCharID(10), 
					poop);
			Object o = forwardEngineRequest(cire, cirer1);
			Vector<ExecutionBlock> execs = (Vector<ExecutionBlock>) o;
			if(execs.size() > 0) updatedOthers = true;
			for(int k = 0; k < execs.size(); k++) {
				ExecutionBlock exec = execs.get(k);
				Component com = cr.getComponent(exec.getComID());
				mainLOG.info("Changing commponent " + com.getSSID() + " property " + 
						exec.getPropSSID() + " to " + exec.getPropValue() + "...");
				try {
					AbstProperty prop = com.getProperty(exec.getPropSSID());
					prop.setValue(exec.getPropValue(), dbe, propsTable, logDomain);
					prop.publishPropertyValueToMQTT(mh, poop.rid, com.getSSID(), poop.rty);
				} catch (Exception e) {
					mainLOG.error("Cannot change property " + exec.getPropSSID() + " of component " + 
							com.getSSID());
					error(e);
					e.printStackTrace();
				}
				//updates the physical component
				//mh.publish(com.getTopic(), new ResPOOP(poop.rid, com.getSSID(), poop.rty,
				//		exec.getPropSSID(), exec.getPropValue()));
				//updates OpenHAB component item
				//mh.publish(oh_topic + "/" + com.getTopic(), exec.getPropSSID() + "_"
				//		+ exec.getPropValue());
			}
			if(!updatedOthers) mainLOG.info("No other components updated!");
		}
		mainLOG.debug("POOP processing complete!");
	}

	/**
	 * Checks if request follows the following requirements:
	 * <ol>
	 * 	<li>Component with CID has the specified property</li>
	 * 	<li>Value specified is valid for the property</li>
	 * 	<ul>
	 * 		<li><b>For CommonProperty: <i>true</i></b> if value is an integer and is within min/max
	 * 			range of the specified property</li>
	 * 	</ul>
	 * </ol>
	 */
	@Override
	protected boolean additionalRequestChecking(ReqRequest request) {
		boolean b = false;
		ReqPOOP poop = new ReqPOOP(request.getJSON(), propIDParam, propValParam);
		
		Component c = cr.getComponent(poop.cid);
		if(c.getProperty(poop.propSSID) != null) { //checks if property exists in the component
			AbstProperty prop = c.getProperty(poop.propSSID);
			
			if(prop.getClass().equals(CommonProperty.class)) { //for CommonProperty checking
				CommonProperty p = (CommonProperty) prop;
				try {//checks if request propValue is a percentage (denoted by a % symbol), used for handling POOP requests from OH Dimmer items
					String poop_pval = (String) poop.propValue;
					if(p.getClass().equals(CommonProperty.class) && poop_pval.substring(0, 1).equals("%")) {
						int poop_real_pval = 0;
						try {//checks if request propValue is an integer
							poop_real_pval = Integer.parseInt(poop_pval.substring(1));
						} catch(NumberFormatException e) {
							ResError re = new ResError(poop, "POOPRequest contains invalid property "
									+ "value! Ending request processing...");
							error(re);
							return false;
						}
						poop.propValue = poop_real_pval * (p.getMax() / 100);
					}
				} catch(ClassCastException e) {
					ResError re = new ResError(poop, "POOPRequest contains nonstring and non-integer "
							+ "property value. Errors may arise. Ending request processing...");
					error(re);
					return false;
				}
				try {//checks if request propValue is within min/max range
					if(p.getMin() <= Integer.parseInt(poop.propValue.toString()) && 
							p.getMax() >= Integer.parseInt(poop.propValue.toString())) {
						b = true;
					}
					else {
						ResError re = new ResError(poop, "Property value not within min/max of the "
								+ "property!");
						error(re);
						return false;
					}
				} catch (NumberFormatException e) {
					ResError re = new ResError(poop, "Specified property requires an integer property "
							+ "value!");
					error(re);
					return false;
				}
			} 
		}
		else {
			error(new ResError(poop, "Property does not exist!"));
			return false;
		}
		
		return b;
	}

	public void setPropsTable(String s) {
		this.propsTable = s;
	}
	
	/**
	 * Updates the system of the property change of the requesting component and also the property
	 * changes of all the affected components according to CIR.
	 * 
	 * @param poop
	 
	private void updateSystem(ReqPOOP poop) {
		LOG.debug("Updating component property in system...");
		Component c = cr.getComponent(poop.cid);
		c.setPropertyValue(poop.propSSID, poop.propValue);
		//Vector<ExecutionBlock> execsToBeApplied = new Vector<ExecutionBlock>(1,1);
		
		LOG.info("Updating affected component properties in system...");
		boolean updatedOthers = false;
		GetStatementsCIREReq cirer1 = new GetStatementsCIREReq(idg.generateMixedCharID(10), 
				c, c.getProperty(poop.propSSID));
		cire.forwardRequest(cirer1, Thread.currentThread());
		try {
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		Object o = cire.getResponse(cirer1.getId());
		Vector<Statement> rules = new Vector<Statement>(0);
		if(o.getClass().equals(ResError.class)) {
			error((ResError) o);
		}
		else {
			rules = (Vector<Statement>) o;
		}
		for(int i = 0; i < rules.size(); i++) {
			Statement rule = rules.get(i);
			Argument[] args = rule.getArguments();
			ExecutionBlock[] execs = rule.getExecBlocks();
			boolean rule_result = false; //TRUE if rule will be used, false otherwise
			
			//START FIX HERE! DON'T FORGET TO DO THE SAME IN updateDatabase()
			for(int j = 0; j < args.length; j++) {
				Argument arg = args[j];
				Component arg_com = cr.getComponent(arg.getComID());
				String pval = String.valueOf(arg_com.getProperty(arg.getPropSSID()).getValue());
				//LOG.fatal(arg_com.getName() + pval);
				if(arg.getOperator().equals(ArgOperator.EQUALS)) {
					if(arg.getPropValue().equals(pval)) {
						rule_result = true;
					}
				}
				else if(arg.getOperator().equals(ArgOperator.GREATER)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) < Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) {
						error(new ResError(poop,"Property is not a number!"));
					}
				}
				else if(arg.getOperator().equals(ArgOperator.LESS)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) > Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) {
						error(new ResError(poop,"Property is not a number!"));
					}
				}
				else if(arg.getOperator().equals(ArgOperator.GREATEREQUALS)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) <= Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) {
						error(new ResError(poop,"Property is not a number!"));
					}
				}
				else if(arg.getOperator().equals(ArgOperator.LESSEQUALS)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) >= Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) {
						error(new ResError(poop,"Property is not a number!"));
					}
				}
				else if(arg.getOperator().equals(ArgOperator.INEQUAL)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) != Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) { //signifies that the property is a string
						if(!arg.getPropValue().equals(pval)) {
							rule_result = true;
						}
					}
				}
				if(!rule_result) { //rule will not be used
					break;
				}
			}
			
			if(rule_result) { //rule will be used
				updatedOthers = true;
				
				//updates system
				for(int k = 0; k < execs.length; k++) {
					//LOG.fatal("ERROR ERROR");
					ExecutionBlock exec = execs[k];
					Component com = cr.getComponent(exec.getComID());
					LOG.info("Changing commponent " + com.getSSID() + " property " + 
							exec.getPropSSID() + " to " + exec.getPropValue() + "...");
					com.setPropertyValue(exec.getPropSSID(), Integer.parseInt(exec.getPropValue()));
					//LOG.fatal("2nd" + com.getName() + com.getProperty(exec.getPropName()));
					//updates the physical component
					mh.publish(com.getTopic(), new ResPOOP(poop.rid, com.getSSID(), poop.rty,
							exec.getPropSSID(), Integer.parseInt(exec.getPropValue())));
					//updates OpenHAB component item
					mh.publish("openhab/" + com.getTopic(), exec.getPropSSID() + "_"
							+ exec.getPropValue());
				}
				
				//updates database
				HashMap<String, Object> args1 = new HashMap<String, Object>(2);
				args1.put("com_id", poop.cid);
				args1.put("cpl_ssid", poop.propSSID);
				HashMap<String, Object> vals1 = new HashMap<String, Object>(1);
				vals1.put("prop_value", String.valueOf(poop.propValue));
				dbe.forwardRequest(new UpdateDBEReq(idg.generateMixedCharID(10), propsTable, vals1, args1),
						Thread.currentThread());
				for(int k = 0; k < execs.length; k++) {
					ExecutionBlock exec = execs[k];
					Component com = cr.getComponent(exec.getComID());
					HashMap<String, Object> args2 = new HashMap<String, Object>(2);
					args2.put("com_id", com.getSSID());
					args2.put("cpl_ssid", exec.getPropSSID());
					HashMap<String, Object> vals2 = new HashMap<String, Object>(1);
					vals2.put("prop_value", String.valueOf(exec.getPropValue()));
					LOG.debug("Updating component property in DB...");
					dbe.forwardRequest(new UpdateDBEReq(idg.generateMixedCharID(10), propsTable, vals2, 
							args2), Thread.currentThread());
					try {
						synchronized (Thread.currentThread()){Thread.currentThread().wait();}
					} catch (InterruptedException e) {
						LOG.error("Cannot stop thread!", e);
						e.printStackTrace();
					}
				}
			}	
		}
		if(!updatedOthers) LOG.info("No other components updated!");
	}*/
	
	/*private void updateDatabase(ReqPOOP poop) {
		LOG.debug("Updating component property in DB...");
		HashMap<String, Object> args1 = new HashMap<String, Object>(2);
		args1.put("com_id", poop.cid);
		args1.put("cpl_ssid", poop.propSSID);
		HashMap<String, Object> vals1 = new HashMap<String, Object>(1);
		vals1.put("prop_value", String.valueOf(poop.propValue));
		dbe.forwardRequest(new UpdateDBEReq(idg.generateMixedCharID(10), propsTable, vals1, args1),
				Thread.currentThread());
		try {
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		/*try {
			LOG.debug("Updating component property in DB...");
			//dbe.updateQuery(propsTable, args, vals);
		} catch (SQLException e) {
			LOG.error("Cannot update component property in DB!");
			e.printStackTrace();
		}
		
		LOG.debug("Updating affected component properties in DB...");
		Component c = cr.getComponent(poop.cid);
		//Vector<Statement> rules = cire.getCIRStatementsWithArgComponent(c, c.getProperty(poop.propIndex));
		GetStatementsCIREReq cirer1 = new GetStatementsCIREReq(idg.generateMixedCharID(10), 
				c, c.getProperty(poop.propSSID));
		cire.forwardRequest(cirer1, Thread.currentThread());
		try {
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		Object o = cire.getResponse(cirer1.getId());
		Vector<Statement> rules = new Vector<Statement>(0);
		if(o.getClass().equals(ResError.class)) {
			error((ResError) o);
			return;
		}
		else {
			rules = (Vector<Statement>) o;
		}
		
		for(int i = 0; i < rules.size(); i++) {
			Statement rule = rules.get(i);
			Argument[] args = rule.getArguments();
			ExecutionBlock[] execs = rule.getExecBlocks();
			boolean rule_result = false;
			for(int j = 0; j < args.length; j++) {
				Argument arg = args[j];
				Component arg_com = cr.getComponent(arg.getComID());
				//ArgOperator operator = arg.getOperator();
				String pval = String.valueOf(arg_com.getProperty(arg.getPropName()).getValue());
				if(arg.getOperator().equals(ArgOperator.EQUALS)) {
					if(arg.getPropValue().equals(pval)) {
						rule_result = true;
					}
				}
				else if(arg.getOperator().equals(ArgOperator.GREATER)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) < Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) {
						error(new ResError(poop,"Property is not a number!"));
					}
				}
				else if(arg.getOperator().equals(ArgOperator.LESS)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) > Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) {
						error(new ResError(poop,"Property is not a number!"));
					}
				}
				else if(arg.getOperator().equals(ArgOperator.GREATEREQUALS)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) <= Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) {
						error(new ResError(poop,"Property is not a number!"));
					}
				}
				else if(arg.getOperator().equals(ArgOperator.LESSEQUALS)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) >= Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) {
						error(new ResError(poop,"Property is not a number!"));
					}
				}
				else if(arg.getOperator().equals(ArgOperator.INEQUAL)) {
					try {
						if(Float.parseFloat(arg.getPropValue()) != Float.parseFloat(pval)) {
							rule_result = true;
						}
					} catch(NumberFormatException e) { //signifies that the property is a string
						if(!arg.getPropValue().equals(pval)) {
							rule_result = true;
						}
					}
				}
				if(!rule_result) {
					break;
				}
			}
			if(rule_result) {
				for(int k = 0; k < execs.length; k++) {
					ExecutionBlock exec = execs[k];
					Component com = cr.getComponent(exec.getComID());
					HashMap<String, Object> args2 = new HashMap<String, Object>(2);
					args2.put("com_id", com.getSSID());
					args2.put("cpl_ssid", exec.getPropName());
					HashMap<String, Object> vals2 = new HashMap<String, Object>(1);
					vals2.put("prop_value", String.valueOf(exec.getPropValue()));
					LOG.debug("Updating component property in DB...");
					dbe.forwardRequest(new UpdateDBEReq(idg.generateMixedCharID(10), propsTable, vals2, 
							args2), Thread.currentThread());
					try {
						synchronized (Thread.currentThread()){Thread.currentThread().wait();}
					} catch (InterruptedException e) {
						LOG.error("Cannot stop thread!", e);
						e.printStackTrace();
					}
				}
			}
		}
	}*/
}
