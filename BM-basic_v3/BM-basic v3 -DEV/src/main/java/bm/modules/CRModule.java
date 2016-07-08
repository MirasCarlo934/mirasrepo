package bm.modules;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import bm.ComponentRepository;
import bm.OpenhabHandlingEngine;
import bm.TransactionEngine;
import bm.modules.parents.*;
import main.TransTechSystem;
import main.components.Component;
import main.components.openhab.OpenhabItem;
import main.components.properties.Property;
import main.components.properties.PropertyMode;
import main.objects.DB.DBObject;
import main.objects.request_response.BMRequest;
import main.objects.request_response.Command;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.InsertTransactionRequest;
import main.objects.request_response.InstructionResponse;
import main.objects.request_response.Request;
import main.objects.request_response.Response;
import main.objects.request_response.TransactionRequest;
import main.objects.request_response.TransactionResponse;
import mqtt.MQTTHandler;
import tools.IDGenerator;
import tools.StringTools;
import tools.TrafficController;

public class CRModule extends TransactionModule {
	private static final Logger logger = Logger.getLogger(CRModule.class);
	private ComponentRepository repository;
	private IDGenerator idGenerator; //instantiated in Spring boot with max char length of 3
	private OpenhabHandlingEngine ohe;
	
	/*
	 * The following are for easier value manipulation in case they need to be changed.
	 */
	private static final String ssid = TransTechSystem.config.getDatabaseConfig().getSsidColName();
	private static final String func_obj_type = "function";
	private static final String request_type ="register_com";
	private static final String function_req_param = "product";
	private static final String description_req_param = "name";
	private static final String room_req_param = "room";
	private static final String[] request_params = {function_req_param, description_req_param, room_req_param};
	private static final String esp_com_type = "ESP";
	private static final String bm_com_type = "BM";
	private static final String obj_type_request_param = "obj_type";
	private static final String response_topic = TransTechSystem.config.getMqttTopicConfig().getDefaultTopic(); //topic where response will be published to
	/*
	 * variables used in response parameter handling
	 */
	private static final String newCID_param_name = "" + TransTechSystem.config.getRequestParamConfig().getComponentIdKey();
	private static final String newMqttTopic_param_name = "mqtt_topic";
	/*
	 * variables used in persistence
	 */
	private static final String SSID_col_name = TransTechSystem.config.getDatabaseConfig().getSsidColName().toLowerCase();
	private static final String comType_col_name = "type";
	private static final String comMqttTopic_col_name = "topic";
	private static final String comMacAddress_col_name = "mac";
	private static final String comFunction_col_name = "functn";
	private static final String comRoomid_col_name = "room";
	private static final String comDescription_col_name = "name";
	private static final String component_obj_type = "component";
	private static final String propComid_col_name = "com_id";
	private static final String propPropname_col_name = "prop_name";
	private static final String propPropval_col_name = "prop_value";
	private static final String prop_obj_type = "com_prop";
	private static final String room_obj_type = "room";
	private static final String room_name_col = "name";
	private static final String system_room_id = "A000";
	private static final String system_function_id = "0000";
	
	private static final String cplist_table = "comproplist";
	private static final String cplist_comtype = "com_type";
	private static final String cplist_proptype = "prop_type";
	private static final String cplist_qty = "qty";
	
	private static final String pcat_table = "";
	private static final String pcat_name = "name";
	private static final String pcat_desc = "description";
	private static final String pcat_mode = "mode";
	private static final String pcat_type = "type";
	/*
	 * function id's
	 */
	private static final String switch_funct_id = "0003";
	private static final String physswitch_funct_id = "0002";

	public CRModule(MQTTHandler mqttHandler, TransactionEngine transactionEngine, ComponentRepository repository, 
			IDGenerator idGenerator, OpenhabHandlingEngine openhabHandlingEngine) {
		super("BM/CRModule", request_type, request_params, mqttHandler, transactionEngine);
		this.repository = repository;
		this.idGenerator = idGenerator;
		ohe = openhabHandlingEngine;
	}

	@Override
	public void process(Request request) throws Exception {
		logger.info("Registering new component to system. Checking for registry parameter errors...");
		String request_id = request.getRequestID(); //contains the MAC address of the registering component
		String function = (String) request.get(function_req_param);
		String name = (String) request.get(description_req_param);
		String room = request.getString(room_req_param);
		//String id = ""; //used for generating ID of Component
		
		/*
		 * checks validity of com_type, and functionid
		 */
		logger.info("Checking validity of com_type, room_id, and function_id...");
		Component c = null;
		boolean b = false; //true if com_type, roomid, and functionid exists
		
		HashMap<String, Object> func_id_args = new HashMap<String, Object>(1);
		func_id_args.put(TransTechSystem.config.getDatabaseConfig().getSsidColName(), function);
		TransactionRequest func_id_treq = new TransactionRequest(func_obj_type, func_id_args);
		TransactionResponse func_id_tres = getTransactionEngine().selectSpecific(func_id_treq);
		
		if(!func_id_tres.getObjects().isEmpty()) { //true if func_id exists
			b = true;
		} else { //error 3: Function does not exist!
			logger.error("Error! Function does not exist!");
			ErrorResponse r = new ErrorResponse(request_id,  request.getRequestType(), response_topic, "Function does not exist");
			getMqttHandler().publish(response_topic, r.toJSONObject().toString());
			b = false;
		}
		
		if(b) {
			logger.info("Checking complete! Registering component...");
			/*
			 * checks if component is already registered
			 */
			HashMap<String, Object> args = new HashMap<String, Object>(1);
			args.put(comMacAddress_col_name, request_id);
			TransactionRequest treq = new TransactionRequest(component_obj_type, args);
			TransactionResponse tres = getTransactionEngine().selectSpecific(treq);
			
			if(tres.getObjects().isEmpty()) { //registration success!!!!!!				
				/*
				 * persists new component to database
				 */
				c = repository.registerComponent(request_id, name, function, room);
				
				if(c != null) { //true if registration was successful					
					/*
					 * assembles and sends CR response
					 */
					Response response = new Response(request_id, request.getRequestType(), TransTechSystem.config.getMqttTopicConfig().getDefaultTopic());
					response.put(newCID_param_name, c.getId()); 				//issues new component_id
					response.put(newMqttTopic_param_name, c.getMqttTopic());	//issues new mqtt topic
					getMqttHandler().publish(response.getTopic(), response.toJSONObject().toString());
					
					logger.info("Component now integrated into system!");					
				} else { //error 3: Error in insertion to DB
					logger.error("Error! Error in insertion to DB");
					//ErrorResponse r = new ErrorResponse(request_id, response_topic, getTransactionEngine().getError());
					ErrorResponse r = new ErrorResponse(request_id, request.getRequestType(), response_topic, "DB Error! Some of the params you entered may be invalid!");
					getMqttHandler().publish(response_topic, r.toJSONObject().toString());
				}
				
			} else { //error 2: Component already registered in system!
				logger.warn("MAC:" + request_id + " already registered in system! Discontinuing registration process...");
				logger.info("Returning existing component credentials for MAC:" + request_id);
				//ErrorResponse r = new ErrorResponse(request_id, response_topic, "Component already registered in system");
				DBObject dbo = tres.getObjects().get(0);
				String cid = (String) dbo.get(SSID_col_name);
				String t = (String) dbo.get(comMqttTopic_col_name);
				Component com = repository.getComponent(cid);
				
				Response r = new Response(request_id, request.getRequestType(), TransTechSystem.config.getMqttTopicConfig().getDefaultTopic());
				r.put(newCID_param_name, cid); 						//issues new component_id
				r.put(newMqttTopic_param_name, t);
				getMqttHandler().publish(response_topic, r.toJSONObject().toString());
				
				//updates the properties of the component by sending instruction responses
				logger.info("Updating component's properties. Sending instruction/s...");
				Property[] com_props = com.getProperties();
				for(int i = 0; i < com_props.length; i++) {
					Property prop = com_props[i];
					InstructionResponse ir = new InstructionResponse(com, "prop_change", prop.getSystemName(), 
							String.valueOf(prop.getValue()));
					getMqttHandler().publish(ir);
				}
			}
			
			/*
			 * updates ComponentRepository records
			 */
			repository.update();
			
			/*
			 * updates openhab items
			 */
			ohe.updateOpenhabRecords(repository.getAllRooms(), repository.getAllComponents());
			
		} else { //error 1: invalid com_type
			logger.error("Error! Invalid component type! Component registration failed");
			JSONObject response = new JSONObject();
			response.put(TransTechSystem.config.getRequestParamConfig().getRequestIdKey(), request_id);
			response.put(TransTechSystem.config.getRequestParamConfig().getErrorKey(), "invalid:::com_id");
			getMqttHandler().publish(response_topic, response.toString());
		}
	}
}
