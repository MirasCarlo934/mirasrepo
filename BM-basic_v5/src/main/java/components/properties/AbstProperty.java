package components.properties;

import java.util.HashMap;

import org.apache.log4j.Logger;

import json.RRP.ReqPOOP;
import json.RRP.ResError;
import json.RRP.ResPOOP;
import main.engines.DBEngine;
import main.engines.requests.DBEngine.UpdateDBEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public abstract class AbstProperty {
	protected String SSID;
	private String comID;
	protected String displayName;
	protected String systemName; //[generic name]-[cpl_SSID]
	//private String index;
	protected PropertyMode mode;
	protected PropertyValueType propValType;
	protected String propTypeID;
	protected Object value = null;
	
	public AbstProperty(String propTypeID, String SSID, String comID, String genericName, String dispname, 
			PropertyMode mode, PropertyValueType propValType) {
		this.SSID = (SSID);
		this.comID = (comID);
		this.displayName = (dispname);
		this.setSystemName(genericName, SSID);
		//this.setIndex(index);
		this.mode = (mode);
		this.propValType = (propValType);
		this.propTypeID = (propTypeID);
	}
	
	/**
	 * Persists the property value to DB. This method is used primarily for POOP functions.
	 * 
	 * @param dbe The DBEngine that will handle the persistence
	 * @param propsTable The table name where this property value will be persisted to
	 * @param parentLoggerDomain The log4j logging domain used by the Object that invokes this method
	 */
	public void persistPropertyValueToDB(DBEngine dbe, String propsTable, String parentLoggerDomain) 
			throws Exception {
		Logger LOG = Logger.getLogger(parentLoggerDomain + "." + comID + "_" + SSID);
		IDGenerator idg = new IDGenerator();
		LOG.debug("Persisting property value to DB...");
		Thread t = Thread.currentThread();
		HashMap<String, Object> vals = new HashMap<String, Object>(1, 1);
		HashMap<String, Object> args = new HashMap<String, Object>(2, 1);
		vals.put("prop_value", value);
		args.put("com_id", comID);
		args.put("cpl_ssid", SSID);
		UpdateDBEReq udber = new UpdateDBEReq(idg.generateMixedCharID(10), propsTable, vals, args);
		dbe.processRequest(udber, t);
		try {
			synchronized(t) {t.wait();}
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		Object o = dbe.getResponse(udber.getId());
		if(o.getClass().equals(ResError.class)) {
			ResError error = (ResError) o;
			LOG.error("Cannot persist property value!");
			throw new Exception(error.message);
		}
		LOG.debug("Persistence complete!");
	}
	
	/**
	 * Publishes the property value to the specified component (CID). This method is called <b>ONLY</b>
	 * by the POOPModule after a successful property value change.
	 * 
	 * @param mh The MQTTHandler which will handle the publishing
	 * @param RID The RID of the response that will be published
	 * @param CID The CID of the component where the response will be published
	 * @param poopRTY The RTY designation for POOP requests
	 */
	public void publishPropertyValueToMQTT(MQTTHandler mh, String RID, String CID, String poopRTY) {
		ResPOOP response = new ResPOOP(RID, CID, poopRTY, SSID, value);
		mh.publish(response);
	}

	/**
	 * Returns the value of this Property. Is usually overridden by child classes of the AbstProperty 
	 * to return the value with the adequate data type.
	 * 
	 * @return The value of this Property.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Sets the value of this property.
	 * 
	 * @param value The value of the Property to be set
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	
	/**
	 * Sets the value of this property. Also persists this new property value to the DB. Used primarily
	 * for POOP functions.
	 * 
	 * @param value The value of the property to be set
	 * @param dbe The DBEngine that will handle the persistence
	 * @param propsTable The table name where this property value will be persisted to
	 * @param parentLoggerDomain The log4j logging domain used by the Object that invokes this method
	 * @throws Exception thrown when property value cannot be persisted
	 */
	public void setValue(Object value, DBEngine dbe, String propsTable, String parentLoggerDomain) 
			throws Exception {
		this.value = value;
		persistPropertyValueToDB(dbe, propsTable, parentLoggerDomain);
	}

	/**
	 * Returns the display name of this Property. Used primarily in OpenHAB and other UI.
	 * 
	 * @return the display name of this Property
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the SSID of the property as stated in COMPROPLIST
	 * 
	 * @return the propertyID
	 */
	public String getSSID() {
		return SSID;
	}

	/**
	 * Returns the mode of this Property. Usually denotes if the property is an input or output.
	 * 
	 * @return the PropertyMode that represents the mode of this property
	 */
	public PropertyMode getMode() {
		return mode;
	}


	/**
	 * Returns the system name of this Property. <br><br>
	 * 
	 * <b>Construction:</b><br>
	 * [<i>genericName</i>]-[<i>SSID</i>]
	 * 
	 * @return the system name of this Property
	 */
	public String getSystemName() {
		return systemName;
	}

	/**
	 * @param systemName the systemName to set
	 * @param index the index set in table COMPROPLIST
	 */
	private void setSystemName(String systemName, String index) {
		this.systemName = systemName + "-" + index;
	}

	/**
	 * @return the index (also the SSID of the property in comproplist)
	 */
	/*public String getIndex() {
		return index;
	}*/

	/**
	 * @param index the index to set
	 */
	/*public void setIndex(String index) {
		this.index = index;
	}*/

	/**
	 * Returns the data type of the value held by this Property.
	 * 
	 * @return the PropertyValueType that represents the data type of the value held by this Property
	 */
	public PropertyValueType getPropValType() {
		return propValType;
	}

	/**
	 * Returns the property type of this Property denoted in PROPCAT table
	 * 
	 * @return the property type of this Property
	 */
	public String getPropTypeID() {
		return propTypeID;
	}

	/**
	 * Returns the Component SSID that owns this property
	 * 
	 * @return the Component SSID that owns this property
	 */
	public String getComID() {
		return comID;
	}
}
