package main.objects;

import org.json.JSONObject;

import tools.Date;
import tools.Time;

public class EventRequest {
	private String id = "-1"; //not required to be filled immediately
	private String name;
	private Date date;
	private Time timeStart;
	private Time timeEnd;
	private String agenda;
	private String reservee;
	private String[] admins;

	public EventRequest(JSONObject json) {
		setName(json.getString("name"));
		setDate(new Date(json.getString("petsa")));
		setTimeStart(new Time(json.getString("timestart")));
		setTimeEnd(new Time(json.getString("timeend")));
		setReservee(json.getString("reservee"));
		setAgenda(json.getString("agenda"));
		admins = json.getString("admins").split(",");
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
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @return the timeStart
	 */
	public Time getTimeStart() {
		return timeStart;
	}

	/**
	 * @param timeStart the timeStart to set
	 */
	public void setTimeStart(Time timeStart) {
		this.timeStart = timeStart;
	}

	/**
	 * @return the timeEnd
	 */
	public Time getTimeEnd() {
		return timeEnd;
	}

	/**
	 * @param timeEnd the timeEnd to set
	 */
	public void setTimeEnd(Time timeEnd) {
		this.timeEnd = timeEnd;
	}

	/**
	 * @return the agenda
	 */
	public String getAgenda() {
		return agenda;
	}

	/**
	 * @param agenda the agenda to set
	 */
	public void setAgenda(String agenda) {
		this.agenda = agenda;
	}

	/**
	 * @return the reservee
	 */
	public String getReservee() {
		return reservee;
	}

	/**
	 * @param reservee the reservee to set
	 */
	public void setReservee(String reservee) {
		this.reservee = reservee;
	}

	/**
	 * @return the admins
	 */
	public String[] getAdmins() {
		return admins;
	}

	/**
	 * @param admins the admins to set
	 */
	public void setAdmins(String[] admins) {
		this.admins = admins;
	}

}
