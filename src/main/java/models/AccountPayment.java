package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AccountPayment {
	private String contactName;
	private int contactID;
	private int storeID;
	private String invoiceNumber;
	private LocalDate invDate;
	private LocalDate dueDate;
	private String description;
	private double quantity;
	private double unitAmount;
	private boolean accountAdjusted;
	private String accountCode;
	private String taxRate;

	public AccountPayment(ResultSet resultSet) {
		try {
			this.contactName = resultSet.getString("contactName");
			this.contactID = resultSet.getInt("contactID");
			this.storeID = resultSet.getInt("storeID");
			this.invoiceNumber = resultSet.getString("invoiceNo");
			this.invDate = resultSet.getDate("invoiceDate").toLocalDate();
			this.dueDate = resultSet.getDate("dueDate").toLocalDate();
			this.description = resultSet.getString("description");
			this.unitAmount = resultSet.getDouble("unitAmount");
			this.accountAdjusted = resultSet.getBoolean("accountAdjusted");
			this.taxRate = resultSet.getString("taxRate");
			this.accountCode = resultSet.getString("accountCode");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public AccountPayment(String contactName, int contactID, int storeID, String invoiceNumber, LocalDate invDate, LocalDate dueDate, String description, double quantity, double unitAmount, boolean accountAdjusted, String accountCode, String taxRate) {
		this.contactName = contactName;
		this.contactID = contactID;
		this.storeID = storeID;
		this.invoiceNumber = invoiceNumber;
		this.invDate = invDate;
		this.dueDate = dueDate;
		this.description = description;
		this.quantity = quantity;
		this.unitAmount = unitAmount;
		this.accountAdjusted = accountAdjusted;
		this.accountCode = accountCode;
		this.taxRate = taxRate;
	}

	public String getContactName() {return contactName;}
	public void setContactName(String contactName) {this.contactName = contactName;}
	public int getContactID() {return contactID;}
	public void setContactID(int contactID) {this.contactID = contactID;}
	public int getStoreID() {return storeID;}
	public void setStoreID(int storeID) {this.storeID = storeID;}
	public String getInvoiceNumber() {return invoiceNumber;}
	public String getInvoiceNumberString() {return invoiceNumber==null?"":invoiceNumber;}
	public void setInvoiceNumber(String invoiceNumber) {this.invoiceNumber = invoiceNumber;}
	public LocalDate getInvDate() {return invDate;}

	public String getInvDateString() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return formatter.format(invDate);
	}
	public void setInvDate(LocalDate invDate) {this.invDate = invDate;}
	public LocalDate getDueDate() {return dueDate;}

	public String getDueDateString() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return formatter.format(dueDate);
	}
	public void setDueDate(LocalDate dueDate) {this.dueDate = dueDate;}
	public String getDescription() {return description;}
	public void setDescription(String description) {this.description = description;}
	public double getQuantity() {return quantity;}
	public void setQuantity(double quantity) {this.quantity = quantity;}
	public double getUnitAmount() {return unitAmount;}
	public void setUnitAmount(double unitAmount) {this.unitAmount = unitAmount;}
	public boolean isAccountAdjusted() {return accountAdjusted;}

	public String getAccountAdjustedString() {
		return accountAdjusted?"Y":"";
	}
	public void setAccountAdjusted(boolean accountAdjusted) {this.accountAdjusted = accountAdjusted;}
	public String getAccountCode() {return accountCode;}
	public void setAccountCode(String accountCode) {this.accountCode = accountCode;}
	public String getTaxRate() {return taxRate;}
	public void setTaxRate(String taxRate) {this.taxRate = taxRate;}
	public String getUnitAmountString(){return NumberFormat.getCurrencyInstance(Locale.US).format(unitAmount);}
}
