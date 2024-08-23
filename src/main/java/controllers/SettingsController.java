package controllers;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import utils.LegacyImportTool;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

//TODO: REMOVE THIS PAGE
public class SettingsController extends PageController {

	@FXML private MFXTextField filePathField;

	@Override
	public void fill() {}

	@FXML
	private void chooseFolder(){
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(main.getStg());
		filePathField.setText(selectedDirectory.getAbsolutePath());
	}

	@FXML
	private void legacyImport() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Archived file");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Income Report Files", "*.xlsm","*.xlsx"));
		List<File> files = fileChooser.showOpenMultipleDialog(main.getStg());
		String password = "1234"; // replace with the actual password
		if (files != null) {
			for (File newfile : files) {
				try{
					FileInputStream file = new FileInputStream(newfile);
					XSSFWorkbook workbook = (XSSFWorkbook) WorkbookFactory.create(file, password);
					LegacyImportTool wbp = new LegacyImportTool(main);
					if(newfile.getName().contains("owners")){
						String filename = newfile.getName().substring(0, newfile.getName().lastIndexOf("."));
						String[] parts = filename.split(" "); // split the filename into parts
						String month = parts[parts.length - 2]; // second last part is the month
						String year = parts[parts.length - 1]; // last part is the year
						System.out.println("Currently importing: " + month + " " + year + " (Owner's Copy)");
						wbp.importOwnersCopy(workbook);
					}else{
						wbp.ImportStaffCopy(workbook);
					}
					System.out.println("Imported file: " + newfile.getName());
				}catch(Exception e){
					System.out.println("Error importing file: " + newfile.getName());
					e.printStackTrace();
				}
			}
			System.out.println("All done!");
		}
	}
}
