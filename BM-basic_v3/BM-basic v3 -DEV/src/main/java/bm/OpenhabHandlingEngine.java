package bm;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import main.TransTechSystem;
import main.components.Component;
import main.components.Room;
import main.components.openhab.OpenhabItem;
import main.components.properties.Property;
import main.components.properties.PropertyMode;
import main.objects.DB.DBObject;
import main.objects.request_response.BMRequest;
import main.objects.request_response.InstructionResponse;
import main.objects.request_response.Request;
import main.objects.request_response.TransactionRequest;
import mqtt.MQTTHandler;
import tools.StringTools;

/**
 * Every IO and O properties of every Component within the system can be controlled through the OpenHab interface by the
 * end-user. The OpenhabHandlingEngine is the Engine that manages the OpenHab server according to the current states and
 * records of all the Components within the system.
 * 
 * @author miras
 *
 */
public class OpenhabHandlingEngine {
	private static final Logger logger = Logger.getLogger(OpenhabHandlingEngine.class);
	
	private static final String room_obj_type = "room";
	private static final String ssid_col = TransTechSystem.config.getDatabaseConfig().getSsidColName().toLowerCase();
	private static final String room_name_col = "name";
	
	private static final String comp_obj_type = "component";
	private static final String comp_room_col = "room";
	private static final String comp_funct_col = "functn";
	private static final String comp_desc_col = "name";
	
	private static final String props_obj_type = "com_prop";
	private static final String props_cid_col = "com_id";
	private static final String props_pname_col = "prop_name";
	private static final String props_pval_col = "prop_value";
	
	private static final String system_room = "A000";
	private static final String roomless_room = "A001";
	
	private static final String switch_funct = "0003";
	
	private static final String bm_topic = TransTechSystem.config.getMqttTopicConfig().getBMTopic();
	
	public static final String openhab_mqtt_publisher = "mqtt_pub"; //name of the item that facilitates the mqtt publishing
	public static final String openhab_mqtt_topic = "openhab"; //the MQTT topic where OpenHab listens to
	private static final String openhab_topic = TransTechSystem.config.getMqttTopicConfig().getOpenhabTopic(); //the topic where openhab listens to in the system
	private static final String openhab_config = TransTechSystem.config.getInstructionPropsConfig().getOpenhabSpecs().get("dir") 
			+ "/configurations/openhab.cfg";
	private static final String openhab_items = TransTechSystem.config.getInstructionPropsConfig().getOpenhabSpecs().get("dir") 
			+ "/configurations/items/home.items";
	private static final String openhab_rules = TransTechSystem.config.getInstructionPropsConfig().getOpenhabSpecs().get("dir")
			+ "/configurations/rules/home.rules";
	private static final String openhab_sitemap = TransTechSystem.config.getInstructionPropsConfig().getOpenhabSpecs().get("dir")
			+ "/configurations/sitemaps/home.sitemap";
	private static final String openhab_sitemap_name = 
			TransTechSystem.config.getInstructionPropsConfig().getOpenhabSpecs().get("site_name");

	private MQTTHandler mh;
	private Properties OH_props = new Properties();
	private String OH_mqtt_broker; //name of the mqtt broker defined in the openhab.cfg
	
	public OpenhabHandlingEngine(MQTTHandler mqttHandler) 
			throws FileNotFoundException, IOException {
		mh = mqttHandler;
		
		OH_props.load(new FileInputStream(openhab_config));
		OH_mqtt_broker = OH_props.get("mqtt").toString().split("\\.")[0];
	}
	
	/** 
	 * Receives the InstructionResponse sent by the POOPModule and uses it to instruct the OpenHab-equivalent item of the
	 * affected Component Property.
	 * 
	 * @param ir the InstructionResponse
	 * @throws MqttException 
	 */
	public void receiveInstruction(InstructionResponse ir) throws MqttException {
		logger.trace("Instructing property " + ir.getPropertyName() + " of the component ");
		String pname = ir.getPropertyName();
		int pval = Integer.parseInt(ir.getPropertyValue());
		mh.publish(openhab_topic + "/" + ir.getTopic(), pname + "_" + pval);
	}
	
	/**
	 * Updates properties of openhab items by sending the appropriate command to the items' respective openhab topics.
	 * 
	 * @throws SQLException if OpenhabHandler cannot transact with DB
	 * @throws MqttException if OpenhabHandler cannot publish to items' respective openhab topics
	 */
	public void updateItemProperties(Component[] coms) throws SQLException, MqttException {
		logger.trace("Updating items' properties...");
		
		//Gets all component properties from DB
		for(int i = 0; i < coms.length; i++) {
			Component com = coms[i];
			Property[] com_props = com.getProperties();
			for(int j = 0; j < com_props.length; j++) {
				String pname = com_props[j].getSystemName();
				int pval = com_props[j].getValue();
				int maxval = com_props[j].getMaxval();

				//if maxval > 100, convert the instruction value into percentile
				if(maxval > 100) {
					mh.publish(openhab_topic + "/" + com.getMqttTopic(), pname + "_" + (pval / maxval * 100));
				} else {
					mh.publish(openhab_topic + "/" + com.getMqttTopic(), pname + "_" + pval);
				}
			}
		}
		logger.trace("Update complete!");
	}
	
	/**
	 * Updates openhab .items, .rules, and .sitemap files according to DB components and rooms records.
	 * @throws SQLException 
	 * @throws MqttException 
	 */
	public void updateOpenhabRecords(Room[] rooms, Component[] components) throws SQLException, MqttException {
		logger.trace("Updating OpenHab .items and .sitemap files...");
		
		//Updates .items, .rules, and .sitemap
		updateItems(rooms, components);
		updateRules(components);
		updateSitemap(rooms);
		
		try { //waits until all files have been reloaded by the OH server
			Thread.sleep(3100);
		} catch (InterruptedException e) {
			logger.warn("Thread cannot be stalled in record update!", e);
		}
		
		/*
		 * Updates items' properties
		 */
		updateItemProperties(components);
	}
	
	/**
	 * Updates the .rules file.
	 * @throws SQLException
	 */
	private void updateRules(Component[] coms) throws SQLException {
		logger.trace("Updating .rules file...");
		String str = "";
		
		for(int i = 0; i < coms.length; i++) {
			Component com = coms[i];
			Property[] com_props = com.getProperties();
			for(int j = 0; j < com_props.length; j++) {
				Property prop = com_props[j];
				str += prop.getOhItem().getOHRulesScript(com, prop);
				str += "\n";
			}
		}
		
		logger.trace("Writing to bm.rules...");
		try (BufferedWriter items_writer = new BufferedWriter(new FileWriter(openhab_rules))) {
			items_writer.append(str);
		    logger.trace(".rules file updated!");
		} catch (IOException x) {
		    logger.error("Cannot update rules file!", x);
		}
	}
	
	/**
	 * Updates .items file.
	 * @throws SQLException 
	 */
	private void updateItems(Room[] rooms, Component[] coms) throws SQLException {
		logger.trace("Updating .items file...");
		String str = "";
		
		//Sets groups section from rooms
		logger.trace("Setting groups from rooms...");
		String groups = "";
		for(int i = 0; i < rooms.length; i++) {
			Room room = rooms[i];
			groups += room.toOHGroup() + "\n";
		}
		//groups += "\n";
		//if com props > 1, add com to groups
		for(int i = 0; i < coms.length; i++) {
			Component com = coms[i];
			if(com.getProperties().length > 1) {
				groups += com.toOHGroup() + "\n";
			}
		}
		//
		
		str += groups + "\n";
		
		//Sets mqtt_pub to handle all mqtt outbound traffic
		str += "String " + openhab_mqtt_publisher + " " + StringTools.encloseInQuotes("[%s]") + " {mqtt=" 
				+ StringTools.encloseInQuotes(">[mqttb:" + bm_topic + ":state:*:default]") + "}\n";
		
		//Sets items section from components
		logger.trace("Setting items from components...");
		String items = "";
		for(int i = 0; i < coms.length; i++) {
			Component com = coms[i];
			if(com.getProperties().length > 1) {
				String[] com_items = com.getOHItemScripts(com.getId(), OH_mqtt_broker, openhab_mqtt_topic);
				for(int j = 0; j < com_items.length; j++) {
					items += com_items[j];
					items += "\n";
				}
			}
			else {
				String[] com_items = com.getOHItemScripts(com.getRoom(), OH_mqtt_broker, openhab_mqtt_topic);
				items += com_items[0];
				items += "\n";
			}
		}
		str += items;
		
		logger.trace("Writing to bm.sitemap...");
		try (BufferedWriter items_writer = new BufferedWriter(new FileWriter(openhab_items))) {
			items_writer.append(str);
		    logger.trace(".items file updated!");
		} catch (IOException x) {
		    logger.error("Cannot update items file!", x);
		}
	}
	
	/**
	 * Updates .sitemap file.
	 * @throws SQLException 
	 */
	private void updateSitemap(Room[] rooms) throws SQLException {
		logger.trace("Updating .sitemap file...");		
		String header = "sitemap myhome label=" + StringTools.encloseInQuotes(openhab_sitemap_name) + "{\n\n";
		String frames = "Frame {\n";
		
		logger.trace("Populating sitemap frames...");
		for(int i = 0; i < rooms.length; i++) {
			Room room = rooms[i];
			String room_id = room.getId();
			String room_name = room.getName();
			frames += "Group item=" + room_id + " label=" + StringTools.encloseInQuotes(room_name) + "\n";
		}
		frames += "}\n";
		
		String text = header + frames + "}";
		
		logger.trace("Writing to bm.sitemap...");
		try (BufferedWriter sitemap_writer = new BufferedWriter(new FileWriter(openhab_sitemap))) {
		    sitemap_writer.append(text);
		    logger.trace(".sitemap file updated!");
		} catch (IOException x) {
		    logger.error("Cannot update sitemap file!", x);
		}
	}
}
