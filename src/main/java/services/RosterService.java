package services;

import models.Shift;
import models.LeaveRequest;
import models.SpecialDateObj;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RosterService {

    public List<Shift> getShifts(int storeId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM shifts JOIN accounts a on a.username = shifts.username " +
                "WHERE storeID = ? AND" +
                "(shifts.repeating=TRUE AND (isNull(shiftEndDate) OR shiftEndDate>=?) AND shiftStartDate<=?)"+
                "OR (shifts.repeating=false AND shiftStartDate>=? AND shiftStartDate<=?)"+
                "ORDER BY shiftStartTime, a.first_name";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            preparedStatement.setDate(4, Date.valueOf(startDate));
            preparedStatement.setDate(5, Date.valueOf(endDate));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    shifts.add(new Shift(resultSet));
                }
            }
        }
        return shifts;
    }

    public List<Shift> getShiftModifications(int storeId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Shift> modifications = new ArrayList<>();
        String sql = "SELECT * FROM shiftmodifications JOIN accounts a on a.username = shiftmodifications.username " +
                "WHERE storeID = ? AND "+
                "modificationID in (select max(modificationID) from shiftmodifications group by shift_id, originalDate) AND" +
                "((shiftmodifications.shiftStartDate>=? AND shiftmodifications.shiftStartDate<=?) OR (shiftmodifications.originalDate>=? AND shiftmodifications.originalDate<=?))";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            preparedStatement.setDate(4, Date.valueOf(startDate));
            preparedStatement.setDate(5, Date.valueOf(endDate));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    modifications.add(new Shift(resultSet));
                }
            }
        }
        return modifications;
    }

    public void addShift(Shift shift) throws SQLException {
        String sql = "INSERT INTO shifts(storeID, username, shiftStartTime, shiftEndTime, shiftStartDate, thirtyMinBreaks, tenMinBreaks, repeating, daysPerRepeat) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, shift.getStoreID());
            preparedStatement.setString(2, shift.getUsername());
            preparedStatement.setTime(3, Time.valueOf(shift.getShiftStartTime()));
            preparedStatement.setTime(4, Time.valueOf(shift.getShiftEndTime()));
            preparedStatement.setDate(5, Date.valueOf(shift.getShiftStartDate()));
            preparedStatement.setInt(6, shift.getThirtyMinBreaks());
            preparedStatement.setInt(7, shift.getTenMinBreaks());
            preparedStatement.setBoolean(8, shift.isRepeating());
            preparedStatement.setInt(9, shift.getDaysPerRepeat());
            preparedStatement.executeUpdate();
        }
    }

    public void updateShift(Shift shift) throws SQLException {
        String sql = "UPDATE shifts SET username=?, shiftStartTime=?, shiftEndTime=?, shiftStartDate=?, thirtyMinBreaks=?, tenMinBreaks=?, repeating=?, daysPerRepeat=? WHERE shift_id=?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, shift.getUsername());
            preparedStatement.setTime(2, Time.valueOf(shift.getShiftStartTime()));
            preparedStatement.setTime(3, Time.valueOf(shift.getShiftEndTime()));
            preparedStatement.setDate(4, Date.valueOf(shift.getShiftStartDate()));
            preparedStatement.setInt(5, shift.getThirtyMinBreaks());
            preparedStatement.setInt(6, shift.getTenMinBreaks());
            preparedStatement.setBoolean(7, shift.isRepeating());
            preparedStatement.setInt(8, shift.getDaysPerRepeat());
            preparedStatement.setInt(9, shift.getShiftID());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteShift(int shiftId) throws SQLException {
        String sql = "DELETE FROM shifts WHERE shift_id = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, shiftId);
            preparedStatement.executeUpdate();
        }
    }

    public void addShiftModification(Shift modification) throws SQLException {
        String sql = "INSERT INTO shiftModifications(storeID, username, shiftStartTime, shiftEndTime, shiftStartDate, thirtyMinBreaks, tenMinBreaks, repeating, daysPerRepeat, shift_id, originalDate) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, modification.getStoreID());
            preparedStatement.setString(2, modification.getUsername());
            preparedStatement.setTime(3, Time.valueOf(modification.getShiftStartTime()));
            preparedStatement.setTime(4, Time.valueOf(modification.getShiftEndTime()));
            preparedStatement.setDate(5, modification.getShiftStartDate() != null ? Date.valueOf(modification.getShiftStartDate()) : null);
            preparedStatement.setInt(6, modification.getThirtyMinBreaks());
            preparedStatement.setInt(7, modification.getTenMinBreaks());
            preparedStatement.setBoolean(8, modification.isRepeating());
            preparedStatement.setInt(9, modification.getDaysPerRepeat());
            preparedStatement.setInt(10, modification.getShiftID());
            preparedStatement.setDate(11, Date.valueOf(modification.getOriginalDate()));
            preparedStatement.executeUpdate();
        }
    }

    public void deleteShiftModifications(int shiftId) throws SQLException {
        String sql = "DELETE FROM shiftmodifications WHERE shift_id = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, shiftId);
            preparedStatement.executeUpdate();
        }
    }

    public void deleteShiftModifications(int shiftId, LocalDate cutoffDate) throws SQLException {
        String sql = "DELETE FROM shiftmodifications WHERE shift_id = ? AND originalDate > ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, shiftId);
            preparedStatement.setDate(2, Date.valueOf(cutoffDate));
            preparedStatement.executeUpdate();
        }
    }

    public void updateShiftEndDate(int shiftId, LocalDate endDate) throws SQLException {
        String sql = "UPDATE shifts SET shiftEndDate = ? WHERE shift_id = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, Date.valueOf(endDate));
            preparedStatement.setInt(2, shiftId);
            preparedStatement.executeUpdate();
        }
    }

    public SpecialDateObj getSpecialDateInfo(LocalDate date) throws SQLException {
        String sql = "SELECT * FROM specialDates Where eventDate = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, date.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new SpecialDateObj(resultSet);
                }
            }
        }
        return null;
    }

    public void addSpecialDate(SpecialDateObj specialDateObj) throws SQLException {
        String sql = "INSERT INTO specialDates(storeStatus, note, eventDate) VALUES(?,?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, specialDateObj.getStoreStatus());
            preparedStatement.setString(2, specialDateObj.getNote());
            preparedStatement.setString(3, specialDateObj.getEventDate().toString());
            preparedStatement.executeUpdate();
        }
    }

    public void updateSpecialDate(SpecialDateObj specialDateObj) throws SQLException {
        String sql = "UPDATE specialDates SET storeStatus = ?, note = ? WHERE eventDate = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, specialDateObj.getStoreStatus());
            preparedStatement.setString(2, specialDateObj.getNote());
            preparedStatement.setString(3, specialDateObj.getEventDate().toString());
            preparedStatement.executeUpdate();
        }
    }
}
