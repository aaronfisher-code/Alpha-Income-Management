package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.FilterView;
import components.ActionableFilterComboBox;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import models.AccountPayment;
import models.AccountPaymentContactDataPoint;
import models.Invoice;
import models.InvoiceSupplier;
import org.controlsfx.control.PopOver;
import utils.AnimationUtils;
import utils.GUIUtils;
import utils.TableUtils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
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
	private VBox controlBox,addInvoicePopover;
	@FXML
	private BorderPane storesButton;
	@FXML
	private BorderPane invoicesButton;
	@FXML
	private Region contentDarken;
	@FXML
	private DialogPane dialogPane;
	@FXML
	private MFXTextField invoiceNoField,descriptionField,amountField,expectedAmountField,varianceField;
	@FXML
	private MFXDatePicker invoiceDateField, dueDateField;
	@FXML
	private MFXButton saveButton;
	@FXML
	private Button deleteButton;
	@FXML
	private Label paymentPopoverTitle;

	private TableView<Invoice> invoicesTable = new TableView<>();
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
	private FilterView<Invoice> filterView = new FilterView<>();
	private ActionableFilterComboBox afx;
	private DialogPane.Dialog<Object> dialog;
	
	
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private Invoice selectedInvoice;
	private PopOver currentDatePopover;

	private ObservableList<Invoice> allInvoices = FXCollections.observableArrayList();
	
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
	public void fill() {

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
		afx = new ActionableFilterComboBox(addSupplierButton,manageSuppliersButton);

		afx.setFloatMode(FloatMode.ABOVE);
		afx.setFloatingText("Contact name");
		afx.setFloatingTextGap(5);
		afx.setBorderGap(0);
		afx.setStyle("-mfx-gap: 5");
		afx.setMaxWidth(Double.MAX_VALUE);
		afx.setMinHeight(38.4);
		addInvoicePopover.getChildren().add(1,afx);

		filterView = new FilterView<>();
		filterView.setTitle("Current Invoices");
		filterView.setTextFilterProvider(text -> invoice -> invoice.getInvoiceNo().toLowerCase().contains(text) || invoice.getSupplierName().toLowerCase().contains(text));
		allInvoices = filterView.getFilteredItems();

		supplierNameCol = new TableColumn<>("SUPPLIER");
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
		invoiceDateCol.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
		dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
		unitAmountCol.setCellValueFactory(new PropertyValueFactory<>("unitAmount"));
		importedInvoiceAmountCol.setCellValueFactory(new PropertyValueFactory<>("importedInvoiceAmount"));
		varianceCol.setCellValueFactory(new PropertyValueFactory<>("variance"));
		creditsCol.setCellValueFactory(new PropertyValueFactory<>("credits"));
		totalAfterCreditCol.setCellValueFactory(new PropertyValueFactory<>("totalAfterCredits"));
		notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

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
		filterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left
		controlBox.getChildren().addAll(filterView,invoicesTable);
		invoicesTable.setFixedCellSize(25.0);
		VBox.setVgrow(invoicesTable, Priority.ALWAYS);
		invoicesTable.setItems(allInvoices);
		for(TableColumn tc: invoicesTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		Platform.runLater(() -> GUIUtils.customResize(invoicesTable,notesCol));
		Platform.runLater(() -> setDate(LocalDate.now()));
		fillContactList();
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
			afx.getItems().add(new InvoiceSupplier(0,"*Please add new suppliers below",0));
		}else{
			afx.setItems(contacts);
		}
	}

	public void exportFiles(){}

	public void importFiles(){}

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
		fillTable();
		fillContactList();
	}

	public void openPopover(){
		saveButton.setOnAction(actionEvent -> addInvoice());
		paymentPopoverTitle.setText("Add new Invoice");
		deleteButton.setVisible(false);
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(addInvoicePopover,0);
		afx.clear();
		invoiceNoField.clear();
		invoiceDateField.clear();
		dueDateField.clear();
		descriptionField.clear();
		amountField.clear();
		Platform.runLater(() -> afx.requestFocus());
	}

	public void closePopover(){
		contentDarken.setVisible(false);
		AnimationUtils.slideIn(addInvoicePopover,425);
	}

	public void addInvoice(){
		Boolean validEntry = true;
//		if(!afx.isValid()){afx.requestFocus();}
//		else if(!invoiceNoField.isValid()){invoiceNoField.requestFocus();}
//		else if(!dueDateField.isValid()){dueDateField.requestFocus();}
//		else if(!amountField.isValid()){amountField.requestFocus();}
//		else{
			InvoiceSupplier contact = (InvoiceSupplier) afx.getSelectedItem();
			String invoiceNo = invoiceNoField.getText();
			LocalDate invoiceDate = invoiceDateField.getValue();
			LocalDate dueDate = dueDateField.getValue();
			String description = descriptionField.getText();
			Double unitAmount = Double.valueOf(amountField.getText());
			String sql = "INSERT INTO invoices(supplierID,invoiceNo,invoiceDate,dueDate,description,unitAmount,storeID) VALUES(?,?,?,?,?,?,?)";
			try {
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setInt(1, contact.getContactID());
				preparedStatement.setString(2, invoiceNo);
				preparedStatement.setDate(3, Date.valueOf(invoiceDate));
				preparedStatement.setDate(4, Date.valueOf(dueDate));
				preparedStatement.setString(5, description);
				preparedStatement.setDouble(6, unitAmount);
				preparedStatement.setInt(7, main.getCurrentStore().getStoreID());
				preparedStatement.executeUpdate();
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
			}
			closePopover();
			fillTable();
			dialogPane.showInformation("Success","Payment was succesfully added");
//		}

	}

	public void fillTable(){

		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		ObservableList<Invoice> currentInvoiceDataPoints = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM invoices JOIN invoiceSuppliers i on invoices.supplierID = i.idinvoiceSuppliers WHERE invoices.storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
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
		filterView.getItems().setAll(currentInvoiceDataPoints);
//		addDoubleClickfunction();
		invoicesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		invoicesTable.setMaxWidth(Double.MAX_VALUE);
		invoicesTable.setMaxHeight(Double.MAX_VALUE);
		invoicesTable.setFixedCellSize(25.0);
		VBox.setVgrow(invoicesTable, Priority.ALWAYS);
		for(TableColumn tc: invoicesTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		Platform.runLater(() -> GUIUtils.customResize(invoicesTable,notesCol));
	}

	public DialogPane.Dialog<Object> getDialog() {
		return dialog;
	}
}



