package com.mnfll.bill_splitter_app;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Responsible for managing database connections
 */
public class DatabaseConnectionManager {
    private static final String CONFIG_FILE = "/config.properties";
    private static final String DB_URL_KEY = "db.url";
    private static final String DB_USERNAME_KEY = "db.username";
    private static final String DB_PASSWORD_KEY = "db.password";
    public static Properties loadConfig() {
        Properties config = new Properties();
        try (InputStream is = DatabaseConnectionManager.class.getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                config.load(is);
            } else {
                System.err.println("config.properties file not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }


    public static Connection establishConnection() throws SQLException {
        // Load the configuration from the properties file
        Properties config = loadConfig();

        Connection conn = null;

        // Get the database connection details from the properties file
        String dbUrl = config.getProperty(DB_URL_KEY);
        String dbUsername = config.getProperty(DB_USERNAME_KEY);
        String dbPassword = config.getProperty(DB_PASSWORD_KEY);

        try {
            conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);

        } catch (SQLException e) {
            throw new SQLException("Failed to establish a database connection.", e);
        }
        return conn;
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
