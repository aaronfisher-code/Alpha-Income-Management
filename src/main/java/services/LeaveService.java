package services;

import models.LeaveRequest;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveService {
    public List<LeaveRequest> getLeaveRequests(int storeId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String sql = "SELECT * FROM leaverequests JOIN accounts a on a.username = leaverequests.employeeID WHERE storeID = ? AND ((leaveStartDate>=? AND leaveStartDate<=?) OR (leaveEndDate>=? AND leaveEndDate<=?) OR (leaveStartDate<=? AND leaveEndDate>=?))";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            preparedStatement.setDate(4, Date.valueOf(startDate));
            preparedStatement.setDate(5, Date.valueOf(endDate));
            preparedStatement.setDate(6, Date.valueOf(startDate));
            preparedStatement.setDate(7, Date.valueOf(endDate));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    leaveRequests.add(new LeaveRequest(resultSet));
                }
            }
        }
        return leaveRequests;
    }

    public void addLeaveRequest(LeaveRequest leaveRequest) throws SQLException {
        String sql = "INSERT INTO leaverequests (employeeID, storeID, leaveType, leaveStartDate, leaveEndDate, reason) VALUES (?,?,?,?,?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, leaveRequest.getEmployeeID());
            preparedStatement.setInt(2, leaveRequest.getStoreID());
            preparedStatement.setString(3, leaveRequest.getLeaveType());
            preparedStatement.setTimestamp(4, Timestamp.valueOf(leaveRequest.getFromDate()));
            preparedStatement.setTimestamp(5, Timestamp.valueOf(leaveRequest.getToDate()));
            preparedStatement.setString(6, leaveRequest.getLeaveReason());
            preparedStatement.executeUpdate();
        }
    }

    public void updateLeaveRequest(LeaveRequest leaveRequest) throws SQLException {
        String sql = "UPDATE leaverequests SET leaveType = ?, leaveStartDate = ?, leaveEndDate = ?, reason = ? WHERE leaveID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, leaveRequest.getLeaveType());
            preparedStatement.setTimestamp(2, Timestamp.valueOf(leaveRequest.getFromDate()));
            preparedStatement.setTimestamp(3, Timestamp.valueOf(leaveRequest.getToDate()));
            preparedStatement.setString(4, leaveRequest.getLeaveReason());
            preparedStatement.setInt(5, leaveRequest.getLeaveID());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteLeaveRequest(int leaveId) throws SQLException {
        String sql = "DELETE FROM leaverequests WHERE leaveID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, leaveId);
            preparedStatement.executeUpdate();
        }
    }
}
