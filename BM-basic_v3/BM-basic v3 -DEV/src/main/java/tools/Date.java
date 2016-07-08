package tools;

public class Date {
	private String dateString;
	private int month;
	private int day;
	private int year;
	
	public Date(String dateStr) {
		String s[] = dateStr.split("-");
		setYear(Integer.parseInt(s[0]));
		setMonth(Integer.parseInt(s[1]));
		setDay(Integer.parseInt(s[2]));
		setDateString(dateStr);
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getDateString() {
		return dateString;
	}

	public void setDateString(String dateString) {
		this.dateString = dateString;
	}
}
