package main.configuration;

import java.util.HashMap;

/**
 * ComponentPropertyList documentation
 * 		A ComponentPropertyList is a HashMap containing all the possible properties that a component can have and all the possible property
 * 	values under each property. The keys of this HashMap contains the name of the property while the values contain the Object array
 * 	containing all the possible values of the respective property.
 */
public class ComponentPropertyList extends HashMap<String, Object[]>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2041358005362482737L;

	/**
	 * Instantiates this ComponentPropertyList. Has a dependency of the HashMap list from the Spring config.
	 * 
	 * @param list the HashMap containing all the properties and their possible values.
	 */
	public ComponentPropertyList(HashMap<String, String[]> list) {
		super(1);
		putAll(list);
	}
}
