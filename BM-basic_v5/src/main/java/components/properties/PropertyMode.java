package components.properties;

public enum PropertyMode {
	I, O, IO;
	
	public static PropertyMode parseModeFromString(String str) {
		if(str.equals("I"))
			return I;
		else if(str.equals("O"))
			return O;
		else if(str.equals("IO"))
			return IO;
		else
			throw new IllegalArgumentException();
	}
}
