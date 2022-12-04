package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.DialogPane.Dialog;
import components.ActionableFilterComboBox;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import models.*;
import org.controlsfx.control.PopOver;
import utils.AnimationUtils;
import utils.GUIUtils;
import utils.TableUtils;
import utils.ValidatorUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import static com.dlsc.gemsfx.DialogPane.Type.*;

public class AccountPaymentsPageController extends DateSelectController{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private MainMenuController parent;
    private User selectedUser;
	private MFXDatePicker datePkr;
	private PopOver currentDatePopover;

	@FXML
	private FlowPane datePickerPane;
	@FXML
	private StackPane monthSelector;
	@FXML
	private MFXTextField monthSelectorField;
    @FXML
    private TableView<AccountPayment> accountPaymentTable;
	@FXML
	private MFXTableView<AccountPaymentContactDataPoint> accountTotalsTable;
    @FXML
	private VBox addPaymentPopover, entryFieldBox;
    @FXML
	private Region contentDarken;
	@FXML
	private DialogPane dialogPane;
	@FXML
	private VBox dataEntryRowPane;
	@FXML
	private MFXFilterComboBox contactNameField;
	@FXML
	private MFXDatePicker invoiceDateField,dueDateField;
	@FXML
	private MFXTextField invoiceNoField,descriptionField,amountField;

	@FXML
	private Label afxValidationLabel,invoiceNoValidationLabel,invoiceDateValidationLabel,dueDateValidationLabel,amountValidationLabel;
	@FXML
	private MFXCheckbox accountAdjustedBox;
	@FXML
	private MFXButton saveButton;
	@FXML
	private Button deleteButton;
	@FXML
	private Label paymentPopoverTitle,supplierTotalLabel;
	@FXML
	private MFXComboBox<String> taxRateField;

	private TableColumn<AccountPayment,String> contactCol;
	private TableColumn<AccountPayment,String> invNumberCol;
	private TableColumn<AccountPayment,LocalDate> invDateCol;
	private TableColumn<AccountPayment,LocalDate> dueDateCol;
	private TableColumn<AccountPayment,String> descriptionCol;
	private TableColumn<AccountPayment,Double> unitAmountCol;
	private TableColumn<AccountPayment,String> accountAdjustedCol;
	private MFXTableColumn<AccountPaymentContactDataPoint> contactNameCol;
	private MFXTableColumn<AccountPaymentContactDataPoint> totalCol;
	private ActionableFilterComboBox afx;
	private Dialog<Object> dialog;
	
	 @FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	public void setParent(MainMenuController p){this.parent = p;}

	@Override
	public void fill() {
		accountTotalsTable.autosizeColumnsOnInitialization();


		MFXButton addContactButton = new MFXButton("Create New");
		addContactButton.setOnAction(actionEvent -> {
			dialog = new Dialog(dialogPane, BLANK);
			dialog.setPadding(false);
			dialog.setContent(createAddNewContactDialog());
			dialogPane.showDialog(dialog);
		});
		MFXButton manageContactsButton = new MFXButton("Manage Contacts");
		manageContactsButton.setOnAction(actionEvent -> {
			dialog = new Dialog(dialogPane, BLANK);
			dialog.setPadding(false);
			dialog.setContent(createManageContactsDialog());
			dialogPane.showDialog(dialog);
		});
		afx = new ActionableFilterComboBox(addContactButton,manageContactsButton);

		afx.setFloatMode(FloatMode.ABOVE);
		afx.setFloatingText("Contact name");
		afx.setFloatingTextGap(5);
		afx.setBorderGap(0);
		afx.setStyle("-mfx-gap: 5");
		afx.setMaxWidth(Double.MAX_VALUE);
		afx.setMinHeight(38.4);
		entryFieldBox.getChildren().add(1,afx);

		//Init Payments Table
		contactCol = new TableColumn<>("CONTACT");
		invNumberCol = new TableColumn<>("INVOICE NUMBER");
		invDateCol = new TableColumn<>("INVOICE DATE");
		dueDateCol = new TableColumn<>("DUE DATE");
		descriptionCol = new TableColumn<>("DESCRIPTION");
		unitAmountCol = new TableColumn<>("UNIT AMOUNT");
		accountAdjustedCol = new TableColumn<>("ACCOUNT ADJUSTED?");
		contactCol.setCellValueFactory(new PropertyValueFactory<>("contactName"));
		invNumberCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
		invDateCol.setCellValueFactory(new PropertyValueFactory<>("invDate"));
		dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
		descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
		unitAmountCol.setCellValueFactory(new PropertyValueFactory<>("unitAmount"));
		accountAdjustedCol.setCellValueFactory(new PropertyValueFactory<>("accountAdjusted"));
		accountPaymentTable.getColumns().addAll(
				contactCol,
				invNumberCol,
				invDateCol,
				dueDateCol,
				descriptionCol,
				unitAmountCol,
				accountAdjustedCol
		);
		accountPaymentTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		accountPaymentTable.setMaxWidth(Double.MAX_VALUE);
		accountPaymentTable.setMaxHeight(Double.MAX_VALUE);
		accountPaymentTable.setFixedCellSize(25.0);
		VBox.setVgrow(accountPaymentTable, Priority.ALWAYS);
		for(TableColumn tc: accountPaymentTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		Platform.runLater(() -> GUIUtils.customResize(accountPaymentTable,descriptionCol));
		Platform.runLater(() -> addDoubleClickfunction());

		//Init Totals Table
		contactNameCol = new MFXTableColumn<>("CONTACT",false, Comparator.comparing(AccountPaymentContactDataPoint::getContactName));
		totalCol = new MFXTableColumn<>("TOTAL",false, Comparator.comparing(AccountPaymentContactDataPoint::getTotalValue));
		contactNameCol.setRowCellFactory(accountPaymentContactDataPoint -> new MFXTableRowCell<>(AccountPaymentContactDataPoint::getContactName));
		totalCol.setRowCellFactory(accountPaymentContactDataPoint -> new MFXTableRowCell<>(AccountPaymentContactDataPoint::getTotalValueString));
		accountTotalsTable.getTableColumns().addAll(
				contactNameCol,
				totalCol
		);
		ObservableList<String> taxRates = FXCollections.observableArrayList("BAS Excluded",
																			"GST Free Expenses",
																			"GST Free Income",
																			"GST on Expenses",
																			"GST on Imports",
																			"GST on Income");
		taxRateField.setItems(taxRates);
		taxRateField.setValue("Gst Free Income");

		setDate(LocalDate.now());

		ValidatorUtils.setupRegexValidation(afx,afxValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(invoiceNoField,invoiceNoValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(invoiceDateField,invoiceDateValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(dueDateField,dueDateValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(amountField,amountValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
	}

	private void addDoubleClickfunction(){
		accountPaymentTable.setRowFactory( tv -> {
			TableRow<AccountPayment> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					AccountPayment rowData = row.getItem();
					openPopover(rowData);
				}
			});
			return row ;
		});
	}

	private Node createAddNewContactDialog() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/AddNewContactDialog.fxml"));
		StackPane newContactDialog = null;
		try {
			newContactDialog = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		AddNewContactDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setConnection(this.con);
		dialogController.setMain(this.main);
		return newContactDialog;
	}

	private Node createManageContactsDialog() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/ManageContactsDialog.fxml"));
		StackPane manageContactsDialog = null;
		try {
			manageContactsDialog = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ManageContactsDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setConnection(this.con);
		dialogController.setMain(this.main);
		dialogController.fill();
		return manageContactsDialog;
	}

	public void fillContactList(){
		ObservableList<AccountPaymentContactDataPoint> contacts = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM accountPaymentContacts where storeID = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				contacts.add(new AccountPaymentContactDataPoint(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		afx.setItems(contacts);
	}
	
	

	public void fillTable(){

		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		ObservableList<AccountPayment> currentAccountPaymentDataPoints = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM accountPayments JOIN accountPaymentContacts a on a.idaccountPaymentContacts = accountPayments.contactID WHERE accountPayments.storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentAccountPaymentDataPoints.add(new AccountPayment(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		ObservableList<AccountPaymentContactDataPoint> currentContactTotals = FXCollections.observableArrayList();
		boolean contactFound = false;
		for(AccountPayment a:currentAccountPaymentDataPoints){
			contactFound = false;
			for(AccountPaymentContactDataPoint c: currentContactTotals){
				if(a.getContactName().equals(c.getContactName())){
					c.setTotalValue(c.getTotalValue()+a.getUnitAmount());
					contactFound = true;
				}
			}
			if(!contactFound){
				AccountPaymentContactDataPoint acdp = getContactfromName(a.getContactName());
				acdp.setTotalValue(a.getUnitAmount());
				currentContactTotals.add(acdp);
			}
		}
		accountTotalsTable.setItems(currentContactTotals);
		accountPaymentTable.setItems(currentAccountPaymentDataPoints);
		double supplierTotal=0;
		for(AccountPaymentContactDataPoint acdp:currentContactTotals)
			supplierTotal+=acdp.getTotalValue();
		supplierTotalLabel.setText(NumberFormat.getCurrencyInstance().format(supplierTotal));
		addDoubleClickfunction();
		accountPaymentTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		accountPaymentTable.setMaxWidth(Double.MAX_VALUE);
		accountPaymentTable.setMaxHeight(Double.MAX_VALUE);
		accountPaymentTable.setFixedCellSize(25.0);
		VBox.setVgrow(accountPaymentTable, Priority.ALWAYS);
		for(TableColumn tc: accountPaymentTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		Platform.runLater(() -> GUIUtils.customResize(accountPaymentTable,descriptionCol));
	}

	public void exportToXero() throws FileNotFoundException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data entry File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		File file = fileChooser.showSaveDialog(main.getStg());
		if (file != null) {
			try (PrintWriter pw = new PrintWriter(file)) {
				//TODO: Catch file not found error if this file is in use already
				pw.println("Contact,,,,,,,,,,Invoice number,Invoice date ,Due Date,,Description,Quantity,Unit amount,Account code,GST free,");

				YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
				int daysInMonth = yearMonthObject.lengthOfMonth();

				ObservableList<AccountPayment> currentAccountPaymentDataPoints = FXCollections.observableArrayList();
				String sql = null;
				try {
					sql = "SELECT * FROM accountPayments JOIN accountPaymentContacts a on a.idaccountPaymentContacts = accountPayments.contactID WHERE accountPayments.storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
					preparedStatement.setInt(2, yearMonthObject.getMonthValue());
					preparedStatement.setInt(3, yearMonthObject.getYear());
					resultSet = preparedStatement.executeQuery();
					while (resultSet.next()) {
						currentAccountPaymentDataPoints.add(new AccountPayment(resultSet));
					}
				} catch (SQLException throwables) {
					throwables.printStackTrace();
				}

				for(AccountPayment a: currentAccountPaymentDataPoints){
					pw.print(a.getContactName()+",,,,,,,,,,");
					pw.print(a.getInvoiceNumber()+",");
					pw.print(a.getInvDate()+",");
					pw.print(a.getDueDate()+",,");
					pw.print(a.getDescription()+",1,");
					pw.print("$"+a.getUnitAmount()+",");
					pw.print(a.getAccountCode()+",");
					pw.println(a.getTaxRate());
				}
				dialogPane.showInformation("Success", "Information exported succesfully");
			} catch (FileNotFoundException e){
				dialogPane.showError("Error", "This file could not be accessed, please ensure its not open in another program");
			}
		}
	}

	public void openPopover(){
		saveButton.setOnAction(actionEvent -> addPayment());
		paymentPopoverTitle.setText("Add new account payment");
		deleteButton.setVisible(false);
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addPaymentPopover,0);
		afx.clear();
		invoiceNoField.clear();
		invoiceDateField.clear();
		dueDateField.clear();
		descriptionField.clear();
		amountField.clear();
		accountAdjustedBox.setSelected(false);
		Platform.runLater(() -> afx.requestFocus());
	}

	public void closePopover(){
		AnimationUtils.slideIn(addPaymentPopover,425);
		contentDarken.setVisible(false);
		afxValidationLabel.setVisible(false);
		invoiceNoValidationLabel.setVisible(false);
		invoiceDateValidationLabel.setVisible(false);
		dueDateValidationLabel.setVisible(false);
		amountValidationLabel.setVisible(false);
		saveButton.setDisable(false);
	}

	public void openPopover(AccountPayment ap){
		saveButton.setOnAction(actionEvent -> editPayment(ap));
		paymentPopoverTitle.setText("Edit account payment");
		deleteButton.setVisible(true);
		deleteButton.setOnAction(actionEvent -> deletePayment(ap));
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addPaymentPopover,0);
		afx.setValue(getContactfromName(ap.getContactName()));
		invoiceNoField.setText(ap.getInvoiceNumber());
		invoiceDateField.setValue(ap.getInvDate());
		dueDateField.setValue(ap.getDueDate());
		descriptionField.setText(ap.getDescription());
		amountField.setText(String.valueOf(ap.getUnitAmount()));
		accountAdjustedBox.setSelected(ap.isAccountAdjusted());
		Platform.runLater(() -> afx.requestFocus());
	}

	public AccountPaymentContactDataPoint getContactfromName(String name){
		String sql = null;
		try {
			sql = "SELECT * FROM accountPaymentContacts  WHERE contactName = ? AND storeID = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1,name);
			preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				return new AccountPaymentContactDataPoint(resultSet);
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return null;
	}

	public void monthForward() {
		setDate(main.getCurrentDate().plusMonths(1));
	}

	public void monthBackward() {
		setDate(main.getCurrentDate().minusMonths(1));
	}

	public void openMonthSelector(){
		if(currentDatePopover!=null&&currentDatePopover.isShowing()){
			currentDatePopover.hide();
		}else {
			PopOver monthSelectorMenu = new PopOver();
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/MonthYearSelectorContent.fxml"));
			VBox monthSelectorMenuContent = null;
			try {
				monthSelectorMenuContent = loader.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
			MonthYearSelectorContentController rdc = loader.getController();
			rdc.setMain(main);
			rdc.setConnection(con);
			rdc.setParent(this);
			rdc.fill();

			monthSelectorMenu.setOpacity(1);
			monthSelectorMenu.setContentNode(monthSelectorMenuContent);
			monthSelectorMenu.setArrowSize(0);
			monthSelectorMenu.setAnimated(true);
			monthSelectorMenu.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
			monthSelectorMenu.setAutoHide(true);
			monthSelectorMenu.setDetachable(false);
			monthSelectorMenu.setHideOnEscape(true);
			monthSelectorMenu.setCornerRadius(10);
			monthSelectorMenu.setArrowIndent(0);
			monthSelectorMenu.show(monthSelector);
			currentDatePopover=monthSelectorMenu;
			monthSelectorField.requestFocus();
		}
	}
	public Dialog<Object> getDialog() {
		return dialog;
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		String fieldText = main.getCurrentDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		fieldText += ", ";
		fieldText += main.getCurrentDate().getYear();
		monthSelectorField.setText(fieldText);
		fillTable();
		fillContactList();
	}

	public void addPayment(){
 		Boolean validEntry = true;
 		if(!afx.isValid()){afx.requestFocus();}
		else if(!invoiceNoField.isValid()){invoiceNoField.requestFocus();}
		else if(!dueDateField.isValid()){dueDateField.requestFocus();}
		else if(!amountField.isValid()){amountField.requestFocus();}
		else{
			AccountPaymentContactDataPoint contact = (AccountPaymentContactDataPoint) afx.getSelectedItem();
			String invoiceNo = invoiceNoField.getText();
			LocalDate invoiceDate = invoiceDateField.getValue();
			LocalDate dueDate = dueDateField.getValue();
			String description = descriptionField.getText();
			Double unitAmount = Double.valueOf(amountField.getText());
			String taxRate = taxRateField.getText();
			String sql = "INSERT INTO accountPayments(contactID,storeID,invoiceNo,invoiceDate,dueDate,description,unitAmount,accountAdjusted,taxRate) VALUES(?,?,?,?,?,?,?,?,?)";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setInt(1, contact.getContactID());
				preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
				preparedStatement.setString(3, invoiceNo);
				preparedStatement.setDate(4, Date.valueOf(invoiceDate));
				preparedStatement.setDate(5, Date.valueOf(dueDate));
				preparedStatement.setString(6, description);
				preparedStatement.setDouble(7, unitAmount);
				preparedStatement.setBoolean(8, accountAdjustedBox.isSelected());
				preparedStatement.setString(9, taxRate);
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
			closePopover();
			fillTable();
			dialogPane.showInformation("Success","Payment was succesfully added");
		}

	}

	public void editPayment(AccountPayment accountPayment){
		Boolean validEntry = true;
		if(!afx.isValid()){afx.requestFocus();}
		else if(!invoiceNoField.isValid()){invoiceNoField.requestFocus();}
		else if(!dueDateField.isValid()){dueDateField.requestFocus();}
		else if(!amountField.isValid()){amountField.requestFocus();}
		else{
			String contactName = accountPayment.getContactName();
			String invoiceNo = invoiceNoField.getText();
			LocalDate invoiceDate = invoiceDateField.getValue();
			LocalDate dueDate = dueDateField.getValue();
			String description = descriptionField.getText();
			Double unitAmount = Double.valueOf(amountField.getText());
			String taxRate = taxRateField.getText();

			String sql = "UPDATE accountPayments SET contactID = ?,storeID = ?,invoiceNo = ?, invoiceDate = ?, dueDate = ?,description = ?,unitAmount = ?,accountAdjusted = ?,taxRate = ? WHERE idaccountPayments = ?";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setInt(1, accountPayment.getContactID());
				preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
				preparedStatement.setString(3, invoiceNo);
				preparedStatement.setDate(4, Date.valueOf(invoiceDate));
				preparedStatement.setDate(5, Date.valueOf(dueDate));
				preparedStatement.setString(6, description);
				preparedStatement.setDouble(7, unitAmount);
				preparedStatement.setBoolean(8, accountAdjustedBox.isSelected());
				preparedStatement.setString(9, taxRate);
				preparedStatement.setInt(10, accountPayment.getAccountPaymentID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
			closePopover();
			fillTable();
			dialogPane.showInformation("Success","Payment was succesfully edited");
		}

	}

	public void deletePayment(AccountPayment accountPayment){
		 dialogPane.showWarning("Confirm Delete",
				 "This action will permanently delete this Account payment from all systems,\n" +
				 "Are you sure you still want to delete this Account payment?").thenAccept(buttonType -> {
			 if (buttonType.equals(ButtonType.OK)) {
				 String sql = "DELETE from accountPayments WHERE idaccountPayments = ?";
				 try {
					 preparedStatement = con.prepareStatement(sql);
					 preparedStatement.setInt(1, accountPayment.getAccountPaymentID());
					 preparedStatement.executeUpdate();
				 } catch (SQLException ex) {
					 System.err.println(ex.getMessage());
				 }
				 closePopover();
				 fillTable();
				 dialogPane.showInformation("Success","Payment was succesfully deleted");
			 }
		 });

	}
}
