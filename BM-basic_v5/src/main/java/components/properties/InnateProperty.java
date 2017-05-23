package components.properties;

public class InnateProperty extends AbstProperty {

	public InnateProperty(String propTypeID, String SSID, String comID,  String genericName, String dispname,
			PropertyValueType propValType) {
		super(propTypeID, SSID, comID, genericName, dispname, PropertyMode.Null, propValType);
	}
}
