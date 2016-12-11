package main.objects;

import org.json.JSONObject;

import tools.Date;
import tools.Time;

public class Reservation {
	private String id = "-1"; //not required to be filled immediately
	private String room_id;
	private Date date;
	private Time timeStart;
	private Time timeEnd;
	private String agenda;
	private int attendees;
	private String reservee;
	
	public Reservation(String str) { //for instantiation from MQTTbroker
		String[] parsedInput = str.split("-!-");
		String room = parsedInput[0];
		String date = parsedInput[1];
		String time1 = parsedInput[2];
		String time2 = parsedInput[3];
		int attendees = Integer.parseInt(parsedInput[4]);
		String agenda = parsedInput[5];
		String reservee = parsedInput[6];
		
		setRoomID(room);
		setDate(new Date(date));
		setTimeStart(new Time(time1));
		setTimeEnd(new Time(time2));
		setAgenda(agenda);
		setAttendees(attendees);
		setReservee(reservee);
	}
	
	public Reservation(String id, String room, String date, String timeStart, String timeEnd, 
			String agenda, int attendees, String reservee) { //for instantiation from DB Derby
		setId(id);
		setRoomID(room);
		setDate(new Date(date));
		setTimeStart(new Time(timeStart));
		setTimeEnd(new Time(timeEnd));
		setAgenda(agenda);
		setAttendees(attendees);
		setReservee(reservee);
	}
	
	public Reservation(String id, String room, Date date, Time timeStart, Time timeEnd, 
			String agenda, int attendees, String reservee) { //for instantiation from DB Derby
		setId(id);
		setRoomID(room);
		setDate(date);
		setTimeStart(timeStart);
		setTimeEnd(timeEnd);
		setAgenda(agenda);
		setAttendees(attendees);
		setReservee(reservee);
	}
	
	public Reservation(JSONObject json) { //for instantiation from MQTTbroker json message
		setRoomID(json.getString("roomid"));
		setDate(new Date(json.getString("petsa")));
		setTimeStart(new Time(json.getString("timestart")));
		setTimeEnd(new Time(json.getString("timeend")));
		setReservee(json.getString("reservee"));
		setAgenda(json.getString("agenda"));
		setAttendees(json.getInt("attendees"));
	}
	
	/*public Reservation(JSONObject json) { //for instantiation from MQTTbroker json message
		setRoomID(json.getString("room"));
		setDate(new Date(json.getString("date")));
		setTimeStart(new Time(json.getString("start")));
		setTimeEnd(new Time(json.getString("end")));
		setReservee(json.getString("by"));
		setAgenda(json.getString("agenda"));
		setAttendees(json.getInt("ppl"));
	}*/
	
	public JSONObject toJson() { //converts this object to JSON (to minimize conversion length)
		JSONObject json = new JSONObject();
		json.put("id", id); //puts -1 if id is not yet set
		json.put("room_id", room_id);
		json.put("date", date.getDateString());
		json.put("time_start", timeStart.getTimeString());
		json.put("time_end", timeEnd.getTimeString());
		json.put("agenda", agenda);
		json.put("attendees", attendees);
		json.put("reservee", reservee);
		return json;
	}

	public String getRoomID() {
		return room_id;
	}

	public void setRoomID(String room) {
		this.room_id = room;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Time getTimeStart() {
		return timeStart;
	}

	public void setTimeStart(Time time) {
		this.timeStart = time;
	}

	public String getAgenda() {
		return agenda;
	}

	public void setAgenda(String agenda) {
		this.agenda = agenda;
	}

	public int getAttendees() {
		return attendees;
	}

	public void setAttendees(int attendees) {
		this.attendees = attendees;
	}

	public String getReservee() {
		return reservee;
	}

	public void setReservee(String reservee) {
		this.reservee = reservee;
	}

	public Time getTimeEnd() {
		return timeEnd;
	}

	public void setTimeEnd(Time timeEnd) {
		this.timeEnd = timeEnd;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
