package components.bindings;

public class Binding {
	private String SSID;
	private String comType;
	private String propIndex;
	private String binding;
	
	public Binding(String SSID, String comID, String propID, String binding) {
		this.SSID = SSID;
		this.comType = comID;
		this.propIndex = propID;
		this.binding = binding;
	}
	
	public String getSSID() {
		return SSID;
	}

	public String getComType() {
		return comType;
	}

	public String getPropIndex() {
		return propIndex;
	}

	public String getBinding() {
		return binding;
	}
}
