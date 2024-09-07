package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import models.*;
import services.BASCheckerService;
import services.EODService;
import services.InvoiceService;
import services.TillReportService;
import utils.TableUtils;
import utils.ValidatorUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class BASCheckerController extends DateSelectController{

	@FXML private GridPane incomeCheckTable;
	@FXML private GridPane medicareCheckTable;
	@FXML private GridPane cogsCheckTable;
	@FXML private MFXTextField cash1,cash2,cash3;
	@FXML private MFXTextField eftpos1,eftpos2,eftpos3;
	@FXML private MFXTextField amex1,amex2,amex3;
	@FXML private MFXTextField googleSquare1,googleSquare2,googleSquare3;
	@FXML private MFXTextField cheque1,cheque2,cheque3;
	@FXML private MFXTextField medicare1,medicare2,medicare3;
	@FXML private MFXTextField total1,total2,total3;
	@FXML private MFXTextField gst1,gst3;
	@FXML private MFXTextField tillBalance;
	@FXML private MFXCheckbox cashCorrect;
	@FXML private MFXCheckbox eftposCorrect;
	@FXML private MFXCheckbox amexCorrect;
	@FXML private MFXCheckbox googleSquareCorrect;
	@FXML private MFXCheckbox chequeCorrect;
	@FXML private MFXCheckbox medicareCorrect;
	@FXML private MFXCheckbox totalIncomeCorrect;
	@FXML private MFXCheckbox gstCorrect;
	@FXML private MFXTextField medicareSpreadsheet, medicareBAS, medicareAdjustment;
	@FXML private MFXTextField spreadsheetCheck1,spreadsheetCheck2,spreadsheetCheck3;
	@FXML private MFXTextField cogsCheck1,cogsCheck2,cogsCheck3;
	@FXML private Label errorLabel;
	@FXML private MFXButton saveButton;
	@FXML private MFXProgressSpinner progressSpinner, saveProgressSpinner;
	private EODService eodService;
	private TillReportService tillReportService;
	private InvoiceService invoiceService;
	private BASCheckerService basCheckerService;

	@FXML
	private void initialize() {
        try {
            eodService = new EODService();
			tillReportService = new TillReportService();
			invoiceService = new InvoiceService();
			basCheckerService = new BASCheckerService();
			executor = Executors.newCachedThreadPool();
        } catch (IOException e) {
			dialogPane.showError("Error", "Error initialising services", e);
        }
	}

	@Override
	public void fill() {
        for (MFXTextField mfxTextField : Arrays.asList(cash2, eftpos2, amex2, googleSquare2, cheque2, medicare2, total2, medicareBAS)) {
			if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("BAS - Edit"))) {
            	ValidatorUtils.setupRegexValidation(mfxTextField,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
			}else{
				mfxTextField.setDisable(true);
			}
        }
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("BAS - Edit"))) {
			saveButton.setOnAction(_ -> save());
		}else{
			saveButton.setDisable(true);
		}
		for (Node node : incomeCheckTable.getChildren()) {
			if (node instanceof MFXTextField textField) {
                textField.setOnKeyPressed(event -> {
					int currentRow = GridPane.getRowIndex(textField);
					int newRow = currentRow;
					if(event.getCode().equals(KeyCode.ENTER)){
						newRow = Math.min(incomeCheckTable.getRowConstraints().size() - 1, currentRow + 1);
					}
					MFXTextField newTextField = getTextFieldAt(newRow, GridPane.getColumnIndex(textField));
					if (newTextField != null) {
						newTextField.requestFocus();
					}
				});
			}
		}
		//set all text fields to have $ leading icon
		TableUtils.formatTextFields(incomeCheckTable,this::updateTotals);
		TableUtils.formatTextFields(medicareCheckTable,this::updateTotals);
		TableUtils.formatTextFields(cogsCheckTable,this::updateTotals);
		setDate(main.getCurrentDate());
	}

	private MFXTextField getTextFieldAt(int targetRow, int targetColumn) {
		for (Node node : incomeCheckTable.getChildren()) {
			if(GridPane.getRowIndex(node) == null || GridPane.getColumnIndex(node) == null) continue;
			if (GridPane.getRowIndex(node) == targetRow && GridPane.getColumnIndex(node) == targetColumn) {
				if (node instanceof AnchorPane) {
					return medicareBAS;
				} else{
					return (MFXTextField) node;
				}
			}
		}
		return null;
	}

	public void updateValues() {
		progressSpinner.setVisible(true);
		errorLabel.setText("");
		errorLabel.setVisible(false);
		errorLabel.setStyle("-fx-text-fill: red");
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		LocalDate startOfMonth = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonthValue(), 1);
		LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

		CompletableFuture<ObservableList<EODDataPoint>> eodFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return FXCollections.observableArrayList(eodService.getEODDataPoints(main.getCurrentStore().getStoreID(), startOfMonth, endOfMonth));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<ObservableList<TillReportDataPoint>> tillReportFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return FXCollections.observableArrayList(tillReportService.getTillReportDataPoints(main.getCurrentStore().getStoreID(), startOfMonth, endOfMonth));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<Double> invoiceTotalFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return invoiceService.getTotalInvoiceAmount(main.getCurrentStore().getStoreID(), yearMonthObject);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture<BASCheckerDataPoint> basDataFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return basCheckerService.getBASData(main.getCurrentStore().getStoreID(), yearMonthObject);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);

		CompletableFuture.allOf(eodFuture, tillReportFuture, invoiceTotalFuture, basDataFuture)
				.thenRunAsync(() -> {
					try {
						ObservableList<EODDataPoint> currentEODDataPoints = eodFuture.get();
						ObservableList<TillReportDataPoint> currentTillDataPoints = tillReportFuture.get();
						double invoiceTotal = invoiceTotalFuture.get();
						BASCheckerDataPoint basData = basDataFuture.get();

						// Process data
						DataProcessingResult result = processData(currentEODDataPoints, currentTillDataPoints, yearMonthObject, daysInMonth, invoiceTotal);

						// Update UI on JavaFX Application Thread
						Platform.runLater(() -> updateUI(result, basData));
					} catch (Exception e) {
						Platform.runLater(() -> dialogPane.showError("Error", "Error updating values", e));
					} finally {
						Platform.runLater(() -> progressSpinner.setVisible(false));
					}
				}, executor);
	}

	private DataProcessingResult processData(ObservableList<EODDataPoint> currentEODDataPoints,
											 ObservableList<TillReportDataPoint> currentTillDataPoints,
											 YearMonth yearMonthObject, int daysInMonth, double invoiceTotal) {
		double cashTotal = 0;
		double eftposTotal = 0;
		double amexTotal = 0;
		double googleSquareTotal = 0;
		double chequesTotal = 0;
		double medicareTotal = 0;
		double gstTotal = 0;
		double runningTillBalance = 0;

		for (EODDataPoint eod : currentEODDataPoints) {
			cashTotal += eod.getCashAmount();
			eftposTotal += eod.getEftposAmount();
			amexTotal += eod.getAmexAmount();
			googleSquareTotal += eod.getGoogleSquareAmount();
			chequesTotal += eod.getChequeAmount();
			runningTillBalance += eod.getTillBalance();
		}

		double totalSales = 0;
		double gp = 0;
		for (int i = 1; i <= daysInMonth; i++) {
			LocalDate d = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(), i);
			boolean foundMedicare = false;
			boolean foundGST = false;
			boolean foundSales = false;
			boolean foundGP = false;
			for (TillReportDataPoint tdp : currentTillDataPoints) {
				if (tdp.getAssignedDate().equals(d)) {
					switch (tdp.getKey()) {
						case "Govt Recovery":
							medicareTotal += tdp.getAmount();
							foundMedicare = true;
							break;
						case "Total GST Collected":
							gstTotal += tdp.getAmount();
							foundGST = true;
							break;
						case "Total Sales":
							totalSales += tdp.getAmount();
							foundSales = true;
							break;
						case "Gross Profit ($)":
							gp += tdp.getAmount();
							foundGP = true;
							break;
					}
					if (foundMedicare && foundGST && foundSales && foundGP) {
						break;
					}
				}
			}
		}

		double finalRunningTillBalance = runningTillBalance;
		for (EODDataPoint e : currentEODDataPoints) {
			boolean foundTillReport = false;
			for (TillReportDataPoint t : currentTillDataPoints) {
				if (e.getDate().equals(t.getAssignedDate()) && t.getKey().equals("Total Takings")) {
					e.calculateTillBalances(t.getAmount(), finalRunningTillBalance);
					foundTillReport = true;
					break;
				}
			}
			if (!foundTillReport) {
				e.calculateTillBalances(0, finalRunningTillBalance);
			}
			finalRunningTillBalance = e.getRunningTillBalance();
		}

		LocalDate startOfNextMonth = yearMonthObject.plusMonths(1).atDay(1);
		LocalDate startOfCurrentMonth = yearMonthObject.atDay(1);
		double sohGrowth = currentEODDataPoints.stream()
				.filter(e -> e.getDate().equals(startOfNextMonth))
				.mapToDouble(EODDataPoint::getStockOnHandAmount)
				.findFirst()
				.orElse(0)
				-
				currentEODDataPoints.stream()
						.filter(e -> e.getDate().equals(startOfCurrentMonth))
						.mapToDouble(EODDataPoint::getStockOnHandAmount)
						.findFirst()
						.orElse(0);

		double cogsCheck2 = (totalSales + gp) - sohGrowth;

		return new DataProcessingResult(
				cashTotal, eftposTotal, amexTotal, googleSquareTotal, chequesTotal,
				medicareTotal, gstTotal, finalRunningTillBalance, totalSales, gp,
				invoiceTotal, sohGrowth, cogsCheck2
		);
	}

	private void updateUI(DataProcessingResult result, BASCheckerDataPoint basData) {
		cash1.setText(String.format("%.2f", result.getCashTotal()));
		eftpos1.setText(String.format("%.2f", result.getEftposTotal()));
		amex1.setText(String.format("%.2f", result.getAmexTotal()));
		googleSquare1.setText(String.format("%.2f", result.getGoogleSquareTotal()));
		cheque1.setText(String.format("%.2f", result.getChequesTotal()));
		medicare1.setText(String.format("%.2f", result.getMedicareTotal()));
		medicareSpreadsheet.setText(String.format("%.2f", result.getMedicareTotal()));
		total1.setText(String.format("%.2f", result.getTotalIncome()));
		gst1.setText(String.format("%.2f", result.getGstTotal()));
		gst3.setText(String.format("%.2f", result.getGstTotal()));

		if (result.getRunningTillBalance() < 0) {
			tillBalance.setText(String.format("%.2f", result.getRunningTillBalance()));
		} else if (result.getRunningTillBalance() > 0) {
			tillBalance.setText("0.00");
		} else {
			tillBalance.setText("");
		}

		spreadsheetCheck1.setText(String.format("%.2f", result.getTotalIncome()));
		spreadsheetCheck2.setText(String.format("%.2f", result.getTotalSales()));
		spreadsheetCheck3.setText(String.format("%.2f", result.getTotalSales() - result.getTotalIncome()));

		cogsCheck1.setText(String.format("%.2f", result.getInvoiceTotal()));
		cogsCheck2.setText(String.format("%.2f", result.getCogsCheck2()));
		cogsCheck3.setText(String.format("%.2f", result.getInvoiceTotal() - result.getCogsCheck2()));

		// Update BASChecker values
		if (basData == null) {
			cash2.setText("0.00");
			eftpos2.setText("0.00");
			amex2.setText("0.00");
			googleSquare2.setText("0.00");
			cheque2.setText("0.00");
			total2.setText("0.00");
			cashCorrect.setSelected(false);
			eftposCorrect.setSelected(false);
			amexCorrect.setSelected(false);
			googleSquareCorrect.setSelected(false);
			chequeCorrect.setSelected(false);
			medicareCorrect.setSelected(false);
			totalIncomeCorrect.setSelected(false);
			gstCorrect.setSelected(false);
			medicareBAS.setText("0.00");
		} else {
			cash2.setText(String.format("%.2f", basData.getCashAdjustment()));
			eftpos2.setText(String.format("%.2f", basData.getEftposAdjustment()));
			amex2.setText(String.format("%.2f", basData.getAmexAdjustment()));
			googleSquare2.setText(String.format("%.2f", basData.getGoogleSquareAdjustment()));
			cheque2.setText(String.format("%.2f", basData.getChequeAdjustment()));
			total2.setText(String.format("%.2f", basData.getTotalIncomeAdjustment()));
			cashCorrect.setSelected(basData.isCashCorrect());
			eftposCorrect.setSelected(basData.isEftposCorrect());
			amexCorrect.setSelected(basData.isAmexCorrect());
			googleSquareCorrect.setSelected(basData.isGoogleSquareCorrect());
			chequeCorrect.setSelected(basData.isChequeCorrect());
			medicareCorrect.setSelected(basData.isMedicareCorrect());
			totalIncomeCorrect.setSelected(basData.isTotalIncomeCorrect());
			gstCorrect.setSelected(basData.isGstCorrect());
			medicareBAS.setText(String.format("%.2f", basData.getBasDailyScript()));
		}
		updateTotals();
	}

	public void updateTotals(){
		if(cash2.isValid()){
			if(cash2.getText().isEmpty())
				cash2.setText("0.00");
			else{
				cash2.setText(String.format("%.2f", Double.parseDouble(cash2.getText())));
			}
			cash3.setText(String.format("%.2f", Double.parseDouble(cash1.getText())+Double.parseDouble(cash2.getText())));
		}
		if(eftpos2.isValid()){
			if(eftpos2.getText().isEmpty())
				eftpos2.setText("0.00");
			else{
				eftpos2.setText(String.format("%.2f", Double.parseDouble(eftpos2.getText())));
			}
			eftpos3.setText(String.format("%.2f", Double.parseDouble(eftpos1.getText())+Double.parseDouble(eftpos2.getText())));
		}
		if(amex2.isValid()){
			if(amex2.getText().isEmpty())
				amex2.setText("0.00");
			else{
				amex2.setText(String.format("%.2f", Double.parseDouble(amex2.getText())));
			}
			amex3.setText(String.format("%.2f", Double.parseDouble(amex1.getText())+Double.parseDouble(amex2.getText())));
		}
		if(googleSquare2.isValid()){
			if(googleSquare2.getText().isEmpty())
				googleSquare2.setText("0.00");
			else{
				googleSquare2.setText(String.format("%.2f", Double.parseDouble(googleSquare2.getText())));
			}
			googleSquare3.setText(String.format("%.2f", Double.parseDouble(googleSquare1.getText())+Double.parseDouble(googleSquare2.getText())));
		}
		if(cheque2.isValid()){
			if(cheque2.getText().isEmpty())
				cheque2.setText("0.00");
			else{
				cheque2.setText(String.format("%.2f", Double.parseDouble(cheque2.getText())));
			}
			cheque3.setText(String.format("%.2f", Double.parseDouble(cheque1.getText())+Double.parseDouble(cheque2.getText())));
		}
		if(total2.isValid()){
			if(total2.getText().isEmpty())
				total2.setText("0.00");
			else{
				total2.setText(String.format("%.2f", Double.parseDouble(total2.getText())));
			}
			total3.setText(String.format("%.2f", Double.parseDouble(total1.getText())+Double.parseDouble(total2.getText())));
		}
		if(medicareBAS.isValid()){
			if(medicareBAS.getText().isEmpty())
				medicareBAS.setText("0.00");
			else{
				medicareBAS.setText(String.format("%.2f", Double.parseDouble(medicareBAS.getText())));
			}
			medicareAdjustment.setText(String.format("%.2f", Double.parseDouble(medicareBAS.getText())-Double.parseDouble(medicareSpreadsheet.getText())));
			medicare2.setText(String.format("%.2f", Double.parseDouble(medicareBAS.getText())-Double.parseDouble(medicareSpreadsheet.getText())));
		}
		if(medicare2.isValid()){
			if(medicare2.getText().isEmpty())
				medicare2.setText("0.00");
			else{
				medicare2.setText(String.format("%.2f", Double.parseDouble(medicare2.getText())));
			}
			medicare3.setText(String.format("%.2f", Double.parseDouble(medicare1.getText())+Double.parseDouble(medicare2.getText())));
		}
	}

	public void save() {
		updateTotals();
		if (!cash2.isValid() || !eftpos2.isValid() || !amex2.isValid() || !googleSquare2.isValid() ||
				!cheque2.isValid() || !total2.isValid() || !medicareBAS.isValid()) {
			Platform.runLater(() -> {
				errorLabel.setText("Please ensure all fields are valid");
				errorLabel.setVisible(true);
			});
			return;
		}
		saveProgressSpinner.setMaxWidth(Region.USE_COMPUTED_SIZE);
		CompletableFuture.supplyAsync(() -> {
			try {
				BASCheckerDataPoint newDataPoint = new BASCheckerDataPoint();
				newDataPoint.setDate(LocalDate.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth(), 1));
				newDataPoint.setStoreID(main.getCurrentStore().getStoreID());
				newDataPoint.setCashAdjustment(Double.parseDouble(cash2.getText()));
				newDataPoint.setEftposAdjustment(Double.parseDouble(eftpos2.getText()));
				newDataPoint.setAmexAdjustment(Double.parseDouble(amex2.getText()));
				newDataPoint.setGoogleSquareAdjustment(Double.parseDouble(googleSquare2.getText()));
				newDataPoint.setChequeAdjustment(Double.parseDouble(cheque2.getText()));
				newDataPoint.setMedicareAdjustment(Double.parseDouble(medicare2.getText()));
				newDataPoint.setTotalIncomeAdjustment(Double.parseDouble(total2.getText()));
				newDataPoint.setCashCorrect(cashCorrect.isSelected());
				newDataPoint.setEftposCorrect(eftposCorrect.isSelected());
				newDataPoint.setAmexCorrect(amexCorrect.isSelected());
				newDataPoint.setGoogleSquareCorrect(googleSquareCorrect.isSelected());
				newDataPoint.setChequeCorrect(chequeCorrect.isSelected());
				newDataPoint.setMedicareCorrect(medicareCorrect.isSelected());
				newDataPoint.setTotalIncomeCorrect(totalIncomeCorrect.isSelected());
				newDataPoint.setGstCorrect(gstCorrect.isSelected());
				newDataPoint.setBasDailyScript(Double.parseDouble(medicareBAS.getText()));
				basCheckerService.updateBASData(newDataPoint);
				return true;
			} catch (Exception ex) {
				return ex;
			}
		}, executor).thenAcceptAsync(result -> {
			saveProgressSpinner.setMaxWidth(0);
			if (result instanceof Exception) {
				Exception ex = (Exception) result;
				dialogPane.showError("Error", "Error saving BAS data", ex);
			} else {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss a");
				errorLabel.setVisible(true);
				errorLabel.setText("Last saved at " + LocalTime.now().format(formatter));
				errorLabel.setStyle("-fx-text-fill: black");
			}
		}, Platform::runLater);
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		updateMonthSelectorField();
		updateValues();
	}
}

class DataProcessingResult {
	private final double cashTotal;
	private final double eftposTotal;
	private final double amexTotal;
	private final double googleSquareTotal;
	private final double chequesTotal;
	private final double medicareTotal;
	private final double gstTotal;
	private final double runningTillBalance;
	private final double totalSales;
	private final double grossProfit;
	private final double invoiceTotal;
	private final double sohGrowth;
	private final double cogsCheck2;

	// Constructor
	public DataProcessingResult(double cashTotal, double eftposTotal, double amexTotal,
								double googleSquareTotal, double chequesTotal, double medicareTotal,
								double gstTotal, double runningTillBalance, double totalSales,
								double grossProfit, double invoiceTotal, double sohGrowth,
								double cogsCheck2) {
		this.cashTotal = cashTotal;
		this.eftposTotal = eftposTotal;
		this.amexTotal = amexTotal;
		this.googleSquareTotal = googleSquareTotal;
		this.chequesTotal = chequesTotal;
		this.medicareTotal = medicareTotal;
		this.gstTotal = gstTotal;
		this.runningTillBalance = runningTillBalance;
		this.totalSales = totalSales;
		this.grossProfit = grossProfit;
		this.invoiceTotal = invoiceTotal;
		this.sohGrowth = sohGrowth;
		this.cogsCheck2 = cogsCheck2;
	}

	// Getters
	public double getCashTotal() { return cashTotal; }
	public double getEftposTotal() { return eftposTotal; }
	public double getAmexTotal() { return amexTotal; }
	public double getGoogleSquareTotal() { return googleSquareTotal; }
	public double getChequesTotal() { return chequesTotal; }
	public double getMedicareTotal() { return medicareTotal; }
	public double getGstTotal() { return gstTotal; }
	public double getRunningTillBalance() { return runningTillBalance; }
	public double getTotalSales() { return totalSales; }
	public double getGrossProfit() { return grossProfit; }
	public double getInvoiceTotal() { return invoiceTotal; }
	public double getSohGrowth() { return sohGrowth; }
	public double getCogsCheck2() { return cogsCheck2; }

	public double getTotalIncome() {
		return cashTotal + eftposTotal + amexTotal + googleSquareTotal + chequesTotal + medicareTotal;
	}
}
