package utils;
import application.Main;
import controllers.RosterDayCardController;
import controllers.ShiftCardController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.LeaveRequest;
import models.Shift;

import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;

public class RosterUtils {

    Main main;
    Connection conn;
    PreparedStatement preparedStatement;
    ResultSet resultSet;

    YearMonth yearMonth;

    ArrayList<Shift> allShifts = new ArrayList<>();
    ArrayList<Shift> allModifications = new ArrayList<>();
    ArrayList<LeaveRequest> allLeaveRequests = new ArrayList<>();

    Map<LocalDate, Double> dayDurationMap = new HashMap<>();

    public RosterUtils(Connection conn, Main main, LocalDate startDate, LocalDate endDate){
        this.conn = conn;
        this.main = main;

        fillDayDurations(startDate, endDate);
    }
    public RosterUtils(Connection conn, Main main, YearMonth yearMonth){
        this.conn = conn;
        this.main = main;
        this.yearMonth = yearMonth;

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        fillDayDurations(monthStart, monthEnd);
    }

    private void fillDayDurations(LocalDate startDate, LocalDate endDate){
        String sql = "SELECT * FROM shifts JOIN accounts a on a.username = shifts.username " +
                "WHERE storeID = ? AND" +
                "(shifts.repeating=TRUE AND (isNull(shiftEndDate) OR shiftEndDate>=?) AND shiftStartDate<=?)"+
                "OR (shifts.repeating=false AND shiftStartDate>=? AND shiftStartDate<=?)"+
                "ORDER BY shiftStartTime, a.first_name";
        try {
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            preparedStatement.setDate(4, Date.valueOf(startDate));
            preparedStatement.setDate(5, Date.valueOf(endDate));
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                allShifts.add(new Shift(resultSet));
            }

            sql = "SELECT * FROM shiftmodifications JOIN accounts a on a.username = shiftmodifications.username " +
                    "WHERE storeID = ? AND "+
                    "modificationID in (select max(modificationID) from shiftmodifications group by shift_id, originalDate) AND" +
                    "((shiftmodifications.shiftStartDate>=? AND shiftmodifications.shiftStartDate<=?) OR (shiftmodifications.originalDate>=? AND shiftmodifications.originalDate<=?))";

            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            preparedStatement.setDate(4, Date.valueOf(startDate));
            preparedStatement.setDate(5, Date.valueOf(endDate));
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                allModifications.add(new Shift(resultSet));;
            }

            sql = "SELECT * FROM leaverequests JOIN accounts a on a.username = leaverequests.employeeID WHERE storeID = ? AND (leaveStartDate<=?) OR (leaveEndDate>=?)";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setInt(1, main.getCurrentStore().getStoreID());
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                allLeaveRequests.add(new LeaveRequest(resultSet));
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

        for (LocalDate day = startDate; day.isBefore(endDate.plusDays(1)); day = day.plusDays(1)) {
            dayDurationMap.put(day, loadDayDuration(day));
        }
    }

    public double getDayDuration(LocalDate day){
        return dayDurationMap.get(day);
    }

    private double loadDayDuration(LocalDate day){
        LocalTime earliestStart = LocalTime.MAX;
        LocalTime latestEnd = LocalTime.MIN;

        for (Shift s : allShifts) {
            boolean repeatShiftDay = (s.isRepeating() && DAYS.between(s.getShiftStartDate(), day) % s.getDaysPerRepeat() == 0 && DAYS.between(s.getShiftStartDate(), day) >= 0);
            boolean equalDay = s.getShiftStartDate().equals(day);
            boolean pastEnd = s.getShiftEndDate() != null && s.getShiftEndDate().isBefore(day);
            if ((equalDay || repeatShiftDay) && !pastEnd) {
                Shift updatedShift = s;
                boolean shiftIsModified = false;
                for(Shift m: allModifications){
                    if(m.getShiftID()==s.getShiftID() && m.getOriginalDate().equals(day)){
                        updatedShift = m;
                        shiftIsModified=true;
                    }
                }
                if(!shiftIsModified || (shiftIsModified&&updatedShift.getShiftStartDate()!=null&&updatedShift.getShiftStartDate().equals(day))){
                    boolean shiftOnLeave = false;
                    for(LeaveRequest lr: allLeaveRequests){
                        LocalDateTime shiftStart = LocalDateTime.of(day,updatedShift.getShiftStartTime());
                        LocalDateTime shiftEnd = LocalDateTime.of(day,updatedShift.getShiftEndTime());
                        if(lr.getEmployeeID().equals(updatedShift.getUsername())&&lr.getFromDate().isBefore(shiftEnd)&&lr.getToDate().isAfter(shiftStart)){
                            shiftOnLeave = true;
                        }
                    }
                    if(!shiftOnLeave) {
                        if (updatedShift.getShiftStartTime().isBefore(earliestStart)) {
                            earliestStart = updatedShift.getShiftStartTime();
                        }
                        if (updatedShift.getShiftEndTime().isAfter(latestEnd)) {
                            latestEnd = updatedShift.getShiftEndTime();
                        }
                    }
                }
            }
        }

        for(Shift m: allModifications){
            if(m.getShiftStartDate()!=null&&m.getShiftStartDate().equals(day)&&(!(m.getShiftStartDate().equals(m.getOriginalDate())))){
                boolean shiftOnLeave = false;
                for(LeaveRequest lr: allLeaveRequests){
                    LocalDateTime shiftStart = LocalDateTime.of(day,m.getShiftStartTime());
                    LocalDateTime shiftEnd = LocalDateTime.of(day,m.getShiftEndTime());
                    if(lr.getEmployeeID().equals(m.getUsername())&&lr.getFromDate().isBefore(shiftEnd)&&lr.getToDate().isAfter(shiftStart)){
                        shiftOnLeave = true;
                    }
                }
                if(!shiftOnLeave) {
                    if (m.getShiftStartTime().isBefore(earliestStart)) {
                        earliestStart = m.getShiftStartTime();
                    }
                    if (m.getShiftEndTime().isAfter(latestEnd)) {
                        latestEnd = m.getShiftEndTime();
                    }
                }
            }
        }
        if(earliestStart.equals(LocalTime.MAX)||latestEnd.equals(LocalTime.MIN)){
            return 0;
        }else {
            return (double) Duration.between(earliestStart, latestEnd).toHours() / 10;
        }
    }

    public int getOpenDays(){
        int openDays = 0;
        for(LocalDate day = yearMonth.atDay(1); day.isBefore(yearMonth.atEndOfMonth().plusDays(1)); day = day.plusDays(1)){
            if(getDayDuration(day)>0){
                openDays++;
            }
        }
        return openDays;
    }

    public int getPartialDays(){
        int partialDays = 0;
        for(LocalDate day = yearMonth.atDay(1); day.isBefore(yearMonth.atEndOfMonth().plusDays(1)); day = day.plusDays(1)){
            if(getDayDuration(day)>0&&getDayDuration(day)<0.5){
                partialDays++;
            }
        }
        return partialDays;
    }

    public int getTotalDays(){
        return yearMonth.lengthOfMonth();
    }

    public double getDuration(LocalDate startDate, LocalDate endDate){
        double duration = 0;
        for(LocalDate day = startDate; day.isBefore(endDate.plusDays(1)); day = day.plusDays(1)){
            duration+=getDayDuration(day);
        }
        return duration;
    }
}