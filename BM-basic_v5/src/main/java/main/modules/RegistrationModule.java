package main.modules;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import components.Component;
import components.Product;
import components.properties.Property;
import json.objects.ReqRegister;
import json.objects.ReqRequest;
import json.objects.ResError;
import json.objects.ResPOOP;
import json.objects.ResRegister;
import main.ComponentRepository;
import main.engines.DBEngine;
import main.engines.OHEngine;
import main.engines.requests.DBEngine.InsertDBEReq;
import main.engines.requests.DBEngine.RawDBEReq;
import main.engines.requests.DBEngine.SelectDBEReq;
import main.engines.requests.DBEngine.UpdateDBEReq;
import main.engines.requests.OHEngine.UpdateOHEReq;
import mqtt.MQTTHandler;
import tools.IDGenerator;

public class RegistrationModule extends AbstModule {
	private static String comsTable = ""; //COMPONENTS table
	private static String propsTable = ""; //COMPONENTS table
	private IDGenerator idg = new IDGenerator();
	private DBEngine dbe;
	private OHEngine ohe;
	private String productQuery;
	private String nameParam;
	private String prodIDParam;
	private String roomIDParam;
	private String poopRTY;
	
	public RegistrationModule(String logDomain, String errorLogDomain, String RTY, String poopRTY, 
			String nameParam, String prodIDParam, String roomIDParam, MQTTHandler mh, 
			ComponentRepository components, DBEngine dbe, OHEngine ohe,String productQuery) {
		super(logDomain, errorLogDomain, "RegistrationModule", RTY, new String[]{nameParam, prodIDParam, roomIDParam}, 
				mh, components);
		this.dbe = dbe;
		this.ohe = ohe;
		this.productQuery = productQuery;
		this.nameParam = nameParam;
		this.prodIDParam = prodIDParam;
		this.roomIDParam = roomIDParam;
		this.poopRTY = poopRTY;
	}

	/**
	 * Registers component into system.
	 */
	@Override
	protected void process(ReqRequest request) {
		ReqRegister reg = new ReqRegister(request.getJSON(), nameParam, prodIDParam, roomIDParam);
		if(request.getJSON().has("exists")) {
			Component c = cr.getComponent(reg.mac);
			request.cid = c.getSSID();
			mainLOG.info("Component already exists in system as " + c.getSSID() + "! "
					+ "Returning existing credentials and property states.");
			
			mainLOG.debug("Activating component in BM...");
			c.setActive(true);
			
			mainLOG.debug("Returning existing credentials to default topic...");
			mh.publishToDefaultTopic(new ResRegister(request, c.getSSID(), c.getTopic()));
			
			mainLOG.debug("Activating component " + c.getSSID() + " in DB...");
			HashMap<String, Object> args = new HashMap<String, Object>(1,1);
			args.put("mac", request.rid);
			HashMap<String, Object> vals = new HashMap<String, Object>(1,1);
			vals.put("active", true);
			UpdateDBEReq updateActive = new UpdateDBEReq(idg.generateMixedCharID(10), comsTable, 
					vals, args);
			Object o1 = forwardEngineRequest(dbe, updateActive);
			if(o1.getClass().equals(ResError.class)) {
				ResError error = (ResError) o1;
				error(error);
				return;
			}
			
			mainLOG.debug("Activating component in OH...");
			Object o2 = forwardEngineRequest(ohe, new UpdateOHEReq(idg.generateMixedCharID(10)));
			if(o2.getClass().equals(ResError.class)) {
				ResError error = (ResError) o2;
				error(error);
				return;
			}
			
			mainLOG.debug("Returning component properties states...");
			Iterator<Property> props = c.getProperties().values().iterator();
			while(props.hasNext()) {
				Property p = props.next();
				ResPOOP poop = new ResPOOP(request, p.getSSID(), p.getValue());
				poop.rty = poopRTY;
				mh.publish(poop);
				mh.publish("openhab/" + c.getTopic(), poop.getPropSSID() + "_" + poop.getPropVal());
			}
			mainLOG.info("Registration complete!");
			return;
		}
		
		mainLOG.info("Registering component " + reg.mac + " to system...");
		Vector<String> ids = new Vector<String>(1,1);
		Product product = null;
		
		//getting Component product and properties
		try {
			//getting existing Component SSIDs
			mainLOG.debug("Retrieving existing SSIDs from DB...");
			SelectDBEReq dber1 = new SelectDBEReq(idg.generateMixedCharID(10), 
					"components", new String[]{"ssid"});
			Object o = forwardEngineRequest(dbe, dber1);
			if(!o.getClass().equals(ResError.class)) {
				ResultSet rs1 = (ResultSet) o;
				while(rs1.next()) {
					ids.add(rs1.getString("ssid"));
				}
				rs1.close();
				mainLOG.debug("Existing SSIDs retrieved!");
			}
			//getting Component product properties
			mainLOG.debug("Retrieving Component product properties...");
			RawDBEReq dber2 = new RawDBEReq(idg.generateMixedCharID(10), 
					productQuery + " and cpl.COM_TYPE = '" + reg.cid + "'");
			o = forwardEngineRequest(dbe, dber2);
			if(!o.getClass().equals(ResError.class)) {
				ResultSet rs2 = (ResultSet) o;
				product = new Product(rs2);
				mainLOG.debug("Component product properties retrieved!");
			}
		} catch (SQLException e) {
			error(new ResError(reg, "Cannot process register request!"));
			e.printStackTrace();
			return;
		}
		
		//creation of Component object within BM system
		mainLOG.debug("Creating Component object...");
		String ssid = idg.generateMixedCharID(4, ids.toArray(new String[0]));
		String topic = ssid + "_topic";
		Component c = new Component(ssid, reg.mac, reg.name, topic, reg.room, true, product);
		mainLOG.debug("Component object created!");
		if(persistComponent(c, request)) {
			cr.addComponent(c);
		}
		else {
			mainLOG.error("Insert to DB error! Registration failed!");
			return;
		}
		
		//updates OH
		forwardEngineRequest(ohe, new UpdateOHEReq(idg.generateMixedCharID(10)));
		
		//publishing of Component credentials to default topic
		mainLOG.debug("Publishing Component credentials to default topic...");
		mh.publishToDefaultTopic(new ResRegister(request, c.getSSID(), c.getTopic()));
		mainLOG.debug("Registration complete!");
	}
	
	private boolean persistComponent(Component c, ReqRequest request) {
		//persisting component credentials to DB
		mainLOG.debug("Persisting component into DB...");
		HashMap<String, Object> vals1 = new HashMap<String, Object>(1);
		vals1.put("ssid", c.getSSID());
		vals1.put("topic", c.getTopic());
		vals1.put("mac", c.getMAC());
		vals1.put("name", c.getName());
		vals1.put("room", c.getRoom());
		vals1.put("functn", c.getProduct().getSSID());
		vals1.put("active", c.isActive());
		String vals1ID = idg.generateMixedCharID(10);
		InsertDBEReq dber1 = new InsertDBEReq(vals1ID, comsTable, vals1);
		Object o = forwardEngineRequest(dbe, dber1);
		if(o.getClass().equals(ResError.class)) {
			return false;
		}
		
		//persisting component properties to DB
		Property[] props = c.getProperties().values().toArray(new Property[0]);
		mainLOG.trace("Persisting component properties into DB...");
		for(int i = 0; i < props.length; i++) {
			Property p = props[i];
			HashMap<String, Object> vals2 = new HashMap<String, Object>(1);
			vals2.put("com_id", c.getSSID());
			vals2.put("prop_name", p.getSystemName());
			vals2.put("prop_value", String.valueOf(p.getValue()));
			vals2.put("cpl_ssid", p.getSSID());
			String vals2ID = idg.generateMixedCharID(10);
			
			Object o2 = forwardEngineRequest(dbe, new InsertDBEReq(vals2ID, propsTable, vals2));
			if(o2.getClass().equals(ResError.class)) {
				return false;
			}
		}
		mainLOG.debug("Component persistence complete!");
		return true;
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
		mainLOG.trace("Additional secondary request parameter checking...");
		ReqRegister reg = new ReqRegister(request.getJSON(), nameParam, prodIDParam, roomIDParam);
		boolean b = true;
		
		mainLOG.trace("Checking MAC validity...");
		if(cr.containsComponent(reg.mac) || cr.containsComponentWithName(reg.name)) {
			request.getJSON().put("exists", true);
			return true;
		}
		try {
			mainLOG.trace("Checking productID validity...");
			RawDBEReq dber3 = new RawDBEReq(idg.generateMixedCharID(10), 
					productQuery + " and cpl.COM_TYPE = '" + reg.cid + "'");
			Object obj = forwardEngineRequest(dbe, dber3);
			if(obj.getClass().equals(ResError.class)) {
				return false;
			}
			ResultSet rs2 = (ResultSet) obj;
			if (!rs2.isBeforeFirst() ) {    
			    error(new ResError(reg, "Product ID is invalid!"));
			    rs2.close();
			    return false;
			}
			rs2.close();
			
			//ResultSet rs3 = dbe.selectQuery("ssid", "rooms");
			mainLOG.trace("Checking roomID validity...");
			b = false;
			SelectDBEReq dber4 = new SelectDBEReq(idg.generateMixedCharID(10), 
					"rooms");
			Object o = forwardEngineRequest(dbe, dber4);
			if(o.getClass().equals(ResError.class)) {
				error((ResError) o);
			} else {
				ResultSet rs3 = (ResultSet) o;
				while(rs3.next()) {
					//LOG.debug(rs3.getString("ssid"));
					if(reg.room.equals(rs3.getString("ssid")) || 
							reg.room.equalsIgnoreCase(rs3.getString("name"))) {
						b = true;
						mainLOG.fatal(rs3.getString("ssid"));
						request.getJSON().put(roomIDParam, rs3.getString("ssid"));
						rs3.close();
						break;
					}
				}
				rs3.close();
			}
			if(!b) {
				mainLOG.error("Room ID is invalid!");
				return false;
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
