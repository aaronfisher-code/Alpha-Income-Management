package models;

import javafx.scene.control.Cell;
import utils.WorkbookProcessor;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

public class TillReportDataPoint {

	private int storeID;
	private LocalDate assignedDate;
	private LocalDate periodStartDate;
	private LocalDate periodEndDate;
	private String key;
	private double quantity;
	private double amount;

	public TillReportDataPoint() {}

	public TillReportDataPoint(CellDataPoint cdp, WorkbookProcessor wp, LocalDate targetDate, int storeID) {
		this.storeID = storeID;
		this.assignedDate = (cdp.getAssignedDate() == null) ? targetDate : cdp.getAssignedDate();
		this.periodStartDate = (wp.getPeriodStart() != null) ? LocalDate.from(wp.getPeriodStart().atZone(ZoneId.of("Australia/Melbourne"))) : null;
		this.periodEndDate = (wp.getPeriodStart() != null) ? LocalDate.from(wp.getPeriodEnd().atZone(ZoneId.of("Australia/Melbourne"))) : null;
		this.key = cdp.getCategory() + ((cdp.getSubCategory() != "") ? "-" + cdp.getSubCategory() : "");
		this.quantity = cdp.getQuantity();
		this.amount = cdp.getAmount();
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
