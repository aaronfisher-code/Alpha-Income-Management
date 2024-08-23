package controllers;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import models.InvoiceSupplier;
import services.InvoiceSupplierService;
import java.sql.SQLException;

public class AddNewSupplierDialogController extends Controller {

	@FXML private MFXTextField newContactField;
	private InvoiceEntryController parent;
	private InvoiceSupplierService invoiceSupplierService;

	@FXML
	private void initialize() {
		invoiceSupplierService = new InvoiceSupplierService();
	}

	public void setParent(InvoiceEntryController d) {this.parent = d;}

	public void addContact(){
		try {
			InvoiceSupplier newInvoiceSupplier = new InvoiceSupplier();
			newInvoiceSupplier.setSupplierName(newContactField.getText());
			newInvoiceSupplier.setStoreID(main.getCurrentStore().getStoreID());
			invoiceSupplierService.addInvoiceSupplier(newInvoiceSupplier);
		} catch (SQLException ex) {
			parent.getDialogPane().showError("Error", "Error adding new supplier", ex);
		}
		parent.getDialog().cancel();
		parent.fillContactList();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}
}
