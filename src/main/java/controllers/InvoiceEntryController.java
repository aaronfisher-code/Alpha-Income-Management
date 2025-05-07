package controllers;

import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.FilterView;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXNodesList;
import components.ActionableFilterComboBox;
import components.CustomDateStringConverter;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import models.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import services.CreditService;
import services.InvoiceService;
import services.InvoiceSupplierService;
import utils.*;
import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dlsc.gemsfx.DialogPane.Type.BLANK;

public class InvoiceEntryController extends DateSelectController{

	@FXML private VBox controlBox,addInvoicePopover,addCreditPopover;
	@FXML private BorderPane invoicesButton,creditsButton;
	@FXML private Region contentDarken;
	@FXML private MFXTextField invoiceNoField,descriptionField,amountField,notesField;
	@FXML private MFXDatePicker invoiceDateField, dueDateField;
	@FXML private MFXButton saveButton;
	@FXML private Button deleteButton;
	@FXML private Label paymentPopoverTitle,expectedUnitAmountLabel,varianceLabel;
	@FXML private Label afxValidationLabel,invoiceNoValidationLabel,invoiceDateValidationLabel,dueDateValidationLabel,amountValidationLabel;
	@FXML private Label creditAFXValidationLabel,creditNoValidationLabel,refInvNoValidationLabel,creditDateValidationLabel,creditAmountValidationLabel;
	@FXML private Label creditPopoverTitle;
	@FXML private MFXTextField creditNoField,refInvNoField,creditAmountField,creditNotesField;
	@FXML private MFXDatePicker creditDateField;
	@FXML private MFXButton creditSaveButton;
	@FXML private Button creditDeleteButton;
	@FXML private JFXButton plusButton;
	@FXML private JFXNodesList addList;
	@FXML private Button importDataButton,exportDataButton;
	@FXML private MFXProgressSpinner progressSpinner;
	private Label invoiceTotalLabel;
	private TableView<Invoice> invoicesTable = new TableView<>();
	private TableView<Credit> creditsTable = new TableView<>();
	private TableColumn<Invoice,String> supplierNameCol;
	private TableColumn<Invoice,String> invoiceNoCol;
	private TableColumn<Invoice,LocalDate> invoiceDateCol;
	private TableColumn<Invoice,LocalDate> dueDateCol;
	private TableColumn<Invoice,Double> unitAmountCol;
	private TableColumn<Invoice,Double> importedInvoiceAmountCol;
	private TableColumn<Invoice,String> varianceCol;
	private TableColumn<Invoice,Double> creditsCol;
	private TableColumn<Invoice,Double> totalAfterCreditCol;
	private TableColumn<Invoice,String> notesCol;
	private TableColumn<Credit,String> creditNoCol;
	private TableColumn<Credit,String> creditSupplierNameCol;
	private TableColumn<Credit,String> referenceInvCol;
	private TableColumn<Credit,LocalDate> creditDateCol;
	private TableColumn<Credit,Double> creditAmountCol;
	private TableColumn<Credit,String> creditNotesCol;
	private FilterView<Invoice> invoiceFilterView = new FilterView<>();
	private FilterView<Credit> creditFilterView = new FilterView<>();
	private ActionableFilterComboBox<InvoiceSupplier> invoiceAFX,creditAFX;
	private InvoiceService invoiceService;
	private InvoiceSupplierService invoiceSupplierService;
	private CreditService creditService;
	private AtomicInteger taskCounter = new AtomicInteger(0);

    @FXML
	private void initialize() {
		try{
			invoiceService = new InvoiceService();
			invoiceSupplierService = new InvoiceSupplierService();
			creditService = new CreditService();
			executor = Executors.newCachedThreadPool();
		}catch(IOException ex){
			dialogPane.showError("Error","An error occurred while initialising the invoice service",ex);
		}
	}

	@Override
	public void fill(){
		invoiceAFX = createAFX();
		creditAFX = createAFX();
		addInvoicePopover.getChildren().add(1, invoiceAFX);
		addCreditPopover.getChildren().add(1, creditAFX);
		//setup invoice validation
		ValidatorUtils.setupRegexValidation(invoiceAFX,afxValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(invoiceNoField,invoiceNoValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(invoiceDateField,invoiceDateValidationLabel,ValidatorUtils.DATE_REGEX,ValidatorUtils.DATE_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(dueDateField,dueDateValidationLabel,ValidatorUtils.DATE_REGEX,ValidatorUtils.DATE_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(amountField,amountValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		//setup credit validation
		ValidatorUtils.setupRegexValidation(creditAFX,creditAFXValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,creditSaveButton);
		ValidatorUtils.setupRegexValidation(creditNoField,creditNoValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,creditSaveButton);
		ValidatorUtils.setupRegexValidation(refInvNoField,refInvNoValidationLabel,ValidatorUtils.BLANK_REGEX,ValidatorUtils.BLANK_ERROR,null,creditSaveButton);
		ValidatorUtils.setupRegexValidation(creditDateField,creditDateValidationLabel,ValidatorUtils.DATE_REGEX,ValidatorUtils.DATE_ERROR,null,creditSaveButton);
		ValidatorUtils.setupRegexValidation(creditAmountField,creditAmountValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",creditSaveButton);
		//setup date parsers
		invoiceDateField.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
		dueDateField.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
		creditDateField.setConverterSupplier(() -> new CustomDateStringConverter("dd/MM/yyyy"));
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Invoicing - Edit"))) {
			addList.setVisible(true);
			importDataButton.setDisable(false);
		}else{
			addList.setVisible(false);
			importDataButton.setDisable(true);
		}
		exportDataButton.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Invoicing - Export")));
		//Live update expected unit amount if invoice is recognised
		invoiceNoField.delegateFocusedProperty().addListener((_, _, _) -> {
			if (invoiceNoField.isValid() && invoiceAFX.isValid()) {
				progressSpinner.setVisible(true);
				Task<Boolean> duplicateTask = new Task<>() {
					@Override
					protected Boolean call() {
						try{
							return invoiceService.checkDuplicateInvoice(invoiceNoField.getText(), main.getCurrentStore().getStoreID(), invoiceAFX.getValue().getContactID());
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
				duplicateTask.setOnSucceeded(_ -> {
					boolean isDuplicate = duplicateTask.getValue();
					if (isDuplicate) {
						invoiceNoValidationLabel.setStyle("-fx-text-fill: red;");
						invoiceNoValidationLabel.setText("Invoice Already Exists");
						invoiceNoValidationLabel.setVisible(true);
						expectedUnitAmountLabel.setText("N/A");
						varianceLabel.setText("N/A");
					} else {
						Task<Invoice> invoiceTask = new Task<>() {
							@Override
							protected Invoice call() {
								try{
									return invoiceService.getInvoice(invoiceNoField.getText(), main.getCurrentStore().getStoreID());
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							}
						};
						invoiceTask.setOnSucceeded(_ -> {
							Invoice invoice = invoiceTask.getValue();
							if (invoice != null) {
								expectedUnitAmountLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(invoice.getImportedInvoiceAmount()));
								invoiceNoValidationLabel.setStyle("-fx-text-fill: red;");
								invoiceNoValidationLabel.setText("");
								invoiceNoValidationLabel.setVisible(false);
								if (amountField.isValid()) {
									varianceLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(
											Double.parseDouble(expectedUnitAmountLabel.getText().replace("$", "").replace(",","")) - Double.parseDouble(amountField.getText())));
								}
							} else {
								expectedUnitAmountLabel.setText("N/A");
								invoiceNoValidationLabel.setStyle("-fx-text-fill: orange;");
								invoiceNoValidationLabel.setText("Warning: Invoice not recognised");
								invoiceNoValidationLabel.setVisible(true);
								varianceLabel.setText("N/A");
							}
							progressSpinner.setVisible(false);
						});
						invoiceTask.setOnFailed(_ -> {
							invoiceTask.getException().printStackTrace();
							dialogPane.showError("Error", "An error occurred while loading invoice information", invoiceTask.getException());
							progressSpinner.setVisible(false);
						});
						executor.submit(invoiceTask);
					}
					progressSpinner.setVisible(false);
				});
				duplicateTask.setOnFailed(_ -> {
					duplicateTask.getException().printStackTrace();
					dialogPane.showError("Error", "An error occurred while loading invoice information", duplicateTask.getException());
					progressSpinner.setVisible(false);
				});
				executor.submit(duplicateTask);
			}
		});
		invoicesView();
	}

	public void invoicesView() {
		GUIUtils.formatTabSelect(invoicesButton);
		GUIUtils.formatTabDeselect(creditsButton);
		controlBox.getChildren().clear();
		invoiceFilterView = new FilterView<>();
		invoiceFilterView.setTitle("Current Invoices");
		invoiceFilterView.setTextFilterProvider(text -> invoice -> invoice.getInvoiceNo().toLowerCase().contains(text) || invoice.getSupplierName().toLowerCase().contains(text));
        ObservableList<Invoice> allInvoices = invoiceFilterView.getFilteredItems();
		supplierNameCol = new TableColumn<>("     SUPPLIER NAME     ");
		invoiceNoCol = new TableColumn<>("INVOICE NUMBER");
		invoiceDateCol = new TableColumn<>("INVOICE DATE");
		dueDateCol = new TableColumn<>("DUE DATE");
		unitAmountCol = new TableColumn<>("UNIT AMOUNT");
		importedInvoiceAmountCol = new TableColumn<>("IMPORTED INVOICE\nAMOUNT");
		varianceCol = new TableColumn<>("VARIANCE");
		creditsCol = new TableColumn<>("CREDITS");
		totalAfterCreditCol = new TableColumn<>("TOTAL AFTER\nCREDIT");
		notesCol = new TableColumn<>("NOTES");
		supplierNameCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
		invoiceNoCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNo"));
		invoiceDateCol.setCellValueFactory(new PropertyValueFactory<>("invoiceDateString"));
		dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDateString"));
		unitAmountCol.setCellValueFactory(new PropertyValueFactory<>("unitAmountString"));
		importedInvoiceAmountCol.setCellValueFactory(new PropertyValueFactory<>("importedInvoiceAmountString"));
		varianceCol.setCellValueFactory(new PropertyValueFactory<>("varianceString"));
		varianceCol.setCellFactory(tc -> new TableCell<Invoice, String>() {
			@Override
			protected void updateItem(String value, boolean empty) {
				super.updateItem(value, empty);

				if (empty || value == null) {
					setText(null);
					setStyle(""); // reset style
				} else {
					// Display the text in the cell
					setText(value);

					// Remove any currency symbols/commas so we can parse as double
					String numericPart = value.replaceAll("[^0-9\\-\\.]", "");
					double varianceAmount = 0.0;
					try {
						varianceAmount = Double.parseDouble(numericPart);
					} catch (NumberFormatException e) {
						// If parsing fails, revert to default styling and just show the text
						setStyle("");
						return;
					}

					// If absolute variance > 20 cents, highlight in red
					if (Math.abs(varianceAmount) > 0.20) {
						setStyle("-fx-background-color: red; -fx-text-fill: white;");
					} else {
						// Otherwise, reset or use your default style
						setStyle("");
					}
				}
			}
		});
		creditsCol.setCellValueFactory(new PropertyValueFactory<>("creditsString"));
		totalAfterCreditCol.setCellValueFactory(new PropertyValueFactory<>("totalAfterCreditsString"));
		notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
		invoiceNoCol.setComparator((o1, o2) -> {
			try {
				int i1 = Integer.parseInt(o1);
				int i2 = Integer.parseInt(o2);
				return Integer.compare(i1, i2);
			} catch(NumberFormatException e) {
				return o1.compareTo(o2);
			}
		});
		invoicesTable.getColumns().clear();
		invoicesTable.getColumns().addAll(
				supplierNameCol,
				invoiceNoCol,
				invoiceDateCol,
				dueDateCol,
				unitAmountCol,
				importedInvoiceAmountCol,
				varianceCol,
				creditsCol,
				totalAfterCreditCol,
				notesCol
		);
		invoiceFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left
		controlBox.getChildren().addAll(invoiceFilterView,invoicesTable);
		invoicesTable.setItems(allInvoices);
		Platform.runLater(this::addInvoiceDoubleClickFunction);
		setDate(main.getCurrentDate());
		plusButton.setOnAction(_ -> openInvoicePopover());
		contentDarken.setOnMouseClicked(_ -> closeInvoicePopover());
		amountField.delegateFocusedProperty().addListener((_, _, _) -> {
			if (amountField.isValid()) {
				if(expectedUnitAmountLabel.getText().equals("N/A")) {
					varianceLabel.setText("N/A");
					varianceLabel.setStyle("");
				} else {
					try {
						double expected = Double.parseDouble(expectedUnitAmountLabel.getText().replace("$", "").replace(",", ""));
						double amount = Double.parseDouble(amountField.getText());
						double variance = expected - amount;
						String formattedVariance = NumberFormat.getCurrencyInstance(Locale.US).format(variance);
						varianceLabel.setText(formattedVariance);

						// If the absolute variance is greater than 20 cents, highlight the label in red
						if (Math.abs(variance) > 0.20) {
							varianceLabel.setStyle("-fx-background-color: red; -fx-text-fill: white;");
						} else {
							varianceLabel.setStyle("");
						}
					} catch (NumberFormatException e) {
						varianceLabel.setText("N/A");
						varianceLabel.setStyle("");
					}
				}
			}
		});
		DoubleBinding totalBinding = Bindings.createDoubleBinding(
				() -> invoiceFilterView.getFilteredItems()
						.stream()
						.mapToDouble(Invoice::getUnitAmount)
						.sum(),
				invoiceFilterView.getFilteredItems()
		);


		Text prefix = new Text("Invoice Total: ");
		Text amountText = new Text();
		NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
		StringBinding formattedAmount = Bindings.createStringBinding(
				() -> currencyFormatter.format(totalBinding.get()),
				totalBinding
		);
		amountText.textProperty().bind(formattedAmount);
		prefix.getStyleClass().add("totalsText");
		amountText.getStyleClass().add("totalsText");

		TextFlow totalFlow = new TextFlow(prefix, amountText);
		totalFlow.setPadding(new Insets(10));            // 10px on all sides
		totalFlow.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
		totalFlow.setMaxWidth(Double.MAX_VALUE);          // let it stretch in the VBox

		controlBox.setAlignment(Pos.TOP_LEFT);
		controlBox.getChildren().clear();
		invoiceFilterView.setPadding(new Insets(20,20,10,20));
		controlBox.getChildren().addAll(
				invoiceFilterView,
				invoicesTable,
				totalFlow
		);
	}

	public void creditsView(){
		GUIUtils.formatTabSelect(creditsButton);
		GUIUtils.formatTabDeselect(invoicesButton);
		controlBox.getChildren().clear();
		creditFilterView = new FilterView<>();
		creditFilterView.setTitle("Current Credits");
		creditFilterView.setTextFilterProvider(text -> credit -> credit.getSupplierName().toLowerCase().contains(text) || credit.getCreditNo().toLowerCase().contains(text) || credit.getReferenceInvoiceNo().toLowerCase().contains(text));
        ObservableList<Credit> allCredits = creditFilterView.getFilteredItems();
		creditSupplierNameCol = new TableColumn<>("     SUPPLIER NAME     ");
		creditNoCol = new TableColumn<>("CREDIT NUMBER");
		referenceInvCol = new TableColumn<>("REFERENCE INVOICE NUMBER");
		creditDateCol = new TableColumn<>("CREDIT DATE");
		creditAmountCol = new TableColumn<>("CREDIT AMOUNT");
		creditNotesCol = new TableColumn<>("NOTES");
		creditSupplierNameCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
		creditNoCol.setCellValueFactory(new PropertyValueFactory<>("creditNo"));
		referenceInvCol.setCellValueFactory(new PropertyValueFactory<>("referenceInvoiceNo"));
		creditDateCol.setCellValueFactory(new PropertyValueFactory<>("creditDateString"));
		creditAmountCol.setCellValueFactory(new PropertyValueFactory<>("creditAmountString"));
		creditNotesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
		creditsTable.getColumns().clear();
		creditsTable.getColumns().addAll(
				creditSupplierNameCol,
				creditNoCol,
				referenceInvCol,
				creditDateCol,
				creditAmountCol,
				creditNotesCol
		);
		creditFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left
		controlBox.getChildren().addAll(creditFilterView,creditsTable);
		creditsTable.setItems(allCredits);
		setDate(main.getCurrentDate());
		Platform.runLater(this::addCreditDoubleClickFunction);
		addCreditDoubleClickFunction();
		plusButton.setOnAction(_ -> openCreditPopover());
		contentDarken.setOnMouseClicked(_ -> closeCreditPopover());
	}

	public ActionableFilterComboBox<InvoiceSupplier> createAFX(){
		MFXButton addSupplierButton = new MFXButton("Create New");
		addSupplierButton.setOnAction(_ -> {
			dialog = new DialogPane.Dialog<>(dialogPane, BLANK);
			dialog.setPadding(false);
			dialog.setContent(createAddNewSupplierDialog());
			dialogPane.showDialog(dialog);
		});
		MFXButton manageSuppliersButton = new MFXButton("Manage Contacts");
		manageSuppliersButton.setOnAction(_ -> {
			dialog = new DialogPane.Dialog<>(dialogPane, BLANK);
			dialog.setPadding(false);
			dialog.setContent(createManageSuppliersDialog());
			dialogPane.showDialog(dialog);
		});
		ActionableFilterComboBox<InvoiceSupplier> newAFX = new ActionableFilterComboBox<>(addSupplierButton, manageSuppliersButton);
		newAFX.setFloatMode(FloatMode.ABOVE);
		newAFX.setFloatingText("Contact name");
		newAFX.setFloatingTextGap(5);
		newAFX.setBorderGap(0);
		newAFX.setStyle("-mfx-gap: 5");
		newAFX.setMaxWidth(Double.MAX_VALUE);
		newAFX.setMinHeight(38.4);
		return newAFX;
	}

	private void addInvoiceDoubleClickFunction(){
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Invoicing - Edit"))) {
			invoicesTable.setRowFactory(_ -> {
				TableRow<Invoice> row = new TableRow<>();
				row.setOnMouseClicked(event -> {
					if (event.getClickCount() == 2 && (!row.isEmpty())) {
						Invoice rowData = row.getItem();
						openInvoicePopover(rowData);
					}
				});
				return row;
			});
		}
	}

	private void addCreditDoubleClickFunction(){
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Invoicing - Edit"))) {
			creditsTable.setRowFactory(_ -> {
				TableRow<Credit> row = new TableRow<>();
				row.setOnMouseClicked(event -> {
					if (event.getClickCount() == 2 && (!row.isEmpty())) {
						Credit rowData = row.getItem();
						openCreditPopover(rowData);
					}
				});
				return row;
			});
		}
	}

	private Node createAddNewSupplierDialog() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/AddNewSupplierDialog.fxml"));
		StackPane newContactDialog = null;
		try {
			newContactDialog = loader.load();
		} catch (IOException e) {
			dialogPane.showError("Error","An error occurred while loading the new supplier dialog",e);
		}
		AddNewSupplierDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setMain(this.main);
		return newContactDialog;
	}

	private Node createManageSuppliersDialog() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/ManageSuppliersDialog.fxml"));
		StackPane manageSuppliersDialog = null;
		try {
			manageSuppliersDialog = loader.load();
		} catch (IOException e) {
			dialogPane.showError("Error","An error occurred while loading the manage suppliers dialog",e);
		}
		ManageSuppliersDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setMain(this.main);
		dialogController.fill();
		return manageSuppliersDialog;
	}

	public void fillContactList() {
		Task<ObservableList<InvoiceSupplier>> task = new Task<>() {
			@Override
			protected ObservableList<InvoiceSupplier> call() {
				return FXCollections.observableArrayList(invoiceSupplierService.getAllInvoiceSuppliers(main.getCurrentStore().getStoreID()));
			}
		};
		task.setOnSucceeded(_ -> {
			ObservableList<InvoiceSupplier> contacts = task.getValue();
			if (contacts == null || contacts.isEmpty()) {
				invoiceAFX.getItems().add(new InvoiceSupplier(0, "*Please add new suppliers below", 0));
				creditAFX.getItems().add(new InvoiceSupplier(0, "*Please add new suppliers below", 0));
			} else {
				invoiceAFX.setItems(contacts);
				creditAFX.setItems(contacts);
			}
			invoiceAFX.clearSelection();
			creditAFX.clearSelection();
		});
		task.setOnFailed(_ -> {
			dialogPane.showError("Error", "An error occurred while loading invoice suppliers", task.getException());
		});
		executor.submit(task);
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		updateMonthSelectorField();
		progressSpinner.setVisible(true);
		CompletableFuture<ObservableList<Invoice>> invoiceFuture = fetchInvoiceData();
		CompletableFuture<ObservableList<Credit>> creditFuture = fetchCreditData();
		CompletableFuture<ObservableList<InvoiceSupplier>> contactFuture = fetchContactData();
		CompletableFuture.allOf(invoiceFuture, creditFuture, contactFuture)
				.thenRunAsync(() -> {
					Platform.runLater(() -> {
						try {
							updateInvoiceTable(invoiceFuture.get());
							updateCreditTable(creditFuture.get());
							updateContactList(contactFuture.get());
						} catch (InterruptedException | ExecutionException e) {
							dialogPane.showError("Error", "An error occurred while updating data", e);
						} finally {
							progressSpinner.setVisible(false);
						}
					});
				}, executor)
				.exceptionally(ex -> {
					Platform.runLater(() -> {
						progressSpinner.setVisible(false);
						dialogPane.showError("Error", "An error occurred while loading data", ex);
					});
					return null;
				});
	}

	public void openInvoicePopover(){
		saveButton.setOnAction(_ -> addInvoice());
		paymentPopoverTitle.setText("Add new Invoice");
		deleteButton.setVisible(false);
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addInvoicePopover,0);
		invoiceAFX.clear();
		invoiceAFX.clearSelection();
		invoiceNoField.clear();
		invoiceDateField.setValue(LocalDate.MIN);
		invoiceDateField.clear();
		dueDateField.setValue(LocalDate.MIN);
		dueDateField.clear();
		amountField.clear();
		notesField.clear();
		expectedUnitAmountLabel.setText("$0.00");
		varianceLabel.setText("$0.00");
		Platform.runLater(() -> invoiceAFX.requestFocus());
	}

	public void openInvoicePopover(Invoice invoice){
		Task<InvoiceSupplier> task = new Task<>() {
			@Override
			protected InvoiceSupplier call() {
				return invoiceSupplierService.getInvoiceSupplierByName(invoice.getSupplierName(), main.getCurrentStore().getStoreID());
			}
		};
		task.setOnSucceeded(_ -> {
			InvoiceSupplier supplier = task.getValue();
			invoiceAFX.setValue(supplier);
			progressSpinner.setVisible(false);
			saveButton.setOnAction(_ -> editInvoice(invoice));
			paymentPopoverTitle.setText("Edit Invoice");
			deleteButton.setVisible(true);
			deleteButton.setOnAction(_ -> deleteInvoice(invoice));
			contentDarken.setVisible(true);
			AnimationUtils.slideIn(addInvoicePopover,0);
			invoiceNoField.setText(invoice.getInvoiceNo());
			invoiceDateField.setValue(invoice.getInvoiceDate());
			dueDateField.setValue(invoice.getDueDate());
			amountField.setText(String.valueOf(invoice.getUnitAmount()));
			notesField.setText(invoice.getNotes());
			expectedUnitAmountLabel.setText("$"+String.format("%.2f",invoice.getImportedInvoiceAmount()));
			varianceLabel.setText("$"+String.format("%.2f",invoice.getVariance()));
			Platform.runLater(() -> invoiceAFX.requestFocus());
		});
		task.setOnFailed(_ -> {
			dialogPane.showError("Error", "An error occurred while locating the invoice supplier", task.getException());
			progressSpinner.setVisible(false);
		});
		executor.submit(task);
		progressSpinner.setVisible(true);
	}

	public void openCreditPopover(){
		creditSaveButton.setOnAction(_ -> addCredit());
		creditPopoverTitle.setText("Add new Credit");
		creditDeleteButton.setVisible(false);
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addCreditPopover,0);
		creditAFX.clear();
		creditAFX.clearSelection();
		creditNoField.clear();
		refInvNoField.clear();
		creditDateField.clear();
		creditAmountField.clear();
		creditNotesField.clear();
		Platform.runLater(() -> creditAFX.requestFocus());
	}

	public void openCreditPopover(Credit credit){
		Task<InvoiceSupplier> task = new Task<>() {
			@Override
			protected InvoiceSupplier call() {
				return invoiceSupplierService.getInvoiceSupplierByName(credit.getSupplierName(), main.getCurrentStore().getStoreID());
			}
		};
		task.setOnSucceeded(_ -> {
			InvoiceSupplier supplier = task.getValue();
			creditAFX.setValue(supplier);
			progressSpinner.setVisible(false);
			creditSaveButton.setOnAction(_ -> editCredit(credit));
			creditPopoverTitle.setText("Edit Credit");
			creditDeleteButton.setVisible(true);
			creditDeleteButton.setOnAction(_ -> deleteCredit(credit));
			contentDarken.setVisible(true);
			AnimationUtils.slideIn(addCreditPopover,0);
			creditNoField.setText(credit.getCreditNo());
			refInvNoField.setText(credit.getReferenceInvoiceNo());
			creditDateField.setValue(credit.getCreditDate());
			creditDateField.setText(credit.getCreditDateString());
			creditAmountField.setText(String.valueOf(credit.getCreditAmount()));
			creditNotesField.setText(credit.getNotes());
			Platform.runLater(() -> creditAFX.requestFocus());
		});
		task.setOnFailed(_ -> {
			dialogPane.showError("Error", "An error occurred while locating the credit supplier", task.getException());
			progressSpinner.setVisible(false);
		});
		executor.submit(task);
		progressSpinner.setVisible(true);
	}

	public void closeInvoicePopover(){
		invoicesButton.requestFocus();
		AnimationUtils.slideIn(addInvoicePopover,425);
		afxValidationLabel.setVisible(false);
		invoiceNoValidationLabel.setVisible(false);
		invoiceDateValidationLabel.setVisible(false);
		dueDateValidationLabel.setVisible(false);
		amountValidationLabel.setVisible(false);
		contentDarken.setVisible(false);
		saveButton.setDisable(false);
	}

	public void closeCreditPopover(){
		creditsButton.requestFocus();
		contentDarken.setVisible(false);
		AnimationUtils.slideIn(addCreditPopover,425);
	}

	public void addInvoice() {
		if (!invoiceAFX.isValid()) {
			invoiceAFX.requestFocus();
			return;
		}
		if (!invoiceNoField.isValid()) {
			invoiceNoField.requestFocus();
			return;
		}
		if (!invoiceDateField.isValid()) {
			invoiceDateField.requestFocus();
			return;
		}
		if (!dueDateField.isValid()) {
			dueDateField.requestFocus();
			return;
		}
		if (!amountField.isValid()) {
			amountField.requestFocus();
			return;
		}
		progressSpinner.setVisible(true);
		Task<Boolean> duplicateCheckTask = new Task<>() {
			@Override
			protected Boolean call() {
				return invoiceService.checkDuplicateInvoice(
						invoiceNoField.getText(),
						main.getCurrentStore().getStoreID(),
						invoiceAFX.getValue().getContactID()
				);
			}
		};
		duplicateCheckTask.setOnSucceeded(evt -> {
			boolean isDuplicate = duplicateCheckTask.getValue();
			if (isDuplicate) {
				invoiceNoField.requestFocus();
				invoiceNoValidationLabel.setText("Invoice Already Exists");
				invoiceNoValidationLabel.setVisible(true);
				progressSpinner.setVisible(false);
			} else {
				Task<Void> addInvoiceTask = new Task<>() {
					@Override
					protected Void call() {
						InvoiceSupplier contact = invoiceAFX.getSelectedItem();
						Invoice newInvoice = new Invoice();
						newInvoice.setSupplierID(contact.getContactID());
						newInvoice.setInvoiceNo(invoiceNoField.getText());
						newInvoice.setInvoiceDate(invoiceDateField.getValue());
						newInvoice.setDueDate(dueDateField.getValue());
						newInvoice.setDescription(descriptionField.getText());
						newInvoice.setUnitAmount(Double.parseDouble(amountField.getText()));
						newInvoice.setNotes(notesField.getText());
						newInvoice.setStoreID(main.getCurrentStore().getStoreID());
						invoiceService.addInvoice(newInvoice);
						return null;
					}
				};
				addInvoiceTask.setOnSucceeded(ev -> {
					invoiceNoValidationLabel.setVisible(false);
					invoiceNoField.clear();
					invoiceDateField.setValue(null);
					amountField.clear();
					notesField.clear();
					fillInvoiceTable();
					Platform.runLater(() -> invoiceNoField.requestFocus());
					progressSpinner.setVisible(false);
				});
				addInvoiceTask.setOnFailed(ev -> {
					dialogPane.showError("Error", "An error occurred while adding the invoice, it's likely a duplicate, or there is some server issue", addInvoiceTask.getException());
					progressSpinner.setVisible(false);
				});
				executor.submit(addInvoiceTask);
			}
		});
		duplicateCheckTask.setOnFailed(evt -> {
			dialogPane.showError("Error", "An error occurred while checking for duplicate invoices", duplicateCheckTask.getException());
			progressSpinner.setVisible(false);
		});
		executor.submit(duplicateCheckTask);
	}

	public void editInvoice(Invoice invoice) {
		if (!invoiceAFX.isValid()) {
			invoiceAFX.requestFocus();
			return;
		}
		if (!invoiceNoField.isValid()) {
			invoiceNoField.requestFocus();
			return;
		}
		if (!invoiceDateField.isValid()) {
			invoiceDateField.requestFocus();
			return;
		}
		if (!dueDateField.isValid()) {
			dueDateField.requestFocus();
			return;
		}
		if (!amountField.isValid()) {
			amountField.requestFocus();
			return;
		}
		if (!Objects.equals(invoiceNoField.getText(), invoice.getInvoiceNo())) {
			progressSpinner.setVisible(true);
			Task<Boolean> duplicateCheckTask = new Task<>() {
				@Override
				protected Boolean call() {
					return invoiceService.checkDuplicateInvoice(
							invoiceNoField.getText(),
							main.getCurrentStore().getStoreID(),
							invoiceAFX.getValue().getContactID()
					);
				}
			};
			duplicateCheckTask.setOnSucceeded(evt -> {
				boolean isDuplicate = duplicateCheckTask.getValue();
				if (isDuplicate) {
					invoiceNoField.requestFocus();
					invoiceNoValidationLabel.setText("Invoice Already Exists");
					invoiceNoValidationLabel.setVisible(true);
					progressSpinner.setVisible(false);
				} else {
					updateInvoice(invoice);
				}
			});
			duplicateCheckTask.setOnFailed(evt -> {
				dialogPane.showError("Error", "An error occurred while checking for duplicate invoices", duplicateCheckTask.getException());
				progressSpinner.setVisible(false);
			});
			executor.submit(duplicateCheckTask);
		} else {
			progressSpinner.setVisible(true);
			updateInvoice(invoice);
		}
	}

	private void updateInvoice(Invoice invoice) {
		String oldInvoiceNo = invoice.getInvoiceNo();
		int oldStoreID = invoice.getStoreID();
		int oldSupplierID = invoice.getSupplierID();
		Task<Void> updateTask = new Task<>() {
			@Override
			protected Void call() {
				InvoiceSupplier contact = invoiceAFX.getValue();
				invoice.setSupplierID(contact.getContactID());
				invoice.setInvoiceNo(invoiceNoField.getText());
				invoice.setInvoiceDate(invoiceDateField.getValue());
				invoice.setDueDate(dueDateField.getValue());
				invoice.setDescription(descriptionField.getText());
				invoice.setUnitAmount(Double.parseDouble(amountField.getText()));
				invoice.setNotes(notesField.getText());
				invoiceService.updateInvoice(invoice, oldInvoiceNo, oldStoreID, oldSupplierID);
				return null;
			}
		};
		updateTask.setOnSucceeded(evt -> {
			closeInvoicePopover();
			fillInvoiceTable();
			dialogPane.showInformation("Success", "Invoice was successfully edited");
			progressSpinner.setVisible(false);
		});
		updateTask.setOnFailed(evt -> {
			dialogPane.showError("Error", "An error occurred while updating the invoice", updateTask.getException());
			progressSpinner.setVisible(false);
		});
		executor.submit(updateTask);
	}

	public void deleteInvoice(Invoice invoice) {
		dialogPane.showWarning("Confirm Delete",
				"This action will permanently delete this Invoice from all systems,\n" +
						"Are you sure you still want to delete this Invoice?").thenAccept(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				progressSpinner.setVisible(true);
				Task<Void> task = new Task<>() {
					@Override
					protected Void call() {
						invoiceService.deleteInvoice(invoice.getInvoiceNo(), main.getCurrentStore().getStoreID(), invoice.getSupplierID());
						return null;
					}
				};
				task.setOnSucceeded(_ -> {
					closeInvoicePopover();
					fillInvoiceTable();
					dialogPane.showInformation("Success", "Invoice was successfully deleted");
					progressSpinner.setVisible(false);
				});
				task.setOnFailed(_ -> {
					dialogPane.showError("Error", "An error occurred while deleting the invoice", task.getException());
					progressSpinner.setVisible(false);
				});
				executor.submit(task);
			}
		});
	}

	public void addCredit() {
		if (!creditAFX.isValid()) {creditAFX.requestFocus();}
		else if (!creditNoField.isValid()) {creditNoField.requestFocus();}
		else if (!refInvNoField.isValid()) {refInvNoField.requestFocus();}
		else if (!creditDateField.isValid()) {creditDateField.requestFocus();}
		else if (!creditAmountField.isValid()) {creditAmountField.requestFocus();}
		else {
			InvoiceSupplier contact = creditAFX.getSelectedItem();
			progressSpinner.setVisible(true);
			Task<Void> task = new Task<>() {
				@Override
				protected Void call() {
					Credit newCredit = new Credit();
					newCredit.setSupplierID(contact.getContactID());
					newCredit.setCreditNo(creditNoField.getText());
					newCredit.setReferenceInvoiceNo(refInvNoField.getText());
					newCredit.setCreditDate(creditDateField.getValue());
					newCredit.setCreditAmount(Double.parseDouble(creditAmountField.getText()));
					newCredit.setNotes(creditNotesField.getText());
					newCredit.setStoreID(main.getCurrentStore().getStoreID());
					creditService.addCredit(newCredit);
					return null;
				}
			};
			task.setOnSucceeded(_ -> {
				creditNoField.clear();
				refInvNoField.clear();
				creditDateField.setValue(null);
				creditAmountField.clear();
				creditNotesField.clear();
				fillCreditTable();
				Platform.runLater(() -> creditNoField.requestFocus());
				progressSpinner.setVisible(false);
			});
			task.setOnFailed(_ -> {
				dialogPane.showError("Error", "An error occurred while adding the credit", task.getException());
				progressSpinner.setVisible(false);
			});
			executor.submit(task);
		}
	}

	public void editCredit(Credit credit){
		if (!creditAFX.isValid()) {creditAFX.requestFocus();}
		else if (!creditNoField.isValid()) {creditNoField.requestFocus();}
		else if (!refInvNoField.isValid()) {refInvNoField.requestFocus();}
		else if (!creditDateField.isValid()) {creditDateField.requestFocus();}
		else if (!creditAmountField.isValid()) {creditAmountField.requestFocus();}
		else {
			progressSpinner.setVisible(true);
			Task<Void> task = new Task<>() {
				@Override
				protected Void call() {
					InvoiceSupplier contact = creditAFX.getValue();
					credit.setSupplierID(contact.getContactID());
					credit.setCreditNo(creditNoField.getText());
					credit.setReferenceInvoiceNo(refInvNoField.getText());
					credit.setCreditDate(creditDateField.getValue());
					credit.setCreditAmount(Double.parseDouble(creditAmountField.getText()));
					credit.setNotes(creditNotesField.getText());
					creditService.updateCredit(credit);
					return null;
				}
			};
			task.setOnSucceeded(_ -> {
				closeCreditPopover();
				fillCreditTable();
				dialogPane.showInformation("Success", "Credit was successfully edited");
				progressSpinner.setVisible(false);
			});
			task.setOnFailed(_ -> {
				dialogPane.showError("Error", "An error occurred while updating the credit", task.getException());
				progressSpinner.setVisible(false);
			});
			executor.submit(task);
		}
	}

	public void deleteCredit(Credit credit) {
		dialogPane.showWarning("Confirm Delete",
				"This action will permanently delete this Credit from all systems,\n" +
						"Are you sure you still want to delete this Credit?").thenAccept(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				progressSpinner.setVisible(true);
				Task<Void> task = new Task<>() {
					@Override
					protected Void call() {
						creditService.deleteCredit(credit.getCreditID());
						return null;
					}
				};
				task.setOnSucceeded(_ -> {
					closeCreditPopover();
					fillCreditTable();
					dialogPane.showInformation("Success", "Credit was successfully deleted");
					progressSpinner.setVisible(false);
				});
				task.setOnFailed(_ -> {
					dialogPane.showError("Error", "An error occurred while deleting the credit", task.getException());
					progressSpinner.setVisible(false);
				});
				executor.submit(task);
			}
		});
	}

	public CompletableFuture<ObservableList<Invoice>> fetchInvoiceData() {
		return CompletableFuture.supplyAsync(() -> {
			YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
			return FXCollections.observableArrayList(
					invoiceService.getInvoiceTableData(main.getCurrentStore().getStoreID(), yearMonthObject)
			);
		}, executor);
	}

	public CompletableFuture<ObservableList<Credit>> fetchCreditData() {
		return CompletableFuture.supplyAsync(() -> {
			YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
			return FXCollections.observableArrayList(
					creditService.getAllCredits(main.getCurrentStore().getStoreID(), yearMonthObject)
			);
		}, executor);
	}

	public CompletableFuture<ObservableList<InvoiceSupplier>> fetchContactData() {
		return CompletableFuture.supplyAsync(() ->
						FXCollections.observableArrayList(
								invoiceSupplierService.getAllInvoiceSuppliers(main.getCurrentStore().getStoreID())
						)
				, executor);
	}

	public void updateInvoiceTable(ObservableList<Invoice> invoices) {
		invoiceFilterView.getItems().setAll(invoices);
		addInvoiceDoubleClickFunction();
		TableUtils.resizeTableColumns(invoicesTable, notesCol);
		for (TableColumn<?, ?> tc : invoicesTable.getColumns()) {
			tc.setSortable(true);
		}
		Platform.runLater(() -> invoicesTable.sort());
	}

	public void updateCreditTable(ObservableList<Credit> credits) {
		creditFilterView.getItems().setAll(credits);
		TableUtils.resizeTableColumns(creditsTable, creditNotesCol);
		for (TableColumn<?, ?> tc : creditsTable.getColumns()) {
			tc.setSortable(true);
		}
	}

	public void updateContactList(ObservableList<InvoiceSupplier> contacts) {
		if (contacts == null || contacts.isEmpty()) {
			invoiceAFX.getItems().add(new InvoiceSupplier(0, "*Please add new suppliers below", 0));
			creditAFX.getItems().add(new InvoiceSupplier(0, "*Please add new suppliers below", 0));
		} else {
			invoiceAFX.setItems(contacts);
			creditAFX.setItems(contacts);
		}
		invoiceAFX.clearSelection();
		creditAFX.clearSelection();
	}

	public void fillInvoiceTable() {
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		Task<ObservableList<Invoice>> task = new Task<>() {
			@Override
			protected ObservableList<Invoice> call() {
				return FXCollections.observableArrayList(invoiceService.getInvoiceTableData(main.getCurrentStore().getStoreID(), yearMonthObject));
			}
		};
		task.setOnSucceeded(_ -> {
			invoiceFilterView.getItems().setAll(task.getValue());
			addInvoiceDoubleClickFunction();
			TableUtils.resizeTableColumns(invoicesTable, notesCol);
			for (TableColumn<?, ?> tc : invoicesTable.getColumns()) {
				tc.setSortable(true);
			}
			Platform.runLater(() -> invoicesTable.sort());
		});
		task.setOnFailed(_ -> {
			dialogPane.showError("Error", "An error occurred while loading invoice data", task.getException());
		});
		executor.submit(task);
	}

	public void fillCreditTable() {
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		Task<ObservableList<Credit>> task = new Task<>() {
			@Override
			protected ObservableList<Credit> call() {
				return FXCollections.observableArrayList(creditService.getAllCredits(main.getCurrentStore().getStoreID(), yearMonthObject));
			}
		};
		task.setOnSucceeded(_ -> {
			creditFilterView.getItems().setAll(task.getValue());
			TableUtils.resizeTableColumns(creditsTable, creditNotesCol);
			for (TableColumn<?, ?> tc : creditsTable.getColumns()) {
				tc.setSortable(true);
			}
		});
		task.setOnFailed(_ -> {
			dialogPane.showError("Error", "An error occurred while loading credit data", task.getException());
		});
		executor.submit(task);
	}

	public void importFiles() {
		// 1) Load configuration to get the last used directory (if any)
		Properties props = new Properties();
		File configFile = new File("appConfig.properties");
		if (configFile.exists()) {
			try (FileInputStream fis = new FileInputStream(configFile)) {
				props.load(fis);
			} catch (IOException e) {
				dialogPane.showError("Error", "An error occurred while loading the app configuration", e);
			}
		}
		String lastDir = props.getProperty("lastInvoiceImportDir");

		// 2) Configure the FileChooser
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Invoice export File");
		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("XLS Files", "*.xls"));

		// If we have a saved last directory, set it as initial
		if (lastDir != null) {
			File lastDirFile = new File(lastDir);
			if (lastDirFile.exists() && lastDirFile.isDirectory()) {
				fileChooser.setInitialDirectory(lastDirFile);
			}
		}

		// 3) Show the file chooser
		File newfile = fileChooser.showOpenDialog(main.getStg());

		// If user selected a file...
		if (newfile != null) {
			// 4) Save the directory of this selected file as the new "last import dir"
			File parentDir = newfile.getParentFile();
			if (parentDir != null) {
				props.setProperty("lastInvoiceImportDir", parentDir.getAbsolutePath());
				try (FileOutputStream fos = new FileOutputStream(configFile)) {
					props.store(fos, "App configuration");
				} catch (IOException e) {
					dialogPane.showError("Error", "An error occurred while saving the last import directory", e);
				}
			}

			// 5) Continue your import logic (same as before)
			progressSpinner.setVisible(true);

			Task<Void> task = new Task<>() {
				@Override
				protected Void call() throws Exception {
					try (FileInputStream file = new FileInputStream(newfile)) {
						HSSFWorkbook workbook = new HSSFWorkbook(file);
						WorkbookProcessor wbp = new WorkbookProcessor(workbook);
						invoiceService.importInvoiceData(
								main.getCurrentStore().getStoreID(),
								wbp.getDataPoints()
						);
					}
					return null;
				}
			};

			task.setOnSucceeded(_ -> {
				progressSpinner.setVisible(false);
				dialogPane.showInformation("Success", "Invoice data imported successfully");
				fillInvoiceTable();
			});

			task.setOnFailed(_ -> {
				progressSpinner.setVisible(false);
				dialogPane.showError("Error",
						"An error occurred while importing invoice data",
						task.getException());
			});

			executor.submit(task);
		}
	}

	public void exportToXero() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Choose export save location");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		File file = fileChooser.showSaveDialog(main.getStg());
		if (file != null) {
			progressSpinner.setVisible(true);
			Task<Void> task = new Task<>() {
				@Override
				protected Void call() throws Exception {
					try (PrintWriter pw = new PrintWriter(file)) {
						pw.println("*ContactName,EmailAddress,POAddressLine1,POAddressLine2,POAddressLine3,POAddressLine4,POCity,PORegion,POPostalCode,POCountry,*InvoiceNumber,*InvoiceDate,*DueDate,InventoryItemCode,Description,*Quantity,*UnitAmount,*AccountCode,*TaxType,TrackingName1,TrackingOption1,TrackingName2,TrackingOption2");
						YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
						ObservableList<Invoice> currentInvoices = FXCollections.observableArrayList(invoiceService.getAllInvoices(main.getCurrentStore().getStoreID(), yearMonthObject));
						for (Invoice a : currentInvoices) {
							pw.print(a.getSupplierName() + ",,,,,,,,,,");
							pw.print(a.getInvoiceNo() + ",");
							pw.print(a.getInvoiceDate() + ",");
							pw.print(a.getDueDate() + ",,");
							pw.print(a.getDescription() + ",1,");
							pw.print("$" + a.getUnitAmount() + ",");
							pw.println("310,gst on expenses,");
						}
					}
					return null;
				}
			};
			task.setOnSucceeded(_ -> {
				progressSpinner.setVisible(false);
				dialogPane.showInformation("Success", "Information exported successfully");
			});
			task.setOnFailed(_ -> {
				progressSpinner.setVisible(false);
				Throwable exception = task.getException();
				if (exception instanceof FileNotFoundException) {
					dialogPane.showError("Error", "This file could not be accessed, please ensure it's not open in another program", exception);
				} else {
					dialogPane.showError("Error", "An error occurred while exporting data", exception);
				}
			});
			executor.submit(task);
		}
	}
}
