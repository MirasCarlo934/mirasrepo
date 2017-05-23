package components;

import java.util.HashMap;
import java.util.Hashtable;

import components.properties.InnateProperty;
import components.properties.AbstProperty;
import components.properties.StringProperty;
import json.RRP.ReqRegister;
import json.RRP.ResError;
import json.RRP.ResRegister;
import main.engines.DBEngine;
import main.engines.requests.DBEngine.UpdateDBEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class Component {
	private Hashtable<String, AbstProperty> properties = new Hashtable<String, AbstProperty>(1);
	private Product product;
	private String SSID;
	private String MAC;
	private String name;
	private String topic;
	private String room;
	private boolean active;
	
	public Component(String SSID, String MAC, String name, String topic, String room, boolean active, Product product) {
		this.setSSID(SSID);
		this.setMAC(MAC);
		this.setName(name);
		this.setTopic(SSID + "_topic");
		this.setRoom(room);
		this.properties = product.getProperties();
		this.setProduct(product);
		setActive(active);
	}
	
	/**
	 * Publishes the credentials of this Component object to the default topic. Creates a new
	 * ResRegister object that contains the credentials of this Component. This method is invoked <b>
	 * solely</b> by the RegistrationModule.
	 * 
	 * @param mh The MQTTHandler that handles the publishing
	 * @param registerRTY The RTY designation for registration requests
	 */
	public void publishCredentials(MQTTHandler mh, String registerRTY) {
		ResRegister response = new ResRegister(MAC, SSID, registerRTY, SSID, topic);
		mh.publish(response);
	}
	
	/**
	 * Returns the property object with the SSID specified
	 * 
	 * @param ssid The SSID of the property
	 * @return The property, <b><i>null</i></b> if the property does not exist
	 */
	public AbstProperty getProperty(String ssid) {
		return properties.get(ssid);
	}
	
	public Hashtable<String, AbstProperty> getProperties() {
		return properties;
	}

	/**
	 * @return the mAC
	 */
	public String getMAC() {
		return MAC;
	}

	/**
	 * @param mAC the mAC to set
	 */
	public void setMAC(String mAC) {
		MAC = mAC;
	}

	/**
	 * @return the sSID
	 */
	public String getSSID() {
		return SSID;
	}

	/**
	 * @param sSID the sSID to set
	 */
	public void setSSID(String sSID) {
		SSID = sSID;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * @param topic the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * @return the room
	 */
	public String getRoom() {
		return room;
	}

	/**
	 * @param room the room to set
	 */
	public void setRoom(String room) {
		this.room = room;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Sets the active property of this Component and persists it to the DB.
	 * 
	 * @param active <b>true</b> if the Component is active, <b>false</b> if not
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	
	/**
	 * Sets the active state of this Component and persists it to the DB.
	 * 
	 * @param active <b>true</b> if the Component is active, <b>false</b> if not
	 * @param dbe The DBEngine that will handle the persistence
	 * @param comsTable The DB table name where the persistence will take place
	 * @throws Exception thrown when persistence failed
	 */
	public void setActive(boolean active, DBEngine dbe, String comsTable) throws Exception {
		this.active = active;
		Thread t = Thread.currentThread();
		IDGenerator idg = new IDGenerator();
		HashMap<String, Object> vals = new HashMap<String, Object>(1, 1);
		HashMap<String, Object> args = new HashMap<String, Object>(1, 1);
		vals.put("active", active);
		args.put("SSID", SSID);
		UpdateDBEReq udber = new UpdateDBEReq(idg.generateMixedCharID(10), comsTable, vals, args);
		dbe.processRequest(udber, t);
		try {
			synchronized(t){t.wait();}
		} catch(InterruptedException e) {
			throw new Exception("Problem with thread waiting!");
		}
		Object o = dbe.getResponse(udber.getId());
		if(o.getClass().equals(ResError.class)) {
			throw new Exception("Cannot persist Component active state!");
		}
	}

	/**
	 * @return the product
	 */
	public Product getProduct() {
		return product;
	}

	/**
	 * @param product the product to set
	 */
	public void setProduct(Product product) {
		this.product = product;
	}
}
