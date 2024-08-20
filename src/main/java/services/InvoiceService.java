package services;

import models.CellDataPoint;
import models.Invoice;
import utils.DatabaseConnectionManager;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class InvoiceService {
    public Invoice getInvoice(String invoiceID) throws SQLException {
        String sql = "SELECT * FROM invoicedatapoints WHERE invoiceNo = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, invoiceID);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new Invoice(resultSet);
                }
            }
        }
        return null;
    }

    public List<Invoice> getAllInvoices(int storeId, YearMonth yearMonth) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices JOIN invoicesuppliers a on a.idinvoiceSuppliers = invoices.supplierID JOIN invoicedatapoints i on invoices.invoiceNo = i.invoiceNo WHERE invoices.storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setInt(2, yearMonth.getMonthValue());
            preparedStatement.setInt(3, yearMonth.getYear());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    invoices.add(new Invoice(resultSet));
                }
            }
        }
        return invoices;
    }

    public double getTotalInvoiceAmount(int storeId, YearMonth yearMonth) throws SQLException {
    String sql = "SELECT SUM(unitAmount) AS total FROM invoices WHERE storeID = ? AND MONTH(invoiceDate) = ? AND YEAR(invoiceDate) = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setInt(2, yearMonth.getMonthValue());
            preparedStatement.setInt(3, yearMonth.getYear());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("total");
                }
            }
        }
        return 0;
    }

    public List<Invoice> getInvoiceTableData(int storeId, YearMonth yearMonth) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = """
				SELECT
					invoices.*,
					i.*,
					idp.*,
					(SELECT SUM(credits.creditAmount)
					 FROM credits
					 WHERE credits.referenceInvoiceNo = invoices.invoiceNo) AS total_credits
				FROM
					invoices
				JOIN
					invoiceSuppliers i ON invoices.supplierID = i.idinvoiceSuppliers
				LEFT JOIN
					invoicedatapoints idp ON invoices.invoiceNo = idp.invoiceNo
				WHERE
					invoices.storeID = ?
					AND MONTH(invoices.invoiceDate) = ?
					AND YEAR(invoices.invoiceDate) = ?
				ORDER BY
					invoices.invoiceNo ASC
				""";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeId);
            preparedStatement.setInt(2, yearMonth.getMonthValue());
            preparedStatement.setInt(3, yearMonth.getYear());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    invoices.add(new Invoice(resultSet));
                }
            }
        }
        return invoices;
    }

    public boolean checkDuplicateInvoice(String invoiceID, int storeID, int supplierID) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE invoiceNo = ? AND storeID = ? AND supplierID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, invoiceID);
            preparedStatement.setInt(2, storeID);
            preparedStatement.setInt(3, supplierID);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void addInvoice(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices(supplierID,invoiceNo,invoiceDate,dueDate,description,unitAmount,notes,storeID) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, invoice.getSupplierID());
            preparedStatement.setString(2, invoice.getInvoiceNo());
            preparedStatement.setDate(3, Date.valueOf(invoice.getInvoiceDate()));
            preparedStatement.setDate(4, Date.valueOf(invoice.getDueDate()));
            preparedStatement.setString(5, invoice.getDescription());
            preparedStatement.setDouble(6, invoice.getUnitAmount());
            preparedStatement.setString(7, invoice.getNotes());
            preparedStatement.setInt(8, invoice.getStoreID());
            preparedStatement.executeUpdate();
        }
    }

    public void updateInvoice(Invoice invoice, String originalInvoiceNo) throws SQLException {
        String sql = "UPDATE invoices SET supplierID = ?,storeID = ?,invoiceNo = ?, invoiceDate = ?, dueDate = ?,description = ?,unitAmount = ?,notes = ? WHERE invoiceNo = ? AND storeID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, invoice.getSupplierID());
            preparedStatement.setInt(2, invoice.getStoreID());
            preparedStatement.setString(3, invoice.getInvoiceNo());
            preparedStatement.setDate(4, Date.valueOf(invoice.getInvoiceDate()));
            preparedStatement.setDate(5, Date.valueOf(invoice.getDueDate()));
            preparedStatement.setString(6, invoice.getDescription());
            preparedStatement.setDouble(7, invoice.getUnitAmount());
            preparedStatement.setString(8, invoice.getNotes());
            preparedStatement.setString(9, originalInvoiceNo);
            preparedStatement.setInt(10, invoice.getStoreID());
            preparedStatement.executeUpdate();
        }
    }

    public void deleteInvoice(String invoiceID, int storeID) throws SQLException {
        String sql = "DELETE from invoices WHERE invoiceNo = ? AND storeID = ?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, invoiceID);
            preparedStatement.setInt(2, storeID);
            preparedStatement.executeUpdate();
        }
    }

    public void importInvoiceData(int storeID, CellDataPoint cdp) throws SQLException {
        String sql = "INSERT INTO invoicedatapoints(storeID,invoiceNo,amount) VALUES(?,?,?) ON DUPLICATE KEY UPDATE amount=?";
        try (Connection connection = DatabaseConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, storeID);
            preparedStatement.setString(2, cdp.getCategory());
            preparedStatement.setDouble(3, cdp.getAmount());
            preparedStatement.setDouble(4, cdp.getAmount());
            preparedStatement.executeUpdate();
        }
    }
}
