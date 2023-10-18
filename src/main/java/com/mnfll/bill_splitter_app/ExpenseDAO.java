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

    public List<Integer> insertPeopleData(Expense expense) {
        List<Integer> generatedKeys = new ArrayList<>();
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

            // Prepare the select statement to check if the name already exists
            String selectQuery = "SELECT person_id FROM people WHERE person_name = ?";

            // Prepare the insert statement to add new records to the `people` table
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

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }

        return generatedKeys;
    }

    public int insertExpenseData(Expense expense) {
        Connection connection = null;
        int generatedExpenseId = -1;
        int creditorId = getPersonIdByName(expense.getCreditorName());

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
                insertTableStatement.setDouble(4, expense.getItemCost()/expense.getDebtorNames().size());

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

    public void saveExpenseDataToDatabase(Expense expense) {
        List<Integer> personIds = insertPeopleData(expense);
        int expenseId = insertExpenseData(expense);
        insertExpensePersonsData(expense, personIds, expenseId);
    }

    public void createCombinedExpensePersons() {
        Connection connection = null;
        String createViewQuery = "CREATE VIEW combinedExpensePersons AS " +
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

    public int getPersonIdByName(String personName) {
        Connection connection = null;
        int personId = -1;
        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Get the creditor_id for the
            String selectQuery = "SELECT person_id FROM people WHERE person_name = ? LIMIT 1";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.setString(1, personName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                personId = resultSet.getInt("person_id");
                System.out.println("Person ID: " + personId);
            } else {
                System.out.println("Person Id not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
        return personId;
    }

    public int getCreditorIdByExpenseId(int expenseId) {
        Connection connection = null;
        int creditorId = -1;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Get the creditor_id for the
            String selectQuery = "SELECT creditor_id FROM expenses WHERE expense_id = ? LIMIT 1";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.setInt(1, expenseId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                creditorId = resultSet.getInt("creditor_id");
                System.out.println("creditor ID: " + creditorId);
            } else {
                System.out.println("creditor Id not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }

        return creditorId;
    }

    public List<DebtRecord> calculateDebt() {
        Connection connection = null;
        List<DebtRecord> debtRecords = new ArrayList<>();

        try {
            connection = DatabaseConnectionManager.establishConnection();
            String selectQuery = "SELECT creditor_name, debtor_name, SUM(amount_owed) AS total_amount_owed " +
                    "FROM combinedExpensePersons " +
                    "WHERE creditor_name <> debtor_name AND payment_status = 'n' " +
                    "GROUP BY creditor_name, debtor_name";

            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String creditorName = resultSet.getString("creditor_name");
                    String debtorName = resultSet.getString("debtor_name");
                    double totalAmountOwed = resultSet.getDouble("total_amount_owed");

                    // Create a DebtRecord and add it to the list
                    DebtRecord debtRecord = new DebtRecord(creditorName, debtorName, totalAmountOwed);
                    debtRecords.add(debtRecord);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }

        return debtRecords;
    }

    public List<String> getAllPeopleNames() {
        Connection connection = null;
        List<String> peopleNames = new ArrayList<>();

        try {
            connection = DatabaseConnectionManager.establishConnection();
            String query = "SELECT person_name FROM people";

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {

                while (resultSet.next()) {
                    String personName = resultSet.getString("person_name");
                    peopleNames.add(personName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }

        return peopleNames;
    }

    public double[][] createDebtMatrix(List<DebtRecord> debtRecords, List<String> peopleNames) {
        int n = peopleNames.size();
        double[][] debtMatrix = new double[n][n];

        // Initialize the debt matrix with zeros
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Transpose the matrix
                debtMatrix[j][i] = 0.0;
            }
        }

        // Fill the debt matrix with debt data from debtRecords
        for (DebtRecord debtRecord : debtRecords) {
            String creditorName = debtRecord.getCreditor();
            String debtorName = debtRecord.getDebtor();
            double amountOwed = debtRecord.getAmountOwed();

            int creditorIndex = peopleNames.indexOf(creditorName);
            int debtorIndex = peopleNames.indexOf(debtorName);

            // Update the debt matrix with the amount owed
            debtMatrix[creditorIndex][debtorIndex] = amountOwed;
        }

        return debtMatrix;
    }

    public void displayDebtMatrix(double[][] debtMatrix, List<String> peopleNames) {
        int n = peopleNames.size();

        System.out.println("Debt Matrix:");

        // Print the column headers (people names)
        System.out.print("         ");  // Indent for row labels
        for (String personName : peopleNames) {
            System.out.printf("%10s", personName);
        }
        System.out.println();

        // Print the matrix rows
        for (int i = 0; i < n; i++) {
            System.out.printf("%10s", peopleNames.get(i));  // Row label (person name)
            for (int j = 0; j < n; j++) {
                System.out.printf("%10.2f", debtMatrix[j][i]);
            }
            System.out.println();
        }
    }

    public void calculateNetDebts(double[][] debtMatrix, List<String> names) {
        int n = names.size();

        // Calculate and display net debts
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double netDebt = debtMatrix[i][j] - debtMatrix[j][i];
                if (netDebt > 0) {
                    System.out.println(names.get(j) + " owes " + names.get(i) + " " + netDebt);
                } else if (netDebt < 0) {
                    System.out.println(names.get(i) + " owes " + names.get(j) + " " + -netDebt);
                }
            }
        }
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
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
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
            PreparedStatement preparedStatement = connection.prepareStatement(updateStatement);
            preparedStatement.setString(1, newCreditorName);
            preparedStatement.setInt(2, expenseId);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Update successful.");
            } else {
                System.out.println("No rows were updated.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    public void removeDebtorName(int expenseId, Scanner scanner) {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            System.out.println("Enter the debtor name you would like to remove");

            String debtorName = scanner.nextLine();
            if (InputValidator.isValidName(debtorName)) {
                int personId = getPersonIdByName(debtorName);
                int creditorId = getCreditorIdByExpenseId(expenseId);

                // FIXME: temporary fix, will implement either singleton pattern and/or command pattern
                if (personId == creditorId) {
                    throw new IllegalArgumentException("You cannot remove debtor name as they are the creditor");
                } else {
                    // Otherwise, remove record in `expensePersons` table based on person_id and expense_id
                    String deleteQuery = "DELETE FROM expensePersons WHERE expense_id = ? AND person_id = ?";
                    PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                    deleteStatement.setInt(1, expenseId);
                    deleteStatement.setInt(2, personId);
                    deleteStatement.executeUpdate();

                    // Update split_count in `expenses` table based on expense_id
                    int splitCount = updateSplitCount(connection, expenseId, false);
                    // Re-calculate cost per debtor
                    double newAmountOwed = calculateNewAmountOwed(connection, expenseId, splitCount);
                    // Update amount_owed based on expense_id
                    updateAmountOwed(connection, expenseId, newAmountOwed);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    public void addDebtorName(int expenseId, Scanner scanner) {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            String newDebtorName = scanner.nextLine();
            if (InputValidator.isValidName(newDebtorName)) {
                int personId = addNewPersonIfNotExists(connection, newDebtorName);
                int splitCount = updateSplitCount(connection, expenseId, true);
                double newAmountOwed = calculateNewAmountOwed(connection, expenseId, splitCount);
                updateAmountOwed(connection, expenseId, newAmountOwed);
                addExpensePersonRecord(connection, expenseId, personId, newAmountOwed);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    private int addNewPersonIfNotExists(Connection connection, String debtorName) throws SQLException {
        String selectQuery = "SELECT person_id FROM people WHERE person_name = ?";
        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setString(1, debtorName);
        ResultSet selectResultSet = selectStatement.executeQuery();

        if (selectResultSet.next()) {
            // Person already exists, retrieve the person id
            return selectResultSet.getInt("person_id");
        } else {
            // Person does not exist, create a new record
            String insertQuery = "INSERT INTO people (person_name) VALUES (?)";
            PreparedStatement insertStatement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            insertStatement.setString(1, debtorName);
            insertStatement.executeUpdate();

            // Retrieve the generated person_id
            ResultSet generatedKeys = insertStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        }

        return -1; // Return a default value if personId is not found (shouldn't happen)
    }

    private int updateSplitCount(Connection connection, int expenseId, Boolean increment) throws SQLException {
        String selectQuery = "SELECT split_count FROM expenses WHERE expense_id = ?";
        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, expenseId);
        ResultSet selectResultSet = selectStatement.executeQuery();
        int splitCount;

        if (selectResultSet.next()) {
            if (increment) {
                splitCount = selectResultSet.getInt("split_count") + 1;
            } else {
                splitCount = selectResultSet.getInt("split_count") - 1;
            }


            String updateQuery = "UPDATE expenses SET split_count = ? WHERE expense_id = ?";
            PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
            updateStatement.setInt(1, splitCount);
            updateStatement.setInt(2, expenseId);

            int rowsAffected = updateStatement.executeUpdate();
            System.out.println("Update split_count -- Rows affected: " + rowsAffected);

            return splitCount;
        }

        return -1;
    }

    private double calculateNewAmountOwed(Connection connection, int expenseId, int splitCount) throws SQLException {
        String selectQuery = "SELECT total_cost FROM expenses WHERE expense_id = ?";
        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, expenseId);
        ResultSet selectResultSet = selectStatement.executeQuery();

        if (selectResultSet.next()) {
            double expenseCost = selectResultSet.getDouble("total_cost");
            return expenseCost / splitCount;
        }

        return -1;
    }

    private void updateAmountOwed(Connection connection, int expenseId, double newAmountOwed) throws SQLException {
        String updateQuery = "UPDATE expensePersons SET amount_owed = ? WHERE expense_id = ?";
        PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
        updateStatement.setDouble(1, newAmountOwed);
        updateStatement.setInt(2, expenseId);

        int rowsAffected = updateStatement.executeUpdate();
        System.out.println("Update amount owed -- Rows affected: " + rowsAffected);
    }

    private void addExpensePersonRecord(Connection connection, int expenseId, int personId, double newAmountOwed) throws SQLException {
        String insertQuery = "INSERT INTO expensePersons (expense_id, person_id, amount_owed) VALUES (?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(insertQuery);
        statement.setInt(1, expenseId);
        statement.setInt(2, personId);
        statement.setDouble(3, newAmountOwed);

        int rowsAffected = statement.executeUpdate();
        System.out.println("Add row for new debtor name -- Rows affected: " + rowsAffected);
    }

    public void updateExpenseDate(int expenseId, Scanner scanner) {
        Connection connection = null;

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
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setDate(1, sqlDate);
            statement.setInt(2, expenseId);

            // Execute the update query
            int rowsAffected = statement.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    public void updateExpenseEstablishmentName(int expenseId, Scanner scanner) {
        Connection connection = null;

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
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setString(1, establishmentName);
            statement.setInt(2, expenseId);

            // Execute the update query
            int rowsAffected = statement.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    public void updateExpenseName(int expenseId, Scanner scanner) {
        Connection connection = null;

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
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setString(1, expenseName);
            statement.setInt(2, expenseId);

            // Execute the update query
            int rowsAffected = statement.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    // Update total_cost in `expense` table based on expense_id
    public void updateExpenseCost(int expenseId, Scanner scanner) {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
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
            PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
            updateStatement.setDouble(1, expenseCost);
            updateStatement.setInt(2, expenseId);

            // Execute the update query
            int rowsAffected = updateStatement.executeUpdate();

            // Check the number of rows affected
            System.out.println("Rows affected: " + rowsAffected);

            // Get the number of people that splitting this expense cost
            String selectQuery = "SELECT split_count FROM expenses WHERE expense_id = ?";
            PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
            selectStatement.setInt(1, expenseId);

            // Execute the select query
            ResultSet resultSet = selectStatement.executeQuery();
            int splitCount = 0;
            if (resultSet.next()) {
                splitCount = resultSet.getInt("split_count");
            }

            // Re-calculate the cost per person
            double newAmountOwed = expenseCost / splitCount;

            // Update amount_owed in `expensePersons` table for each person
            String updateAmountOwedQuery = "UPDATE expensePersons SET amount_owed = ? WHERE expense_id = ?";
            PreparedStatement updateAmountOwedStatement = connection.prepareStatement(updateAmountOwedQuery);
            updateAmountOwedStatement.setDouble(1, newAmountOwed);
            updateAmountOwedStatement.setInt(2, expenseId);

            // Execute the update query
            int rowsUpdated = updateAmountOwedStatement.executeUpdate();

            // Check the number of rows updated
            System.out.println("Rows updated: " + rowsUpdated);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }


    public void deleteOrphanUsers() {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Delete from `people` table if they're not associated with any expenses
            String selectQuery = "SELECT person_id from people";
            PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
            ResultSet resultSet = selectStatement.executeQuery();

            // Iterate through the result set
            while (resultSet.next()) {
                int personId = resultSet.getInt("person_id");

                // Check if the person_id exists in the person table
                String checkQuery = "SELECT COUNT(*) FROM expensePersons WHERE person_id = ?";
                PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
                checkStatement.setInt(1, personId);
                ResultSet checkResult = checkStatement.executeQuery();
                checkResult.next();

                int count = checkResult.getInt(1);

                if (count == 0) {
                    // Delete the user from the people table
                    String deleteQuery = "DELETE FROM people WHERE person_id = ?";
                    PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                    deleteStatement.setInt(1, personId);
                    int rowsAffected = deleteStatement.executeUpdate();

                    System.out.println(rowsAffected + " rows(s) for user ID: " + personId);
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
        // Create an empty HashSet to contain valid debtor ids
//        Set<Integer> validDebtorIds = new HashSet<>();

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Create a SQL query
            String sqlQuery = "SELECT debtor_id, debtor_name, amount_owed, payment_status FROM combinedExpensePersons WHERE expense_id = ?";

            // Create a Statement object
            PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);

            // Set value for the placeholder in the WHERE clause
            preparedStatement.setInt(1, expenseId);

            // Execute the query and get the result set
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through the result set and print data
            while (resultSet.next()) {
                int debtorId = resultSet.getInt("debtor_id");
                String debtorName = resultSet.getString("debtor_name");
                double amountOwed = resultSet.getDouble("amount_owed");
                char paymentStatus = resultSet.getString("payment_status").charAt(0);

                // Add valid debtor ids to HashSet
//                validDebtorIds.add(debtorId);

                // Print id and name of debtors associated with this expense
                System.out.println("id: " + debtorId + ", name: " + debtorName + ", amount owed: " + amountOwed +
                        ", payment status: " + paymentStatus);
            }

            System.out.println("Enter the ID of debtor to modify payment status");
            String debtorIdModify = scanner.nextLine();

            System.out.println("Enter the new payment status");
            String newPaymentStatus = scanner.nextLine();


            // SQL query to update paymentStatus status
            String updateQuery = "UPDATE expensePersons SET payment_status = ? WHERE expense_id = ? AND debtor_id = ?";

            // Prepare the statement
            PreparedStatement preparedStatement1 = connection.prepareStatement(updateQuery);

            // Set new values for columns
            preparedStatement1.setString(1, newPaymentStatus);
            preparedStatement1.setInt(2, expenseId);
            preparedStatement1.setInt(3, Integer.parseInt(debtorIdModify));

            // Execute the update
            int rowsUpdated = preparedStatement1.executeUpdate();
            System.out.println("Rows updated: " + rowsUpdated);


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

    // TODO: Refactor
    public void deleteExpense(int expenseId) {
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Delete from expensePersons table
            String deleteExpensePersonsQuery = "DELETE FROM expensePersons WHERE expense_id = ?";
            PreparedStatement expensePersonsStatement = connection.prepareStatement(deleteExpensePersonsQuery);
            expensePersonsStatement.setInt(1, expenseId);

            // Execute the statement
            int expensePersonsRowsAffected = expensePersonsStatement.executeUpdate();
            if (expensePersonsRowsAffected > 0) {
                System.out.println(expensePersonsRowsAffected + " row(s) deleted successfully from 'expensePersons' table.");
            } else {
                System.out.println("No rows deleted from 'expensePersons' table.");
            }

            // Delete from expenses table
            String deleteExpensesQuery = "DELETE FROM expenses WHERE expense_id = ?";

            // Prepare the statement
            PreparedStatement expensesStatement = connection.prepareStatement(deleteExpensesQuery);
            expensesStatement.setInt(1, expenseId);

            // Execute the statement
            int expensesRowsAffected = expensesStatement.executeUpdate();
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
            DatabaseConnectionManager.closeConnection(connection);
        }
    }

//    TODO: Make sure the order the results by the expense_id
//    public void displayAllTransactions() {
//        // Display the following: date, establishment nme, expenseId, expense name, cost, debtor name, creditor
//        try (Connection connection = DatabaseConnectionManager.establishConnection()) {
//            String selectQuery = "SELECT ";
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//    }
}