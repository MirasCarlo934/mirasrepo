package main.objects;

import org.json.JSONObject;

public class User {
	private String id;
	private String name;
	private String description;
	private boolean isAdmin;

	public User(String id, String name, String description, boolean isAdmin) {
		setId(id);
		setName(name);
		setDescription(description);
		setAdmin(isAdmin);
	}
	
	public JSONObject toJSON() { //converts this object to JSON (to minimize conversion length)
		JSONObject json = new JSONObject();
		json.put("id", id); //puts -1 if id is not yet set
		json.put("name", name);
		json.put("description", description);
		json.put("isAdmin", isAdmin);
		return json;
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

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the isAdmin
	 */
	public boolean isAdmin() {
		return isAdmin;
	}

	/**
	 * @param isAdmin the isAdmin to set
	 */
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
}
