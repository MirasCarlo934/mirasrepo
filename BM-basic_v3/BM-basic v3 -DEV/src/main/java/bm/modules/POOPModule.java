package bm.modules;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import bm.CIREngine;
import bm.ComponentRepository;
import bm.OpenhabHandlingEngine;
import bm.TransactionEngine;
import bm.modules.parents.Module;
import bm.objects.cir.ArgOperator;
import bm.objects.cir.Argument;
import bm.objects.cir.CodeBlock;
import bm.objects.cir.Conditional;
import bm.objects.cir.ExecutionBlock;
import bm.objects.cir.Relationship;
import bm.objects.cir.Statement;
import bm.objects.cir.exceptions.CIRSSyntaxException;
import main.TransTechSystem;
import main.components.Component;
import main.components.properties.Property;
import main.configuration.ComponentPropertyList;
import main.objects.request_response.BMRequest;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.InsertTransactionRequest;
import main.objects.request_response.InstructionResponse;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import mqtt.MQTTHandler;
import tools.StringTools;

public class POOPModule extends Module {
	private static final Logger logger = Logger.getLogger(POOPModule.class);
	
	/*
	 * Request param declarations
	 */
	public static final String request_type = "prop_change";
	private static final String prop_req_param = "property"; //the property that changed in the requesting component
	private static final String propval_req_param = "value"; //the value needed to perform the action
	private static final String[] request_params = {prop_req_param, propval_req_param};
	/*
	 * Response param declarations
	 */
	private static final String inst_response_id = "inst"; //states that the response is an instruction
	private static final String success_res_param = "success"; //true if instruction delivery is successful
	/*
	 * comp_properties table colnames
	 */
	private static final String comid_col_name = "com_id";
	private static final String propname_col_name = "prop_name";
	private static final String propval_col_name = "prop_value";
	private static final String prop_obj_type = "com_prop";
	
	private ComponentRepository cr;
	private CIREngine cir;
	private TransactionEngine te;
	private OpenhabHandlingEngine ohe;
	
	private Request request; //changes with each request to this Module
	private int index = 0; //counter for all the requests processed by this module
	
	public POOPModule(MQTTHandler mqttHandler, TransactionEngine transactionEngine, ComponentRepository componentRepository, 
			CIREngine cirInterpreter, OpenhabHandlingEngine openhabHandlingEngine) {
		super("BM/POOPModule", request_type, request_params, mqttHandler);
		cr = componentRepository;
		cir = cirInterpreter;
		te = transactionEngine;
		ohe = openhabHandlingEngine;
	}

	@Override
	protected void process(Request request) throws Exception {
		logger.info("Processing instruction... " + index);
		this.request = request;
		String com_id = request.getComponentID();
		String prop_name = request.getString(prop_req_param);
		Object prop_val = request.get(propval_req_param);
		Component com = cr.getComponent(com_id);
		
		int val = parsePOOPRequestValue(com, request);
		if(val != -1) { //checkpoint 1
			prop_val = String.valueOf(val);
			if(checkRequest(request, com)) { //checkpoint 2
				if(cr.getComponent(com_id).getProperty(prop_name).getValue() != Integer.parseInt((String)prop_val)) { //checkpoint 3
					//true if component's registered property value is different from the new property value
					logger.trace("Updating property '" + prop_name + "' of component " + com_id + "...");
					
					//Instructs same component for instance when openhab sends the request
					instruct(cr.getComponent(com_id), request.getRequestType(), prop_name, (String)prop_val);
					
					//Updates the property of the component in database
					HashMap<String, Object> vals = new HashMap<String, Object>(1);
					vals.put(propval_col_name, prop_val);
					HashMap<String, Object> args = new HashMap<String, Object>(1);
					args.put(comid_col_name, com_id);
					args.put(propname_col_name, prop_name);
					InsertTransactionRequest treq = new InsertTransactionRequest(prop_obj_type, args, vals);
					te.update(treq);
					
					//Updates the property of the component in repository
					cr.getComponent(com_id).setPropertyValue(prop_name, prop_val);
					
					//Instructs components based on CIR
					applyRules(com_id, prop_name, (String) prop_val);
					
					//Updates all components in CR
					cr.update();
					
					logger.trace("Property update successful!");
				}
				else {
					logger.info("Component " + com_id + " property '" + prop_name + "' is already set to '" + prop_val + "'");
					//instruct(cr.getComponent(com_id), request.getRequestType(), prop_name, (String)prop_val);
				}
			}			
			logger.info("Instruction processing complete! " + index);
		}
		index++;
	}
	
	/**
	 * Parses the 'value' field of the POOP request to determine whether the value sent is an increment/decrement 
	 * or a direct value change.
	 * 
	 * @param com The Component that undergone the value change
	 * @param request The POOP request
	 * @return the new value of the property, -1 if the value field is not constructed correctly, or the max/min value
	 * 		if the increment/decrement exceeds the max/min limit of the property
	 * @throws MqttException 
	 */
	private int parsePOOPRequestValue(Component com, Request request) throws MqttException {
		int val = -1;
		String pname = request.getString(prop_req_param);
		String pval = (String)request.get(propval_req_param);
		Property prop = com.getProperty(pname);
		//logger.debug(prop.getName() + " - " + prop.getValue());
		//if(pval.startsWith("+") || pval.startsWith("-")) {
		if(!Character.isDigit(pval.charAt(0))) {
			try {
				if(pval.startsWith("+"))
					val = prop.getValue() + Integer.parseInt(pval.substring(1));
				else if(pval.startsWith("-"))
					val = prop.getValue() - Integer.parseInt(pval.substring(1));
				else if(pval.startsWith("%")) {
					val = Math.round(((float)prop.getMaxval() / 100) * Integer.parseInt(pval.substring(1)));
				}
				else 
					throw new NumberFormatException();
				
				if(val < prop.getMinval() || val > prop.getMaxval()) {
					if(val < prop.getMinval()) {
						logger.warn("Decrement is less than the minimum capacity of the property " + pname + ". Returning"
								+ " the value of the minimum capacity.");
						val = prop.getMinval();
					}
					else {
						logger.warn("Decrement is exceeds the maximum capacity of the property " + pname + ". Returning"
								+ " the value of the maximum capacity.");
						val = prop.getMaxval();
					}
				} else {
					//successful parsing goes through here
					return val;
				}
			} catch(NumberFormatException e) {
				logger.error("Field 'value' contains an invalid value! (b)");
				ErrorResponse er = new ErrorResponse(request.getRequestID(), request.getRequestType(), request.getTopic(), 
						"Field 'value' contains an invalid value!");
				getMqttHandler().publish(er);
			}
		} else {
			try {
				val = Integer.parseInt(pval);
			} catch(NumberFormatException e) {
				logger.error("Field 'value' contains an invalid value! (a)", e);
				ErrorResponse er = new ErrorResponse(request.getRequestID(), request.getRequestType(), request.getTopic(), 
						"Field 'value' contains an invalid value!");
				getMqttHandler().publish(er);
			}
		}
		
		return val;
	}
	
	/**
	 * Sends an Instruction Response to the specified component. This method also sends the instruction to the openhab
	 * topic to instruct the OpenHab representation of the component to act accordingly.
	 * 
	 * @param com the Component which will receive the instruction
	 * @param prop_name the name of the property to be changed
	 * @param prop_val the value of the property to be changed
	 * @throws MqttException
	 */
	private void instruct(Component com, String RTY, String prop_name, String prop_val) 
			throws MqttException {
		logger.info("Sending instruction to component with ID:" + com.getId() + "...");
		InstructionResponse r = null;
		r = new InstructionResponse(com, RTY, prop_name, prop_val);
		
		//publishes instruction to Component's topic
		getMqttHandler().publish(r);	
		
		//sends instruction to OpenHab
		ohe.receiveInstruction(r);
	}
	
	/**
	 * Applies the CIR rules based on the given instance of property change.
	 * 
	 * @param pooper the Component that undergone the property change.
	 * @param pname the name of the property that changed
	 * @param pval the value of the property that changed
	 */
	private void applyRules(String pooper, String pname, String pval) {
		logger.info("Checking for rules that may apply... " + index);
		Vector<Statement> rules = cir.getCirStatements();
		for(int i = 0; i < rules.size(); i++) {
			Statement rule = rules.get(i);
			Argument[] args = rule.getArguments();
			ExecutionBlock[] execs = rule.getExecBlocks();
			
			if(rule.getCondition().equals(Conditional.IF)) {
				boolean rule_result = false; //true if the rule will apply
				for(int j = 0; j < args.length; j++) {
					Argument arg = args[j];
					if(arg.getComID().equals(pooper) && arg.getComProperty().equals(pname)) {
						if(arg.getOperator().equals(ArgOperator.EQUALS)) {
							if(arg.getPropValue().equals(pval)) {
								rule_result = true;
							}
						}
						else if(arg.getOperator().equals(ArgOperator.GREATER)) {
							try {
								if(Float.parseFloat(arg.getPropValue()) > Float.parseFloat(pval)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) {
								handleError(request, "Property is not a number!");
							}
						}
						else if(arg.getOperator().equals(ArgOperator.LESS)) {
							try {
								if(Float.parseFloat(arg.getPropValue()) < Float.parseFloat(pval)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) {
								handleError(request, "Property is not a number!");
							}
						}
						else if(arg.getOperator().equals(ArgOperator.GREATEREQUALS)) {
							try {
								if(Float.parseFloat(arg.getPropValue()) >= Float.parseFloat(pval)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) {
								handleError(request, "Property is not a number!");
							}
						}
						else if(arg.getOperator().equals(ArgOperator.LESSEQUALS)) {
							try {
								if(Float.parseFloat(arg.getPropValue()) <= Float.parseFloat(pval)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) {
								handleError(request, "Property is not a number!");
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
					}
				}
				//logger.debug(rule_result);
				if(rule_result) {
					logger.info("An applicable rule is found!");
					for(int j = 0; j < execs.length; j++) {
						ExecutionBlock exec = execs[j];
						
						//instructs the affected components
						try {
							instruct(cr.getComponent(exec.getComID()), request.getRequestType(), exec.getComProperty(), exec.getPropValue());
						} catch (MqttException e) {
							logger.error("Cannot instruct rule-affected component due to an MQTT Exception!", e);
						}
						
						//updates the property of the affected components in the database
						logger.trace("Updating properties of affected components in DB");
						HashMap<String, Object> arg0 = new HashMap<String, Object>(1,1);
						arg0.put(comid_col_name, exec.getComID());
						arg0.put(propname_col_name, pname);
						HashMap<String, Object> arg1 = new HashMap<String, Object>(1,1);
						arg1.put(propval_col_name, pval);
						InsertTransactionRequest intreq = new InsertTransactionRequest(prop_obj_type, arg0, arg1);
						try {
							te.update(intreq);
						} catch (SQLException e) {
							logger.error("Cannot update component property due to an SQL Exception!", e);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Checks if rule specified may apply on the component's property change
	 * 
	 * @param com_id the ID of the component that changed its property
	 * @throws SQLException 
	 * @throws MqttException 
	 */
	/*private void updateComponentsProperties(Request request, String com_id) throws MqttException, SQLException {
		Vector<Statement> rules = null;
		try {
			rules = cir.getCirStatements();
		} catch(Exception e) {
			if(e.getClass().equals(CIRSSyntaxException.class)) {
				handleError(request, "Syntax error in CIR file");
			} else {
				handleError(request, "Error in reading CIR file");
			}
			logger.error(e);
		} 
		
		for(int i = 0; i < rules.size(); i++) {
			Statement rule = rules.get(i);
			boolean[] rule_results = new boolean[rule.getArguments().length];
			
			for(int j = 0; j < rule.getArguments().length; j++) {
				Argument arg = rule.getArguments()[j];
				String c_id = arg.getComID();
				String prop_name = arg.getComProperty();
				String prop_val = arg.getPropValue();
				Component com = cr.getComponent(c_id);
				boolean rule_result = false; //true if the argument condition is met
				
				if(com_id.equals(c_id)) {
					if(com.hasProperty(prop_name)) { //true if component contains the property specified
						if(arg.getOperator().equals(ArgOperator.EQUALS)) {
							if(com.getProperty(prop_name).equals(prop_val)) {
								rule_result = true;
							}
						}
						else if(arg.getOperator().equals(ArgOperator.GREATER)) {
							try {
								if(Float.parseFloat((String) com.getProperty(prop_name)) > Float.parseFloat((String)prop_val)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) {
								handleError(request, "Property is not a number!");
							}
						}
						else if(arg.getOperator().equals(ArgOperator.LESS)) {
							try {
								if(Float.parseFloat((String) com.getProperty(prop_name)) < Float.parseFloat((String)prop_val)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) {
								handleError(request, "Property is not a number!");
							}
						}
						else if(arg.getOperator().equals(ArgOperator.GREATEREQUALS)) {
							try {
								if(Float.parseFloat((String) com.getProperty(prop_name)) >= Float.parseFloat((String)prop_val)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) {
								handleError(request, "Property is not a number!");
							}
						}
						else if(arg.getOperator().equals(ArgOperator.LESSEQUALS)) {
							try {
								if(Float.parseFloat((String) com.getProperty(prop_name)) <= Float.parseFloat((String)prop_val)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) {
								handleError(request, "Property is not a number!");
							}
						}
						else if(arg.getOperator().equals(ArgOperator.INEQUAL)) {
							try {
								if(Float.parseFloat((String) com.getProperty(prop_name)) != Float.parseFloat((String)prop_val)) {
									rule_result = true;
								}
							} catch(NumberFormatException e) { //signifies that the property is a string
								if(!com.getProperty(prop_name).equals(prop_val)) {
									rule_result = true;
								}
							}
						}
					} 
					else { //error: Invalid component property! Recheck CIR file for error!
						handleError(request, "Invalid component property! Recheck CIR file for error!");
					}
				}
				//logger.debug(rule_result);
				rule_results[j] = rule_result;
			}
			/*
			 * Determines if execution block will be executed
			 
			boolean prev = rule_results[0]; //result of the previous argument block, in this case the first
			boolean b = prev; //if b == true, applyRules()
			for(int j = 1; j < rule_results.length; j++) {
				boolean now = rule_results[j];
				Relationship r = rule.getArguments()[j - 1].getRelationshipWithNextArgument();
				if(r.equals(Relationship.AND)) {
					if(prev && now) {
						b = true;
					} else {
						b = false;
					}
				} else if(r.equals(Relationship.OR)) {
					if(prev || now) {
						b = true;
					} else {
						b = false;
					}
				} else { //true if r = NULL
					b = true;
				}
				prev = now;
			}
			
			if(b) {
				applyRules(request, rule.getExecBlocks());
			}
		}
	}*/
	
	/*private void applyRules(Request request, CodeBlock[] execs) throws MqttException, SQLException {
		logger.info("A rule is found that will apply to this property change...");
		for(int i = 0; i < execs.length; i++) {
			CodeBlock exec = execs[i];
			tttie.instruct(exec.getComID(), exec.getComProperty(), exec.getPropValue());
		}
	}*/
	
	/**
	 * Checks if request supplied a valid prop_name. The prop_val checking is left for the parsePOOPRequestValue()
	 * @param request
	 * @return
	 */
	private boolean checkRequest(Request request, Component com) {
		String prop_name = request.getString(prop_req_param);
		//int prop_val = Integer.parseInt((String)request.get(propval_req_param));
		boolean b = true; //true if request checks out okay
		String error = null; //contains errors in request if there are any
		
		Vector<String> proplist = new Vector<String>(1,1);
		for(int i = 0; i < com.getProperties().length; i++) {
			proplist.add(com.getProperties()[i].getSystemName());
		}
		/*
		 * Checks if prop_name and prop_val are valid properties
		 */
		if(proplist.contains(prop_name)) {
			/*Property prop = com.getProperty(prop_name);
			int min = prop.getMinval();
			int max = prop.getMaxval();
			if(min <= prop_val && prop_val <= max) {
				b = true;
			}
			else { //error 2: Property value does not exist
				error = ("Invalid property value");
				b = false;
			}*/
		}
		else { //error 1: Property does not exist
			error = ("Invalid property name");
			b = false;
		}
		
		if(!b) {
			handleError(request, error);
		}
		return b;
	}
	
	private void handleError(Request request, String error) {
		logger.error(error + "!");
		ErrorResponse e = new ErrorResponse(request.getRequestID(), request.getRequestType(),request.getTopic(), error);
		try {
			getMqttHandler().publish(e);
		} catch (MqttException e1) {
			logger.error("Cannot publish error message!", e1);
		}
	}
}
