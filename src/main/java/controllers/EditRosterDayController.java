package controllers;

import application.Main;
import com.jfoenix.controls.*;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Paint;
import models.Shift;
import models.SpecialDateObj;
import services.RosterService;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class EditRosterDayController extends Controller {

    @FXML
    private Label dateLabel;

    @FXML
    private MFXToggleButton publicHolidayToggle;

    @FXML
    private MFXTextField noteField;

    private Main main;
    private RosterPageController parent;
    private LocalDate date;
    private RosterService rosterService;

    @Override
    public void setMain(Main main) { this.main = main; }

    public void setParent(RosterPageController m) {
        this.parent = m;
    }

    public void setDate(LocalDate d) { this.date = d; }

    @FXML
    private void initialize() {
        rosterService = new RosterService();
    }

    @Override
    public void fill() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = date.format(formatter);

        dateLabel.setText("Editing " + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", " + formattedDate);
        try {
            SpecialDateObj specialDateObj = rosterService.getSpecialDateInfo(date);
            if(specialDateObj != null){
                publicHolidayToggle.setSelected(specialDateObj.getStoreStatus().equals("Public Holiday"));
                noteField.setText(specialDateObj.getNote());
            }else{
                publicHolidayToggle.setSelected(false);
                noteField.setText("");
            }
        } catch (SQLException ex) {
            parent.getDialogPane().showError("Error", "Error loading special date info", ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void addDayInfo(){
        String storeStatus = publicHolidayToggle.isSelected() ? "Public Holiday" : "Open";
        String note = noteField.getText();

        try {
            SpecialDateObj specialDateObj = rosterService.getSpecialDateInfo(date);
            if(specialDateObj != null){
                specialDateObj.setStoreStatus(storeStatus);
                specialDateObj.setNote(note);
                rosterService.updateSpecialDate(specialDateObj);
            }else{
                SpecialDateObj newSpecialDate = new SpecialDateObj();
                newSpecialDate.setEventDate(date);
                newSpecialDate.setStoreStatus(storeStatus);
                newSpecialDate.setNote(note);
                rosterService.addSpecialDate(newSpecialDate);
            }
            parent.updatePage();
            closeDialog();
            parent.getDialogPane().showInformation("Success", "Changes Successfully saved");
        } catch (SQLException ex) {
            parent.getDialogPane().showError("Error", "Error saving special date info", ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void closeDialog(){
        parent.getDialog().cancel();
    }

}
