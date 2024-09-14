package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class Shift {
	private int shiftID;
	private int storeID;
	private String username;
    private LocalTime shiftStartTime;
	private LocalTime shiftEndTime;
	private LocalDate shiftStartDate;
	private LocalDate shiftEndDate;
	private int thirtyMinBreaks;
	private int tenMinBreaks;
	private boolean repeating;
	private int daysPerRepeat;

	private String password;
	private String first_name;
	private String last_name;
	private String role;
	private String profileBG;
	private String profileText;
	private LocalDate originalDate = null;

	public Shift(){}

	public int getShiftID() {
		return shiftID;
	}

	public void setShiftID(int shiftID) {
		this.shiftID = shiftID;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public LocalTime getShiftStartTime() {
		return shiftStartTime;
	}

	public void setShiftStartTime(LocalTime shiftStartTime) {
		this.shiftStartTime = shiftStartTime;
	}

	public LocalTime getShiftEndTime() {
		return shiftEndTime;
	}

	public void setShiftEndTime(LocalTime shiftEndTime) {
		this.shiftEndTime = shiftEndTime;
	}

	public LocalDate getShiftStartDate() {
		return shiftStartDate;
	}

	public void setShiftStartDate(LocalDate shiftStartDate) {
		this.shiftStartDate = shiftStartDate;
	}

	public LocalDate getShiftEndDate() {
		return shiftEndDate;
	}

	public void setShiftEndDate(LocalDate shiftEndDate) {
		this.shiftEndDate = shiftEndDate;
	}

	public int getThirtyMinBreaks() {
		return thirtyMinBreaks;
	}

	public void setThirtyMinBreaks(int thirtyMinBreaks) {
		this.thirtyMinBreaks = thirtyMinBreaks;
	}

	public int getTenMinBreaks() {
		return tenMinBreaks;
	}

	public void setTenMinBreaks(int tenMinBreaks) {
		this.tenMinBreaks = tenMinBreaks;
	}

	public boolean isRepeating() {
		return repeating;
	}

	public void setRepeating(boolean repeating) {
		this.repeating = repeating;
	}

	public int getDaysPerRepeat() {
		return daysPerRepeat;
	}

	public void setDaysPerRepeat(int daysPerRepeat) {
		this.daysPerRepeat = daysPerRepeat;
	}
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirst_name() {
		return first_name;
	}

	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}

	public String getLast_name() {
		return last_name;
	}

	public void setLast_name(String last_name) {
		this.last_name = last_name;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getProfileBG() {
		return profileBG;
	}

	public void setProfileBG(String profileBG) {
		this.profileBG = profileBG;
	}

	public String getProfileText() {
		return profileText;
	}

	public void setProfileText(String profileText) {
		this.profileText = profileText;
	}

	public LocalDate getOriginalDate() {return originalDate;}

	public void setOriginalDate(LocalDate originalDate) {this.originalDate = originalDate;}

	public int getStoreID() {
		return storeID;
	}

	public void setStoreID(int storeID) {
		this.storeID = storeID;
	}
}
