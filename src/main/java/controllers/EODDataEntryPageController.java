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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import models.EODDataPoint;
import models.User;

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

	private MFXTableColumn<EODDataPoint> dateCol;
	private MFXTableColumn<EODDataPoint> cashAmountCol;
	private MFXTableColumn<EODDataPoint> eftposAmountCol;
	private MFXTableColumn<EODDataPoint> amexAmountCol;
	private MFXTableColumn<EODDataPoint> googleSquareAmountCol;
	private MFXTableColumn<EODDataPoint> chequeAmountCol;
	private MFXTableColumn<EODDataPoint> clinicalInterventionsCol;
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
		datePkr.getStylesheets().add("/views/CSS/RosterPage.css");

		dateCol = new MFXTableColumn<>("Date",false, Comparator.comparing(EODDataPoint::getDate));
		cashAmountCol = new MFXTableColumn<>("Cash",false, Comparator.comparing(EODDataPoint::getCashAmount));
		eftposAmountCol = new MFXTableColumn<>("Eftpos",false, Comparator.comparing(EODDataPoint::getEftposAmount));
		amexAmountCol = new MFXTableColumn<>("AMEX",false, Comparator.comparing(EODDataPoint::getAmexAmount));
		googleSquareAmountCol = new MFXTableColumn<>("Google Square",false, Comparator.comparing(EODDataPoint::getGoogleSquareAmount));
		chequeAmountCol = new MFXTableColumn<>("Cheque",false, Comparator.comparing(EODDataPoint::getChequeAmount));
		clinicalInterventionsCol = new MFXTableColumn<>("Clinical Interventions",false, Comparator.comparing(EODDataPoint::getClinicalInterventions));
		medschecksCol = new MFXTableColumn<>("Medschecks",false, Comparator.comparing(EODDataPoint::getMedschecks));
		stockOnHandAmountCol = new MFXTableColumn<>("Stock on Hand",false, Comparator.comparing(EODDataPoint::getStockOnHandAmount));
		scriptsOnFileCol = new MFXTableColumn<>("Scripts on File",false, Comparator.comparing(EODDataPoint::getScriptsOnFile));
		smsPatientsCol = new MFXTableColumn<>("SMS Patients",false, Comparator.comparing(EODDataPoint::getSmsPatients));
		tillBalanceCol = new MFXTableColumn<>("Till Balance",false, Comparator.comparing(EODDataPoint::getTillBalance));
		runningTillBalanceCol = new MFXTableColumn<>("Running Till Balance",false, Comparator.comparing(EODDataPoint::getRunningTillBalance));
		notesCol = new MFXTableColumn<>("Notes",false, Comparator.comparing(EODDataPoint::getNotes));

		dateCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getDate));
		cashAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getCashAmount));
		eftposAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getEftposAmount));
		amexAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getAmexAmount));
		googleSquareAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getGoogleSquareAmount));
		chequeAmountCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getChequeAmount));
		clinicalInterventionsCol.setRowCellFactory(eodDataPoint -> new MFXTableRowCell<>(EODDataPoint::getClinicalInterventions));
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
				clinicalInterventionsCol,
				medschecksCol,
				stockOnHandAmountCol,
				scriptsOnFileCol,
				smsPatientsCol,
				tillBalanceCol,
				runningTillBalanceCol,
				notesCol
		);



	}

	public void importFiles(){
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Data entry File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
		fileChooser.showOpenDialog(main.getStg());
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
	
}
