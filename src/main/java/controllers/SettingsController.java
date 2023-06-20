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
import java.util.List;

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
		List<File> files = fileChooser.showOpenMultipleDialog(main.getStg());

		if (files != null) {
			for (File newfile : files) {
				FileInputStream file = new FileInputStream(newfile);
				XSSFWorkbook workbook = new XSSFWorkbook(file);
				LegacyImportTool wbp = new LegacyImportTool(con, preparedStatement, main);
				wbp.ImportStaffCopy(workbook);
				System.out.println("Imported file: " + newfile.getName());
			}
			System.out.println("All done!");
		}
	}
	
}
