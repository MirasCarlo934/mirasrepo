package components.properties;

public class CommonProperty extends AbstProperty {
	private int min;
	private int max;
	
	public CommonProperty(String propTypeID, String SSID, String comID, String genericName, String dispname, 
			PropertyMode mode, PropertyValueType propValType, int minValue, int maxValue) {
		super(propTypeID, SSID, comID, genericName, dispname, mode, propValType);
		this.min = (minValue);
		this.max = (maxValue);
	}

	/**
	 * @return The value of this CommonProperty in Integer format
	 */
	@Override
	public Integer getValue() {
		return Integer.parseInt(value.toString());
	}

	/**
	 * Returns the minimum value to be accepted by this CommonProperty
	 * 
	 * @return the minimum value accepted by this CommonProperty
	 */
	public int getMin() {
		return min;
	}

	/**
	 * Returns the maximum value to be accepted by this CommonProperty
	 * 
	 * @return the maximum value accepted by this CommonProperty
	 */
	public int getMax() {
		return max;
	}
}
