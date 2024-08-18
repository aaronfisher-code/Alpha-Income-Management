package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
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

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class ManageContactsDialogController extends DateSelectController{
	//TODO: Reconsider how to edit payment contacts (visibility?/hide not delete/show results of update after complete)
    private Main main;
	private AccountPaymentsPageController parent;
	private AccountPaymentContactService accountPaymentContactService;

	@FXML
	private TableColumn<AccountPaymentContactDataPoint,String> nameCol;
	@FXML
	private TableColumn<AccountPaymentContactDataPoint,String> accountCodeCol;
	@FXML
	private TableColumn<AccountPaymentContactDataPoint,Button> deleteCol;
	@FXML
	private TableView<AccountPaymentContactDataPoint> contactsTable;

	@FXML
	private void initialize() {
		accountPaymentContactService = new AccountPaymentContactService();
	}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}

	public void setParent(AccountPaymentsPageController d) {this.parent = d;}

	@Override
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
					} catch (SQLException e) {
						parent.getDialogPane().showError("Error","An error occurred while deleting the contact",e.getMessage());
						e.printStackTrace();
					}
				});
				a.setDeleteButton(delButton);
			}
			allContacts = FXCollections.observableArrayList(currentAccountPaymentDataPoints);
			contactsTable.setItems(allContacts);
		} catch (SQLException e) {
			parent.getDialogPane().showError("Error","An error occurred while loading existing contacts",e.getMessage());
			e.printStackTrace();
		}

		for(TableColumn tc: contactsTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+20);
		}
		Platform.runLater(() -> GUIUtils.customResize(contactsTable,nameCol));
	}

	@Override
	public void setDate(LocalDate date) {}

	private void editableCols() {
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setOnEditCommit(e -> {
			try {
				AccountPaymentContactDataPoint a = e.getTableView().getItems().get(e.getTablePosition().getRow());
				a.setContactName(e.getNewValue());
				accountPaymentContactService.updateAccountPaymentContact(a);
				parent.fillContactList();
			} catch (SQLException ex) {
				parent.getDialogPane().showError("Error","An error occurred while updating the contact name",ex.getMessage());
				ex.printStackTrace();
			}
		});

		accountCodeCol.setCellFactory(TextFieldTableCell.forTableColumn());
		accountCodeCol.setOnEditCommit(e -> {
			try {
				AccountPaymentContactDataPoint a = e.getTableView().getItems().get(e.getTablePosition().getRow());
				a.setAccountCode(e.getNewValue());
				accountPaymentContactService.updateAccountPaymentContact(a);
				parent.fillContactList();
			} catch (SQLException ex) {
				parent.getDialogPane().showError("Error","An error occurred while updating the contact account code",ex.getMessage());
				ex.printStackTrace();
			}
		});
		contactsTable.setEditable(true);
	}
}
