package devices;

import java.util.Hashtable;

import json.objects.ReqRegister;

public class Component {
	private Hashtable<String, Property> properties = new Hashtable<String, Property>(1);
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
	
	/*public Device(String SSID, Register register, Product product, boolean active) {
		this.setSSID(SSID);
		this.setMAC(register.rid);
		this.setName(register.name);
		this.setTopic(SSID + "_topic");
		this.setRoom(register.room);
		this.properties = product.getProperties();
		setActive(active);
	}*/
	
	public void setPropertyValue(String prop_ssid, int value) {
		Property property = properties.get(prop_ssid);
		property.setValue(value);
	}
	
	/**
	 * Returns the property object with the SSID specified
	 * 
	 * @param ssid The SSID of the property
	 * @return The property, <b><i>null</i></b> if the property does not exist
	 */
	public Property getProperty(String ssid) {
		return properties.get(ssid);
	}
	
	public Hashtable<String, Property> getProperties() {
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
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
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
