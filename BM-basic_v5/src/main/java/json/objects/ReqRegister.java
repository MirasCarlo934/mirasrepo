package json.objects;

import org.json.JSONObject;

import devices.Product;

/*
 * This is the object populated when a register JSON transaction arrives from mqtt
 * Sample data is:
 * 		{"RID":"18fe34cf4fc1","CID":"ESP","RTY":"register","name":"Esp12e_RGB","roomID":"MasterBedroom","prodID":"0002"}
 */
public class ReqRegister extends AbstRequest{
	//we are setting the parameters as public to make it easier to access
	public String name;
	public String room;
	//public String product_id;
	public String mac;

	public ReqRegister(JSONObject json) {
		super(json);
		this.name = json.getString("name");
		this.room = json.getString("roomID");
		//this.product_id = json.getString("prodID");
		this.mac = json.getString("RID");
	}
}
