package models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Employment {



	private int employmentID;
	private String username;
	private int storeID;

	public Employment(ResultSet resultSet) {
		try {
			if(resultSet.getString("employmentsID")!=null)
				this.employmentID = resultSet.getInt("employmentsID");
			if(resultSet.getString("username")!=null)
				this.username = resultSet.getString("username");
			if(resultSet.getString("storeID")!=null)
				this.storeID = resultSet.getInt("storeID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

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
