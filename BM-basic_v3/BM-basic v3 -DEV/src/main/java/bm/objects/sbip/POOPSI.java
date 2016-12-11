package bm.objects.sbip;

import tools.Date;
import tools.Time;

public class POOPSI extends ScheduledInstruction {
	private String propertyName;
	private String propertyValue;

	public POOPSI(String id, Date date, Time time, String componentID, boolean periodical, String propertyName, String propertyValue) {
		super(id, date, time, SBIPAction.POOP, componentID, periodical);
		setPropertyName(propertyName);
		setPropertyValue(propertyValue);
	}

	/**
	 * The name of the property that will be changed when the set schedule is met.
	 * @return the propertyName
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * 
	 * @param propertyName the propertyName to set
	 */
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * The value of the property that will be changed when the set schedule is met.
	 * @return the propertyValue
	 */
	public String getPropertyValue() {
		return propertyValue;
	}

	/**
	 * @param propertyValue the propertyValue to set
	 */
	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}
}
