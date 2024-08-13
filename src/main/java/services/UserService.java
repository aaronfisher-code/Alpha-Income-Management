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

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM accounts";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                users.add(new User(resultSet));
            }
        }
        return users;
    }

    public List<User> getAllUserEmployments(int storeID) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM accounts JOIN employments e on accounts.username = e.username WHERE storeID = ? AND inactiveDate IS NULL ";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)){
            preparedStatement.setInt(1, storeID);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(new User(resultSet));
                }
            }
        }
        return users;
    }

    public List<Employment> getEmploymentsForUser(String username) throws SQLException {
        List<Employment> employments = new ArrayList<>();
        String sql = "SELECT * FROM employments WHERE username = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    employments.add(new Employment(resultSet));
                }
            }
        }
        return employments;
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

    public boolean isPasswordResetRequested(String username) throws SQLException {
        String sql = "SELECT CASE WHEN password IS NOT NULL THEN 1 ELSE 0 END AS password_exists FROM accounts WHERE username = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) {
                    return resultSet.getInt("password_exists") == 0;
                }
            }
        }
        return false;
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

    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO accounts(username,first_name,last_name,role,profileBG,profileText) VALUES(?,?,?,?,?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            preparedStatement.setString(3, user.getFirst_name());
            preparedStatement.setString(4, user.getLast_name());
            preparedStatement.setString(5, user.getRole());
            preparedStatement.setString(6, user.getBgColour());
            preparedStatement.setString(7, user.getTextColour());
            preparedStatement.executeUpdate();
        }
    }

    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE accounts SET first_name = ?,last_name = ?,role = ?, profileBG = ?, profileText = ?,inactiveDate = ? WHERE username = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getFirst_name());
            preparedStatement.setString(2, user.getLast_name());
            preparedStatement.setString(3, user.getRole());
            preparedStatement.setString(4, user.getBgColour());
            preparedStatement.setString(5, user.getTextColour());
            if(user.getInactiveDate()!=null)
                preparedStatement.setDate(6, Date.valueOf(user.getInactiveDate()));
            else
                preparedStatement.setNull(6, Types.DATE);
            preparedStatement.setString(7, user.getUsername());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteUser(User user) throws SQLException {
        String sql = "DELETE FROM accounts WHERE username = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.executeUpdate();
        }
    }

    public void resetUserPassword(User user) throws SQLException{
        String sql = "UPDATE accounts SET password = ? WHERE username = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            String hashedPassword = BCrypt.hashpw("password", BCrypt.gensalt());
            preparedStatement.setString(1, hashedPassword);
            preparedStatement.setString(2, user.getUsername());
            preparedStatement.executeUpdate();
        }
    }
}
