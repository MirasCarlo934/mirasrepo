package components;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import components.properties.InnateProperty;
import components.properties.AbstProperty;
import components.properties.CommonProperty;
import components.properties.PropertyMode;
import components.properties.PropertyValueType;
import components.properties.StringProperty;

public class Product {
	private static final Logger LOG = Logger.getLogger("BM_LOG.Product");
	private String SSID;
	private String name;
	private String description;
	private String OH_icon;
	private Hashtable<String, AbstProperty> properties = new Hashtable<String, AbstProperty>(1,1);
	
	/**
	 * The default constructor for the instantiation of the Product object
	 * 
	 * @param ssid The SSID of the product
	 * @param name The name of the product
	 * @param description The description of the product
	 * @param OH_icon The icon used to represent the product in OpenHAB
	 * @param properties The properties that this product possesses
	 */
	public Product(String ssid, String name, String description, String OH_icon, 
			AbstProperty[] properties) {
		this.SSID = ssid;
		this.name = name;
		this.description = description;
		this.OH_icon = OH_icon;
		for(int i = 0; i < properties.length; i++) {
			AbstProperty p = properties[i];
			this.properties.put(p.getSSID(), p);
		}
	}
	
	/**
	 * <i>Defunct.</i>
	 * A constructor used for instantiation of Product object using a ResultSet that contains all 
	 * the properties of a specified product
	 * 
	 * @param rs The ResultSet of the select query containing all the properties of the specified 
	 * product
	 */
	public Product(String comID, ResultSet rs, String stringPropTypeID, String innatePropTypeID) throws SQLException{
		while(rs.next()) {
			SSID = rs.getString("prod_ssid");
			name = rs.getString("prod_name");
			description = rs.getString("prod_desc");
			OH_icon = rs.getString("oh_icon");
			
			String prop_type = rs.getString("prop_type");
			String prop_dispname = rs.getString("prop_dispname");
			String prop_sysname = rs.getString("prop_sysname");
			String prop_mode = rs.getString("prop_mode");
			String pval_type = rs.getString("prop_val_type");
			int prop_min = rs.getInt("prop_min");
			int prop_max = rs.getInt("prop_max");
			String prop_index = rs.getString("prop_index");
			if(prop_type.equals(stringPropTypeID)) {
				StringProperty prop = new StringProperty(prop_type, prop_index, comID, prop_sysname, prop_dispname, 
						PropertyMode.parseModeFromString(prop_mode));
				properties.put(prop.getSSID(), prop);
				//System.out.println("s-" + prop.getDisplayName());
			} else if(prop_type.equals(innatePropTypeID)) {
				InnateProperty prop = new InnateProperty(prop_type, prop_index, comID, prop_sysname, prop_dispname, 
						PropertyValueType.parsePropValTypeFromString(pval_type));
				properties.put(prop.getSSID(), prop);
				//System.out.println("i-" + prop.getDisplayName());
			} else {
				CommonProperty prop = new CommonProperty(prop_type, prop_index, comID, prop_sysname, prop_dispname, 
						PropertyMode.parseModeFromString(prop_mode), 
						PropertyValueType.parsePropValTypeFromString(pval_type), prop_min, prop_max);
				properties.put(prop.getSSID(), prop);
			}
		}
	}
	
	/*public String toString(){
		String s = SSID + ", " +displayName + ", "+description;
		String props = "";
		Enumeration<String> enumKey = properties.keys();
		while(enumKey.hasMoreElements()) {
		    String key = enumKey.nextElement();
		    Property prop = (Property)properties.get(key);
		    props = props + "\n\t ID=" + prop.getPropertyID()+ ","+ key +","+ prop.getType() +"," + prop.getMin() + "," + prop.getMax();
		}
		s = s + props;
		return s;
	}*/
	
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

	/**
	 * @return the oH_icon
	 */
	public String getOH_icon() {
		return OH_icon;
	}

	/**
	 * @param oH_icon the oH_icon to set
	 */
	public void setOH_icon(String oH_icon) {
		OH_icon = oH_icon;
	}
}
