/**
 * Command documentation
 * 		A subclass of the Response class. The Command response serves as the medium in which the BusinessMachine issues commands to other
 * 	system components. This response is only made up of three parameters: the [response_id], [topic] and the [command] parameters. 
 * 	The value of the [response_id] parameter must be the same as the name of the [command] parameter. The [command] parameter will contain 
 * 	the command issued by the BusinessMachine. While the [topic] parameter will contain the MQTT topic where the command will be sent to.
 * 
 * 		Commands are usually a String composed of two smaller strings separated by the delimiter '!!-'. The first string defines the action 
 * 	the component has to do (eg. 'connect', 'subscribe') while the second string contains the value the component will need in order 
 * 	to perform the command (eg. 'mqtt', 'ESP_755G').
 */
package main.objects.request_response;

import org.json.JSONObject;

import main.TransTechSystem;

public class Command extends Response {
	private String command;
	private String commandValue;
	/*
	 * The following are for easier value manipulation in case they need to be changed.
	 */
	private static final String delimiter = "!!-";
	private static final String topicParamName = "topic";

	/**
	 * Instantiates the Command response. This constructor is typically used by the BusinessMachine to encode its command.
	 * 
	 * @param topic The MQTT topic where the Command will be published to.
	 * @param command The action that will be performed by the receiving component.
	 * @param commandValue The value needed to perform the action. Can be empty.
	 */
	public Command(String topic, String command, String commandValue) {
		super(TransTechSystem.config.getRequestParamConfig().getCommandKey(), "BM", topic);
		setCommand(command);
		setCommandValue(commandValue);
		json.put(TransTechSystem.config.getRequestParamConfig().getCommandKey(), command + delimiter + commandValue);
		json.put(topicParamName, topic);
	}
	
	/**
	 * Instantiates the Command response. This constructor is ONLY used by the receiving component to parse the command sent by the
	 * BusinessMachine.
	 * 
	 * @param json The JSONObject representation of the command.
	 */
	public Command(JSONObject json) {
		super(json.getString(TransTechSystem.config.getRequestParamConfig().getRequestIdKey()), "BM", json.getString(topicParamName));
		String[] com = json.getString(TransTechSystem.config.getRequestParamConfig().getCommandKey()).split(delimiter);
		setCommand(com[0]);
		setCommandValue(com[1]);
	}

	/**
	 * @return the command that will be performed by the receiving component.
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @param command the command to set
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * @return the commandValue needed to perform the action. Can be empty.
	 */
	public String getCommandValue() {
		return commandValue;
	}

	/**
	 * @param commandValue the commandValue to set
	 */
	public void setCommandValue(String commandValue) {
		this.commandValue = commandValue;
	}
}
