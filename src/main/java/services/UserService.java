package services;

import models.Employment;
import models.Store;
import models.User;
import models.Permission;
import org.mindrot.jbcrypt.BCrypt;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE username = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(resultSet);
                }
            }
        }
        return null;
    }

    public boolean verifyPassword(User user, String password) {
        return BCrypt.checkpw(password, user.getPassword());
    }

    public void updateUserPassword(String username, String newPassword) throws SQLException {
        String sql = "UPDATE accounts SET password = ? WHERE username = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            preparedStatement.setString(1, hashedPassword);
            preparedStatement.setString(2, username);
            preparedStatement.executeUpdate();
        }
    }

    public List<Permission> getUserPermissions(String username) throws SQLException {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT * FROM permissions WHERE permissionID IN (SELECT permissionID FROM userpermissions WHERE userID = ?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    permissions.add(new Permission(resultSet));
                }
            }
        }
        return permissions;
    }

    public List<Store> getStoresForUser(String username) throws SQLException {
        List<Store> stores = new ArrayList<>();
        String sql = "SELECT * FROM employments JOIN stores a on a.storeID = employments.storeID where username = ?";

        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    stores.add(new Store(resultSet));
                }
            }
        }
        return stores;
    }
}
