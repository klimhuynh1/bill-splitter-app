package com.mnfll.bill_splitter_app;

import com.mnfll.bill_splitter_app.utilities.InputValidator;

import java.sql.Date;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Coordinate and utilise the other managers. This class remains the entry point for interacting with the data layer but
 * delegates tasks to the specialised class.
 */
public class ExpenseDAO {
    private final JdbcPersonDAO jdbcPersonDAO;

    public ExpenseDAO(JdbcPersonDAO jdbcPersonDAO) {
        this.jdbcPersonDAO = jdbcPersonDAO;
    }

    public void saveExpenseDataToDatabase(Expense expense, TableCreationManager tableCreationManager, DataInsertionManager dataInsertionManager) {
        tableCreationManager.createPeopleTable();
        tableCreationManager.createExpensesTable();
        tableCreationManager.createExpensePersonsTable();
        tableCreationManager.createCombinedExpensePersonsView();

        List<Integer> personIds = dataInsertionManager.insertPeopleData(expense);
        int expenseId = dataInsertionManager.insertExpenseData(expense);
        dataInsertionManager.insertExpensePersonsData(expense, personIds, expenseId);
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
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                creditorId = resultSet.getInt("creditor_id");
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

    public List<String> getAllPeopleNames() {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        List<String> peopleNames = new ArrayList<>();

        try {
            conn = DatabaseConnectionManager.establishConnection();
            String query = "SELECT person_name FROM people";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                String personName = rs.getString("person_name");
                peopleNames.add(personName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeConnection(conn);
        }
        return peopleNames;
    }

    //	TODO: Requires testing
    public void updateExpense(int expenseId, int updateOption, Scanner scanner) throws ParseException {
        switch (updateOption) {
            case 0:
                break;
            case 1:
                updateExpenseDate(expenseId, scanner);
                break;
            case 2:
                updateExpenseEstablishmentName(expenseId, scanner);
                break;
            case 3:
                updateExpenseName(expenseId, scanner);
                break;
            case 4:
                updateExpenseCost(expenseId, scanner);
                break;
            case 5:
                addDebtorName(expenseId, scanner);
                break;
            case 6:
                removeDebtorName(expenseId, scanner);
                break;
            case 7:
                updateCreditorName(expenseId, scanner);
                break;
            case 8:
                updatePaymentStatus(expenseId, scanner);
                break;
            case 9:
                deleteExpense(expenseId);
                break;
            default:
                System.out.println("Invalid update option");
        }
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

    public void removeDebtorName(int expenseId, Scanner scanner) {
        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            System.out.println("Enter the debtor name you would like to remove");
            String debtorName = scanner.nextLine();

            if (InputValidator.isValidName(debtorName)) {
                int personId = jdbcPersonDAO.getPersonIdByName(debtorName);
                int creditorId = getCreditorIdByExpenseId(expenseId);

                // FIXME: temporary fix, will implement either singleton pattern and/or command pattern
                if (personId == creditorId) {
                    throw new IllegalArgumentException("You cannot remove debtor name as they are the creditor");
                } else {
                    // Otherwise, remove record in `expensePersons` table based on person_id and expense_id
                    String query = "DELETE FROM expensePersons WHERE expense_id = ? AND person_id = ?";
                    ps = conn.prepareStatement(query);
                    ps.setInt(1, expenseId);
                    ps.setInt(2, personId);
                    ps.executeUpdate();

                    // Update split_count in `expenses` table based on expense_id
                    int splitCount = updateSplitCount(conn, expenseId, false);
                    // Re-calculate cost per debtor
                    double newAmountOwed = calculateNewAmountOwed(conn, expenseId, splitCount);
                    // Update amount_owed based on expense_id
                    updateAmountOwed(conn, expenseId, newAmountOwed);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePrepapredStatement(ps);
            ResourcesUtils.closeConnection(conn);
        }
    }

    public void addDebtorName(int expenseId, Scanner scanner) {
        Connection conn = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            String newDebtorName = scanner.nextLine();

            if (InputValidator.isValidName(newDebtorName)) {
                int personId = addNewPersonIfNotExists(conn, newDebtorName);
                int splitCount = updateSplitCount(conn, expenseId, true);
                double newAmountOwed = calculateNewAmountOwed(conn, expenseId, splitCount);
                updateAmountOwed(conn, expenseId, newAmountOwed);
                addExpensePersonRecord(conn, expenseId, personId, newAmountOwed);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeConnection(conn);
        }
    }

    //    TODO: Requires testing
    private int addNewPersonIfNotExists(Connection conn, String debtorName) throws SQLException {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            String selectQuery = "SELECT person_id FROM people WHERE person_name = ?";
            ps1 = conn.prepareStatement(selectQuery);
            ps1.setString(1, debtorName);
            rs = ps1.executeQuery();

            if (rs.next()) {
                // Person already exists, retrieve the person id
                return rs.getInt("person_id");
            } else {
                // Person does not exist, create a new record
                String insertQuery = "INSERT INTO people (person_name) VALUES (?)";
                ps2 = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                ps2.setString(1, debtorName);
                ps2.executeUpdate();

                // Retrieve the generated person_id
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

    private int updateSplitCount(Connection conn, int expenseId, Boolean increment) throws SQLException {
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

    private double calculateNewAmountOwed(Connection connection, int expenseId, int splitCount) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String query = "SELECT total_cost FROM expenses WHERE expense_id = ?";
            ps = connection.prepareStatement(query);
            ps.setInt(1, expenseId);
            ResultSet selectResultSet = ps.executeQuery();

            if (selectResultSet.next()) {
                double expenseCost = selectResultSet.getDouble("total_cost");
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

    private void updateAmountOwed(Connection connection, int expenseId, double newAmountOwed) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String query = "UPDATE expensePersons SET amount_owed = ? WHERE expense_id = ?";
            ps = connection.prepareStatement(query);
            ps.setDouble(1, newAmountOwed);
            ps.setInt(2, expenseId);

            int rowsAffected = ps.executeUpdate();
            System.out.println("Update amount owed -- Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePrepapredStatement(ps);
        }
    }

    private void addExpensePersonRecord(Connection connection, int expenseId, int personId, double newAmountOwed) throws SQLException {
        PreparedStatement ps = null;

        try {
            String insertQuery = "INSERT INTO expensePersons (expense_id, person_id, amount_owed) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(insertQuery);
            statement.setInt(1, expenseId);
            statement.setInt(2, personId);
            statement.setDouble(3, newAmountOwed);

            int rowsAffected = statement.executeUpdate();
            System.out.println("Add row for new debtor name -- Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePrepapredStatement(ps);
        }
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


    public void deleteOrphanUsers() {
        Connection connection = null;
        PreparedStatement ps1 = null;
        ResultSet rs1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs2 = null;
        PreparedStatement ps3 = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Delete from `people` table if they're not associated with any expenses
            String selectQuery = "SELECT person_id from people";
            ps1 = connection.prepareStatement(selectQuery);
            rs1 = ps1.executeQuery();

            // Iterate through the result set
            while (rs1.next()) {
                int personId = rs1.getInt("person_id");

                // Check if the person_id exists in the person table
                String checkQuery = "SELECT COUNT(*) FROM expensePersons WHERE person_id = ?";
                ps2 = connection.prepareStatement(checkQuery);
                ps2.setInt(1, personId);
                rs2 = ps2.executeQuery();

                int count = rs2.getInt(1);

                if (count == 0) {
                    // Delete the user from the people table
                    String deleteQuery = "DELETE FROM people WHERE person_id = ?";
                    ps3 = connection.prepareStatement(deleteQuery);
                    ps3.setInt(1, personId);
                    int rowsAffected = ps3.executeUpdate();

                    System.out.println(rowsAffected + " rows(s) for user ID: " + personId);
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
            String sqlQuery = "SELECT debtor_id, debtor_name, amount_owed, payment_status FROM combinedExpensePersons WHERE expense_id = ?";

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

            System.out.println("Enter the ID of debtor to modify payment status");
            String debtorIdModify = scanner.nextLine();

            System.out.println("Enter the new payment status");
            String newPaymentStatus = scanner.nextLine();


            // SQL query to update paymentStatus status
            String updateQuery = "UPDATE expensePersons SET payment_status = ? WHERE expense_id = ? AND debtor_id = ?";

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
            ResourcesUtils.closePrepapredStatement(ps2);
            ResourcesUtils.closeResultSet(rs1);
            ResourcesUtils.closePrepapredStatement(ps1);
            ResourcesUtils.closeConnection(connection);
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

            // Delete users that are no longer associated with any expenses
            deleteOrphanUsers();

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
}