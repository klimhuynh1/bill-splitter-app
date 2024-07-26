package com.mnfll.bill_splitter_cli;

import com.mnfll.bill_splitter_cli.utilities.InputValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class JdbcUserExpenseDAO implements UserExpenseDAO {
    public void insertUserExpenseData(Expense expense, List<Integer> personIds, int expenseId) {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Retrieve creditor_id based on expenseId
            String selectQuery = "SELECT creditor_id FROM expense WHERE expense_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
            preparedStatement.setInt(1, expenseId);
            ResultSet resultSet = preparedStatement.executeQuery();
            int creditorId = 0;

            if (resultSet.next()) {
                creditorId = resultSet.getInt("creditor_id");
            }

            // Prepare the insert statement
            String insertQuery = "INSERT INTO user_expense (expense_id, creditor_id, debtor_id, amount_owed) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement insertTableStatement = connection.prepareStatement(insertQuery);

            for (Integer personId : personIds) {
                insertTableStatement.setInt(1, expenseId);
                insertTableStatement.setInt(2, creditorId);
                insertTableStatement.setInt(3, personId);
                insertTableStatement.setDouble(4, expense.getItemCost() / expense.getDebtorNames().size());

                int rowsInserted = insertTableStatement.executeUpdate();

                if (rowsInserted > 0) {
                    System.out.println("Record inserted into `user_expense` table successfully");
                } else {
                    System.out.println("Failed to insert into `user_expense` table");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    // TODO: Allow multiple updates
    // TODO: Input validation for user inputs
    public void updatePaymentStatus(int expenseId, Scanner scanner) {
        Connection connection = null;
        PreparedStatement ps1 = null;
        ResultSet rs1 = null;
        PreparedStatement ps2 = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Create a SQL query
            String sqlQuery = "SELECT debtor_id, debtor_name, amount_owed, payment_status FROM combinedUser_expense WHERE expense_id = ?";

            // Create a Statement object
            ps1 = connection.prepareStatement(sqlQuery);

            // Set value for the placeholder in the WHERE clause
            ps1.setInt(1, expenseId);

            // Execute the query and get the result set
            rs1 = ps1.executeQuery();

            // Iterate through the result set and print data
            while (rs1.next()) {
                int debtorId = rs1.getInt("debtor_id");
                String debtorName = rs1.getString("debtor_name");
                double amountOwed = rs1.getDouble("amount_owed");
                char paymentStatus = rs1.getString("payment_status").charAt(0);

                // Print id and name of debtors associated with this expense
                System.out.println("id: " + debtorId + ", name: " + debtorName + ", amount owed: " + amountOwed + ", payment status: " + paymentStatus);
            }

            System.out.print("Enter the ID of debtor to modify payment status ");
            String debtorIdModify = scanner.nextLine();

            System.out.print("Enter the new payment status ");
            String newPaymentStatus = scanner.nextLine();


            // SQL query to update paymentStatus status
            String updateQuery = "UPDATE user_expense SET payment_status = ? WHERE expense_id = ? AND debtor_id = ?";

            // Prepare the statement
            ps2 = connection.prepareStatement(updateQuery);

            // Set new values for columns
            ps2.setString(1, newPaymentStatus);
            ps2.setInt(2, expenseId);
            ps2.setInt(3, Integer.parseInt(debtorIdModify));

            // Execute the update
            int rowsUpdated = ps2.executeUpdate();
            System.out.println("Rows updated: " + rowsUpdated);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps2);
            ResourcesUtils.closeResultSet(rs1);
            ResourcesUtils.closePreparedStatement(ps1);
            ResourcesUtils.closeConnection(connection);
        }
    }

    public void addDebtorName(int expenseId, Scanner scanner) {
        Connection conn = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            System.out.print("Please provide the debtor's name ");
            String newDebtorName = scanner.nextLine();

            if (InputValidator.isValidName(newDebtorName)) {
                JdbcUserDAO jdbcUserDAO = new JdbcUserDAO();
                JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();

                int userId;

                if (jdbcUserDAO.getUserId(conn, newDebtorName) == -1) {
                    userId = jdbcUserDAO.addNewUser(conn, newDebtorName);
                } else {
                    userId = jdbcUserDAO.getUserId(conn, newDebtorName);
                }

                int creditorId = jdbcExpenseDAO.getCreditorId(expenseId);
                int splitCount = jdbcExpenseDAO.updateSplitCount(conn, expenseId, true);

                if (isPersonAndSplitCountValid(userId, splitCount)) {
                    double newAmountOwed = jdbcExpenseDAO.calculateNewAmountOwed(conn, expenseId, splitCount);

                    if (newAmountOwed != -1) {
                        updateAmountOwed(conn, expenseId, newAmountOwed);
                        addUserExpenseRecord(conn, expenseId, creditorId, userId, newAmountOwed);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeConnection(conn);
        }
    }

    public boolean isPersonAndSplitCountValid(int personId, int splitCount) {
        return (personId != -1 && splitCount != -1);
    }

    public void removeDebtorName(int expenseId, Scanner scanner) {
        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            System.out.print("Enter the debtor name you would like to remove ");
            String debtorName = scanner.nextLine();
            JdbcUserDAO jdbcUserDAO = new JdbcUserDAO();
            JdbcExpenseDAO jdbcExpensesDAO = new JdbcExpenseDAO();

            if (InputValidator.isValidName(debtorName)) {
                int personId = jdbcUserDAO.getUserIdByName(debtorName);
                int creditorId = jdbcExpensesDAO.getCreditorId(expenseId);

                // FIXME: temporary fix, will implement either singleton pattern and/or command pattern
                if (personId == creditorId) {
                    throw new IllegalArgumentException("You cannot remove debtor name as they are the creditor");
                } else {
                    // Otherwise, remove record in `user_expense` table based on person_id and expense_id
                    String query = "DELETE FROM user_expense WHERE expense_id = ? AND person_id = ?";
                    ps = conn.prepareStatement(query);
                    ps.setInt(1, expenseId);
                    ps.setInt(2, personId);
                    ps.executeUpdate();

                    // Update split_count in `expenses` table based on expense_id
                    int splitCount = jdbcExpensesDAO.updateSplitCount(conn, expenseId, false);
                    // Re-calculate cost per debtor
                    double newAmountOwed = jdbcExpensesDAO.calculateNewAmountOwed(conn, expenseId, splitCount);

                    if (newAmountOwed != -1) {
                        // Update amount_owed based on expense_id
                        updateAmountOwed(conn, expenseId, newAmountOwed);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps);
            ResourcesUtils.closeConnection(conn);
        }
    }

    public void addUserExpenseRecord(Connection conn, int expenseId, int creditorId, int debtorId, double newAmountOwed) throws SQLException {
        PreparedStatement ps = null;

        try {
            String insertQuery = "INSERT INTO user_expense (expense_id, creditor_id, debtor_id, amount_owed) VALUES (?, ?, ?, ?)";
            ps = conn.prepareStatement(insertQuery);
            ps.setInt(1, expenseId);
            ps.setInt(2, creditorId);
            ps.setInt(3, debtorId);
            ps.setDouble(4, newAmountOwed);

            int rowsAffected = ps.executeUpdate();
            System.out.println(rowsAffected + " row(s) updated successfully in the `user_expense` table");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps);
        }
    }

    public void updateAmountOwed(Connection conn, int expenseId, double newAmountOwed) throws SQLException {
        PreparedStatement ps = null;

        try {
            String query = "UPDATE user_expense SET amount_owed = ? WHERE expense_id = ?";
            ps = conn.prepareStatement(query);
            ps.setDouble(1, newAmountOwed);
            ps.setInt(2, expenseId);

            int rowsAffected = ps.executeUpdate();
            System.out.println("Update amount_owed for each person to " + newAmountOwed);
            System.out.println(rowsAffected + " row(s) updated successfully in the `user_expense` table");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps);
        }
    }
}