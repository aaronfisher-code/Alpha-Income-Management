package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import models.SpecialDateObj;
import services.RosterService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class RosterDayCardController extends PageController {

    @FXML private Label weekdayLbl, dateLbl, eventLbl;
    @FXML private Region selectionHighlight;
    private LocalDate date;
    private RosterPageController parent;
    private boolean selected = false;

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

    public void setSpecialDate(SpecialDateObj sd) {
        eventLbl.setText(sd.getNote());
    }
}
