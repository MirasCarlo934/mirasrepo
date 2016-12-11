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
		logger.info("Registering new component to system...");
		String request_id = request.getRequestID(); //contains the MAC address of the registering component
		String function = (String) request.get(function_req_param);
		String name = (String) request.get(description_req_param);
		String room = request.getString(room_req_param);
		String id = ""; //used for generating ID of Component
		
		/*
		 * generates component ssid
		 */
		do{
			id = idGenerator.generateMixedCharID(TransTechSystem.config.getDatabaseConfig().getSsidLength());
		} while(repository.containsComponent(id));
		
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
			//creates component with the generated mqtttopic
			logger.info("Checking complete! Registering component...");
			
			//gets properties using cplist (IMPROVE THIS)
			HashMap<String, Object> arg0 = new HashMap<String, Object>(1);
			arg0.put("com_type", function);
			TransactionRequest treq0 = new TransactionRequest("cplist", arg0);
			TransactionResponse tres0 = getTransactionEngine().selectSpecific(treq0);
			Vector<DBObject> cplist = tres0.getObjects();		
			Vector<Property> com_props = new Vector<Property>(1,1);
			for(int i = 0; i < cplist.size(); i++) {
				DBObject list = cplist.get(i);
				String prop_id = (String) list.get("prop_type");
				int qty = (int)list.get("qty");
				
				for(int j = 0; j < qty; j++) {
					HashMap<String, Object> arg1 = new HashMap<String, Object>(1);
					arg1.put("ssid", prop_id);
					TransactionRequest treq1 = new TransactionRequest("property", arg1);
					TransactionResponse tres1 = getTransactionEngine().selectSpecific(treq1);
					DBObject prop = tres1.getObjects().get(0);
					
					String pname = (String)prop.get("name");
					PropertyMode pmode = PropertyMode.parseMode((String)prop.get("mode"));
					String ptype = (String)prop.get("type");
					
					HashMap<String, Object> arg2 = new HashMap<String, Object>(1);
					arg2.put("ssid", ptype);
					TransactionRequest treq2 = new TransactionRequest("pval", arg2);
					TransactionResponse tres2 = getTransactionEngine().selectSpecific(treq2);
					DBObject pvals = tres2.getObjects().get(0);
					
					int min = (int)pvals.get("minim");
					int max = (int)pvals.get("maxim");
					
					OpenhabItem ohitem = OpenhabItem.parseItem((String)pvals.get("oh_item"));
					com_props.add(new Property(prop_id, pname + "-" + ((int)j + (int)1), pmode, min, max, 0, ohitem));
				}
			}
			
			Property[] props = com_props.toArray(new Property[0]);
			c = new Component(id, name, function, room, esp_com_type + "_" + id + "_topic", request_id, props);
			b = true;
		} else { //error 5: Function does not exist!
			logger.error("Function does not exist!");
			ErrorResponse r = new ErrorResponse(request_id, response_topic, "Function does not exist");
			getMqttHandler().publish(response_topic, r.toJSONObject().toString());
			b = false;
		}
		
		if(b) {
			/*
			 * checks if component is already registered
			 */
			HashMap<String, Object> args = new HashMap<String, Object>(1);
			args.put(comMacAddress_col_name, request_id);
			TransactionRequest treq = new TransactionRequest(component_obj_type, args);
			TransactionResponse tres = getTransactionEngine().selectSpecific(treq);
			
			if(tres.getObjects().isEmpty()) { //registration success!!!!!!	
				/*
				 * Checks if room exists, if not, create new room
				 * Checks for both SSID and name
				 */
				HashMap<String, Object> a = new HashMap<String, Object>(1);
				a.put(SSID_col_name, c.getRoom());
				TransactionResponse rtres = getTransactionEngine().selectSpecific(
						new TransactionRequest(room_obj_type, a));
				HashMap<String, Object> a2 = new HashMap<String, Object>(1);
				a2.put(room_name_col, c.getRoom());
				TransactionResponse rtres2 = getTransactionEngine().selectSpecific(
						new TransactionRequest(room_obj_type, a2));
				if(rtres2.getObjects().isEmpty() && rtres.getObjects().isEmpty()) { //true if a room exists
					//create new room, assumes that the value put is the name of the new room
					HashMap<String, Object> new_room = new HashMap<String, Object>(1);
					new_room.put(room_name_col, c.getRoom());
					TransactionResponse tres2 = getTransactionEngine().insert(
							new InsertTransactionRequest(room_obj_type, new_room));
					c.setRoom((String)tres2.get(SSID_col_name));
				} else if(!rtres2.getObjects().isEmpty()){ //there is a room with the specified NAME
					//gets the ssid of the existing room
					c.setRoom((String)rtres2.getObjects().get(0).get(SSID_col_name));
				}
				
				/*
				 * persists new component to database
				 */
				HashMap<String, Object> vals = new HashMap<String, Object>(1);
				vals.put(TransTechSystem.config.getDatabaseConfig().getSsidColName().toLowerCase(), c.getId());
				vals.put(comMacAddress_col_name, c.getMacAddress());
				vals.put(comMqttTopic_col_name, c.getMqttTopic());
				vals.put(comFunction_col_name, c.getFunction());
				vals.put(comDescription_col_name, name);
				vals.put(comRoomid_col_name, c.getRoom());
				InsertTransactionRequest insert = new InsertTransactionRequest(component_obj_type, vals);
				TransactionResponse insert_res = getTransactionEngine().insert(insert);
				
				/*
				 * persists properties of new component to database
				 */
				boolean bb = true;
				Property[] com_props = c.getProperties();
				for(int i = 0; i < com_props.length; i++) {
					Property prop = com_props[i];
					String prop_name = prop.getName();
					Object prop_val = "0"; //is always 0 for newly created objects
					HashMap<String, Object> vals2 = new HashMap<String, Object>(1);
					vals2.put(propComid_col_name, c.getId());
					vals2.put(propPropname_col_name, prop_name);
					vals2.put(propPropval_col_name, prop_val);
					request.put(obj_type_request_param, prop_obj_type);
					InsertTransactionRequest insert2 = new InsertTransactionRequest(request, vals2);
					TransactionResponse insert2_res = getTransactionEngine().insert(insert2); 
					if(!insert2_res.isSuccessful()) {
						bb = false;
						break;
					}
				}
				
				if(insert_res.isSuccessful() && bb) {
					c.setId((String) insert_res.get(SSID_col_name.toLowerCase()));
					c.setMqttTopic(esp_com_type + "_" + c.getId() + "_topic");
					
					/*
					 * assembles and sends CR response
					 */
					Response response = new Response(request_id, TransTechSystem.config.getMqttTopicConfig().getDefaultTopic());
					response.put(newCID_param_name, c.getId()); 				//issues new component_id
					response.put(newMqttTopic_param_name, c.getMqttTopic());	//issues new mqtt topic
					getMqttHandler().publish(response.getTopic(), response.toJSONObject().toString());
					
					/*
					 * updates ComponentRepository records
					 */
					repository.update();
					
					/*
					 * updates openhab items
					 */
					ohe.updateOpenhabRecords(repository.getAllRooms(), repository.getAllComponents());
					logger.info("Component now integrated into system!");					
				} else { //error 3: Error in insertion to DB
					logger.error("Error in insertion to DB");
					//ErrorResponse r = new ErrorResponse(request_id, response_topic, getTransactionEngine().getError());
					ErrorResponse r = new ErrorResponse(request_id, response_topic, "DB Error! Some of the params you entered may be invalid!");
					getMqttHandler().publish(response_topic, r.toJSONObject().toString());
				}
				
			} else { //error 2: Component already registered in system!
				logger.warn("MAC:" + request_id + " already registered in system! Discontinuing registration process...");
				logger.info("Returning existing component credentials for MAC:" + request_id);
				//ErrorResponse r = new ErrorResponse(request_id, response_topic, "Component already registered in system");
				DBObject dbo = tres.getObjects().get(0);
				String cid = (String) dbo.get(SSID_col_name);
				String t = (String) dbo.get(comMqttTopic_col_name);
				Response r = new Response(request_id, TransTechSystem.config.getMqttTopicConfig().getDefaultTopic());
				r.put(newCID_param_name, cid); 						//issues new component_id
				r.put(newMqttTopic_param_name, t);
				getMqttHandler().publish(response_topic, r.toJSONObject().toString());
			}
			
		} else { //error 1: invalid com_type
			logger.error("Invalid component type! Component registration failed");
			JSONObject response = new JSONObject();
			response.put(TransTechSystem.config.getRequestParamConfig().getRequestIdKey(), request_id);
			response.put(TransTechSystem.config.getRequestParamConfig().getErrorKey(), "invalid:::com_id");
			getMqttHandler().publish(response_topic, response.toString());
		}
	}
}
