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
import org.springframework.web.client.HttpClientErrorException;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
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
	private boolean paymentChangeInProgress;
	private long tableLoadVersion;

	@FXML
	private void initialize() {
		progressSpinner.managedProperty().bind(progressSpinner.visibleProperty());
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
			dialog = createDialog(DialogPane.Type.BLANK);
			dialog.setUsingPadding(false);
			dialog.setContent(createAddNewContactDialog());
			dialogPane.showDialog(dialog);
		});
		MFXButton manageContactsButton = new MFXButton("Manage Contacts");
		manageContactsButton.setOnAction(_ -> {
			dialog = createDialog(DialogPane.Type.BLANK);
			dialog.setUsingPadding(false);
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
		reloadTable().exceptionally(failure -> {
			if (!(unwrapAsyncFailure(failure) instanceof CancellationException)) {
				dialogPane.showError("Error", "An error occurred while trying to retrieve account payment information", unwrapAsyncFailure(failure));
			}
			return null;
		});
	}

	private CompletableFuture<List<AccountPayment>> reloadTable() {
		progressSpinner.setVisible(true);
		int storeId = main.getCurrentStore().getStoreID();
		YearMonth month = YearMonth.from(main.getCurrentDate());
		long version = ++tableLoadVersion;
		var payments = CompletableFuture.supplyAsync(() -> accountPaymentService.getAccountPaymentsForMonth(storeId, month), executor);
		var contacts = CompletableFuture.supplyAsync(() -> accountPaymentContactService.getAllAccountPaymentContacts(storeId), executor);
		var displayed = new CompletableFuture<List<AccountPayment>>();
		payments.thenCombine(contacts, PaymentTableData::new).whenComplete((data, failure) -> Platform.runLater(() -> {
			try {
				if (version != tableLoadVersion || storeId != main.getCurrentStore().getStoreID()
						|| !month.equals(YearMonth.from(main.getCurrentDate()))) {
					displayed.cancel(false);
					return;
				}
				if (failure != null) throw new java.util.concurrent.CompletionException(failure);
				Map<Integer, AccountPaymentContactDataPoint> contactTotals = new LinkedHashMap<>();
				for (AccountPayment payment : data.payments()) {
					AccountPaymentContactDataPoint contact = data.contacts().stream()
							.filter(c -> c.getContactID() == payment.getContactID()).findFirst().orElse(null);
					if (contact != null) {
						AccountPaymentContactDataPoint total = contactTotals.computeIfAbsent(contact.getContactID(),
								_ -> new AccountPaymentContactDataPoint(contact.getContactID(), contact.getContactName(), storeId));
						total.setTotalValue(total.getTotalValue() + payment.getUnitAmount());
					}
				}
				accountPaymentTable.setItems(FXCollections.observableArrayList(data.payments()));
				accountTotalsTable.setItems(FXCollections.observableArrayList(contactTotals.values()));
				double total = data.payments().stream().mapToDouble(AccountPayment::getUnitAmount).sum();
				supplierTotalLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(total));
				TableUtils.resizeTableColumns(accountPaymentTable, descriptionCol);
				displayed.complete(data.payments());
			} catch (Exception error) {
				displayed.completeExceptionally(unwrapAsyncFailure(error));
			} finally {
				if (version == tableLoadVersion && !paymentChangeInProgress) progressSpinner.setVisible(false);
			}
		}));
		return displayed;
	}

	private record PaymentTableData(List<AccountPayment> payments, List<AccountPaymentContactDataPoint> contacts) {}

	private void setPaymentChangeInProgress(boolean busy) {
		paymentChangeInProgress = busy;
		addPaymentPopover.setDisable(busy);
		addList.setDisable(busy);
		accountPaymentTable.setDisable(busy);
		monthSelector.setDisable(busy);
		progressSpinner.setVisible(busy);
	}

	private void finishPaymentChange(String successMessage, AccountPayment deletedPayment) {
		closePopover();
		reloadTable().whenComplete((payments, failure) -> {
			setPaymentChangeInProgress(false);
			if (failure != null) {
				dialogPane.showError("Refresh failed", "The request completed, but the payment list could not be refreshed. Refresh it before making more changes.", unwrapAsyncFailure(failure));
			} else if (deletedPayment != null && payments.stream().anyMatch(p ->
					p.getStoreID() == deletedPayment.getStoreID() && p.getInvoiceNumber().equals(deletedPayment.getInvoiceNumber()))) {
				dialogPane.showError("Deletion not confirmed", "The payment is still present after refreshing. It has not been confirmed as deleted.", "");
			} else {
				dialogPane.showInformation("Success", successMessage);
			}
		});
	}

	private void paymentChangeFailed(String action, Throwable failure) {
		setPaymentChangeInProgress(false);
		String message = "An error occurred while trying to " + action + " the payment. Refresh the list before retrying if the request timed out.";
		if (failure instanceof HttpClientErrorException.Conflict) {
			message = "That invoice number is already used for this store, possibly in another month. Use a different invoice number. The payment was not saved.";
		} else if (failure instanceof HttpClientErrorException.NotFound) {
			message = "This payment no longer exists. Refresh the payment list before trying again.";
		}
		dialogPane.showError("Unable to " + action + " payment", message, failure);
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
						pw.println("*ContactName,EmailAddress,POAddressLine1,POAddressLine2,POAddressLine3,POAddressLine4,POCity,PORegion,POPostalCode,POCountry,*InvoiceNumber,*InvoiceDate,*DueDate,InventoryItemCode,*Description,*Quantity,*UnitAmount,Discount,*AccountCode,*TaxType,TrackingName1,TrackingOption1,TrackingName2,TrackingOption2,Currency,BrandingTheme");
						YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
						ObservableList<AccountPayment> currentAccountPaymentDataPoints = FXCollections.observableArrayList(
								accountPaymentService.getAccountPaymentsForMonth(main.getCurrentStore().getStoreID(), yearMonthObject)
						);
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
						for(AccountPayment a: currentAccountPaymentDataPoints){
							pw.print(a.getContactName()+",,,,,,,,,,");
							pw.print(a.getInvoiceNumber()+",");
							pw.print(a.getInvDate().format(formatter)+",");
							pw.print(a.getDueDate().format(formatter)+",,");
							pw.print(a.getDescription()+",1,");
							pw.print("$"+a.getUnitAmount()+",,");
							pw.print(a.getAccountCode()+",");
							pw.println(a.getTaxRate()+",,,,,,");
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
		if (paymentChangeInProgress) return;
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
		if (paymentChangeInProgress) return;
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
		if (paymentChangeInProgress) return;
		main.setCurrentDate(date);
		updateMonthSelectorField();
		fillTable();
		fillContactList();
	}

	public void addPayment(){
		if (paymentChangeInProgress) return;
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
			setPaymentChangeInProgress(true);
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
				reloadTable().whenComplete((_, failure) -> {
					setPaymentChangeInProgress(false);
					if (failure != null) dialogPane.showError("Refresh failed", "The payment was added, but the list could not be refreshed. Refresh it before adding the payment again.", unwrapAsyncFailure(failure));
					else invoiceNoField.requestFocus();
				});
			});
			addPaymentTask.setOnFailed(_ -> {
				paymentChangeFailed("add", addPaymentTask.getException());
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
		if (paymentChangeInProgress) return;
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
			AccountPayment editedPayment = new AccountPayment(
					contact.getContactName(), contact.getContactID(), accountPayment.getStoreID(),
					invoiceNo, invoiceDate, dueDate, description, accountPayment.getQuantity(),
					unitAmount, accountAdjustedBox.isSelected(), contact.getAccountCode(), taxRate);
			setPaymentChangeInProgress(true);
			Task<Void> editPaymentTask = new Task<>() {
				@Override
				protected Void call() {
					accountPaymentService.updateAccountPayment(originalInvoiceNo, editedPayment);
					return null;
				}
			};
			editPaymentTask.setOnSucceeded(_ -> {
				finishPaymentChange("Payment was successfully edited", null);
			});
			editPaymentTask.setOnFailed(_ -> {
				paymentChangeFailed("edit", editPaymentTask.getException());
			});
			executor.submit(editPaymentTask);
		}
	}

	public void deletePayment(AccountPayment accountPayment) {
		if (paymentChangeInProgress) return;
		dialogPane.showWarning("Confirm Delete",
				"Permanently delete invoice " + accountPayment.getInvoiceNumber() + " for " +
						accountPayment.getContactName() + " (" + accountPayment.getUnitAmountString() + ")?").onClose(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				if (paymentChangeInProgress) return;
				setPaymentChangeInProgress(true);
				Task<Void> deletePaymentTask = new Task<>() {
					@Override
					protected Void call() {
						accountPaymentService.deleteAccountPayment(accountPayment);
						return null;
					}
				};
				deletePaymentTask.setOnSucceeded(_ -> {
					finishPaymentChange("Payment was successfully deleted", accountPayment);
				});
				deletePaymentTask.setOnFailed(event -> {
					paymentChangeFailed("delete", deletePaymentTask.getException());
				});
				executor.submit(deletePaymentTask);
			}
		});
	}
}
