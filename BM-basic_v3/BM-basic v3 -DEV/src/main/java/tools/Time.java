package tools;

public class Time {
	private String timeString;
	private int hour;
	private int minutes;
	
	public Time(String timeStr) {
		String s[] = timeStr.split(":");
		setHour(Integer.parseInt(s[0]));
		setMinutes(Integer.parseInt(s[1]));
		setTimeString(timeStr);
	}

	public int getMinutes() {
		return minutes;
	}

	public void setMinutes(int minutes) {
		this.minutes = minutes;
	}

	public int getHour() {
		return hour;
	}

	public void setHour(int hour) {
		this.hour = hour;
	}

	public String getTimeString() {
		return timeString;
	}

	public void setTimeString(String timeString) {
		this.timeString = timeString;
	}
}
