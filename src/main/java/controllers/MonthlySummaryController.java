package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.*;
import models.*;
import org.controlsfx.control.PopOver;
import utils.GUIUtils;
import utils.TableUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

public class MonthlySummaryController extends DateSelectController{

	@FXML
	private StackPane monthSelector;
	@FXML
	private MFXTextField monthSelectorField;
	@FXML
	private FlowPane datePickerPane;
	@FXML
	private StackPane backgroundPane;
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
	private TableColumn<?, ?> rentAndOngoingsCol;
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

	private PopOver currentDatePopover;

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;

	private ObservableList<MonthlySummaryDataPoint> monthlySummaryPoints = FXCollections.observableArrayList();
	
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
		summaryTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		summaryTable.setFixedCellSize(25.0);
		VBox.setVgrow(summaryTable, Priority.ALWAYS);
		for(TableColumn tc: summaryTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		Platform.runLater(() -> GUIUtils.customResize(summaryTable,runningTillBalanceCol));

	}

	public void exportFiles(){}

	public void importFiles(){}

	public void monthForward() {setDate(main.getCurrentDate().plusMonths(1));
	}

	public void monthBackward() {
		setDate(main.getCurrentDate().minusMonths(1));
	}

	public void fillTable(){
//		System.out.println("fill table method called");
		monthlySummaryPoints = FXCollections.observableArrayList();
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		ObservableList<TillReportDataPoint> currentTillReportDataPoints = FXCollections.observableArrayList();
		ObservableList<EODDataPoint> currentEODDataPoints = FXCollections.observableArrayList();
		String sql = null;
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
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}

		for(int i = 1; i<daysInMonth+1; i++){
			LocalDate d = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(),i);
			monthlySummaryPoints.add(new MonthlySummaryDataPoint(d,currentTillReportDataPoints,currentEODDataPoints,monthlySummaryPoints));
		}
		summaryTable.setItems(monthlySummaryPoints);
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
}



