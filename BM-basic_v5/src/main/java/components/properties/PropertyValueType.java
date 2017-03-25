package components.properties;

public enum PropertyValueType {
	digital, analog, analoghue, percent, string;
	
	/**
	 * Parses a specified String and returns a PropertyValueType if the String is equal
	 * to a PropertyValueType.
	 * @param str The String to be parsed
	 * @return a PropertyValueType. <b><i>Null</i></b> if str is not a valid PropertyValueType
	 */
	public static PropertyValueType parsePropValTypeFromString(String str) {
		PropertyValueType pvt = null;
		if(str.equalsIgnoreCase("digital"))
			pvt = digital;
		else if(str.equalsIgnoreCase("analog"))
			pvt = analog;
		else if(str.equalsIgnoreCase("analogHue"))
			pvt = analoghue;
		else if(str.equalsIgnoreCase("percent"))
			pvt = percent;
		else if(str.equalsIgnoreCase("string"))
			pvt = string;
		
		return pvt;
	}
}
