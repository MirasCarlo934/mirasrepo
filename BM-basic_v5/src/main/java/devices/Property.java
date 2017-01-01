package devices;

public class Property {
	private String id;
	private String displayName;
	private String systemName; //[generic name]-[index]
	private String index;
	private PropertyMode mode;
	private int min;
	private int max;
	private int value = 0;
	
	public Property(String propertyID, String index, String genericName, String dispname, String mode, int minValue, int maxValue) {
		this.setSSID(index);
		this.setDisplayName(dispname);
		this.setSystemName(genericName, index);
		this.setIndex(index);
		this.setMode(PropertyMode.parseModeFromString(mode));
		this.setMin(minValue);
		this.setMax(maxValue);
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
	 * @return the name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param name the name to set
	 */
	public void setDisplayName(String name) {
		this.displayName = name;
	}

	/**
	 * @return the min
	 */
	public int getMin() {
		return min;
	}

	/**
	 * @param min the min to set
	 */
	public void setMin(int min) {
		this.min = min;
	}

	/**
	 * @return the max
	 */
	public int getMax() {
		return max;
	}

	/**
	 * @param max the max to set
	 */
	public void setMax(int max) {
		this.max = max;
	}

	/**
	 * Returns the generic SSID of the property in COMPROPLIST
	 * @return the propertyID
	 */
	public String getSSID() {
		return id;
	}

	/**
	 * @param propertyID the propertyID to set
	 */
	public void setSSID(String propertyID) {
		this.id = propertyID;
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
	public void setMode(PropertyMode mode) {
		this.mode = mode;
	}

	/**
	 * @return the systemName
	 */
	public String getSystemName() {
		return systemName;
	}

	/**
	 * @param systemName the systemName to set
	 * @param index the index set in table COMPROPLIST
	 */
	public void setSystemName(String systemName, String index) {
		this.systemName = systemName + "-" + index;
	}

	/**
	 * @return the index (also the SSID of the property in comproplist)
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * @param index the index to set
	 */
	public void setIndex(String index) {
		this.index = index;
	}
}
