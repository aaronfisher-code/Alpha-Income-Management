package services;

import models.EODDataPoint;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EODService {

    public List<EODDataPoint> getEODDataPoints(int storeId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<EODDataPoint> dataPoints = new ArrayList<>();
        String sql = "SELECT * FROM eoddatapoints where storeID = ? AND date>=? AND date<=?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    dataPoints.add(new EODDataPoint(resultSet));
                }
            }
        }
        return dataPoints;
    }

    public void updateEODDataPoint(EODDataPoint eodDataPoint) throws SQLException {
        String sql = "UPDATE eodDataPoints SET cash=?, eftpos=?, amex=?, googleSquare=?, cheque=?, medschecks=?, scriptsOnFile=?, stockOnHand=?, smsPatients=?, notes=? WHERE date = ? AND storeID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDouble(1, eodDataPoint.getCashAmount());
            preparedStatement.setDouble(2, eodDataPoint.getEftposAmount());
            preparedStatement.setDouble(3, eodDataPoint.getAmexAmount());
            preparedStatement.setDouble(4, eodDataPoint.getGoogleSquareAmount());
            preparedStatement.setDouble(5, eodDataPoint.getChequeAmount());
            preparedStatement.setInt(6, eodDataPoint.getMedschecks());
            preparedStatement.setInt(7, eodDataPoint.getScriptsOnFile());
            preparedStatement.setDouble(8, eodDataPoint.getStockOnHandAmount());
            preparedStatement.setInt(9, eodDataPoint.getSmsPatients());
            preparedStatement.setString(10, eodDataPoint.getNotes());
            preparedStatement.setDate(11, Date.valueOf(eodDataPoint.getDate()));
            preparedStatement.setInt(12, eodDataPoint.getStoreID());
            preparedStatement.executeUpdate();
        }
    }

    public void insertEODDataPoint(EODDataPoint eodDataPoint) throws SQLException {
        String sql = "INSERT INTO eodDataPoints(cash, eftpos, amex, googleSquare, cheque, medschecks, scriptsOnFile, stockOnHand, smsPatients, notes, date, storeID) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDouble(1, eodDataPoint.getCashAmount());
            preparedStatement.setDouble(2, eodDataPoint.getEftposAmount());
            preparedStatement.setDouble(3, eodDataPoint.getAmexAmount());
            preparedStatement.setDouble(4, eodDataPoint.getGoogleSquareAmount());
            preparedStatement.setDouble(5, eodDataPoint.getChequeAmount());
            preparedStatement.setInt(6, eodDataPoint.getMedschecks());
            preparedStatement.setInt(7, eodDataPoint.getScriptsOnFile());
            preparedStatement.setDouble(8, eodDataPoint.getStockOnHandAmount());
            preparedStatement.setInt(9, eodDataPoint.getSmsPatients());
            preparedStatement.setString(10, eodDataPoint.getNotes());
            preparedStatement.setDate(11, Date.valueOf(eodDataPoint.getDate()));
            preparedStatement.setInt(12, eodDataPoint.getStoreID());
            preparedStatement.executeUpdate();
        }
    }
}
