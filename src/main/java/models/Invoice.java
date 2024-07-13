package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Invoice {

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

	private boolean invoiceExists, importExists, creditExists;

	public Invoice(ResultSet resultSet) {
		try {
			this.supplierName = resultSet.getString("supplierName");
			this.supplierID = resultSet.getInt("supplierID");
			this.invoiceNo = resultSet.getString("invoiceNo");
			this.invoiceDate = resultSet.getDate("invoiceDate").toLocalDate();
			this.dueDate = resultSet.getDate("dueDate").toLocalDate();
			this.description = resultSet.getString("description");
			this.unitAmount = resultSet.getDouble("unitAmount");
			this.storeID = resultSet.getInt("storeID");
			this.notes = resultSet.getString("notes");
			this.importedInvoiceAmount = resultSet.getDouble("amount");

			// Check if the column "total_credits" exists
			if (columnExists(resultSet, "total_credits")) {
				this.credits = resultSet.getDouble("total_credits");
			} else {
				this.credits = 0.0; // or some default value
			}

			if (resultSet.getObject("unitAmount") != null) {
				this.invoiceExists = true;
			}
			if (resultSet.getObject("amount") != null) {
				this.variance = unitAmount - importedInvoiceAmount;
				importExists = true;
			}
			this.totalAfterCredits = unitAmount - credits;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private boolean columnExists(ResultSet rs, String columnName) throws SQLException {
		ResultSetMetaData rsMetaData = rs.getMetaData();
		int columnCount = rsMetaData.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			if (columnName.equals(rsMetaData.getColumnName(i))) {
				return true;
			}
		}
		return false;
	}

	public String getSupplierName() {
		return supplierName;
	}

	public void setSupplierName(String supplierName) {
		this.supplierName = supplierName;
	}

	public String getInvoiceNo() {
		return invoiceNo;
	}

	public void setInvoiceNo(String invoiceNo) {
		this.invoiceNo = invoiceNo;
	}

	public LocalDate getInvoiceDate() {
		return invoiceDate;
	}

	public String getInvoiceDateString() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return formatter.format(invoiceDate);
	}

	public void setInvoiceDate(LocalDate invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public String getDueDateString() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return formatter.format(dueDate);
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getUnitAmount() {
		return unitAmount;
	}

	public String getUnitAmountString() {
		return (!invoiceExists) ? "" : NumberFormat.getCurrencyInstance(Locale.US).format(unitAmount);
	}

	public void setUnitAmount(double unitAmount) {
		this.unitAmount = unitAmount;
	}

	public double getImportedInvoiceAmount() {
		return importedInvoiceAmount;
	}

	public String getImportedInvoiceAmountString() {
		return (!importExists) ? "" : NumberFormat.getCurrencyInstance(Locale.US).format(importedInvoiceAmount);
	}

	public void setImportedInvoiceAmount(double importedInvoiceAmount) {
		this.importedInvoiceAmount = importedInvoiceAmount;
	}

	public double getVariance() {
		return variance;
	}

	public String getVarianceString() {
		return (!importExists) ? "" : NumberFormat.getCurrencyInstance(Locale.US).format(variance);
	}

	public void setVariance(double variance) {
		this.variance = variance;
	}

	public double getCredits() {
		return credits;
	}

	public String getCreditsString() {
		return NumberFormat.getCurrencyInstance(Locale.US).format(credits);
	}

	public void setCredits(double credits) {
		this.credits = credits;
	}

	public double getTotalAfterCredits() {
		return totalAfterCredits;
	}

	public String getTotalAfterCreditsString() {
		return NumberFormat.getCurrencyInstance(Locale.US).format(totalAfterCredits);
	}

	public void setTotalAfterCredits(double totalAfterCredits) {
		this.totalAfterCredits = totalAfterCredits;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

}
