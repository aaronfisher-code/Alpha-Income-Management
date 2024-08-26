package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.BudgetAndExpensesDataPoint;
import services.AccountPaymentService;
import services.BudgetExpensesService;
import utils.RosterUtils;
import utils.TableUtils;
import utils.ValidatorUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class BudgetAndExpensesController extends DateSelectController{

	@FXML private MFXTextField numDaysField,numOpenDaysField,numPartialDaysField,dailyRentField,totalAvgField,monthlyRentField,dailyOutgoingsField,monthlyLoanField,monthlyWagesField;
	@FXML private MFXTextField cpaIncomeXero, cpaIncomeSpreadsheet,cpaIncomeVariance,lanternPayIncomeXero,lanternPayIncomeSpreadsheet,lanternPayIncomeVariance,otherIncomeXero,otherIncomeSpreadsheet,otherIncomeVariance,atoGSTrefundXero;
	@FXML private MFXButton saveButton;
	@FXML private Label errorLabel;
    @FXML private GridPane endOfMonthTable;
	private BudgetExpensesService budgetExpensesService;
	private AccountPaymentService accountPaymentService;

	@FXML
	private void initialize() {
		try{
			budgetExpensesService = new BudgetExpensesService();
			accountPaymentService = new AccountPaymentService();
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

	public void updateValues(){
		errorLabel.setText("");
		errorLabel.setVisible(false);
		errorLabel.setStyle("-fx-text-fill: red");
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		RosterUtils rosterUtils = null;
		try{
			rosterUtils = new RosterUtils(main,yearMonthObject);
		}catch (IOException e){
			dialogPane.showError("Error", "Error loading roster data", e);
		}
        assert rosterUtils != null;
        int daysInMonth = rosterUtils.getTotalDays();
		int openDays = rosterUtils.getOpenDays();
		int partialDays = rosterUtils.getPartialDays();
		numDaysField.setText(daysInMonth+" days");
		numOpenDaysField.setText(openDays+" days");
		numPartialDaysField.setText(partialDays+" days");
		try {
			BudgetAndExpensesDataPoint data = budgetExpensesService.getBudgetExpensesData(main.getCurrentStore().getStoreID(),yearMonthObject);
			//check if data returns no results
			if (data == null) {
				//Daily expenses calculator
				monthlyRentField.setText("");
				dailyRentField.setText("");
				dailyOutgoingsField.setText("");
				totalAvgField.setText("");
				monthlyLoanField.setText("");
				monthlyWagesField.setText("");
				//End of month figures
				cpaIncomeXero.setText("");
				lanternPayIncomeXero.setText("");
				otherIncomeXero.setText("");
				atoGSTrefundXero.setText("");
			}else{
				//Daily expenses calculator
				double monthlyRent = data.getMonthlyRent();
				monthlyRentField.setText(String.format("%.2f", monthlyRent));
				dailyRentField.setText(String.format("%.2f", monthlyRent/daysInMonth));
				dailyOutgoingsField.setText(String.format("%.2f", data.getDailyOutgoings()));
				totalAvgField.setText(String.format("%.2f", data.getDailyOutgoings()+(monthlyRent/daysInMonth)));
				monthlyLoanField.setText(String.format("%.2f", data.getMonthlyLoan()));
				monthlyWagesField.setText(String.format("%.2f", data.getMonthlyWages()));
				//End of month figures
				cpaIncomeXero.setText(String.format("%.2f", data.getCpaIncome()));
				lanternPayIncomeXero.setText(String.format("%.2f", data.getLanternIncome()));
				otherIncomeXero.setText(String.format("%.2f", data.getOtherIncome()));
				atoGSTrefundXero.setText(String.format("%.2f", data.getAtoGSTrefund()));
			}
			double totalCPAPayment = accountPaymentService.getTotalPayment(main.getCurrentStore().getStoreID(),yearMonthObject, AccountPaymentService.PaymentType.CPA);
			cpaIncomeSpreadsheet.setText(totalCPAPayment==0.0?"":String.format("%.2f", totalCPAPayment));
			double totalTACPayment = accountPaymentService.getTotalPayment(main.getCurrentStore().getStoreID(),yearMonthObject, AccountPaymentService.PaymentType.TAC);
			lanternPayIncomeSpreadsheet.setText(totalTACPayment==0.0?"":String.format("%.2f", totalTACPayment));
			double totalOtherPayment = accountPaymentService.getTotalPayment(main.getCurrentStore().getStoreID(),yearMonthObject, AccountPaymentService.PaymentType.OTHER);
			otherIncomeSpreadsheet.setText(totalOtherPayment==0.0?"":String.format("%.2f", totalOtherPayment));
		} catch (Exception ex) {
			dialogPane.showError("Error", "Error loading budget and expenses data", ex);
		}
		TableUtils.formatTextFields(endOfMonthTable, this::updateTotals);
		updateTotals();
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

	public void save(){
		updateTotals();
		//Validate all fields
		if(!monthlyRentField.isValid()||!dailyOutgoingsField.isValid()||!monthlyLoanField.isValid()||!cpaIncomeXero.isValid()||!lanternPayIncomeXero.isValid()||!otherIncomeXero.isValid()||!atoGSTrefundXero.isValid()||!monthlyWagesField.isValid()){
			errorLabel.setText("Please ensure all fields are valid");
			errorLabel.setVisible(true);
			return;
		}
		try{
			BudgetAndExpensesDataPoint newData = new BudgetAndExpensesDataPoint();
			newData.setDate(LocalDate.of(main.getCurrentDate().getYear(),main.getCurrentDate().getMonth(),1));
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
		} catch (Exception e) {
			dialogPane.showError("Error", "Error saving budget and expenses data", e);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm:ss a");
		errorLabel.setVisible(true);
		errorLabel.setText("Successfully saved at "+LocalTime.now().format(formatter));
		errorLabel.setStyle("-fx-text-fill: black");
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		updateMonthSelectorField();
		updateValues();
	}
}
