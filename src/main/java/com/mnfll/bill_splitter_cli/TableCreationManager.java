package com.mnfll.bill_splitter_cli;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Create tables if they don't exist
 */
public class TableCreationManager {
    public void createUserTable() {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Create the `user` table if it doesn't exist
            String createTableQuery = "CREATE TABLE IF NOT EXISTS user (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_name VARCHAR(255)" +
                    ")";

            Statement createTableStatement = connection.createStatement();
            int tableCreated = createTableStatement.executeUpdate(createTableQuery);

            if (tableCreated >= 0) {
                System.out.println("Table `user` created successfully");
            } else {
                System.out.println("Failed to create table `user`");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeConnection(connection);
        }
    }

    public void createExpenseTable() {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Create the table if it doesn't exist
            String createTableQuery = "CREATE TABLE IF NOT EXISTS expense (" +
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
                System.out.println("Table 'expense' created successfully");
            } else {
                System.out.println("Failed to create table 'expenses'");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeConnection(connection);
        }
    }

    public void createUserExpenseTable() {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Create the `user_expense` table if it doesn't exist
            String createQuery = "CREATE TABLE IF NOT EXISTS user_expense (" +
                    "expense_id INT NOT NULL, " +
                    "creditor_id INT NOT NULL, " +
                    "debtor_id INT NOT NULL, " +
                    "amount_owed DECIMAL(10,2) NOT NULL, " +
                    "payment_status CHAR(1) DEFAULT 'n' CHECK (payment_status IN ('y', 'n')), " +
                    "PRIMARY KEY (expense_id, debtor_id), " +
                    "FOREIGN KEY (expense_id) REFERENCES expense(expense_id), " +
                    "FOREIGN KEY (debtor_id) REFERENCES user(user_id)" +
                    ")";

            Statement createTableStatement = connection.createStatement();
            int tableCreated = createTableStatement.executeUpdate(createQuery);

            if (tableCreated >= 0) {
                System.out.println("Table 'user_expense' created successfully");
            } else {
                System.out.println("Failed to create 'user_expense' table");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeConnection(connection);
        }
    }

    public void createCombinedUserExpenseView() {
        Connection connection = null;
        String createViewQuery = "CREATE OR REPLACE VIEW combined_user_expense AS " +
                "SELECT ue.expense_id, e.expense_date, e.establishment_name, e.expense_name, " +
                "ue.creditor_id, u1.user_name AS creditor_name, " +
                "ue.debtor_id, u2.user_name AS debtor_name, ue.amount_owed, ue.payment_status " +
                "FROM user_expense ue " +
                "JOIN user u1 ON ue.creditor_id = u1.user_id " +
                "JOIN user u2 ON ue.debtor_id = u2.user_id " +
                "JOIN expense e ON ue.expense_id = e.expense_id";

        try {
            connection = DatabaseConnectionManager.establishConnection();
            Statement statement = connection.createStatement();
            statement.execute(createViewQuery);
            System.out.println("View `combined_user_expense` created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeConnection(connection);
        }
    }
}
