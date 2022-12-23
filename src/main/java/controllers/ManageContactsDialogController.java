package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.controls.legacy.MFXLegacyTableView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import models.AccountPayment;
import models.AccountPaymentContactDataPoint;
import models.EODDataPoint;
import utils.GUIUtils;
import utils.TableUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.ResourceBundle;

public class ManageContactsDialogController extends DateSelectController{
	//TODO: Reconsider how to edit payment contacts (visibility?/hide not delete/show results of update after complete)
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
	private AccountPaymentsPageController parent;

	@FXML
	private TableColumn<AccountPaymentContactDataPoint,String> nameCol;
	@FXML
	private TableColumn<AccountPaymentContactDataPoint,String> accountCodeCol;
	@FXML
	private TableColumn<AccountPaymentContactDataPoint,Button> deleteCol;

	@FXML
	private TableView<AccountPaymentContactDataPoint> contactsTable;

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
	public void fill() {
		nameCol.setCellValueFactory(new PropertyValueFactory<>("contactName"));
		accountCodeCol.setCellValueFactory(new PropertyValueFactory<>("accountCode"));
		deleteCol.setCellValueFactory(new PropertyValueFactory<>("deleteButton"));
		editableCols();

		ObservableList<AccountPaymentContactDataPoint> currentAccountPaymentDataPoints = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM accountPaymentContacts  WHERE accountPaymentContacts.storeID = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				AccountPaymentContactDataPoint a = new AccountPaymentContactDataPoint(resultSet);
				MFXButton delButton = new MFXButton("X");
				delButton.setOnAction(actionEvent -> {
					String sqlQuery = "DELETE from accountPaymentContacts WHERE idaccountPaymentContacts = ?";
					try {
						preparedStatement = con.prepareStatement(sqlQuery);
						preparedStatement.setInt(1, a.getContactID());
						preparedStatement.executeUpdate();
					} catch (SQLException ex) {
						System.err.println(ex.getMessage());
					}
				});
				a.setDeleteButton(delButton);
				currentAccountPaymentDataPoints.add(a);
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		contactsTable.setItems(currentAccountPaymentDataPoints);
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
			String sql = "UPDATE accountPaymentContacts SET contactName = ? WHERE idaccountPaymentContacts = ?";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, e.getNewValue());
				preparedStatement.setInt(2, e.getTableView().getItems().get(e.getTablePosition().getRow()).getContactID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
		});

		accountCodeCol.setCellFactory(TextFieldTableCell.forTableColumn());
		accountCodeCol.setOnEditCommit(e -> {
			String sql = "UPDATE accountPaymentContacts SET accountCode = ? WHERE idaccountPaymentContacts = ?";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, e.getNewValue());
				preparedStatement.setInt(2, e.getTableView().getItems().get(e.getTablePosition().getRow()).getContactID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
		});
		contactsTable.setEditable(true);
	}


}
