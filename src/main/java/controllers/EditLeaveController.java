package controllers;

import application.Main;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleGroup;
import models.Shift;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class EditLeaveController extends Controller {


    @FXML
    private JFXComboBox employeePicker;

    @FXML
    private JFXDatePicker startDatePicker, endDatePicker;


    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private Shift shift;
    private RosterPageController parent;
    private LocalDate date;
    private boolean noEntries;

    @Override
    public void setMain(Main main) { this.main = main; }

    public void setParent(RosterPageController m) {
        this.parent = m;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }

    public void setDate(LocalDate d) { this.date = d; }

    @Override
    public void fill() {
        String sql = "SELECT * FROM accounts";
        try {
            preparedStatement = con.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next())
                employeePicker.getItems().add(resultSet.getString("first_name") + " " + resultSet.getString("last_name"));
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void addLeaveRequest(){
        String sql = "";
        String sDate = startDatePicker.getValue().toString();
        String eDate = endDatePicker.getValue().toString();
        String employeeName = employeePicker.getValue().toString();
        String usrname = getUserName(employeeName);

        sql = "INSERT INTO leaveRequests(leaveStartDate,leaveEndDate,username) VALUES(?,?,?)";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, sDate);
            preparedStatement.setString(2, eDate);
            preparedStatement.setString(3, usrname);
            preparedStatement.executeUpdate();
            parent.updatePage();
            JOptionPane.showMessageDialog(null, "Changes Successfully saved");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public String getUserName(String employeeName) {
        String sql = "Select * from accounts where accounts.first_name || ' ' || accounts.last_name = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, employeeName);
            resultSet = preparedStatement.executeQuery();
            return resultSet.getString("username");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return "";
    }

}
