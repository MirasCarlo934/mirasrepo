package components;

import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import components.properties.AbstProperty;

public abstract class AbstProduct {
	private static final Logger LOG = Logger.getLogger("BM_LOG.Product");
	private String SSID;
	private String name;
	private String description;
	private Hashtable<String, AbstProperty> properties;
	
	public AbstProduct() {
		properties = new Hashtable<String, AbstProperty>() ;
	}
	
	public String toString(){
		String s = SSID + ", " +name + ", "+description;
		String props = "";
		Enumeration<String> enumKey = properties.keys();
		while(enumKey.hasMoreElements()) {
		    String key = enumKey.nextElement();
		    AbstProperty prop = (AbstProperty)properties.get(key);
		    //props = props + "\n\t ID=" + prop.getPropertyID()+ ","+ key +","+prop.getType() +"," + prop.getMin() + "," + prop.getMax();
		}
		s = s + props;
		return s;
	}
	
	public void addProperty(AbstProperty prop) {
		properties.put(prop.getSSID(), prop);
	}
	
	public AbstProperty getProperty(String name) {
		return properties.get(name);
	}
	
	public Hashtable<String, AbstProperty> getProperties() {
		return properties;
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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
