package services;

import models.BASCheckerDataPoint;
import models.BudgetAndExpensesDataPoint;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.YearMonth;

public class BudgetExpensesService {

    public BudgetAndExpensesDataPoint getBudgetExpensesData(int storeId, YearMonth yearMonth) throws SQLException {
        String sql = "SELECT * FROM budgetandexpenses WHERE storeID = ? AND MONTH(date) = ? AND YEAR(date) = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setInt(2, yearMonth.getMonthValue());
            preparedStatement.setInt(3, yearMonth.getYear());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new BudgetAndExpensesDataPoint(resultSet);
                }
            }
        }
        return null;
    }

    public void updateBudgetExpensesData(BudgetAndExpensesDataPoint data) throws SQLException {
        String sql = "INSERT INTO budgetAndExpenses (date,storeID,monthlyRent,dailyOutgoings,monthlyLoan,6CPAIncome,LanternPayIncome,OtherIncome,ATO_GST_BAS_refund,monthlyWages) VALUES (?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE monthlyRent=?,dailyOutgoings=?,monthlyLoan=?,6CPAIncome=?,LanternPayIncome=?,OtherIncome=?,ATO_GST_BAS_refund=?,monthlyWages=?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDate(1, Date.valueOf(data.getDate()));
            preparedStatement.setInt(2, data.getStoreID());
            preparedStatement.setDouble(3, data.getMonthlyRent());
            preparedStatement.setDouble(4, data.getDailyOutgoings());
            preparedStatement.setDouble(5, data.getMonthlyLoan());
            preparedStatement.setDouble(6, data.getCpaIncome());
            preparedStatement.setDouble(7, data.getLanternIncome());
            preparedStatement.setDouble(8, data.getOtherIncome());
            preparedStatement.setDouble(9, data.getAtoGSTrefund());
            preparedStatement.setDouble(10, data.getMonthlyWages());
            preparedStatement.setDouble(11, data.getMonthlyRent());
            preparedStatement.setDouble(12, data.getDailyOutgoings());
            preparedStatement.setDouble(13, data.getMonthlyLoan());
            preparedStatement.setDouble(14, data.getCpaIncome());
            preparedStatement.setDouble(15, data.getLanternIncome());
            preparedStatement.setDouble(16, data.getOtherIncome());
            preparedStatement.setDouble(17, data.getAtoGSTrefund());
            preparedStatement.setDouble(18, data.getMonthlyWages());
            preparedStatement.executeUpdate();
        }
    }
}
