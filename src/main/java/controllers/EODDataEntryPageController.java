package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import models.CellDataPoint;
import models.EODDataPoint;
import models.User;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.controlsfx.control.PopOver;
import utils.AnimationUtils;
import utils.ValidatorUtils;
import utils.WorkbookProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
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
    private MFXTableView<EODDataPoint> eodDataTable;
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

	private MFXTableColumn<EODDataPoint> dateCol;
	private MFXTableColumn<EODDataPoint> cashAmountCol;
	private MFXTableColumn<EODDataPoint> eftposAmountCol;
	private MFXTableColumn<EODDataPoint> amexAmountCol;
	private MFXTableColumn<EODDataPoint> googleSquareAmountCol;
	private MFXTableColumn<EODDataPoint> chequeAmountCol;
	private MFXTableColumn<EODDataPoint> medschecksCol;
	private MFXTableColumn<EODDataPoint> stockOnHandAmountCol;
	private MFXTableColumn<EODDataPoint> scriptsOnFileCol;
	private MFXTableColumn<EODDataPoint> smsPatientsCol;
	private MFXTableColumn<EODDataPoint> tillBalanceCol;
	private MFXTableColumn<EODDataPoint> runningTillBalanceCol;
	private MFXTableColumn<EODDataPoint> notesCol;

	private static final String[] digits = "0 1 2 3 4 5 6 7 8 9".split(" ");
	private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

	
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

		eodDataTable.autosizeColumnsOnInitialization();

		dateCol = new MFXTableColumn<>("DATE",true, Comparator.comparing(EODDataPoint::getDate));
		cashAmountCol = new MFXTableColumn<>("CASH",true, Comparator.comparing(EODDataPoint::getCashAmount));
		eftposAmountCol = new MFXTableColumn<>("EFTPOS",true, Comparator.comparing(EODDataPoint::getEftposAmount));
		amexAmountCol = new MFXTableColumn<>("AMEX",true, Comparator.comparing(EODDataPoint::getAmexAmount));
		googleSquareAmountCol = new MFXTableColumn<>("GOOGLE SQUARE",true, Comparator.comparing(EODDataPoint::getGoogleSquareAmount));
		chequeAmountCol = new MFXTableColumn<>("CHEQUE",true, Comparator.comparing(EODDataPoint::getChequeAmount));
		medschecksCol = new MFXTableColumn<>("MEDSCHECKS",true, Comparator.comparing(EODDataPoint::getMedschecks));
		stockOnHandAmountCol = new MFXTableColumn<>("STOCK ON HAND",true, Comparator.comparing(EODDataPoint::getStockOnHandAmount));
		scriptsOnFileCol = new MFXTableColumn<>("SCRIPTS ON FILE",true, Comparator.comparing(EODDataPoint::getScriptsOnFile));
		smsPatientsCol = new MFXTableColumn<>("SMS PATIENTS",true, Comparator.comparing(EODDataPoint::getSmsPatients));
		tillBalanceCol = new MFXTableColumn<>("TILL BALANCE",true, Comparator.comparing(EODDataPoint::getTillBalance));
		runningTillBalanceCol = new MFXTableColumn<>("RUNNING TILL BALANCE",true, Comparator.comparing(EODDataPoint::getRunningTillBalance));
		notesCol = new MFXTableColumn<>("NOTES",true, Comparator.comparing(EODDataPoint::getNotes));
		dateCol.setMinWidth(400);

		dateCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getDateString));
		cashAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getCashAmountString));
		eftposAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getEftposAmountString));
		amexAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getAmexAmountString));
		googleSquareAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getGoogleSquareAmountString));
		chequeAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getChequeAmountString));
		medschecksCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getMedschecks));
		stockOnHandAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getStockOnHandAmount));
		scriptsOnFileCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getScriptsOnFile));
		smsPatientsCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getSmsPatients));
		tillBalanceCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getTillBalanceString));
		runningTillBalanceCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getRunningTillBalanceString));
		notesCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getNotes));

		eodDataTable.getTableColumns().addAll(
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
		eodDataTable.autosizeColumnsOnInitialization();
		eodDataTable.autosizeColumns();
		eodDataTable.virtualFlowInitializedProperty().addListener((observable, oldValue, newValue) -> {addDoubleClickfunction();});
	}

	private void addDoubleClickfunction(){
		for (Map.Entry<Integer, MFXTableRow<EODDataPoint>> entry:eodDataTable.getCells().entrySet()) {
			entry.getValue().setOnMouseClicked(event -> {
				if(event.getClickCount()==2) openEODPopover(entry.getValue().getData());
			});
			for (MFXTableRowCell<EODDataPoint, ?> cell:entry.getValue().getCells()) {
				cell.setOnMouseClicked(event -> {
					if(event.getClickCount()==2){
						MFXTableRow<EODDataPoint> parentRow = (MFXTableRow<EODDataPoint>) cell.getParent();
						openEODPopover(parentRow.getData());
					}
				});
			}
		}
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
				String sql = "INSERT INTO tillReportDatapoints(storeID,assignedDate,periodStartDate,periodEndDate,`key`,quantity,amount) VALUES(?,?,?,?,?,?,?)";
				try {
					preparedStatement = con.prepareStatement(sql);
					preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
					preparedStatement.setDate(2, Date.valueOf(targetDate));
					preparedStatement.setObject(3, wbp.getPeriodStart().atZone(ZoneId.of("Australia/Melbourne")));
					preparedStatement.setObject(4, wbp.getPeriodEnd().atZone(ZoneId.of("Australia/Melbourne")));
					preparedStatement.setString(5,cdp.getCategory()+((cdp.getSubCategory()!="")?"-"+cdp.getSubCategory():""));
					preparedStatement.setDouble(6,cdp.getQuantity());
					preparedStatement.setDouble(7,cdp.getAmount());
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
		eodDataTable.setItems(eodDataPoints);
		for (Map.Entry<Integer, MFXTableRow<EODDataPoint>> entry:eodDataTable.getCells().entrySet()) {
			entry.getValue().setOnMouseClicked(event -> {
				if(event.getClickCount()==2) openEODPopover(entry.getValue().getData());
			});
			for (MFXTableRowCell<EODDataPoint, ?> cell:entry.getValue().getCells()) {
				cell.setOnMouseClicked(event -> {
					if(event.getClickCount()==2){
						MFXTableRow<EODDataPoint> parentRow = (MFXTableRow<EODDataPoint>) cell.getParent();
						openEODPopover(parentRow.getData());
					}
				});
			}
		}
		eodDataTable.autosizeColumns();
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
}
