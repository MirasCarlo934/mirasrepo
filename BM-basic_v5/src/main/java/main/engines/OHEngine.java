package main.engines;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

import components.Component;
import components.bindings.Binding;
import components.properties.Property;
import components.properties.PropertyValueType;
import json.objects.ResError;
import main.ComponentRepository;
import main.engines.requests.EngineRequest;
import main.engines.requests.DBEngine.SelectDBEReq;
import main.engines.requests.OHEngine.OHEngineRequest;
import main.engines.requests.OHEngine.OHRequestType;
import main.engines.requests.OHEngine.UpdateOHEReq;
import mqtt.MQTTHandler;
import tools.FileHandler;
import tools.IDGenerator;
import tools.StringTools;

/**
 * Handles all OpenHab related interactions for the BM. <br><br>
 * 
 * <b>NOTE:</b> This OHEngine version is only for OpenHab-2.0.0 only! Check backups for 
 * more primitive versions.
 * @author Carlo
 *
 */
public class OHEngine extends AbstEngine {
	//private static final Logger LOG = Logger.getLogger("BM_LOG.OHEngine");
	private ComponentRepository cr;
	private String os;
	private String OHMqttBroker;
	private String oh_location;
	private String items_filename;
	private String rules_filename;
	private String sitemap_filename;
	private String sitemap_name;
	private FileHandler items;
	private FileHandler sitemap;
	private FileHandler rules;
	private HashMap<String, String> itemsList;
	//private IDGenerator idg = new IDGenerator();
	private OHEngineRequest oher = null;

	public OHEngine(String os, String oh_filepath, String items_filename, String sitemap_filename, 
			String rules_filename, String sitemap_name, ComponentRepository cr, 
			HashMap<String, String> itemsList, String OHMqttBroker) {
		super("OHEngine", OHEngine.class.toString());
		this.oh_location = oh_filepath;
		this.items_filename = items_filename;
		this.sitemap_filename = sitemap_filename;
		this.rules_filename = rules_filename;
		this.sitemap_name = sitemap_name;
		this.cr = cr;
		this.os = os;
		this.itemsList = itemsList;
		this.OHMqttBroker = OHMqttBroker;
		/*try {
			LOG.info("Connecting to bm.properties...");
			FileHandler fh = new FileHandler(bm_props_filepath);
			bm_props.load(fh.getFileReader());
			LOG.debug("Connected to bm.properties!");
		} catch (FileNotFoundException e) {
			LOG.error("Cannot open bm.properties!", e);
			e.printStackTrace();
		} catch (IOException e) {
			LOG.error("Cannot open bm.properties!", e);
			e.printStackTrace();
		}*/
	}

	@Override
	protected Object processRequest(EngineRequest er) {
		oher = (OHEngineRequest) er;
		
		if(oher.getType() == OHRequestType.start) {
			//startOH();
			connectToFiles();
			updateItems();
			updateRules();
			updateSitemap();
			return oher;
		}
		else if(oher.getType() == OHRequestType.stop) {
			//stopOH();
			return oher;
		}
		else if(oher.getType() == OHRequestType.update) {
			LOG.info("Updating OpenHAB files...");
			updateItems();
			updateRules();
			updateSitemap();
			LOG.debug("OpenHAB update complete!");
			return oher;
		}
		else {
			return null;
		}
	}
	
	/**
	 * <b><i>Defunct.</b><i> Used to start the OpenHAB server. Now, this function is done outside
	 * the BusinessMachine.
	 */
	private void startOH() {
		LOG.info("Starting OpenHAB...");
		String start;
		LOG.info(oh_location);
		if(os.equalsIgnoreCase("windows")) {
			start = "cmd /c \"cd " + oh_location + " && start.bat\"";
		}
		else if(os.equalsIgnoreCase("linux")) {
			start = "cd " + oh_location + " && ./start.sh";
		}
		else {
			LOG.fatal("Invalid OS specified in bm.properties! OH startup failed!");
			currentRequest.setResponse(new ResError(name, "Invalid OS specified in bm.properties!"));
			return;
		}
		
		try {
			//String[] execs = {"cmd", "/c", "start", "cd C:\\Applications\\openhab", "start.bat"};
			Runtime.getRuntime().exec(start);
			LOG.debug("OpenHAB started!");
		} catch (IOException e) {
			LOG.error("Cannot start OpenHAB!", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * <b><i>Defunct.</b><i> Used to stop the OpenHAB server. Now, this function is done outside
	 * the BusinessMachine.
	 */
	private void stopOH() {
		LOG.info("Stopping OpenHAB...");
		try {
			Runtime.getRuntime().exec("cmd /c \"taskkill /im java.exe /f\"");
		} catch (IOException e) {
			LOG.error("Cannot stpp OpenHAB!", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Opens a FileHandler connected to the .items, .rules, and .sitemap files in OpenHAB directory
	 */
	private void connectToFiles() {
		LOG.info("Connecting to OH files in OpenHAB directory...");
		try {
			//LOG.fatal(items_filepath);
			//LOG.fatal(sitemap_filepath);
			//LOG.fatal(oh_location + "/configurations/rules/" + rules_filename);
			items = new FileHandler(oh_location + "/conf/items/" + items_filename);
			rules = new FileHandler(oh_location + "/conf/rules/" + rules_filename);
			sitemap = new FileHandler(oh_location + "/conf/sitemaps/" + sitemap_filename);
			LOG.debug("Connected to OH files!");
		} catch (FileNotFoundException e) {
			LOG.error("Cannot open OH files!", e);
		}
	}
	
	/**
	 * Updates the contents of the .items file in OpenHAB.
	 */
	private void updateItems() {
		LOG.debug("Updating .items file...");
		cr.retrieveRooms();
		cr.retrieveBindings();
		Component[] coms = cr.getAllComponents();
		Binding[] bindings = cr.getAllBindings();
		HashMap<String, String> rooms = cr.getAllRooms();
		String str = "";
		String groups = "";
		//itemsStr with the mqtt publisher item 
		String itemsStr = "String mqtt_pub \"[%s]\" {mqtt=\">[mqttb:BM:state:*:default]\"}\n\n";		
		
		LOG.debug("Adding rooms to groups declaration...");
		String[] roomIDs = rooms.keySet().toArray(new String[0]);
		for(int i = 0; i < roomIDs.length; i++) {
			String roomID = roomIDs[i];
			String roomName = rooms.get(roomID);
			LOG.trace("Adding room '" + roomName + "' with SSID:" + roomID);
			groups += "Group " + roomID + " \"" + roomName + "\" <attic>\n";
		}
		
		LOG.debug("Adding Components with multiple properties to groups declaration...");
		for(int i = 0; i < coms.length; i++) {
			Component c = coms[i];
			LOG.trace("Adding component " + c.getSSID() + " with " + c.getProperties().size() 
					+ " properties to groups");
			if(c.getProperties().size() > 1) {
				groups += "Group " + c.getSSID() + " \"" + c.getName() + "\" <_0> (" 
						+ c.getRoom() + ")\n";
			}
		}
		
		LOG.debug("Adding individual Properties to items declaration...");
		for(int i = 0; i < coms.length; i++) {
			HashMap<String, String> values = new HashMap<String,String>(1); //for use with BINDINGS
			Component c = coms[i];
			Property[] props = c.getProperties().values().toArray(new Property[0]);
			for(int j = 0; j < props.length; j++) {
				Property p = props[j];
				LOG.trace("Adding property " + p.getSSID() + " of component " + c.getSSID());
				String room = null;
				String itemName = null;
				//Switch UR6C_DigitalOutput_1 "DEFAULT DigitalOutput-1" (U1T4) { mqtt="<[mqttb:openhab/ESP_UR6C_topic:command:ON:DigitalOutput-1_1],<[mqttb:openhab/ESP_UR6C_topic:command:OFF:DigitalOutput-1_0],"}
				if(props.length > 1) { //room = c.SSID
					room = c.getSSID();
					itemName = p.getDisplayName();
				} else { //room = c.room
					room = c.getRoom();
					itemName = c.getName();
				}
				values.put("mac", c.getMAC());
				values.put("topic", c.getTopic());
				values.put("prop_id", p.getSSID());
				values.put("mqtt_broker", OHMqttBroker);
				
				//System.out.println(p.getDisplayName());
				itemsStr += itemsList.get(p.getPropValType().toString()) + " " + c.getSSID()
					+ "_" + p.getSSID() + " \"" + itemName;
				//to add a field variable for String type OH items
				if(p.getPropValType().equals(PropertyValueType.string)) {
					itemsStr += " [%s]";
				}
				itemsStr += "\" (" + room
					+ ") { ";
				
				//adding bindings
				for(int k = 0; k < bindings.length; k++) {
					Binding b = bindings[k];
					if((b.getComType().equals("0000") && b.getPropIndex().equals(p.getPropTypeID())) //comtype '0000' means for ALL components
							|| (b.getComType().equals(c.getProduct().getSSID()) && 
							b.getPropIndex().equals(p.getSSID()))) {
						String bind = b.getBinding();
						bind = StringTools.injectStrings(bind, values, new String[]{"{","}"});
						itemsStr += bind + ",";
					}
				}
				itemsStr = itemsStr.substring(0, itemsStr.length() - 1) +  "} \n\n";
			}
		}
		
		str = groups + "\n\n" + itemsStr;
		
		try {
			items.writeToFile(str);
		} catch (IOException e) {
			LOG.error("Cannot write to .items file!", e);
			e.printStackTrace();
			currentRequest.setResponse(new ResError(name, "Cannot write to .items file!"));
		}
	}
	
	/**
	 * Updates the contents of the .rules file in OpenHAB.
	 */
	private void updateRules() {
		LOG.debug("Updating .rules file...");
		String str = "";
		Component[] coms = cr.getAllComponents();
		
		for(int i = 0; i < coms.length; i++) {
			Component c = coms[i];
			Property[] props = c.getProperties().values().toArray(new Property[0]);
			for(int j = 0; j < props.length; j++) {
				Property p = props[j];
				LOG.trace("Updating rules of property " + p.getSSID());
				try {
					FileHandler fh = new FileHandler
							("resources/openhab/rules/" + p.getPropValType() + ".rules");
					String[] lines = fh.readAllLines();
					HashMap<String, String> values = new HashMap<String, String>(2);
					values.put("prop_ssid", p.getSSID());
					values.put("com_ssid", c.getSSID());
					for(int k = 0; k < lines.length; k++) {
						//LOG.fatal(lines[k]);
						str += StringTools.injectStrings(lines[k], values, 
								new String[]{"[","]"}) + "\n";
					}
				} catch (FileNotFoundException e) {
					LOG.error("Rules for " + p.getPropValType() + " property value type not "
							+ "found!");
					e.printStackTrace();
				} catch (IOException e) {
					LOG.error("Cannot read lines from " + p.getPropValType() + ".rules!");
					e.printStackTrace();
				}
				/*if(p.getPropValType().equals(PropertyValueType.digital)) {
		        	
		        	str += "rule \"" + p.getSSID() + " ON\"\n"
        			+ "when \n"
        			+ "\t Item " + c.getSSID() + "_" + p.getSSID() + " received command ON \n"
        			+ "then \n"
        			+ "\t mqtt_pub.postUpdate(\"{'RTY':'poop','property':'" + p.getSSID() + "','RID':'OH-" + p.getSSID() + "','value':'1','CID':'" + c.getSSID() + "'}\") \n"
        			+ "end \n\n";
        			str += "rule \"" + p.getSSID() + " OFF\"\n"
        			+ "when \n"
        			+ "\t Item " + c.getSSID() + "_" + p.getSSID() + " received command OFF \n"
        			+ "then \n "
        			+ "\t mqtt_pub.postUpdate(\"{'RTY':'poop','property':'" + p.getSSID() + "','RID':'OH-" + p.getSSID() + "','value':'0','CID':'" + c.getSSID() + "'}\") \n"
        			+ "end \n\n";
		        }
		        else {
		        	str += "rule \"" + p.getSSID() + " CHANGED\"\n"
		        			+ "when \n"
		        			+ "\t Item " + c.getSSID() + "_" + p.getSSID() + " received command \n"
		        			+ "then \n "
		        			+ "\t mqtt_pub.postUpdate(\"{'RTY':'poop','property':'" + p.getSSID() + "','RID':'OH-" + p.getSSID() + "','value':'%\" + receivedCommand + \"','CID':'" + c.getSSID() + "'}\") \n"
		        			+ "end \n\n";
		        }*/
		        
			}
		}
		
		try {
			rules.writeToFile(str);
		} catch (IOException e) {
			LOG.error("Cannot write to .rules file!", e);
			e.printStackTrace();
			currentRequest.setResponse(new ResError(name, "Cannot write to .rules file!"));
		}
	}
	
	/**
	 * Updates the contents of the .sitemap file in OpenHAB.
	 */
	private void updateSitemap() {
		LOG.debug("Updating .sitemap file...");
		cr.retrieveRooms();
		HashMap<String, String> rooms = cr.getAllRooms();
		String str = "sitemap home label=\"" + sitemap_name + "\"{ \n"
				+ "Frame {\n";
		
		String[] roomIDs = rooms.keySet().toArray(new String[0]);
		for(int i = 0; i < roomIDs.length; i++) {
			String roomID = roomIDs[i];
			String roomName = rooms.get(roomID);
			str += "Group item=" + roomID + " label=\"" + roomName + "\"\n";
		}
		str += "}\n}";
		
		try {
			sitemap.writeToFile(str);
		} catch (IOException e) {
			LOG.error("Cannot write to .sitemap file!", e);
			e.printStackTrace();
			currentRequest.setResponse(new ResError(name, "Cannot write to .sitemap file!"));
		}
	}
}

/*LOG.debug("Retrieving rooms from DB...");
Object o = dbe.forwardRequest(new SelectDBEReq(idg.generateMixedCharID(10), roomsTable));
if(o.getClass().equals(ResError.class)) {
	ResError e = (ResError) o;
	LOG.error("Cannot retrieve rooms from DB!");
	LOG.error("Error message: " + e.message);
	return false;
} else {
	ResultSet rs1 = (ResultSet) o;
	try {
		while(rs1.next()) {
			rooms.put(rs1.getString("ssid"), rs1.getString("name"));
		}
		rs1.close();
		LOG.debug("Rooms retrieved!");
	} catch (SQLException e) {
		LOG.error("ResultSet error in retrieving rooms!", e);
		e.printStackTrace();
	}
}
*/