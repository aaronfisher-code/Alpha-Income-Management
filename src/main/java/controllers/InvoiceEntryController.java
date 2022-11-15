package controllers;

import application.Main;
import com.dlsc.gemsfx.FilterView;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import models.Invoice;
import utils.AnimationUtils;
import utils.GUIUtils;
import utils.TableUtils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class InvoiceEntryController extends Controller{

	private MFXDatePicker datePkr;
	@FXML
	private FlowPane datePickerPane;
	@FXML
	private StackPane backgroundPane;
	@FXML
	private VBox controlBox;
	@FXML
	private BorderPane storesButton;
	@FXML
	private BorderPane invoicesButton;
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
	
	
    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private Invoice selectedInvoice;

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

		datePkr = new MFXDatePicker();
//		datePkr.setOnAction(e -> updatePage());
		datePickerPane.getChildren().add(1,datePkr);
		datePkr.setValue(LocalDate.now());
		datePkr.setText(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
		datePkr.getStylesheets().add("/views/CSS/RosterPage.css");

		filterView = new FilterView<>();
		filterView.setTitle("Current Invoices");
		filterView.setTextFilterProvider(text -> invoice -> invoice.getInvoiceNo().toLowerCase().contains(text) || invoice.getSupplierName().toLowerCase().contains(text));
		allInvoices = filterView.getFilteredItems();

		supplierNameCol = new TableColumn<>("SUPPLIER");
		invoiceNoCol = new TableColumn<>("INVOICE NUMBER");
		invoiceDateCol = new TableColumn<>("INVOICE DATE");
		dueDateCol = new TableColumn<>("DUE DATE");
		unitAmountCol = new TableColumn<>("UNIT AMOUNT");
		importedInvoiceAmountCol = new TableColumn<>("IMPORTED INVOICE AMOUNT");
		varianceCol = new TableColumn<>("VARIANCE");
		creditsCol = new TableColumn<>("CREDITS");
		totalAfterCreditCol = new TableColumn<>("TOTAL AFTER CREDIT");
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
		LocalDate date = LocalDate.now();
		Invoice test1 = new Invoice("Test","1234",date,date,"test",123,123.45,123.45,123.45,123.45,1234.45,"test");
		Invoice test2 = new Invoice("Test","5678",date,date,"test",123,123.45,123.45,123.45,123.45,1234.45,"test");
		Invoice test3 = new Invoice("Test","91011",date,date,"test",123,123.45,123.45,123.45,123.45,1234.45,"test");
		filterView.getItems().addAll(test1,test2,test3);
		invoicesTable.setFixedCellSize(25.0);
		VBox.setVgrow(invoicesTable, Priority.ALWAYS);
		invoicesTable.setItems(allInvoices);
		for(TableColumn tc: invoicesTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		Platform.runLater(() -> GUIUtils.customResize(invoicesTable));




	}

	public void exportFiles(){}

	public void importFiles(){}

	public void weekForward() {
		setDatePkr(datePkr.getValue().plusMonths(1));
	}

	public void weekBackward() {
		setDatePkr(datePkr.getValue().minusMonths(1));
	}

	public void setDatePkr(LocalDate date) {
		datePkr.setValue(date);
	}

	public void openPopover(){
//		saveButton.setOnAction(actionEvent -> addPayment());
//		paymentPopoverTitle.setText("Add new account payment");
//		deleteButton.setVisible(false);
//		contentDarken.setVisible(true);
//		AnimationUtils.changeSize(addPaymentPopover,0);
//		afx.clear();
//		invoiceNoField.clear();
//		invoiceDateField.clear();
//		dueDateField.clear();
//		descriptionField.clear();
//		amountField.clear();
//		accountAdjustedBox.setSelected(false);
//		Platform.runLater(() -> afx.requestFocus());
	}
}



