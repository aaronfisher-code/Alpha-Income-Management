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
import javafx.concurrent.Task;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

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
	@FXML private MFXProgressSpinner progressSpinner;
	private AccountPaymentService accountPaymentService;
	private AccountPaymentContactService accountPaymentContactService;
    private TableColumn<AccountPayment,String> descriptionCol;
    private ActionableFilterComboBox<AccountPaymentContactDataPoint> afx;

	@FXML
	private void initialize() {
		try {
			accountPaymentService = new AccountPaymentService();
			accountPaymentContactService = new AccountPaymentContactService();
			executor = Executors.newCachedThreadPool();
		} catch (IOException e) {
			dialogPane.showError("Error initializing account payment service", e);
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

	public void fillContactList() {
		Task<ObservableList<AccountPaymentContactDataPoint>> task = new Task<>() {
			@Override
			protected ObservableList<AccountPaymentContactDataPoint> call() {
				return FXCollections.observableArrayList(
						accountPaymentContactService.getAllAccountPaymentContacts(main.getCurrentStore().getStoreID())
				);
			}
		};
		task.setOnSucceeded(_ -> {
			progressSpinner.setVisible(false);
			ObservableList<AccountPaymentContactDataPoint> contacts = task.getValue();
			if (contacts.isEmpty()) {
				afx.getItems().add(new AccountPaymentContactDataPoint(0, "*Please add new suppliers below", 0));
			} else {
				afx.setItems(contacts);
			}
			afx.selectFirst();
		});
		task.setOnFailed(_ -> {
			progressSpinner.setVisible(false);
			dialogPane.showError("Error", "An error occurred while trying to retrieve account payment contact information", task.getException());
		});
		progressSpinner.setVisible(true);
		executor.submit(task);
	}

	public void fillTable() {
		progressSpinner.setVisible(true);
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		CompletableFuture<List<AccountPayment>> accountPaymentsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return accountPaymentService.getAccountPaymentsForMonth(main.getCurrentStore().getStoreID(), yearMonthObject);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
		CompletableFuture<List<AccountPaymentContactDataPoint>> contactsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return accountPaymentContactService.getAllAccountPaymentContacts(main.getCurrentStore().getStoreID());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
		CompletableFuture.allOf(accountPaymentsFuture, contactsFuture).thenRunAsync(() -> {
			try {
				List<AccountPayment> currentAccountPaymentDataPoints = accountPaymentsFuture.get();
				List<AccountPaymentContactDataPoint> contacts = contactsFuture.get();
				// Create a map to store the totals for each contact
				Map<String, AccountPaymentContactDataPoint> contactTotalsMap = new HashMap<>();
				double supplierTotal = 0;
				for (AccountPayment a : currentAccountPaymentDataPoints) {
					AccountPaymentContactDataPoint contact = contacts.stream()
							.filter(c -> c.getContactName().equals(a.getContactName()))
							.findFirst()
							.orElse(null);
					if (contact != null) {
						// Get or create the contact total
						AccountPaymentContactDataPoint contactTotal = contactTotalsMap.computeIfAbsent(
								contact.getContactName(),
								_ -> new AccountPaymentContactDataPoint(contact.getContactID(), contact.getContactName(), 0)
						);
						// Update the total
						contactTotal.setTotalValue(contactTotal.getTotalValue() + a.getUnitAmount());
						supplierTotal += a.getUnitAmount();
					}
				}
				// Convert the map values to a list
				ObservableList<AccountPaymentContactDataPoint> currentContactTotals =
						FXCollections.observableArrayList(contactTotalsMap.values());
				double finalSupplierTotal = supplierTotal;
				Platform.runLater(() -> {
					accountPaymentTable.setItems(FXCollections.observableArrayList(currentAccountPaymentDataPoints));
					accountTotalsTable.setItems(currentContactTotals);
					supplierTotalLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(finalSupplierTotal));

					if (main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Account Payments - Edit"))) {
						addDoubleClickFunction();
					}
					TableUtils.resizeTableColumns(accountPaymentTable, descriptionCol);
					progressSpinner.setVisible(false);
				});
			} catch (Exception e) {
				Platform.runLater(() -> {
					progressSpinner.setVisible(false);
					dialogPane.showError("Error", "An error occurred while trying to retrieve account payment information", e);
				});
			}
		}, executor);
	}

	public void exportToXero() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Choose export save location");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		File file = fileChooser.showSaveDialog(main.getStg());
		if (file != null) {
			progressSpinner.setVisible(true);
			Task<Void> exportTask = new Task<>() {
				@Override
				protected Void call() throws FileNotFoundException {
					try (PrintWriter pw = new PrintWriter(file)) {
						pw.println("*ContactName,,,,,,,,,,*InvoiceNumber,*InvoiceDate,*DueDate,,*Description,*Quantity,*UnitAmount,*AccountCode,*TaxType,");
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
					} catch (FileNotFoundException e){
						throw new FileNotFoundException();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					return null;
				}
			};
			exportTask.setOnSucceeded(_ -> {
				dialogPane.showInformation("Success", "Information exported successfully");
				progressSpinner.setVisible(false);
			});
			exportTask.setOnFailed(_ -> {
				Throwable e = exportTask.getException();
				if (e instanceof FileNotFoundException) {
					dialogPane.showError("Error", "This file could not be accessed, please ensure it's not open in another program", e);
				} else {
					dialogPane.showError("Error", "An error occurred while trying to export account payment information", e);
				}
				progressSpinner.setVisible(false);
			});
			executor.submit(exportTask);
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
		Task<AccountPaymentContactDataPoint> contactsTask = new Task<>() {
			@Override
			protected AccountPaymentContactDataPoint call() {
                return accountPaymentContactService.getContactByName(ap.getContactName(), main.getCurrentStore().getStoreID());
			}
		};
		contactsTask.setOnSucceeded(_ -> {
			if (contactsTask.getValue() != null) {
				afx.getSelectionModel().selectItem(contactsTask.getValue());
			} else {
				dialogPane.showError("Error", "Could not find the contact for this payment", "");
			}
			progressSpinner.setVisible(false);
			saveButton.setOnAction(_ -> editPayment(ap));
			paymentPopoverTitle.setText("Edit account payment");
			deleteButton.setVisible(true);
			deleteButton.setOnAction(_ -> deletePayment(ap));
			contentDarken.setVisible(true);
			AnimationUtils.slideIn(addPaymentPopover,0);
			invoiceNoField.setText(ap.getInvoiceNumber());
			invoiceDateField.setValue(ap.getInvDate());
			dueDateField.setValue(ap.getDueDate());
			descriptionField.setText(ap.getDescription());
			amountField.setText(String.valueOf(ap.getUnitAmount()));
			accountAdjustedBox.setSelected(ap.isAccountAdjusted());
			Platform.runLater(() -> afx.requestFocus());
		});
		contactsTask.setOnFailed(_ -> {
			dialogPane.showError("Error","An error occurred while trying to retrieve the contact information",contactsTask.getException());
			progressSpinner.setVisible(false);
		});
		progressSpinner.setVisible(true);
		executor.submit(contactsTask);
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
			progressSpinner.setVisible(true);
			Task<Void> addPaymentTask = new Task<>() {
				@Override
				protected Void call() {
					accountPaymentService.addAccountPayment(newPayment);
					return null;
				}
			};
			addPaymentTask.setOnSucceeded(_ -> {
				invoiceNoField.clear();
				invoiceDateField.clear();
				descriptionField.clear();
				amountField.clear();
				accountAdjustedBox.setSelected(false);
				fillTable();
				Platform.runLater(() -> invoiceNoField.requestFocus());
				progressSpinner.setVisible(false);
			});
			addPaymentTask.setOnFailed(_ -> {
				dialogPane.showError("Error", "An error occurred while trying to add the payment", addPaymentTask.getException());
				progressSpinner.setVisible(false);
			});
			executor.submit(addPaymentTask);
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
			progressSpinner.setVisible(true);
			Task<Void> editPaymentTask = new Task<>() {
				@Override
				protected Void call() {
					accountPaymentService.updateAccountPayment(originalInvoiceNo, accountPayment);
					return null;
				}
			};
			editPaymentTask.setOnSucceeded(_ -> {
				closePopover();
				fillTable();
				dialogPane.showInformation("Success", "Payment was successfully edited");
				progressSpinner.setVisible(false);
			});
			editPaymentTask.setOnFailed(_ -> {
				dialogPane.showError("Error", "An error occurred while trying to edit the payment", editPaymentTask.getException());
				progressSpinner.setVisible(false);
			});
			executor.submit(editPaymentTask);
		}
	}

	public void deletePayment(AccountPayment accountPayment) {
		dialogPane.showWarning("Confirm Delete",
				"This action will permanently delete this Account payment from all systems,\n" +
						"Are you sure you still want to delete this Account payment?").thenAccept(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				progressSpinner.setVisible(true);
				Task<Void> deletePaymentTask = new Task<>() {
					@Override
					protected Void call() {
						accountPaymentService.deleteAccountPayment(accountPayment.getStoreID(), accountPayment.getInvoiceNumber());
						return null;
					}
				};
				deletePaymentTask.setOnSucceeded(_ -> {
					closePopover();
					fillTable();
					dialogPane.showInformation("Success", "Payment was successfully deleted");
					progressSpinner.setVisible(false);
				});
				deletePaymentTask.setOnFailed(event -> {
					dialogPane.showError("Error", "An error occurred while trying to delete the payment", deletePaymentTask.getException());
					progressSpinner.setVisible(false);
				});
				executor.submit(deletePaymentTask);
			}
		});
	}
}
