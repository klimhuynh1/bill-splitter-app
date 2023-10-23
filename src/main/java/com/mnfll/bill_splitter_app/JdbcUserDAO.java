package com.mnfll.bill_splitter_app;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcUserDAO implements UserDAO {
    // Add data into User table
    public List<Integer> insertUserData(Expense expense) {
        List<Integer> generatedKeys = new ArrayList<>();
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Check if the name already exists
            String selectQuery = "SELECT user_id FROM user WHERE user_name = ?";

            // Add new records to the `user` table
            String insertQuery = "INSERT INTO user (user_name) VALUES (?)";

            for (String debtorName : expense.getDebtorNames()) {
                // Check if the name already exists
                PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                selectStatement.setString(1, debtorName);
                ResultSet selectResultSet = selectStatement.executeQuery();

                if (selectResultSet.next()) {
                    // Name already exists, retrieve the user id
                    generatedKeys.add(selectResultSet.getInt("user_id"));
                } else {
                    // Name doesn't exist, insert a new record
                    PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, debtorName);

                    int rowsInserted = statement.executeUpdate();
                    if (rowsInserted > 0) {
                        System.out.println("Record inserted into `user` table successfully");

                        // Save the user_id to the ArrayList
                        ResultSet resultSet = statement.getGeneratedKeys();
                        if (resultSet.next()) {
                            generatedKeys.add(resultSet.getInt(1));
                        }
                    } else {
                        System.out.println("Failed to insert into `user` table");
                    }

                }
            }
        } catch (
                SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }

        return generatedKeys;
    }

    public int getUserIdByName(String userName) {
        Connection connection = null;
        int userId = -1;
        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Retrieve user_id given user_name
            String selectQuery = "SELECT user_id FROM user WHERE user_name = ? LIMIT 1";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.setString(1, userName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                userId = resultSet.getInt("user_id");
            } else {
                System.out.println("User Id not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
        return userId;
    }

    public List<String> getAllUserNames() {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        List<String> userNames = new ArrayList<>();

        try {
            conn = DatabaseConnectionManager.establishConnection();
            String query = "SELECT user_name FROM user";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                String userName = rs.getString("user_name");
                userNames.add(userName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeConnection(conn);
        }
        return userNames;
    }

    public void deleteOrphanUsers() {
        Connection connection = null;
        PreparedStatement ps1 = null;
        ResultSet rs1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs2 = null;
        PreparedStatement ps3 = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Delete from `user` table if they're not associated with any expense
            String selectQuery = "SELECT user_id fROM user";
            ps1 = connection.prepareStatement(selectQuery);
            rs1 = ps1.executeQuery();

            // Iterate through the result set
            while (rs1.next()) {
                int userId = rs1.getInt("user_id");

                // Check if the user_id exists in the user table
                String checkQuery = "SELECT COUNT(*) FROM user WHERE user_id = ?";
                ps2 = connection.prepareStatement(checkQuery);
                ps2.setInt(1, userId);
                rs2 = ps2.executeQuery();

                int count = rs2.getInt(1);

                if (count == 0) {
                    // Delete the user from the user table
                    String deleteQuery = "DELETE FROM user WHERE user_id = ?";
                    ps3 = connection.prepareStatement(deleteQuery);
                    ps3.setInt(1, userId);
                    int rowsAffected = ps3.executeUpdate();

                    System.out.println(rowsAffected + " rows(s) for user ID: " + userId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePrepapredStatement(ps3);
            ResourcesUtils.closeResultSet(rs2);
            ResourcesUtils.closePrepapredStatement(ps2);
            ResourcesUtils.closeResultSet(rs1);
            ResourcesUtils.closePrepapredStatement(ps1);
            ResourcesUtils.closeConnection(connection);
        }
    }

    public int addNewUserIfNotExist(Connection conn, String debtorName) throws SQLException {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            String selectQuery = "SELECT user_id FROM user WHERE user_name = ?";
            ps1 = conn.prepareStatement(selectQuery);
            ps1.setString(1, debtorName);
            rs = ps1.executeQuery();

            if (rs.next()) {
                // User already exists, retrieve the user id
                return rs.getInt("user_id");
            } else {
                // User does not exist, create a new record
                String insertQuery = "INSERT INTO user (user_name) VALUES (?)";
                ps2 = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                ps2.setString(1, debtorName);
                ps2.executeUpdate();

                // Retrieve the generated user_id
                ResultSet generatedKeys = ps2.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePrepapredStatement(ps2);
            ResourcesUtils.closePrepapredStatement(ps1);
        }

        return -1;
    }
}
