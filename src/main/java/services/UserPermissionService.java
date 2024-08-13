package services;

import models.Permission;
import models.User;
import utils.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserPermissionService {
    public void addUserPermission(User user, Permission permission) throws SQLException {
        String sql = "INSERT INTO userpermissions(userID,permissionID) VALUES(?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setInt(2, permission.getPermissionID());
            preparedStatement.executeUpdate();
        }
    }

    public void deletePermissionsForUser(User user) throws SQLException {
        String sql = "DELETE from userpermissions WHERE userID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.executeUpdate();
        }
    }
}
