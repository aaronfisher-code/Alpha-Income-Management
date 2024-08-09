package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class SpecialDateObj {
	private int eventID;
	private LocalDate eventDate;
	private String storeStatus;
	private String note;

	public SpecialDateObj(ResultSet resultSet) {
		try {
			if(resultSet.getString("eventID")!=null)
				this.eventID = resultSet.getInt("eventID");
			if(resultSet.getString("eventDate")!=null)
				this.eventDate = LocalDate.parse(resultSet.getString("eventDate"));
			if(resultSet.getString("storeStatus")!=null)
				this.storeStatus = resultSet.getString("storeStatus");
			if(resultSet.getString("note")!=null)
				this.note = resultSet.getString("note");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public SpecialDateObj() {
	}

	public int getEventID() {
		return eventID;
	}

	public void setEventID(int eventID) {
		this.eventID = eventID;
	}

	public LocalDate getEventDate() {
		return eventDate;
	}

	public void setEventDate(LocalDate eventDate) {
		this.eventDate = eventDate;
	}

	public String getStoreStatus() {
		return storeStatus;
	}

	public void setStoreStatus(String storeStatus) {
		this.storeStatus = storeStatus;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
