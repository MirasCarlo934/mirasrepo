package main.components.properties;

public enum PropertyMode {
	I, O, IO;
	
	/**
	 * Parses the given string to a PropertyMode
	 * 
	 * @param str the string to be parsed
	 * @throws IllegalArgumentException when the string specified is not I, O, or IO.
	 * @return
	 */
	public static PropertyMode parseMode(String str) throws IllegalArgumentException {
		PropertyMode[] modes = PropertyMode.values();
		PropertyMode m = null;
		for(int i = 0; i < modes.length; i++) {
			PropertyMode mode = modes[i];
			if(str.equalsIgnoreCase(mode.toString())) {
				m =  mode;
				break;
			}
		}
		
		if(m != null) {
			return m;
		} else {
			throw new IllegalArgumentException("String specified is not a PropertyMode!");
		}
		/*if(str.equals("I"))
			return I;
		else if(str.equals("O"))
			return O;
		else if(str.equals("IO"))
			return IO;
		else
			throw new IllegalArgumentException("String specified is not I, O, or IO!");*/
	}
}
