package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import models.Invoice;
import models.InvoiceSupplier;
import services.InvoiceSupplierService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddNewSupplierDialogController {

	@FXML private MFXTextField newContactField;
    private Main main;
	private InvoiceEntryController parent;
	private InvoiceSupplierService invoiceSupplierService;

	@FXML
	private void initialize() {
		invoiceSupplierService = new InvoiceSupplierService();
	}

	public void setMain(Main main) {
		this.main = main;
	}

	public void setParent(InvoiceEntryController d) {this.parent = d;}

	public void addContact(){
		try {
			InvoiceSupplier newInvoiceSupplier = new InvoiceSupplier();
			newInvoiceSupplier.setSupplierName(newContactField.getText());
			newInvoiceSupplier.setStoreID(main.getCurrentStore().getStoreID());
			invoiceSupplierService.addInvoiceSupplier(newInvoiceSupplier);
		} catch (SQLException ex) {
			parent.getDialogPane().showError("Error", "Error adding new supplier", ex.getMessage());
			ex.printStackTrace();
		}
		parent.getDialog().cancel();
		parent.fillContactList();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}
}
