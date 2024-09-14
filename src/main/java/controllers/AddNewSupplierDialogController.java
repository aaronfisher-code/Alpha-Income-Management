package controllers;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import models.InvoiceSupplier;
import services.InvoiceSupplierService;

import java.io.IOException;

public class AddNewSupplierDialogController extends Controller {

	@FXML private MFXTextField newContactField;
	private InvoiceEntryController parent;
	private InvoiceSupplierService invoiceSupplierService;

	@FXML
	private void initialize() {
		try {
			invoiceSupplierService = new InvoiceSupplierService();
		} catch (IOException e) {
			parent.getDialogPane().showError("Error initializing supplier service",e);
		}
	}

	public void setParent(InvoiceEntryController d) {this.parent = d;}

	public void addContact(){
		try {
			InvoiceSupplier newInvoiceSupplier = new InvoiceSupplier();
			newInvoiceSupplier.setSupplierName(newContactField.getText());
			newInvoiceSupplier.setStoreID(main.getCurrentStore().getStoreID());
			invoiceSupplierService.addInvoiceSupplier(newInvoiceSupplier);
		} catch (Exception ex) {
			parent.getDialogPane().showError("Error", "Error adding new supplier", ex);
		}
		parent.getDialog().cancel();
		parent.fillContactList();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}
}
