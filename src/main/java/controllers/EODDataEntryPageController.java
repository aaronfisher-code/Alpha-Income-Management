package controllers;

import application.Main;
import com.dlsc.gemsfx.FilterView;
import eu.hansolo.medusa.Gauge;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.filter.StringFilter;
import io.github.palexdev.materialfx.utils.others.observables.When;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import models.EODDataPoint;
import models.User;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;

public class EODDataEntryPageController extends Controller{

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private MainMenuController parent;
    private User selectedUser;

	private MFXDatePicker datePkr;

	@FXML
	private FlowPane datePickerPane;
    @FXML
    private MFXTableView<EODDataPoint> eodDataTable;
	@FXML
	private VBox editDayPopover;

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

		datePkr = new MFXDatePicker();
//		datePkr.setOnAction(e -> updatePage());
		datePickerPane.getChildren().add(1,datePkr);
		datePkr.setValue(LocalDate.now());
		datePkr.setText(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
//		datePkr.getStylesheets().add("/views/CSS/RosterPage.css");

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

	public void weekForward() {
		setDatePkr(datePkr.getValue().plusMonths(1));
	}

	public void weekBackward() {
		setDatePkr(datePkr.getValue().minusMonths(1));
	}

	public void setDatePkr(LocalDate date) {
		datePkr.setValue(date);
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
