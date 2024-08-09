package services;

import models.AccountPayment;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class AccountPaymentService {

    public List<AccountPayment> getAccountPaymentsForMonth(int storeId, YearMonth yearMonth) throws SQLException {
        List<AccountPayment> accountPayments = new ArrayList<>();
        String sql = "SELECT * FROM accountPayments JOIN accountPaymentContacts a on a.idaccountPaymentContacts = accountPayments.contactID WHERE accountPayments.storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, storeId);
            preparedStatement.setInt(2, yearMonth.getMonthValue());
            preparedStatement.setInt(3, yearMonth.getYear());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    accountPayments.add(new AccountPayment(resultSet));
                }
            }
        }
        return accountPayments;
    }

    public void addAccountPayment(AccountPayment payment) throws SQLException {
        String sql = "INSERT INTO accountPayments(contactID,storeID,invoiceNo,invoiceDate,dueDate,description,unitAmount,accountAdjusted,taxRate) VALUES(?,?,?,?,?,?,?,?,?)";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, payment.getContactID());
            preparedStatement.setInt(2, payment.getStoreID());
            preparedStatement.setString(3, payment.getInvoiceNumber());
            preparedStatement.setDate(4, Date.valueOf(payment.getInvDate()));
            preparedStatement.setDate(5, Date.valueOf(payment.getDueDate()));
            preparedStatement.setString(6, payment.getDescription());
            preparedStatement.setDouble(7, payment.getUnitAmount());
            preparedStatement.setBoolean(8, payment.isAccountAdjusted());
            preparedStatement.setString(9, payment.getTaxRate());

            preparedStatement.executeUpdate();
        }
    }

    public void updateAccountPayment(String originalInvoiceNo,AccountPayment payment) throws SQLException  {
        String sql = "UPDATE accountPayments SET contactID = ?,storeID = ?,invoiceNo = ?, invoiceDate = ?, dueDate = ?,description = ?,unitAmount = ?,accountAdjusted = ?,taxRate = ? WHERE storeID = ? AND invoiceNo = ?";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, payment.getContactID());
            preparedStatement.setInt(2, payment.getStoreID());
            preparedStatement.setString(3, payment.getInvoiceNumber());
            preparedStatement.setDate(4, Date.valueOf(payment.getInvDate()));
            preparedStatement.setDate(5, Date.valueOf(payment.getDueDate()));
            preparedStatement.setString(6, payment.getDescription());
            preparedStatement.setDouble(7, payment.getUnitAmount());
            preparedStatement.setBoolean(8, payment.isAccountAdjusted());
            preparedStatement.setString(9, payment.getTaxRate());
            preparedStatement.setInt(10, payment.getStoreID());
            preparedStatement.setString(11, originalInvoiceNo);

            preparedStatement.executeUpdate();
        }
    }

    public void deleteAccountPayment(int storeId, String invoiceNumber) throws SQLException  {
        String sql = "DELETE from accountPayments WHERE invoiceNo = ? AND storeID = ?";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, invoiceNumber);
            preparedStatement.setInt(2, storeId);

            preparedStatement.executeUpdate();
        }
    }
}
