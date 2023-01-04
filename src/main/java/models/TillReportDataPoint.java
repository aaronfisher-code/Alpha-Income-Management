package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class TillReportDataPoint {

	private int storeID;
	private LocalDate assignedDate;
	private LocalDate periodStartDate;
	private LocalDate periodEndDate;
	private String key;
	private double quantity;
	private double amount;

	public TillReportDataPoint(ResultSet resultSet) {
		try {
			this.storeID = resultSet.getInt("storeID");
			this.assignedDate = resultSet.getDate("assignedDate").toLocalDate();
			if(resultSet.getDate("periodStartDate")!=null)
				this.periodStartDate = resultSet.getDate("periodStartDate").toLocalDate();
			if(resultSet.getDate("periodEndDate")!=null)
				this.periodEndDate = resultSet.getDate("periodEndDate").toLocalDate();
			if(resultSet.getString("key")!=null)
				this.key = resultSet.getString("key");
			this.quantity = resultSet.getDouble("quantity");
			this.amount = resultSet.getDouble("amount");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getStoreID() {
		return storeID;
	}

	public void setStoreID(int storeID) {
		this.storeID = storeID;
	}

	public LocalDate getAssignedDate() {
		return assignedDate;
	}

	public void setAssignedDate(LocalDate assignedDate) {
		this.assignedDate = assignedDate;
	}

	public LocalDate getPeriodStartDate() {
		return periodStartDate;
	}

	public void setPeriodStartDate(LocalDate periodStartDate) {
		this.periodStartDate = periodStartDate;
	}

	public LocalDate getPeriodEndDate() {
		return periodEndDate;
	}

	public void setPeriodEndDate(LocalDate periodEndDate) {
		this.periodEndDate = periodEndDate;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public double getQuantity() {
		return quantity;
	}

	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

}
