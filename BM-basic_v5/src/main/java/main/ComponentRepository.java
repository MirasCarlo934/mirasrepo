package main;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import devices.Component;
import devices.Product;
import devices.Property;
import json.objects.ReqRegister;
import main.engines.DBEngine;
import main.engines.requests.DBEngine.UpdateDBEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class ComponentRepository {
	private static final Logger LOG = Logger.getLogger("BM_LOG.Devices");
	@Autowired
	private MQTTHandler mh;
	private Hashtable<String, Component> components = new Hashtable<String, Component>(1);
	private Hashtable<String, String> registeredMACs = new Hashtable<String, String>(1,1); //registered MAC and corresponding SSID
	private IDGenerator idg = new IDGenerator();
	private DBEngine dbm;
	//private Catalog catalog;
	private String deviceQuery;
	private String productQuery;

	public ComponentRepository(/*Catalog catalog, */DBEngine dbm, String deviceQuery, String productQuery) {
		this.deviceQuery = deviceQuery;
		this.productQuery = productQuery;
		//this.catalog = catalog;
		this.dbm = dbm;
		populateDevices();
	}
	
	public void populateDevices() {
		try {
			LOG.info("Populating Devices...");
			ResultSet rs = dbm.executeQuery(deviceQuery);
			while(rs.next()) {
				String SSID = rs.getString("SSID");
				String topic = rs.getString("topic");
				String MAC = rs.getString("MAC");
				String room = rs.getString("room");
				String prod_id = rs.getString("functn");
				String name = rs.getString("name");
				boolean active = rs.getBoolean("ACTIVE");
				
				String prop_id = rs.getString("prop_id");
				int prop_val = rs.getInt("prop_value");
				
				if(!components.containsKey(SSID)) { //true if devices does NOT contain this device
					ResultSet rs2 = dbm.executeQuery(productQuery + " and cpl.COM_TYPE = '" + prod_id + "'");
					LOG.debug("Adding device: " + SSID + " into Devices");
					Component com = new Component(SSID, MAC, name, topic, room, active, new Product(rs2));
					addComponent(com);
				}
				
				//populates properties of device with persisted values
				LOG.debug("Setting property: " + prop_id + " of device: " + SSID + " with value: " + prop_val);
				Component com = components.get(SSID);
				com.getProperty(prop_id).setValue(prop_val);
			}
			LOG.info("Devices population done!");
		} catch (SQLException e) {
			LOG.error("Cannot populate Devices!", e);
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
		Property property = device.getProperty(propID);
		property.setValue(propValue);
		
		LOG.info("Updating property:" + propID + " of device:" + deviceID + " in DB...");
		HashMap<String, Object> args = new HashMap<String, Object>(1);
		args.put("CPL_SSID", propID);
		HashMap<String, Object> vals = new HashMap<String, Object>(1);
		vals.put("prop_value", String.valueOf(propValue));
		//dbm.updateQuery("comp_properties", args, vals);
		dbm.forwardRequest(new UpdateDBEReq(idg.generateMixedCharID(10), "comp_properties", 
				args, vals));
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
}