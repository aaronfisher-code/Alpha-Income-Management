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
import javafx.scene.layout.*;
import models.*;
import org.controlsfx.control.PopOver;
import utils.GUIUtils;
import utils.RosterUtils;
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

	private ObservableList<MonthlySummaryDataPoint> monthlySummaryPoints = FXCollections.observableArrayList();
	
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
		for(TableColumn tc: summaryTable.getColumns()){
			if(tc.getGraphic()!=null){
				Label l = (Label) tc.getGraphic();
				tc.setPrefWidth(TableUtils.getColumnWidth(l)+30);
				l.setMinWidth(TableUtils.getColumnWidth(l)+30);
			}else{
				tc.setPrefWidth(TableUtils.getColumnWidth(tc)+50);
			}

		}

		totalsDateCol.setCellValueFactory(new PropertyValueFactory<>("dateString"));
		//Add center alignment to date column

		totalsDurationCol.setCellValueFactory(new PropertyValueFactory<>("dayDuration"));
		totalsCustomersCol.setCellValueFactory(new PropertyValueFactory<>("noOfCustomersString"));
		totalsItemsCol.setCellValueFactory(new PropertyValueFactory<>("noOfItemsString"));
		totalsScriptsCol.setCellValueFactory(new PropertyValueFactory<>("noOfScriptsString"));
		totalsDollarPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("dollarPerCustomerString"));
		totalsItemsPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("itemsPerCustomerString"));
		totalsOtcDollarPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("otcDollarPerCustomerString"));
		totalsOtcItemsCol.setCellValueFactory(new PropertyValueFactory<>("noOfOTCItemsString"));
		totalsOtcPerCustomerCol.setCellValueFactory(new PropertyValueFactory<>("otcPerCustomerString"));
		totalsTotalIncomeCol.setCellValueFactory(new PropertyValueFactory<>("totalIncomeString"));
		totalsGpDollarCol.setCellValueFactory(new PropertyValueFactory<>("gpDollarsString"));
		totalsGpPercentCol.setCellValueFactory(new PropertyValueFactory<>("gpPercentageString"));
		totalsWagesCol.setCellValueFactory(new PropertyValueFactory<>("wagesString"));
		totalsRentAndOutgoingsCol.setCellValueFactory(new PropertyValueFactory<>("rentAndOutgoingsString"));
		totalsRunningZProfitCol.setCellValueFactory(new PropertyValueFactory<>("runningZProfitString"));
		totalsZReportProfitCol.setCellValueFactory(new PropertyValueFactory<>("zReportProfitString"));
		totalsTillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("tillBalanceString"));
		totalsRunningTillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("runningTillBalanceString"));

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
			if (node instanceof ScrollBar) {
				ScrollBar bar = (ScrollBar) node;

				if (bar.getOrientation().equals(Orientation.HORIZONTAL)) {
					result = bar;
				}
			}
		}

		return result;
	}

	public void exportFiles(){}

	public void importFiles(){}

	public void monthForward() {setDate(main.getCurrentDate().plusMonths(1));
	}

	public void monthBackward() {
		setDate(main.getCurrentDate().minusMonths(1));
	}

	public void fillTable(){
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

		RosterUtils rosterUtils = new RosterUtils(con,main,yearMonthObject);
		for(int i = 1; i<daysInMonth+1; i++){
			LocalDate d = LocalDate.of(yearMonthObject.getYear(), yearMonthObject.getMonth(),i);
			monthlySummaryPoints.add(new MonthlySummaryDataPoint(d,currentTillReportDataPoints,currentEODDataPoints,monthlySummaryPoints,rosterUtils));
		}
		summaryTable.setItems(monthlySummaryPoints);
		totalsTable.getItems().clear();
		totalsTable.getItems().add(new MonthlySummaryDataPoint(monthlySummaryPoints, true));
		totalsTable.getItems().add(new MonthlySummaryDataPoint(monthlySummaryPoints, false));
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
}



