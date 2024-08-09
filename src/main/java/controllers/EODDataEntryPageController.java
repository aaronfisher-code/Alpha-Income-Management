package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import io.github.palexdev.materialfx.controls.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import models.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.controlsfx.control.PopOver;
import services.EODService;
import services.TillReportService;
import utils.*;

import java.io.*;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;

public class EODDataEntryPageController extends DateSelectController{

    private Main main;
	private PopOver currentDatePopover;

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
	private Label popoverLabel,tillBalanceLabel,runningTillBalanceLabel,subheading;
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
	private Button importDataButton,xeroExportButton;
	@FXML
	private DialogPane dialogPane;

    private TableColumn<EODDataPoint, String> notesCol;

	private double currentTotalTakings;
	private double currentRunningTillBalance;
	private EODService eodService;
	private TillReportService tillReportService;

	 @FXML
	private void initialize(){
		 eodService = new EODService();
		 tillReportService = new TillReportService();
	 }

	@Override
	public void setMain(Main main) {
		this.main = main;
	}

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

        TableColumn<EODDataPoint, LocalDate> dateCol = new TableColumn<>("DATE");
        TableColumn<EODDataPoint, Double> cashAmountCol = new TableColumn<>("CASH");
        TableColumn<EODDataPoint, Double> eftposAmountCol = new TableColumn<>("EFTPOS");
        TableColumn<EODDataPoint, Double> amexAmountCol = new TableColumn<>("AMEX");
        TableColumn<EODDataPoint, Double> googleSquareAmountCol = new TableColumn<>("GOOGLE\nSQUARE");
        TableColumn<EODDataPoint, Double> chequeAmountCol = new TableColumn<>("CHEQUE");
        TableColumn<EODDataPoint, Integer> medschecksCol = new TableColumn<>("MEDSCHECKS");
        TableColumn<EODDataPoint, Double> stockOnHandAmountCol = new TableColumn<>("STOCK ON\nHAND");
        TableColumn<EODDataPoint, Integer> scriptsOnFileCol = new TableColumn<>("SCRIPTS ON\nFILE");
        TableColumn<EODDataPoint, Integer> smsPatientsCol = new TableColumn<>("SMS PATIENTS");
        TableColumn<EODDataPoint, Double> tillBalanceCol = new TableColumn<>("TILL BALANCE");
        TableColumn<EODDataPoint, Double> runningTillBalanceCol = new TableColumn<>("RUNNING TILL\nBALANCE");
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
		setDate(main.getCurrentDate());
		eodDataTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		eodDataTable.setMaxWidth(Double.MAX_VALUE);
		eodDataTable.setMaxHeight(Double.MAX_VALUE);
		eodDataTable.setFixedCellSize(25.0);
		VBox.setVgrow(eodDataTable, Priority.ALWAYS);
		for(TableColumn<EODDataPoint, ?> tc: eodDataTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+40);
		}
		cashField.textProperty().addListener(_ -> updatePopoverTillBalance());
		eftposField.textProperty().addListener(_ -> updatePopoverTillBalance());
		amexField.textProperty().addListener(_ -> updatePopoverTillBalance());
		googleSquareField.textProperty().addListener(_ -> updatePopoverTillBalance());
		chequeField.textProperty().addListener(_ -> updatePopoverTillBalance());
		Platform.runLater(() -> GUIUtils.customResize(eodDataTable,notesCol));
		if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("EOD - Edit"))){
			Platform.runLater(this::addDoubleClickfunction);
		}else{
			subheading.setVisible(false);
		}
        xeroExportButton.setVisible(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("EOD - Export")));
	}

	private void updatePopoverTillBalance() {
		 double tillBalanceTotal = 0;
		 if(cashField.isValid()) tillBalanceTotal += Double.parseDouble(cashField.getText());
		 if(eftposField.isValid()) tillBalanceTotal += Double.parseDouble(eftposField.getText());
		 if(amexField.isValid()) tillBalanceTotal += Double.parseDouble(amexField.getText());
		 if(googleSquareField.isValid()) tillBalanceTotal += Double.parseDouble(googleSquareField.getText());
		 if(chequeField.isValid()) tillBalanceTotal += Double.parseDouble(chequeField.getText());
		tillBalanceTotal-=currentTotalTakings;
		tillBalanceLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(tillBalanceTotal));
		runningTillBalanceLabel.setText(NumberFormat.getCurrencyInstance(Locale.US).format(currentRunningTillBalance+tillBalanceTotal));
	}

	private void addDoubleClickfunction(){
		eodDataTable.setRowFactory(_ -> {
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

	public void importFiles(LocalDate targetDate) throws IOException, SQLException {
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
				tillReportService.importTillReportDataPoint(cdp,wbp,targetDate,main.getCurrentStore().getStoreID());
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
			} catch (IOException ex) {
				dialogPane.showError("Failed to open month selector", ex.getMessage());
				ex.printStackTrace();
			}
			MonthYearSelectorContentController rdc = loader.getController();
			rdc.setMain(main);
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
		ObservableList<EODDataPoint> eodDataPoints = FXCollections.observableArrayList();
		YearMonth yearMonthObject = YearMonth.of(main.getCurrentDate().getYear(), main.getCurrentDate().getMonth());
		int daysInMonth = yearMonthObject.lengthOfMonth();

		try {
			List<EODDataPoint> currentEODDataPoints = eodService.getEODDataPoints(
					main.getCurrentStore().getStoreID(),
					yearMonthObject.atDay(1),
					yearMonthObject.atEndOfMonth()
			);

			List<TillReportDataPoint> currentTillReportDataPoints = tillReportService.getTillReportDataPointsByKey(
					main.getCurrentStore().getStoreID(),
					yearMonthObject.atDay(1),
					yearMonthObject.atEndOfMonth(),
					"Total Takings"
			);

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
			if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("EOD - Edit"))){
				addDoubleClickfunction();
			}else{
				subheading.setVisible(false);
			}
		} catch (SQLException ex) {
			dialogPane.showError("Failed to fill table", ex.getMessage());
			ex.printStackTrace();
		}
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
		saveButton.setOnAction(_ -> editEODEntry(e));
		currentTotalTakings = 0;
		try {
			tillReportService.getTillReportDataPointsByKey(main.getCurrentStore().getStoreID(),e.getDate(),e.getDate(),"Total Takings").forEach(t -> currentTotalTakings += t.getAmount());
		} catch (SQLException throwables) {
			dialogPane.showError("Failed to get total takings", throwables.getMessage());
			throwables.printStackTrace();
		}
		currentRunningTillBalance = e.getRunningTillBalance()-e.getTillBalance();
		updatePopoverTillBalance();
		importDataButton.setOnAction(_ -> {
			try {
				importFiles(e.getDate());
			} catch (IOException|SQLException ex) {
				dialogPane.showError("Failed to import data", ex.getMessage());
				ex.printStackTrace();
			}
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
		e.setCashAmount(cashValue);
		e.setEftposAmount(eftposValue);
		e.setAmexAmount(amexValue);
		e.setGoogleSquareAmount(googleSquareValue);
		e.setChequeAmount(chequeValue);
		e.setMedschecks(medschecksValue);
		e.setStockOnHandAmount(sohValue);
		e.setScriptsOnFile(sofValue);
		e.setSmsPatients(smsPatientsValue);
		e.setNotes(notesValue);

		try {
			if(e.isInDB()){
				eodService.updateEODDataPoint(e);
				dialogPane.showInformation("Success","EOD data was succesfully edited");
			}else{
				eodService.insertEODDataPoint(new EODDataPoint(true,e.getDate(),main.getCurrentStore().getStoreID(),cashValue,eftposValue,amexValue,googleSquareValue,chequeValue,medschecksValue,sohValue,sofValue,smsPatientsValue,0,0,notesValue));
				dialogPane.showInformation("Success","EOD data was succesfully added");
			}
			fillTable();
		} catch (SQLException ex) {
			dialogPane.showError("Failed to edit EOD entry", ex.getMessage());
			ex.printStackTrace();
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

	public void exportToXero(){
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

				List<EODDataPoint> currentEODDataPoints = FXCollections.observableArrayList();
				List<TillReportDataPoint> currentTillDataPoints = FXCollections.observableArrayList();
				try {
					currentEODDataPoints = eodService.getEODDataPoints(
							main.getCurrentStore().getStoreID(),
							yearMonthObject.atDay(1),
							yearMonthObject.atEndOfMonth()
					);
					currentTillDataPoints = tillReportService.getTillReportDataPoints(
							main.getCurrentStore().getStoreID(),
							yearMonthObject.atDay(1),
							yearMonthObject.atEndOfMonth()
					);
				} catch (SQLException ex) {
					dialogPane.showError("Failed to get EOD data", ex.getMessage());
					ex.printStackTrace();
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
					}catch(NumberFormatException ex){
						dialogPane.showError("Failed to get total takings", "Total takings for "+d+" could not be found");
						ex.printStackTrace();
					}
					double tillBalance = e.getCashAmount()+e.getEftposAmount()+e.getAmexAmount()+e.getGoogleSquareAmount()+e.getChequeAmount() - totalTakings;
					pw.print(NumberFormat.getCurrencyInstance(Locale.US).format(tillBalance)+",");
					runningTillBalance+=tillBalance;
					pw.print(NumberFormat.getCurrencyInstance(Locale.US).format(runningTillBalance)+",");
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
					}catch(NumberFormatException ex){
						dialogPane.showError("Failed to get govt recovery", "Govt recovery for "+d+" could not be found");
						ex.printStackTrace();
					}

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
				dialogPane.showInformation("Success","EOD data was succesfully exported in Xero format");
			} catch (FileNotFoundException ex){
				dialogPane.showError("Failed to export data", ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	private String searchTillData(List<TillReportDataPoint> dataPoints,LocalDate date, String key,String field){
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
