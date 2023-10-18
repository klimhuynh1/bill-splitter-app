package com.mnfll.bill_splitter_app;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Create tables if they don't exist
 */
public class TableCreationManager {
    public void createPeopleTable() {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Create the 'people' table if it doesn't exist
            String createTableQuery = "CREATE TABLE IF NOT EXISTS people (" +
                    "person_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "person_name VARCHAR(255)" +
                    ")";

            Statement createTableStatement = connection.createStatement();
            int tableCreated = createTableStatement.executeUpdate(createTableQuery);

            if (tableCreated >= 0) {
                System.out.println("Table `people` created successfully");
            } else {
                System.out.println("Failed to create table `people`");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    public void createExpensesTable() {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Create the table if it doesn't exist
            String createTableQuery = "CREATE TABLE IF NOT EXISTS expenses (" +
                    "expense_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "expense_date DATE NOT NULL, " +
                    "establishment_name VARCHAR(255) NOT NULL, " +
                    "expense_name VARCHAR(255) NOT NULL, " +
                    "total_cost DECIMAL(10,2) NOT NULL, " +
                    "split_count INT NOT NULL, " +
                    "creditor_id INT NOT NULL," +
                    "creditor_name VARCHAR(255) NOT NULL" +
                    ")";

            Statement createTableStatement = connection.createStatement();
            int tableCreated = createTableStatement.executeUpdate(createTableQuery);

            if (tableCreated >= 0) {
                System.out.println("Table 'expenses' created successfully");
            } else {
                System.out.println("Failed to create table 'expenses'");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }
    public void createExpensePersonsTable() {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Create the 'expensePersons' table if it doesn't exist
            String createQuery = "CREATE TABLE IF NOT EXISTS expensePersons (" +
                    "expense_id INT NOT NULL, " +
                    "creditor_id INT NOT NULL, " +
                    "debtor_id INT NOT NULL, " +
                    "amount_owed DECIMAL(10,2) NOT NULL, " +
                    "payment_status CHAR(1) DEFAULT 'n' CHECK (payment_status IN ('y', 'n')), " +
                    "PRIMARY KEY (expense_id, debtor_id), " +
                    "FOREIGN KEY (expense_id) REFERENCES expenses(expense_id), " +
                    "FOREIGN KEY (debtor_id) REFERENCES people(person_id)" +
                    ")";

            Statement createTableStatement = connection.createStatement();
            int tableCreated = createTableStatement.executeUpdate(createQuery);

            if (tableCreated >= 0) {
                System.out.println("Table 'expensePersons' created successfully");
            } else {
                System.out.println("Failed to create table 'expensePersons'");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    public void createCombinedExpensePersonsView() {
        Connection connection = null;
        String createViewQuery = "CREATE OR REPLACE VIEW combinedExpensePersons AS " +
                "SELECT ep.expense_id, e.expense_date, e.establishment_name, e.expense_name, " +
                "ep.creditor_id, p1.person_name AS creditor_name, " +
                "ep.debtor_id, p2.person_name AS debtor_name, ep.amount_owed, ep.payment_status " +
                "FROM expensePersons ep " +
                "JOIN people p1 ON ep.creditor_id = p1.person_id " +
                "JOIN people p2 ON ep.debtor_id = p2.person_id " +
                "JOIN expenses e ON ep.expense_id = e.expense_id";

        try {
            connection = DatabaseConnectionManager.establishConnection();
            Statement statement = connection.createStatement();
            statement.execute(createViewQuery);
            System.out.println("View `createCombinedExpensePersons` created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }
}
