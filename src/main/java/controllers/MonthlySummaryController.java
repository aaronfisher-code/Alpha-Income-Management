package controllers;

import application.Main;
import com.dlsc.gemsfx.DialogPane;
import com.dlsc.gemsfx.FilterView;
import components.ActionableFilterComboBox;
import interfaces.actionableComboBox;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import models.AccountPaymentContactDataPoint;
import models.EODDataPoint;
import models.Invoice;
import utils.AnimationUtils;
import utils.GUIUtils;
import utils.TableUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static com.dlsc.gemsfx.DialogPane.Type.BLANK;

public class MonthlySummaryController extends Controller{

	private MFXDatePicker datePkr;
	@FXML
	private FlowPane datePickerPane;
	@FXML
	private StackPane backgroundPane;
	@FXML
	private TableView<EODDataPoint> summaryTable;

	@FXML
	private TableColumn<EODDataPoint,String> endCol;

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

		datePkr = new MFXDatePicker();
//		datePkr.setOnAction(e -> updatePage());
		datePickerPane.getChildren().add(1,datePkr);
		datePkr.setValue(LocalDate.now());
		datePkr.setText(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
		datePkr.getStylesheets().add("/views/CSS/RosterPage.css");

		summaryTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		summaryTable.setFixedCellSize(25.0);
		VBox.setVgrow(summaryTable, Priority.ALWAYS);
		for(TableColumn tc: summaryTable.getColumns()){
			tc.setPrefWidth(TableUtils.getColumnWidth(tc)+30);
		}
		Platform.runLater(() -> GUIUtils.customResize(summaryTable,endCol));
	}

	public void exportFiles(){}

	public void importFiles(){}

	public void weekForward() {
		setDatePkr(datePkr.getValue().plusMonths(1));
	}

	public void weekBackward() {
		setDatePkr(datePkr.getValue().minusMonths(1));
	}

	public void setDatePkr(LocalDate date) {
		datePkr.setValue(date);
	}

	public void openPopover(){
	}

	public void closePopover(){
	}
}



