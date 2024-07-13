package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.FilterView;
import com.jfoenix.controls.JFXButton;
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
import javafx.util.Callback;
import models.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.controlsfx.control.PopOver;
import utils.*;

import java.io.*;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.Locale;

import static com.dlsc.gemsfx.DialogPane.Type.BLANK;

public class InvoiceEntryController extends DateSelectController implements actionableComboBox{

	private MFXDatePicker datePkr;
	@FXML
	private FlowPane datePickerPane;
	@FXML
	private StackPane monthSelector;
	@FXML
	private MFXTextField monthSelectorField;
	@FXML
	private StackPane backgroundPane;
	@FXML
	private VBox controlBox,addInvoicePopover,addCreditPopover;
	@FXML
	private BorderPane storesButton;
	@FXML
	private BorderPane invoicesButton,creditsButton;
	@FXML
	private Region contentDarken;
	@FXML
	private DialogPane dialogPane;
	@FXML
	private MFXTextField invoiceNoField,descriptionField,amountField,notesField;
	@FXML
	private MFXDatePicker invoiceDateField, dueDateField;
	@FXML
	private MFXButton saveButton;
	@FXML
	private Button deleteButton;
	@FXML
	private Label paymentPopoverTitle,expectedUnitAmountLabel,varianceLabel;
	@FXML
	private Label afxValidationLabel,invoiceNoValidationLabel,invoiceDateValidationLabel,dueDateValidationLabel,amountValidationLabel;
	@FXML
	private Label creditAFXValidationLabel,creditNoValidationLabel,refInvNoValidationLabel,creditDateValidationLabel,creditAmountValidationLabel;
	@FXML
	private Label creditPopoverTitle;
	@FXML
	private MFXTextField creditNoField,refInvNoField,creditAmountField,creditNotesField;
	@FXML
	private MFXDatePicker creditDateField;
	@FXML
	private MFXButton creditSaveButton;
	@FXML
	private Button creditDeleteButton;
	@FXML
	private JFXButton plusButton;

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


	private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private Invoice selectedInvoice;
	private PopOver currentDatePopover;

	private ObservableList<Invoice> allInvoices = FXCollections.observableArrayList();
	private ObservableList<Credit> allCredits = FXCollections.observableArrayList();

	@FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}

	public void setConnection(Connection c) {
		this.con = c;
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
		invoicesView();
	}

	public void invoicesView() {
		formatTabSelect(invoicesButton);
		formatTabDeselect(creditsButton);

		controlBox.getChildren().clear();
		invoiceFilterView = new FilterView<>();
		invoiceFilterView.setTitle("Current Invoices");
		invoiceFilterView.setTextFilterProvider(text -> invoice -> invoice.getInvoiceNo().toLowerCase().contains(text) || invoice.getSupplierName().toLowerCase().contains(text));
		allInvoices = invoiceFilterView.getFilteredItems();

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
			// Here you need to decide how you want to sort your strings.
			// For example, you can sort numerically if both strings are parseable as integers, 
			// or lexicographically otherwise. 
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
		Platform.runLater(() -> addInvoiceDoubleClickfunction());
		fillContactList();
		fillInvoiceTable();
		plusButton.setOnAction(actionEvent -> openInvoicePopover());
		contentDarken.setOnMouseClicked(actionEvent -> closeInvoicePopover());

		//Live update expected unit amount if invoice is recognised
		invoiceNoField.delegateFocusedProperty().addListener((obs, oldVal, newVal) -> {
			if (invoiceNoField.isValid()) {
				String sql = "SELECT * FROM invoicedatapoints  WHERE invoiceNo = ?";
				try {
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setString(1, invoiceNoField.getText());
					resultSet = preparedStatement.executeQuery();
					if(resultSet.next()) {
						expectedUnitAmountLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(resultSet.getDouble("amount")));
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
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		});

		amountField.delegateFocusedProperty().addListener((obs, oldVal, newVal) -> {
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
		allCredits = creditFilterView.getFilteredItems();

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
		Platform.runLater(() -> addCreditDoubleClickfunction());
		addCreditDoubleClickfunction();
		plusButton.setOnAction(actionEvent -> openCreditPopover());
		contentDarken.setOnMouseClicked(actionEvent -> closeCreditPopover());
	}

	public ActionableFilterComboBox createAFX(){
		MFXButton addSupplierButton = new MFXButton("Create New");
		addSupplierButton.setOnAction(actionEvent -> {
			dialog = new DialogPane.Dialog(dialogPane, BLANK);
			dialog.setPadding(false);
			dialog.setContent(createAddNewSupplierDialog());
			dialogPane.showDialog(dialog);
		});
		MFXButton manageSuppliersButton = new MFXButton("Manage Contacts");
		manageSuppliersButton.setOnAction(actionEvent -> {
			dialog = new DialogPane.Dialog(dialogPane, BLANK);
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

	private void addInvoiceDoubleClickfunction(){
		invoicesTable.setRowFactory( tv -> {
			TableRow<Invoice> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					Invoice rowData = row.getItem();
					openInvoicePopover(rowData);
				}
			});
			return row ;
		});
	}

	private void addCreditDoubleClickfunction(){
		creditsTable.setRowFactory( tv -> {
			TableRow<Credit> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					Credit rowData = row.getItem();
					openCreditPopover(rowData);
				}
			});
			return row ;
		});
	}


	private Node createAddNewSupplierDialog() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/AddNewSupplierDialog.fxml"));
		StackPane newContactDialog = null;
		try {
			newContactDialog = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		AddNewSupplierDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setConnection(this.con);
		dialogController.setMain(this.main);
		return newContactDialog;
	}

	private Node createManageSuppliersDialog() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FXML/ManageSuppliersDialog.fxml"));
		StackPane manageSuppliersDialog = null;
		try {
			manageSuppliersDialog = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ManageSuppliersDialogController dialogController = loader.getController();
		dialogController.setParent(this);
		dialogController.setConnection(this.con);
		dialogController.setMain(this.main);
		dialogController.fill();
		return manageSuppliersDialog;
	}

	public void fillContactList(){
		ObservableList<InvoiceSupplier> contacts = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM invoiceSuppliers where storeID = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				contacts.add(new InvoiceSupplier(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		if(contacts.size()==0){
			invoiceAFX.getItems().add(new InvoiceSupplier(0,"*Please add new suppliers below",0));
			creditAFX.getItems().add(new InvoiceSupplier(0,"*Please add new suppliers below",0));
		}else{
			invoiceAFX.setItems(contacts);
			creditAFX.setItems(contacts);
		}
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
		saveButton.setOnAction(actionEvent -> addInvoice());
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
		saveButton.setOnAction(actionEvent -> editInvoice(invoice));
		paymentPopoverTitle.setText("Edit Invoice");
		deleteButton.setVisible(true);
		deleteButton.setOnAction(actionEvent -> deleteInvoice(invoice));
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addInvoicePopover,0);
		invoiceAFX.setValue(getContactfromName(invoice.getSupplierName()));
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
		creditSaveButton.setOnAction(actionEvent -> addCredit());
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
		creditSaveButton.setOnAction(actionEvent -> editCredit(credit));
		creditPopoverTitle.setText("Edit Credit");
		creditDeleteButton.setVisible(true);
		creditDeleteButton.setOnAction(actionEvent -> deleteCredit(credit));
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addCreditPopover,0);
		creditAFX.setValue(getContactfromName(credit.getSupplierName()));
		creditNoField.setText(credit.getCreditNo());
		refInvNoField.setText(credit.getReferenceInvoiceNo());
		creditDateField.setValue(credit.getCreditDate());
		creditAmountField.setText(String.valueOf(credit.getCreditAmount()));
		creditNotesField.setText(credit.getNotes());
		Platform.runLater(() -> creditAFX.requestFocus());
	}

	public InvoiceSupplier getContactfromName(String name){
		String sql = null;
		try {
			sql = "SELECT * FROM invoicesuppliers  WHERE supplierName = ? AND storeID = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1,name);
			preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				return new InvoiceSupplier(resultSet);
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return null;
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
		String sql = "SELECT * FROM invoices WHERE invoiceNo = ? AND storeID = ? AND supplierID = ?";
		try {
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1, invoiceNoField.getText());
			preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(3, ((InvoiceSupplier) invoiceAFX.getValue()).getContactID());
			resultSet = preparedStatement.executeQuery();
			if(resultSet.next()) {
				return true;
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
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
		}
		else{
			InvoiceSupplier contact = (InvoiceSupplier) invoiceAFX.getSelectedItem();
			String invoiceNo = invoiceNoField.getText();
			LocalDate invoiceDate = invoiceDateField.getValue();
			LocalDate dueDate = dueDateField.getValue();
			String description = descriptionField.getText();
			String notes = notesField.getText();
			double unitAmount = Double.parseDouble(amountField.getText());
			String sql = "INSERT INTO invoices(supplierID,invoiceNo,invoiceDate,dueDate,description,unitAmount,notes,storeID) VALUES(?,?,?,?,?,?,?,?)";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setInt(1, contact.getContactID());
				preparedStatement.setString(2, invoiceNo);
				preparedStatement.setDate(3, Date.valueOf(invoiceDate));
				preparedStatement.setDate(4, Date.valueOf(dueDate));
				preparedStatement.setString(5, description);
				preparedStatement.setDouble(6, unitAmount);
				preparedStatement.setString(7, notes);
				preparedStatement.setInt(8, main.getCurrentStore().getStoreID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
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
		if(!invoiceAFX.isValid()){invoiceAFX.requestFocus();} else if(!invoiceNoField.isValid()){invoiceNoField.requestFocus();} else if(!invoiceDateField.isValid()){invoiceDateField.requestFocus();} else if(!dueDateField.isValid()){dueDateField.requestFocus();} else if(!amountField.isValid()){amountField.requestFocus();} else {
			InvoiceSupplier contact = (InvoiceSupplier) invoiceAFX.getValue();
			String invoiceNo = invoiceNoField.getText();
			LocalDate invoiceDate = invoiceDateField.getValue();
			LocalDate dueDate = dueDateField.getValue();
			String description = descriptionField.getText();
			double unitAmount = Double.parseDouble(amountField.getText());
			String notes = notesField.getText();

			String sql = "UPDATE invoices SET supplierID = ?,storeID = ?,invoiceNo = ?, invoiceDate = ?, dueDate = ?,description = ?,unitAmount = ?,notes = ? WHERE invoiceNo = ? AND storeID = ?";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setInt(1, contact.getContactID());
				preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
				preparedStatement.setString(3, invoiceNo);
				preparedStatement.setDate(4, Date.valueOf(invoiceDate));
				preparedStatement.setDate(5, Date.valueOf(dueDate));
				preparedStatement.setString(6, description);
				preparedStatement.setDouble(7, unitAmount);
				preparedStatement.setString(8, notes);
				preparedStatement.setString(9, invoice.getInvoiceNo());
				preparedStatement.setInt(10, main.getCurrentStore().getStoreID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
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
				String sql = "DELETE from invoices WHERE invoiceNo=? AND storeID=?";
				try {
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setString(1, invoice.getInvoiceNo());
					preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
					preparedStatement.executeUpdate();
				} catch (SQLException ex) {
					System.err.println(ex.getMessage());
				}
				closeInvoicePopover();
				fillInvoiceTable();
				dialogPane.showInformation("Success","Invoice was successfully deleted");
			}
		});

	}

	public void addCredit() {
		if (!creditAFX.isValid()) {creditAFX.requestFocus();} else if (!creditNoField.isValid()) {creditNoField.requestFocus();} else if (!refInvNoField.isValid()) {refInvNoField.requestFocus();} else if (!creditDateField.isValid()) {creditDateField.requestFocus();} else if (!creditAmountField.isValid()) {creditAmountField.requestFocus();} else {
			InvoiceSupplier contact = (InvoiceSupplier) creditAFX.getSelectedItem();
			String creditNo = creditNoField.getText();
			String refInvNo = refInvNoField.getText();
			LocalDate creditDate = creditDateField.getValue();
			double creditAmount = Double.parseDouble(creditAmountField.getText());
			String creditNotes = creditNotesField.getText();

			String sql = "INSERT INTO credits(supplierID,creditNo,referenceInvoiceNo,creditDate,creditAmount,notes,storeID) VALUES(?,?,?,?,?,?,?)";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setInt(1, contact.getContactID());
				preparedStatement.setString(2, creditNo);
				preparedStatement.setString(3, refInvNo);
				preparedStatement.setDate(4, Date.valueOf(creditDate));
				preparedStatement.setDouble(5, creditAmount);
				preparedStatement.setString(6, creditNotes);
				preparedStatement.setInt(7, main.getCurrentStore().getStoreID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
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
		if (!creditAFX.isValid()) {creditAFX.requestFocus();} else if (!creditNoField.isValid()) {creditNoField.requestFocus();} else if (!refInvNoField.isValid()) {refInvNoField.requestFocus();} else if (!creditDateField.isValid()) {creditDateField.requestFocus();} else if (!creditAmountField.isValid()) {creditAmountField.requestFocus();} else {
			InvoiceSupplier contact = (InvoiceSupplier) creditAFX.getValue();
			String creditNo = creditNoField.getText();
			String refInvNo = refInvNoField.getText();
			LocalDate creditDate = creditDateField.getValue();
			double creditAmount = Double.parseDouble(creditAmountField.getText());
			String creditNotes = creditNotesField.getText();

			String sql = "UPDATE credits SET supplierID = ?,creditNo = ?,referenceInvoiceNo = ?,creditDate = ?,creditAmount = ?,notes = ? WHERE idCredits = ?";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setInt(1, contact.getContactID());
				preparedStatement.setString(2, creditNo);
				preparedStatement.setString(3, refInvNo);
				preparedStatement.setDate(4, Date.valueOf(creditDate));
				preparedStatement.setDouble(5, creditAmount);
				preparedStatement.setString(6, creditNotes);
				preparedStatement.setInt(7, credit.getCreditID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
			closeCreditPopover();
			fillCreditTable();
			dialogPane.showInformation("Success", "Credit was succesfully edited");
		}
	}

	public void deleteCredit(Credit credit) {
		dialogPane.showWarning("Confirm Delete",
				"This action will permanently delete this Credit from all systems,\n" +
						"Are you sure you still want to delete this Credit?").thenAccept(buttonType -> {
			if (buttonType.equals(ButtonType.OK)) {
				String sql = "DELETE from credits WHERE idCredits = ?";
				try {
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setInt(1, credit.getCreditID());
					preparedStatement.executeUpdate();
				} catch (SQLException ex) {
					System.err.println(ex.getMessage());
				}
				closeCreditPopover();
				fillCreditTable();
				dialogPane.showInformation("Success","Credit was succesfully deleted");
			}
		});
	}

	public void fillInvoiceTable(){

		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		ObservableList<Invoice> currentInvoiceDataPoints = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT \n" +
					"    invoices.*,\n" +
					"    i.*,\n" +
					"    idp.*,\n" +
					"    (SELECT \n" +
					"            SUM(credits.creditAmount)\n" +
					"        FROM\n" +
					"            credits\n" +
					"        WHERE\n" +
					"            credits.referenceInvoiceNo = invoices.invoiceNo) AS total_credits\n" +
					"FROM\n" +
					"    invoices\n" +
					"        JOIN\n" +
					"    invoiceSuppliers i ON invoices.supplierID = i.idinvoiceSuppliers\n" +
					"        LEFT JOIN\n" +
					"    invoicedatapoints idp ON invoices.invoiceNo = idp.invoiceNo\n" +
					"WHERE\n" +
					"    invoices.storeID = ?\n" + "AND MONTH(invoices.invoiceDate) = ?\n" + "AND YEAR(invoices.invoiceDate) = ?\n" +
					"ORDER BY invoices.invoiceNo ASC";

			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentInvoiceDataPoints.add(new Invoice(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		invoiceFilterView.getItems().setAll(currentInvoiceDataPoints);
		addInvoiceDoubleClickfunction();
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
		int daysInMonth = yearMonthObject.lengthOfMonth();
		ObservableList<Credit> currentCreditDataPoints = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM credits JOIN invoiceSuppliers i on credits.supplierID = i.idinvoiceSuppliers WHERE credits.storeID = ? AND MONTH(creditDate) = ? AND YEAR(creditDate) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentCreditDataPoints.add(new Credit(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		creditFilterView.getItems().setAll(currentCreditDataPoints);
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
				String sql = "INSERT INTO invoicedatapoints(storeID,invoiceNo,amount) VALUES(?,?,?) ON DUPLICATE KEY UPDATE amount=?";
				try {
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
					preparedStatement.setString(2, cdp.getCategory());
					preparedStatement.setDouble(3, cdp.getAmount());
					preparedStatement.setDouble(4, cdp.getAmount());
					preparedStatement.executeUpdate();
				} catch (SQLException ex) {
					System.err.println(ex.getMessage());
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
				int daysInMonth = yearMonthObject.lengthOfMonth();

				ObservableList<Invoice> currentInvoices = FXCollections.observableArrayList();
				String sql = null;
				try {
					sql = "SELECT * FROM invoices JOIN invoicesuppliers a on a.idinvoiceSuppliers = invoices.supplierID JOIN invoicedatapoints i on invoices.invoiceNo = i.invoiceNo WHERE invoices.storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
					preparedStatement.setInt(2, yearMonthObject.getMonthValue());
					preparedStatement.setInt(3, yearMonthObject.getYear());
					resultSet = preparedStatement.executeQuery();
					while (resultSet.next()) {
						currentInvoices.add(new Invoice(resultSet));
					}
				} catch (SQLException throwables) {
					throwables.printStackTrace();
				}


				for(Invoice a: currentInvoices){
					pw.print(a.getSupplierName()+",,,,,,,,,,");
					pw.print(a.getInvoiceNo()+",");
					pw.print(a.getInvoiceDate()+",");
					pw.print(a.getDueDate()+",,");
					pw.print(a.getDescription()+",1,");
					pw.print("$"+a.getUnitAmount()+",");
					pw.println("310,gst on expenses,");
				}
				dialogPane.showInformation("Success", "Information exported succesfully");
			} catch (FileNotFoundException e){
				dialogPane.showError("Error", "This file could not be accessed, please ensure its not open in another program");
			}
		}
	}

	public DialogPane.Dialog<Object> getDialog() {
		return dialog;
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



