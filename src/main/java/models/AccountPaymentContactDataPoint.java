package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class AccountPaymentContactDataPoint {


	private int contactID;
	private String contactName;
	private int storeID;
	private double totalValue;

	public AccountPaymentContactDataPoint(ResultSet resultSet) {
		try {
			this.contactID = resultSet.getInt("idaccountPaymentContacts");
			this.contactName = resultSet.getString("contactName");
			this.storeID = resultSet.getInt("storeID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getContactID() {return contactID;}

	public void setContactID(int contactID) {this.contactID = contactID;}

	public String getContactName() {return contactName;}

	public void setContactName(String contactName) {this.contactName = contactName;}

	public double getTotalValue() {return totalValue;}

	public void setTotalValue(double totalValue) {this.totalValue = totalValue;}

	@Override
	public String toString() {
		return contactName;
	}
}
