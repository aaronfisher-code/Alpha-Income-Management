package controllers;

import application.Main;
import models.Shift;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TimePickerContentController extends Controller {

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

    }



}
