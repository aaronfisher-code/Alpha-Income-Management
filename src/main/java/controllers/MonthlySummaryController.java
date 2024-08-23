package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import models.*;
import services.*;
import utils.GUIUtils;
import utils.RosterUtils;
import utils.TableUtils;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

public class MonthlySummaryController extends DateSelectController{

    @FXML private TableView<MonthlySummaryDataPoint> summaryTable;
	@FXML private TableColumn<?, ?> customersCol;
	@FXML private TableColumn<?, ?> dateCol;
	@FXML private TableColumn<?, ?> dollarPerCustomerCol;
	@FXML private TableColumn<?, ?> durationCol;
	@FXML private TableColumn<?, ?> gpDollarCol;
	@FXML private TableColumn<?, ?> gpPercentCol;
	@FXML private TableColumn<?, ?> itemsCol;
	@FXML private TableColumn<?, ?> itemsPerCustomerCol;
	@FXML private TableColumn<?, ?> otcDollarPerCustomerCol;
	@FXML private TableColumn<?, ?> otcItemsCol;
	@FXML private TableColumn<?, ?> otcPerCustomerCol;
	@FXML private TableColumn<?, ?> rentAndOutgoingsCol;
	@FXML private TableColumn<?, ?> runningZProfitCol;
	@FXML private TableColumn<?, ?> scriptsCol;
	@FXML private TableColumn<?, ?> tillBalanceCol;
	@FXML private TableColumn<?, ?> totalIncomeCol;
	@FXML private TableColumn<?, ?> wagesCol;
	@FXML private TableColumn<?, ?> zReportProfitCol;
	@FXML private TableColumn<MonthlySummaryDataPoint,String> runningTillBalanceCol;
	@FXML private TableView<MonthlySummaryDataPoint> totalsTable;
	@FXML private TableColumn<?, ?> totalsDateCol;
	@FXML private TableColumn<?, ?> totalsCustomersCol;
	@FXML private TableColumn<?, ?> totalsDollarPerCustomerCol;
	@FXML private TableColumn<?, ?> totalsDurationCol;
	@FXML private TableColumn<?, ?> totalsGpDollarCol;
	@FXML private TableColumn<?, ?> totalsGpPercentCol;
	@FXML private TableColumn<?, ?> totalsItemsCol;
	@FXML private TableColumn<?, ?> totalsItemsPerCustomerCol;
	@FXML private TableColumn<?, ?> totalsOtcDollarPerCustomerCol;
	@FXML private TableColumn<?, ?> totalsOtcItemsCol;
	@FXML private TableColumn<?, ?> totalsOtcPerCustomerCol;
	@FXML private TableColumn<?, ?> totalsRentAndOutgoingsCol;
	@FXML private TableColumn<?, ?> totalsRunningZProfitCol;
	@FXML private TableColumn<?, ?> totalsScriptsCol;
	@FXML private TableColumn<?, ?> totalsTillBalanceCol;
	@FXML private TableColumn<?, ?> totalsTotalIncomeCol;
	@FXML private TableColumn<?, ?> totalsWagesCol;
	@FXML private TableColumn<?, ?> totalsZReportProfitCol;
	@FXML private TableColumn<MonthlySummaryDataPoint,String> totalsRunningTillBalanceCol;
	@FXML private Button exportDataButton;
    private final ObservableList<TillReportDataPoint> currentTillReportDataPoints = FXCollections.observableArrayList();
	private final ObservableList<EODDataPoint> currentEODDataPoints = FXCollections.observableArrayList();
	private YearMonth yearMonthObject;
	private int daysInMonth;
	RosterUtils rosterUtils;
	EODService eodService;
	TillReportService tillReportService;
	BudgetExpensesService budgetExpensesService;
	BASCheckerService basCheckerService;
	AccountPaymentService accountPaymentService;
	InvoiceService invoiceService;
	AccountPaymentContactService accountPaymentContactService;

	@FXML
	private void initialize() {
		eodService = new EODService();
		tillReportService = new TillReportService();
		budgetExpensesService = new BudgetExpensesService();
		basCheckerService = new BASCheckerService();
		accountPaymentService = new AccountPaymentService();
		invoiceService = new InvoiceService();
		accountPaymentContactService = new AccountPaymentContactService();
	 }

	@Override
	public void fill() {
        exportDataButton.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Monthly Summary - Export")));
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
		TableUtils.resizeTableColumns(summaryTable,runningTillBalanceCol);
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

	public void fillTable(){
        ObservableList<MonthlySummaryDataPoint> monthlySummaryPoints = FXCollections.observableArrayList();
		yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		daysInMonth = yearMonthObject.lengthOfMonth();
		LocalDate startDate = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(), 1);
		LocalDate endDate = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(), daysInMonth);
		try{
			rosterUtils = new RosterUtils(main,yearMonthObject);
		}catch (SQLException e){
			dialogPane.showError("Error","Error loading roster data",e);
		}
		double monthlyRent = 0;
		double dailyOutgoings = 0;
		double monthlyWages = 0;
		try {
			currentTillReportDataPoints.setAll(tillReportService.getTillReportDataPoints(main.getCurrentStore().getStoreID(), startDate, endDate));
			currentEODDataPoints.setAll(eodService.getEODDataPoints(main.getCurrentStore().getStoreID(), startDate, endDate));
			BudgetAndExpensesDataPoint currentBudgetAndExpensesDataPoint = budgetExpensesService.getBudgetExpensesData(main.getCurrentStore().getStoreID(), yearMonthObject);
			if(currentBudgetAndExpensesDataPoint!=null){
				monthlyRent = currentBudgetAndExpensesDataPoint.getMonthlyRent();
				dailyOutgoings = currentBudgetAndExpensesDataPoint.getDailyOutgoings();
				monthlyWages = currentBudgetAndExpensesDataPoint.getMonthlyWages();
			}
		} catch (SQLException ex) {
			dialogPane.showError("Error","Error loading data",ex);
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

	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		updateMonthSelectorField();
		fillTable();
	}

	public void exportData(){
        BASCheckerDataPoint currentBASCheckerDataPoint;
		try{
			currentBASCheckerDataPoint = basCheckerService.getBASData(main.getCurrentStore().getStoreID(), yearMonthObject);
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
			double cpaIncome=0;
			double lanternPayIncome=0;
			double otherIncome=0;
			double basRefund=0;
			double monthlyLoan=0;
			BudgetAndExpensesDataPoint currentBudgetAndExpensesDataPoint = budgetExpensesService.getBudgetExpensesData(main.getCurrentStore().getStoreID(), yearMonthObject);
			if(currentBudgetAndExpensesDataPoint!=null) {
				cpaIncome = currentBudgetAndExpensesDataPoint.getCpaIncome();
				lanternPayIncome = currentBudgetAndExpensesDataPoint.getLanternIncome();
				otherIncome = currentBudgetAndExpensesDataPoint.getOtherIncome();
				basRefund = currentBudgetAndExpensesDataPoint.getAtoGSTrefund();
				monthlyLoan = currentBudgetAndExpensesDataPoint.getMonthlyLoan();
			}
			ObservableList<AccountPayment> currentAccountPaymentDataPoints = FXCollections.observableArrayList(accountPaymentService.getAccountPaymentsForMonth(main.getCurrentStore().getStoreID(), yearMonthObject));
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
					AccountPaymentContactDataPoint acdp = accountPaymentContactService.getContactByName(a.getContactName(), main.getCurrentStore().getStoreID());
					acdp.setTotalValue(a.getUnitAmount());
					currentContactTotals.add(acdp);
				}
			}
			ObservableList<Invoice> currentInvoices = FXCollections.observableArrayList(invoiceService.getAllInvoices(main.getCurrentStore().getStoreID(), yearMonthObject));
			double totalCOGS = 0;
			for(Invoice i:currentInvoices){
				totalCOGS+=i.getUnitAmount();
			}
			StringBuilder outString = new StringBuilder();
//			outString.append("KPI\tTotal Turnover\tAverage Turnover\tTotal GP ($)\tAverage GP ($)\tAverage GP (%)\tActual T/over (incl other income)\tActual Average T/over (incl other income)\tActual GP ($) (incl other income)\tActual Average GP (incl other income)\tActual GP (%) (incl other income)\tTotal Customer #\tAverage Customer #\tTotal Script #\tAverage Script #\t$ / customer\tItems / customer\tOTC $ / Customer\tOTC items / Customer\tStock hold @ end of month\t# scripts on file\t# sms patients\tTotal Expenses\tAverage Expenses\t% wages\t% outgoings\t6CPA Income\tLatern Pay Income\tPharmaPrograms\tOther Income\tBAS - GST refund\tTotal Profit (Excl Loan+ GST refund)\tTotal Profit (Incl Loan + GST refund)\tCOGS from invoices (Match Xero)\tGP (banked income - Invoiced COGs + Stock not sold)\tProfit (as per Xero)\r\n");
			//Empty first cell
			outString.append("\t");
			//Total Turnover
			double totalIncome = Double.parseDouble(totalsTable.getItems().getFirst().getTotalIncomeValue().replace("$", "").replace(",", ""));
			double medicareBAS = 0;
			if(currentBASCheckerDataPoint!=null){
				medicareBAS = currentBASCheckerDataPoint.getBasDailyScript();
			}
			double medicareSpreadsheet = medicareTotal;
			double medicareIncome = medicareBAS - medicareSpreadsheet;
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(totalIncome+medicareIncome)).append("\t");
			//Average Turnover
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format((totalIncome+medicareIncome)/rosterUtils.getOpenDays())).append("\t");
			//Total GP ($)
			double totalGP = Double.parseDouble(totalsTable.getItems().getFirst().getGpDollarsValue().replace("$", "").replace(",", ""));
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(totalGP+medicareIncome)).append("\t");
			//Average GP ($)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format((totalGP+medicareIncome)/rosterUtils.getOpenDays())).append("\t");
			//Average GP (%)
			outString.append(totalsTable.getItems().get(1).getGpPercentageValue()).append("\t");
			//Actual T/over (incl other income)
			List<AccountPaymentContactDataPoint> filteredList = currentContactTotals.stream()
					.filter(a -> a.getContactName().equals("PharmaPrograms"))
					.toList();
			double pharmaProgramsIncome = 0.0;
			if (!filteredList.isEmpty()) {
				pharmaProgramsIncome = filteredList.getFirst().getTotalValue();
			}
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)).append("\t");
			//Actual Average T/over (incl other income)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format((totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)/rosterUtils.getOpenDays())).append("\t");
			//Actual GP ($) (incl other income)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)).append("\t");
			//Actual Average GP (incl other income)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome)/rosterUtils.getOpenDays())).append("\t");
			//Actual GP (%) (incl other income)
			outString.append(String.format("%.2f", 100 * ((totalGP + medicareIncome) + cpaIncome + lanternPayIncome + pharmaProgramsIncome + otherIncome) / (totalIncome + medicareIncome + cpaIncome + lanternPayIncome + pharmaProgramsIncome + otherIncome))).append("%").append("\t");
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
			double initialStockOnHand = currentEODDataPoints.getFirst().getStockOnHandAmount();
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
			double totalExpenses = Double.parseDouble(totalsTable.getItems().getFirst().getRentAndOutgoingsValue().replace("$", "").replace(",", ""))+Double.parseDouble(totalsTable.getItems().getFirst().getWagesValue().replace("$", "").replace(",", ""));
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(totalExpenses)).append("\t");
			//Average Expenses
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(totalExpenses/rosterUtils.getOpenDays())).append("\t");
			//% wages
			outString.append(String.format("%.2f", 100 * Double.parseDouble(totalsTable.getItems().getFirst().getWagesValue().replace("$", "").replace(",", "")) / totalIncome)).append("%").append("\t");
			//% outgoings
			outString.append(String.format("%.2f", 100 * Double.parseDouble(totalsTable.getItems().getFirst().getRentAndOutgoingsValue().replace("$", "").replace(",", "")) / totalIncome)).append("%").append("\t");
			//6CPA Income
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(cpaIncome)).append("\t");
			//Lantern Pay Income
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(lanternPayIncome)).append("\t");
			//Pharma Programs
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(pharmaProgramsIncome)).append("\t");
			//Other Income
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(otherIncome)).append("\t");
			//BAS - GST refund
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(basRefund)).append("\t");
			//Total Profit (Excl Loan+ GST refund)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome-totalExpenses)).append("\t");
			//Total Profit (Incl Loan + GST refund)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format((totalGP+medicareIncome)+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome-totalExpenses-monthlyLoan+basRefund)).append("\t");
			//COGS from invoices (Match Xero)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format(totalCOGS)).append("\t");
			//GP (banked income - Invoiced COGs + Stock not sold)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format((totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome-totalCOGS)+(endStockOnHand-initialStockOnHand))).append("\t");
			//Profit (as per Xero)
			outString.append(NumberFormat.getCurrencyInstance(Locale.US).format((totalIncome+medicareIncome+cpaIncome+lanternPayIncome+pharmaProgramsIncome+otherIncome-totalCOGS)+(endStockOnHand-initialStockOnHand)-totalExpenses)).append("\r\n");
			ClipboardContent content = new ClipboardContent();
			content.putString(outString.toString());
			Clipboard.getSystemClipboard().setContent(content);
			dialogPane.showInformation("Data Exported","Data copied to clipboard!");
		} catch (SQLException e) {
			dialogPane.showError("Failed to export data", e);
		}
	}
}
