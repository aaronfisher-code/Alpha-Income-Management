package services;

import models.Store;
import utils.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StoreService {
    public List<Store> getAllStores() throws SQLException {
        List<Store> stores = new ArrayList<>();
        String sql = "SELECT * FROM stores";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                stores.add(new Store(resultSet));
            }
        }
        return stores;
    }

    public void addStore(Store store) throws SQLException {
        String sql = "INSERT INTO stores (storeName) VALUES (?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             java.sql.PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, store.getStoreName());
            preparedStatement.executeUpdate();
        }
    }
}
