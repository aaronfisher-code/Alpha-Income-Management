package models;

import io.github.palexdev.materialfx.controls.MFXButton;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InvoiceSupplier {


	private int contactID;
	private String supplierName;
	private int storeID;
	private MFXButton deleteButton;

	public InvoiceSupplier(ResultSet resultSet) {
		try {
			this.contactID = resultSet.getInt("idinvoiceSuppliers");
			this.supplierName = resultSet.getString("supplierName");
			this.storeID = resultSet.getInt("storeID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public InvoiceSupplier(int contactID, String supplierName, int storeID){
		this.contactID = contactID;
		this.supplierName = supplierName;
		this.storeID = storeID;
	}

	public int getContactID() {return contactID;}

	public void setContactID(int contactID) {this.contactID = contactID;}

	public String getSupplierName() {return supplierName;}

	public void setSupplierName(String supplierName) {this.supplierName = supplierName;}

	public MFXButton getDeleteButton() {return deleteButton;}

	public void setDeleteButton(MFXButton deleteButton) {this.deleteButton = deleteButton;}

	@Override
	public String toString() {
		return supplierName;
	}
}
