package main.engines;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Executor;

import components.Component;
import components.properties.Property;
import json.objects.ResError;
import main.ComponentRepository;
import main.engines.requests.EngineRequest;
import main.engines.requests.DBEngine.SelectDBEReq;
import main.engines.requests.OHEngine.OHEngineRequest;
import main.engines.requests.OHEngine.OHRequestType;
import mqtt.MQTTHandler;
import tools.FileHandler;
import tools.IDGenerator;

public class OHEngine extends Engine {
	private ComponentRepository cr;
	private String os;
	private String OHMqttBroker;
	private String oh_location;
	private String items_filename;
	private String sitemap_filename;
	private String sitemap_name;
	//private Properties bm_props = new Properties();
	private FileHandler items;
	private FileHandler sitemap;
	private HashMap<String, String> itemsList;
	//private IDGenerator idg = new IDGenerator();
	private OHEngineRequest oher = null;

	public OHEngine(String os, String oh_filepath, String items_filename, String sitemap_filename, 
			String sitemap_name, ComponentRepository cr, HashMap<String, String> itemsList, 
			String OHMqttBroker) {
		super("OHEngine", OHEngine.class.toString());
		this.oh_location = oh_filepath;
		this.items_filename = items_filename;
		this.sitemap_filename = sitemap_filename;
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
	 * Opens a FileHandler connected to the .items and .sitemap files in OpenHAB directory
	 */
	private void connectToFiles() {
		LOG.info("Connecting to .items and .sitemap files in OpenHAB directory...");
		try {
			//LOG.fatal(items_filepath);
			//LOG.fatal(sitemap_filepath);
			items = new FileHandler(oh_location + "/configurations/items/" + items_filename);
			sitemap = new FileHandler(oh_location + "/configurations/sitemaps/" + sitemap_filename);
			LOG.debug("Connected to. items and .sitemap files!");
		} catch (FileNotFoundException e) {
			LOG.error("Cannot open .items and .sitemap files!", e);
		}
	}
	
	/**
	 * Updates the contents of the .items file in OpenHAB.
	 */
	private void updateItems() {
		LOG.debug("Updating .items file...");
		cr.retrieveRooms();
		Component[] coms = cr.getAllComponents();
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
			Component c = coms[i];
			Property[] props = c.getProperties().values().toArray(new Property[0]);
			for(int j = 0; j < props.length; j++) {
				Property p = props[j];
				LOG.trace("Adding property " + p.getIndex() + " of component " + c.getSSID());
				String room = null;
				//Switch UR6C_DigitalOutput_1 "DEFAULT DigitalOutput-1" (U1T4) { mqtt="<[mqttb:openhab/ESP_UR6C_topic:command:ON:DigitalOutput-1_1],<[mqttb:openhab/ESP_UR6C_topic:command:OFF:DigitalOutput-1_0],"}
				if(props.length > 1) { //room = c.SSID
					room = c.getSSID();
				} else { //room = c.room
					room = c.getRoom();
				}
				if(itemsList.get(p.getPropValType().toString()).equalsIgnoreCase("switch")) {
					itemsStr += itemsList.get(p.getPropValType().toString()) + " " + c.getSSID()
						+ "_" + p.getIndex() + " \"" + p.getDisplayName() + "\" (" + room
						+ ") {mqtt=\"<[" + OHMqttBroker + ":openhab/" + c.getTopic() 
						+ ":command:ON:" + p.getIndex() + "_1],<[" + OHMqttBroker + ":openhab/" 
						+ c.getTopic() + ":command:OFF:" + p.getIndex() + "_0]\"} \n\n";
				}
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
	 * Updates the contents of the .sitemap file in OpenHAB.
	 */
	private void updateSitemap() {
		LOG.debug("Updating .sitemap file...");
		cr.retrieveRooms();
		HashMap<String, String> rooms = cr.getAllRooms();
		String str = "sitemap myhome label=\"" + sitemap_name + "\"{ \n"
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