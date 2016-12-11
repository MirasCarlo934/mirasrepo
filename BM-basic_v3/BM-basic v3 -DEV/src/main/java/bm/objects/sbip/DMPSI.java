package bm.objects.sbip;

import tools.Date;
import tools.Time;

public class DMPSI extends ScheduledInstruction {
	private String messageName;
	private String message;
	private String receiver;

	/**
	 * 
	 * @param id
	 * @param date
	 * @param time
	 * @param componentID the component that sent the message
	 * @param periodical
	 * @param messageName
	 * @param message
	 * @param receiver
	 */
	public DMPSI(String id, Date date, Time time, String componentID, boolean periodical, String messageName, String message, 
			String receiver) {
		super(id, date, time, SBIPAction.DMP, componentID, periodical);
		setMessageName(messageName);
		setMessage(message);
		setReceiver(receiver);
	}

	/**
	 * The name of the message that will be sent when the set schedule is met.
	 * @return the messageName
	 */
	public String getMessageName() {
		return messageName;
	}

	/**
	 * @param messageName the messageName to set
	 */
	public void setMessageName(String messageName) {
		this.messageName = messageName;
	}

	/**
	 * The message text that will be sent when the set schedule is met.
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * The SSID of the component that will receive the message when the set schedule is met.
	 * @return the receiver
	 */
	public String getReceiver() {
		return receiver;
	}

	/**
	 * @param receiver the receiver to set
	 */
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
}
