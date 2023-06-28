package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import models.BASCheckerDataPoint;
import models.EODDataPoint;
import models.TillReportDataPoint;
import org.controlsfx.control.PopOver;
import utils.ValidatorUtils;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class BudgetAndExpensesController extends DateSelectController{

	private PopOver currentDatePopover;

	@FXML
	private StackPane monthSelector;
	@FXML
	private MFXTextField monthSelectorField;
	@FXML
	private FlowPane datePickerPane;
	@FXML
	private StackPane backgroundPane;

	@FXML
	private MFXTextField numDaysField,numOpenDaysField,numPartialDaysField,dailyRentField,weeklyWagesField,totalAvgField,monthlyRentField,dailyOutgoingsField,monthlyLoanField;

	@FXML
	private MFXTextField cpaIncomeXero, cpaIncomeSpreadsheet,cpaIncomeVariance,lanternPayIncomeXero,lanternPayIncomeSpreadsheet,lanternPayIncomeVariance,otherIncomeXero,otherIncomeSpreadsheet,otherIncomeVariance,atoGSTrefundXero,atoGSTrefundSpreadsheet,atoGSTrefundVariance;

	@FXML
	private MFXButton saveButton;

	@FXML
	private Label errorLabel;

	@FXML
	private GridPane dailyExpensesTable, endOfMonthTable;
	private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;

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
		ValidatorUtils.setupRegexValidation(monthlyRentField,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR, "$", saveButton);
		ValidatorUtils.setupRegexValidation(dailyOutgoingsField,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR, "$", saveButton);
		ValidatorUtils.setupRegexValidation(monthlyLoanField,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR, "$", saveButton);
		ValidatorUtils.setupRegexValidation(cpaIncomeXero,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR, "$", saveButton);
		ValidatorUtils.setupRegexValidation(lanternPayIncomeXero,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR, "$", saveButton);
		ValidatorUtils.setupRegexValidation(otherIncomeXero,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR, "$", saveButton);
		ValidatorUtils.setupRegexValidation(atoGSTrefundXero,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR, "$", saveButton);
		saveButton.setOnAction(actionEvent -> save());
		setDate(main.getCurrentDate());
	}

	public void updateValues(){
		errorLabel.setText("");
		errorLabel.setVisible(false);
		errorLabel.setStyle("-fx-text-fill: red");
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		numDaysField.setText(daysInMonth+" days");
		//todo: use roster to calculate open days
		//todo: use roster to calculate partial days
		String sql = null;
		try {
			sql = "SELECT * FROM budgetandexpenses WHERE storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, main.getCurrentDate().getMonthValue());
			preparedStatement.setInt(3, main.getCurrentDate().getYear());
			resultSet = preparedStatement.executeQuery();
			//check if resultset returns no results
			if (resultSet == null || !resultSet.next()) {
				//Daily expenses calculator
				monthlyRentField.setText("");
				dailyRentField.setText("");
				weeklyWagesField.setText("");
				dailyOutgoingsField.setText("");
				totalAvgField.setText("");
				monthlyLoanField.setText("");

				//End of month figures
				cpaIncomeXero.setText("");
				lanternPayIncomeXero.setText("");
				otherIncomeXero.setText("");
				atoGSTrefundXero.setText("");
			}else{
				//Daily expenses calculator
				double monthlyRent = resultSet.getDouble("monthlyRent");
				monthlyRentField.setText(String.format("%.2f", monthlyRent));
				dailyRentField.setText(String.format("%.2f", monthlyRent/daysInMonth));
				weeklyWagesField.setText(String.format("%.2f", 0.00)); //todo: add wage calculation
				dailyOutgoingsField.setText(String.format("%.2f", resultSet.getDouble("dailyOutgoings")));
				totalAvgField.setText(String.format("%.2f", resultSet.getDouble("dailyOutgoings")+(monthlyRent/daysInMonth)));
				monthlyLoanField.setText(String.format("%.2f", resultSet.getDouble("monthlyLoan")));

				//End of month figures
				cpaIncomeXero.setText(String.format("%.2f", resultSet.getDouble("6CPAIncome")));
				lanternPayIncomeXero.setText(String.format("%.2f", resultSet.getDouble("LanternPayIncome")));
				otherIncomeXero.setText(String.format("%.2f", resultSet.getDouble("OtherIncome")));
				atoGSTrefundXero.setText(String.format("%.2f", resultSet.getDouble("ATO_GST_BAS_refund")));
			}

			sql = "SELECT SUM(ap.unitAmount) AS TotalPayment " +
					"FROM accountpayments ap " +
					"INNER JOIN accountpaymentcontacts apc ON apc.idaccountPaymentContacts = ap.contactID " +
					"WHERE apc.contactName LIKE ? " +
					"AND MONTH(ap.invoiceDate) = ? AND YEAR(ap.invoiceDate) = ? " +
					"AND ap.storeID = ?";

			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1, "%CPA%");
			preparedStatement.setInt(2, main.getCurrentDate().getMonthValue());
			preparedStatement.setInt(3, main.getCurrentDate().getYear());
			preparedStatement.setInt(4, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			if (resultSet == null || !resultSet.next()) {
				cpaIncomeSpreadsheet.setText("");
			}else{
				cpaIncomeSpreadsheet.setText(String.format("%.2f", resultSet.getDouble("TotalPayment")));
			}

			preparedStatement.setString(1, "%TAC%");
			resultSet = preparedStatement.executeQuery();
			if (resultSet == null || !resultSet.next()) {
				lanternPayIncomeSpreadsheet.setText("");
			}else{
				lanternPayIncomeSpreadsheet.setText(String.format("%.2f", resultSet.getDouble("TotalPayment")));
			}

			sql = "SELECT SUM(ap.unitAmount) AS TotalPayment " +
					"FROM accountpayments ap " +
					"INNER JOIN accountpaymentcontacts apc ON apc.idaccountPaymentContacts = ap.contactID " +
					"WHERE apc.contactName NOT LIKE ? AND apc.contactName NOT LIKE ?" +
					"AND MONTH(ap.invoiceDate) = ? AND YEAR(ap.invoiceDate) = ? " +
					"AND ap.storeID = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setString(1, "%CPA%");
			preparedStatement.setString(2, "%TAC%");
			preparedStatement.setInt(3, main.getCurrentDate().getMonthValue());
			preparedStatement.setInt(4, main.getCurrentDate().getYear());
			preparedStatement.setInt(5, main.getCurrentStore().getStoreID());
			resultSet = preparedStatement.executeQuery();
			if (resultSet == null || !resultSet.next()) {
				otherIncomeSpreadsheet.setText("");
			}else{
				otherIncomeSpreadsheet.setText(String.format("%.2f", resultSet.getDouble("TotalPayment")));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}

		formatTextFields(dailyExpensesTable);
		formatTextFields(endOfMonthTable);

		updateTotals();
	}

	private void formatTextFields(GridPane table) {
		for(Node n: table.getChildren()){
			if(n instanceof MFXTextField){
				((MFXTextField) n).setLeadingIcon(new Label("$"));
				((MFXTextField) n).setAlignment(Pos.CENTER_RIGHT);
				((MFXTextField) n).delegateFocusedProperty().addListener((obs, oldVal, newVal) -> {
					if (((MFXTextField) n).isValid()) {
						updateTotals();
					}
				});
			}
		}
	}

	public void updateTotals(){
		if(monthlyRentField.isValid()) {
			if (monthlyRentField.getText().equals(""))
				monthlyRentField.setText("0.00");
			else {
				monthlyRentField.setText(String.format("%.2f", Double.parseDouble(monthlyRentField.getText())));
			}
			dailyRentField.setText(String.format("%.2f", Double.parseDouble(monthlyRentField.getText())/Integer.parseInt(numDaysField.getText().split(" ")[0])));
		}
		if(dailyOutgoingsField.isValid()) {
			if (dailyOutgoingsField.getText().equals(""))
				dailyOutgoingsField.setText("0.00");
			else {
				dailyOutgoingsField.setText(String.format("%.2f", Double.parseDouble(dailyOutgoingsField.getText())));
			}
		}
		if(monthlyRentField.isValid() && dailyOutgoingsField.isValid()) {
			totalAvgField.setText(String.format("%.2f", Double.parseDouble(dailyOutgoingsField.getText())+(Double.parseDouble(monthlyRentField.getText())/Integer.parseInt(numDaysField.getText().split(" ")[0]))));
		}
		if(monthlyLoanField.isValid()) {
			if (monthlyLoanField.getText().equals(""))
				monthlyLoanField.setText("0.00");
			else {
				monthlyLoanField.setText(String.format("%.2f", Double.parseDouble(monthlyLoanField.getText())));
			}
		}
		if(cpaIncomeXero.isValid()) {
			if (cpaIncomeXero.getText().equals(""))
				cpaIncomeXero.setText("0.00");
			else {
				cpaIncomeXero.setText(String.format("%.2f", Double.parseDouble(cpaIncomeXero.getText())));
			}
			cpaIncomeVariance.setText(String.format("%.2f", Double.parseDouble(cpaIncomeXero.getText())-Double.parseDouble(cpaIncomeSpreadsheet.getText())));
		}
		if(lanternPayIncomeXero.isValid()) {
			if (lanternPayIncomeXero.getText().equals(""))
				lanternPayIncomeXero.setText("0.00");
			else {
				lanternPayIncomeXero.setText(String.format("%.2f", Double.parseDouble(lanternPayIncomeXero.getText())));
			}
			lanternPayIncomeVariance.setText(String.format("%.2f", Double.parseDouble(lanternPayIncomeXero.getText())-Double.parseDouble(lanternPayIncomeSpreadsheet.getText())));
		}
		if(otherIncomeXero.isValid()) {
			if (otherIncomeXero.getText().equals(""))
				otherIncomeXero.setText("0.00");
			else {
				otherIncomeXero.setText(String.format("%.2f", Double.parseDouble(otherIncomeXero.getText())));
			}
			otherIncomeVariance.setText(String.format("%.2f", Double.parseDouble(otherIncomeXero.getText())-Double.parseDouble(otherIncomeSpreadsheet.getText())));
		}
//		if(atoGSTrefundXero.isValid()) {
//			if (atoGSTrefundXero.getText().equals(""))
//				atoGSTrefundXero.setText("0.00");
//			else {
//				atoGSTrefundXero.setText(String.format("%.2f", Double.parseDouble(atoGSTrefundXero.getText())));
//			}
//			atoGSTrefundVariance.setText(String.format("%.2f", Double.parseDouble(atoGSTrefundXero.getText())-Double.parseDouble(atoGSTrefundSpreadsheet.getText())));
//		}
	}

	public void save(){
		updateTotals();
		//Validate all fields
		if(!monthlyRentField.isValid()||!dailyOutgoingsField.isValid()||!monthlyLoanField.isValid()||!cpaIncomeXero.isValid()||!lanternPayIncomeXero.isValid()||!otherIncomeXero.isValid()||!atoGSTrefundXero.isValid()){
			errorLabel.setText("Please ensure all fields are valid");
			errorLabel.setVisible(true);
			return;
		}
		Date date = Date.valueOf(LocalDate.of(main.getCurrentDate().getYear(),main.getCurrentDate().getMonth(),1));
		String sql = "INSERT INTO budgetAndExpenses (date,storeID,monthlyRent,dailyOutgoings,monthlyLoan,6CPAIncome,LanternPayIncome,OtherIncome,ATO_GST_BAS_refund) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE monthlyRent=?,dailyOutgoings=?,monthlyLoan=?,6CPAIncome=?,LanternPayIncome=?,OtherIncome=?,ATO_GST_BAS_refund=?";
		try{
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setDate(1,date);
			preparedStatement.setInt(2,main.getCurrentStore().getStoreID());
			preparedStatement.setDouble(3,Double.parseDouble(monthlyRentField.getText()));
			preparedStatement.setDouble(4,Double.parseDouble(dailyOutgoingsField.getText()));
			preparedStatement.setDouble(5,Double.parseDouble(monthlyLoanField.getText()));
			preparedStatement.setDouble(6,Double.parseDouble(cpaIncomeXero.getText()));
			preparedStatement.setDouble(7,Double.parseDouble(lanternPayIncomeXero.getText()));
			preparedStatement.setDouble(8,Double.parseDouble(otherIncomeXero.getText()));
			preparedStatement.setDouble(9,Double.parseDouble(atoGSTrefundXero.getText()));
			preparedStatement.setDouble(10,Double.parseDouble(monthlyRentField.getText()));
			preparedStatement.setDouble(11,Double.parseDouble(dailyOutgoingsField.getText()));
			preparedStatement.setDouble(12,Double.parseDouble(monthlyLoanField.getText()));
			preparedStatement.setDouble(13,Double.parseDouble(cpaIncomeXero.getText()));
			preparedStatement.setDouble(14,Double.parseDouble(lanternPayIncomeXero.getText()));
			preparedStatement.setDouble(15,Double.parseDouble(otherIncomeXero.getText()));
			preparedStatement.setDouble(16,Double.parseDouble(atoGSTrefundXero.getText()));
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm:ss a");
		errorLabel.setVisible(true);
		errorLabel.setText("Last saved at "+LocalTime.now().format(formatter));
		errorLabel.setStyle("-fx-text-fill: black");
	}


	public void monthForward() {
		setDate(main.getCurrentDate().plusMonths(1));
	}

	public void monthBackward() {
		setDate(main.getCurrentDate().minusMonths(1));
	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		String fieldText = main.getCurrentDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		fieldText += ", ";
		fieldText += main.getCurrentDate().getYear();
		monthSelectorField.setText(fieldText);
		updateValues();
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
}
