package services;

import models.AccountPaymentContactDataPoint;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountPaymentContactService {

    public List<AccountPaymentContactDataPoint> getAllAccountPaymentContacts(int storeId) throws SQLException {
        List<AccountPaymentContactDataPoint> contacts = new ArrayList<>();
        String sql = "SELECT * FROM accountPaymentContacts where storeID = ?";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    contacts.add(new AccountPaymentContactDataPoint(resultSet));
                }
            }
        }
        return contacts;
    }

    public void addAccountPaymentContact(AccountPaymentContactDataPoint contact) throws SQLException {
        String sql = "INSERT INTO accountPaymentContacts(contactName,storeID,accountCode) VALUES(?,?,?)";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, contact.getContactName());
            preparedStatement.setInt(2, contact.getStoreID());
            preparedStatement.setString(3, contact.getAccountCode());

            preparedStatement.executeUpdate();
        }
    }

    public void updateAccountPaymentContact(AccountPaymentContactDataPoint contact) throws SQLException {
        String sql = "UPDATE accountPaymentContacts SET contactName = ?, accountCode = ? WHERE idaccountPaymentContacts = ?";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, contact.getContactName());
            preparedStatement.setString(2, contact.getAccountCode());
            preparedStatement.setInt(3, contact.getContactID());

            preparedStatement.executeUpdate();
        }
    }

    public void deleteAccountPaymentContact(int contactId) throws SQLException {
        String sql = "DELETE from accountPaymentContacts WHERE idaccountPaymentContacts = ?";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, contactId);

            preparedStatement.executeUpdate();
        }
    }

    public AccountPaymentContactDataPoint getContactByName(String name, int storeId) throws SQLException {
        String sql = "SELECT * FROM accountPaymentContacts WHERE contactName = ? AND storeID = ?";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, storeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new AccountPaymentContactDataPoint(resultSet);
                }
            }
        }
        return null;
    }
}
