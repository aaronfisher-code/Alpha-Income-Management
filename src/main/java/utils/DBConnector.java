package utils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConnector {
    public static Connection conDB()
    {
        	try {
                Properties prop = new Properties();
                InputStream in = DBConnector.class.getClassLoader().getResourceAsStream("application.properties");
                prop.load(in);
                assert in != null;
                in.close();

                return DriverManager.getConnection(prop.getProperty("DB_URL"),prop.getProperty("DB_USERNAME"),prop.getProperty("DB_PASSWORD"));

            } catch ( Exception e ) {
                e.printStackTrace();
                return null;
            }
    }
}