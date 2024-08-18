package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXTextField;
import com.dlsc.gemsfx.DialogPane.Dialog;
import javafx.fxml.FXML;
import models.AccountPaymentContactDataPoint;
import services.AccountPaymentContactService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class AddNewContactDialogController{

    private Main main;
	private AccountPaymentsPageController parent;
	private AccountPaymentContactService accountPaymentContactService;

	@FXML
	private MFXTextField newContactField,accountCodeField;

	@FXML
	private void initialize() {
		accountPaymentContactService = new AccountPaymentContactService();
	}

	public void setMain(Main main) {
		this.main = main;
	}

	public void setParent(AccountPaymentsPageController d) {this.parent = d;}

	public void addContact(){
	 	String contactName = newContactField.getText();
		String accountCode = accountCodeField.getText();
		try {
			accountPaymentContactService.addAccountPaymentContact(new AccountPaymentContactDataPoint(contactName,main.getCurrentStore().getStoreID(),accountCode));
		} catch (SQLException ex) {
			parent.getDialogPane().showError("Error adding contact",ex.getMessage());
			ex.printStackTrace();
		}
		parent.getDialog().cancel();
		parent.fillContactList();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}
}
