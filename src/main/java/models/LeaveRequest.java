package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LeaveRequest {
	private int leaveID;
	private int storeID;

	private String employeeID;

	private String employeeFirstName, employeeLastName;

	private String employeeRole;

	private LocalDateTime fromDate;

	private LocalDateTime toDate;

	private String leaveType;

	private String leaveReason;


	public LeaveRequest(ResultSet resultSet) {
		try {
			this.leaveID = resultSet.getInt("leaveID");
			this.storeID = resultSet.getInt("storeID");
			this.employeeID = resultSet.getString("employeeID");
			this.employeeFirstName = resultSet.getString("first_name");
			this.employeeLastName = resultSet.getString("last_name");
			this.employeeRole = resultSet.getString("role");
			this.fromDate = resultSet.getTimestamp("leaveStartDate").toLocalDateTime();
			this.toDate = resultSet.getTimestamp("leaveEndDate").toLocalDateTime();
			this.leaveType = resultSet.getString("leaveType");
			this.leaveReason = resultSet.getString("reason");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public LeaveRequest() {
	}

	public String getEmployeeName(){
		return employeeFirstName + " " + employeeLastName;
	}

	public String getEmployeeRole(){
		return employeeRole;
	}

	public int getLeaveID() {
		return leaveID;
	}

	public void setLeaveID(int leaveID) {
		this.leaveID = leaveID;
	}

	public int getStoreID() {
		return storeID;
	}

	public void setStoreID(int storeID) {
		this.storeID = storeID;
	}

	public String getEmployeeID() {
		return employeeID;
	}

	public void setEmployeeID(String employeeID) {
		this.employeeID = employeeID;
	}

	public LocalDateTime getFromDate() {
		return fromDate;
	}

	public String getFromDateString(){
		return fromDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a"));
	}

	public void setFromDate(LocalDateTime fromDate) {
		this.fromDate = fromDate;
	}

	public LocalDateTime getToDate() {
		return toDate;
	}

	public String getToDateString(){
		return toDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a"));
	}

	public void setToDate(LocalDateTime toDate) {
		this.toDate = toDate;
	}

	public String getLeaveType() {
		return leaveType;
	}

	public void setLeaveType(String leaveType) {
		this.leaveType = leaveType;
	}

	public String getLeaveReason() {
		return leaveReason;
	}

	public void setLeaveReason(String leaveReason) {
		this.leaveReason = leaveReason;
	}

}
