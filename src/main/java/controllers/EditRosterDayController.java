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
        // Create a formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = date.format(formatter);

        dateLabel.setText("Editing " + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", " + formattedDate);
        String sql = "SELECT * FROM specialDates Where eventDate = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, date.toString());
            resultSet = preparedStatement.executeQuery();
            if (!(resultSet == null || !resultSet.next())) {
                noEntries = false;
                if(resultSet.getString("storeStatus").equals("Public Holiday"))
                    publicHolidayToggle.setSelected(true);
                 else
                    publicHolidayToggle.setSelected(false);
                noteField.setText(resultSet.getString("note"));
            }else{
                noEntries = true;
                publicHolidayToggle.setSelected(false);
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void addDayInfo(){
        String sql = "";
        String eventDate = date.toString();
        String storeStatus = publicHolidayToggle.isSelected() ? "Public Holiday" : "Open";
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

    public void closeDialog(){
        parent.getDialog().cancel();
    }

}
