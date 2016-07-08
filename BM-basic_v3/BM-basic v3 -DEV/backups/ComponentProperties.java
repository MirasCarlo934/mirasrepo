package main.components.properties;

import java.util.HashMap;
import java.util.Map;

import main.TransTechSystem;
import main.components.Component;

public class ComponentProperties {
	private HashMap<String, Object> properties;
	
	/**
	 * Creates a ComponentProperties object that contains all the property parameters of the Component that implements this
	 * ComponentProperties object. <br><br>
	 * 
	 * When initializing a Component, that Component must also specify the properties it holds (eg. "state", "gradient"). The system can
	 * only see and change the values of these specified properties.
	 * 
	 * @param propertyNames the property names, is case-friendly and registers these property names in lowercase
	 * @throws InvalidPropertyException throws an exception if a property name specified is invalid
	 */
	public ComponentProperties(String[] propertyNames) throws InvalidPropertyException {
		properties = new HashMap<String, Object>(1);
		
		for(int i = 0; i < propertyNames.length; i++) {
			String p = propertyNames[i].toLowerCase();
			if(TransTechSystem.configuration.getInstructionPropsConfig().getComponentPropertyList().containsKey(p)) {
				properties.put(p, Component.unset_prop_val);
			}
			else {
				throw new InvalidPropertyException("Invalid property specified!");
			}
		}
	}
	
	/**
	 * Sets the value of the specified property. <br><br>
	 * 
	 * <i><b>If the specified property does not exist, the setting will stop and this method will return false</i></b>
	 * 
	 * @param propertyName the property name, is case-friendly
	 * @param propertyValue
	 * @return true if property setting is successful, false if property does not exist
	 */
	public boolean setProperty(String propertyName, Object propertyValue) {
		boolean b = true;
		if(properties.containsKey(propertyName.toLowerCase())) {
			properties.put(propertyName.toLowerCase(), propertyValue);
		}
		else {
			b = false;
		}
		return b;
	}
	
	/**
	 * Sets the value of the specified property. <br><br>
	 * 
	 * <i><b>If the not all specified properties are initialized, this method will return false.</i></b><br><br>
	 * 
	 * <i>Ignores properties in the 'props' HashMap' that do not exist within the records of this ComponentProperties object.</i>
	 * 
	 * @param props the HashMap containing all the properties
	 * @return true if property setting is successful, false if not all properties were initialized
	 */
	public boolean setProperties(HashMap<String, Object> props) {
		boolean b = true;
		Object[] keys = props.keySet().toArray();
		
		for(int i = 0; i < props.size(); i++) {
			String key = (String)keys[i];
			setProperty(key.toLowerCase(), props.get(key));
		}
		
		if(properties.containsValue("empty")) {
			b = false;
		}
		return b;
	}
	
	/**
	 * Gets the specified property of the component.
	 * @param propertyName the name of the property, is case-friendly
	 * @return the value of the property, "empty" if property is not yet initialized and <i>null</i> if property does not exist
	 */
	public Object getProperty(String propertyName) {
		return properties.get(propertyName.toLowerCase());
	}
	
	public String[] getKeys() {
		String[] keys = new String[properties.size()];
		properties.keySet().toArray(keys);
		return keys;
	}
	
	public int getSize() {
		return properties.size();
	}
}
