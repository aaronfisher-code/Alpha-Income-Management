package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import models.CellDataPoint;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import utils.LegacyImportTool;
import utils.WorkbookProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.time.ZoneId;

public class SettingsController extends Controller{

	@FXML
	private MFXTextField filePathField;
	
	
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

	}

	@FXML
	private void chooseFolder(){
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(main.getStg());
		filePathField.setText(selectedDirectory.getAbsolutePath());
	}

	@FXML
	private void legacyImport() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Archived file");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XLSM Files", "*.xlsm"));
		File newfile = fileChooser.showOpenDialog(main.getStg());
		if (newfile != null) {
			FileInputStream file = new FileInputStream(newfile);
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			LegacyImportTool wbp = new LegacyImportTool(workbook);
		}
	}
	
}
