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
		setDate(LocalDate.now());
	}

	public void updateValues(){
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
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
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
