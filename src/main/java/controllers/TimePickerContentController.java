package controllers;

import application.Main;
import javafx.scene.control.ToggleButton;
import models.Shift;
import javafx.fxml.FXML;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TimePickerContentController extends Controller {

    @FXML
    private ToggleButton amSelect,pmSelect;


    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private Shift shift;
    private RosterPageController parent;

    @Override
    public void setMain(Main main) { this.main = main; }

    public void setParent(RosterPageController m) {
        this.parent = m;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }


    @Override
    public void fill() {
        amSelect.setOnAction(event -> {if(!amSelect.isSelected()){amSelect.setSelected(true);}});
        pmSelect.setOnAction(event -> {if(!pmSelect.isSelected()){pmSelect.setSelected(true);}});
    }



}
