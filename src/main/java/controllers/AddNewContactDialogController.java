package controllers;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import models.AccountPaymentContactDataPoint;
import services.AccountPaymentContactService;

import java.io.IOException;

public class AddNewContactDialogController extends Controller {

	@FXML private MFXTextField newContactField,accountCodeField;
	private AccountPaymentsPageController parent;
	private AccountPaymentContactService accountPaymentContactService;

	@FXML
	private void initialize() {
		try{
			accountPaymentContactService = new AccountPaymentContactService();
		}catch (IOException ex){
			parent.getDialogPane().showError("Error initializing contact service",ex);
		}
	}

	public void setParent(AccountPaymentsPageController d) {this.parent = d;}

	public void addContact(){
	 	String contactName = newContactField.getText();
		String accountCode = accountCodeField.getText();
		try {
			accountPaymentContactService.addAccountPaymentContact(new AccountPaymentContactDataPoint(contactName,main.getCurrentStore().getStoreID(),accountCode));
		} catch (Exception ex) {
			parent.getDialogPane().showError("Error adding contact",ex);
		}
		parent.getDialog().cancel();
		parent.fillContactList();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}
}
