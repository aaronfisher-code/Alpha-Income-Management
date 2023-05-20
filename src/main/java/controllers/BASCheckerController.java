package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import models.*;
import org.controlsfx.control.PopOver;
import utils.ValidatorUtils;

import java.io.IOException;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;

public class BASCheckerController extends DateSelectController{

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
	private GridPane incomeCheckTable;
	@FXML
	private GridPane medicareCheckTable;
	@FXML
	private GridPane cogsCheckTable;
	@FXML
	private MFXTextField cash1,cash2,cash3;
	@FXML
	private MFXTextField eftpos1,eftpos2,eftpos3;
	@FXML
	private MFXTextField amex1,amex2,amex3;
	@FXML
	private MFXTextField googleSquare1,googleSquare2,googleSquare3;
	@FXML
	private MFXTextField cheque1,cheque2,cheque3;
	@FXML
	private MFXTextField medicare1,medicare2,medicare3;
	@FXML
	private MFXTextField total1,total2,total3;
	@FXML
	private MFXTextField gst1,gst3;
	@FXML
	private MFXTextField tillBalance;
	@FXML
	private MFXCheckbox cashCorrect;
	@FXML
	private MFXCheckbox eftposCorrect;
	@FXML
	private MFXCheckbox amexCorrect;
	@FXML
	private MFXCheckbox googleSquareCorrect;
	@FXML
	private MFXCheckbox chequeCorrect;
	@FXML
	private MFXCheckbox medicareCorrect;
	@FXML
	private MFXCheckbox totalIncomeCorrect;
	@FXML
	private MFXCheckbox gstCorrect;
	@FXML
	private MFXTextField medicareSpreadsheet, medicareBAS, medicareAdjustment;
	@FXML
	private MFXTextField spreadsheetCheck1,spreadsheetCheck2,spreadsheetCheck3;
	@FXML
	private MFXTextField cogsCheck1,cogsCheck2,cogsCheck3;
	@FXML
	private Label errorLabel;
	@FXML
	private MFXButton saveButton;


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
		ValidatorUtils.setupRegexValidation(cash2,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(eftpos2,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(amex2,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(googleSquare2,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(cheque2,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(medicare2,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(total2,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(medicareBAS,errorLabel,ValidatorUtils.CASH_EMPTY_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		saveButton.setOnAction(actionEvent -> save());

		for (Node node : incomeCheckTable.getChildren()) {
			if (node instanceof MFXTextField) {
				MFXTextField textField = (MFXTextField) node;
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

		setDate(LocalDate.now());
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

	public void updateValues(){
		errorLabel.setText("");
		errorLabel.setVisible(false);
		errorLabel.setStyle("-fx-text-fill: red");
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		ObservableList<EODDataPoint> currentEODDataPoints = FXCollections.observableArrayList();
		ObservableList<TillReportDataPoint> currentTillDataPoints = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM eoddatapoints WHERE eoddatapoints.storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentEODDataPoints.add(new EODDataPoint(resultSet));
			}
			sql = "SELECT * FROM tillreportdatapoints WHERE storeID = ? AND MONTH(assignedDate) = ? AND YEAR(assignedDate) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentTillDataPoints.add(new TillReportDataPoint(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}

		double cashTotal = 0;
		double eftposTotal = 0;
		double amexTotal = 0;
		double googleSquareTotal = 0;
		double chequesTotal = 0;
		double medicareTotal = 0;
		double gstTotal = 0;
		double runningTillBalance = 0;
		for(EODDataPoint eod:currentEODDataPoints){
			cashTotal+=eod.getCashAmount();
			eftposTotal+=eod.getEftposAmount();
			amexTotal+=eod.getAmexAmount();
			googleSquareTotal+=eod.getGoogleSquareAmount();
			chequesTotal+=eod.getChequeAmount();
			runningTillBalance+=eod.getTillBalance();
		}
		for(int i=1;i<daysInMonth+1;i++) {
			LocalDate d = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(), i);
			boolean foundMedicare = false;
			boolean foundGST = false;
			for (TillReportDataPoint tdp : currentTillDataPoints) {
				if (tdp.getAssignedDate().equals(d) && tdp.getKey().equals("Govt Recovery")) {
					medicareTotal += tdp.getAmount();
					foundMedicare = true;
				}
				if (tdp.getAssignedDate().equals(d) && tdp.getKey().equals("Total GST Collected")) {
					gstTotal += tdp.getAmount();
					foundGST = true;
				}
				if(foundMedicare&&foundGST){
					break;
				}
			}
		}
		cash1.setText(String.format("%.2f", cashTotal));
		eftpos1.setText(String.format("%.2f", eftposTotal));
		amex1.setText(String.format("%.2f", amexTotal));
		googleSquare1.setText(String.format("%.2f", googleSquareTotal));
		cheque1.setText(String.format("%.2f", chequesTotal));
		medicare1.setText(String.format("%.2f", medicareTotal));
		medicareSpreadsheet.setText(String.format("%.2f", medicareTotal));
		total1.setText(String.format("%.2f", cashTotal+eftposTotal+amexTotal+googleSquareTotal+chequesTotal+medicareTotal));
		gst1.setText(String.format("%.2f", gstTotal));
		gst3.setText(String.format("%.2f", gstTotal));
		for(EODDataPoint e: currentEODDataPoints){
			boolean foundTillReport = false;
			for(TillReportDataPoint t: currentTillDataPoints){
				if(e.getDate().equals(t.getAssignedDate())&&t.getKey().equals("Total Takings")){
					e.calculateTillBalances(t.getAmount(),runningTillBalance);
					foundTillReport = true;
				}
			}
			if(!foundTillReport)
				e.calculateTillBalances(0,runningTillBalance);
			runningTillBalance = e.getRunningTillBalance();
		}
		if(runningTillBalance<0){
			tillBalance.setText(String.format("%.2f", runningTillBalance));
		}else if(runningTillBalance>0){
			tillBalance.setText("0.00");
		}else{
			tillBalance.setText("");
		}

		spreadsheetCheck1.setText(String.format("%.2f", cashTotal+eftposTotal+amexTotal+googleSquareTotal+chequesTotal+medicareTotal));
		spreadsheetCheck2.setText("0.00"); //TODO: implement spreadsheetCheck2 to match monthly summary
		spreadsheetCheck3.setText(String.format("%.2f", Double.parseDouble(spreadsheetCheck2.getText())-Double.parseDouble(spreadsheetCheck1.getText())));


		try {
			sql = "SELECT SUM(unitAmount) AS total FROM invoices WHERE storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			if(resultSet.next()){
				cogsCheck1.setText(String.format("%.2f", resultSet.getDouble("total")));
				cogsCheck2.setText(String.format("%.2f", resultSet.getDouble("total")));
				cogsCheck3.setText("0.00");
			}
		}catch(SQLException e){
				e.printStackTrace();
		}

		//set all textfields to have $ leading icon
		formatTextFields(incomeCheckTable);
		formatTextFields(medicareCheckTable);
		formatTextFields(cogsCheckTable);
		//Add BASChecker values
		try {
			sql = "SELECT * FROM baschecker WHERE storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, main.getCurrentDate().getMonthValue());
			preparedStatement.setInt(3, main.getCurrentDate().getYear());
			resultSet = preparedStatement.executeQuery();
			//check if resultset returns no results
			if (resultSet == null || !resultSet.next()) {
				cash2.setText("0.00");
				eftpos2.setText("0.00");
				amex2.setText("0.00");
				googleSquare2.setText("0.00");
				cheque2.setText("0.00");
//				medicare2.setText("0.00");
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
			}else{
				BASCheckerDataPoint data = new BASCheckerDataPoint(resultSet);
				cash2.setText(String.format("%.2f", data.getCashAdjustment()));
				eftpos2.setText(String.format("%.2f", data.getEftposAdjustment()));
				amex2.setText(String.format("%.2f", data.getAmexAdjustment()));
				googleSquare2.setText(String.format("%.2f", data.getGoogleSquareAdjustment()));
				cheque2.setText(String.format("%.2f", data.getChequeAdjustment()));
//				medicare2.setText(String.format("%.2f", data.getMedicareAdjustment()));
				total2.setText(String.format("%.2f", data.getTotalIncomeAdjustment()));
				cashCorrect.setSelected(data.isCashCorrect());
				eftposCorrect.setSelected(data.isEftposCorrect());
				amexCorrect.setSelected(data.isAmexCorrect());
				googleSquareCorrect.setSelected(data.isGoogleSquareCorrect());
				chequeCorrect.setSelected(data.isChequeCorrect());
				medicareCorrect.setSelected(data.isMedicareCorrect());
				totalIncomeCorrect.setSelected(data.isTotalIncomeCorrect());
				gstCorrect.setSelected(data.isGstCorrect());
				medicareBAS.setText(String.format("%.2f", data.getBasDailyScript()));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
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
		if(cash2.isValid()){
			if(cash2.getText().equals(""))
				cash2.setText("0.00");
			else{
				cash2.setText(String.format("%.2f", Double.parseDouble(cash2.getText())));
			}
			cash3.setText(String.format("%.2f", Double.parseDouble(cash1.getText())+Double.parseDouble(cash2.getText())));
		}
		if(eftpos2.isValid()){
			if(eftpos2.getText().equals(""))
				eftpos2.setText("0.00");
			else{
				eftpos2.setText(String.format("%.2f", Double.parseDouble(eftpos2.getText())));
			}
			eftpos3.setText(String.format("%.2f", Double.parseDouble(eftpos1.getText())+Double.parseDouble(eftpos2.getText())));
		}
		if(amex2.isValid()){
			if(amex2.getText().equals(""))
				amex2.setText("0.00");
			else{
				amex2.setText(String.format("%.2f", Double.parseDouble(amex2.getText())));
			}
			amex3.setText(String.format("%.2f", Double.parseDouble(amex1.getText())+Double.parseDouble(amex2.getText())));
		}
		if(googleSquare2.isValid()){
			if(googleSquare2.getText().equals(""))
				googleSquare2.setText("0.00");
			else{
				googleSquare2.setText(String.format("%.2f", Double.parseDouble(googleSquare2.getText())));
			}
			googleSquare3.setText(String.format("%.2f", Double.parseDouble(googleSquare1.getText())+Double.parseDouble(googleSquare2.getText())));
		}
		if(cheque2.isValid()){
			if(cheque2.getText().equals(""))
				cheque2.setText("0.00");
			else{
				cheque2.setText(String.format("%.2f", Double.parseDouble(cheque2.getText())));
			}
			cheque3.setText(String.format("%.2f", Double.parseDouble(cheque1.getText())+Double.parseDouble(cheque2.getText())));
		}
		if(total2.isValid()){
			if(total2.getText().equals(""))
				total2.setText("0.00");
			else{
				total2.setText(String.format("%.2f", Double.parseDouble(total2.getText())));
			}
			total3.setText(String.format("%.2f", Double.parseDouble(total1.getText())+Double.parseDouble(total2.getText())));
		}
		if(medicareBAS.isValid()){
			if(medicareBAS.getText().equals(""))
				medicareBAS.setText("0.00");
			else{
				medicareBAS.setText(String.format("%.2f", Double.parseDouble(medicareBAS.getText())));
			}
			medicareAdjustment.setText(String.format("%.2f", Double.parseDouble(medicareBAS.getText())-Double.parseDouble(medicareSpreadsheet.getText())));
			medicare2.setText(String.format("%.2f", Double.parseDouble(medicareBAS.getText())-Double.parseDouble(medicareSpreadsheet.getText())));
		}
		if(medicare2.isValid()){
			if(medicare2.getText().equals(""))
				medicare2.setText("0.00");
			else{
				medicare2.setText(String.format("%.2f", Double.parseDouble(medicare2.getText())));
			}
			medicare3.setText(String.format("%.2f", Double.parseDouble(medicare1.getText())+Double.parseDouble(medicare2.getText())));
		}
	}

	public void save(){
		updateTotals();
		//Validate all fields
		if(!cash2.isValid() || !eftpos2.isValid() || !amex2.isValid() || !googleSquare2.isValid() || !cheque2.isValid() || !total2.isValid() || !medicareBAS.isValid()){
			errorLabel.setText("Please ensure all fields are valid");
			errorLabel.setVisible(true);
			return;
		}
		Date date = Date.valueOf(LocalDate.of(main.getCurrentDate().getYear(),main.getCurrentDate().getMonth(),1));
		String sql = "INSERT INTO baschecker(date,storeID,cashAdjustment,eftposAdjustment,amexAdjustment,googleSquareAdjustment,chequesAdjustment,medicareAdjustment,totalIncomeAdjustment,cashCorrect,eftposCorrect,amexCorrect,googleSquareCorrect,chequesCorrect,medicareCorrect,totalIncomeCorrect,gstCorrect,basDailyScript) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
				"ON DUPLICATE KEY UPDATE cashAdjustment=?,eftposAdjustment=?,amexAdjustment=?,googleSquareAdjustment=?,chequesAdjustment=?,medicareAdjustment=?,totalIncomeAdjustment=?,cashCorrect=?,eftposCorrect=?,amexCorrect=?,googleSquareCorrect=?,chequesCorrect=?,medicareCorrect=?,totalIncomeCorrect=?,gstCorrect=?,basDailyScript=?";
		try {
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setDate(1, date);
			preparedStatement.setInt(2, main.getCurrentStore().getStoreID());
			preparedStatement.setDouble(3, Double.parseDouble(cash2.getText()));
			preparedStatement.setDouble(4, Double.parseDouble(eftpos2.getText()));
			preparedStatement.setDouble(5, Double.parseDouble(amex2.getText()));
			preparedStatement.setDouble(6, Double.parseDouble(googleSquare2.getText()));
			preparedStatement.setDouble(7, Double.parseDouble(cheque2.getText()));
			preparedStatement.setDouble(8, Double.parseDouble(medicare2.getText()));
			preparedStatement.setDouble(9, Double.parseDouble(total2.getText()));
			preparedStatement.setBoolean(10, cashCorrect.isSelected());
			preparedStatement.setBoolean(11, eftposCorrect.isSelected());
			preparedStatement.setBoolean(12, amexCorrect.isSelected());
			preparedStatement.setBoolean(13, googleSquareCorrect.isSelected());
			preparedStatement.setBoolean(14, chequeCorrect.isSelected());
			preparedStatement.setBoolean(15, medicareCorrect.isSelected());
			preparedStatement.setBoolean(16, totalIncomeCorrect.isSelected());
			preparedStatement.setBoolean(17, gstCorrect.isSelected());
			preparedStatement.setDouble(18, Double.parseDouble(medicareBAS.getText()));
			preparedStatement.setDouble(19, Double.parseDouble(cash2.getText()));
			preparedStatement.setDouble(20, Double.parseDouble(eftpos2.getText()));
			preparedStatement.setDouble(21, Double.parseDouble(amex2.getText()));
			preparedStatement.setDouble(22, Double.parseDouble(googleSquare2.getText()));
			preparedStatement.setDouble(23, Double.parseDouble(cheque2.getText()));
			preparedStatement.setDouble(24, Double.parseDouble(medicare2.getText()));
			preparedStatement.setDouble(25, Double.parseDouble(total2.getText()));
			preparedStatement.setBoolean(26, cashCorrect.isSelected());
			preparedStatement.setBoolean(27, eftposCorrect.isSelected());
			preparedStatement.setBoolean(28, amexCorrect.isSelected());
			preparedStatement.setBoolean(29, googleSquareCorrect.isSelected());
			preparedStatement.setBoolean(30, chequeCorrect.isSelected());
			preparedStatement.setBoolean(31, medicareCorrect.isSelected());
			preparedStatement.setBoolean(32, totalIncomeCorrect.isSelected());
			preparedStatement.setBoolean(33, gstCorrect.isSelected());
			preparedStatement.setDouble(34, Double.parseDouble(medicareBAS.getText()));
			preparedStatement.executeUpdate();
		} catch (SQLException ex) {
			System.err.println(ex.getMessage());
		}
		LocalTime now = LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss a");
		errorLabel.setVisible(true);
		errorLabel.setText("Last saved at "+now.format(formatter));
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
