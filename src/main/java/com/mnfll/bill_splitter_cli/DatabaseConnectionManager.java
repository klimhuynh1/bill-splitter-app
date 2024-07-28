package com.mnfll.bill_splitter_cli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Responsible for managing database connections
 */
public class DatabaseConnectionManager {
    private static final Logger logger = LogManager.getLogger(DatabaseConnectionManager.class);
    private static final String CONFIG_FILE = "/config.properties";
    private static final String DB_URL_KEY = "db.url";
    private static final String DB_USERNAME_KEY = "db.username";
    private static final String DB_PASSWORD_KEY = "db.password";
    public static Properties loadConfig() {
        Properties config = new Properties();
        InputStream is;

        try {
            URL resourceUrl = DatabaseConnectionManager.class.getResource(CONFIG_FILE);

            if (resourceUrl != null) {
                // Convert URL to a relative path string for logging
                String relativePath = resourceUrl.getPath();

                // Load properties
                is = resourceUrl.openStream();
                config.load(is);
                logger.info("Config file 'config.properties' successfully loaded from {}", relativePath);
            }else {
                logger.error("Config file 'config.properties' not found");
            }
            } catch (IOException e) {
            logger.error("Error loading config file 'config.properties': {}", e.getMessage(), e);
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
            logger.debug("Connection successfully closed.");
        } catch (SQLException e) {
            logger.error("Failed to establish a database connection. {}", e.getMessage());
        }
        return conn;
    }
}
