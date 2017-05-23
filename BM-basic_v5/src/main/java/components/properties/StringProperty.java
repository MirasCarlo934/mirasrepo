package components.properties;

public class StringProperty extends AbstProperty {

	public StringProperty(String propTypeID, String SSID, String comID, String genericName, String dispname, 
			PropertyMode mode) {
		super(propTypeID, SSID, comID, genericName, dispname, mode, PropertyValueType.string);
	}

	/**
	 * Returns the value of this String property
	 * @return The value of this String property
	 */
	@Override
	public String getValue() {
		return value.toString();
	}
}
