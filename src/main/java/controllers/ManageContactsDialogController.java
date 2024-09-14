package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import models.AccountPaymentContactDataPoint;
import services.AccountPaymentContactService;
import utils.GUIUtils;
import utils.TableUtils;

import java.io.IOException;
import java.util.List;

public class ManageContactsDialogController extends Controller{
	//TODO: Reconsider how to edit payment contacts (visibility?/hide not delete/show results of update after complete)
	@FXML private TableColumn<AccountPaymentContactDataPoint,String> nameCol;
	@FXML private TableColumn<AccountPaymentContactDataPoint,String> accountCodeCol;
	@FXML private TableColumn<AccountPaymentContactDataPoint,Button> deleteCol;
	@FXML private TableView<AccountPaymentContactDataPoint> contactsTable;
	private AccountPaymentsPageController parent;
	private AccountPaymentContactService accountPaymentContactService;

	@FXML
	private void initialize() {
		try {
			accountPaymentContactService = new AccountPaymentContactService();
		} catch (IOException e) {
			parent.getDialogPane().showError("Error initializing contact service",e);
		}
	}

	public void setParent(AccountPaymentsPageController d) {this.parent = d;}

	public void fill() {
		nameCol.setCellValueFactory(new PropertyValueFactory<>("contactName"));
		accountCodeCol.setCellValueFactory(new PropertyValueFactory<>("accountCode"));
		deleteCol.setCellValueFactory(new PropertyValueFactory<>("deleteButton"));
		editableCols();
		ObservableList<AccountPaymentContactDataPoint> allContacts;
		try {
			List<AccountPaymentContactDataPoint> currentAccountPaymentDataPoints = accountPaymentContactService.getAllAccountPaymentContacts(main.getCurrentStore().getStoreID());
			for(AccountPaymentContactDataPoint a: currentAccountPaymentDataPoints){
				MFXButton delButton = new MFXButton("X");
				delButton.setOnAction(_ -> {
					try {
						accountPaymentContactService.deleteAccountPaymentContact(a.getContactID());
						parent.fillContactList();
						fill();
					} catch (Exception e) {
						parent.getDialogPane().showError("Error","An error occurred while deleting the contact",e);
					}
				});
				a.setDeleteButton(delButton);
			}
			allContacts = FXCollections.observableArrayList(currentAccountPaymentDataPoints);
			contactsTable.setItems(allContacts);
		} catch (Exception e) {
			parent.getDialogPane().showError("Error","An error occurred while loading existing contacts",e);
		}
		for(TableColumn<?,?> tc: contactsTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+20);
		}
		Platform.runLater(() -> GUIUtils.customResize(contactsTable,nameCol));
	}

	private void editableCols() {
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setOnEditCommit(e -> {
			try {
				AccountPaymentContactDataPoint a = e.getTableView().getItems().get(e.getTablePosition().getRow());
				a.setContactName(e.getNewValue());
				accountPaymentContactService.updateAccountPaymentContact(a);
				parent.fillContactList();
			} catch (Exception ex) {
				parent.getDialogPane().showError("Error","An error occurred while updating the contact name",ex);
			}
		});
		accountCodeCol.setCellFactory(TextFieldTableCell.forTableColumn());
		accountCodeCol.setOnEditCommit(e -> {
			try {
				AccountPaymentContactDataPoint a = e.getTableView().getItems().get(e.getTablePosition().getRow());
				a.setAccountCode(e.getNewValue());
				accountPaymentContactService.updateAccountPaymentContact(a);
				parent.fillContactList();
			} catch (Exception ex) {
				parent.getDialogPane().showError("Error","An error occurred while updating the contact account code",ex);
			}
		});
		contactsTable.setEditable(true);
	}
}
