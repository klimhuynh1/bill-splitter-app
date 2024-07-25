package com.mnfll.bill_splitter_cli;

import java.sql.*;

import static com.mnfll.bill_splitter_cli.LoggerUtils.logger;

public class ResourcesUtils {
    public static void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close ResultSet: {}", e.getMessage(), e);
        }
    }

    public static void closeStatement(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception e) {
            logger.error("Failed to close Statement: {}", e.getMessage(), e);
        }
    }

    public static void closePreparedStatement(PreparedStatement pstmt) {
        try {
            if (pstmt != null) {
                pstmt.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close PreparedStatement: {}", e.getMessage(), e);
        }
    }

    public static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close Connection: {}", e.getMessage(), e);
        }
    }
}
