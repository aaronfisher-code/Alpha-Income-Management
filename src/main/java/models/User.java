package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

public class User {
	private String username;
	
	private String password;

    private String first_name;

    private String last_name;
    
    private String role;

    private String bgColour;

    private String textColour;

	private LocalDate inactiveDate;

	private ArrayList<Employment> employments;

	private ArrayList<Permission> permissions;

	public User(ResultSet resultSet) {
		try {
			if(resultSet.getString("username")!=null)
				this.username = resultSet.getString("username");
			if(resultSet.getString("password")!=null)
				this.password = resultSet.getString("password");
			if(resultSet.getString("first_name")!=null)
				this.first_name = resultSet.getString("first_name");
			if(resultSet.getString("last_name")!=null)
				this.last_name = resultSet.getString("last_name");
			if(resultSet.getString("role")!=null)
				this.role = resultSet.getString("role");
			if(resultSet.getString("profileBG")!=null)
				this.bgColour = resultSet.getString("profileBG");
			if(resultSet.getString("profileText")!=null)
				this.textColour = resultSet.getString("profileText");
			if(resultSet.getString("inactiveDate")!=null)
				this.inactiveDate = LocalDate.parse(resultSet.getString("inactiveDate"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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

	public String getBgColour() { return bgColour; }

	public void setBgColour(String bgColour) { this.bgColour = bgColour; }

	public String getTextColour() { return textColour; }

	public void setTextColour(String textColour) { this.textColour = textColour; }

	public LocalDate getInactiveDate() { return inactiveDate; }

	public void setInactiveDate(LocalDate inactiveDate) { this.inactiveDate = inactiveDate; }

	public ArrayList<Employment> getEmployments() {return employments;}

	public void setEmployments(ArrayList<Employment> employments) {this.employments = employments;}

	public ArrayList<Permission> getPermissions() {return permissions;}

	public void setPermissions(ArrayList<Permission> permissions) {this.permissions = permissions;}

	public String toString(){
		return getFirst_name()+" "+getLast_name();
	}
}
