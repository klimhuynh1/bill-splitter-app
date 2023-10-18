package com.mnfll.bill_splitter_app;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Insert data into tables
 */
public class DataInsertionManager {
    private final JdbcPersonDAO jdbcPersonDAO;

    public DataInsertionManager(JdbcPersonDAO jdbcPersonDAO) {
        this.jdbcPersonDAO = jdbcPersonDAO;
    }
    public List<Integer> insertPeopleData(Expense expense) {
        List<Integer> generatedKeys = new ArrayList<>();
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Check if the name already exists
            String selectQuery = "SELECT person_id FROM people WHERE person_name = ?";

            // Add new records to the `people` table
            String insertQuery = "INSERT INTO people (person_name) VALUES (?)";

            for (String debtorName : expense.getDebtorNames()) {
                // Check if the name already exists
                PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                selectStatement.setString(1, debtorName);
                ResultSet selectResultSet = selectStatement.executeQuery();

                if (selectResultSet.next()) {
                    // Name already exists, retrieve the person id
                    generatedKeys.add(selectResultSet.getInt("person_id"));
                } else {
                    // Name doesn't exist, insert a new record
                    PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, debtorName);

                    int rowsInserted = statement.executeUpdate();
                    if (rowsInserted > 0) {
                        System.out.println("Record inserted into `people` table successfully");

                        // Save the person_id to the ArrayList
                        ResultSet resultSet = statement.getGeneratedKeys();
                        if (resultSet.next()) {
                            generatedKeys.add(resultSet.getInt(1));
                        }
                    } else {
                        System.out.println("Failed to insert into `people` table");
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

    public int insertExpenseData(Expense expense) {
        Connection connection = null;
        int generatedExpenseId = -1;
        int creditorId = jdbcPersonDAO.getPersonIdByName(expense.getCreditorName());

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

    public void insertExpensePersonsData(Expense expense, List<Integer> personIds, int expenseId) {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Retrieve creditor_id based on expenseId
            String selectQuery = "SELECT creditor_id FROM expenses WHERE expense_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
            preparedStatement.setInt(1, expenseId);
            ResultSet resultSet = preparedStatement.executeQuery();
            int creditorId = 0;

            if (resultSet.next()) {
                creditorId = resultSet.getInt("creditor_id");
            }

            // Prepare the insert statement
            String insertQuery = "INSERT INTO expensePersons (expense_id, creditor_id, debtor_id, amount_owed) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement insertTableStatement = connection.prepareStatement(insertQuery);

            for (Integer personId : personIds) {
                insertTableStatement.setInt(1, expenseId);
                insertTableStatement.setInt(2, creditorId);
                insertTableStatement.setInt(3, personId);
                insertTableStatement.setDouble(4, expense.getItemCost() / expense.getDebtorNames().size());

                int rowsInserted = insertTableStatement.executeUpdate();

                if (rowsInserted > 0) {
                    System.out.println("Record inserted into `expensePersons` table successfully");
                } else {
                    System.out.println("Failed to insert into `expensePersons` table");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }
}
