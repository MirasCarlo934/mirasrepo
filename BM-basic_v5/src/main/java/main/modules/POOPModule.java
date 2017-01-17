package main.modules;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import cir.ArgOperator;
import cir.Argument;
import cir.ExecutionBlock;
import cir.Statement;
import components.Component;
import components.properties.Property;
import json.objects.ReqPOOP;
import json.objects.ReqRequest;
import json.objects.ResError;
import json.objects.ResPOOP;
import main.ComponentRepository;
import main.engines.CIREngine;
import main.engines.DBEngine;
import main.engines.OHEngine;
import main.engines.requests.CIREngine.GetStatementsCIREReq;
import main.engines.requests.CIREngine.UpdateCIREReq;
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
	private IDGenerator idg = new IDGenerator();

	public POOPModule(String RTY, String propIDParam, String propValParam, MQTTHandler mh, 
			OHEngine ohe, ComponentRepository cr, DBEngine dbe, CIREngine cire) {
		super("POOPModule", RTY, new String[]{propIDParam, propValParam}, mh, cr);
		this.dbe = dbe;
		this.cire = cire;
		this.propIDParam = propIDParam;
		this.propValParam = propValParam;
	}

	@Override
	protected void process(ReqRequest request) {
		ReqPOOP poop = new ReqPOOP(request, propIDParam, propValParam);
		Component c = cr.getComponent(poop.cid);
		Property p = c.getProperty(poop.propSSID);
		
		//check if propValue is a percentage (denoted by a % symbol)
		//System.out.println("HEY" + poop.propValueStr);
		if(poop.propValueStr.contains("%")) {
			poop.propValue = Math.abs(poop.propValue) * (p.getMax() / 100);
		}
		
		LOG.info("Changing component " + request.cid + " property " 
				+ poop.propSSID + " to " + poop.propValue + "...");
		if(c.getProperty(poop.propSSID).getValue() == poop.propValue) {
			LOG.info("Property is already set at " + poop.propValue);
		}
		else {
			//cire.update();
			cire.forwardRequest(new UpdateCIREReq(idg.generateMixedCharID(10)));
			updateSystem(poop);
			updateDatabase(poop);
			mh.publish(new ResPOOP(request, poop.propSSID, poop.propValue));
			mh.publish("openhab/" + c.getTopic(), poop.propSSID + "_" + poop.propValue);
		}
		LOG.info("POOP processing complete!");
	}
	
	/**
	 * Updates the system of the property change of the requesting component and also the property
	 * changes of all the affected components according to CIR.
	 * 
	 * @param poop
	 */
	private void updateSystem(ReqPOOP poop) {
		LOG.debug("Updating component property in system...");
		Component c = cr.getComponent(poop.cid);
		c.setPropertyValue(poop.propSSID, poop.propValue);
		
		LOG.debug("Updating affected component properties in system...");
		//LOG.fatal(poop.propIndex);
		//Vector<Statement> rules = cire.getCIRStatementsWithArgComponent(c, c.getProperty(poop.propIndex));
		Object o = cire.forwardRequest(new GetStatementsCIREReq(idg.generateMixedCharID(10), 
				c, c.getProperty(poop.propSSID)));
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
			for(int j = 0; j < args.length; j++) {
				Argument arg = args[j];
				//Component arg_com = cr.getComponent(arg.getComID());
				//ArgOperator operator = arg.getOperator();
				boolean rule_result = false;
				String pval = String.valueOf(c.getProperty(arg.getPropName()).getValue());
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
				
				if(rule_result) {
					//System.out.println(true);
					for(int k = 0; k < execs.length; k++) {
						ExecutionBlock exec = execs[k];
						Component com = cr.getComponent(exec.getComID());
						com.setPropertyValue(exec.getPropName(), Integer.parseInt(exec.getPropValue()));
						//updates the physical component
						mh.publish(com.getTopic(), new ResPOOP(poop.rid, com.getSSID(), poop.rty,
								exec.getPropName(), Integer.parseInt(exec.getPropValue())));
						//updates OpenHAB component item
						mh.publish("openhab/" + com.getTopic(), exec.getPropName() + "_"
								+ exec.getPropValue());
					}
				}					
			}
		}
	}
	
	private void updateDatabase(ReqPOOP poop) {
		LOG.debug("Updating component property in DB...");
		HashMap<String, Object> args1 = new HashMap<String, Object>(2);
		args1.put("com_id", poop.cid);
		args1.put("cpl_ssid", poop.propSSID);
		HashMap<String, Object> vals1 = new HashMap<String, Object>(1);
		vals1.put("prop_value", String.valueOf(poop.propValue));
		dbe.forwardRequest(new UpdateDBEReq(idg.generateMixedCharID(10), propsTable, vals1, args1));
		/*try {
			LOG.debug("Updating component property in DB...");
			//dbe.updateQuery(propsTable, args, vals);
		} catch (SQLException e) {
			LOG.error("Cannot update component property in DB!");
			e.printStackTrace();
		}*/
		
		LOG.debug("Updating affected component properties in DB...");
		Component c = cr.getComponent(poop.cid);
		//Vector<Statement> rules = cire.getCIRStatementsWithArgComponent(c, c.getProperty(poop.propIndex));
		Object o = cire.forwardRequest(new GetStatementsCIREReq(idg.generateMixedCharID(10), 
				c, c.getProperty(poop.propSSID)));
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
			for(int j = 0; j < args.length; j++) {
				Argument arg = args[j];
				//Component arg_com = cr.getComponent(arg.getComID());
				//ArgOperator operator = arg.getOperator();
				boolean rule_result = false;
				String pval = String.valueOf(c.getProperty(arg.getPropName()).getValue());
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
								args2));
					}
				}
			}
		}
	}

	/**
	 * Checks if request follows the ff:
	 * <ul>
	 * 	<li>Component with CID has the specified property</li>
	 * 	<li>Value specified is valid for the property</li>
	 * </ul>
	 */
	@Override
	protected boolean additionalRequestChecking(ReqRequest request) {
		boolean b = false;
		ReqPOOP poop = new ReqPOOP(request.getJSON(), propIDParam, propValParam);
		
		Component c = cr.getComponent(poop.cid);
		if(c.getProperty(poop.propSSID) != null) {
			Property p = c.getProperty(poop.propSSID);
			if(p.getMin() <= poop.propValue && p.getMax() >= poop.propValue) {
				b = true;
			}
			else {
				ResError re = new ResError(poop, "Property value not within min/max of the property!");
				error(re);
			}
		}
		else {
			error(new ResError(poop, "Property does not exist!"));
		}
		return b;
	}

	public void setPropsTable(String s) {
		this.propsTable = s;
	}
}
