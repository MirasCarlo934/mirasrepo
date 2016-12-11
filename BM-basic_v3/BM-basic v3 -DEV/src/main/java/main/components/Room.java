package main.components;

import main.TransTechSystem;
import main.objects.DB.DBObject;

public class Room {
	public static final String obj_type = "room";
	private static final String ssid_col = TransTechSystem.config.getDatabaseConfig().getSsidColName().toLowerCase();
	private static final String name_col = "name";
	private static final String[] rooms_cols = {ssid_col, name_col};
	
	private String id;
	private String name;

	public Room(String id, String name) {
		setId(id);
		setName(name);
	}
	
	/**
	 * Creates a Room object using a DBObject.
	 * 
	 * @param room_object the DBObject
	 */
	public Room(DBObject room_object) {
		//checks if DBObject is a component object
		for(int i = 0; i < rooms_cols.length; i++) {
			if(!room_object.containsKey(rooms_cols[i])) {
				throw new NullPointerException("DBObject is not a room object!");
			}
		}
		setId((String) room_object.get(ssid_col));
		setName((String) room_object.get(name_col));
	}
	
	/**
	 * Builds an openhab group using the specs of this Room object. The built string is a String representation of this Room in
	 * the openhab server. This String is appended to the .items file as a 'Group'. <br><br>
	 * 
	 * <i><b>Note:</b>Default image is attic</i>
	 * 
	 * @return the Group string
	 */
	public String toOHGroup() {
		String str = "Group " + id + " " + encloseInQuotes(name) + " <attic>";
		return str;
	}
	
	/**
	 * Returns the specified string enclosed in double quotes. This saves the hassle of doing the enclosure manually.
	 * 
	 * @param str the string needed to be enclosed
	 * @return
	 */
	private String encloseInQuotes(String str) {
		return '"' + str + '"';
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}
