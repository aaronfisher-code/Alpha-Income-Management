package services;

import models.Store;
import models.User;
import utils.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EmploymentService {
    public void addEmployment(User user, Store store) throws SQLException {
        String sql = "INSERT INTO employments(username,storeID) VALUES(?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setInt(2, store.getStoreID());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteEmploymentsForUser(User user) throws SQLException {
        String sql = "DELETE FROM employments WHERE username = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.executeUpdate();
        }
    }
}
