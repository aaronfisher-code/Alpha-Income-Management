package services;

import models.CellDataPoint;
import models.TillReportDataPoint;
import utils.DatabaseConnectionManager;
import utils.WorkbookProcessor;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class TillReportService {

    public List<TillReportDataPoint> getTillReportDataPoints(int storeId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<TillReportDataPoint> dataPoints = new ArrayList<>();
        String sql = "SELECT * FROM tillreportdatapoints WHERE storeID = ? AND assignedDate >= ? AND assignedDate <= ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    dataPoints.add(new TillReportDataPoint(resultSet));
                }
            }
        }
        return dataPoints;
    }

    public List<TillReportDataPoint> getTillReportDataPointsByKey(int storeId, LocalDate startDate, LocalDate endDate, String key) throws SQLException {
        List<TillReportDataPoint> dataPoints = new ArrayList<>();
        String sql = "SELECT * FROM tillreportdatapoints WHERE storeID = ? AND assignedDate >= ? AND assignedDate <= ? AND `key` = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setDate(2, Date.valueOf(startDate));
            preparedStatement.setDate(3, Date.valueOf(endDate));
            preparedStatement.setString(4, key);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    dataPoints.add(new TillReportDataPoint(resultSet));
                }
            }
        }
        return dataPoints;
    }

    public void importTillReportDataPoint(CellDataPoint cdp, WorkbookProcessor wbp, LocalDate targetDate, int storeID) throws SQLException {
        String sql = "INSERT INTO tillReportDatapoints(storeID,assignedDate,periodStartDate,periodEndDate,`key`,quantity,amount) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE periodStartDate=?,periodEndDate=?,quantity=?,amount=?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeID);
            preparedStatement.setDate(2, (cdp.getAssignedDate()==null)?Date.valueOf(targetDate): Date.valueOf(cdp.getAssignedDate()));
            preparedStatement.setObject(3, (wbp.getPeriodStart()!=null)?wbp.getPeriodStart().atZone(ZoneId.of("Australia/Melbourne")):null);
            preparedStatement.setObject(4, (wbp.getPeriodEnd()!=null)?wbp.getPeriodEnd().atZone(ZoneId.of("Australia/Melbourne")):null);
            preparedStatement.setString(5,cdp.getCategory()+((cdp.getSubCategory()!="")?"-"+cdp.getSubCategory():""));
            preparedStatement.setDouble(6,cdp.getQuantity());
            preparedStatement.setDouble(7,cdp.getAmount());
            preparedStatement.setObject(8, (wbp.getPeriodStart()!=null)?wbp.getPeriodStart().atZone(ZoneId.of("Australia/Melbourne")):null);
            preparedStatement.setObject(9, (wbp.getPeriodEnd()!=null)?wbp.getPeriodEnd().atZone(ZoneId.of("Australia/Melbourne")):null);
            preparedStatement.setDouble(10,cdp.getQuantity());
            preparedStatement.setDouble(11,cdp.getAmount());
            preparedStatement.executeUpdate();
        }
    }
}
