package services;

import models.Credit;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class CreditService {

    public List<Credit> getAllCredits(int storeId, YearMonth yearMonth) throws SQLException {
        List<Credit> credits = new ArrayList<>();
        String sql = "SELECT * FROM credits JOIN invoiceSuppliers i on credits.supplierID = i.idinvoiceSuppliers WHERE credits.storeID = ? AND MONTH(creditDate) = ? AND YEAR(creditDate) = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setInt(2, yearMonth.getMonthValue());
            preparedStatement.setInt(3, yearMonth.getYear());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    credits.add(new Credit(resultSet));
                }
            }
        }
        return credits;
    }

    public void addCredit(Credit credit) throws SQLException {
        String sql = "INSERT INTO credits(supplierID,creditNo,referenceInvoiceNo,creditDate,creditAmount,notes,storeID) VALUES(?,?,?,?,?,?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, credit.getSupplierID());
            preparedStatement.setString(2, credit.getCreditNo());
            preparedStatement.setString(3, credit.getReferenceInvoiceNo());
            preparedStatement.setDate(4, Date.valueOf(credit.getCreditDate()));
            preparedStatement.setDouble(5, credit.getCreditAmount());
            preparedStatement.setString(6, credit.getNotes());
            preparedStatement.setInt(7, credit.getStoreID());
            preparedStatement.executeUpdate();
        }
    }

    public void updateCredit(Credit credit) throws SQLException {
        String sql = "UPDATE credits SET supplierID = ?,creditNo = ?,referenceInvoiceNo = ?,creditDate = ?,creditAmount = ?,notes = ? WHERE idCredits = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, credit.getSupplierID());
            preparedStatement.setString(2, credit.getCreditNo());
            preparedStatement.setString(3, credit.getReferenceInvoiceNo());
            preparedStatement.setDate(4, Date.valueOf(credit.getCreditDate()));
            preparedStatement.setDouble(5, credit.getCreditAmount());
            preparedStatement.setString(6, credit.getNotes());
            preparedStatement.setInt(7, credit.getCreditID());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteCredit(int creditID) throws SQLException {
        String sql = "DELETE from credits WHERE idCredits = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, creditID);
            preparedStatement.executeUpdate();
        }
    }
}
