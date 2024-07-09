package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import models.*;
import org.controlsfx.control.PopOver;
import utils.GUIUtils;
import utils.RosterUtils;
import utils.TableUtils;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MonthlySummaryController extends DateSelectController{

	@FXML
	private StackPane monthSelector;
	@FXML
	private MFXTextField monthSelectorField;
    @FXML
	private TableView<MonthlySummaryDataPoint> summaryTable;
	@FXML
	private TableColumn<?, ?> customersCol;
	@FXML
	private TableColumn<?, ?> dateCol;
	@FXML
	private TableColumn<?, ?> dollarPerCustomerCol;
	@FXML
	private TableColumn<?, ?> durationCol;
	@FXML
	private TableColumn<?, ?> gpDollarCol;
	@FXML
	private TableColumn<?, ?> gpPercentCol;
	@FXML
	private TableColumn<?, ?> itemsCol;
	@FXML
	private TableColumn<?, ?> itemsPerCustomerCol;
	@FXML
	private TableColumn<?, ?> otcDollarPerCustomerCol;
	@FXML
	private TableColumn<?, ?> otcItemsCol;
	@FXML
	private TableColumn<?, ?> otcPerCustomerCol;
	@FXML
	private TableColumn<?, ?> rentAndOutgoingsCol;
	@FXML
	private TableColumn<?, ?> runningZProfitCol;
	@FXML
	private TableColumn<?, ?> scriptsCol;
	@FXML
	private TableColumn<?, ?> tillBalanceCol;
	@FXML
	private TableColumn<?, ?> totalIncomeCol;
	@FXML
	private TableColumn<?, ?> wagesCol;
	@FXML
	private TableColumn<?, ?> zReportProfitCol;
	@FXML
	private TableColumn<MonthlySummaryDataPoint,String> runningTillBalanceCol;

	@FXML
	private TableView<MonthlySummaryDataPoint> totalsTable;
	@FXML
	private TableColumn<?, ?> totalsDateCol;
	@FXML
	private TableColumn<?, ?> totalsCustomersCol;
	@FXML
	private TableColumn<?, ?> totalsDollarPerCustomerCol;
	@FXML
	private TableColumn<?, ?> totalsDurationCol;
	@FXML
	private TableColumn<?, ?> totalsGpDollarCol;
	@FXML
	private TableColumn<?, ?> totalsGpPercentCol;
	@FXML
	private TableColumn<?, ?> totalsItemsCol;
	@FXML
	private TableColumn<?, ?> totalsItemsPerCustomerCol;
	@FXML
	private TableColumn<?, ?> totalsOtcDollarPerCustomerCol;
	@FXML
	private TableColumn<?, ?> totalsOtcItemsCol;
	@FXML
	private TableColumn<?, ?> totalsOtcPerCustomerCol;
	@FXML
	private TableColumn<?, ?> totalsRentAndOutgoingsCol;
	@FXML
	private TableColumn<?, ?> totalsRunningZProfitCol;
	@FXML
	private TableColumn<?, ?> totalsScriptsCol;
	@FXML
	private TableColumn<?, ?> totalsTillBalanceCol;
	@FXML
	private TableColumn<?, ?> totalsTotalIncomeCol;
	@FXML
	private TableColumn<?, ?> totalsWagesCol;
	@FXML
	private TableColumn<?, ?> totalsZReportProfitCol;
	@FXML
	private TableColumn<MonthlySummaryDataPoint,String> totalsRunningTillBalanceCol;



	private PopOver currentDatePopover;

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;

    private final ObservableList<TillReportDataPoint> currentTillReportDataPoints = FXCollections.observableArrayList();
	private final ObservableList<EODDataPoint> currentEODDataPoints = FXCollections.observableArrayList();
	private YearMonth yearMonthObject;
	private int daysInMonth;
	RosterUtils rosterUtils = null;

	
	 @FXML
	private void initialize() {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	@Override
	public void fill() {
//		itemsPerCustomerCol.getStyleClass().add("yellowColumn");
		summaryTable.setSelectionModel(null);
		dateCol.setCellValueFactory(new PropertyValueFactory<>("dateString"));
		//Add center alignment to date column
		
		durationCol.setCellValueFactory(new PropertyValueFactory<>("dayDuration"));
		customersCol.setCellValueFactory(new PropertyValueFactory<>("noOfCustomersString"));
		itemsCol.setCellValueFactory(new PropertyValueFactory<>("noOfItemsString"));
		scriptsCol.setCellValueFactory(new PropertyValueFactory<>("noOfScriptsString"));
		dollarPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("dollarPerCustomerString"));
		itemsPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("itemsPerCustomerString"));
		otcDollarPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("otcDollarPerCustomerString"));
		otcItemsCol.setCellValueFactory(new PropertyValueFactory<>("noOfOTCItemsString"));
		otcPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("otcPerCustomerString"));
		totalIncomeCol.setCellValueFactory(new PropertyValueFactory<>("totalIncomeString"));
		gpDollarCol.setCellValueFactory(new PropertyValueFactory<>("gpDollarsString"));
		gpPercentCol.setCellValueFactory(new PropertyValueFactory<>("gpPercentageString"));
		wagesCol.setCellValueFactory(new PropertyValueFactory<>("wagesString"));
		rentAndOutgoingsCol.setCellValueFactory(new PropertyValueFactory<>("rentAndOutgoingsString"));
		runningZProfitCol.setCellValueFactory(new PropertyValueFactory<>("runningZProfitString"));
		zReportProfitCol.setCellValueFactory(new PropertyValueFactory<>("zReportProfitString"));
		tillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("tillBalanceString"));
		runningTillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("runningTillBalanceString"));

		summaryTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		summaryTable.setFixedCellSize(25.0);
		VBox.setVgrow(summaryTable, Priority.ALWAYS);
		for(TableColumn<?, ?> tc: summaryTable.getColumns()){
			if(tc.getGraphic()!=null){
				Label l = (Label) tc.getGraphic();
				tc.setPrefWidth(TableUtils.getColumnWidth(l)+30);
				l.setMinWidth(TableUtils.getColumnWidth(l)+30);
			}else{
				tc.setPrefWidth(TableUtils.getColumnWidth(tc)+50);
			}

		}

		totalsDateCol.setCellValueFactory(new PropertyValueFactory<>("dateValue"));
		totalsDurationCol.setCellValueFactory(new PropertyValueFactory<>("dateDurationValue"));
		totalsCustomersCol.setCellValueFactory(new PropertyValueFactory<>("noOfCustomersValue"));
		totalsItemsCol.setCellValueFactory(new PropertyValueFactory<>("noOfItemsValue"));
		totalsScriptsCol.setCellValueFactory(new PropertyValueFactory<>("noOfScriptsValue"));
		totalsDollarPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("dollarPerCustomerValue"));
		totalsItemsPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("itemsPerCustomerValue"));
		totalsOtcDollarPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("otcDollarPerCustomerValue"));
		totalsOtcItemsCol.setCellValueFactory(new PropertyValueFactory<>("noOfOTCItemsValue"));
		totalsOtcPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("otcPerCustomerValue"));
		totalsTotalIncomeCol.setCellValueFactory(new PropertyValueFactory<>("totalIncomeValue"));
		totalsGpDollarCol.setCellValueFactory(new PropertyValueFactory<>("gpDollarsValue"));
		totalsGpPercentCol.setCellValueFactory(new PropertyValueFactory<>("gpPercentageValue"));
		totalsWagesCol.setCellValueFactory(new PropertyValueFactory<>("wagesValue"));
		totalsRentAndOutgoingsCol.setCellValueFactory(new PropertyValueFactory<>("rentAndOutgoingsValue"));
		totalsRunningZProfitCol.setCellValueFactory(new PropertyValueFactory<>("runningZProfitValue"));
		totalsZReportProfitCol.setCellValueFactory(new PropertyValueFactory<>("zReportProfitValue"));
		totalsTillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("tillBalanceValue"));
		totalsRunningTillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("runningTillBalanceValue"));

		totalsDateCol.prefWidthProperty().bind(dateCol.widthProperty());
		totalsDurationCol.prefWidthProperty().bind(durationCol.widthProperty());
		totalsCustomersCol.prefWidthProperty().bind(customersCol.widthProperty());
		totalsItemsCol.prefWidthProperty().bind(itemsCol.widthProperty());
		totalsScriptsCol.prefWidthProperty().bind(scriptsCol.widthProperty());
		totalsDollarPerCustomerCol.prefWidthProperty().bind(dollarPerCustomerCol.widthProperty());
		totalsItemsPerCustomerCol.prefWidthProperty().bind(itemsPerCustomerCol.widthProperty());
		totalsOtcDollarPerCustomerCol.prefWidthProperty().bind(otcDollarPerCustomerCol.widthProperty());
		totalsOtcItemsCol.prefWidthProperty().bind(otcItemsCol.widthProperty());
		totalsOtcPerCustomerCol.prefWidthProperty().bind(otcPerCustomerCol.widthProperty());
		totalsTotalIncomeCol.prefWidthProperty().bind(totalIncomeCol.widthProperty());
		totalsGpDollarCol.prefWidthProperty().bind(gpDollarCol.widthProperty());
		totalsGpPercentCol.prefWidthProperty().bind(gpPercentCol.widthProperty());
		totalsWagesCol.prefWidthProperty().bind(wagesCol.widthProperty());
		totalsRentAndOutgoingsCol.prefWidthProperty().bind(rentAndOutgoingsCol.widthProperty());
		totalsRunningZProfitCol.prefWidthProperty().bind(runningZProfitCol.widthProperty());
		totalsZReportProfitCol.prefWidthProperty().bind(zReportProfitCol.widthProperty());
		totalsTillBalanceCol.prefWidthProperty().bind(tillBalanceCol.widthProperty());
		totalsRunningTillBalanceCol.prefWidthProperty().bind(runningTillBalanceCol.widthProperty());
		totalsTable.setFixedCellSize(25.0);
		totalsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

		Platform.runLater(() -> {
			ScrollBar mainTableScrollBar = getHorizontalScrollbar(summaryTable);
			ScrollBar summaryTableScrollBar = getHorizontalScrollbar(totalsTable);

			if (mainTableScrollBar != null && summaryTableScrollBar != null) {
				mainTableScrollBar.valueProperty().bindBidirectional(summaryTableScrollBar.valueProperty());
			}
		});

//		Platform.runLater(() -> GUIUtils.customResize(summaryTable,runningTillBalanceCol,(Label) runningTillBalanceCol.getGraphic()));
		setDate(main.getCurrentDate());

	}

	private ScrollBar getHorizontalScrollbar(TableView<?> table) {
		ScrollBar result = null;

		for (Node node : table.lookupAll(".scroll-bar")) {
			if (node instanceof ScrollBar bar) {
                if (bar.getOrientation().equals(Orientation.HORIZONTAL)) {
					result = bar;
				}
			}
		}

		return result;
	}

	public void monthForward() {setDate(main.getCurrentDate().plusMonths(1));
	}

	public void monthBackward() {
		setDate(main.getCurrentDate().minusMonths(1));
	}

	public void fillTable(){
        ObservableList<MonthlySummaryDataPoint> monthlySummaryPoints = FXCollections.observableArrayList();
		yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		daysInMonth = yearMonthObject.lengthOfMonth();
		rosterUtils = new RosterUtils(con,main,yearMonthObject);
		double monthlyRent = 0;
		double dailyOutgoings = 0;
		double monthlyWages = 0;
		String sql;
		try {
			sql = "SELECT * FROM tillReportDatapoints where storeID = ? AND MONTH(assignedDate) = ? AND YEAR(assignedDate) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentTillReportDataPoints.add(new TillReportDataPoint(resultSet));
			}

			sql = "SELECT * FROM eodDataPoints where storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentEODDataPoints.add(new EODDataPoint(resultSet));
			}
			sql = "SELECT * FROM budgetandexpenses WHERE storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, main.getCurrentDate().getMonthValue());
			preparedStatement.setInt(3, main.getCurrentDate().getYear());
			resultSet = preparedStatement.executeQuery();
			if (resultSet == null || !resultSet.next()) {
				monthlyRent = 0;
				dailyOutgoings = 0;
				monthlyWages = 0;
			}else{
				monthlyRent = resultSet.getDouble("monthlyRent");
				dailyOutgoings = resultSet.getDouble("dailyOutgoings");
				monthlyWages = resultSet.getDouble("monthlyWages");
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}


		double totalOpenDuration = rosterUtils.getOpenDuration();
		for(int i = 1; i<daysInMonth+1; i++){
			LocalDate d = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(),i);
			monthlySummaryPoints.add(new MonthlySummaryDataPoint(d,currentTillReportDataPoints,currentEODDataPoints, monthlySummaryPoints,rosterUtils,monthlyRent,dailyOutgoings,totalOpenDuration,monthlyWages));
		}

		summaryTable.setItems(monthlySummaryPoints);
		totalsTable.getItems().clear();
		totalsTable.getItems().add(new MonthlySummaryDataPoint(monthlySummaryPoints, true, rosterUtils.getOpenDays()));
		totalsTable.getItems().add(new MonthlySummaryDataPoint(monthlySummaryPoints, false, rosterUtils.getOpenDays()));
		Platform.runLater(() -> GUIUtils.customResize(summaryTable,runningTillBalanceCol,(Label) runningTillBalanceCol.getGraphic()));
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

	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		String fieldText = main.getCurrentDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		fieldText += ", ";
		fieldText += main.getCurrentDate().getYear();
		monthSelectorField.setText(fieldText);
		fillTable();
	}

	public void exportData(){
		String sql;
		BASCheckerDataPoint currentBASCheckerDataPoint = null;
		try{
			sql = "SELECT * FROM baschecker WHERE storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, main.getCurrentDate().getMonthValue());
			preparedStatement.setInt(3, main.getCurrentDate().getYear());
			resultSet = preparedStatement.executeQuery();
			if (resultSet == null || !resultSet.next()) {
            }else{
				currentBASCheckerDataPoint = new BASCheckerDataPoint(resultSet);
			}
			double medicareTotal = 0;
			for(int i=1;i<daysInMonth+1;i++) {
				LocalDate d = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(), i);
				boolean foundMedicare = false;
				for (TillReportDataPoint tdp : currentTillReportDataPoints) {
					if (tdp.getAssignedDate().equals(d) && tdp.getKey().equals("Govt Recovery")) {
						medicareTotal += tdp.getAmount();
						foundMedicare = true;
					}
					if(foundMedicare){
						break;
					}
				}
			}
			double cpaIncome;
			double lanternPayIncome;
			double otherIncome;
			double basRefund;
			double monthlyLoan;
			sql = "SELECT * FROM budgetandexpenses WHERE storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, main.getCurrentDate().getMonthValue());
			preparedStatement.setInt(3, main.getCurrentDate().getYear());
			resultSet = preparedStatement.executeQuery();
			if (resultSet == null || !resultSet.next()) {
				cpaIncome = 0;
				lanternPayIncome = 0;
				otherIncome = 0;
				basRefund = 0;
				monthlyLoan = 0;
			}else{
				cpaIncome = resultSet.getDouble("6CPAIncome");
				lanternPayIncome = resultSet.getDouble("LanternPayIncome");
				otherIncome = resultSet.getDouble("OtherIncome");
				basRefund = resultSet.getDouble("ATO_GST_BAS_refund");
				monthlyLoan = resultSet.getDouble("monthlyLoan");
			}
			ObservableList<AccountPayment> currentAccountPaymentDataPoints = FXCollections.observableArrayList();
			sql = "SELECT * FROM accountPayments JOIN accountPaymentContacts a on a.idaccountPaymentContacts = accountPayments.contactID WHERE accountPayments.storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentAccountPaymentDataPoints.add(new AccountPayment(resultSet));
			}
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
					AccountPaymentContactDataPoint acdp = getContactfromName(a.getContactName());
					acdp.setTotalValue(a.getUnitAmount());
					currentContactTotals.add(acdp);
				}
			}
			ObservableList<Invoice> currentInvoices = FXCollections.observableArrayList();
			sql = "SELECT * FROM invoices JOIN invoicesuppliers a on a.idinvoiceSuppliers = invoices.supplierID JOIN invoicedatapoints i on invoices.invoiceNo = i.invoiceNo WHERE invoices.storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentInvoices.add(new Invoice(resultSet));
			}
			double totalCOGS = 0;
			for(Invoice i:currentInvoices){
				totalCOGS+=i.getUnitAmount();
			}


			StringBuilder outString = new StringBuilder();
//			outString.append("KPI\tTotal Turnover\tAverage Turnover\tTotal GP ($)\tAverage GP ($)\tAverage GP (%)\tActual T/over (incl other income)\tActual Average T/over (incl other income)\tActual GP ($) (incl other income)\tActual Average GP (incl other income)\tActual GP (%) (incl other income)\tTotal Customer #\tAverage Customer #\tTotal Script #\tAverage Script #\t$ / customer\tItems / customer\tOTC $ / Customer\tOTC items / Customer\tStock hold @ end of month\t# scripts on file\t# sms patients\tTotal Expenses\tAverage Expenses\t% wages\t% outgoings\t6CPA Income\tLatern Pay Income\tPharmaPrograms\tOther Income\tBAS - GST refund\tTotal Profit (Excl Loan+ GST refund)\tTotal Profit (Incl Loan + GST refund)\tCOGS from invoices (Match Xero)\tGP (banked income - Invoiced COGs + Stock not sold)\tProfit (as per Xero)\r\n");
			//Empty first cell
			outString.append("\t");
			//Total Turnover
			double totalIncome = Double.parseDouble(totalsTable.getItems().get(0).getTotalIncomeValue().replace("$", "").replace(",", ""));
			double medicareBAS = 0;
			if(currentBASCheckerDataPoint!=null){
				medicareBAS = currentBASCheckerDataPoint.getBasDailyScript();
			}
			double medicareSpreadsheet = medicareTotal;
			double medicareIncome = medicareBAS - medicareSpreadsheet;
			outString.append(NumberFormat.getCurrencyInstance().format(totalIncome+medicareIncome)).append("\t");
			//Average Turnover
			outString.append(NumberFormat.getCurrencyInstance().format((totalIncome+medicareIncome)/rosterUtils.getOpenDays())).append("\t");
			//Total GP ($)
			double totalGP = Double.parseDouble(totalsTable.getItems().get(0).getGpDollarsValue().replace("$", "").replace(",", ""));
			outString.append(NumberFormat.getCurrencyInstance().format(totalGP+medicareIncome)).append("\t");
			//Average GP ($)
			outString.append(NumberFormat.getCurrencyInstance().format((totalGP+medicareIncome)/rosterUtils.getOpenDays())).append("\t");
			//Average GP (%)
			outString.append(totalsTable.getItems().get(1).getGpPercentageValue()).append("\t");
			//Actual T/over (incl other income)
			List<AccountPaymentContactDataPoint> filteredList = currentContactTotals.stream()
					.filter(a -> a.getContactName().equals("PharmaPrograms"))
					.toList();
			double pharmaProgramsIncome = 0.0;
			if (!filteredList.isEmpty()) {
				pharmaProgramsIncome = filteredList.get(0).getTotalValue();
			}
			outString.append(NumberFormat.getCurrencyInstance().format(totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)).append("\t");
			//Actual Average T/over (incl other income)
			outString.append(NumberFormat.getCurrencyInstance().format((totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)/rosterUtils.getOpenDays())).append("\t");
			//Actual GP ($) (incl other income)
			outString.append(NumberFormat.getCurrencyInstance().format((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)).append("\t");
			//Actual Average GP (incl other income)
			outString.append(NumberFormat.getCurrencyInstance().format(((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)/rosterUtils.getOpenDays())).append("\t");
			//Actual GP (%) (incl other income)
			outString.append(String.format("%.2f", 100*((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)/(totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)) + "%").append("\t");
			//Total Customer #
			outString.append(totalsTable.getItems().get(0).getNoOfCustomersValue()).append("\t");
			//Average Customer #
			outString.append(totalsTable.getItems().get(1).getNoOfCustomersValue()).append("\t");
			//Total Script #
			outString.append(totalsTable.getItems().get(0).getNoOfScriptsValue()).append("\t");
			//Average Script #
			outString.append(totalsTable.getItems().get(1).getNoOfScriptsValue()).append("\t");
			//$/customer
			outString.append(totalsTable.getItems().get(1).getDollarPerCustomerValue()).append("\t");
			//Items/customer
			outString.append(totalsTable.getItems().get(1).getItemsPerCustomerValue()).append("\t");
			//OTC $/Customer
			outString.append(totalsTable.getItems().get(1).getOtcDollarPerCustomerValue()).append("\t");
			//OTC items/Customer
			outString.append(totalsTable.getItems().get(1).getOtcPerCustomerValue()).append("\t");
			//Stock on hand @ end of month
			double initialStockOnHand = currentEODDataPoints.get(0).getStockOnHandAmount();
			double endStockOnHand = 0;
			double scriptsOnFile = 0;
			double smsPatients = 0;
			for(EODDataPoint eod:currentEODDataPoints){
				if(eod.getStockOnHandAmount()!=0){
					endStockOnHand = eod.getStockOnHandAmount();
				}
				if(eod.getScriptsOnFile()!=0){
					scriptsOnFile = eod.getScriptsOnFile();
				}
				if(eod.getSmsPatients()!=0){
					smsPatients = eod.getSmsPatients();
				}
			}
			outString.append(endStockOnHand).append("\t");
			//Scripts on file
			outString.append(scriptsOnFile).append("\t");
			//SMS patients
			outString.append(smsPatients).append("\t");
			//Total Expenses
			double totalExpenses = Double.parseDouble(totalsTable.getItems().get(0).getRentAndOutgoingsValue().replace("$", "").replace(",", ""))+Double.parseDouble(totalsTable.getItems().get(0).getWagesValue().replace("$", "").replace(",", ""));
			outString.append(NumberFormat.getCurrencyInstance().format(totalExpenses)).append("\t");
			//Average Expenses
			outString.append(NumberFormat.getCurrencyInstance().format(totalExpenses/rosterUtils.getOpenDays())).append("\t");
			//% wages
			outString.append(String.format("%.2f", 100*Double.parseDouble(totalsTable.getItems().get(0).getWagesValue().replace("$", "").replace(",", ""))/totalIncome)+"%").append("\t");
			//% outgoings
			outString.append(String.format("%.2f", 100*Double.parseDouble(totalsTable.getItems().get(0).getRentAndOutgoingsValue().replace("$", "").replace(",", ""))/totalIncome)+"%").append("\t");
			//6CPA Income
			outString.append(NumberFormat.getCurrencyInstance().format(cpaIncome)).append("\t");
			//Latern Pay Income
			outString.append(NumberFormat.getCurrencyInstance().format(lanternPayIncome)).append("\t");
			//Pharma Programs
			outString.append(NumberFormat.getCurrencyInstance().format(pharmaProgramsIncome)).append("\t");
			//Other Income
			outString.append(NumberFormat.getCurrencyInstance().format(otherIncome)).append("\t");
			//BAS - GST refund
			outString.append(NumberFormat.getCurrencyInstance().format(basRefund)).append("\t");
			//Total Profit (Excl Loan+ GST refund)
			outString.append(NumberFormat.getCurrencyInstance().format((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome-totalExpenses)).append("\t");
			//Total Profit (Incl Loan + GST refund)
			outString.append(NumberFormat.getCurrencyInstance().format((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome-totalExpenses-monthlyLoan+basRefund)).append("\t");
			//COGS from invoices (Match Xero)
			outString.append(NumberFormat.getCurrencyInstance().format(totalCOGS)).append("\t");
			//GP (banked income - Invoiced COGs + Stock not sold)
			outString.append(NumberFormat.getCurrencyInstance().format((totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome-totalCOGS)+(endStockOnHand-initialStockOnHand))).append("\t");
			//Profit (as per Xero)
			outString.append(NumberFormat.getCurrencyInstance().format((totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome-totalCOGS)+(endStockOnHand-initialStockOnHand)-totalExpenses)).append("\r\n");

			ClipboardContent content = new ClipboardContent();
			content.putString(outString.toString());
			Clipboard.getSystemClipboard().setContent(content);
			JOptionPane.showMessageDialog(null, "Data copied to clipboard!");

		} catch (SQLException e) {
		throw new RuntimeException(e);
		}
	}

	public AccountPaymentContactDataPoint getContactfromName(String name){
		String sql;
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
}



