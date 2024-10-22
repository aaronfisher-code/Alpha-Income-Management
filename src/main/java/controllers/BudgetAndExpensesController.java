package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.*;
import services.*;
import utils.RosterUtils;
import utils.TableUtils;
import utils.ValidatorUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class BudgetAndExpensesController extends DateSelectController{

	@FXML private MFXTextField numDaysField,numOpenDaysField,numPartialDaysField,dailyRentField,totalAvgField,monthlyRentField,dailyOutgoingsField,monthlyLoanField,monthlyWagesField;
	@FXML private MFXTextField cpaIncomeXero, cpaIncomeSpreadsheet,cpaIncomeVariance,lanternPayIncomeXero,lanternPayIncomeSpreadsheet,lanternPayIncomeVariance,otherIncomeXero,otherIncomeSpreadsheet,otherIncomeVariance,atoGSTrefundXero;
	@FXML private MFXButton saveButton;
	@FXML private Label errorLabel;
    @FXML private GridPane endOfMonthTable;
	@FXML private MFXProgressSpinner progressSpinner,saveProgressSpinner;
	@FXML private MFXTextField noOfScriptsLast, noOfScriptsGrowth1, noOfScriptsTarget1, noOfScriptsGrowth2, noOfScriptsTarget2;
	private boolean noOfScriptsTarget1UseGrowth, noOfScriptsTarget2UseGrowth;
	@FXML private MFXTextField otcCustomerLast, otcCustomerGrowth1, otcCustomerTarget1, otcCustomerGrowth2, otcCustomerTarget2;
	private boolean otcCustomerTarget1UseGrowth, otcCustomerTarget2UseGrowth;
	@FXML private MFXTextField gpDollarLast, gpDollarGrowth1, gpDollarTarget1, gpDollarGrowth2, gpDollarTarget2;
	private boolean gpDollarTarget1UseGrowth, gpDollarTarget2UseGrowth;
	@FXML private MFXTextField scriptsOnFileLast, scriptsOnFileGrowth1, scriptsOnFileTarget1, scriptsOnFileGrowth2, scriptsOnFileTarget2;
	private boolean scriptsOnFileTarget1UseGrowth, scriptsOnFileTarget2UseGrowth;
	@FXML private MFXTextField medschecksLast, medschecksGrowth1, medschecksTarget1, medschecksGrowth2, medschecksTarget2;
	private boolean medschecksTarget1UseGrowth, medschecksTarget2UseGrowth;
	private BudgetExpensesService budgetExpensesService;
	private AccountPaymentService accountPaymentService;
	private TillReportService tillReportService;
	private EODService eodService;
	private TargetService targetService;

	@FXML
	private void initialize() {
		try{
			budgetExpensesService = new BudgetExpensesService();
			accountPaymentService = new AccountPaymentService();
			tillReportService = new TillReportService();
			eodService = new EODService();
			targetService = new TargetService();
			executor = Executors.newCachedThreadPool();
		}catch (IOException e){
			dialogPane.showError("Error", "Error initializing budget and expenses service", e);
		}
	}

	@Override
	public void fill() {
		for (MFXTextField mfxTextField : Arrays.asList(monthlyRentField, dailyOutgoingsField, monthlyLoanField, monthlyWagesField, cpaIncomeXero, lanternPayIncomeXero, otherIncomeXero, atoGSTrefundXero)) {
			if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Budget - Edit"))) {
				ValidatorUtils.setupRegexValidation(mfxTextField,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
			}else{
				mfxTextField.setDisable(true);
			}
		}
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Budget - Edit"))) {
			saveButton.setOnAction(_ -> save());
		}else{
			saveButton.setDisable(true);
		}
		setDate(main.getCurrentDate());
	}

	public void updateValues() {
		errorLabel.setText("");
		errorLabel.setVisible(false);
		errorLabel.setStyle("-fx-text-fill: red");
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		YearMonth lastYearMonthObject = yearMonthObject.minusYears(1);

		progressSpinner.setVisible(true);

		CompletableFuture<RosterUtils> rosterUtilsFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return new RosterUtils(main, yearMonthObject);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<BudgetAndExpensesDataPoint> budgetDataFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return budgetExpensesService.getBudgetExpensesData(main.getCurrentStore().getStoreID(), yearMonthObject);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Double> cpaPaymentFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return accountPaymentService.getTotalPayment(main.getCurrentStore().getStoreID(), yearMonthObject, AccountPaymentService.PaymentType.CPA);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Double> tacPaymentFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return accountPaymentService.getTotalPayment(main.getCurrentStore().getStoreID(), yearMonthObject, AccountPaymentService.PaymentType.TAC);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Double> otherPaymentFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return accountPaymentService.getTotalPayment(main.getCurrentStore().getStoreID(), yearMonthObject, AccountPaymentService.PaymentType.OTHER);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Double> lastYearScriptCountFuture = CompletableFuture.supplyAsync(() -> {
			try {
				List<TillReportDataPoint> tillReportData = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						lastYearMonthObject.atDay(1),
						lastYearMonthObject.atEndOfMonth(),
						"Script Count"
				);
				return tillReportData.stream()
						.mapToDouble(TillReportDataPoint::getQuantity)
						.sum();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Double> lastYearOtcCustomerFuture = CompletableFuture.supplyAsync(() -> {
			try {
				List<TillReportDataPoint> tillReportData = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						lastYearMonthObject.atDay(1),
						lastYearMonthObject.atEndOfMonth(),
						"Avg. OTC Sales Per Customer"
				);
				return tillReportData.stream()
						.mapToDouble(TillReportDataPoint::getAmount)
						.average().getAsDouble();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Double> lastYearGrossProfitFuture = CompletableFuture.supplyAsync(() -> {
			try {
				double grossProfit;
				double zDispenseGovtContribution;
				double totalGovtContribution;

				// Gross Profit ($) + Z Dispense govt contribution - Total govt contribution, (I + W - G)
				List<TillReportDataPoint> tillReportData = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						lastYearMonthObject.atDay(1),
						lastYearMonthObject.atEndOfMonth(),
						"Gross Profit ($)"
				);

				grossProfit = tillReportData.stream()
						.mapToDouble(TillReportDataPoint::getAmount)
						.sum();

				tillReportData = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						lastYearMonthObject.atDay(1),
						lastYearMonthObject.atEndOfMonth(),
						"Govt Recovery"
				);

				zDispenseGovtContribution = tillReportData.stream()
						.mapToDouble(TillReportDataPoint::getAmount)
						.sum();

				tillReportData = tillReportService.getTillReportDataPointsByKey(
						main.getCurrentStore().getStoreID(),
						lastYearMonthObject.atDay(1),
						lastYearMonthObject.atEndOfMonth(),
						"Total Government Contribution"
				);

				totalGovtContribution = tillReportData.stream()
						.mapToDouble(TillReportDataPoint::getAmount)
						.sum();

				return grossProfit + zDispenseGovtContribution - totalGovtContribution;

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Integer> lastYearScriptsOnFileFuture = CompletableFuture.supplyAsync(() -> {
			try {
				List<EODDataPoint> eodData = eodService.getEODDataPoints(
						main.getCurrentStore().getStoreID(),
						lastYearMonthObject.atDay(1),
						lastYearMonthObject.atEndOfMonth()
				);

				return eodData.stream()
						.sorted(Comparator.comparing(EODDataPoint::getDate).reversed())
						.filter(dataPoint -> dataPoint.getScriptsOnFile() > 0)
						.findFirst()
						.map(EODDataPoint::getScriptsOnFile)
						.orElse(0);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Integer> lastYearMedsChecksFuture = CompletableFuture.supplyAsync(() -> {
			try {
				List<EODDataPoint> eodData = eodService.getEODDataPoints(
						main.getCurrentStore().getStoreID(),
						lastYearMonthObject.atDay(1),
						lastYearMonthObject.atEndOfMonth()
				);

				return eodData.stream()
						.sorted(Comparator.comparing(EODDataPoint::getDate).reversed())
						.filter(dataPoint -> dataPoint.getMedschecks() > 0)
						.findFirst()
						.map(EODDataPoint::getMedschecks)
						.orElse(0);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture.allOf(rosterUtilsFuture, budgetDataFuture, cpaPaymentFuture,
						tacPaymentFuture, otherPaymentFuture, lastYearScriptCountFuture, lastYearOtcCustomerFuture, lastYearGrossProfitFuture, lastYearScriptsOnFileFuture, lastYearMedsChecksFuture)
				.thenRunAsync(() -> {
					try {
						RosterUtils rosterUtils = rosterUtilsFuture.get();
						BudgetAndExpensesDataPoint data = budgetDataFuture.get();
						double totalCPAPayment = cpaPaymentFuture.get();
						double totalTACPayment = tacPaymentFuture.get();
						double totalOtherPayment = otherPaymentFuture.get();
						double lastYearScriptCount = lastYearScriptCountFuture.get();
						double lastYearOtcCustomer = lastYearOtcCustomerFuture.get();
						double lastYearGrossProfit = lastYearGrossProfitFuture.get();
						int lastYearScriptsOnFile = lastYearScriptsOnFileFuture.get();
						int lastYearMedsChecks = lastYearMedsChecksFuture.get();

						Platform.runLater(() -> {
							updateUIWithData(rosterUtils, data, totalCPAPayment, totalTACPayment, totalOtherPayment);
							noOfScriptsLast.setText(String.format("%.0f", lastYearScriptCount));
							otcCustomerLast.setText(String.format("%.2f", lastYearOtcCustomer));
							gpDollarLast.setText(String.format("%.2f", lastYearGrossProfit));
							scriptsOnFileLast.setText(String.valueOf(lastYearScriptsOnFile));
							medschecksLast.setText(String.valueOf(lastYearMedsChecks));
							formatIntegerTargetFields(noOfScriptsLast, noOfScriptsGrowth1, noOfScriptsTarget1, noOfScriptsGrowth2, noOfScriptsTarget2, noOfScriptsTarget1UseGrowth, noOfScriptsTarget2UseGrowth);
							formatCurrencyTargetFields(otcCustomerLast, otcCustomerGrowth1, otcCustomerTarget1, otcCustomerGrowth2, otcCustomerTarget2, otcCustomerTarget1UseGrowth, otcCustomerTarget2UseGrowth);
							formatCurrencyTargetFields(gpDollarLast, gpDollarGrowth1, gpDollarTarget1, gpDollarGrowth2, gpDollarTarget2, gpDollarTarget1UseGrowth, gpDollarTarget2UseGrowth);
							formatIntegerTargetFields(scriptsOnFileLast, scriptsOnFileGrowth1, scriptsOnFileTarget1, scriptsOnFileGrowth2, scriptsOnFileTarget2, scriptsOnFileTarget1UseGrowth, scriptsOnFileTarget2UseGrowth);
							formatIntegerTargetFields(medschecksLast, medschecksGrowth1, medschecksTarget1, medschecksGrowth2, medschecksTarget2, medschecksTarget1UseGrowth, medschecksTarget2UseGrowth);
							TableUtils.formatTextFields(endOfMonthTable, this::updateTotals);
							updateTotals();
							progressSpinner.setVisible(false);
						});
					} catch (Exception e) {
						Platform.runLater(() -> {
							dialogPane.showError("Error", "Error loading budget and expenses data", e);
							progressSpinner.setVisible(false);
						});
					}
				}, executor);
	}

	private void formatIntegerTargetFields(MFXTextField lastYearField, MFXTextField growth1Field, MFXTextField target1Field, MFXTextField growth2Field, MFXTextField target2Field, boolean useGrowth1, boolean useGrowth2) {
		// Apply the standard formatting
		TableUtils.formatTextField(lastYearField, this::updateTotals, TableUtils.formatStyle.INTEGER);
		TableUtils.formatTextField(growth1Field, this::updateTotals, TableUtils.formatStyle.PERCENTAGE);
		TableUtils.formatTextField(target1Field, this::updateTotals, TableUtils.formatStyle.INTEGER);
		TableUtils.formatTextField(growth2Field, this::updateTotals, TableUtils.formatStyle.PERCENTAGE);
		TableUtils.formatTextField(target2Field, this::updateTotals, TableUtils.formatStyle.INTEGER);

		setupTargetPairListeners(lastYearField, growth1Field, target1Field, TableUtils.formatStyle.INTEGER, useGrowth1);
		setupTargetPairListeners(target1Field, growth2Field, target2Field, TableUtils.formatStyle.INTEGER, useGrowth2);
	}

	private void formatCurrencyTargetFields(MFXTextField lastYearField, MFXTextField growth1Field, MFXTextField target1Field, MFXTextField growth2Field, MFXTextField target2Field, boolean useGrowth1, boolean useGrowth2) {
		TableUtils.formatTextField(lastYearField, this::updateTotals, TableUtils.formatStyle.CURRENCY);
		TableUtils.formatTextField(growth1Field, this::updateTotals, TableUtils.formatStyle.PERCENTAGE);
		TableUtils.formatTextField(target1Field, this::updateTotals, TableUtils.formatStyle.CURRENCY);
		TableUtils.formatTextField(growth2Field, this::updateTotals, TableUtils.formatStyle.PERCENTAGE);
		TableUtils.formatTextField(target2Field, this::updateTotals, TableUtils.formatStyle.CURRENCY);

		setupTargetPairListeners(lastYearField, growth1Field, target1Field, TableUtils.formatStyle.CURRENCY, useGrowth1);
		setupTargetPairListeners(target1Field, growth2Field, target2Field, TableUtils.formatStyle.CURRENCY, useGrowth2);
	}

	private void setupTargetPairListeners(MFXTextField baseField, MFXTextField growthField, MFXTextField targetField, TableUtils.formatStyle format, boolean useGrowth) {
		growthField.textProperty().addListener((_, oldValue, newValue) -> {
			if (!newValue.equals(oldValue)) {
				if (!growthField.isFocused() || !newValue.isEmpty()) {
					updateTargetFromGrowth(baseField, growthField, targetField, format, useGrowth);
				}
			}
		});

		targetField.textProperty().addListener((_, oldValue, newValue) -> {
			if (!newValue.equals(oldValue)) {
				if (!targetField.isFocused() || !newValue.isEmpty()) {
					updateGrowthFromTarget(baseField, growthField, targetField, useGrowth);
				}
			}
		});
	}

	private void updateTargetFromGrowth(MFXTextField baseField, MFXTextField growthField, MFXTextField targetField, TableUtils.formatStyle format, boolean useGrowth) {
		try {
			if (!baseField.getText().isEmpty() && !growthField.getText().isEmpty()) {
				String growthText = growthField.getText().replace("%", "").trim();
				double baseValue = Double.parseDouble(baseField.getText());
				double growth = Double.parseDouble(growthText) / 100; // Convert percentage to decimal
				double target = baseValue * (1 + growth);
				useGrowth = true;

				// Format based on the field type
				if (format == TableUtils.formatStyle.INTEGER) {
					targetField.setText(String.format("%.0f", target));
				} else if (format == TableUtils.formatStyle.CURRENCY) {
					targetField.setText(String.format("%.2f", target));
				}
			}
		} catch (NumberFormatException ex) {
			if (growthField.isFocused()) {
				targetField.setText("");
			}
		}
	}

	private void updateGrowthFromTarget(MFXTextField baseField, MFXTextField growthField, MFXTextField targetField, boolean useGrowth) {
		try {
			if (!baseField.getText().isEmpty() && !targetField.getText().isEmpty()) {
				double baseValue = Double.parseDouble(baseField.getText());
				double target = Double.parseDouble(targetField.getText());
				if (baseValue != 0) {
					double growth = ((target / baseValue) - 1) * 100; // Convert to percentage
					growthField.setText(String.format("%.1f", growth));
					useGrowth = false;
				}
			}
		} catch (NumberFormatException ex) {
			if (targetField.isFocused()) {
				growthField.setText("");
			}
		}
	}

	private void updateUIWithData(RosterUtils rosterUtils, BudgetAndExpensesDataPoint data,
								  double totalCPAPayment, double totalTACPayment, double totalOtherPayment) {
		int daysInMonth = rosterUtils.getTotalDays();
		int openDays = rosterUtils.getOpenDays();
		int partialDays = rosterUtils.getPartialDays();
		numDaysField.setText(daysInMonth + " days");
		numOpenDaysField.setText(openDays + " days");
		numPartialDaysField.setText(partialDays + " days");
		if (data == null) {
			Arrays.asList(monthlyRentField, dailyRentField, dailyOutgoingsField, totalAvgField,
					monthlyLoanField, monthlyWagesField, cpaIncomeXero, lanternPayIncomeXero,
					otherIncomeXero, atoGSTrefundXero).forEach(field -> field.setText(""));
		} else {
			double monthlyRent = data.getMonthlyRent();
			monthlyRentField.setText(String.format("%.2f", monthlyRent));
			dailyRentField.setText(String.format("%.2f", monthlyRent / daysInMonth));
			dailyOutgoingsField.setText(String.format("%.2f", data.getDailyOutgoings()));
			totalAvgField.setText(String.format("%.2f", data.getDailyOutgoings() + (monthlyRent / daysInMonth)));
			monthlyLoanField.setText(String.format("%.2f", data.getMonthlyLoan()));
			monthlyWagesField.setText(String.format("%.2f", data.getMonthlyWages()));
			cpaIncomeXero.setText(String.format("%.2f", data.getCpaIncome()));
			lanternPayIncomeXero.setText(String.format("%.2f", data.getLanternIncome()));
			otherIncomeXero.setText(String.format("%.2f", data.getOtherIncome()));
			atoGSTrefundXero.setText(String.format("%.2f", data.getAtoGSTrefund()));
		}
		cpaIncomeSpreadsheet.setText(totalCPAPayment == 0.0 ? "" : String.format("%.2f", totalCPAPayment));
		lanternPayIncomeSpreadsheet.setText(totalTACPayment == 0.0 ? "" : String.format("%.2f", totalTACPayment));
		otherIncomeSpreadsheet.setText(totalOtherPayment == 0.0 ? "" : String.format("%.2f", totalOtherPayment));
	}

	public void updateTotals(){
		if(monthlyRentField.isValid()) {
			if (monthlyRentField.getText().isEmpty())
				monthlyRentField.setText("0.00");
			else {
				monthlyRentField.setText(String.format("%.2f", Double.parseDouble(monthlyRentField.getText())));
			}
			dailyRentField.setText(String.format("%.2f", Double.parseDouble(monthlyRentField.getText())/Integer.parseInt(numDaysField.getText().split(" ")[0])));
		}
		if(dailyOutgoingsField.isValid()) {
			if (dailyOutgoingsField.getText().isEmpty())
				dailyOutgoingsField.setText("0.00");
			else {
				dailyOutgoingsField.setText(String.format("%.2f", Double.parseDouble(dailyOutgoingsField.getText())));
			}
		}
		if(monthlyRentField.isValid() && dailyOutgoingsField.isValid()) {
			totalAvgField.setText(String.format("%.2f", Double.parseDouble(dailyOutgoingsField.getText())+(Double.parseDouble(monthlyRentField.getText())/Integer.parseInt(numDaysField.getText().split(" ")[0]))));
		}
		if(monthlyLoanField.isValid()) {
			if (monthlyLoanField.getText().isEmpty())
				monthlyLoanField.setText("0.00");
			else {
				monthlyLoanField.setText(String.format("%.2f", Double.parseDouble(monthlyLoanField.getText())));
			}
		}
		if(monthlyWagesField.isValid()) {
			if (monthlyWagesField.getText().isEmpty())
				monthlyWagesField.setText("0.00");
			else {
				monthlyWagesField.setText(String.format("%.2f", Double.parseDouble(monthlyWagesField.getText())));
			}
		}
		if(cpaIncomeXero.isValid()) {
			cpaIncomeXero.setText(cpaIncomeXero.getText().isEmpty()?"0.00":String.format("%.2f", Double.parseDouble(cpaIncomeXero.getText())));
			cpaIncomeSpreadsheet.setText(cpaIncomeSpreadsheet.getText().isEmpty()?"0.00":String.format("%.2f", Double.parseDouble(cpaIncomeSpreadsheet.getText())));
			cpaIncomeVariance.setText(String.format("%.2f", Double.parseDouble(cpaIncomeXero.getText())-Double.parseDouble(cpaIncomeSpreadsheet.getText())));
		}
		if(lanternPayIncomeXero.isValid()) {
			lanternPayIncomeXero.setText(lanternPayIncomeXero.getText().isEmpty()?"0.00":String.format("%.2f", Double.parseDouble(lanternPayIncomeXero.getText())));
			lanternPayIncomeSpreadsheet.setText(lanternPayIncomeSpreadsheet.getText().isEmpty()?"0.00":String.format("%.2f", Double.parseDouble(lanternPayIncomeSpreadsheet.getText())));
			lanternPayIncomeVariance.setText(String.format("%.2f", Double.parseDouble(lanternPayIncomeXero.getText())-Double.parseDouble(lanternPayIncomeSpreadsheet.getText())));
		}
		if(otherIncomeXero.isValid()) {
			otherIncomeXero.setText(otherIncomeXero.getText().isEmpty()?"0.00":String.format("%.2f", Double.parseDouble(otherIncomeXero.getText())));
			otherIncomeSpreadsheet.setText(otherIncomeSpreadsheet.getText().isEmpty()?"0.00":String.format("%.2f", Double.parseDouble(otherIncomeSpreadsheet.getText())));
			otherIncomeVariance.setText(String.format("%.2f", Double.parseDouble(otherIncomeXero.getText())-Double.parseDouble(otherIncomeSpreadsheet.getText())));
		}
	}

	public void save() {
		updateTotals();
		// Validate all fields
		if (!monthlyRentField.isValid() || !dailyOutgoingsField.isValid() || !monthlyLoanField.isValid() ||
				!cpaIncomeXero.isValid() || !lanternPayIncomeXero.isValid() || !otherIncomeXero.isValid() ||
				!atoGSTrefundXero.isValid() || !monthlyWagesField.isValid() || !noOfScriptsTarget1.isValid() ||
				!noOfScriptsTarget2.isValid() || !otcCustomerTarget1.isValid() || !otcCustomerTarget2.isValid() ||
				!gpDollarTarget1.isValid() || !gpDollarTarget2.isValid() || !scriptsOnFileTarget1.isValid() ||
				!scriptsOnFileTarget2.isValid() || !medschecksTarget1.isValid() || !medschecksTarget2.isValid()) {
			errorLabel.setText("Please ensure all fields are valid");
			errorLabel.setVisible(true);
			return;
		}

		saveProgressSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);

		Task<Void> saveTask = new Task<>() {
			@Override
			protected Void call() {
				BudgetAndExpensesDataPoint newData = new BudgetAndExpensesDataPoint();
				newData.setDate(LocalDate.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth(), 1));
				newData.setStoreID(main.getCurrentStore().getStoreID());
				newData.setMonthlyRent(Double.parseDouble(monthlyRentField.getText()));
				newData.setDailyOutgoings(Double.parseDouble(dailyOutgoingsField.getText()));
				newData.setMonthlyLoan(Double.parseDouble(monthlyLoanField.getText()));
				newData.setCpaIncome(Double.parseDouble(cpaIncomeXero.getText()));
				newData.setLanternIncome(Double.parseDouble(lanternPayIncomeXero.getText()));
				newData.setOtherIncome(Double.parseDouble(otherIncomeXero.getText()));
				newData.setAtoGSTrefund(Double.parseDouble(atoGSTrefundXero.getText()));
				newData.setMonthlyWages(Double.parseDouble(monthlyWagesField.getText()));
				budgetExpensesService.updateBudgetExpensesData(newData);

				DBTargetDatapoint noOfScriptsTargetData = new DBTargetDatapoint(
						LocalDate.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth(), 1),
						"NoOfScripts",
						Double.parseDouble(noOfScriptsGrowth1.getText()),
						Double.parseDouble(noOfScriptsTarget1.getText()),
						noOfScriptsTarget1UseGrowth,
						Double.parseDouble(noOfScriptsGrowth2.getText()),
						Double.parseDouble(noOfScriptsTarget2.getText()),
						noOfScriptsTarget2UseGrowth
				);

				DBTargetDatapoint otcCustomerTargetData = new DBTargetDatapoint(
						LocalDate.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth(), 1),
						"OTCCustomer",
						Double.parseDouble(otcCustomerGrowth1.getText()),
						Double.parseDouble(otcCustomerTarget1.getText()),
						otcCustomerTarget1UseGrowth,
						Double.parseDouble(otcCustomerGrowth2.getText()),
						Double.parseDouble(otcCustomerTarget2.getText()),
						otcCustomerTarget2UseGrowth
				);

				DBTargetDatapoint gpDollarTargetData = new DBTargetDatapoint(
						LocalDate.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth(), 1),
						"GPDollar",
						Double.parseDouble(gpDollarGrowth1.getText()),
						Double.parseDouble(gpDollarTarget1.getText()),
						gpDollarTarget1UseGrowth,
						Double.parseDouble(gpDollarGrowth2.getText()),
						Double.parseDouble(gpDollarTarget2.getText()),
						gpDollarTarget2UseGrowth
				);

				DBTargetDatapoint scriptsOnFileTargetData = new DBTargetDatapoint(
						LocalDate.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth(), 1),
						"ScriptsOnFile",
						Double.parseDouble(scriptsOnFileGrowth1.getText()),
						Double.parseDouble(scriptsOnFileTarget1.getText()),
						scriptsOnFileTarget1UseGrowth,
						Double.parseDouble(scriptsOnFileGrowth2.getText()),
						Double.parseDouble(scriptsOnFileTarget2.getText()),
						scriptsOnFileTarget2UseGrowth
				);

				DBTargetDatapoint medschecksTargetData = new DBTargetDatapoint(
						LocalDate.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth(), 1),
						"Medschecks",
						Double.parseDouble(medschecksGrowth1.getText()),
						Double.parseDouble(medschecksTarget1.getText()),
						medschecksTarget1UseGrowth,
						Double.parseDouble(medschecksGrowth2.getText()),
						Double.parseDouble(medschecksTarget2.getText()),
						medschecksTarget2UseGrowth
				);

				targetService.updateTargetData(noOfScriptsTargetData);
				targetService.updateTargetData(otcCustomerTargetData);
				targetService.updateTargetData(gpDollarTargetData);
				targetService.updateTargetData(scriptsOnFileTargetData);
				targetService.updateTargetData(medschecksTargetData);

				return null;
			}
		};
		saveTask.setOnSucceeded(_ -> {
			saveProgressSpinner.setMaxWidth(0);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm:ss a");
			errorLabel.setVisible(true);
			errorLabel.setText("Successfully saved at " + LocalTime.now().format(formatter));
			errorLabel.setStyle("-fx-text-fill: black");
		});
		saveTask.setOnFailed(_ -> {
			saveProgressSpinner.setMaxWidth(0);
			dialogPane.showError("Error", "Error saving budget and expenses data", saveTask.getException());
		});
		executor.submit(saveTask);
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		updateMonthSelectorField();
		updateValues();
	}
}
