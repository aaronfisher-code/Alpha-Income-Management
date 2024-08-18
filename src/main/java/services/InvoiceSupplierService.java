package services;

import models.AccountPaymentContactDataPoint;
import models.Invoice;
import models.InvoiceSupplier;
import utils.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InvoiceSupplierService {
    public List<InvoiceSupplier> getAllInvoiceSuppliers(int storeId) throws SQLException {
        List<InvoiceSupplier> suppliers = new ArrayList<>();
        String sql = "SELECT * FROM invoiceSuppliers where storeID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    suppliers.add(new InvoiceSupplier(resultSet));
                }
            }
        }
        return suppliers;
    }

    public InvoiceSupplier getInvoiceSupplierByName(String supplierName, int storeID) throws SQLException {
        String sql = "SELECT * FROM invoicesuppliers  WHERE supplierName = ? AND storeID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, supplierName);
            preparedStatement.setInt(2, storeID);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new InvoiceSupplier(resultSet);
                }
            }
        }
        return null;
    }

    public void addInvoiceSupplier(InvoiceSupplier invoiceSupplier) throws SQLException {
        String sql = "INSERT INTO invoiceSuppliers(supplierName,storeID) VALUES(?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, invoiceSupplier.getSupplierName());
            preparedStatement.setInt(2, invoiceSupplier.getStoreID());
            preparedStatement.executeUpdate();
        }
    }

    public void updateInvoiceSupplier(InvoiceSupplier invoiceSupplier) throws SQLException {
        String sql = "UPDATE invoiceSuppliers SET supplierName = ? WHERE idinvoiceSuppliers = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, invoiceSupplier.getSupplierName());
            preparedStatement.setInt(2, invoiceSupplier.getContactID());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteInvoiceSupplier(int supplierId) throws SQLException {
        String sql = "DELETE from invoiceSuppliers WHERE idinvoiceSuppliers = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, supplierId);
            preparedStatement.executeUpdate();
        }
    }
}
