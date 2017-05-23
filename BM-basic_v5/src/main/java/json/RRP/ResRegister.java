package json.RRP;

public class ResRegister extends AbstResponse {
	public String id; //ssid of the newly registered component
	public String topic; //topic of the newly registered component

	public ResRegister(AbstRequest request, String id, String topic) {
		super(request, true);
		this.id = id;
		this.topic = topic;
		addParameter("id", id);
		addParameter("topic", topic);
	}
	
	public ResRegister(String MAC, String CID, String RTY, String id, String topic) {
		super(MAC, CID, RTY, true);
		this.id = id;
		this.topic = topic;
		addParameter("id", id);
		addParameter("topic", topic);
	}
}
