package models;

import java.sql.ResultSet;
import java.time.LocalDate;

public class AccountPayment {

	private int accountPaymentID;
	private String contactName;
	private String invoiceNumber;
	private LocalDate invDate;
	private LocalDate dueDate;
	private String description;
	private double quantity;
	private double unitAmount;
	private boolean accountAdjusted;

	public AccountPayment(ResultSet resultSet) {
	}

	public int getAccountPaymentID() {return accountPaymentID;}
	public void setAccountPaymentID(int accountPaymentID) {this.accountPaymentID = accountPaymentID;}
	public String getContactName() {return contactName;}
	public void setContactName(String contactName) {this.contactName = contactName;}
	public String getInvoiceNumber() {return invoiceNumber;}
	public void setInvoiceNumber(String invoiceNumber) {this.invoiceNumber = invoiceNumber;}
	public LocalDate getInvDate() {return invDate;}
	public void setInvDate(LocalDate invDate) {this.invDate = invDate;}
	public LocalDate getDueDate() {return dueDate;}
	public void setDueDate(LocalDate dueDate) {this.dueDate = dueDate;}
	public String getDescription() {return description;}
	public void setDescription(String description) {this.description = description;}
	public double getQuantity() {return quantity;}
	public void setQuantity(double quantity) {this.quantity = quantity;}
	public double getUnitAmount() {return unitAmount;}
	public void setUnitAmount(double unitAmount) {this.unitAmount = unitAmount;}
	public boolean isAccountAdjusted() {return accountAdjusted;}
	public void setAccountAdjusted(boolean accountAdjusted) {this.accountAdjusted = accountAdjusted;}

}
