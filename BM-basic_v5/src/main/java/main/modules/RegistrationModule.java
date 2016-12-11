package main.modules;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import devices.Component;
import devices.Product;
import devices.Property;
import json.objects.ReqRegister;
import json.objects.ReqRequest;
import json.objects.ResError;
import json.objects.ResRegister;
import main.ComponentRepository;
import main.engines.DBEngine;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class RegistrationModule extends AbstModule {
	private static String comsTable = ""; //COMPONENTS table
	private static String propsTable = ""; //COMPONENTS table
	private IDGenerator idg = new IDGenerator();
	private DBEngine dbe;
	private String productQuery;
	
	public RegistrationModule(String RTY, MQTTHandler mh, ComponentRepository components, DBEngine dbe, String productQuery) {
		super("RegistrationModule", RTY, new String[]{"name", "prodID", "roomID"}, mh, components);
		this.dbe = dbe;
		this.productQuery = productQuery;
	}

	/**
	 * Registers component into system.
	 */
	@Override
	protected void process(ReqRequest request) {
		ReqRegister reg = new ReqRegister(request.getJSON());
		LOG.info("Registering component " + reg.mac + " to system...");
		Vector<String> ids = new Vector<String>(1,1);
		Product product = null;
		try {
			ResultSet rs1 = dbe.selectQuery("ssid", "components");
			while(rs1.next()) {
				ids.add(rs1.getString("ssid"));
			}
			ResultSet rs2 = dbe.executeQuery(productQuery + " and cpl.COM_TYPE = '" + reg.cid + "'");
			product = new Product(rs2);
		} catch (SQLException e) {
			error(new ResError(reg.rid, reg.cid, "Cannot process register request!"));
			e.printStackTrace();
		}
		String ssid = idg.generateMixedCharID(4, ids.toArray(new String[0]));
		String topic = ssid + "_topic";
		Component c = new Component(ssid, reg.mac, reg.name, topic, reg.room, true, product);
		cr.addComponent(c);
		persistComponent(c, request);
		mh.publishToDefaultTopic(new ResRegister(request, c.getSSID(), c.getTopic()));
		LOG.info("Registration complete!");
	}
	
	private void persistComponent(Component c, ReqRequest request) {
		ReqRegister reg = new ReqRegister(request.getJSON());
		LOG.debug("Persisting component into DB...");
		HashMap<String, Object> vals1 = new HashMap<String, Object>(1);
		vals1.put("ssid", c.getSSID());
		vals1.put("topic", c.getTopic());
		vals1.put("mac", c.getMAC());
		vals1.put("name", c.getName());
		vals1.put("room", c.getRoom());
		vals1.put("functn", c.getProduct().getSSID());
		vals1.put("active", c.isActive());
		try {
			dbe.insertQuery(comsTable, vals1);
			Property[] props = c.getProperties().values().toArray(new Property[0]);
			for(int i = 0; i < props.length; i++) {
				Property p = props[i];
				HashMap<String, Object> vals2 = new HashMap<String, Object>(1);
				vals2.put("com_id", c.getSSID());
				vals2.put("prop_name", p.getSystemName());
				vals2.put("prop_value", String.valueOf(p.getValue()));
				vals2.put("cpl_ssid", p.getIndex());
				dbe.insertQuery(propsTable, vals2);
			}
			LOG.debug("Component persistence complete!");
		} catch (SQLException e) {
			error(new ResError(reg.rid, reg.cid, "Cannot persist component!"));
			e.printStackTrace();
		}
	}

	/**
	 * Checks for the following deficiencies in the request:
	 * <ul>
	 * 	<li>CID already exists</li>
	 * 	<li>Invalid product ID</li>
	 * 	<li>Invalid room ID</li>
	 * </ul>
	 */
	@Override
	protected boolean additionalRequestChecking(ReqRequest request) {
		LOG.trace("Additional secondary request parameter checking...");
		ReqRegister reg = new ReqRegister(request.getJSON());
		boolean b = true;
		if(cr.containsDevice(reg.mac)) {
			error(new ResError(reg.rid, reg.cid, "Component already exists in system!"));
			return false;
		}
		try {
			ResultSet rs2 = dbe.executeQuery(productQuery + " and cpl.COM_TYPE = '" + reg.cid + "'");
			if (!rs2.isBeforeFirst() ) {    
			    error(new ResError(reg.rid, reg.cid, "Product ID is invalid!"));
			    return false;
			}
			
			ResultSet rs3 = dbe.selectQuery("ssid", "rooms");
			b = false;
			while(rs3.next()) {
				if(reg.room.equals(rs3.getString("ssid"))) {
					b = true;
					return true;
				}
			}
			if(!b) {
				LOG.error("Room ID is invalid!");
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return b;
	}

	public void setComsTable(String comsTable) {
		RegistrationModule.comsTable = comsTable;
	}
	
	public void setPropsTable(String propsTable) {
		RegistrationModule.propsTable = propsTable;
	}
}
