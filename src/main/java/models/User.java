package models;

import java.time.LocalDate;
import java.util.List;

public class User {
	private String username;
	
	private String password;

    private String first_name;

    private String last_name;
    
    private String role;

    private String bgColour;

    private String textColour;

	private LocalDate inactiveDate;

	private List<Employment> employments;

	private List<Permission> permissions;

	public User() {
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

	public List<Employment> getEmployments() {return employments;}

	public void setEmployments(List<Employment> employments) {this.employments = employments;}

	public List<Permission> getPermissions() {return permissions;}

	public void setPermissions(List<Permission> permissions) {this.permissions = permissions;}

	public String toString(){
		return getFirst_name()+" "+getLast_name();
	}
}
