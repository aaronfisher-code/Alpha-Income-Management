package models;

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

	public Invoice(){}

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

	public int getStoreID() {
		return storeID;
	}

	public void setStoreID(int storeID) {
		this.storeID = storeID;
	}

	public int getSupplierID() {
		return supplierID;
	}

	public void setSupplierID(int supplierID) {
		this.supplierID = supplierID;
	}

	public boolean isInvoiceExists() {
		return invoiceExists;
	}

	public void setInvoiceExists(boolean invoiceExists) {
		this.invoiceExists = invoiceExists;
	}

	public boolean isImportExists() {
		return importExists;
	}

	public void setImportExists(boolean importExists) {
		this.importExists = importExists;
	}

	public boolean isCreditExists() {
		return creditExists;
	}

	public void setCreditExists(boolean creditExists) {
		this.creditExists = creditExists;
	}

}
