package controllers;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import models.AccountPaymentContactDataPoint;
import services.AccountPaymentContactService;
import java.sql.SQLException;

public class AddNewContactDialogController extends Controller {

	@FXML private MFXTextField newContactField,accountCodeField;
	private AccountPaymentsPageController parent;
	private AccountPaymentContactService accountPaymentContactService;

	@FXML
	private void initialize() {
		accountPaymentContactService = new AccountPaymentContactService();
	}

	public void setParent(AccountPaymentsPageController d) {this.parent = d;}

	public void addContact(){
	 	String contactName = newContactField.getText();
		String accountCode = accountCodeField.getText();
		try {
			accountPaymentContactService.addAccountPaymentContact(new AccountPaymentContactDataPoint(contactName,main.getCurrentStore().getStoreID(),accountCode));
		} catch (SQLException ex) {
			parent.getDialogPane().showError("Error adding contact",ex);
		}
		parent.getDialog().cancel();
		parent.fillContactList();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}
}
