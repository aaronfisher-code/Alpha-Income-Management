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
	private Dialog parent;

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

	public void setParent(Dialog d) {this.parent = d;}

	@Override
	public void fill() {}

	@Override
	public void setDate(LocalDate date) {}

	public void addContact(){
	 	String contactName = newContactField.getText();
		String sql = "INSERT INTO accountPaymentContacts(contactName) VALUES(?)";
		try {
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1, contactName);
			preparedStatement.executeUpdate();
		} catch (SQLException ex) {
			System.err.println(ex.getMessage());
		}
		parent.cancel();
	}

	public void closeDialog(){
		parent.cancel();
	}


}
