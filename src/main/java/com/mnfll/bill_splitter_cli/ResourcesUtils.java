package com.mnfll.bill_splitter_cli;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.sql.*;

public class ResourcesUtils {
    private static final Logger logger = LogManager.getLogger(ResourcesUtils.class);
    public static void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
                logger.debug("ResultSet successfully closed.");            }
        } catch (SQLException e) {
            logger.error("Failed to close ResultSet: {}", e.getMessage(), e);
        }
    }

    public static void closeStatement(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
                logger.debug("Statement successfully closed.");
            }
        } catch (Exception e) {
            logger.error("Failed to close Statement: {}", e.getMessage(), e);
        }
    }

    public static void closePreparedStatement(PreparedStatement pstmt) {
        try {
            if (pstmt != null) {
                pstmt.close();
                logger.debug("PreparedStatement successfully closed.");
            }
        } catch (SQLException e) {
            logger.error("Failed to close PreparedStatement: {}", e.getMessage(), e);
        }
    }

    public static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
                logger.debug("Connection successfully closed.");
            }
        } catch (SQLException e) {
            logger.error("Failed to close Connection: {}", e.getMessage(), e);
        }
    }
}
