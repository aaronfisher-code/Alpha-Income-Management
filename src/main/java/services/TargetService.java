package services;

import utils.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TargetService {
    public double[] getTargets(int storeId, String targetName) throws SQLException {
        String sql = "SELECT * FROM targets WHERE storeID = ? AND targetName = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setString(2, targetName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new double[]{
                            resultSet.getDouble("target1"),
                            resultSet.getDouble("target2")
                    };
                }
            }
        }
        return new double[]{0, 0};
    }
}
