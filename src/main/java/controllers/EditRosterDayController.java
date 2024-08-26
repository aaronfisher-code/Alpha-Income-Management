package controllers;

import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import models.SpecialDateObj;
import services.RosterService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class EditRosterDayController extends PageController {

    @FXML private Label dateLabel;
    @FXML private MFXToggleButton publicHolidayToggle;
    @FXML private MFXTextField noteField;
    private RosterPageController parent;
    private LocalDate date;
    private RosterService rosterService;

    public void setParent(RosterPageController m) {
        this.parent = m;
    }

    public void setRosterDayDate(LocalDate d) { this.date = d; }

    @FXML
    private void initialize() {
        try{
            rosterService = new RosterService();
        }catch (IOException e){
            parent.getDialogPane().showError("Error", "Error loading roster service", e);
        }
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
        } catch (Exception ex) {
            parent.getDialogPane().showError("Error", "Error loading special date info", ex);
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
        } catch (Exception ex) {
            parent.getDialogPane().showError("Error", "Error saving special date info", ex);
        }
    }

    public void closeDialog(){
        parent.getDialog().cancel();
    }
}
