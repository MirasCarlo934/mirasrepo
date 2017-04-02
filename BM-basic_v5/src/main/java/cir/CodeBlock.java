package cir;

public abstract class CodeBlock {
	private String comID;
	private String propSSID;
	private String propValue;
	
	public CodeBlock(String comID, String comProperty, String comValue) {
		setComID(comID);
		setComProperty(comProperty);
		setPropValue(comValue);
	}
	
	/**
	 * Turns this CodeBlock into a CIR Script String.
	 */
	public abstract String toString();

	/**
	 * @return the comID
	 */
	public String getComID() {
		return comID;
	}

	/**
	 * @param comID the comID to set
	 */
	public void setComID(String comID) {
		this.comID = comID;
	}

	/**
	 * @return the comProperty
	 */
	public String getPropSSID() {
		return propSSID;
	}

	/**
	 * @param comProperty the comProperty to set
	 */
	public void setComProperty(String comProperty) {
		this.propSSID = comProperty;
	}

	/**
	 * @return the value of the property
	 */
	public String getPropValue() {
		return propValue;
	}

	/**
	 * @param comValue the comValue to set
	 */
	public void setPropValue(String comValue) {
		this.propValue = comValue;
	}
}
