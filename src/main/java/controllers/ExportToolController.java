package controllers;

import application.Main;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import models.Shift;
import models.User;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import static java.time.temporal.ChronoUnit.DAYS;

public class ExportToolController extends PageController {


    @FXML
    private MFXComboBox monthPicker;

    @FXML
    private MFXTextField yearPicker;

    private Connection con = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    private Shift shift;
    private RosterPageController parent;
    private LocalDate date;
    private boolean noEntries;

    public void setParent(RosterPageController m) {
        this.parent = m;
    }

    public void setConnection(Connection c) {
        this.con = c;
    }

    public void setDate(LocalDate d) { this.date = d; }

    @Override
    public void fill() {
        for(int i = 0; i<12; i++)
            monthPicker.getItems().add(new DateFormatSymbols().getMonths()[i]);
    }

    public void copyToClipboard(){
        StringBuilder outString = new StringBuilder();
        outString.append(publicHolidays()).append("\r\n");
        ArrayList<User> users = getAllUsers();
        for(User u: users){
            outString.append(userHours(u)).append("\r\n");
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(outString.toString());
        Clipboard.getSystemClipboard().setContent(content);
        JOptionPane.showMessageDialog(null, "Data copied to clipboard!");
    }

    public String publicHolidays(){
        LocalDate sDate = LocalDate.of(Integer.parseInt(yearPicker.getText()),monthPicker.getItems().indexOf(monthPicker.getValue())+1,1);
        LocalDate eDate = LocalDate.of(Integer.parseInt(yearPicker.getText()),monthPicker.getItems().indexOf(monthPicker.getValue())+1,sDate.lengthOfMonth());
        long daysBetween = Duration.between(sDate.atStartOfDay(), eDate.atStartOfDay()).toDays()+1;
        String[] publicHolidays = new String[(int) (daysBetween+1)];
        publicHolidays[0] = "Public holidays:\t\t";
        String sql = "SELECT * FROM specialDates";
        try {
            preparedStatement = con.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                LocalDate eventDate = LocalDate.parse(resultSet.getString("eventDate"));
                if(eventDate.compareTo(sDate)>=0 && eventDate.compareTo(eDate)<=0 ){
                    if (resultSet.getString("storeStatus").equals("Public Holiday")) {
                        publicHolidays[eventDate.getDayOfMonth()] = "Y\t\t";
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        StringBuilder resultString = new StringBuilder();
        for(int i = 0;i<publicHolidays.length;i++){
            if(publicHolidays[i] == null || publicHolidays[i].isEmpty()){
                publicHolidays[i] = "N\t\t";
            }
            resultString.append(publicHolidays[i]);
        }
        resultString.append("\r\n");
        resultString.append("Staff hours\t\t");
        for(int i = 1;i<publicHolidays.length;i++){
            resultString.append(String.valueOf(i)+"\t\t");
        }
        return resultString.toString();
    }
    
    public String userHours(User u){
        LocalDate sDate = LocalDate.of(Integer.parseInt(yearPicker.getText()),monthPicker.getItems().indexOf(monthPicker.getValue())+1,1);
        LocalDate eDate = LocalDate.of(Integer.parseInt(yearPicker.getText()),monthPicker.getItems().indexOf(monthPicker.getValue())+1,sDate.lengthOfMonth());
        long daysBetween = Duration.between(sDate.atStartOfDay(), eDate.atStartOfDay()).toDays()+1;
        String[] hoursArray = new String[(int) (daysBetween+1)];
        String[] leaveArray = new String[(int) (daysBetween+1)];
        Shift s = null;

        String sql = "SELECT * FROM shifts JOIN accounts a on a.username = shifts.username Where shifts.username = ? ";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, u.getUsername());
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                for(int i = 0; i<daysBetween; i++){
//                    s=new Shift(resultSet);
                    s = new Shift();
                    boolean repeatShiftDay = (s.isRepeating() && DAYS.between(s.getShiftStartDate(), sDate.plusDays(i)) % s.getDaysPerRepeat() == 0 && DAYS.between(s.getShiftStartDate(), sDate.plusDays(i)) >= 0);
                    boolean equalDay = s.getShiftStartDate().equals(sDate.plusDays(i));
                    boolean pastEnd = s.getShiftEndDate() != null && s.getShiftEndDate().isBefore(sDate.plusDays(i));
                    if ((equalDay || repeatShiftDay) && !pastEnd) {
                        hoursArray[i] = String.valueOf(s.getShiftStartTime().until(s.getShiftEndTime(), ChronoUnit.HOURS)-(0.5*s.getThirtyMinBreaks()));
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        sql = "SELECT * FROM leaveRequests Where username = ?";
        try {
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, u.getUsername());
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                for(int i = 0; i<daysBetween; i++){
                    if(sDate.plusDays(i).compareTo(LocalDate.parse(resultSet.getString("leaveStartDate")))>=0
                    && sDate.plusDays(i).compareTo(LocalDate.parse(resultSet.getString("leaveEndDate")))<=0){
                        leaveArray[i] = "L";
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        StringBuilder resultString = new StringBuilder();
        resultString.append(u.getFirst_name()+" "+u.getLast_name()+"\t");
        for(int i = 0;i<daysBetween;i++){
            if(hoursArray[i] == null || hoursArray[i].isEmpty()){
                hoursArray[i] = "0";
            }
            if(leaveArray[i] == null || leaveArray[i].isEmpty()){
                leaveArray[i] = "";
            }
            resultString.append(leaveArray[i]+"\t"+hoursArray[i]+"\t");
        }


        return resultString.toString();
    }

    public ArrayList<User> getAllUsers(){
        ArrayList<User> allUsers = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY last_name";
        try {
            preparedStatement = con.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
//                allUsers.add(new User(resultSet));
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return allUsers;
    }

}
