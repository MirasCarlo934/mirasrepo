package main.configuration;

public class Configuration {
	private SystemConfiguration systemConfig;
	private ConnectionConfiguration	connectionConfig;
	private RequestParameterConfiguration requestParamConfig;
	private MqttTopicConfiguration mqttTopicConfig;
	private DatabaseConfiguration databaseConfig;
	private InstructionPropsConfiguration instructionPropsConfig;
	
	public Configuration(SystemConfiguration systemConfiguration, ConnectionConfiguration connectionConfiguration, 
			DatabaseConfiguration databaseConfiguration, MqttTopicConfiguration mqttTopicConfiguration,
			RequestParameterConfiguration requestParameterConfiguration, InstructionPropsConfiguration instructionPropsConfig) {
		setSystemConfig(systemConfiguration);
		setConnectionConfig(connectionConfiguration);
		setDatabaseConfig(databaseConfiguration);
		setMqttTopicConfig(mqttTopicConfiguration);
		setRequestParamConfig(requestParameterConfiguration);
		setInstructionPropsConfig(instructionPropsConfig);
	}

	/**
	 * @return the systemConfig
	 */
	public SystemConfiguration getSystemConfig() {
		return systemConfig;
	}

	/**
	 * @param systemConfig the systemConfig to set
	 */
	public void setSystemConfig(SystemConfiguration systemConfig) {
		this.systemConfig = systemConfig;
	}

	/**
	 * @return the connectionConfig
	 */
	public ConnectionConfiguration getConnectionConfig() {
		return connectionConfig;
	}

	/**
	 * @param connectionConfig the connectionConfig to set
	 */
	public void setConnectionConfig(ConnectionConfiguration connectionConfig) {
		this.connectionConfig = connectionConfig;
	}

	/**
	 * @return the requestParamConfig
	 */
	public RequestParameterConfiguration getRequestParamConfig() {
		return requestParamConfig;
	}

	/**
	 * @param requestParamConfig the requestParamConfig to set
	 */
	public void setRequestParamConfig(RequestParameterConfiguration requestParamConfig) {
		this.requestParamConfig = requestParamConfig;
	}

	/**
	 * @return the mqttTopicConfig
	 */
	public MqttTopicConfiguration getMqttTopicConfig() {
		return mqttTopicConfig;
	}

	/**
	 * @param mqttTopicConfig the mqttTopicConfig to set
	 */
	public void setMqttTopicConfig(MqttTopicConfiguration mqttTopicConfig) {
		this.mqttTopicConfig = mqttTopicConfig;
	}

	/**
	 * @return the databaseConfig
	 */
	public DatabaseConfiguration getDatabaseConfig() {
		return databaseConfig;
	}

	/**
	 * @param databaseConfig the databaseConfig to set
	 */
	public void setDatabaseConfig(DatabaseConfiguration databaseConfig) {
		this.databaseConfig = databaseConfig;
	}

	/**
	 * @return the instructionPropsConfig
	 */
	public InstructionPropsConfiguration getInstructionPropsConfig() {
		return instructionPropsConfig;
	}

	/**
	 * @param instructionPropsConfig the instructionPropsConfig to set
	 */
	public void setInstructionPropsConfig(InstructionPropsConfiguration instructionPropsConfig) {
		this.instructionPropsConfig = instructionPropsConfig;
	}
}
