package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class BASCheckerController extends Controller{

	private MFXDatePicker datePkr;
	@FXML
	private FlowPane datePickerPane;
	@FXML
	private StackPane backgroundPane;
//	@FXML
//	private FlexBoxPane smallTableFlex;
	@FXML
	private GridPane medicareCheckTable;
	@FXML
	private GridPane sheetCheckTable;

	
	
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

//		smallTableFlex.setAlignContent(FlexboxLayout.AlignContent.FLEX_START);
//		smallTableFlex.setFlexDirection(FlexboxLayout.FlexDirection.ROW);
//		FlexBoxPane.setGrow(medicareCheckTable, 1.0f);
//		FlexBoxPane.setMargin(medicareCheckTable, new Insets(20));
////		FlexBoxPane.setFlexBasisPercent(medicareCheckTable, 10f);
//		FlexBoxPane.setGrow(sheetCheckTable, 1.0f);
//		FlexBoxPane.setMargin(sheetCheckTable, new Insets(20));
////		FlexBoxPane.setFlexBasisPercent(sheetCheckTable, 10f);



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
