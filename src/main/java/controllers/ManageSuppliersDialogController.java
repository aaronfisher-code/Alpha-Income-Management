package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import models.InvoiceSupplier;
import services.InvoiceSupplierService;
import java.sql.SQLException;
import java.util.List;

public class ManageSuppliersDialogController extends Controller{
	//TODO: Reconsider how to edit payment contacts (visibility?/hide not delete/show results of update after complete)
	@FXML private TableColumn<InvoiceSupplier,String> nameCol;
	@FXML private TableColumn<InvoiceSupplier,Button> deleteCol;
	@FXML private TableView<InvoiceSupplier> contactsTable;
	private InvoiceEntryController parent;
	private InvoiceSupplierService invoiceSupplierService;

	@FXML
	private void initialize() {
		try{
			invoiceSupplierService = new InvoiceSupplierService();
		} catch (Exception e){
			parent.getDialogPane().showError("Error", "Error initializing the supplier service", e);
		}
	}

	public void setParent(InvoiceEntryController d) {this.parent = d;}

	public void fill() {
		nameCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
		deleteCol.setCellValueFactory(new PropertyValueFactory<>("deleteButton"));
		editableCols();
		try {
			List<InvoiceSupplier> currentInvoiceSuppliers = invoiceSupplierService.getAllInvoiceSuppliers(main.getCurrentStore().getStoreID());
			for(InvoiceSupplier a: currentInvoiceSuppliers){
				MFXButton delButton = new MFXButton("X");
				delButton.setOnAction(_ -> {
					try {
						invoiceSupplierService.deleteInvoiceSupplier(a.getContactID());
						parent.fillContactList();
						fill();
					} catch (Exception ex) {
						parent.getDialogPane().showError("Error", "Error deleting the supplier", ex);
					}
				});
				a.setDeleteButton(delButton);
			}
			contactsTable.setItems(FXCollections.observableArrayList(currentInvoiceSuppliers));
		} catch (Exception ex) {
			parent.getDialogPane().showError("Error", "Error loading invoice suppliers", ex);
		}
	}

	private void editableCols() {
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setOnEditCommit(e -> {
			try {
				InvoiceSupplier updatedSupplier = e.getTableView().getItems().get(e.getTablePosition().getRow());
				updatedSupplier.setSupplierName(e.getNewValue());
				invoiceSupplierService.updateInvoiceSupplier(updatedSupplier);
				parent.fillContactList();
			} catch (Exception ex) {
				parent.getDialogPane().showError("Error", "Error updating the supplier", ex);
			}
		});
		contactsTable.setEditable(true);
	}
}
