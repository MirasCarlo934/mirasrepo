package main;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import components.Component;
import components.Product;
import components.bindings.Binding;
import components.properties.AbstProperty;
import components.properties.CommonProperty;
import components.properties.InnateProperty;
import components.properties.PropertyMode;
import components.properties.PropertyValueType;
import components.properties.StringProperty;
import json.RRP.ReqRegister;
import json.RRP.ResError;
import main.engines.DBEngine;
import main.engines.requests.DBEngine.RawDBEReq;
import main.engines.requests.DBEngine.SelectDBEReq;
import main.engines.requests.DBEngine.UpdateDBEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class ComponentRepository {
	private static final Logger LOG = Logger.getLogger("BM_LOG.ComponentRepository");
	@Autowired
	private MQTTHandler mh;
	private HashMap<String, String> rooms = new HashMap<String, String>(1,1);
	private HashMap<String, Component> components = new HashMap<String, Component>(1);
	private Vector<Binding> bindings = new Vector<Binding>(1,1);
	private HashMap<String, String> registeredMACs = new HashMap<String, String>(1,1); //registered MAC and corresponding SSID
	private IDGenerator idg = new IDGenerator();
	private DBEngine dbm;
	//private Catalog catalog;
	private String deviceQuery;
	private String productQuery;
	private String roomsTable;
	private String bindingsTable;
	
	//for instantiating different properties
	private String stringPropTypeID;
	private String innatePropTypeID;

	public ComponentRepository(/*Catalog catalog, */DBEngine dbm, String deviceQuery, 
			String productQuery, String roomsTable, String bindingsTable, String stringPropTypeID, 
			String innatePropTypeID) {
		this.deviceQuery = deviceQuery;
		this.productQuery = productQuery;
		this.roomsTable = roomsTable;
		this.bindingsTable = bindingsTable;
		this.dbm = dbm;
		this.stringPropTypeID = stringPropTypeID;
		this.innatePropTypeID = innatePropTypeID;
		populateDevices();
		retrieveRooms();
		retrieveBindings();
	}
	
	/**
	 * Retrieves all components from DB.
	 */
	public void populateDevices() {
		try {
			LOG.info("Populating Devices...");
			RawDBEReq dber1 = new RawDBEReq(idg.generateMixedCharID(10), deviceQuery);
			RawDBEReq dber2 = new RawDBEReq(idg.generateMixedCharID(10), productQuery);
			dbm.processRequest(dber1, Thread.currentThread());
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
			Object o = dbm.getResponse(dber1.getId());
			if(o.getClass().equals(ResError.class)) {
				ResError error = (ResError) o;
				LOG.error(error.message);
				return;
			}
			ResultSet coms_rs = (ResultSet) o;
			
			dbm.processRequest(dber2, Thread.currentThread());
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
			Object o2 = dbm.getResponse(dber2.getId());
			if(o2.getClass().equals(ResError.class)) {
				ResError error = (ResError) o2;
				LOG.error(error.message);
				coms_rs.close();
				return;
			}
			ResultSet prod_rs = (ResultSet) o2;
			
			while(coms_rs.next()) {
				String SSID = coms_rs.getString("SSID");
				String topic = coms_rs.getString("topic");
				String MAC = coms_rs.getString("MAC");
				String room = coms_rs.getString("room");
				String prod_id = coms_rs.getString("functn");
				String name = coms_rs.getString("name");
				boolean active = coms_rs.getBoolean("ACTIVE");
				
				String prop_id = coms_rs.getString("prop_id");
				Object prop_val = coms_rs.getString("prop_value");
				
				if(!components.containsKey(SSID)) { //true if devices does NOT contain this device
					String prod_name = "null";
					String prod_desc = "null";
					String prod_OH_icon = "null";
					Vector<AbstProperty> prod_props = new Vector<AbstProperty>(1,1);
					while(prod_rs.next()) {
						String rs2_ssid = prod_rs.getString("prod_ssid");
						if(rs2_ssid.equals(prod_id)) {
							prod_name = prod_rs.getString("prod_name");
							prod_desc = prod_rs.getString("prod_desc");
							prod_OH_icon = prod_rs.getString("oh_icon");
							
							String prop_type = prod_rs.getString("prop_type");
							String prop_dispname = prod_rs.getString("prop_dispname");
							String prop_sysname = prod_rs.getString("prop_sysname");
							String prop_mode = prod_rs.getString("prop_mode");
							String pval_type = prod_rs.getString("prop_val_type");
							int prop_min = prod_rs.getInt("prop_min");
							int prop_max = prod_rs.getInt("prop_max");
							String prop_index = prod_rs.getString("prop_index");
							AbstProperty prop;
							if(prop_type.equals(stringPropTypeID)) {
								prop = new StringProperty(prop_type, prop_index, SSID, 
										prop_sysname, prop_dispname, 
										PropertyMode.parseModeFromString(prop_mode));
							} else if(prop_type.equals(innatePropTypeID)) {
								prop = new InnateProperty(prop_type, prop_index, SSID, prop_sysname, prop_dispname, 
										PropertyValueType.parsePropValTypeFromString(pval_type));
							} else {
								prop = new CommonProperty(prop_type, prop_index, SSID, prop_sysname, 
									prop_dispname, PropertyMode.parseModeFromString(prop_mode), 
									PropertyValueType.parsePropValTypeFromString(pval_type), 
									prop_min, prop_max);
							}
							prod_props.add(prop);
						}
					}
					prod_rs.beforeFirst();
					Product product = new Product(prod_id, prod_name, prod_desc, prod_OH_icon, 
							prod_props.toArray(new AbstProperty[prod_props.size()]));
					LOG.debug("Adding component " + SSID + " into ComponentRepository...");
					Component com = new Component(SSID, MAC, name, topic, room, active, product);
					addComponent(com);
					LOG.debug("Component " + SSID + " added!");
				}
				
				//populates properties of device with persisted values
				LOG.debug("Setting property: " + prop_id + " of device: " + SSID + " with value: " + prop_val);
				Component com = components.get(SSID);
				com.getProperty(prop_id).setValue(prop_val);
			}
			coms_rs.close();
			prod_rs.close();
			LOG.info("Devices population done!");
		} catch (SQLException e) {
			LOG.error("Cannot populate Devices!", e);
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Retrieves all rooms from DB.
	 */
	public void retrieveRooms() {
		LOG.info("Retrieving rooms from DB...");
		SelectDBEReq dber1 = new SelectDBEReq(idg.generateMixedCharID(10), roomsTable);
		dbm.processRequest(dber1, Thread.currentThread());
		try {
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		Object o = dbm.getResponse(dber1.getId());
		if(o.getClass().equals(ResError.class)) {
			ResError e = (ResError) o;
			LOG.error("Cannot retrieve rooms from DB!");
			LOG.error("Error message: " + e.message);
		} else {
			ResultSet rs1 = (ResultSet) o;
			try {
				while(rs1.next()) {
					rooms.put(rs1.getString("ssid"), rs1.getString("name"));
				}
				rs1.close();
				LOG.debug("Rooms retrieved!");
			} catch (SQLException e) {
				LOG.error("ResultSet error in retrieving rooms!", e);
				e.printStackTrace();
			}
		}
	}
	
	public void retrieveBindings() {
		LOG.info("Retrieving OH bindings from DB...");
		SelectDBEReq dber1 = new SelectDBEReq(idg.generateMixedCharID(10), bindingsTable);
		dbm.processRequest(dber1, Thread.currentThread());
		try {
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		Object o = dbm.getResponse(dber1.getId());
		if(o.getClass().equals(ResError.class)) {
			ResError e = (ResError) o;
			LOG.error("Cannot retrieve bindings from DB!");
			LOG.error("Error message: " + e.message);
		} else {
			ResultSet rs1 = (ResultSet) o;
			try {
				while(rs1.next()) {
					bindings.add(new Binding(rs1.getString("SSID"), rs1.getString("com_type"),
							rs1.getString("prop_id"), rs1.getString("binding")));
				}
				rs1.close();
				LOG.debug("Bindings retrieved!");
			} catch (SQLException e) {
				LOG.error("ResultSet error in retrieving bindings!", e);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Creates a new device entry in BM and DB. Main method called upon by a register request
	 * 
	 * @param register The register request
	 * @return The new Device object, <b>null</b> if:<br>
	 * 		<ul>
	 * 			<li>Error in persistence process</li>
	 * 			<li>Invalid room</li>
	 * 		</ul>
	 */
	/*public Component createNewDevice(Register register) {
		LOG.info("Registering new device...");
		
		if(registeredMACs.containsKey(register.rid)) { //checks if Devices already contain this Device
			LOG.warn("Device: " + register.rid + " already registered!");
			return devices.get(registeredMACs.get(register.rid));
		}
		else {
			LOG.info("Creating new device...");
			String[] existingIDs = new String[0];
			devices.keySet().toArray(existingIDs);
			Component device = new Component(idgen.generateMixedCharID(4, existingIDs), register, register.getProduct(), true);
			LOG.info("Device: " + device.getSSID() + " created! ");
			
			try {
				LOG.info("Persisting new device to DB...");
				dbm.insertQuery(new String[]{device.getSSID(), device.getTopic(), device.getMAC(), device.getName(), 
						device.getRoom(), device.getProduct().getSSID(), String.valueOf(device.isActive())}, 
						"components");
				LOG.info("Device persisted!");
				
				/*=====================================================
				
				LOG.info("Persisting new device properties to DB...");
				Property[] properties = device.getProperties().values().toArray(new Property[0]);
				
				//gets all existing property SSIDs from DB
				ResultSet rs = dbm.selectQuery("SSID", "comp_properties");
				Vector<String> prop_SSIDs = new Vector<String>(1,1);
				while(rs.next()) {
					prop_SSIDs.add(rs.getString("SSID"));
				}
				
				for(int i = 0; i < properties.length; i++) {
					Property property = properties[i];
					String prop_ssid = idgen.generateMixedCharID(4, prop_SSIDs.toArray(new String[0]));
					dbm.insertQuery(new String[]{prop_ssid, device.getSSID(), property.getName(), 
							"0", property.getPropertyID()}, 
							"comp_properties");
					prop_SSIDs.add(prop_ssid);
				}
				LOG.info("New device properties persisted!");
				
				/*=====================================================
				
				LOG.info("Registration process complete! Device: " + device.getSSID() + " registered into BM.");
				addDevice(device);
				return device;
			} catch (SQLException e) {
				LOG.error("Cannot persist new device to DB!", e);
				return null;
			}
		}
	}*/
	
	public void changeDBProperty(String deviceID, String propID, int propValue) {
		Component device = components.get(deviceID);
		AbstProperty property = device.getProperty(propID);
		property.setValue(propValue);
		
		LOG.info("Updating property:" + propID + " of device:" + deviceID + " in DB...");
		HashMap<String, Object> args = new HashMap<String, Object>(1);
		args.put("CPL_SSID", propID);
		HashMap<String, Object> vals = new HashMap<String, Object>(1);
		vals.put("prop_value", String.valueOf(propValue));
		//dbm.updateQuery("comp_properties", args, vals);
		UpdateDBEReq dber1 = new UpdateDBEReq(idg.generateMixedCharID(10), "comp_properties", 
				args, vals);
		dbm.processRequest(dber1, Thread.currentThread());
		LOG.info("Property updated!");
		/*try {
			LOG.info("Updating property:" + propID + " of device:" + deviceID + " in DB...");
			HashMap<String, Object> args = new HashMap<String, Object>(1);
			args.put("CPL_SSID", propID);
			HashMap<String, Object> vals = new HashMap<String, Object>(1);
			vals.put("prop_value", String.valueOf(propValue));
			//dbm.updateQuery("comp_properties", args, vals);
			dbm.forwardRequest(new UpdateDBEReq(idg.generateMixedCharID(10), "comp_properties", 
					args, vals));
			LOG.info("Property updated!");
		} catch (SQLException e) {
			LOG.error("Cannot update property in DB!", e);
		}*/
	}
	
	public void addComponent(Component device) {
		components.put(device.getSSID(), device);
		registeredMACs.put(device.getMAC(), device.getSSID());
	}
	
	/**
	 * Removes the component with the specified SSID from the repository
	 * 
	 * @param SSID The SSID of the component to be removed
	 */
	public void removeComponent(String SSID) {
		String mac = components.remove(SSID).getMAC();
		registeredMACs.remove(mac);
	}
	
	/**
	 * Returns the Component with the specified SSID or MAC
	 * @param s The SSID or MAC to specify
	 * @return the Component with the specified SSID or MAC, <i>null</i> if nonexistent
	 */
	public Component getComponent(String s) {
		if(components.containsKey(s)) {
			return components.get(s);
		} else if (registeredMACs.containsKey(s)) {
			return components.get(registeredMACs.get(s));
		} else {
			return null;
		}
	}
	
	/**
	 * Returns all the Components registered in this ComponentRepository
	 * @return the array containing all Components
	 */
	public Component[] getAllComponents() {
		return components.values().toArray(new Component[components.size()]);
	}
	
	/**
	 * Returns all existing rooms in this ComponentRepository
	 * @return the HashMap containing all room SSID and room names
	 */
	public HashMap<String, String> getAllRooms() {
		return rooms;
	}
	
	/**
	 * Returns all retrieved Bindings from DB
	 * @return the array containing all Bindings
	 */
	public Binding[] getAllBindings() {
		return bindings.toArray(new Binding[bindings.size()]);
	}
	
	/**
	 * Checks if the SSID or the MAC specified already exists in this ComponentRepository
	 * @param str The SSID or MAC to be tested
	 * @return
	 */
	public boolean containsComponent(String str) {
		if(components.containsKey(str) || registeredMACs.containsKey(str)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Checks if a component with the specified name exists in the repository.
	 * 
	 * @param name The name of the component to be checked
	 * @return
	 */
	public boolean containsComponentWithName(String name) {
		Iterator<Component> coms = components.values().iterator();
		while(coms.hasNext()) {
			Component c = coms.next();
			if(c.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
}