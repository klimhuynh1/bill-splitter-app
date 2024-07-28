package com.mnfll.bill_splitter_cli;

import com.mnfll.bill_splitter_cli.utilities.InputValidator;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

/**
 * Coordinate and utilise the other managers. This class remains the entry point for interacting with the data layer but
 * delegates tasks to the specialised class.
 */

// TODO: Add Log4j logging
public class JdbcExpenseDAO implements ExpenseDAO {
    // Add data into Expense table
    public int insertExpenseData(Expense expense) {
        Connection connection = null;
        int generatedExpenseId = -1;
        JdbcUserDAO jdbcUserDAO = new JdbcUserDAO();
        int creditorId = jdbcUserDAO.getUserIdByName(expense.getCreditorName());

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Prepare the insert statement
            String insertQuery = "INSERT INTO expense (expense_date, establishment_name, expense_name, total_cost, " +
                    "split_count, creditor_id, creditor_name) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            // Convert java.util.Date to java.sql.Date
            Date sqlDate = new Date(expense.getDate().getTime());

            PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            statement.setDate(1, sqlDate);
            statement.setString(2, expense.getEstablishmentName());
            statement.setString(3, expense.getItemName());
            statement.setDouble(4, expense.getItemCost());
            statement.setInt(5, expense.getDebtorNames().size());
            statement.setInt(6, creditorId);
            statement.setString(7, expense.getCreditorName());

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Record inserted into `expense` table successfully");

                // Retrieves the expense_id that was generated
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    generatedExpenseId = generatedKeys.getInt(1);
                }
            } else {
                System.out.println("Failed to insert into `expense` table");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeConnection(connection);
        }

        return generatedExpenseId;
    }

    public int getCreditorId(int expenseId) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection conn = null;
        int creditorId = -1;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            // Get the creditor_id for the
            String selectQuery = "SELECT creditor_id FROM expense WHERE expense_id = ? LIMIT 1";
            ps = conn.prepareStatement(selectQuery);
            ps.setInt(1, expenseId);
            rs = ps.executeQuery();
            if (rs.next()) {
                creditorId = rs.getInt("creditor_id");
                System.out.println("Creditor ID found: " + creditorId);
            } else {
                System.out.println("Creditor Id not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePreparedStatement(ps);
            ResourcesUtils.closeConnection(conn);
        }

        return creditorId;
    }

    // TODO: Update both creditor_id and creditor_name
    // TODO: Create a new entry for people table if they don't exist
    public void updateCreditorName(int expenseId, Scanner scanner) {
        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            boolean isValidName = false;
            String newCreditorName = "";

            while (!isValidName) {
                System.out.print("Enter a new creditor name ");
                newCreditorName = scanner.nextLine();

                if (InputValidator.isValidName(newCreditorName)) {
                    isValidName = true;
                } else {
                    System.out.println("Invalid name. Please enter a valid name");
                }
            }

            String updateStatement = "UPDATE expense SET creditor_name = ? WHERE expense_id = ?";
            ps = conn.prepareStatement(updateStatement);
            ps.setString(1, newCreditorName);
            ps.setInt(2, expenseId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Update successful.");
            } else {
                System.out.println("No rows were updated.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps);
            ResourcesUtils.closeConnection(conn);
        }
    }

    //    TODO: Requires testing
    public double calculateNewAmountOwed(Connection connection, int expenseId, int splitCount) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String query = "SELECT total_cost FROM expense WHERE expense_id = ?";
            ps = connection.prepareStatement(query);
            ps.setInt(1, expenseId);
            rs = ps.executeQuery();

            if (rs.next()) {
                double expenseCost = rs.getDouble("total_cost");
                double newAmountOwed = expenseCost / splitCount;
                return Math.round(newAmountOwed * 100.0) / 100.0; // Return the new amount owed rounded to 2 decimal places.6
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePreparedStatement(ps);
        }
        return -1;
    }


    public void updateExpenseEstablishmentName(int expenseId, Scanner scanner) {
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            String establishmentName = null;
            boolean isValidEstablishmentName = false;

            while (!isValidEstablishmentName) {
                System.out.print("Enter the new establishment name (leave blank to remain unchanged) ");
                establishmentName = scanner.nextLine();

                // Leaving establishment name blank will return to previous options
                if (establishmentName.trim().isBlank()) {
                    return;
                } else if (InputValidator.isValidEstablishmentName(establishmentName)) {
                    isValidEstablishmentName = true;
                } else {
                    System.out.println("Invalid establishment name. Please enter a valid establishment name. ");
                }
            }

            // Update establishment_name in `expense` table based on expense_id
            String query = "UPDATE expense SET establishment_name = ? WHERE expense_id = ?";
            ps = connection.prepareStatement(query);

            ps.setString(1, establishmentName);
            ps.setInt(2, expenseId);

            // Execute the update query
            int rowsAffected = ps.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps);
            ResourcesUtils.closeConnection(connection);
        }
    }


    // Update total_cost in `expense` table based on expense_id
    public void updateExpenseCost(int expenseId, Scanner scanner) {
        Connection conn = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;
        PreparedStatement ps3 = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            double expenseCost = 0;
            boolean isValidExpenseCost = false;

            while (!isValidExpenseCost) {
                System.out.print("Enter the new expense cost: (leave blank to remain unchanged) ");
                String userInput = scanner.nextLine();

                if (InputValidator.isValidCost(userInput)) {
                    expenseCost = Double.parseDouble(userInput);
                    isValidExpenseCost = true;
                } else {
                    System.out.print("Invalid expense cost. Please enter a valid expense cost. ");
                }
            }

            // Update expense_cost in `expense` table based on expense_id
            String updateQuery = "UPDATE expense SET total_cost = ? WHERE expense_id = ?";
            ps1 = conn.prepareStatement(updateQuery);
            ps1.setDouble(1, expenseCost);
            ps1.setInt(2, expenseId);

            // Execute the update query
            int rowsUpdatedExpense = ps1.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows updated: " + rowsUpdatedExpense);

            // Get the number of people that splitting this expense cost
            String selectQuery = "SELECT split_count FROM expense WHERE expense_id = ?";
            ps2 = conn.prepareStatement(selectQuery);
            ps2.setInt(1, expenseId);

            // Execute the select query
            rs = ps2.executeQuery();
            int splitCount = 0;
            if (rs.next()) {
                splitCount = rs.getInt("split_count");
            }

            // Re-calculate the cost per person
            double newAmountOwed = expenseCost / splitCount;

            // Update amount_owed in `user_expense` table for each person
            String updateAmountOwedQuery = "UPDATE user_expense SET amount_owed = ? WHERE expense_id = ?";
            ps3 = conn.prepareStatement(updateAmountOwedQuery);
            ps3.setDouble(1, newAmountOwed);
            ps3.setInt(2, expenseId);

            // Execute the update query
            int rowsUpdatedUserExpense = ps3.executeUpdate();

            // Check the number of rows updated
            System.out.println("Rows updated: " + rowsUpdatedUserExpense);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps3);
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePreparedStatement(ps2);
            ResourcesUtils.closePreparedStatement(ps1);
            ResourcesUtils.closeConnection(conn);
        }
    }

    // TODO: Refactor
    public void deleteExpense(int expenseId) {
        Connection connection = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Delete from user_expense table
            String deleteUserExpenseQuery = "DELETE FROM user_expense WHERE expense_id = ?";
            ps1 = connection.prepareStatement(deleteUserExpenseQuery);
            ps1.setInt(1, expenseId);

            // Execute the statement
            int rowsDeleteUserExpense = ps1.executeUpdate();
            if (rowsDeleteUserExpense > 0) {
                System.out.println(rowsDeleteUserExpense + " row(s) deleted successfully from 'user_expense' table.");
            } else {
                System.out.println("No rows deleted from `user_expense` table.");
            }

            // Delete from expense table
            String deleteExpenseQuery = "DELETE FROM expense WHERE expense_id = ?";

            // Prepare the statement
            ps2 = connection.prepareStatement(deleteExpenseQuery);
            ps2.setInt(1, expenseId);

            // Execute the statement
            int rowsDeleteExpense = ps2.executeUpdate();
            if (rowsDeleteExpense > 0) {
                System.out.println(rowsDeleteExpense + " row(s) deleted successfully from `expense` table.");
            } else {
                System.out.println("No rows deleted from `expense` table.");
            }

            JdbcUserDAO jdbcUserDAO = new JdbcUserDAO();
            // Delete users that are no longer associated with any expense
            jdbcUserDAO.deleteOrphanUsers();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps2);
            ResourcesUtils.closePreparedStatement(ps1);
            ResourcesUtils.closeConnection(connection);
        }
    }

    public void displayExpenseTransactions() {
        String query = "SELECT expense_id, expense_date, establishment_name, expense_name, total_cost, split_count, creditor_name " +
                "FROM expense " +
                "ORDER BY expense_date, establishment_name, expense_name, creditor_name";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            // Get metadata to retrieve column names
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Print column headers
            for (int i = 1; i <= columnCount; i++) {
                // Adjust the column with
                System.out.printf("%-20s", metaData.getColumnName(i));
            }
            System.out.println();

            // Print the result set
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.printf("%-20s", rs.getString(i));
                }
                System.out.println();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeConnection(conn);
        }
    }

    public void displayCombinedExpenseTransactions() {
        String query = "SELECT expense_id, expense_date, establishment_name, expense_name, creditor_name, debtor_name, amount_owed, payment_status " +
                "FROM combined_user_expense " +
                "ORDER BY expense_date, establishment_name, expense_name, creditor_name, debtor_name";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            // Get metadata to retrieve column names
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Print column headers
            for (int i = 1; i <= columnCount; i++) {
                // Adjust the column with
                System.out.printf("%-20s", metaData.getColumnName(i));
            }
            System.out.println();

            // Print the result set
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.printf("%-20s", rs.getString(i));
                }
                System.out.println();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeConnection(conn);
        }
    }

    public void displayFilteredCombinedExpenseTransactions(int expenseId) {
        String query = "SELECT expense_id, expense_date, establishment_name, expense_name, creditor_name, debtor_name, amount_owed, payment_status " +
                "FROM combined_user_expense " +
                "WHERE expense_id = ? " +
                "ORDER BY expense_date, establishment_name, expense_name, creditor_name, debtor_name";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            pstmt = conn.prepareStatement(query);

            // Set the parameter for the prepared statement
            pstmt.setInt(1, expenseId);

            rs = pstmt.executeQuery();

            // Get metadata to retrieve column names
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Print column headers
            for (int i = 1; i <= columnCount; i++) {
                // Adjust the column with
                System.out.printf("%-20s", metaData.getColumnName(i));
            }
            System.out.println();

            // Print the result set
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.printf("%-20s", rs.getString(i));
                }
                System.out.println();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePreparedStatement(pstmt);
            ResourcesUtils.closeConnection(conn);
        }
    }

    public boolean expenseIdExists(int expenseId) {
        String query = "SELECT expense_id FROM expense WHERE expense_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            pstmt = conn.prepareStatement(query);

            // Set the parameter for the prepared statement
            pstmt.setInt(1, expenseId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePreparedStatement(pstmt);
            ResourcesUtils.closeConnection(conn);
        }
        return false;
    }

    public int updateSplitCount(Connection conn, int expenseId, Boolean increment) throws SQLException {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            String selectQuery = "SELECT split_count FROM expense WHERE expense_id = ?";
            ps1 = conn.prepareStatement(selectQuery);
            ps1.setInt(1, expenseId);
            rs = ps1.executeQuery();
            int splitCount;

            if (rs.next()) {
                if (increment) {
                    splitCount = rs.getInt("split_count") + 1;
                } else {
                    splitCount = rs.getInt("split_count") - 1;
                }

                String updateQuery = "UPDATE expense SET split_count = ? WHERE expense_id = ?";
                ps2 = conn.prepareStatement(updateQuery);
                ps2.setInt(1, splitCount);
                ps2.setInt(2, expenseId);

                int rowsUpdateExpense = ps2.executeUpdate();
                System.out.println("Update split_count to " + splitCount);
                System.out.println(rowsUpdateExpense + " row(s) updated successfully in the `expense` table.");

                return splitCount;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePreparedStatement(ps2);
            ResourcesUtils.closePreparedStatement(ps1);
        }

        return -1;
    }

    public void updateExpenseDate(int expenseId, Scanner scanner) {
        Connection connection = null;
        PreparedStatement ps = null;


        try {
            connection = DatabaseConnectionManager.establishConnection();
            java.util.Date date = null;
            Date sqlDate = null;
            boolean isValidDate = false;

            while (!isValidDate) {
                System.out.print("Enter the new date [dd/MM/yyyy] (leave blank to remain unchanged) ");
                String dateString = scanner.nextLine();


                if (dateString.trim().isBlank()) {
                    return;
                } else if (InputValidator.isValidDate(dateString)) {
                    try {
                        date = new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    isValidDate = true;
                } else {
                    System.out.println("Invalid date format. Please enter the date in dd/mm/yyyy format. ");
                }
            }

            // Convert java.util.Date to java.sql.Date
            if (date != null) {
                sqlDate = new Date(date.getTime());
            }

            // Update expense_date in `expense` table based on expense_id
            String query = "UPDATE expense SET expense_date = ? WHERE expense_id = ?";
            ps = connection.prepareStatement(query);

            ps.setDate(1, sqlDate);
            ps.setInt(2, expenseId);

            // Execute the update query
            int rowsAffected = ps.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps);
            ResourcesUtils.closeConnection(connection);
        }
    }

    public void updateExpenseName(int expenseId, Scanner scanner) {
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            String expenseName = null;
            boolean isValidExpenseName = false;


            while (!isValidExpenseName) {
                System.out.print("Enter the new expense name (leave blank to remain unchanged). ");
                expenseName = scanner.nextLine();

                if (expenseName.trim().isBlank()) {
                    return;
                } else if (InputValidator.isValidExpenseName(expenseName)) {
                    isValidExpenseName = true;
                } else {
                    System.out.println("Invalid expense name. Please enter a valid expense name. ");
                }
            }

            // Update expense_date in `expense` table based on expense_id
            String query = "UPDATE expense SET expense_name = ? WHERE expense_id = ?";
            ps = connection.prepareStatement(query);

            ps.setString(1, expenseName);
            ps.setInt(2, expenseId);

            // Execute the update query
            int rowsAffected = ps.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePreparedStatement(ps);
            ResourcesUtils.closeConnection(connection);
        }
    }
}