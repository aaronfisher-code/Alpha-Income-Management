package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import models.Invoice;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddNewSupplierDialogController {

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
	private InvoiceEntryController parent;

	@FXML
	private MFXTextField newContactField;

	@FXML
	private void initialize() throws IOException {}

	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	public void setParent(InvoiceEntryController d) {this.parent = d;}

	public void addContact(){
	 	String supplierName = newContactField.getText();
		String sql = "INSERT INTO invoiceSuppliers(supplierName,storeID) VALUES(?,?)";
		try {
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1, supplierName);
			preparedStatement.setInt(2,main.getCurrentStore().getStoreID());
			preparedStatement.executeUpdate();
		} catch (SQLException ex) {
			System.err.println(ex.getMessage());
		}
		parent.getDialog().cancel();
		parent.fillContactList();
	}

	public void closeDialog(){
		parent.getDialog().cancel();
	}


}
