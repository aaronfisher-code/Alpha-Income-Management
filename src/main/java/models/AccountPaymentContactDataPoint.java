package models;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.scene.control.Button;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

public class AccountPaymentContactDataPoint {


	private int contactID;
	private String contactName;
	private int storeID;

	private String accountCode;
	private double totalValue;
	private MFXButton deleteButton;

	public AccountPaymentContactDataPoint(ResultSet resultSet) {
		try {
			this.contactID = resultSet.getInt("idaccountPaymentContacts");
			this.contactName = resultSet.getString("contactName");
			this.accountCode = resultSet.getString("accountCode");
			this.storeID = resultSet.getInt("storeID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public AccountPaymentContactDataPoint(int contactID,String contactName, int storeID){
		this.contactID = contactID;
		this.contactName = contactName;
		this.storeID = storeID;
	}

	public int getContactID() {return contactID;}

	public void setContactID(int contactID) {this.contactID = contactID;}

	public String getContactName() {return contactName;}

	public void setContactName(String contactName) {this.contactName = contactName;}

	public double getTotalValue() {return totalValue;}

	public void setTotalValue(double totalValue) {this.totalValue = totalValue;}

	public String getTotalValueString(){return NumberFormat.getCurrencyInstance(Locale.US).format(totalValue);}

	public MFXButton getDeleteButton() {return deleteButton;}

	public void setDeleteButton(MFXButton deleteButton) {this.deleteButton = deleteButton;}

	public String getAccountCode() {return accountCode;}

	public void setAccountCode(String accountCode) {this.accountCode = accountCode;}

	@Override
	public String toString() {
		return contactName;
	}
}
