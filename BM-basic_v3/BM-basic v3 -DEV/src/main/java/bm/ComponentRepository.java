package bm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import main.TransTechSystem;
import main.components.Component;
import main.components.Room;
import main.components.openhab.OpenhabItem;
import main.components.properties.Property;
import main.components.properties.PropertyMode;
import main.objects.*;
import main.objects.DB.DBObject;
import main.objects.request_response.BMRequest;
import main.objects.request_response.ErrorResponse;
import main.objects.request_response.InsertTransactionRequest;
import main.objects.request_response.Response;
import main.objects.request_response.TransactionRequest;
import main.objects.request_response.TransactionResponse;
import tools.IDGenerator;
import tools.TrafficController;

/**
 * ComponentRepository documentation:
 * 		The ComponentRepository object exists as a container of all Java object representation of each component in the IoT system. A
 * 	component is defined as "an electronic device that communicates with the BusinessMachine through the use of an MQTT broker in order to
 * 	send requests and receive responses for client-system interaction." The ComponentRepository is necessary for the system in order to 
 * 	keep record of all the components that exist within the IoT system. Another function of the ComponentRepository is for the BM to access
 * 	specific component information that may be used in the processing of requests in the Module objects.
 * 
 * 		The ComponentRepository loads all of the already-registered components from the DB in instantiation. In order to update the
 * 	records of the ComponentRepository, the 'update' method must be called.
 */
public class ComponentRepository {
	private static final Logger logger = Logger.getLogger(ComponentRepository.class);
	private HashMap<String, Component> repository = new HashMap<String, Component>(1);
	private TrafficController tc;
	private TransactionEngine te;
	private IDGenerator idg;
	/*
	 * The following are for easier value manipulation in case they need to be changed.
	 */
	//variables used in database interaction
	private static final String room_obj_type = "room";
	private static final String comSSID_col_name = TransTechSystem.config.getDatabaseConfig().getSsidColName();
	private static final String comType_col_name = "type";
	private static final String comMqttTopic_col_name = "topic";
	private static final String comMacAddress_col_name = "mac";
	private static final String comRoomid_col_name = "room";
	private static final String comFunction_col_name = "functn";
	private static final String comDescription_col_name = "name";
	private static final String components_table = 
			TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get("component").getTableName();
	private static final String rooms_table =
			TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(room_obj_type).getTableName();
	
	private static final String com_props_table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get("com_prop").getTableName();
	private static final String com_id_col_name = "com_id";
	private static final String switch_funct_ssid = "0003";
	private static final String physswitch_funct_id = "0002";
	private static final String system_funct_ssid = "0000";
	private static final String SSID_col_name = TransTechSystem.config.getDatabaseConfig().getSsidColName().toLowerCase();
	private static final String esp_com_type = "ESP";
	
	public ComponentRepository(TrafficController trafficController, TransactionEngine transactionEngine, IDGenerator
			idgenerator) {
		this.tc = trafficController;
		te = transactionEngine;
		idg = idgenerator;
	}
	
	/**
	 * Updates the database components' records using this ComponentRepository's records. 
	 * This method is invoked on a scheduled basis.
	 */
	public void updateDatabase() {
		//updates COMPONENTS and COMP_PROPERTIES tables
		logger.debug("Updating database records...");
		Component[] coms = repository.values().toArray(new Component[0]);
		for(int i = 0; i < coms.length; i++) {
			Component com = coms[i];
			HashMap<String, Object> args1 = new HashMap<String, Object>(1);
			args1.put("ssid", com.getId());
			args1.put("mac", com.getMacAddress());
			args1.put("topic", com.getMqttTopic());
			args1.put("name", com.getName());
			args1.put("functn", com.getFunction());
			args1.put("room", com.getRoom());
			InsertTransactionRequest itr1 = new InsertTransactionRequest("component", args1);
			try {
				te.insert(itr1);
				logger.debug("New component added to the database!");
			} catch (SQLException e) {
				if(e.getErrorCode() != 30000) //for non-unique constraint errors
					logger.error("Cannot update component records in database!", e);
			}
			
		}
	}
	
	/**
	 * Removes the component with the specified SSID from this ComponentRepository. Also deletes the component's
	 * records from the DB.
	 * 
	 * @param com_id The SSID of the component to be detached.
	 * @return <b>true</b> if the component was detached successfully, <b>false</b> if there is no component found with
	 * 		the specified <b>com_id</b>.
	 * @throws SQLException 
	 */
	public boolean detachComponent(String com_id) throws SQLException {
		boolean b = false;
		String cid = com_id;
		
		if(containsComponent(cid)) {
			//deletes registered component properties first
			logger.trace("Deleting registered component properties from DB...");
			HashMap<String, Object> args = new HashMap<String, Object>(1);
			args.put("com_id", cid);
			TransactionRequest treq = new TransactionRequest("com_prop", args);
			te.delete(treq);
			
			//deletes component from DB
			logger.trace("Deleting component registry from DB...");
			HashMap<String, Object> args1 = new HashMap<String, Object>(1);
			args1.put("ssid", cid);
			TransactionRequest treq1 = new TransactionRequest("component", args1);
			te.delete(treq1);
			
			update();
			b = true;
		}
		else { //ERROR: Component not yet registered
			b = false;
		}
		
		return b;
	}
	
	/**
	 * Registers the component to the system. This includes the persistence of the component's attributes to the DB and
	 * also the inclusion of this new component to this ComponentRepository.<br><br>
	 * 
	 * <i>This method is invoked ONLY by the CRModule</i>
	 * 
	 * @param mac The MAC Address of the new component
	 * @param name The name of the new component
	 * @param function The product ID of the new component
	 * @param room The room name/SSID of the new component
	 * @return true if registration was successful, false otherwise
	 * @throws SQLException
	 */
	public Component registerComponent(String mac, String name, String function, String room) throws SQLException {
		boolean b = true;
		Component c = null;
		String id;
		
		//generates CID
		do{
			id = idg.generateMixedCharID(TransTechSystem.config.getDatabaseConfig().getSsidLength());
		} while(containsComponent(id));
		
		//gets properties using cplist (IMPROVE THIS)
		HashMap<String, Object> arg0 = new HashMap<String, Object>(1);
		arg0.put("com_type", function);
		TransactionRequest treq0 = new TransactionRequest("cplist", arg0);
		TransactionResponse tres0 = te.selectSpecific(treq0);
		Vector<DBObject> cplist = tres0.getObjects();		
		Vector<Property> com_props = new Vector<Property>(1,1);
		
		HashMap<String, Integer> props_map = new HashMap<String, Integer>(1); //map containing the prop name and the prop qty
		Vector<String> disp_names = new Vector<String>(1,1); //contains disp names of the properties in order of the props_map
		for(int i = 0; i < cplist.size(); i++) {
			DBObject list = cplist.get(i);
			String prop_id = (String) list.get("prop_type");
			String disp_name = (String) list.get("disp_name");
			props_map.put(prop_id, props_map.getOrDefault(prop_id, 0) + 1);
			disp_names.add(disp_name);
		}
		
		int index = 0; //component property index indicator
		for(int i = 0; i < props_map.size(); i++) {
			String prop_id = (String)props_map.keySet().toArray()[i];
			String disp_name = disp_names.get(i);
			int qty = props_map.get(prop_id);
			for(int j = 0; j < qty; j++) {
				HashMap<String, Object> arg1 = new HashMap<String, Object>(1);
				arg1.put("ssid", prop_id);
				TransactionRequest treq1 = new TransactionRequest("property", arg1);
				TransactionResponse tres1 = te.selectSpecific(treq1);
				DBObject prop = tres1.getObjects().get(0);
				
				String pname = (String)prop.get("name");
				PropertyMode pmode = PropertyMode.parseMode((String)prop.get("mode"));
				String ptype = (String)prop.get("type");
				
				HashMap<String, Object> arg2 = new HashMap<String, Object>(1);
				arg2.put("ssid", ptype);
				TransactionRequest treq2 = new TransactionRequest("pval", arg2);
				TransactionResponse tres2 = te.selectSpecific(treq2);
				DBObject pvals = tres2.getObjects().get(0);
				
				int min = (int)pvals.get("minim");
				int max = (int)pvals.get("maxim");
				
				OpenhabItem ohitem = OpenhabItem.parseItem((String)pvals.get("oh_item"));
				com_props.add(new Property(prop_id, pname + "-" + ((int)index + (int)1), disp_name, pmode, min, max, 0, ohitem));
				index++;
			}
		}
		
		Property[] props = com_props.toArray(new Property[0]);
		c = new Component(id, name, function, room, esp_com_type + "_" + id + "_topic", mac, props);
		
		HashMap<String, Object> a = new HashMap<String, Object>(1);
		a.put(SSID_col_name, c.getRoom());
		TransactionResponse rtres = te.selectSpecific(
				new TransactionRequest(room_obj_type, a));
		HashMap<String, Object> a2 = new HashMap<String, Object>(1);
		a2.put("name", c.getRoom());
		TransactionResponse rtres2 = te.selectSpecific(
				new TransactionRequest(room_obj_type, a2));
		
		if(rtres2.getObjects().isEmpty() && rtres.getObjects().isEmpty()) { //true if a room exists
			//create new room, assumes that the value put is the name of the new room
			HashMap<String, Object> new_room = new HashMap<String, Object>(1);
			new_room.put("name", c.getRoom());
			TransactionResponse tres2 = te.insert(
					new InsertTransactionRequest(room_obj_type, new_room));
			c.setRoom((String)tres2.get(SSID_col_name));
		} else if(!rtres2.getObjects().isEmpty()){ //there is a room with the specified NAME
			//gets the ssid of the existing room
			c.setRoom((String)rtres2.getObjects().get(0).get(SSID_col_name));
			logger.debug((String)rtres2.getObjects().get(0).get(SSID_col_name));
		}
		
		//persists new component to database
		HashMap<String, Object> vals = new HashMap<String, Object>(1);
		vals.put(TransTechSystem.config.getDatabaseConfig().getSsidColName().toLowerCase(), c.getId());
		vals.put(comMacAddress_col_name, c.getMacAddress());
		vals.put(comMqttTopic_col_name, c.getMqttTopic());
		vals.put(comFunction_col_name, c.getFunction());
		vals.put(comDescription_col_name, name);
		vals.put(comRoomid_col_name, c.getRoom());
		logger.debug(c.getRoom());
		logger.debug(c.getMqttTopic());
		logger.debug(c.getFunction());
		logger.debug(c.getMacAddress());
		InsertTransactionRequest insert = new InsertTransactionRequest("component", vals);
		te.insert(insert);
		
		//persists properties of new component to database
		Property[] com_props1 = c.getProperties();
		for(int i = 0; i < com_props1.length; i++) {
			Property prop = com_props1[i];
			String prop_name = prop.getSystemName();
			Object prop_val = "0"; //is always 0 for newly created objects
			HashMap<String, Object> vals2 = new HashMap<String, Object>(1);
			vals2.put("com_id", c.getId());
			vals2.put("prop_name", prop_name);
			vals2.put("prop_value", prop_val);
			InsertTransactionRequest insert2 = new InsertTransactionRequest("com_prop", vals2);
			TransactionResponse insert2_res = te.insert(insert2); 
			if(!insert2_res.isSuccessful()) {
				b = false;
				c = null;
				break;
			}
		}
		
		if(b) {
			update();
		}
		return c;
	}
	
	/**
	 * Updates the records of this ComponentRepository by fetching the records from the database
	 */
	public void update() {
		logger.trace("Updating records...");
		try {
			HashMap<String, Component> newRecords = new HashMap<String, Component>(1,1);
			
			/*
			 * Gets component basic info
			 */
			ResultSet rs = tc.selectQuery("*", components_table);
			while(rs.next()) {
				Component c = null;
				String id = rs.getString(comSSID_col_name);
				String topic = rs.getString(comMqttTopic_col_name);
				String mac = rs.getString(comMacAddress_col_name);
				String function = rs.getString(comFunction_col_name);
				String description = rs.getString(comDescription_col_name);
				String room = rs.getString(comRoomid_col_name);
				
				c = getComponentFromDB(id);
				
				/*
				 * Gets component properties
				 */
				HashMap<String, Object> args = new HashMap<String, Object>(1);
				args.put(com_id_col_name, c.getId());
				ResultSet rs2 = tc.selectQuery("*", com_props_table, args);
				while(rs2.next()) {
					String prop_name = rs2.getString("prop_name");
					String prop_val = rs2.getString("prop_value");
					c.setPropertyValue(prop_name, prop_val);
				}
				newRecords.put(id, c);
			}
			
			repository = newRecords;
			logger.trace("Update completed!");
		} catch (SQLException e) {
			logger.error("Cannot update records!", e);
		}
	}
	
	/**
	 * Gets all the Rooms from the DB.
	 * 
	 * @return an array containing all the Rooms
	 */
	public Room[] getAllRooms() {
		TransactionRequest treq = new TransactionRequest(room_obj_type, null);
		Vector<DBObject> raw_rooms = null;
		try {
			raw_rooms = te.selectAll(treq).getObjects();
		} catch (SQLException e) {
			logger.error("Cannot get rooms from DB!", e);
		}
		Room[] rooms = new Room[raw_rooms.size()];
		
		for(int i = 0; i < rooms.length; i++) {
			rooms[i] = new Room(raw_rooms.get(i));
		}
		
		return rooms;
	}
	
	/**
	 * Gets components that belong to the room with the specified ID.
	 * 
	 * @param room_id the room's SSID
	 * @return an array of components, <b>null</b> if no components are in the room or if <i>room_id</i> is not a valid
	 * 		room SSID.
	 */
	public Component[] getComponentsByRoom(String room_id) {
		Component[] coms = null;
		Vector<Component> vcoms = new Vector<Component>(1,1);
		for(int i = 0; i < repository.size(); i++) {
			Component com = repository.get(i);
			if(com.getRoom().equals(room_id)) {
				vcoms.add(com);
			}
		}
		if(!vcoms.isEmpty()) {
			vcoms.toArray(coms);
		}
		return coms;
	}
	
	/**
	 * Retrieves data for the creation of a Component object using the SSID specified.
	 * @param ssid the SSID of the component
	 * @throws SQLException 
	 */
	private Component getComponentFromDB(String ssid) throws SQLException {
		HashMap<String, Object> args = new HashMap<String, Object>(1);
		args.put("ssid", ssid);
		TransactionRequest treq = new TransactionRequest("component", args);
		TransactionResponse tres = te.selectSpecific(treq);
		DBObject comp = tres.getObjects().get(0);
		String id = (String) comp.get("ssid");
		String name = (String)comp.get("name");
		String topic = (String)comp.get("topic");
		String mac = (String)comp.get("mac");
		String function = (String)comp.get("functn");
		String room = (String)comp.get("room");
		
		//gets all Component information from DB (PROPCAT, COMPROPLIST, PVALCAT)
		HashMap<String, Object> arg0 = new HashMap<String, Object>(1);
		arg0.put("com_type", function);
		TransactionRequest treq0 = new TransactionRequest("cplist", arg0);
		TransactionResponse tres0 = te.selectSpecific(treq0);
		Vector<DBObject> cplist = tres0.getObjects();		
		Vector<Property> com_props = new Vector<Property>(1,1);
		for(int i = 0; i < cplist.size(); i++) {
			DBObject list = cplist.get(i);
			String prop_id = (String) list.get("prop_type");
			String disp_name = (String) list.get("disp_name");
			
			HashMap<String, Object> arg1 = new HashMap<String, Object>(1);
			arg1.put("ssid", prop_id);
			TransactionRequest treq1 = new TransactionRequest("property", arg1);
			TransactionResponse tres1 = te.selectSpecific(treq1);
			DBObject prop = tres1.getObjects().get(0);
			
			String pname = (String)prop.get("name");
			PropertyMode pmode = PropertyMode.parseMode((String)prop.get("mode"));
			String ptype = (String)prop.get("type");
			
			HashMap<String, Object> arg2 = new HashMap<String, Object>(1);
			arg2.put("ssid", ptype);
			TransactionRequest treq2 = new TransactionRequest("pval", arg2);
			TransactionResponse tres2 = te.selectSpecific(treq2);
			DBObject pvals = tres2.getObjects().get(0);
			
			int min = (int)pvals.get("minim");
			int max = (int)pvals.get("maxim");
			
			OpenhabItem ohitem = OpenhabItem.parseItem((String)pvals.get("oh_item"));
			com_props.add(new Property(prop_id, pname + "-" + ((int)i + (int)1), disp_name, pmode, min, max, 0, ohitem));
		}
		//
		
		Property[] props = com_props.toArray(new Property[0]);
		return new Component(id, name, function, room, topic, mac, props);
	}
	
	/**
	 * Adds component to the repository.<br><br>
	 * 
	 * <b>Note:</b> This method is different from registerComponent() because this method only adds the specified component
	 * to the HashMap located in this ComponentRepository while registerComponent() adds a new component to the system
	 * altogether. This method is only usually invoked in the update() method after the already-registered components
	 * are retrieved from the database.
	 * 
	 * @param c The Component to be added to the repository.
	 */
	public void addComponent(Component c) {
		repository.put(c.getId(), c);
		logger.trace("New Component added to system! (ID:" + c.getId() + ")");
	}
	
	public Component getComponent(String id) {
		return repository.get(id);
	}
	
	public boolean containsComponent(String id) {
		if(repository.containsKey(id)) return true;
		else return false;
	}
	
	public Component[] getAllComponents() {
		Component[] c = new Component[repository.size()];
		repository.values().toArray(c);
		/*for(int i = 0; i < c.length; i++) {
			System.out.println(c[i].getName());
			Property[] props = c[i].getProperties();
			for(int j = 0; j < props.length; j++) {
				logger.debug(props[j].getName());
			}
		}*/
		return c;
	}
}
