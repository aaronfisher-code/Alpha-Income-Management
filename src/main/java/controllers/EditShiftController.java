package controllers;

import application.Main;
import com.jfoenix.controls.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Shift;
import org.controlsfx.control.SegmentedButton;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public class EditShiftController extends Controller {


    @FXML
    private Button saveSingle, saveMultiple, cancelSingle, cancelMultiple, addShift;
    @FXML
    private Label addShiftError,repeatLabel;
    @FXML
    private JFXComboBox employeeSelect, repeatUnit;
    @FXML
    private JFXDatePicker startDate;
    @FXML
    private JFXTextField repeatValue, tenMinBreaks, thirtyMinBreaks;
    @FXML
    private JFXTimePicker startTime, endTime;
    @FXML
    private JFXToggleButton repeatingShiftToggle;
    @FXML
    private SegmentedButton leaveSelect;


    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Main main;
    private Shift shift;
    private RosterPageController parent;
    private LocalDate currentShiftDate;
    private Stage currentShiftCard;
    ToggleButton regButton = new ToggleButton();
    ToggleButton annButton = new ToggleButton();
    ToggleButton sicButton = new ToggleButton();
    ToggleButton phyButton = new ToggleButton();
    ToggleButton phnButton = new ToggleButton();

    @Override
    public void setMain(Main main) { this.main = main; }

    public void setParent(RosterPageController m) {
        this.parent = m;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }

    public void setDate(LocalDate d) { this.currentShiftDate = d; }

    public void setShift(Shift s) { this.shift = s; }

    public void setController(Stage s) {this.currentShiftCard = s;}

    public String getUserName(String employeeName) {
        String username = null;
        String sql = "Select * from accounts where CONCAT(first_name,' ',last_name) = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, employeeName);
            resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                username = resultSet.getString("username");
            }
            return username;
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return "";
    }

    public void newShiftFormat(Boolean format) {
        saveSingle.setVisible(!format);
        saveMultiple.setVisible(!format&&shift.isRepeating());
        cancelSingle.setVisible(!format);
        cancelMultiple.setVisible(!format&&shift.isRepeating());
        addShift.setVisible(format);
        if (!format) {

            String intendedName = getFullname(shift.getUsername());
            for(Object o:employeeSelect.getItems()){
                if(o.toString().equals(intendedName)){
                    employeeSelect.getSelectionModel().select(o);
                }
            }
            startDate.setValue(currentShiftDate);
            startTime.setValue(shift.getShiftStartTime());
            endTime.setValue(shift.getShiftEndTime());
            thirtyMinBreaks.setText(String.valueOf(shift.getThirtyMinBreaks()));
            tenMinBreaks.setText(String.valueOf(shift.getTenMinBreaks()));
            repeatingShiftToggle.setSelected(shift.isRepeating());
            if(shift.isRepeating()){
                showRepeatOptions();
                repeatValue.setText(String.valueOf(shift.getDaysPerRepeat()));
                repeatUnit.setValue("Days");
            }
        }
    }

    public void showRepeatOptions(){
        if(repeatingShiftToggle.isSelected()){
            repeatValue.setVisible(true);
            repeatUnit.setVisible(true);
            repeatLabel.setVisible(true);
        }else{
            repeatValue.setVisible(false);
            repeatUnit.setVisible(false);
            repeatLabel.setVisible(false);
        }
    }

    public String getFullname(String username) {
        String fullname = "";
        String sql = "SELECT * FROM accounts Where username = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                fullname = resultSet.getString("first_name") + " " + resultSet.getString("last_name");
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return fullname;
    }

    @Override
    public void fill() {

        String sql = "SELECT * FROM accounts";
        try {
            preparedStatement = con.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next())
                employeeSelect.getItems().add(resultSet.getString("first_name") + " " + resultSet.getString("last_name"));
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        repeatUnit.getItems().add("Weeks");
        repeatUnit.getItems().add("Days");

        boolean publicHoldayFormat = false;
        sql = "SELECT * FROM specialDates WHERE storeStatus = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, "Public Holiday");
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                if(currentShiftDate!=null && LocalDate.parse(resultSet.getString("eventDate")).isEqual(currentShiftDate)){
                    publicHoldayFormat = true;
                }
            }

        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        if(publicHoldayFormat){
            phyButton.setText("Public Holiday Worked");
            phnButton.setText("Public Holiday Not Worked");
            if(shift.getShiftType() != null && shift.getShiftType().equals("PHNW")){
                phnButton.setSelected(true);
            }else{
                phyButton.setSelected(true);
            }
            leaveSelect.getButtons().addAll(phyButton,phnButton);
        }else{
            regButton.setText("Regular shift");
            annButton.setText("Annual Leave");
            sicButton.setText("Sick Leave");
            if(shift!= null && shift.getShiftType() != null && shift.getShiftType().equals("AL")){
                annButton.setSelected(true);
            }else if(shift!= null && shift.getShiftType() != null && shift.getShiftType().equals("SL")){
                sicButton.setSelected(true);
            }else{
                regButton.setSelected(true);
            }
            leaveSelect.getButtons().addAll(regButton,annButton,sicButton);
        }

    }

    public boolean validateInput(){
        if (employeeSelect.getValue() == null || startDate.getValue() == null || startTime.getValue() == null || endTime.getValue() == null)
            return false;
        String employeeName = employeeSelect.getValue().toString();
        String sDate = startDate.getValue().toString();
        String sTime = startTime.getValue().toString();
        String eTime = endTime.getValue().toString();
        if (employeeName.isEmpty() || sDate.isEmpty() || sTime.isEmpty() || eTime.isEmpty())
            return false;
        return true;
    }

    public void addShift() {
        String employeeName = employeeSelect.getValue().toString();
        String usrname = getUserName(employeeName);
        String sDate = startDate.getValue().toString();
        String sTime = startTime.getValue().toString();
        String eTime = endTime.getValue().toString();
        int repeat = (repeatingShiftToggle.isSelected()) ? 1 : 0;
        int thirtyMin = 0;
        int tenMin = 0;
        int daysPerRepeat = 1;
        if(repeatingShiftToggle.isSelected()){
            try {
                if (!thirtyMinBreaks.getText().equals("")) {
                    thirtyMin = Integer.parseInt(thirtyMinBreaks.getText());
                }
                if (!tenMinBreaks.getText().equals("")) {
                    tenMin = Integer.parseInt(tenMinBreaks.getText());
                }
                int multiplier = ((repeatUnit.getValue().toString().equals("Weeks")) ? 7 : 1);
                daysPerRepeat = Integer.parseInt(repeatValue.getText()) * multiplier;
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        String shiftType;
        if(annButton.isSelected()){
            shiftType = "AL";
        }else if(sicButton.isSelected()){
            shiftType = "SL";
        }else if(phnButton.isSelected()){
            shiftType = "PHNW";
        }else{
            shiftType = null;
        }

        String sql = "INSERT INTO shifts(username,shiftStartTime,shiftEndTime,shiftStartDate,thirtyMinBreaks,tenMinBreaks,repeating,daysPerRepeat,shiftType) VALUES(?,?,?,?,?,?,?,?,?)";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, usrname);
            preparedStatement.setString(2, sTime);
            preparedStatement.setString(3, eTime);
            preparedStatement.setString(4, sDate);
            preparedStatement.setInt(5, thirtyMin);
            preparedStatement.setInt(6, tenMin);
            preparedStatement.setInt(7, repeat);
            preparedStatement.setInt(8, daysPerRepeat);
            preparedStatement.setString(9, shiftType);
            preparedStatement.executeUpdate();
            parent.updatePage();
            JOptionPane.showMessageDialog(null, "Shift successfully created");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void endOriginal(){
        String sql = "";
        //End original shift 1 day early
        try {
            if(currentShiftDate.minusDays(1).isAfter(shift.getShiftStartDate())) {
                sql = "UPDATE shifts SET shiftEndDate = ? WHERE shift_id = ?";
                preparedStatement = con.prepareStatement(sql);
                preparedStatement.setString(1, currentShiftDate.minusDays(1).toString());
                preparedStatement.setInt(2, shift.getShiftID());
            }else {
                sql = "DELETE FROM shifts WHERE shift_id = ?";
                preparedStatement = con.prepareStatement(sql);
                preparedStatement.setInt(1, shift.getShiftID());
            }
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void resumeOnNextCycle(){
        //Resume Original shift starting from next cycle
        String sql = "";
        if(shift.getShiftEndDate() == null || shift.getShiftEndDate().isAfter(currentShiftDate)){
            if(shift.getShiftEndDate() == null){
                String eDate = "";
                sql = "INSERT INTO shifts(username,shiftStartTime,shiftEndTime,shiftStartDate,thirtyMinBreaks,tenMinBreaks,repeating,daysPerRepeat,shiftType) VALUES(?,?,?,?,?,?,?,?,?)";
            }else{
                sql = "INSERT INTO shifts(username,shiftStartTime,shiftEndTime,shiftStartDate,shiftEndDate,thirtyMinBreaks,tenMinBreaks,repeating,daysPerRepeat,shiftType) VALUES(?,?,?,?,?,?,?,?,?,?)";
            }
            try {
                preparedStatement = con.prepareStatement(sql);
                preparedStatement.setString(1, shift.getUsername());
                preparedStatement.setString(2, shift.getShiftStartTime().toString());
                preparedStatement.setString(3, shift.getShiftEndTime().toString());
                preparedStatement.setString(4, currentShiftDate.plusDays(shift.getDaysPerRepeat()).toString());
                if(shift.getShiftEndDate() == null) {
                    preparedStatement.setInt(5, shift.getThirtyMinBreaks());
                    preparedStatement.setInt(6, shift.getTenMinBreaks());
                    preparedStatement.setInt(7, shift.isRepeating() ? 1 : 0);
                    preparedStatement.setInt(8, shift.getDaysPerRepeat());
                    preparedStatement.setString(9, shift.getShiftType());
                }else{
                    preparedStatement.setString(5, shift.getShiftEndDate().toString());
                    preparedStatement.setInt(6, shift.getThirtyMinBreaks());
                    preparedStatement.setInt(7, shift.getTenMinBreaks());
                    preparedStatement.setInt(8, shift.isRepeating() ? 1 : 0);
                    preparedStatement.setInt(9, shift.getDaysPerRepeat());
                    preparedStatement.setString(10, shift.getShiftType());
                }
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    public void singleSave(){
        String sql = "";
        endOriginal();
        resumeOnNextCycle();
        //Create Single new shift with changes added
        String employeeName = employeeSelect.getValue().toString();
        String usrname = getUserName(employeeName);
        String sDate = startDate.getValue().toString();
        String sTime = startTime.getValue().toString();
        String eTime = endTime.getValue().toString();
        int repeat = (repeatingShiftToggle.isSelected()) ? 1 : 0;
        int thirtyMin = 0;
        int tenMin = 0;
        int daysPerRepeat = 1;
        try {
            if (!thirtyMinBreaks.getText().equals("")) {
                thirtyMin = Integer.parseInt(thirtyMinBreaks.getText());
            }
            if (!tenMinBreaks.getText().equals("")) {
                tenMin = Integer.parseInt(tenMinBreaks.getText());
            }
            int multiplier = ((repeatUnit.getValue().toString().equals("Weeks")) ? 7 : 1);
            daysPerRepeat = Integer.parseInt(repeatValue.getText()) * multiplier;
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        String shiftType;
        if(annButton.isSelected()){
            shiftType = "AL";
        }else if(sicButton.isSelected()){
            shiftType = "SL";
        }else if(phnButton.isSelected()){
            shiftType = "PHNW";
        }else{
            shiftType = null;
        }
        sql = "INSERT INTO shifts(username,shiftStartTime,shiftEndTime,shiftStartDate,shiftEndDate,thirtyMinBreaks,tenMinBreaks,repeating,daysPerRepeat,shiftType) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, usrname);
            preparedStatement.setString(2, sTime);
            preparedStatement.setString(3, eTime);
            preparedStatement.setString(4, sDate);
            preparedStatement.setString(5, sDate);
            preparedStatement.setInt(6, thirtyMin);
            preparedStatement.setInt(7, tenMin);
            preparedStatement.setInt(8, repeat);
            preparedStatement.setInt(9, daysPerRepeat);
            preparedStatement.setString(10, shiftType);
            preparedStatement.executeUpdate();
            JOptionPane.showMessageDialog(null, "Shift successfully updated");
            parent.updatePage();
            currentShiftCard.close();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void multiSave(){
        endOriginal();
        //Create new shift with changes added
        String employeeName = employeeSelect.getValue().toString();
        String usrname = getUserName(employeeName);
        String sDate = startDate.getValue().toString();
        String sTime = startTime.getValue().toString();
        String eTime = endTime.getValue().toString();
        int repeat = (repeatingShiftToggle.isSelected()) ? 1 : 0;
        int thirtyMin = 0;
        int tenMin = 0;
        int daysPerRepeat = 1;
        try {
            if (!thirtyMinBreaks.getText().equals("")) {
                thirtyMin = Integer.parseInt(thirtyMinBreaks.getText());
            }
            if (!tenMinBreaks.getText().equals("")) {
                tenMin = Integer.parseInt(tenMinBreaks.getText());
            }
            int multiplier = ((repeatUnit.getValue().toString().equals("Weeks")) ? 7 : 1);
            daysPerRepeat = Integer.parseInt(repeatValue.getText()) * multiplier;
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        String shiftType;
        if(annButton.isSelected()){
            shiftType = "AL";
        }else if(sicButton.isSelected()){
            shiftType = "SL";
        }else if(phnButton.isSelected()){
            shiftType = "PHNW";
        }else{
            shiftType = null;
        }
        String sql = "INSERT INTO shifts(username,shiftStartTime,shiftEndTime,shiftStartDate,shfitEndDate,thirtyMinBreaks,tenMinBreaks,repeating,daysPerRepeat,shiftType) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, usrname);
            preparedStatement.setString(2, sTime);
            preparedStatement.setString(3, eTime);
            preparedStatement.setString(4, sDate);
            preparedStatement.setString(5, (shift.getShiftEndDate()==null)?null:shift.getShiftEndDate().toString());
            preparedStatement.setInt(6, thirtyMin);
            preparedStatement.setInt(7, tenMin);
            preparedStatement.setInt(8, repeat);
            preparedStatement.setInt(9, daysPerRepeat);
            preparedStatement.setString(10, shiftType);
            preparedStatement.executeUpdate();
            JOptionPane.showMessageDialog(null, "Shift successfully updated");
            parent.updatePage();
            currentShiftCard.close();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void singleCancel() throws IOException {
        endOriginal();
        if(shift.isRepeating()){
            resumeOnNextCycle();
        }
        JOptionPane.showMessageDialog(null, "Shift successfully cancelled");
        parent.updatePage();
        currentShiftCard.close();
    }

    public void multiCancel() throws IOException {
        endOriginal();
        JOptionPane.showMessageDialog(null, "Shifts successfully cancelled");
        parent.updatePage();
        currentShiftCard.close();
    }

}
