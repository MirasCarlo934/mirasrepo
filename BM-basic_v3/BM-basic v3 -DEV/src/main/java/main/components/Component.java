package main.components;

import java.util.Vector;

import org.apache.log4j.Logger;

import bm.objects.cir.CodeBlock;
import main.components.properties.Property;
import main.objects.DB.DBObject;

public class Component {
	
	//contains all columns from COMPONENTS table
	public static final String ssid_col = "ssid";
	public static final String type_col = "type";
	public static final String topic_col = "topic";
	public static final String mac_col = "mac";
	public static final String funct_col = "functn";
	public static final String desc_col = "description";
	public static final String room_col = "room";
	public static final String[] components_cols = {"ssid", "topic", "mac", "functn", "description", "room"};
	
	/** The value of all properties that are not yet set. */
	public static final String unset_prop_val = "empty";
	
	private String id;
	private String mqttTopic;
	private String macAddress;
	private String function; 	//SSID of the function of this Component
	private String name;
	private String room; //SSID of the room where this component belongs to
	private Property[] properties;
	
	/**
	 * Creates a raw component. To set properties, use the setProperties() method.
	 * @param type
	 * @param id
	 * @param name
	 * @param room
	 * @param function
	 * @param mqttTopic
	 * @param macAddress
	 */
	public Component(String id, String name, String function, String room, String mqttTopic, 
			String macAddress, Property[] properties) {
		this.setId(id);
		this.setMqttTopic(mqttTopic);
		this.setMacAddress(macAddress);
		setFunction(function);
		setName(name);
		setRoom(room);
		setProperties(properties);
	}
	
	/**
	 * Creates a raw component from DBObjects.
	 * 
	 * @param com_object the data for the component info
	 * @param properties the data for the component properties
	 */
	public Component(DBObject com_object, DBObject properties) throws NullPointerException {
		//checks if DBObject is a component object
		for(int i = 0; i < components_cols.length; i++) {
			if(!com_object.containsKey(components_cols[i])) {
				throw new NullPointerException("DBObject is not a component object!");
			}
		}
		setId((String) com_object.get(ssid_col));
		setMqttTopic((String) com_object.get(topic_col));
		setMacAddress((String) com_object.get(mac_col));
		setFunction((String) com_object.get(funct_col));
		setName((String) com_object.get(desc_col));
		setRoom((String) com_object.get(room_col));
	}
	
	/**
	 * Retrieves the .items script representation of this component for OpenHab. Contains all the .items script 
	 * representation of each property of this Component.
	 * 
	 * @return The array of the .items script representation of each property of this Component.
	 */
	public String[] getOHItemScripts(String room, String mqtt_broker, String openhab_topic) {
		Vector<String> str = new Vector<String>(1,1);
		
		for(int i = 0; i < properties.length; i++) {
			Property prop = properties[i];
			str.add(prop.getOHItemScript(this, room, mqtt_broker, openhab_topic));
		}
		
		return str.toArray(new String[str.size()]);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMqttTopic() {
		return mqttTopic;
	}

	public void setMqttTopic(String mqttTopic) {
		this.mqttTopic = mqttTopic;
	}

	/**
	 * @return the macAddress
	 */
	public String getMacAddress() {
		return macAddress;
	}

	/**
	 * @param macAddress the macAddress to set
	 */
	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	/**
	 * @return the function id of the function of the component
	 */
	public String getFunction() {
		return function;
	}

	/**
	 * @param function the function to set
	 */
	public void setFunction(String function) {
		this.function = function;
	}

	/**
	 * @return the description
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param description the description to set
	 */
	public void setName(String description) {
		this.name = description;
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
	 * Returns the specified string enclosed in double quotes. This saves the hassle of doing the enclosure manually.
	 * 
	 * @param str the string needed to be enclosed
	 * @return
	 */
	private String encloseInQuotes(String str) {
		return '"' + str + '"';
	}

	/**
	 * @return the properties
	 */
	public Property[] getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Property[] properties) {
		this.properties = properties;
	}

	/**
	 * Gets the Property object with the name specified.
	 * @param prop_name the name of the Property object
	 * @return the Property object, null if no Property with the specified name is found
	 */
	public Property getProperty(String prop_name) {
		for(int i = 0; i < properties.length; i++) {
			Property p = properties[i];
			if(p.getSystemName().equals(prop_name)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Sets the value of the Property with the specified name
	 * @param prop_name
	 * @param prop_val
	 * @return the Property which changed value, null if no Property with the specified name is found
	 */
	public Property setPropertyValue(String prop_name, Object prop_val) {
		for(int i = 0; i < properties.length; i++) {
			Property p = properties[i];
			if(p.getSystemName().equals(prop_name)) {
				p.setValue(Integer.parseInt((String)prop_val));
				return p;
			}
		}
		return null;
	}
}
