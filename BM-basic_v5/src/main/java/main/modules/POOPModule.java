package main.modules;

import java.sql.SQLException;
import java.util.HashMap;

import devices.Component;
import devices.Property;
import json.objects.ReqPOOP;
import json.objects.ReqRequest;
import json.objects.ResError;
import main.ComponentRepository;
import main.engines.DBEngine;
import mqtt.MQTTHandler;

public class POOPModule extends AbstModule {
	private DBEngine dbe;
	private String propsTable = ""; //PROPERTIES table

	public POOPModule(String RTY, MQTTHandler mh, ComponentRepository cr, DBEngine dbe) {
		super("POOPModule", RTY, new String[]{"property", "value"}, mh, cr);
		this.dbe = dbe;
	}

	@Override
	protected void process(ReqRequest request) {
		ReqPOOP poop = new ReqPOOP(request);
		LOG.info("Changing component " + request.cid + " property " + poop.propIndex + " to " + poop.propValue + "...");
		updateSystem(poop);
		updateDatabase(poop);
		LOG.info("POOP processing complete!");
	}
	
	private void updateSystem(ReqPOOP poop) {
		LOG.debug("Updating component property in system...");
		Component c = cr.getComponent(poop.cid);
		c.setPropertyValue(poop.propIndex, poop.propValue);
	}
	
	private void updateDatabase(ReqPOOP poop) {
		HashMap<String, Object> args = new HashMap<String, Object>(2);
		args.put("com_id", poop.cid);
		args.put("cpl_ssid", poop.propIndex);
		HashMap<String, Object> vals = new HashMap<String, Object>(1);
		vals.put("prop_value", String.valueOf(poop.propValue));
		try {
			LOG.debug("Updating component property in DB...");
			dbe.updateQuery(propsTable, args, vals);
		} catch (SQLException e) {
			LOG.error("Cannot update component property in DB!");
			e.printStackTrace();
		}
	}

	/**
	 * Checks if request follows the ff:
	 * <ul>
	 * 	<li>Component with CID has the specified property</li>
	 * 	<li>Value specified is valid for the property</li>
	 * </ul>
	 */
	@Override
	protected boolean additionalRequestChecking(ReqRequest request) {
		boolean b = false;
		ReqPOOP poop = new ReqPOOP(request.getJSON());
		
		Component c = cr.getComponent(poop.cid);
		if(c.getProperty(poop.propIndex) != null) {
			Property p = c.getProperty(poop.propIndex);
			if(p.getMin() <= poop.propValue && p.getMax() >= poop.propValue) {
				b = true;
			}
			else {
				ResError re = new ResError(poop.rid, poop.cid, "Property value not within min/max of the property!");
				error(re);
			}
		}
		else {
			error(new ResError(poop.rid, poop.cid, "Property does not exist!"));
		}
		return b;
	}

	public void setPropsTable(String s) {
		this.propsTable = s;
	}
}
