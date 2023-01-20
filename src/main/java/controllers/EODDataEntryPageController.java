package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import models.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.controlsfx.control.PopOver;
import utils.*;

import java.io.*;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

public class EODDataEntryPageController extends DateSelectController{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private MainMenuController parent;
    private User selectedUser;
	private PopOver currentDatePopover;
	private ObservableList<EODDataPoint> eodDataPoints = FXCollections.observableArrayList();

	@FXML
	private FlowPane datePickerPane;
    @FXML
    private TableView<EODDataPoint> eodDataTable;
	@FXML
	private VBox editDayPopover;
	@FXML
	private StackPane monthSelector;
	@FXML
	private MFXTextField monthSelectorField;
	@FXML
	private Region contentDarken;
	@FXML
	private Label popoverLabel,tillBalanceLabel,runningTillBalanceLabel;
	@FXML
	private MFXTextField cashField,eftposField,amexField,googleSquareField,chequeField;
	@FXML
	private MFXTextField medschecksField,sohField,sofField,smsPatientsField;
	@FXML
	private Label cashValidationLabel,eftposValidationLabel,amexValidationLabel,googleSquareValidationLabel,chequeValidationLabel;
	@FXML
	private Label medschecksValidationLabel,sohValidationLabel,sofValidationLabel,smsPatientsValidationLabel;
	@FXML
	private TextArea notesField;
	@FXML
	private MFXButton saveButton;
	@FXML
	private MFXScrollPane popOverScroll;
	@FXML
	private Button importDataButton;

	private TableColumn<EODDataPoint,LocalDate> dateCol;
	private TableColumn<EODDataPoint,Double> cashAmountCol;
	private TableColumn<EODDataPoint,Double> eftposAmountCol;
	private TableColumn<EODDataPoint,Double> amexAmountCol;
	private TableColumn<EODDataPoint,Double> googleSquareAmountCol;
	private TableColumn<EODDataPoint,Double> chequeAmountCol;
	private TableColumn<EODDataPoint, Integer> medschecksCol;
	private TableColumn<EODDataPoint, Double> stockOnHandAmountCol;
	private TableColumn<EODDataPoint, Integer> scriptsOnFileCol;
	private TableColumn<EODDataPoint, Integer> smsPatientsCol;
	private TableColumn<EODDataPoint, Double> tillBalanceCol;
	private TableColumn<EODDataPoint, Double> runningTillBalanceCol;
	private TableColumn<EODDataPoint, String> notesCol;

	private double currentTotalTakings;
	private double currentRunningTillBalance;

	
	 @FXML
	private void initialize() throws IOException {}

	@Override
	public void setMain(Main main) {
		this.main = main;
	}
	
	public void setConnection(Connection c) {
		this.con = c;
	}

	public void setParent(MainMenuController p){this.parent = p;}

	@Override
	public void fill() {
	 	//Fix slow scroll on popover scroll pane
		final double SPEED = 0.002;
		popOverScroll.getContent().setOnScroll(scrollEvent -> {
			double deltaY = scrollEvent.getDeltaY() * SPEED;
			popOverScroll.setVvalue(popOverScroll.getVvalue() - deltaY);
		});
		

		ValidatorUtils.setupRegexValidation(cashField,cashValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(eftposField,eftposValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(amexField,amexValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(googleSquareField,googleSquareValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(chequeField,chequeValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(medschecksField,medschecksValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(sohField,sohValidationLabel,ValidatorUtils.CASH_REGEX,ValidatorUtils.CASH_ERROR,"$",saveButton);
		ValidatorUtils.setupRegexValidation(sofField,sofValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);
		ValidatorUtils.setupRegexValidation(smsPatientsField,smsPatientsValidationLabel,ValidatorUtils.INT_REGEX,ValidatorUtils.INT_ERROR,null,saveButton);

		dateCol = new TableColumn<>("DATE");
		cashAmountCol = new TableColumn<>("CASH");
		eftposAmountCol = new TableColumn<>("EFTPOS");
		amexAmountCol = new TableColumn<>("AMEX");
		googleSquareAmountCol = new TableColumn<>("GOOGLE\nSQUARE");
		chequeAmountCol = new TableColumn<>("CHEQUE");
		medschecksCol = new TableColumn<>("MEDSCHECKS");
		stockOnHandAmountCol = new TableColumn<>("STOCK ON\nHAND");
		scriptsOnFileCol = new TableColumn<>("SCRIPTS ON\nFILE");
		smsPatientsCol = new TableColumn<>("SMS PATIENTS");
		tillBalanceCol = new TableColumn<>("TILL BALANCE");
		runningTillBalanceCol = new TableColumn<>("RUNNING TILL\nBALANCE");
		notesCol = new TableColumn<>("NOTES");
		dateCol.setMinWidth(80);

		dateCol.setCellValueFactory(new PropertyValueFactory<>("dateString"));
		cashAmountCol.setCellValueFactory(new PropertyValueFactory<>("cashAmountString"));
		eftposAmountCol.setCellValueFactory(new PropertyValueFactory<>("eftposAmountString"));
		amexAmountCol.setCellValueFactory(new PropertyValueFactory<>("amexAmountString"));
		googleSquareAmountCol.setCellValueFactory(new PropertyValueFactory<>("googleSquareAmountString"));
		chequeAmountCol.setCellValueFactory(new PropertyValueFactory<>("chequeAmountString"));
		medschecksCol.setCellValueFactory(new PropertyValueFactory<>("medschecksString"));
		stockOnHandAmountCol.setCellValueFactory(new PropertyValueFactory<>("stockOnHandAmountString"));
		scriptsOnFileCol.setCellValueFactory(new PropertyValueFactory<>("scriptsOnFileString"));
		smsPatientsCol.setCellValueFactory(new PropertyValueFactory<>("smsPatientsString"));
		tillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("tillBalanceString"));
		runningTillBalanceCol.setCellValueFactory(new PropertyValueFactory<>("runningTillBalanceString"));
		notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

		eodDataTable.getColumns().addAll(
				dateCol,
				cashAmountCol,
				eftposAmountCol,
				amexAmountCol,
				googleSquareAmountCol,
				chequeAmountCol,
				medschecksCol,
				stockOnHandAmountCol,
				scriptsOnFileCol,
				smsPatientsCol,
				tillBalanceCol,
				runningTillBalanceCol,
				notesCol
		);
		setDate(LocalDate.now());
		eodDataTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		eodDataTable.setMaxWidth(Double.MAX_VALUE);
		eodDataTable.setMaxHeight(Double.MAX_VALUE);
		eodDataTable.setFixedCellSize(25.0);
		VBox.setVgrow(eodDataTable, Priority.ALWAYS);
		for(TableColumn tc: eodDataTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+40);
		}
		cashField.textProperty().addListener(observable -> updatePopoverTillBalance());
		eftposField.textProperty().addListener(observable -> updatePopoverTillBalance());
		amexField.textProperty().addListener(observable -> updatePopoverTillBalance());
		googleSquareField.textProperty().addListener(observable -> updatePopoverTillBalance());
		chequeField.textProperty().addListener(observable -> updatePopoverTillBalance());
		Platform.runLater(() -> GUIUtils.customResize(eodDataTable,notesCol));
		Platform.runLater(() -> addDoubleClickfunction());
	}

	private void updatePopoverTillBalance() {
		 double tillBalanceTotal = 0;
		 if(cashField.isValid()) tillBalanceTotal += Double.valueOf(cashField.getText());
		 if(eftposField.isValid()) tillBalanceTotal += Double.valueOf(eftposField.getText());
		 if(amexField.isValid()) tillBalanceTotal += Double.valueOf(amexField.getText());
		 if(googleSquareField.isValid()) tillBalanceTotal += Double.valueOf(googleSquareField.getText());
		 if(chequeField.isValid()) tillBalanceTotal += Double.valueOf(chequeField.getText());
		tillBalanceTotal-=currentTotalTakings;
		tillBalanceLabel.setText(NumberFormat.getCurrencyInstance().format(tillBalanceTotal));
		runningTillBalanceLabel.setText(NumberFormat.getCurrencyInstance().format(currentRunningTillBalance+tillBalanceTotal));
	}

	private void addDoubleClickfunction(){
		eodDataTable.setRowFactory( tv -> {
			TableRow<EODDataPoint> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					EODDataPoint rowData = row.getItem();
					openEODPopover(rowData);
				}
			});
			return row ;
		});
	}

	public void importFiles(LocalDate targetDate) throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data entry File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XLS Files", "*.xls"));
		File newfile = fileChooser.showOpenDialog(main.getStg());
		if(newfile!=null){
			FileInputStream file = new FileInputStream(newfile);
			HSSFWorkbook workbook = new HSSFWorkbook(file);
			WorkbookProcessor wbp = new WorkbookProcessor(workbook);
			//TODO: Verify store is correct
			//TODO: Verify if period overlaps
			for(CellDataPoint cdp : wbp.getDataPoints()){
				String sql = "INSERT INTO tillReportDatapoints(storeID,assignedDate,periodStartDate,periodEndDate,`key`,quantity,amount) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE periodStartDate=?,periodEndDate=?,quantity=?,amount=?";
				try {
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
					preparedStatement.setDate(2, (cdp.getAssignedDate()==null)?Date.valueOf(targetDate): Date.valueOf(cdp.getAssignedDate()));
					preparedStatement.setObject(3, (wbp.getPeriodStart()!=null)?wbp.getPeriodStart().atZone(ZoneId.of("Australia/Melbourne")):null);
					preparedStatement.setObject(4, (wbp.getPeriodEnd()!=null)?wbp.getPeriodEnd().atZone(ZoneId.of("Australia/Melbourne")):null);
					preparedStatement.setString(5,cdp.getCategory()+((cdp.getSubCategory()!="")?"-"+cdp.getSubCategory():""));
					preparedStatement.setDouble(6,cdp.getQuantity());
					preparedStatement.setDouble(7,cdp.getAmount());
					preparedStatement.setObject(8, (wbp.getPeriodStart()!=null)?wbp.getPeriodStart().atZone(ZoneId.of("Australia/Melbourne")):null);
					preparedStatement.setObject(9, (wbp.getPeriodEnd()!=null)?wbp.getPeriodEnd().atZone(ZoneId.of("Australia/Melbourne")):null);
					preparedStatement.setDouble(10,cdp.getQuantity());
					preparedStatement.setDouble(11,cdp.getAmount());
					preparedStatement.executeUpdate();
				} catch (SQLException ex) {
					System.err.println(ex.getMessage());
				}
			}
		}
	}

	public void monthForward() {
		setDate(main.getCurrentDate().plusMonths(1));
	}

	public void monthBackward() {
		setDate(main.getCurrentDate().minusMonths(1));
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

	public void fillTable(){
	 	eodDataPoints = FXCollections.observableArrayList();
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		ObservableList<EODDataPoint> currentEODDataPoints = FXCollections.observableArrayList();
		ObservableList<TillReportDataPoint> currentTillReportDataPoints = FXCollections.observableArrayList();
		String sql = null;
		try {
			sql = "SELECT * FROM eodDataPoints where storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentEODDataPoints.add(new EODDataPoint(resultSet));
			}
			sql = "SELECT * FROM tillreportdatapoints where storeID = ? AND MONTH(assignedDate) = ? AND YEAR(assignedDate) = ? AND `key` = ?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setInt(2, yearMonthObject.getMonthValue());
			preparedStatement.setInt(3, yearMonthObject.getYear());
			preparedStatement.setString(4, "Total Takings");
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentTillReportDataPoints.add(new TillReportDataPoint(resultSet));
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}

		for(int i = 1; i<daysInMonth+1; i++){
			Boolean dateAlreadyCreated = false;
			for(EODDataPoint e: currentEODDataPoints){
				if(e.getDate().getDayOfMonth()==i){
					eodDataPoints.add(e);
					dateAlreadyCreated = true;
				}
			}
			if(!dateAlreadyCreated)
				eodDataPoints.add(new EODDataPoint(LocalDate.of(main.getCurrentDate().getYear(),main.getCurrentDate().getMonth(),i)));
		}
		double runningTillBalance = 0;
		for(EODDataPoint e: eodDataPoints){
			boolean foundTillReport = false;
			for(TillReportDataPoint t: currentTillReportDataPoints){
				if(e.getDate().equals(t.getAssignedDate())){
					e.calculateTillBalances(t.getAmount(),runningTillBalance);
					foundTillReport = true;
				}
			}
			if(!foundTillReport)
				e.calculateTillBalances(0,runningTillBalance);
			runningTillBalance = e.getRunningTillBalance();
		}
		eodDataTable.setItems(eodDataPoints);
		addDoubleClickfunction();
	}

	public void openEODPopover(EODDataPoint e){
		contentDarken.setVisible(true);
		AnimationUtils.slideIn(editDayPopover,0);
		popoverLabel.setText("Modify EOD Values for " + e.getDateString());
		cashField.setText(String.valueOf(e.getCashAmount()));
		eftposField.setText(String.valueOf(e.getEftposAmount()));
		amexField.setText(String.valueOf(e.getAmexAmount()));
		googleSquareField.setText(String.valueOf(e.getGoogleSquareAmount()));
		chequeField.setText(String.valueOf(e.getChequeAmount()));
		tillBalanceLabel.setText(e.getTillBalanceString());
		runningTillBalanceLabel.setText(e.getRunningTillBalanceString());
		medschecksField.setText(String.valueOf(e.getMedschecks()));
		sohField.setText(String.valueOf(e.getStockOnHandAmount()));
		sofField.setText(String.valueOf(e.getScriptsOnFile()));
		smsPatientsField.setText(String.valueOf(e.getSmsPatients()));
		notesField.setText((e.getNotes()==null || e.getNotes().isBlank())?"":String.valueOf(e.getNotes()));
		saveButton.setOnAction(actionEvent -> editEODEntry(e));
		currentTotalTakings = 0;
		try {
			String sql = "SELECT * FROM tillreportdatapoints where storeID = ? AND assignedDate = ? AND `key`=?";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
			preparedStatement.setDate(2, Date.valueOf(e.getDate()));
			preparedStatement.setString(3, "Total Takings");
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				currentTotalTakings = resultSet.getDouble("amount");
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		currentRunningTillBalance = e.getRunningTillBalance()-e.getTillBalance();
		updatePopoverTillBalance();
		importDataButton.setOnAction(actionEvent -> {
			try {importFiles(e.getDate());
			} catch (IOException ex) {throw new RuntimeException(ex);}
		});
	}

	public void closePopover(){
		AnimationUtils.slideIn(editDayPopover,425);
		contentDarken.setVisible(false);
	}

	public void editEODEntry(EODDataPoint e){
		double cashValue = Double.parseDouble(cashField.getText());
		double eftposValue = Double.parseDouble(eftposField.getText());
		double amexValue = Double.parseDouble(amexField.getText());
		double googleSquareValue = Double.parseDouble(googleSquareField.getText());
		double chequeValue = Double.parseDouble(chequeField.getText());
		int medschecksValue = Integer.parseInt(medschecksField.getText());
		double sohValue = Double.parseDouble(sohField.getText());
		int sofValue = Integer.parseInt(sofField.getText());
		int smsPatientsValue = Integer.parseInt(smsPatientsField.getText());
		String notesValue = notesField.getText();

		String sql;
		if(e.isInDB()){
			sql = "UPDATE eodDataPoints SET cash=?,eftpos=?,amex=?,googleSquare=?,cheque=?,medschecks=?,scriptsOnFile=?,stockOnHand=?,smsPatients=?,notes=? WHERE date = ? AND storeID = ?";
		}else{
			sql = "INSERT INTO eodDataPoints(cash, eftpos, amex, googleSquare, cheque, medschecks, scriptsOnFile, stockOnHand, smsPatients, notes, date, storeID) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
		}

		try {
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setDouble(1, cashValue);
			preparedStatement.setDouble(2, eftposValue);
			preparedStatement.setDouble(3, amexValue);
			preparedStatement.setDouble(4, googleSquareValue);
			preparedStatement.setDouble(5, chequeValue);
			preparedStatement.setInt(6, medschecksValue);
			preparedStatement.setInt(7, sofValue);
			preparedStatement.setDouble(8, sohValue);
			preparedStatement.setInt(9, smsPatientsValue);
			preparedStatement.setString(10, notesValue);
			preparedStatement.setDate(11, Date.valueOf(e.getDate()));
			preparedStatement.setInt(12, main.getCurrentStore().getStoreID());
			preparedStatement.executeUpdate();
			Dialog<String> dialog = new Dialog<String>();
			dialog.setTitle("Success");
			ButtonType type = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
			dialog.setContentText("EOD data was succesfully added to database");
			dialog.getDialogPane().getButtonTypes().add(type);
			dialog.showAndWait();
			fillTable();
		} catch (SQLException ex) {
			System.err.println(ex.getMessage());
		}

	}

	@Override
	public void setDate(LocalDate date) {
		main.setCurrentDate(date);
		String fieldText = main.getCurrentDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		fieldText += ", ";
		fieldText += main.getCurrentDate().getYear();
		monthSelectorField.setText(fieldText);
		fillTable();
	}

	public void exportToXero() throws FileNotFoundException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Choose export save location");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		File file = fileChooser.showSaveDialog(main.getStg());
		if (file != null) {
			try (PrintWriter pw = new PrintWriter(file)) {
				pw.println("*ContactName,Day Of Month,Amount,No. of scripts,Total customers served,"+
						"Total Sales (#),Total Govt Contribution ($),Total Takings,Gross Profit ($)," +
						"Total GST Free Sales,*InvoiceNumber,Total GST Sales,*InvoiceDate,*DueDate," +
						"Total GST Collected,*Description,*Quantity,*UnitAmount,Total OTC Sales (#)," +
						"Avg. OTC  Sales Per Customer ($),*AccountCode,*TaxType,Z Dispense Govt Cont," +
						"Stock on hand,Scripts on file count,SMS patients,Clinical Interventions,Medschecks," +
						"Till Balance,Running till Balance,Notes");

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
				double runningTillBalance = 0;
				for(int i=1;i<daysInMonth+1;i++){
					LocalDate d = LocalDate.of(yearMonthObject.getYear(),yearMonthObject.getMonth(),i);
					EODDataPoint e = null;
					for(EODDataPoint eod:currentEODDataPoints){
						if(eod.getDate().equals(d)){
							e=eod;
							break;
						}
					}
					if(e==null){
						e = new EODDataPoint(false,d,main.getCurrentStore().getStoreID(),0,0,0,0,0,0,0,0,0,0,0,"");
					}
					pw.print("Cash Income,"+i+",");
					pw.print(e.getCashAmount()+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Script Count","quantity")+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Total Customers Served","quantity")+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Total Sales","quantity")+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Total Government Contribution","amount")+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Total Takings","amount")+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Gross Profit ($)","amount")+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Total GST Free Sales","quantity")+",");
					DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
					String formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getCashAmount()>0)?formattedDate+"c,":",");
					pw.print(searchTillData(currentTillDataPoints,d,"Total GST Sales","amount")+",");
					dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getCashAmount()>0)?formattedDate+",":",");
					LocalDate lastDayOfMonth  = d.withDayOfMonth(d.getMonth().length(d.isLeapYear()));
					dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					formattedDate = lastDayOfMonth.format(dateTimeFormatter);
					pw.print((e.getCashAmount()>0)?formattedDate+",":",");
					pw.print(searchTillData(currentTillDataPoints,d,"Total GST Collected","amount")+",");
					pw.print("Cash Income,1,");
					pw.print(e.getCashAmount()+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Total Sales-OTC Sales","quantity")+",");
					pw.print(searchTillData(currentTillDataPoints,d,"Avg. OTC Sales Per Customer","amount")+",");
					pw.print("200,GST Free Income,");
					pw.print(searchTillData(currentTillDataPoints,d,"Govt Recovery","amount")+",");
					pw.print(e.getStockOnHandAmount()+",");
					pw.print(e.getScriptsOnFile()+",");
					pw.print(e.getSmsPatients()+",");
					pw.print(",");//Clinical interventions
					pw.print(e.getMedschecks()+",");
					double totalTakings = 0;
					try{
						totalTakings = Double.parseDouble(searchTillData(currentTillDataPoints,d,"Total Takings","amount"));
					}catch(NumberFormatException ignored){}
					double tillBalance = e.getCashAmount()+e.getEftposAmount()+e.getAmexAmount()+e.getGoogleSquareAmount()+e.getChequeAmount() - totalTakings;
					pw.print(NumberFormat.getCurrencyInstance().format(tillBalance)+",");
					runningTillBalance+=tillBalance;
					pw.print(NumberFormat.getCurrencyInstance().format(runningTillBalance)+",");
					pw.println(e.getNotes());

					pw.print("Eftpos Income,"+i+",");
					pw.print(e.getEftposAmount()+",,,,,,,,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getEftposAmount()>0)?formattedDate+"e,,":",,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getEftposAmount()>0)?formattedDate+",":",");
					formattedDate = lastDayOfMonth.format(dateTimeFormatter);
					pw.print((e.getEftposAmount()>0)?formattedDate+",,":",,");
					pw.println("Eftpos Income,1,"+e.getEftposAmount()+",,,200,GST Free Income");

					pw.print("Amex Income,"+i+",");
					pw.print(e.getAmexAmount()+",,,,,,,,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getAmexAmount()>0)?formattedDate+"a,,":",,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getAmexAmount()>0)?formattedDate+",":",");
					formattedDate = lastDayOfMonth.format(dateTimeFormatter);
					pw.print((e.getAmexAmount()>0)?formattedDate+",,":",,");
					pw.println("Amex Income,1,"+e.getAmexAmount()+",,,200,GST Free Income");

					pw.print("Google Square Income,"+i+",");
					pw.print(e.getGoogleSquareAmount()+",,,,,,,,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getGoogleSquareAmount()>0)?formattedDate+"gs,,":",,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getGoogleSquareAmount()>0)?formattedDate+",":",");
					formattedDate = lastDayOfMonth.format(dateTimeFormatter);
					pw.print((e.getGoogleSquareAmount()>0)?formattedDate+",,":",,");
					pw.println("Google Square Income,1,"+e.getGoogleSquareAmount()+",,,200,GST Free Income");

					pw.print("Cheques Income,"+i+",");
					pw.print(e.getChequeAmount()+",,,,,,,,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getChequeAmount()>0)?formattedDate+"ch,,":",,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((e.getChequeAmount()>0)?formattedDate+",":",");
					formattedDate = lastDayOfMonth.format(dateTimeFormatter);
					pw.print((e.getChequeAmount()>0)?formattedDate+",,":",,");
					pw.println("Cheques Income,1,"+e.getChequeAmount()+",,,200,GST Free Income");
					double govtRecovery = 0;
					try{
						govtRecovery = Double.parseDouble(searchTillData(currentTillDataPoints,d,"Govt Recovery","amount"));
					}catch(NumberFormatException ignored){}

					pw.print("Medicare PBS (Ex GST),"+i+",");
					pw.print(govtRecovery+",,,,,,,,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((govtRecovery>0)?formattedDate+"ch,,":",,");
					dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					formattedDate = d.format(dateTimeFormatter);
					pw.print((govtRecovery>0)?formattedDate+",":",");
					formattedDate = lastDayOfMonth.format(dateTimeFormatter);
					pw.print((govtRecovery>0)?formattedDate+",,":",,");
					pw.println("Medicare PBS (Ex GST),1,"+govtRecovery+",,,200,GST Free Income");
				}
				Dialog<String> dialog = new Dialog<String>();
				dialog.setTitle("Success");
				ButtonType type = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
				dialog.setContentText("EOD data was succesfully added to database");
				dialog.getDialogPane().getButtonTypes().add(type);
				dialog.showAndWait();
			} catch (FileNotFoundException e){
				System.err.println(e.getMessage());
			}
		}
	}

	private String searchTillData(ObservableList<TillReportDataPoint> dataPoints,LocalDate date, String key,String field){
		 for(TillReportDataPoint t:dataPoints){
			 if(t.getAssignedDate().equals(date)&&t.getKey().equals(key)){
				 if(field.equals("quantity")){
					 return String.valueOf(t.getQuantity());
				 }else{
					 return String.valueOf(t.getAmount());
				 }
			 }
		 }
		 return "";
	}
}
