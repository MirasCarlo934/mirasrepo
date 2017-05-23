package main.engines.requests.CIREngine;

import components.Component;
import components.properties.AbstProperty;

public class GetStatementsCIREReq extends CIREngineRequest {
	private Component component = null;
	private AbstProperty property = null;
	private boolean getAll = false;

	/**
	 * Retrieves all CIR statements related with the specified component and property.
	 * 
	 * @param id
	 * @param component The specified Component
	 * @param property The specified Property
	 */
	public GetStatementsCIREReq(String id, Component component, AbstProperty property) {
		super(id, CIRRequestType.getStatements);
		setComponent(component);
		setProperty(property);
	}
	
	/**
	 * Retrieves all CIR statements from the CIR file.
	 * 
	 * @param id
	 */
	public GetStatementsCIREReq(String id) {
		super(id, CIRRequestType.getStatements);
		setGetAll(true);
	}

	public Component getComponent() {
		return component;
	}

	private void setComponent(Component component) {
		this.component = component;
	}

	public AbstProperty getProperty() {
		return property;
	}

	private void setProperty(AbstProperty property) {
		this.property = property;
	}

	public boolean isGetAll() {
		return getAll;
	}

	private void setGetAll(boolean getAll) {
		this.getAll = getAll;
	}
}
