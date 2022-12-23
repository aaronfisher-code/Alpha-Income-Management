package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class Invoice {

	private int invoiceID;
	private String supplierName;

	private int storeID;

	private int supplierID;
	private String invoiceNo;
	private LocalDate invoiceDate;
	private LocalDate dueDate;
	private String description;
	private int quantity;
	private double unitAmount;
	private double importedInvoiceAmount;
	private double variance;
	private double credits;
	private double totalAfterCredits;
	private String notes;

	public Invoice(ResultSet resultSet) {
		try {
			this.invoiceID = resultSet.getInt("idInvoices");
			this.supplierName = resultSet.getString("supplierName");
			this.supplierID = resultSet.getInt("supplierID");
			this.invoiceNo = resultSet.getString("invoiceNo");
			this.invoiceDate = resultSet.getDate("invoiceDate").toLocalDate();
			this.dueDate = resultSet.getDate("dueDate").toLocalDate();
			this.description = resultSet.getString("description");
			this.unitAmount = resultSet.getDouble("unitAmount");
			this.storeID = resultSet.getInt("storeID");
			this.notes = resultSet.getString("notes");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String getSupplierName() {return supplierName;}

	public void setSupplierName(String supplierName) {this.supplierName = supplierName;}

	public String getInvoiceNo() {return invoiceNo;}

	public void setInvoiceNo(String invoiceNo) {this.invoiceNo = invoiceNo;}

	public LocalDate getInvoiceDate() {return invoiceDate;}

	public void setInvoiceDate(LocalDate invoiceDate) {this.invoiceDate = invoiceDate;}

	public LocalDate getDueDate() {return dueDate;}

	public void setDueDate(LocalDate dueDate) {this.dueDate = dueDate;}

	public String getDescription() {return description;}

	public void setDescription(String description) {this.description = description;}

	public int getQuantity() {return quantity;}

	public void setQuantity(int quantity) {this.quantity = quantity;}

	public double getUnitAmount() {return unitAmount;}

	public void setUnitAmount(double unitAmount) {this.unitAmount = unitAmount;}

	public double getImportedInvoiceAmount() {return importedInvoiceAmount;}

	public void setImportedInvoiceAmount(double importedInvoiceAmount) {this.importedInvoiceAmount = importedInvoiceAmount;}

	public double getVariance() {return variance;}

	public void setVariance(double variance) {this.variance = variance;}

	public double getCredits() {return credits;}

	public void setCredits(double credits) {this.credits = credits;}

	public double getTotalAfterCredits() {return totalAfterCredits;}

	public void setTotalAfterCredits(double totalAfterCredits) {this.totalAfterCredits = totalAfterCredits;}

	public String getNotes() {return notes;}

	public void setNotes(String notes) {this.notes = notes;}

}
