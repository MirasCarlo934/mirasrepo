package devices;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

public class Product {
	private static final Logger LOG = Logger.getLogger("BM_LOG.Product");
	private String SSID;
	private String name;
	private String description;
	private Hashtable<String, Property> properties = new Hashtable<String, Property>(1,1);
	
	public Product(String ssid, String name, String description) {
		
	}
	
	/**
	 * The default constructor used for instantiation of Product object.
	 * 
	 * @param rs The ResultSet of the select query containing all the relevant data for the Product object
	 */
	public Product(ResultSet rs) throws SQLException{
		while(rs.next()) {
			SSID = rs.getString("prod_ssid");
			name = rs.getString("prod_name");
			description = rs.getString("prod_desc");
			
			String prop_ID = rs.getString("prop_SSID");
			String prop_dispname = rs.getString("prop_dispname");
			String prop_sysname = rs.getString("prop_sysname");
			String prop_mode = rs.getString("prop_mode");
			int prop_min = rs.getInt("prop_min");
			int prop_max = rs.getInt("prop_max");
			String prop_index = rs.getString("prop_index");
			Property prop = new Property(prop_ID, prop_index, prop_sysname, prop_dispname, prop_mode, prop_min, prop_max);
			properties.put(prop.getIndex(), prop);
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
	
	public void addProperty(Property prop) {
		properties.put(prop.getPropertyID(), prop);
	}
	
	public Property getProperty(String name) {
		return properties.get(name);
	}
	
	public Hashtable<String, Property> getProperties() {
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
