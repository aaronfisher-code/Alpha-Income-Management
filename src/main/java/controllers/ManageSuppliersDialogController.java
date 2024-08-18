package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
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
import java.time.LocalDate;
import java.util.List;

public class ManageSuppliersDialogController extends DateSelectController{
	//TODO: Reconsider how to edit payment contacts (visibility?/hide not delete/show results of update after complete)
	@FXML private TableColumn<InvoiceSupplier,String> nameCol;
	@FXML private TableColumn<InvoiceSupplier,Button> deleteCol;
	@FXML private TableView<InvoiceSupplier> contactsTable;
	private Main main;
	private InvoiceEntryController parent;
	private InvoiceSupplierService invoiceSupplierService;

	@FXML
	private void initialize() {
		invoiceSupplierService = new InvoiceSupplierService();
	}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}

	public void setParent(InvoiceEntryController d) {this.parent = d;}

	@Override
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
					} catch (SQLException ex) {
						parent.getDialogPane().showError("Error", "Error deleting the supplier", ex.getMessage());
						ex.printStackTrace();
					}
				});
				a.setDeleteButton(delButton);
			}
			contactsTable.setItems(FXCollections.observableArrayList(currentInvoiceSuppliers));
		} catch (SQLException ex) {
			parent.getDialogPane().showError("Error", "Error loading invoice suppliers", ex.getMessage());
			ex.printStackTrace();
		}
	}

	@Override
	public void setDate(LocalDate date) {}

	private void editableCols() {
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setOnEditCommit(e -> {
			try {
				InvoiceSupplier updatedSupplier = e.getTableView().getItems().get(e.getTablePosition().getRow());
				updatedSupplier.setSupplierName(e.getNewValue());
				invoiceSupplierService.updateInvoiceSupplier(updatedSupplier);
				parent.fillContactList();
			} catch (SQLException ex) {
				parent.getDialogPane().showError("Error", "Error updating the supplier", ex.getMessage());
				ex.printStackTrace();
			}
		});
		contactsTable.setEditable(true);
	}
}
