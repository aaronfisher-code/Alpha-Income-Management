package controllers;

import application.Main;
import com.jfoenix.controls.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Paint;
import models.Shift;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class EditRosterDayController extends Controller {


    @FXML
    private Button saveSingle, saveMultiple, cancelSingle, cancelMultiple, addShift;

    @FXML
    private ToggleGroup status;

    @FXML
    private JFXTextField noteField;


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
        String sql = "SELECT * FROM specialDates Where eventDate = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, date.toString());
            resultSet = preparedStatement.executeQuery();
            if (!(resultSet == null || !resultSet.next())) {
                noEntries = false;
                if(resultSet.getString("storeStatus").equals("Public Holiday"))
                    status.selectToggle(status.getToggles().get(1));
                 else
                    status.selectToggle(status.getToggles().get(0));
                noteField.setText(resultSet.getString("note"));
            }else{
                noEntries = true;
                status.selectToggle(status.getToggles().get(0));
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void addDayInfo(){
        String sql = "";
        String eventDate = date.toString();
        JFXRadioButton selectedButton = (JFXRadioButton) status.getSelectedToggle();
        String storeStatus = selectedButton.getText();
        String note = noteField.getText();
        if(noEntries){
            sql = "INSERT INTO specialDates(storeStatus,note,eventDate) VALUES(?,?,?)";
        }else{
            sql = "UPDATE specialDates SET storeStatus = ?, note = ? WHERE eventDate = ?";
        }

        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, storeStatus);
            preparedStatement.setString(2, note);
            preparedStatement.setString(3, eventDate);
            preparedStatement.executeUpdate();
            parent.updatePage();
            JOptionPane.showMessageDialog(null, "Changes Successfully saved");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

}
