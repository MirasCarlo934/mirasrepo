package bm.modules;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import org.eclipse.paho.client.mqttv3.MqttException;

import bm.CIRInterpreter;
import bm.ComponentRepository;
import bm.OpenhabHandlingEngine;
import bm.TTTIEngine;
import bm.TransactionEngine;
import bm.modules.parents.Module;
import bm.objects.cir.ArgOperator;
import bm.objects.cir.Argument;
import bm.objects.cir.CodeBlock;
import bm.objects.cir.Relationship;
import bm.objects.cir.Statement;
import bm.objects.cir.exceptions.CIRSSyntaxException;
import main.TransTechSystem;
import main.components.Component;
import main.components.ComponentPropertyList;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.InsertTransactionRequest;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import mqtt.MQTTHandler;
import tools.StringTools;

public class InstructionModule extends Module {
	/*
	 * Request param declarations
	 */
	private static final String request_type = "prop_change";
	private static final String prop_req_param = "property"; //the property that changed in the requesting component
	private static final String propval_req_param = "value"; //the value needed to perform the action
	private static final String[] request_params = {prop_req_param, propval_req_param};
	/*
	 * List of valid properties
	 */
	private static final ComponentPropertyList properties =  
			TransTechSystem.configuration.getInstructionPropsConfig().getComponentPropertyList();
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
	private CIRInterpreter cir;
	private TransactionEngine te;
	private TTTIEngine tttie;
	private OpenhabHandlingEngine ohe;
	
	public InstructionModule(MQTTHandler mqttHandler, TransactionEngine transactionEngine, ComponentRepository componentRepository, 
			CIRInterpreter cirInterpreter, TTTIEngine tttiEngine, OpenhabHandlingEngine openhabHandlingEngine) {
		super("BM/InstructionModule", request_type, request_params, mqttHandler);
		cr = componentRepository;
		cir = cirInterpreter;
		te = transactionEngine;
		tttie = tttiEngine;
		ohe = openhabHandlingEngine;
	}

	@Override
	protected void process(Request request) throws Exception {
		logger.info("Processing instruction...");
		String com_id = request.getComponentID();
		String prop_name = request.getString(prop_req_param);
		Object prop_val = request.get(propval_req_param);
		if(checkRequest(request)) {
			//true if component's registered property is different from the new property
			if(!cr.getComponent(com_id).getProperty(prop_name).equals(prop_val)) { 
				logger.trace("Updating property '" + prop_name + "' of component " + com_id + "...");
				/*
				 * Updates the property of the component in database
				 */
				HashMap<String, Object> vals = new HashMap<String, Object>(1);
				vals.put(propval_col_name, prop_val);
				HashMap<String, Object> args = new HashMap<String, Object>(1);
				args.put(comid_col_name, com_id);
				args.put(propname_col_name, prop_name);
				InsertTransactionRequest treq = new InsertTransactionRequest(request, prop_obj_type, args, vals);
				te.update(treq);
				/*
				 * Updates the property of the component in repository
				 */
				cr.getComponent(com_id).setProperty(prop_name, prop_val);
				
				/*
				 * Instructs components based on CIR
				 */
				updateComponentsProperties(request);
				
				/*
				 * Updates all components in CR
				 */
				cr.update();
				
				/*
				 * Updates all items properties in openhab
				 */
				//ohe.updateOpenhabRecords();
				ohe.updateItemProperties();
				
				logger.trace("Property update successful!");
			}
			else {
				logger.trace("Component " + com_id + " property '" + prop_name + "' is already set to '" + prop_val + "'");
			}
			
			/*
			 * Publishes success to requesting component
			 * Code is only reached if no error has been made
			 */
			Response res = new Response(request.getRequestID(), request.getTopic());
			res.put(success_res_param, true);
			getMqttHandler().publish(res);
			logger.info("Instruction processing complete!");
		}
	}
	
	/**
	 * Checks if rule specified may apply on the component's property change
	 * 
	 * @param rules The Vector containing all the CIR statements from the CIR file
	 * @throws SQLException 
	 * @throws MqttException 
	 */
	private void updateComponentsProperties(Request request) throws MqttException, SQLException {
		Vector<Statement> rules = null;
		try {
			rules = cir.interpret(TransTechSystem.configuration.getInstructionPropsConfig().getRulesFileLocation());
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
				String com_id = arg.getComID();
				String prop_name = arg.getComProperty();
				String prop_val = arg.getPropValue();
				Component com = cr.getComponent(com_id);
				boolean rule_result = false; //true if the argument condition is met
				
				if(com.hasProperty(prop_name)) { //true if component contains the property specified
					if(arg.getOperator().equals(ArgOperator.EQUALS)) {
						if(com.getProperty(prop_name).equals(prop_val)) {
							rule_result = true;
							//applyRules(request, rule.getExecBlocks());
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
							//handleError(request, "Property is not a number!");
						}
					}
				} 
				else { //error: Invalid component property! Recheck CIR file for error!
					handleError(request, "Invalid component property! Recheck CIR file for error!");
				}
				//logger.debug(rule_result);
				rule_results[j] = rule_result;
			}
			/*
			 * Determines if execution block will be executed
			 */
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
	}
	
	private void applyRules(Request request, CodeBlock[] execs) throws MqttException, SQLException {
		logger.info("A rule is found that will apply to this property change...");
		for(int i = 0; i < execs.length; i++) {
			CodeBlock exec = execs[i];
			tttie.instruct(exec.getComID(), exec.getComProperty(), exec.getPropValue());
		}
	}
	
	/**
	 * Checks if request supplied a valid prop_name and prop_val.
	 * @param request
	 * @return
	 */
	private boolean checkRequest(Request request) {
		String prop_name = request.getString(prop_req_param);
		Object prop_val = request.get(propval_req_param);
		boolean b = true; //true if request checks out okay
		String error = null; //contains errors in request if there are any
		
		/*
		 * Checks if prop_name and prop_val are valid properties
		 */
		if(properties.containsKey(prop_name)) {
			Object[] prop_vals = properties.get(prop_name);
			for(int i = 0; i < prop_vals.length; i++) {
				if(prop_val.equals(prop_vals[i])) {
					b = true;
					break;
				}
				else { //error 2: Property value does not exist
					error = ("Invalid property value");
					b = false;
				}
			}
		}
		else { //error 1: Property does not exist
			error = ("Invalid property");
			b = false;
		}
		
		if(!b) {
			handleError(request, error);
		}
		return b;
	}
	
	private void handleError(Request request, String error) {
		logger.error(error + "!");
		ErrorResponse e = new ErrorResponse(request.getRequestID(), request.getTopic(), error);
		try {
			getMqttHandler().publish(e);
		} catch (MqttException e1) {
			logger.error("Cannot publish error message!", e1);
		}
	}
}
