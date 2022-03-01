package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import models.EODDataPoint;
import models.User;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.controlsfx.control.PopOver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.Locale;

public class EODDataEntryPageController extends Controller{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private MainMenuController parent;
    private User selectedUser;
	private PopOver currentDatePopover;

	private LocalDate monthSelectorDate;

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

    @FXML
	private VBox dataEntryRowPane;
	
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
		setMonthSelectorDate(LocalDate.now());
		dateCol = new MFXTableColumn<>("DATE",false, Comparator.comparing(EODDataPoint::getDate));
		cashAmountCol = new MFXTableColumn<>("CASH",false, Comparator.comparing(EODDataPoint::getCashAmount));
		eftposAmountCol = new MFXTableColumn<>("EFTPOS",false, Comparator.comparing(EODDataPoint::getEftposAmount));
		amexAmountCol = new MFXTableColumn<>("AMEX",false, Comparator.comparing(EODDataPoint::getAmexAmount));
		googleSquareAmountCol = new MFXTableColumn<>("GOOGLE SQUARE",false, Comparator.comparing(EODDataPoint::getGoogleSquareAmount));
		chequeAmountCol = new MFXTableColumn<>("CHEQUE",false, Comparator.comparing(EODDataPoint::getChequeAmount));
		medschecksCol = new MFXTableColumn<>("MEDSCHECKS",false, Comparator.comparing(EODDataPoint::getMedschecks));
		stockOnHandAmountCol = new MFXTableColumn<>("STOCK ON HAND",false, Comparator.comparing(EODDataPoint::getStockOnHandAmount));
		scriptsOnFileCol = new MFXTableColumn<>("SCRIPTS ON FILE",false, Comparator.comparing(EODDataPoint::getScriptsOnFile));
		smsPatientsCol = new MFXTableColumn<>("SMS PATIENTS",false, Comparator.comparing(EODDataPoint::getSmsPatients));
		tillBalanceCol = new MFXTableColumn<>("TILL BALANCE",false, Comparator.comparing(EODDataPoint::getTillBalance));
		runningTillBalanceCol = new MFXTableColumn<>("RUNNING TILL BALANCE",false, Comparator.comparing(EODDataPoint::getRunningTillBalance));
		notesCol = new MFXTableColumn<>("NOTES",false, Comparator.comparing(EODDataPoint::getNotes));

		dateCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getDate));
		cashAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getCashAmount));
		eftposAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getEftposAmount));
		amexAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getAmexAmount));
		googleSquareAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getGoogleSquareAmount));
		chequeAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getChequeAmount));
		medschecksCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getMedschecks));
		stockOnHandAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getStockOnHandAmount));
		scriptsOnFileCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getScriptsOnFile));
		smsPatientsCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getSmsPatients));
		tillBalanceCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getTillBalance));
		runningTillBalanceCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getRunningTillBalance));
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

		eodDataTable.autosizeColumnsOnInitialization();



	}

	public void importFiles() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data entry File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XLS Files", "*.xls"));
		File newfile = fileChooser.showOpenDialog(main.getStg());
		FileInputStream file = new FileInputStream(newfile);
		Workbook workbook = new XSSFWorkbook(file);
	}

	public void monthForward() {
		setMonthSelectorDate(monthSelectorDate.plusMonths(1));
	}

	public void monthBackward() {
		setMonthSelectorDate(monthSelectorDate.minusMonths(1));
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

	public void setMonthSelectorDate(LocalDate newDate){
	 	monthSelectorDate = newDate;
	 	String fieldText = monthSelectorDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	 	fieldText += ", ";
	 	fieldText += monthSelectorDate.getYear();
	 	monthSelectorField.setText(fieldText);
	}

	public LocalDate getMonthSelectorDate(){
	 	return monthSelectorDate;
	}

	public void addNewPayment(){
		editDayPopover.setEffect(new DropShadow());
		changeSize(editDayPopover,0);
	}

	public void closePopover(){
		changeSize(editDayPopover,375);
		editDayPopover.setEffect(null);
	}

	public void changeSize(final VBox pane, double width) {
		Duration cycleDuration = Duration.millis(200);
		Timeline timeline = new Timeline(
				new KeyFrame(cycleDuration,
						new KeyValue(pane.translateXProperty(),width, Interpolator.EASE_BOTH))
		);
		timeline.play();
	}
	
}
