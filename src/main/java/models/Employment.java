package models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Employment {



	private int employmentID;
	private String username;
	private int storeID;

	public Employment() {}

	public int getEmploymentID() {
		return employmentID;
	}

	public void setEmploymentID(int employmentID) {
		this.employmentID = employmentID;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getStoreID() {
		return storeID;
	}

	public void setStoreID(int storeID) {
		this.storeID = storeID;
	}
}
