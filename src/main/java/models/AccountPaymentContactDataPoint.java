package models;

import java.sql.ResultSet;
import java.time.LocalDate;

public class AccountPaymentContactDataPoint {
	private String contactName;
	private double totalValue;

	public AccountPaymentContactDataPoint(ResultSet resultSet) {
	}

	public String getContactName() {return contactName;}

	public void setContactName(String contactName) {this.contactName = contactName;}

	public double getTotalValue() {return totalValue;}

	public void setTotalValue(double totalValue) {this.totalValue = totalValue;}

}
