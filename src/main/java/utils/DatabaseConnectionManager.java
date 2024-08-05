package utils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnectionManager {
    private static final Properties prop = new Properties();

    static {
        try (InputStream in = DatabaseConnectionManager.class.getClassLoader().getResourceAsStream("application.properties")) {
            prop.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(
                    prop.getProperty("DB_URL"),
                    prop.getProperty("DB_USERNAME"),
                    prop.getProperty("DB_PASSWORD")
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
