package bm.objects.sbip;

import tools.Date;
import tools.Time;

public abstract class ScheduledInstruction {
	private String id;
	private Date date;
	private Time time;
	private SBIPAction action;
	private String componentID;
	private boolean periodical;
	private boolean triggered;

	/**
	 * 
	 * @param id
	 * @param date <i>Can be null if scheduled instruction is not periodical</i>
	 * @param time
	 * @param action
	 * @param componentID
	 */
	public ScheduledInstruction(String id, Date date, Time time, SBIPAction action, String componentID, boolean periodical) {
		setId(id);
		setDate(date);
		setTime(time);
		setAction(action);
		setComponentID(componentID);
		setPeriodical(periodical);
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
	 * @return the time
	 */
	public Time getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(Time time) {
		this.time = time;
	}

	/**
	 * @return the action
	 */
	public SBIPAction getAction() {
		return action;
	}

	/**
	 * @param action the action to set
	 */
	public void setAction(SBIPAction action) {
		this.action = action;
	}

	/**
	 * @return the componentID
	 */
	public String getComponentID() {
		return componentID;
	}

	/**
	 * @param componentID the componentID to set
	 */
	public void setComponentID(String componentID) {
		this.componentID = componentID;
	}

	/**
	 * @return the periodical
	 */
	public boolean isPeriodical() {
		return periodical;
	}

	/**
	 * @param periodical the periodical to set
	 */
	public void setPeriodical(boolean periodical) {
		this.periodical = periodical;
	}

	/**
	 * States whether the SI has already been triggered by the ScheduleEngine. For the case of periodical SI, this is set back to false in
	 * the minute after the SI was triggered.
	 * @return the triggered
	 */
	public boolean isTriggered() {
		return triggered;
	}

	/**
	 * @param triggered the triggered to set
	 */
	public void setTriggered(boolean triggered) {
		this.triggered = triggered;
	}
}
