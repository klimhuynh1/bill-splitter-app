package com.mnfll.bill_splitter_app;

import com.mnfll.bill_splitter_app.utilities.InputValidator;

import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Coordinate and utilise the other managers. This class remains the entry point for interacting with the data layer but
 * delegates tasks to the specialised class.
 */
public class JdbcExpensesDAO implements ExpensesDAO {

    public int insertExpenseData(Expense expense) {
        Connection connection = null;
        int generatedExpenseId = -1;
        JdbcPeopleDAO jdbcPeopleDAO = new JdbcPeopleDAO();
        int creditorId = jdbcPeopleDAO.getPersonIdByName(expense.getCreditorName());

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Prepare the insert statement
            String insertQuery = "INSERT INTO expenses (expense_date, expense_name, establishment_name, total_cost, " +
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
                System.out.println("Record inserted into `expenses` table successfully");

                // Retrieves the expense_id that was generated
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    generatedExpenseId = generatedKeys.getInt(1);
                }
            } else {
                System.out.println("Failed to insert into `expenses` table");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }

        return generatedExpenseId;
    }

    public int getCreditorIdByExpenseId(int expenseId) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection conn = null;
        int creditorId = -1;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            // Get the creditor_id for the
            String selectQuery = "SELECT creditor_id FROM expenses WHERE expense_id = ? LIMIT 1";
            ps = conn.prepareStatement(selectQuery);
            ps.setInt(1, expenseId);
            rs = ps.executeQuery();
            if (rs.next()) {
                creditorId = rs.getInt("creditor_id");
                System.out.println("creditor ID: " + creditorId);
            } else {
                System.out.println("creditor Id not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePrepapredStatement(ps);
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
                System.out.println("Enter a new creditor name");
                newCreditorName = scanner.nextLine();

                if (InputValidator.isValidName(newCreditorName)) {
                    isValidName = true;
                } else {
                    System.out.println("Invalid name. Please enter a valid name");
                }
            }

            String updateStatement = "UPDATE expenses SET creditor_name = ? WHERE expense_id = ?";
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
            ResourcesUtils.closePrepapredStatement(ps);
            ResourcesUtils.closeConnection(conn);
        }
    }


    //    TODO: Requires testing
    public double calculateNewAmountOwed(Connection connection, int expenseId, int splitCount) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String query = "SELECT total_cost FROM expenses WHERE expense_id = ?";
            ps = connection.prepareStatement(query);
            ps.setInt(1, expenseId);
            rs = ps.executeQuery();

            if (rs.next()) {
                double expenseCost = rs.getDouble("total_cost");
                return expenseCost / splitCount;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePrepapredStatement(ps);
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
                System.out.println("Enter the new establishment name");
                establishmentName = scanner.nextLine();

                if (InputValidator.isValidEstablishmentName(establishmentName)) {

                    isValidEstablishmentName = true;
                } else {
                    System.out.println("Invalid establishment name. Please enter a valid establishment name. ");
                }
            }

            // Update establishment_name in `expenses` table based on expense_id
            String query = "UPDATE expenses SET establishment_name = ? WHERE expense_id = ?";
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
            ResourcesUtils.closePrepapredStatement(ps);
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
                System.out.println("Enter the new expense cost:");
                String userInput = scanner.nextLine();

                if (InputValidator.isValidCost(userInput)) {
                    expenseCost = Double.parseDouble(userInput);
                    isValidExpenseCost = true;
                } else {
                    System.out.print("Invalid expense cost. Please enter a valid expense cost. ");
                }
            }

            // Update expense_cost in `expenses` table based on expense_id
            String updateQuery = "UPDATE expenses SET total_cost = ? WHERE expense_id = ?";
            ps1 = conn.prepareStatement(updateQuery);
            ps1.setDouble(1, expenseCost);
            ps1.setInt(2, expenseId);

            // Execute the update query
            int rowsAffected = ps1.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows affected: " + rowsAffected);

            // Get the number of people that splitting this expense cost
            String selectQuery = "SELECT split_count FROM expenses WHERE expense_id = ?";
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

            // Update amount_owed in `expensePersons` table for each person
            String updateAmountOwedQuery = "UPDATE expensePersons SET amount_owed = ? WHERE expense_id = ?";
            ps3 = conn.prepareStatement(updateAmountOwedQuery);
            ps3.setDouble(1, newAmountOwed);
            ps3.setInt(2, expenseId);

            // Execute the update query
            int rowsUpdated = ps3.executeUpdate();

            // Check the number of rows updated
            System.out.println("Rows updated: " + rowsUpdated);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePrepapredStatement(ps3);
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePrepapredStatement(ps2);
            ResourcesUtils.closePrepapredStatement(ps1);
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
            // Delete from expensePersons table
            String deleteExpensePersonsQuery = "DELETE FROM expensePersons WHERE expense_id = ?";
            ps1 = connection.prepareStatement(deleteExpensePersonsQuery);
            ps1.setInt(1, expenseId);

            // Execute the statement
            int expensePersonsRowsAffected = ps1.executeUpdate();
            if (expensePersonsRowsAffected > 0) {
                System.out.println(expensePersonsRowsAffected + " row(s) deleted successfully from 'expensePersons' table.");
            } else {
                System.out.println("No rows deleted from 'expensePersons' table.");
            }

            // Delete from expenses table
            String deleteExpensesQuery = "DELETE FROM expenses WHERE expense_id = ?";

            // Prepare the statement
            ps2 = connection.prepareStatement(deleteExpensesQuery);
            ps2.setInt(1, expenseId);

            // Execute the statement
            int expensesRowsAffected = ps2.executeUpdate();
            if (expensesRowsAffected > 0) {
                System.out.println(expensesRowsAffected + " row(s) deleted successfully from 'expenses' table.");
            } else {
                System.out.println("No rows deleted from 'expenses' table.");
            }

            JdbcPeopleDAO jdbcPeopleDAO = new JdbcPeopleDAO();
            // Delete users that are no longer associated with any expenses
            jdbcPeopleDAO.deleteOrphanUsers();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePrepapredStatement(ps2);
            ResourcesUtils.closePrepapredStatement(ps1);
            ResourcesUtils.closeConnection(connection);
        }
    }

    public void displayAllExpenseTransactions() {
        String query = "SELECT expense_date, establishment_name, expense_name, creditor_name, " + "debtor_name, amount_owed, payment_status FROM combinedExpensePersons ORDER BY " + "expense_date, establishment_name, expense_name, creditor_name, debtor_name";
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

    public int updateSplitCount(Connection conn, int expenseId, Boolean increment) throws SQLException {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            String selectQuery = "SELECT split_count FROM expenses WHERE expense_id = ?";
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

                String updateQuery = "UPDATE expenses SET split_count = ? WHERE expense_id = ?";
                ps2 = conn.prepareStatement(updateQuery);
                ps2.setInt(1, splitCount);
                ps2.setInt(2, expenseId);

                int rowsAffected = ps2.executeUpdate();
                System.out.println("Update split_count -- Rows affected: " + rowsAffected);

                return splitCount;
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

    public void updateExpenseDate(int expenseId, Scanner scanner) {
        Connection connection = null;
        PreparedStatement ps = null;


        try {
            connection = DatabaseConnectionManager.establishConnection();
            java.util.Date date = null;
            Date sqlDate = null;
            boolean isValidDate = false;

            while (!isValidDate) {
                System.out.println("Enter the new date [dd/MM/yyyy]");
                String dateString = scanner.nextLine();


                if (InputValidator.isValidDate(dateString)) {
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

            // Update expense_date in `expenses` table based on expense_id
            String query = "UPDATE expenses SET expense_date = ? WHERE expense_id = ?";
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
            ResourcesUtils.closePrepapredStatement(ps);
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
                System.out.println("Enter the new expense name");
                expenseName = scanner.nextLine();

                if (InputValidator.isValidExpenseName(expenseName)) {
                    isValidExpenseName = true;
                } else {
                    System.out.println("Invalid expense name. Please enter a valid expense name. ");
                }
            }

            // Update expense_date in `expenses` table based on expense_id
            String query = "UPDATE expenses SET expense_name = ? WHERE expense_id = ?";
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
            ResourcesUtils.closePrepapredStatement(ps);
            ResourcesUtils.closeConnection(connection);
        }
    }
}