package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXTextField;
import com.dlsc.gemsfx.DialogPane.Dialog;
import javafx.fxml.FXML;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class AddNewContactDialogController extends DateSelectController{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
	private AccountPaymentsPageController parent;

	@FXML
	private MFXTextField newContactField;

	@FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	public void setParent(AccountPaymentsPageController d) {this.parent = d;}

	@Override
	public void fill() {}

	@Override
	public void setDate(LocalDate date) {}

	public void addContact(){
	 	String contactName = newContactField.getText();
		String sql = "INSERT INTO accountPaymentContacts(contactName,storeID) VALUES(?,?)";
		try {
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1, contactName);
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
