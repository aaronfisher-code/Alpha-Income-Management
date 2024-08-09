package controllers;

import application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import models.SpecialDateObj;
import services.RosterService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class RosterDayCardController extends Controller {

    @FXML
    private Label weekdayLbl, dateLbl, eventLbl;
    @FXML
    private Region selectionHighlight;

    private Main main;
    private LocalDate date;
    private RosterPageController parent;
    private boolean selected = false;
    private RosterService rosterService;

    @FXML
    private void initialize() {
        rosterService = new RosterService();
    }

    @Override
    public void setMain(Main newMain) {
        this.main = newMain;
    }

    public void setDate(LocalDate d) {
        this.date = d;
    }

    public void setParent(RosterPageController parent) {
        this.parent = parent;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public void fill() {
        weekdayLbl.setText(String.valueOf(date.getDayOfWeek()));
        dateLbl.setText(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth());
        try {
            SpecialDateObj specialDateObj = rosterService.getSpecialDateInfo(date);
            if(specialDateObj != null)
                eventLbl.setText(specialDateObj.getNote());
        } catch (SQLException ex) {
            parent.getDialogPane().showError("Error", "Error loading special date info", ex.getMessage());
            System.err.println(ex.getMessage());
        }
    }

    public void editDay() {
        parent.createRosterDayEditDialog(date);
    }

    public void singleClick(){
        if(!selected)
            parent.setDatePkr(date);
        else {
            if(main.getCurrentUser().getPermissions().stream().anyMatch(permission -> permission.getPermissionName().equals("Roster - Edit public holidays"))){
                editDay();
            }
        }
    }

    public void select() {
        selectionHighlight.setStyle("-fx-background-color: #0F60FF;");
        selected = true;
    }
}
