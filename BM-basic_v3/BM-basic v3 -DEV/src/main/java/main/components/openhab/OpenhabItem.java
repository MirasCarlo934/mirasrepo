package main.components.openhab;

import bm.OpenhabHandlingEngine;
import bm.modules.POOPModule;
import main.components.Component;
import main.components.properties.Property;
import main.components.properties.PropertyMode;
import main.objects.request_response.Request;
import tools.StringTools;

public enum OpenhabItem {	
	DIMMER {
		@Override
		public String getOHItemScript(Component com, Property prop, String room, String mqtt_broker, String openhab_topic) {
			String items = "";
			if(prop.getMode().equals(PropertyMode.O) || prop.getMode().equals(PropertyMode.IO)) {
				items += StringTools.injectStrings("%s %s \"%s\" (%s) \n", 
						new String[] {StringTools.capitalizeFirstLetter(DIMMER.toString().toLowerCase()), 
								com.getId() + "_" + prop.getSystemName().split("-")[0] + "_" + prop.getSystemName().split("-")[1], 
								com.getName() + " " + StringTools.capitalizeFirstLetter(prop.getDisplayName()), 
								room},
						"%s");
			}
			return items;
		}

		@Override
		public String getOHRulesScript(Component com, Property prop) {
			String rules = "";
			if(prop.getMode().equals(PropertyMode.O) || prop.getMode().equals(PropertyMode.IO)) {
				rules += "rule " + StringTools.encloseInQuotes("Publish " + com.getId() + ":" + prop.getSystemName()) +
						"when Item " + com.getId() + "_" + prop.getSystemName().split("-")[0] + "_" + prop.getSystemName().split("-")[1] + " received command then " +
						"mqtt_pub.postUpdate(" +
						StringTools.encloseInQuotes("{'RTY':'prop_change','property':'" + prop.getSystemName() + "',"
								+ "'RID':'0000','value':'%\" + receivedCommand + \"','CID':'" + com.getId() + "'}")
						+ ") end";
			}
			return rules;
		}
	}, SWITCH {
		@Override
		public String getOHItemScript(Component com, Property prop, String room, String mqtt_broker, String openhab_topic) {
			String items = "";
			if(prop.getMode().equals(PropertyMode.O) || prop.getMode().equals(PropertyMode.IO)) {
				items += StringTools.injectStrings("%s %s \"%s\" (%s) \n", 
						new String[] {StringTools.capitalizeFirstLetter(SWITCH.toString().toLowerCase()), 
								com.getId() + "_" + prop.getSystemName().split("-")[0] + "_" + prop.getSystemName().split("-")[1], 
								com.getName() + " " + StringTools.capitalizeFirstLetter(prop.getDisplayName()), 
								room},
						"%s");
				
				items += "{ mqtt=" + StringTools.encloseInQuotes(
						"<[" + mqtt_broker + ":" + openhab_topic + "/" + com.getMqttTopic() + ":command:ON:" + prop.getSystemName() + "_1]," + 
						"<[" + mqtt_broker + ":" + openhab_topic + "/" + com.getMqttTopic() + ":command:OFF:" + prop.getSystemName() + "_0],")
				+ "}";
			}
			
			return items;
		}

		@Override
		public String getOHRulesScript(Component com, Property prop) {
			Request turn_on = new Request("0000", com.getId(), POOPModule.request_type);
			turn_on.put("property", prop.getSystemName());
			turn_on.put("value", 1);
			Request turn_off = new Request("0000", com.getId(), POOPModule.request_type);
			turn_off.put("property", prop.getSystemName());
			turn_off.put("value", 0);

			String str = "";
			if(!prop.getMode().equals(PropertyMode.I)) {
				str += "rule " + StringTools.encloseInQuotes("Publish " + prop.getSystemName() + " state to BM");
				str += " when ";
				str += "Item " + com.getId() + "_" + prop.getSystemName().split("-")[0] + "_" + prop.getSystemName().split("-")[1] + " received command ";
				str += "then switch(receivedCommand) {";
				str += " case ON:" + OpenhabHandlingEngine.openhab_mqtt_publisher + ".postUpdate(" 
						+ StringTools.encloseInQuotes(StringTools.toSingleQuotedJSONString(turn_on.toJSONObject())) + ")";
				str += " case OFF:" + OpenhabHandlingEngine.openhab_mqtt_publisher + ".postUpdate(" 
						+ StringTools.encloseInQuotes(StringTools.toSingleQuotedJSONString(turn_off.toJSONObject()))  + ")";
				str += "} end ";
			}
			return str;
		}
	}, COLOR {
		@Override
		public String getOHItemScript(Component com, Property prop, String room, String mqtt_broker, String openhab_topic) {
			// TODO Auto-generated method stub
			return "";
		}

		@Override
		public String getOHRulesScript(Component com, Property prop) {
			// TODO Auto-generated method stub
			return "";
		}
	}, ROLLERSHUTTER {
		@Override
		public String getOHItemScript(Component com, Property prop, String room, String mqtt_broker, String openhab_topic) {
			String items = "";
			if(prop.getMode().equals(PropertyMode.O) || prop.getMode().equals(PropertyMode.IO)) {
				items += StringTools.injectStrings("%s %s \"%s\" (%s) \n", 
						new String[] {StringTools.capitalizeFirstLetter(ROLLERSHUTTER.toString().toLowerCase()), 
								com.getId() + "_" + prop.getSystemName().split("-")[0] + "_" + prop.getSystemName().split("-")[1], 
								com.getName() + " " + StringTools.capitalizeFirstLetter(prop.getDisplayName()), 
								room},
						"%s");
			}
			
			return items;
		}

		@Override
		public String getOHRulesScript(Component com, Property prop) {
			Request increase = new Request("0000", com.getId(), POOPModule.request_type);
			increase.put("property", prop.getSystemName());
			increase.put("value", "+93");
			Request decrease = new Request("0000", com.getId(), POOPModule.request_type);
			decrease.put("property", prop.getSystemName());
			decrease.put("value", "-93");
			Request stop = new Request("0000", com.getId(), POOPModule.request_type);
			stop.put("property", prop.getSystemName());
			stop.put("value", 0);

			String str = "";
			if(!prop.getMode().equals(PropertyMode.I)) {
				str += "rule " + StringTools.encloseInQuotes("Publish " + prop.getSystemName() + " state to BM");
				str += " when ";
				str += "Item " + com.getId() + "_" + prop.getSystemName().split("-")[0] + "_" + prop.getSystemName().split("-")[1] + " received command ";
				str += "then switch(receivedCommand) {";
				str += " case UP:" + OpenhabHandlingEngine.openhab_mqtt_publisher + ".postUpdate(" 
						+ StringTools.encloseInQuotes(StringTools.toSingleQuotedJSONString(increase.toJSONObject())) + ")";
				str += " case DOWN:" + OpenhabHandlingEngine.openhab_mqtt_publisher + ".postUpdate(" 
						+ StringTools.encloseInQuotes(StringTools.toSingleQuotedJSONString(decrease.toJSONObject()))  + ")";
				str += " case STOP:" + OpenhabHandlingEngine.openhab_mqtt_publisher + ".postUpdate(" 
						+ StringTools.encloseInQuotes(StringTools.toSingleQuotedJSONString(stop.toJSONObject()))  + ")";
				str += "} end ";
			}
			return str;
		}
	};
	
	/**
	 * Parses an OpenhabItem using the String specified
	 * @param str the String where the OpenhabItem will be parsed from
	 */
	public static OpenhabItem parseItem(String str) throws IllegalArgumentException {
		OpenhabItem[] items = OpenhabItem.values();
		OpenhabItem ohitem = null;
		for(int i = 0; i < items.length; i++) {
			OpenhabItem item = items[i];
			if(item.toString().equalsIgnoreCase(str)) {
				ohitem = item;
				break;
			}
		}
		
		if(ohitem != null) {
			return ohitem;
		} else {
			throw new IllegalArgumentException("String is not an openhab item!");
		}
	}
	
	/**
	 * 
	 * Retrieves the Openhab .items script representation for this specific property.
	 * 
	 * @return the .items script of this particular item, or an empty String if the property's mode is I (input)
	 * 
	 * @param com the Component that contains this Property
	 * @param prop the Property that contains this OpenhabItem
	 * @param room the room SSID in which the Component belongs to
	 * @param mqtt_broker the name of the MQTT brokering device used by OpenHab
	 * @param openhab_topic the topic where openhab listens to
	 */
	public abstract String getOHItemScript(Component com, Property prop, String room, String mqtt_broker, String openhab_topic);
	
	/**
	 * Retrieves the Openhab .rules script for the OpenHab rules associated for this specific property
	 * 
	 * @return the .items script of this particular item, or an empty String if the property's mode is I (input)
	 * 
	 * @param com the Component that contains this Property
	 */
	public abstract String getOHRulesScript(Component com, Property prop);
}
