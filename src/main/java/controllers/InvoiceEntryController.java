package controllers;

import application.Main;
import com.dlsc.gemsfx.FilterView;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.filter.StringFilter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.Invoice;
import models.Invoice;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;

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
	private MFXTableView<Invoice> invoicesTable = new MFXTableView<Invoice>();
	private MFXTableColumn<Invoice> supplierNameCol;
	private MFXTableColumn<Invoice> invoiceNoCol;
	private MFXTableColumn<Invoice> invoiceDateCol;
	private MFXTableColumn<Invoice> dueDateCol;
	private MFXTableColumn<Invoice> unitAmountCol;
	private MFXTableColumn<Invoice> importedInvoiceAmountCol;
	private MFXTableColumn<Invoice> varianceCol;
	private MFXTableColumn<Invoice> creditsCol;
	private MFXTableColumn<Invoice> totalAfterCreditCol;
	private MFXTableColumn<Invoice> notesCol;
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

		supplierNameCol = new MFXTableColumn<>("SUPPLIER",false, Comparator.comparing(Invoice::getSupplierName));
		invoiceNoCol = new MFXTableColumn<>("INVOICE NUMBER",false, Comparator.comparing(Invoice::getInvoiceNo));
		invoiceDateCol = new MFXTableColumn<>("INVOICE DATE",false, Comparator.comparing(Invoice::getInvoiceDate));
		dueDateCol = new MFXTableColumn<>("DUE DATE",false, Comparator.comparing(Invoice::getDueDate));
		unitAmountCol = new MFXTableColumn<>("UNIT AMOUNT",false, Comparator.comparing(Invoice::getUnitAmount));
		importedInvoiceAmountCol = new MFXTableColumn<>("IMPORTED AMOUNT",false, Comparator.comparing(Invoice::getImportedInvoiceAmount));
		varianceCol = new MFXTableColumn<>("VARIANCE",false, Comparator.comparing(Invoice::getVariance));
		creditsCol = new MFXTableColumn<>("CREDITS",false, Comparator.comparing(Invoice::getCredits));
		totalAfterCreditCol = new MFXTableColumn<>("TOTAL AFTER CREDIT",false, Comparator.comparing(Invoice::getTotalAfterCredits));
		notesCol = new MFXTableColumn<>("NOTES",false, Comparator.comparing(Invoice::getNotes));


		supplierNameCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getSupplierName));
		invoiceNoCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getInvoiceNo));
		invoiceDateCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getInvoiceDate));
		dueDateCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getDueDate));
		unitAmountCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getUnitAmount));
		importedInvoiceAmountCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getImportedInvoiceAmount));
		varianceCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getVariance));
		creditsCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getCredits));
		totalAfterCreditCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getTotalAfterCredits));
		notesCol.setRowCellFactory(invoice -> new MFXTableRowCell<>(Invoice::getNotes));

		invoicesTable.getTableColumns().addAll(
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

//		invoicesTable.getFilters().addAll(
//				new StringFilter<>("Invoicename",Invoice::getInvoicename),
//				new StringFilter<>("First Name",Invoice::getFirst_name),
//				new StringFilter<>("Last Name",Invoice::getLast_name),
//				new StringFilter<>("Role",Invoice::getRole)
//		);


		invoicesTable.setFooterVisible(true);
		invoicesTable.autosizeColumnsOnInitialization();
		invoicesTable.setMaxWidth(Double.MAX_VALUE);
		invoicesTable.setMaxHeight(Double.MAX_VALUE);
		filterView.setPadding(new Insets(20,20,10,20));//top,right,bottom,left

		controlBox.getChildren().addAll(filterView,invoicesTable);

		VBox.setVgrow(invoicesTable, Priority.ALWAYS);
		invoicesTable.setItems(allInvoices);


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

	
}
