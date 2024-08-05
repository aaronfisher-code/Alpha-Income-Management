package services;

import models.Shift;
import models.LeaveRequest;
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

    public List<LeaveRequest> getLeaveRequests(int storeId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String sql = "SELECT * FROM leaverequests JOIN accounts a on a.username = leaverequests.employeeID WHERE storeID = ? AND (leaveStartDate<=? OR leaveEndDate>=?)";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setDate(2, Date.valueOf(endDate));
            preparedStatement.setDate(3, Date.valueOf(startDate));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    leaveRequests.add(new LeaveRequest(resultSet));
                }
            }
        }
        return leaveRequests;
    }
}
