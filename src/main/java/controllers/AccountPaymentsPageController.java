package controllers;

import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.DialogPane.Dialog;
import com.jfoenix.controls.JFXNodesList;
import components.ActionableFilterComboBox;
import components.CustomDateStringConverter;
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
import services.AccountPaymentContactService;
import services.AccountPaymentService;
import utils.AnimationUtils;
import utils.TableUtils;
import utils.ValidatorUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AccountPaymentsPageController extends DateSelectController{

	@FXML private TableView<AccountPayment> accountPaymentTable;
	@FXML private MFXTableView<AccountPaymentContactDataPoint> accountTotalsTable;
	@FXML private VBox addPaymentPopover, entryFieldBox;
	@FXML private Region contentDarken;
	@FXML private MFXDatePicker invoiceDateField,dueDateField;
	@FXML private MFXTextField invoiceNoField,descriptionField,amountField;
	@FXML private Label afxValidationLabel,invoiceNoValidationLabel,invoiceDateValidationLabel,dueDateValidationLabel,amountValidationLabel;
	@FXML private MFXCheckbox accountAdjustedBox;
	@FXML private MFXButton saveButton;
	@FXML private Button deleteButton;
	@FXML private Label paymentPopoverTitle,supplierTotalLabel;
	@FXML private MFXComboBox<String> taxRateField;
	@FXML private JFXNodesList addList;
	@FXML private Button xeroExportButton;
	private AccountPaymentService accountPaymentService;
	private AccountPaymentContactService accountPaymentContactService;
    private TableColumn<AccountPayment,String> descriptionCol;
    private ActionableFilterComboBox<AccountPaymentContactDataPoint> afx;
	
	 @FXML
	private void initialize() {
		 try {
			 accountPaymentService = new AccountPaymentService();
			 accountPaymentContactService = new AccountPaymentContactService();
		 } catch (IOException e) {
			 dialogPane.showError("Error initializing account payment service",e);
		 }
	}

	@Override
	public void fill() {
		accountTotalsTable.autosizeColumnsOnInitialization();
		MFXButton addContactButton = new MFXButton("Create New");
		addContactButton.setOnAction(_ -> {
			dialog = new Dialog<>(dialogPane, DialogPane.Type.BLANK);
			dialog.setPadding(false);
			dialog.setContent(createAddNewContactDialog());
			dialogPane.showDialog(dialog);
		});
		MFXButton manageContactsButton = new MFXButton("Manage Contacts");
		manageContactsButton.setOnAction(_ -> {
			dialog = new Dialog<>(dialogPane, DialogPane.Type.BLANK);
			dialog.setPadding(false);
			dialog.setContent(createManageContactsDialog());
			dialogPane.showDialog(dialog);
		});
		afx = new ActionableFilterComboBox<>(addContactButton,manageContactsButton);
		afx.setFloatMode(FloatMode.ABOVE);
		afx.setFloatingText("Contact name");
		afx.setFloatingTextGap(5);
		afx.setBorderGap(0);
		afx.setStyle("-mfx-gap: 5");
		afx.setMaxWidth(Double.MAX_VALUE);
		afx.setMinHeight(38.4);
		entryFieldBox.getChildren().add(1,afx);
		//Init Payments Table
        TableColumn<AccountPayment, String> contactCol = new TableColumn<>("CONTACT NAME");
        TableColumn<AccountPayment, String> invNumberCol = new TableColumn<>("INVOICE NUMBER");
        TableColumn<AccountPayment, LocalDate> invDateCol = new TableColumn<>("INVOICE DATE");
        TableColumn<AccountPayment, LocalDate> dueDateCol = new TableColumn<>("DUE DATE");
		descriptionCol = new TableColumn<>("DESCRIPTION");
        TableColumn<AccountPayment, Double> unitAmountCol = new TableColumn<>("UNIT AMOUNT");
        TableColumn<AccountPayment, String> accountAdjustedCol = new TableColumn<>("ACCOUNT\nADJUSTED?");
		contactCol.setCellValueFactory(new PropertyValueFactory<>("contactName"));
		invNumberCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumberString"));
		invDateCol.setCellValueFactory(new PropertyValueFactory<>("invDateString"));
		dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDateString"));
		descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
		unitAmountCol.setCellValueFactory(new PropertyValueFactory<>("unitAmountString"));
		accountAdjustedCol.setCellValueFactory(new PropertyValueFactory<>("accountAdjustedString"));
		List<TableColumn<AccountPayment, ?>> tableColumns = Arrays.asList(contactCol, invNumberCol, invDateCol, dueDateCol, descriptionCol, unitAmountCol, accountAdjustedCol);
		accountPaymentTable.getColumns().addAll(tableColumns);
		TableUtils.resizeTableColumns(accountPaymentTable,descriptionCol);
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Account Payments - Edit"))){
			Platform.runLater(this::addDoubleClickFunction);
		}else{
			addList.setVisible(false);
		}
		xeroExportButton.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Account Payments - Export")));
		//Init Totals Table
        MFXTableColumn<AccountPaymentContactDataPoint> contactNameCol = new MFXTableColumn<>("CONTACT", false, Comparator.comparing(AccountPaymentContactDataPoint::getContactName));
        MFXTableColumn<AccountPaymentContactDataPoint> totalCol = new MFXTableColumn<>("TOTAL", false, Comparator.comparing(AccountPaymentContactDataPoint::getTotalValue));
		contactNameCol.setRowCellFactory(_ -> new MFXTableRowCell<>(AccountPaymentContactDataPoint::getContactName));
		totalCol.setRowCellFactory(_ -> new MFXTableRowCell<>(AccountPaymentContactDataPoint::getTotalValueString));
		List<MFXTableColumn<AccountPaymentContactDataPoint>> contactColumns = Arrays.asList(contactNameCol, totalCol);
		accountTotalsTable.getTableColumns().addAll(contactColumns);
		ObservableList<String> taxRates = FXCollections.observableArrayList("BAS Excluded",
																			"GST Free Expenses",
																			"GST Free Income",
																			"GST on Expenses",
																			"GST on Imports",
																			"GST on Income");
		taxRateField.setItems(taxRates);
		taxRateField.setValue("Gst Free Income");
		Platform.runLater(() -> setDate(main.getCurrentDate()));
		ValidatorUtils.setupRegexValidation(afx,afxValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(invoiceNoField,invoiceNoValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(invoiceDateField,invoiceDateValidationLabel,ValidatorUtils.DATE_REGEX,ValidatorUtils.DATE_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(dueDateField,dueDateValidationLabel,ValidatorUtils.DATE_REGEX,ValidatorUtils.DATE_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(amountField,amountValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		invoiceDateField.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
		dueDateField.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
	}

	private void addDoubleClickFunction(){
		accountPaymentTable.setRowFactory(_ -> {
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
			dialogPane.showError("Error","An error occurred while trying to open the add new contact dialog",e);
		}
		AddNewContactDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setMain(this.main);
		return newContactDialog;
	}

	private Node createManageContactsDialog() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/ManageContactsDialog.fxml"));
		StackPane manageContactsDialog = null;
		try {
			manageContactsDialog = loader.load();
		} catch (IOException e) {
			dialogPane.showError("Error","An error occurred while trying to open the manage contacts dialog",e);
		}
		ManageContactsDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setMain(this.main);
		dialogController.fill();
		return manageContactsDialog;
	}

	public void fillContactList(){
        ObservableList<AccountPaymentContactDataPoint> contacts = null;
        try {
            contacts = FXCollections.observableArrayList(
                    accountPaymentContactService.getAllAccountPaymentContacts(main.getCurrentStore().getStoreID())
            );
        } catch (Exception e) {
            dialogPane.showError("Error", "An error occurred while trying to retrieve account payment contact information", e);
        }
        assert contacts != null;
        if (contacts.isEmpty()) {
			afx.getItems().add(new AccountPaymentContactDataPoint(0, "*Please add new suppliers below", 0));
		} else {
			afx.setItems(contacts);
		}
		afx.selectFirst();
	}

	public void fillTable(){
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
        ObservableList<AccountPayment> currentAccountPaymentDataPoints;
        try {
            currentAccountPaymentDataPoints = FXCollections.observableArrayList(
                    accountPaymentService.getAccountPaymentsForMonth(main.getCurrentStore().getStoreID(), yearMonthObject)
            );
        } catch (Exception e) {
            dialogPane.showError("Error", "An error occurred while trying to retrieve account payment information", e);
			return;
        }
        accountPaymentTable.setItems(currentAccountPaymentDataPoints);
		ObservableList<AccountPaymentContactDataPoint> currentContactTotals = FXCollections.observableArrayList();
		boolean contactFound;
		for(AccountPayment a:currentAccountPaymentDataPoints){
			contactFound = false;
			for(AccountPaymentContactDataPoint c: currentContactTotals){
				if(a.getContactName().equals(c.getContactName())){
					c.setTotalValue(c.getTotalValue()+a.getUnitAmount());
					contactFound = true;
				}
			}
			if(!contactFound){
                AccountPaymentContactDataPoint acdp = null;
                try {
                    acdp = accountPaymentContactService.getContactByName(a.getContactName(),main.getCurrentStore().getStoreID());
                } catch (Exception e) {
                    dialogPane.showError("Error", "An error occurred while trying to retrieve account payment contact information", e);
                }
                assert acdp != null;
                acdp.setTotalValue(a.getUnitAmount());
				currentContactTotals.add(acdp);
			}
		}
		accountTotalsTable.setItems(currentContactTotals);
		accountPaymentTable.setItems(currentAccountPaymentDataPoints);
		double supplierTotal=0;
		for(AccountPaymentContactDataPoint acdp:currentContactTotals)
			supplierTotal+=acdp.getTotalValue();
		supplierTotalLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(supplierTotal));
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Account Payments - Edit"))) {
			addDoubleClickFunction();
		}
		TableUtils.resizeTableColumns(accountPaymentTable,descriptionCol);
	}

	public void exportToXero() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Choose export save location");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		File file = fileChooser.showSaveDialog(main.getStg());
		if (file != null) {
			try (PrintWriter pw = new PrintWriter(file)) {
				pw.println("Contact,,,,,,,,,,Invoice number,Invoice date ,Due Date,,Description,Quantity,Unit amount,Account code,GST free,");
				YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
				ObservableList<AccountPayment> currentAccountPaymentDataPoints = FXCollections.observableArrayList(
						accountPaymentService.getAccountPaymentsForMonth(main.getCurrentStore().getStoreID(), yearMonthObject)
				);
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
				dialogPane.showInformation("Success", "Information exported successfully");
			} catch (FileNotFoundException e){
				dialogPane.showError("Error", "This file could not be accessed, please ensure its not open in another program", e);
			} catch (Exception e) {
				dialogPane.showError("Error", "An error occurred while trying to retrieve account payment information", e);
            }
        }
	}

	public void openPopover(){
		saveButton.setOnAction(_ -> addPayment());
		paymentPopoverTitle.setText("Add new account payment");
		deleteButton.setVisible(false);
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addPaymentPopover,0);
		afx.clear();
		afx.clearSelection();
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
		saveButton.setOnAction(_ -> editPayment(ap));
		paymentPopoverTitle.setText("Edit account payment");
		deleteButton.setVisible(true);
		deleteButton.setOnAction(_ -> deletePayment(ap));
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addPaymentPopover,0);
        try {
			AccountPaymentContactDataPoint contact = accountPaymentContactService.getContactByName(ap.getContactName(), main.getCurrentStore().getStoreID());
			if (contact != null) {
				afx.getSelectionModel().selectItem(contact);
			} else {
				dialogPane.showError("Error", "Could not find the contact for this payment", "");
			}
        } catch (Exception e) {
            dialogPane.showError("Error","An error occurred while trying to retrieve the contact information",e);
        }
        invoiceNoField.setText(ap.getInvoiceNumber());
		invoiceDateField.setValue(ap.getInvDate());
		dueDateField.setValue(ap.getDueDate());
		descriptionField.setText(ap.getDescription());
		amountField.setText(String.valueOf(ap.getUnitAmount()));
		accountAdjustedBox.setSelected(ap.isAccountAdjusted());
		Platform.runLater(() -> afx.requestFocus());
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		updateMonthSelectorField();
		fillTable();
		fillContactList();
	}

	public void addPayment(){
		if(!afx.isValid()){afx.requestFocus();}
		else if(!invoiceNoField.isValid()){invoiceNoField.requestFocus();}
		else if(!dueDateField.isValid()){dueDateField.requestFocus();}
		else if(!amountField.isValid()){amountField.requestFocus();}
		else if(invoiceDateField.getValue().isAfter(dueDateField.getValue())){
			invoiceDateValidationLabel.setVisible(true);
			invoiceDateValidationLabel.setText("Invoice date must be before due date");
			invoiceDateField.requestFocus();
		}else{
			invoiceDateValidationLabel.setVisible(false);
			AccountPayment newPayment = getNewPayment();
			try {
                accountPaymentService.addAccountPayment(newPayment);
            } catch (Exception e) {
				dialogPane.showError("Error","An error occurred while trying to add the payment",e);
            }
            closePopover();
			fillTable();
			dialogPane.showInformation("Success","Payment was successfully added");
		}

	}

	private AccountPayment getNewPayment() {
		AccountPaymentContactDataPoint contact = afx.getSelectedItem();
		String invoiceNo = invoiceNoField.getText();
		LocalDate invoiceDate = invoiceDateField.getValue();
		LocalDate dueDate = dueDateField.getValue();
		String description = descriptionField.getText();
		double unitAmount = Double.parseDouble(amountField.getText());
		String taxRate = taxRateField.getText();
        return new AccountPayment(contact.getContactName(),contact.getContactID(),main.getCurrentStore().getStoreID(),invoiceNo,invoiceDate,dueDate,description,1,unitAmount,accountAdjustedBox.isSelected(),contact.getAccountCode(),taxRate);
	}

	public void editPayment(AccountPayment accountPayment){
		if(!afx.isValid()){afx.requestFocus();}
		else if(!invoiceNoField.isValid()){invoiceNoField.requestFocus();}
		else if(!dueDateField.isValid()){dueDateField.requestFocus();}
		else if(!amountField.isValid()){amountField.requestFocus();}
		else if(invoiceDateField.getValue().isAfter(dueDateField.getValue())){
			invoiceDateValidationLabel.setVisible(true);
			invoiceDateValidationLabel.setText("Invoice date must be before due date");
			invoiceDateField.requestFocus();
		}else{
			invoiceDateValidationLabel.setVisible(false);
			AccountPaymentContactDataPoint contact = afx.getSelectedItem();
			String originalInvoiceNo = accountPayment.getInvoiceNumber();
			String invoiceNo = invoiceNoField.getText();
			LocalDate invoiceDate = invoiceDateField.getValue();
			LocalDate dueDate = dueDateField.getValue();
			String description = descriptionField.getText();
			double unitAmount = Double.parseDouble(amountField.getText());
			String taxRate = taxRateField.getText();
			accountPayment.setContactName(contact.getContactName());
			accountPayment.setContactID(contact.getContactID());
			accountPayment.setInvoiceNumber(invoiceNo);
			accountPayment.setInvDate(invoiceDate);
			accountPayment.setDueDate(dueDate);
			accountPayment.setDescription(description);
			accountPayment.setUnitAmount(unitAmount);
			accountPayment.setAccountAdjusted(accountAdjustedBox.isSelected());
			accountPayment.setAccountCode(contact.getAccountCode());
			accountPayment.setTaxRate(taxRate);
            try {
                accountPaymentService.updateAccountPayment(originalInvoiceNo,accountPayment);
            } catch (Exception e) {
                dialogPane.showError("Error","An error occurred while trying to edit the payment",e);
            }
            closePopover();
			fillTable();
			dialogPane.showInformation("Success","Payment was successfully edited");
		}
	}

	public void deletePayment(AccountPayment accountPayment){
		 dialogPane.showWarning("Confirm Delete",
				 "This action will permanently delete this Account payment from all systems,\n" +
				 "Are you sure you still want to delete this Account payment?").thenAccept(buttonType -> {
			 if (buttonType.equals(ButtonType.OK)) {
                 try {
                     accountPaymentService.deleteAccountPayment(accountPayment.getStoreID(), accountPayment.getInvoiceNumber());
                 } catch (Exception e) {
					 dialogPane.showError("Error","An error occurred while trying to delete the payment",e);
                 }
                 closePopover();
				 fillTable();
				 dialogPane.showInformation("Success","Payment was successfully deleted");
			 }
		 });
	}
}
