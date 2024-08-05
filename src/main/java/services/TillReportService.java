package services;

import models.TillReportDataPoint;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TillReportService {

    public List<TillReportDataPoint> getTillReportDataPoints(int storeId, LocalDate startDate, LocalDate endDate, String key) throws SQLException {
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
}
