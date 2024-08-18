package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.FilterView;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXNodesList;
import components.ActionableFilterComboBox;
import components.CustomDateStringConverter;
import interfaces.actionableComboBox;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import models.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.controlsfx.control.PopOver;
import services.CreditService;
import services.InvoiceService;
import services.InvoiceSupplierService;
import utils.*;

import java.io.*;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

import static com.dlsc.gemsfx.DialogPane.Type.BLANK;

public class InvoiceEntryController extends DateSelectController implements actionableComboBox{

	@FXML private StackPane monthSelector;
	@FXML private MFXTextField monthSelectorField;
	@FXML private VBox controlBox,addInvoicePopover,addCreditPopover;
	@FXML private BorderPane invoicesButton,creditsButton;
	@FXML private Region contentDarken;
	@FXML private DialogPane dialogPane;
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

	private TableView<Invoice> invoicesTable = new TableView<>();
	private TableView<Credit> creditsTable = new TableView<>();
	private TableColumn<Invoice,String> supplierNameCol;
	private TableColumn<Invoice,String> invoiceNoCol;
	private TableColumn<Invoice,LocalDate> invoiceDateCol;
	private TableColumn<Invoice,LocalDate> dueDateCol;
	private TableColumn<Invoice,Double> unitAmountCol;
	private TableColumn<Invoice,Double> importedInvoiceAmountCol;
	private TableColumn<Invoice,Double> varianceCol;
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
	private ActionableFilterComboBox invoiceAFX,creditAFX;
	private DialogPane.Dialog<Object> dialog;

    private Main main;
	private PopOver currentDatePopover;
	private InvoiceService invoiceService;
	private InvoiceSupplierService invoiceSupplierService;
	private CreditService creditService;

    @FXML
	private void initialize() {
		invoiceService = new InvoiceService();
		invoiceSupplierService = new InvoiceSupplierService();
		creditService = new CreditService();
	}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}

	@Override
	public void fill(){
		invoiceAFX = createAFX();
		creditAFX = createAFX();
		setDate(main.getCurrentDate());
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
		invoicesView();
	}

	public void invoicesView() {
		formatTabSelect(invoicesButton);
		formatTabDeselect(creditsButton);
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
		invoicesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		invoicesTable.setMaxWidth(Double.MAX_VALUE);
		invoicesTable.setMaxHeight(Double.MAX_VALUE);
		invoiceFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left
		controlBox.getChildren().addAll(invoiceFilterView,invoicesTable);
		invoicesTable.setFixedCellSize(25.0);
		VBox.setVgrow(invoicesTable, Priority.ALWAYS);
		invoicesTable.setItems(allInvoices);
		for(TableColumn tc: invoicesTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		Platform.runLater(() -> GUIUtils.customResize(invoicesTable,notesCol));
		Platform.runLater(this::addInvoiceDoubleClickFunction);
		fillContactList();
		fillInvoiceTable();
		plusButton.setOnAction(_ -> openInvoicePopover());
		contentDarken.setOnMouseClicked(_ -> closeInvoicePopover());
		//Live update expected unit amount if invoice is recognised
		invoiceNoField.delegateFocusedProperty().addListener((_, _, _) -> {
			if (invoiceNoField.isValid()) {
				try {
					Invoice invoice = invoiceService.getInvoice(invoiceNoField.getText());
					if(invoice!=null) {
						expectedUnitAmountLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(invoice.getImportedInvoiceAmount()));
						invoiceNoValidationLabel.setText("");
						invoiceNoValidationLabel.setStyle("-fx-text-fill: red;");
						invoiceNoValidationLabel.setVisible(false);
						if(amountField.isValid()){
							varianceLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(Double.parseDouble(expectedUnitAmountLabel.getText().replace("$","")) - Double.parseDouble(amountField.getText())));
						}
					}else{
						expectedUnitAmountLabel.setText("N/A");
						invoiceNoValidationLabel.setText("Warning: Invoice not recognised");
						invoiceNoValidationLabel.setStyle("-fx-text-fill: orange;");
						invoiceNoValidationLabel.setVisible(true);
						varianceLabel.setText("N/A");
					}
				} catch (SQLException ex) {
					dialogPane.showError("Error","An error occurred while loading invoice information",ex.getMessage());
					ex.printStackTrace();
				}
			}
		});
		amountField.delegateFocusedProperty().addListener((_, _, _) -> {
			if (amountField.isValid()) {
				if(expectedUnitAmountLabel.getText().equals("N/A"))
					varianceLabel.setText("N/A");
				else
					varianceLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(Double.parseDouble(expectedUnitAmountLabel.getText().replace("$","")) - Double.parseDouble(amountField.getText())));
			}
		});
	}

	public void creditsView(){
		formatTabSelect(creditsButton);
		formatTabDeselect(invoicesButton);
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
		creditsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		creditsTable.setMaxWidth(Double.MAX_VALUE);
		creditsTable.setMaxHeight(Double.MAX_VALUE);
		creditFilterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left
		controlBox.getChildren().addAll(creditFilterView,creditsTable);
		creditsTable.setFixedCellSize(25.0);
		VBox.setVgrow(creditsTable, Priority.ALWAYS);
		creditsTable.setItems(allCredits);
		for(TableColumn tc: creditsTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		fillContactList();
		fillCreditTable();
		Platform.runLater(() -> GUIUtils.customResize(creditsTable,creditNotesCol));
		Platform.runLater(this::addCreditDoubleClickFunction);
		addCreditDoubleClickFunction();
		plusButton.setOnAction(_ -> openCreditPopover());
		contentDarken.setOnMouseClicked(_ -> closeCreditPopover());
	}

	public ActionableFilterComboBox createAFX(){
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
		ActionableFilterComboBox newAFX = new ActionableFilterComboBox(addSupplierButton, manageSuppliersButton);
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
			dialogPane.showError("Error","An error occurred while loading the new supplier dialog",e.getMessage());
			e.printStackTrace();
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
			dialogPane.showError("Error","An error occurred while loading the manage suppliers dialog",e.getMessage());
			e.printStackTrace();
		}
		ManageSuppliersDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setMain(this.main);
		dialogController.fill();
		return manageSuppliersDialog;
	}

	public void fillContactList(){
		ObservableList<InvoiceSupplier> contacts = null;
		try {
			contacts = FXCollections.observableArrayList(invoiceSupplierService.getAllInvoiceSuppliers(main.getCurrentStore().getStoreID()));
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while loading invoice suppliers",ex.getMessage());
			ex.printStackTrace();
		}
        assert contacts != null;
        if(contacts.isEmpty()){
			invoiceAFX.getItems().add(new InvoiceSupplier(0,"*Please add new suppliers below",0));
			creditAFX.getItems().add(new InvoiceSupplier(0,"*Please add new suppliers below",0));
		}else{
			invoiceAFX.setItems(contacts);
			creditAFX.setItems(contacts);
		}
		invoiceAFX.clearSelection();
		creditAFX.clearSelection();
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
				dialogPane.showError("Error","An error occurred while loading the month selector",e.getMessage());
				e.printStackTrace();
			}
			MonthYearSelectorContentController rdc = loader.getController();
			rdc.setMain(main);
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

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		String fieldText = main.getCurrentDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		fieldText += ", ";
		fieldText += main.getCurrentDate().getYear();
		monthSelectorField.setText(fieldText);
		fillInvoiceTable();
		fillCreditTable();
		fillContactList();
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
		saveButton.setOnAction(_ -> editInvoice(invoice));
		paymentPopoverTitle.setText("Edit Invoice");
		deleteButton.setVisible(true);
		deleteButton.setOnAction(_ -> deleteInvoice(invoice));
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addInvoicePopover,0);
		try{
			invoiceAFX.setValue(invoiceSupplierService.getInvoiceSupplierByName(invoice.getSupplierName(),main.getCurrentStore().getStoreID()));
		}catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while locating the invoice supplier",ex.getMessage());
			ex.printStackTrace();
		}
		invoiceNoField.setText(invoice.getInvoiceNo());
		invoiceDateField.setValue(invoice.getInvoiceDate());
		dueDateField.setValue(invoice.getDueDate());
		amountField.setText(String.valueOf(invoice.getUnitAmount()));
		notesField.setText(invoice.getNotes());
		expectedUnitAmountLabel.setText("$"+String.format("%.2f",invoice.getImportedInvoiceAmount()));
		varianceLabel.setText("$"+String.format("%.2f",invoice.getVariance()));
		Platform.runLater(() -> invoiceAFX.requestFocus());
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
		creditSaveButton.setOnAction(_ -> editCredit(credit));
		creditPopoverTitle.setText("Edit Credit");
		creditDeleteButton.setVisible(true);
		creditDeleteButton.setOnAction(_ -> deleteCredit(credit));
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addCreditPopover,0);
		try{
			creditAFX.setValue(invoiceSupplierService.getInvoiceSupplierByName(credit.getSupplierName(),main.getCurrentStore().getStoreID()));
		}catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while locating the credit supplier",ex.getMessage());
			ex.printStackTrace();
		}
		creditNoField.setText(credit.getCreditNo());
		refInvNoField.setText(credit.getReferenceInvoiceNo());
		creditDateField.setValue(credit.getCreditDate());
		creditDateField.setText(credit.getCreditDateString());
		creditAmountField.setText(String.valueOf(credit.getCreditAmount()));
		creditNotesField.setText(credit.getNotes());
		Platform.runLater(() -> creditAFX.requestFocus());
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

	public boolean invoiceDuplicateCheck(){
		try {
			return invoiceService.checkDuplicateInvoice(invoiceNoField.getText(),main.getCurrentStore().getStoreID(),((InvoiceSupplier) invoiceAFX.getValue()).getContactID());
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while checking for duplicate invoices",ex.getMessage());
			ex.printStackTrace();
		}
		return false;
	}

	public void addInvoice(){
		if(!invoiceAFX.isValid()){invoiceAFX.requestFocus();}
		else if(!invoiceNoField.isValid()){invoiceNoField.requestFocus();}
		else if(!invoiceDateField.isValid()){invoiceDateField.requestFocus();}
		else if(!dueDateField.isValid()){dueDateField.requestFocus();}
		else if(!amountField.isValid()){amountField.requestFocus();}
		else if(invoiceDuplicateCheck()){
			invoiceNoField.requestFocus();
			invoiceNoValidationLabel.setText("Invoice Already Exists");
			invoiceNoValidationLabel.setVisible(true);
		}else{
			InvoiceSupplier contact = (InvoiceSupplier) invoiceAFX.getSelectedItem();
			try {
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
			} catch (SQLException ex) {
				dialogPane.showError("Error","An error occurred while adding the invoice",ex.getMessage());
				ex.printStackTrace();
			}
			invoiceNoValidationLabel.setVisible(false);
			invoiceNoField.clear();
			invoiceDateField.setValue(null);
			dueDateField.setValue(null);
			amountField.clear();
			notesField.clear();
			fillInvoiceTable();
			Platform.runLater(() -> invoiceNoField.requestFocus());
		}
	}

	public void editInvoice(Invoice invoice){
		if(!invoiceAFX.isValid()){invoiceAFX.requestFocus();}
		else if(!invoiceNoField.isValid()){invoiceNoField.requestFocus();}
		else if(!invoiceDateField.isValid()){invoiceDateField.requestFocus();}
		else if(!dueDateField.isValid()){dueDateField.requestFocus();}
		else if(!amountField.isValid()){amountField.requestFocus();}
		else if(!Objects.equals(invoiceNoField.getText(), invoice.getInvoiceNo()) &&invoiceDuplicateCheck()){
			invoiceNoField.requestFocus();
			invoiceNoValidationLabel.setText("Invoice Already Exists");
			invoiceNoValidationLabel.setVisible(true);
		}else{
			InvoiceSupplier contact = (InvoiceSupplier) invoiceAFX.getValue();
			try {
				String oldInvoiceNo = invoice.getInvoiceNo();
				invoice.setSupplierID(contact.getContactID());
				invoice.setInvoiceNo(invoiceNoField.getText());
				invoice.setInvoiceDate(invoiceDateField.getValue());
				invoice.setDueDate(dueDateField.getValue());
				invoice.setDescription(descriptionField.getText());
				invoice.setUnitAmount(Double.parseDouble(amountField.getText()));
				invoice.setNotes(notesField.getText());
				invoiceService.updateInvoice(invoice,oldInvoiceNo);
			} catch (SQLException ex) {
				dialogPane.showError("Error","An error occurred while updating the invoice",ex.getMessage());
				ex.printStackTrace();
			}
			closeInvoicePopover();
			fillInvoiceTable();
			dialogPane.showInformation("Success", "Invoice was successfully edited");
		}
	}

	public void deleteInvoice(Invoice invoice){
		dialogPane.showWarning("Confirm Delete",
				"This action will permanently delete this Invoice from all systems,\n" +
						"Are you sure you still want to delete this Invoice?").thenAccept(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				try {
					invoiceService.deleteInvoice(invoice.getInvoiceNo(),main.getCurrentStore().getStoreID());
				} catch (SQLException ex) {
					dialogPane.showError("Error","An error occurred while deleting the invoice",ex.getMessage());
					ex.printStackTrace();
				}
				closeInvoicePopover();
				fillInvoiceTable();
				dialogPane.showInformation("Success","Invoice was successfully deleted");
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
			InvoiceSupplier contact = (InvoiceSupplier) creditAFX.getSelectedItem();
			try {
				Credit newCredit = new Credit();
				newCredit.setSupplierID(contact.getContactID());
				newCredit.setCreditNo(creditNoField.getText());
				newCredit.setReferenceInvoiceNo(refInvNoField.getText());
				newCredit.setCreditDate(creditDateField.getValue());
				newCredit.setCreditAmount(Double.parseDouble(creditAmountField.getText()));
				newCredit.setNotes(creditNotesField.getText());
				newCredit.setStoreID(main.getCurrentStore().getStoreID());
				creditService.addCredit(newCredit);
			} catch (SQLException ex) {
				dialogPane.showError("Error","An error occurred while adding the credit",ex.getMessage());
				ex.printStackTrace();
			}
			creditNoField.clear();
			refInvNoField.clear();
			creditDateField.setValue(null);
			creditAmountField.clear();
			creditNotesField.clear();
			fillCreditTable();
			Platform.runLater(() -> creditNoField.requestFocus());
		}
	}

	public void editCredit(Credit credit){
		if (!creditAFX.isValid()) {creditAFX.requestFocus();}
		else if (!creditNoField.isValid()) {creditNoField.requestFocus();}
		else if (!refInvNoField.isValid()) {refInvNoField.requestFocus();}
		else if (!creditDateField.isValid()) {creditDateField.requestFocus();}
		else if (!creditAmountField.isValid()) {creditAmountField.requestFocus();}
		else {
			InvoiceSupplier contact = (InvoiceSupplier) creditAFX.getValue();
			try {
				credit.setSupplierID(contact.getContactID());
				credit.setCreditNo(creditNoField.getText());
				credit.setReferenceInvoiceNo(refInvNoField.getText());
				credit.setCreditDate(creditDateField.getValue());
				credit.setCreditAmount(Double.parseDouble(creditAmountField.getText()));
				credit.setNotes(creditNotesField.getText());
				creditService.updateCredit(credit);
			} catch (SQLException ex) {
				dialogPane.showError("Error","An error occurred while updating the credit",ex.getMessage());
				ex.printStackTrace();
			}
			closeCreditPopover();
			fillCreditTable();
			dialogPane.showInformation("Success", "Credit was successfully edited");
		}
	}

	public void deleteCredit(Credit credit) {
		dialogPane.showWarning("Confirm Delete",
				"This action will permanently delete this Credit from all systems,\n" +
						"Are you sure you still want to delete this Credit?").thenAccept(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				try {
					creditService.deleteCredit(credit.getCreditID());
				} catch (SQLException ex) {
					dialogPane.showError("Error","An error occurred while deleting the credit",ex.getMessage());
					ex.printStackTrace();
				}
				closeCreditPopover();
				fillCreditTable();
				dialogPane.showInformation("Success","Credit was successfully deleted");
			}
		});
	}

	public void fillInvoiceTable(){
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		try {
			ObservableList<Invoice> currentInvoiceDataPoints  = FXCollections.observableArrayList(invoiceService.getInvoiceTableData(main.getCurrentStore().getStoreID(),yearMonthObject));
			invoiceFilterView.getItems().setAll(currentInvoiceDataPoints);
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while loading invoice data",ex.getMessage());
			ex.printStackTrace();
		}
		addInvoiceDoubleClickFunction();
		invoicesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		invoicesTable.setMaxWidth(Double.MAX_VALUE);
		invoicesTable.setMaxHeight(Double.MAX_VALUE);
		invoicesTable.setFixedCellSize(25.0);
		VBox.setVgrow(invoicesTable, Priority.ALWAYS);
		for(TableColumn tc: invoicesTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
			tc.setSortable(true);
		}
		Platform.runLater(() -> GUIUtils.customResize(invoicesTable,notesCol));
		Platform.runLater(() -> invoicesTable.sort());
	}

	public void fillCreditTable(){
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		try {
			ObservableList<Credit> currentCreditDataPoints = FXCollections.observableArrayList(creditService.getAllCredits(main.getCurrentStore().getStoreID(),yearMonthObject));
			creditFilterView.getItems().setAll(currentCreditDataPoints);
		} catch (SQLException ex) {
			dialogPane.showError("Error","An error occurred while loading credit data",ex.getMessage());
			ex.printStackTrace();
		}
		creditsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		creditsTable.setMaxWidth(Double.MAX_VALUE);
		creditsTable.setMaxHeight(Double.MAX_VALUE);
		creditsTable.setFixedCellSize(25.0);
		VBox.setVgrow(creditsTable, Priority.ALWAYS);
		for(TableColumn tc: creditsTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
			tc.setSortable(true);
		}
		Platform.runLater(() -> GUIUtils.customResize(creditsTable,notesCol));
	}

	public void importFiles() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Invoice export File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XLS Files", "*.xls"));
		File newfile = fileChooser.showOpenDialog(main.getStg());
		if(newfile!=null){
			FileInputStream file = new FileInputStream(newfile);
			HSSFWorkbook workbook = new HSSFWorkbook(file);
			WorkbookProcessor wbp = new WorkbookProcessor(workbook);
			for(CellDataPoint cdp : wbp.getDataPoints()){
				try {
					invoiceService.importInvoiceData(main.getCurrentStore().getStoreID(),cdp);
				} catch (SQLException ex) {
					dialogPane.showError("Error","An error occurred while importing invoice data",ex.getMessage());
					ex.printStackTrace();
				}
			}
		}
	}

	public void exportToXero(){
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Choose export save location");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		File file = fileChooser.showSaveDialog(main.getStg());
		if (file != null) {
			try (PrintWriter pw = new PrintWriter(file)) {
				pw.println("*ContactName,EmailAddress,POAddressLine1,POAddressLine2,POAddressLine3,POAddressLine4,POCity,PORegion,POPostalCode,POCountry,*InvoiceNumber,*InvoiceDate,*DueDate,InventoryItemCode,Description,*Quantity,*UnitAmount,*AccountCode,*TaxType,TrackingName1,TrackingOption1,TrackingName2,TrackingOption2");
				YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
				try {
					ObservableList<Invoice> currentInvoices = FXCollections.observableArrayList(invoiceService.getAllInvoices(main.getCurrentStore().getStoreID(),yearMonthObject));
					for(Invoice a: currentInvoices){
						pw.print(a.getSupplierName()+",,,,,,,,,,");
						pw.print(a.getInvoiceNo()+",");
						pw.print(a.getInvoiceDate()+",");
						pw.print(a.getDueDate()+",,");
						pw.print(a.getDescription()+",1,");
						pw.print("$"+a.getUnitAmount()+",");
						pw.println("310,gst on expenses,");
					}
				} catch (SQLException ex) {
					dialogPane.showError("Error","An error occurred while loading invoice data",ex.getMessage());
					ex.printStackTrace();
				}
				dialogPane.showInformation("Success", "Information exported succesfully");
			} catch (FileNotFoundException e){
				dialogPane.showError("Error", "This file could not be accessed, please ensure its not open in another program");
				e.printStackTrace();
			}
		}
	}

	public DialogPane.Dialog<Object> getDialog() {
		return dialog;
	}

	public DialogPane getDialogPane() {
		return dialogPane;
	}

	public void formatTabSelect(BorderPane b){
		for (Node n:b.getChildren()) {
			if(n.getAccessibleRole() == AccessibleRole.TEXT){
				Label a = (Label) n;
				a.setStyle("-fx-text-fill: #0F60FF");
			}
			if(n.getAccessibleRole() == AccessibleRole.PARENT){
				Region a = (Region) n;
				a.setStyle("-fx-background-color: #0F60FF");
			}
		}
	}

	public void formatTabDeselect(BorderPane b){
		for (Node n:b.getChildren()) {
			if(n.getAccessibleRole() == AccessibleRole.TEXT){
				Label a = (Label) n;
				a.setStyle("-fx-text-fill: #6e6b7b");
			}
			if(n.getAccessibleRole() == AccessibleRole.PARENT){
				Region a = (Region) n;
				a.setStyle("");
			}
		}
	}
}
