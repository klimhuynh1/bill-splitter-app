package com.mnfll.bill_splitter_cli;

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
        PreparedStatement selectStatement = null;
        PreparedStatement checkStatement = null;
        PreparedStatement deleteStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();

            // Query to select all the user IDS in `user` table
            String selectQuery = "SELECT user_id FROM user";
            selectStatement = connection.prepareStatement(selectQuery);
            resultSet = selectStatement.executeQuery();

            // Query to count the number of times a user_id shows up in the `user_expense` table
            String checkQuery = "SELECT COUNT(*) FROM user_expense WHERE debtor_id = ?";
            checkStatement = connection.prepareStatement(checkQuery);

            String deleteQuery = "DELETE FROM user WHERE user_id = ?";
            deleteStatement = connection.prepareStatement(deleteQuery);

            // Iterate through the result set
            while (resultSet.next()) {
                int userId = resultSet.getInt(1);

                // Check if the user_id exists in the `user_expense` table
                checkStatement.setInt(1, userId);
                ResultSet countResultSet = checkStatement.executeQuery();

                if (countResultSet.next()) {
                    int count = countResultSet.getInt(1);

                    // Delete from `user` table if user is not associated with any expenses
                    if (count == 0) {
                        // Delete the user from the user table
                        deleteStatement.setInt(1, userId);
                        int rowsAffected = deleteStatement.executeUpdate();
                        System.out.println(rowsAffected + " row(s) deleted for user ID: " + userId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(deleteStatement);
            ResourcesUtils.closePreparedStatement(checkStatement);
            ResourcesUtils.closeResultSet(resultSet);
            ResourcesUtils.closePreparedStatement(selectStatement);
            ResourcesUtils.closeConnection(connection);
        }
    }

    public int getUserId(Connection conn, String debtorName) {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String selectQuery = "SELECT user_id FROM user WHERE user_name = ?";
            ps = conn.prepareStatement(selectQuery);
            ps.setString(1, debtorName);
            rs = ps.executeQuery();

            if (rs.next()) {
                // User exists, return the user id
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePreparedStatement(ps);
        }

        return -1;
    }

    public int addNewUser(Connection conn, String debtorName) {
        PreparedStatement ps = null;

        try {
            String insertQuery = "INSERT INTO user (user_name) VALUES (?)";
            ps = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, debtorName);
            ps.executeUpdate();

            // Retrieve the generated user_id
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException("Creating user failed, no user ID obtained.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps);
        }

        return -1;
    }
}
