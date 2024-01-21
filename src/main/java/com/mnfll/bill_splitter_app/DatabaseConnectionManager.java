package com.mnfll.bill_splitter_app;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
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

    public static boolean tableExistsSQL(Connection conn, String tableName) throws SQLException {
        String query = "SELECT count(*) FROM information_schema.tables WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        PreparedStatement ps = conn.prepareStatement(query);

        ps.setString(1, "bill_splitter_db");
        ps.setString(2, tableName);

        ResultSet rs = ps.executeQuery();
        rs.next();

        return rs.getInt(1) != 0;
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
