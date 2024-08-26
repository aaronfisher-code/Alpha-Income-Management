package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.palexdev.materialfx.controls.MFXButton;

import java.sql.ResultSet;
import java.sql.SQLException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceSupplier {

	private int contactID;
	private String supplierName;
	private int storeID;
	private MFXButton deleteButton;

	public InvoiceSupplier(int contactID, String supplierName, int storeID){
		this.contactID = contactID;
		this.supplierName = supplierName;
		this.storeID = storeID;
	}

	public InvoiceSupplier() {
	}

	public int getContactID() {return contactID;}

	public void setContactID(int contactID) {this.contactID = contactID;}

	public String getSupplierName() {return supplierName;}

	public void setSupplierName(String supplierName) {this.supplierName = supplierName;}

	public MFXButton getDeleteButton() {return deleteButton;}

	public void setDeleteButton(MFXButton deleteButton) {this.deleteButton = deleteButton;}

	public int getStoreID() {
		return storeID;
	}

	public void setStoreID(int storeID) {
		this.storeID = storeID;
	}

	@Override
	public String toString() {
		return supplierName;
	}
}
