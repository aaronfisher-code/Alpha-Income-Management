package services;

import models.BASCheckerDataPoint;
import models.Credit;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class BASCheckerService {

    public BASCheckerDataPoint getBASData(int storeId, YearMonth yearMonth) throws SQLException {
        String sql = "SELECT * FROM baschecker WHERE storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setInt(2, yearMonth.getMonthValue());
            preparedStatement.setInt(3, yearMonth.getYear());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new BASCheckerDataPoint(resultSet);
                }
            }
        }
        return null;
    }

    public void updateBASData(BASCheckerDataPoint basDataPoint) throws SQLException {
        String sql = "INSERT INTO baschecker(date,storeID,cashAdjustment,eftposAdjustment,amexAdjustment,googleSquareAdjustment,chequesAdjustment,medicareAdjustment,totalIncomeAdjustment,cashCorrect,eftposCorrect,amexCorrect,googleSquareCorrect,chequesCorrect,medicareCorrect,totalIncomeCorrect,gstCorrect,basDailyScript) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE cashAdjustment=?,eftposAdjustment=?,amexAdjustment=?,googleSquareAdjustment=?,chequesAdjustment=?,medicareAdjustment=?,totalIncomeAdjustment=?,cashCorrect=?,eftposCorrect=?,amexCorrect=?,googleSquareCorrect=?,chequesCorrect=?,medicareCorrect=?,totalIncomeCorrect=?,gstCorrect=?,basDailyScript=?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, Date.valueOf(basDataPoint.getDate()));
            preparedStatement.setInt(2, basDataPoint.getStoreID());
            preparedStatement.setDouble(3, basDataPoint.getCashAdjustment());
            preparedStatement.setDouble(4, basDataPoint.getEftposAdjustment());
            preparedStatement.setDouble(5, basDataPoint.getAmexAdjustment());
            preparedStatement.setDouble(6, basDataPoint.getGoogleSquareAdjustment());
            preparedStatement.setDouble(7, basDataPoint.getChequeAdjustment());
            preparedStatement.setDouble(8, basDataPoint.getMedicareAdjustment());
            preparedStatement.setDouble(9, basDataPoint.getTotalIncomeAdjustment());
            preparedStatement.setBoolean(10, basDataPoint.isCashCorrect());
            preparedStatement.setBoolean(11, basDataPoint.isEftposCorrect());
            preparedStatement.setBoolean(12, basDataPoint.isAmexCorrect());
            preparedStatement.setBoolean(13, basDataPoint.isGoogleSquareCorrect());
            preparedStatement.setBoolean(14, basDataPoint.isChequeCorrect());
            preparedStatement.setBoolean(15, basDataPoint.isMedicareCorrect());
            preparedStatement.setBoolean(16, basDataPoint.isTotalIncomeCorrect());
            preparedStatement.setBoolean(17, basDataPoint.isGstCorrect());
            preparedStatement.setDouble(18, basDataPoint.getBasDailyScript());
            preparedStatement.setDouble(19, basDataPoint.getCashAdjustment());
            preparedStatement.setDouble(20, basDataPoint.getEftposAdjustment());
            preparedStatement.setDouble(21, basDataPoint.getAmexAdjustment());
            preparedStatement.setDouble(22, basDataPoint.getGoogleSquareAdjustment());
            preparedStatement.setDouble(23, basDataPoint.getChequeAdjustment());
            preparedStatement.setDouble(24, basDataPoint.getMedicareAdjustment());
            preparedStatement.setDouble(25, basDataPoint.getTotalIncomeAdjustment());
            preparedStatement.setBoolean(26, basDataPoint.isCashCorrect());
            preparedStatement.setBoolean(27, basDataPoint.isEftposCorrect());
            preparedStatement.setBoolean(28, basDataPoint.isAmexCorrect());
            preparedStatement.setBoolean(29, basDataPoint.isGoogleSquareCorrect());
            preparedStatement.setBoolean(30, basDataPoint.isChequeCorrect());
            preparedStatement.setBoolean(31, basDataPoint.isMedicareCorrect());
            preparedStatement.setBoolean(32, basDataPoint.isTotalIncomeCorrect());
            preparedStatement.setBoolean(33, basDataPoint.isGstCorrect());
            preparedStatement.setDouble(34, basDataPoint.getBasDailyScript());
            preparedStatement.executeUpdate();
        }
    }
}
