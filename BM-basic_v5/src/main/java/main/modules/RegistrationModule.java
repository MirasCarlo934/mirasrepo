package main.modules;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import components.Component;
import components.Product;
import components.properties.Property;
import json.objects.ReqRegister;
import json.objects.ReqRequest;
import json.objects.ResError;
import json.objects.ResRegister;
import main.ComponentRepository;
import main.engines.DBEngine;
import main.engines.OHEngine;
import main.engines.requests.DBEngine.InsertDBEReq;
import main.engines.requests.DBEngine.RawDBEReq;
import main.engines.requests.DBEngine.SelectDBEReq;
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
	
	public RegistrationModule(String RTY, String nameParam, String prodIDParam, String roomIDParam,
			MQTTHandler mh, ComponentRepository components, DBEngine dbe, OHEngine ohe,
			String productQuery) {
		super("RegistrationModule", RTY, new String[]{nameParam, prodIDParam, roomIDParam}, 
				mh, components);
		this.dbe = dbe;
		this.ohe = ohe;
		this.productQuery = productQuery;
		this.nameParam = nameParam;
		this.prodIDParam = prodIDParam;
		this.roomIDParam = roomIDParam;
	}

	/**
	 * Registers component into system.
	 */
	@Override
	protected void process(ReqRequest request) {
		ReqRegister reg = new ReqRegister(request.getJSON(), nameParam, prodIDParam, roomIDParam);
		LOG.info("Registering component " + reg.mac + " to system...");
		Vector<String> ids = new Vector<String>(1,1);
		Product product = null;
		
		//getting Component product and properties
		try {
			//getting existing Component SSIDs
			LOG.debug("Retrieving existing SSIDs from DB...");
			SelectDBEReq dber1 = new SelectDBEReq(idg.generateMixedCharID(10), 
					"components", new String[]{"ssid"});
			dbe.forwardRequest(dber1, Thread.currentThread());
			try {
				synchronized (Thread.currentThread()){Thread.currentThread().wait();}
			} catch (InterruptedException e) {
				LOG.error("Cannot stop thread!", e);
				e.printStackTrace();
			}
			Object o = dbe.getResponse(dber1.getId());
			if(o.getClass().equals(ResError.class)) {
				error((ResError) o);
			} else {
				ResultSet rs1 = (ResultSet) o;
				while(rs1.next()) {
					ids.add(rs1.getString("ssid"));
				}
				rs1.close();
				LOG.debug("Existing SSIDs retrieved!");
			}
			//getting Component product properties
			LOG.debug("Retrieving Component product properties...");
			RawDBEReq dber2 = new RawDBEReq(idg.generateMixedCharID(10), 
					productQuery + " and cpl.COM_TYPE = '" + reg.cid + "'");
			dbe.forwardRequest(dber2, Thread.currentThread());
			try {
				synchronized (Thread.currentThread()){Thread.currentThread().wait();}
			} catch (InterruptedException e) {
				LOG.error("Cannot stop thread!", e);
				e.printStackTrace();
			}
			o = dbe.getResponse(dber2.getId());
			if(o.getClass().equals(ResError.class)) {
				error((ResError) o);
			} else {
				ResultSet rs2 = (ResultSet) o;
				product = new Product(rs2);
				LOG.debug("Component product properties retrieved!");
			}
		} catch (SQLException e) {
			error(new ResError(reg, "Cannot process register request!"));
			e.printStackTrace();
		}
		
		//creation of Component object within BM system
		LOG.debug("Creating Component object...");
		String ssid = idg.generateMixedCharID(4, ids.toArray(new String[0]));
		String topic = ssid + "_topic";
		Component c = new Component(ssid, reg.mac, reg.name, topic, reg.room, true, product);
		cr.addComponent(c);
		LOG.debug("Component object created!");
		persistComponent(c, request);
		ohe.forwardRequest(new UpdateOHEReq(idg.generateMixedCharID(10)), Thread.currentThread());
		try {
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		
		//publishing of Component credentials to default topic
		LOG.debug("Publishing Component credentials to default topic...");
		mh.publishToDefaultTopic(new ResRegister(request, c.getSSID(), c.getTopic()));
		LOG.debug("Registration complete!");
	}
	
	private void persistComponent(Component c, ReqRequest request) {
		//ReqRegister reg = new ReqRegister(request.getJSON(), nameParam, prodIDParam, roomIDParam);
		LOG.debug("Persisting component into DB...");
		HashMap<String, Object> vals1 = new HashMap<String, Object>(1);
		vals1.put("ssid", c.getSSID());
		vals1.put("topic", c.getTopic());
		vals1.put("mac", c.getMAC());
		vals1.put("name", c.getName());
		vals1.put("room", c.getRoom());
		vals1.put("functn", c.getProduct().getSSID());
		vals1.put("active", c.isActive());
		dbe.forwardRequest(new InsertDBEReq(idg.generateMixedCharID(10), comsTable, vals1), 
				Thread.currentThread());
		try {
			synchronized (Thread.currentThread()){Thread.currentThread().wait();}
		} catch (InterruptedException e) {
			LOG.error("Cannot stop thread!", e);
			e.printStackTrace();
		}
		Property[] props = c.getProperties().values().toArray(new Property[0]);
		LOG.trace("Persisting component properties into DB...");
		for(int i = 0; i < props.length; i++) {
			Property p = props[i];
			HashMap<String, Object> vals2 = new HashMap<String, Object>(1);
			vals2.put("com_id", c.getSSID());
			vals2.put("prop_name", p.getSystemName());
			vals2.put("prop_value", String.valueOf(p.getValue()));
			vals2.put("cpl_ssid", p.getSSID());
			dbe.forwardRequest(new InsertDBEReq(idg.generateMixedCharID(10), propsTable, vals2), 
					Thread.currentThread());
			try {
				synchronized (Thread.currentThread()){Thread.currentThread().wait();}
			} catch (InterruptedException e) {
				LOG.error("Cannot stop thread!", e);
				e.printStackTrace();
			}
		}
		LOG.debug("Component persistence complete!");
		/*try {
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
		}*/
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
		ReqRegister reg = new ReqRegister(request.getJSON(), nameParam, prodIDParam, roomIDParam);
		boolean b = true;
		if(cr.containsComponent(reg.mac)) {
			Component c = cr.getComponent(reg.mac);
			LOG.warn(new ResError(reg, "Component already exists in system! "
					+ "Returning existing credentials."));
			mh.publishToDefaultTopic(new ResRegister(request, c.getSSID(), c.getTopic()));
			return true;
		}
		try {
			RawDBEReq dber3 = new RawDBEReq(idg.generateMixedCharID(10), 
					productQuery + " and cpl.COM_TYPE = '" + reg.cid + "'");
			dbe.forwardRequest(dber3, Thread.currentThread());
			try {
				synchronized (Thread.currentThread()){Thread.currentThread().wait();}
			} catch (InterruptedException e) {
				LOG.error("Cannot stop thread!", e);
				e.printStackTrace();
			}
			Object obj = dbe.getResponse(dber3.getId());
			if(obj.getClass().equals(ResError.class)) {
				ResError error = (ResError) obj;
				error(error);
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
			b = false;
			SelectDBEReq dber4 = new SelectDBEReq(idg.generateMixedCharID(10), 
					"rooms");
			dbe.forwardRequest(dber4, Thread.currentThread());
			try {
				synchronized (Thread.currentThread()){Thread.currentThread().wait();}
			} catch (InterruptedException e) {
				LOG.error("Cannot stop thread!", e);
				e.printStackTrace();
			}
			Object o = dbe.getResponse(dber4.getId());
			if(o.getClass().equals(ResError.class)) {
				error((ResError) o);
			} else {
				ResultSet rs3 = (ResultSet) o;
				while(rs3.next()) {
					//LOG.debug(rs3.getString("ssid"));
					if(reg.room.equals(rs3.getString("ssid")) || 
							reg.room.equalsIgnoreCase(rs3.getString("name"))) {
						b = true;
						request.getJSON().put("roomID", rs3.getString("ssid"));
						rs3.close();
						return true;
					}
				}
				rs3.close();
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
