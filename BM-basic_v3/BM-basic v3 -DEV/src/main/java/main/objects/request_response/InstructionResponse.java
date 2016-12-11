package main.objects.request_response;

import main.components.Component;

public class InstructionResponse extends Response {
	public static final String RID = "inst";
	
	/**The response parameter for the property name*/
	public static final String propertyName_resparam = "property"; //the property that changed in the requesting component
	/**The response parameter for the property value*/
	public static final String propertyValue_resparam = "value"; //the value needed to perform the action
	
	private Component component;
	private String propertyName;
	private String propertyValue;

	/**
	 * Creates an Instruction Response
	 * 
	 * @param topic the topic of the component where this instruction will be sent to
	 * @param propName the name of the property to be changed
	 * @param propVal the value of the property to be changed
	 * @param com the Component receiving the instruction
	 */
	public InstructionResponse(Component com,  String RTY, String propName, String propVal) {
		super(RID, RTY, com.getMqttTopic());
		setPropertyName(propName);
		setPropertyValue(propVal);
		setComponent(com);
	}

	/**
	 * @return the propertyName
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * @param propertyName the propertyName to set
	 */
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
		put(propertyName_resparam, propertyName);
	}

	/**
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
		put(propertyValue_resparam, propertyValue);
	}

	/**
	 * @return the component
	 */
	public Component getComponent() {
		return component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(Component component) {
		this.component = component;
	}
}
