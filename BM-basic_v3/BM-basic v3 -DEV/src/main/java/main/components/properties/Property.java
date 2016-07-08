package main.components.properties;

import main.components.Component;
import main.components.openhab.OpenhabItem;
import main.objects.DB.DBObject;

public class Property {
	private String id;
	private String systemName;
	private String displayName;
	private PropertyMode mode;
	private int minval;
	private int maxval;
	private int value;
	private OpenhabItem ohItem;

	/**
	 * Creates a Property object
	 * 
	 * @param id the SSID of this property's type
	 * @param systemName the Component-specific name of this property. <i>This is different from the name column in the 
	 * 		SQL table 'PROPCAT'!</i>
	 * @param mode
	 * @param minval
	 * @param maxval
	 * @param default_val The default value of this property <i>(0 by convention)</i>
	 * @param ohItem The openhab item that will represent this property in openhab
	 */
	public Property(String id, String systemName, String displayName, PropertyMode mode, int minval, int maxval, 
			int default_val, OpenhabItem ohItem) {
		setId(id);
		setSystemName(systemName);
		setMode(mode);
		setMinval(minval);
		setMaxval(maxval);
		setValue(default_val);
		setOhItem(ohItem);
		setDisplayName(displayName);
	}
	
	/**
	 * Returns the generic name of this component. The generic name is the name of the property type of this property.
	 * <br><br>
	 * The generic name is retrieved from splitting the system name around the hyphen(-) and getting the first
	 * substring.
	 * 
	 * @return the generic name of this property
	 */
	public String getGenericName() {
		return systemName.split("-")[0];
	}
	
	public String getOHItemScript(Component com, String room, String mqtt_broker, String openhab_topic) {
		return ohItem.getOHItemScript(com, this, room, mqtt_broker, openhab_topic);
	}

	/**
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	private void setId(String id) {
		this.id = id;
	}

	/**
	 *  Component-specific name of this property used to uniquely identify this property in the system-side. The
	 *  system name is derived from the generic property name in the table 'PROPCAT' and the property's index,
	 *  which are both separated by a hyphen (-)<br><br>
	 * 
	 * @return the name
	 */
	public String getSystemName() {
		return systemName;
	}
	
	/**
	 * Returns the index of this property in the Component. <br><br>
	 * 
	 * When properties are named at component registry, properties with similar types are given unique names by adding an
	 * index preceeding their generic property type name.<br><br>
	 * 
	 * For example, a component has three DigitalInput properties. Upon registration, these three properties are given
	 * unique names by giving them the names "DigitalInput-1", "DigitalInput-2", and "DigitalInput-3" respectively. The
	 * generic name and the unique name's index is separated by the delimiter "-".
	 * 
	 * @return the index of this property
	 */
	public String getIndex() {
		return systemName.split("-")[1];
	}

	/**
	 * @param name the name to set
	 */
	private void setSystemName(String name) {
		this.systemName = name;
	}

	/**
	 * @return the mode
	 */
	public PropertyMode getMode() {
		return mode;
	}

	/**
	 * @param mode the mode to set
	 */
	private void setMode(PropertyMode mode) {
		this.mode = mode;
	
}

	/**
	 * Returns the minimum value that this property can accommodate.
	 * 
	 * @return the minval
	 */
	public int getMinval() {
		return minval;
	}

	/**
	 * @param minval the minval to set
	 */
	private void setMinval(int minval) {
		this.minval = minval;
	}

	/**
	 * Returns the maximum value that this property can accommodate.
	 * 
	 * @return the maxval
	 */
	public int getMaxval() {
		return maxval;
	}

	/**
	 * 
	 * @param maxval the maxval to set
	 */
	private void setMaxval(int maxval) {
		this.maxval = maxval;
	}

	/**
	 * @return the value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}

	/**
	 * Returns the openhab item that will represent this property in openhab
	 * 
	 * @return the OpenhabItem enum
	 */
	public OpenhabItem getOhItem() {
		return ohItem;
	}

	/**
	 * @param ohItem the ohItem to set
	 */
	private void setOhItem(OpenhabItem ohItem) {
		this.ohItem = ohItem;
	}

	/**
	 * The name of this property that will be displayed in OpenHab.
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName the displayName to set
	 */
	private void setDisplayName(String displayName) {
		if(displayName.equals("empty")) { //if true, set display name to system name
			this.displayName = systemName;
		} else {
			this.displayName = displayName;
		}
	}
}
