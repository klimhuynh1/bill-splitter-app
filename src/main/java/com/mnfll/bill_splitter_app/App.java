package com.mnfll.bill_splitter_app;

import com.mnfll.bill_splitter_app.utilities.InputValidator;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class App {

    public static void main(String[] args) throws ParseException {
        displayMainMenu();
    }

    public static void displayMainMenu() throws ParseException {
        // Create a Scanner object to read user input
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("Welcome to the bill splitter app.");

        while (running) {
            System.out.println();
            System.out.println("Please select an option:");
            System.out.println("1. Add an expense");
            System.out.println("2. Edit an expense");
            System.out.println("3. Display expenses");
            System.out.println("4. Display combined expenses");
            System.out.println("5. Display net debt");
            System.out.println("6. Clear data");
            System.out.println("7. Exit");
            System.out.println();

            String userInput = scanner.nextLine();

            switch (userInput) {
                case "1" -> addExpense(scanner);
                case "2" -> displayEditExpenseMenu(scanner);
                case "3" -> displayExpenseTransactions();
                case "4" -> displayCombinedExpenseTransactions();
                case "5" -> displayNetDebts();
                case "6" -> {
                    System.out.print("Are you sure you want to drop the table? This action cannot be undone. [y/N]: ");
                    String confirmDrop = scanner.nextLine().trim().toLowerCase();
                    if (!confirmDrop.isBlank() || confirmDrop.equals("n") || confirmDrop.equals("no")) {
                        dropAllTables();
                        dropAllViews();
                        createAllTables();
                    } else {
                        System.out.println("Operation cancelled. The data was not cleared.");
                        System.out.println();
                    }
                }
                case "7" -> running = false;
                default -> System.out.println("Invalid input. Please try again.");
            }
        }

        System.out.println("Thank you for using the bill splitter app.");
        scanner.close();
    }

    public static void addExpense(Scanner scanner) throws ParseException {
        // Create a list to store expense
        List<Expense> expenseList = new ArrayList<>();

        boolean addMoreExpense = true;

        while (addMoreExpense) {
            String dateString;
            Date date = null;
            String establishmentName = null;
            String expenseName = null;
            double expenseCostDouble = 0;
            int numberOfDebtorsInteger = 0;
            List<String> debtorNames = new ArrayList<>();
            boolean isValidDate = false;
            boolean isValidEstablishmentName = false;
            boolean isValidExpenseName = false;
            boolean isValidExpenseCost = false;
            boolean isValidDebtorNumber = false;
            boolean isValidName;

            while (!isValidDate) {
                System.out.print("Enter the date [dd/mm/yyyy] ");
                dateString = scanner.nextLine().trim();

                if (dateString.isBlank()) {
                    return;
                }

                if (InputValidator.isValidDate(dateString)) {
                    date = new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
                    isValidDate = true;
                } else {
                    System.out.print("Invalid date format. Please enter the date in dd/mm/yyyy format. ");
                }
            }

            while (!isValidEstablishmentName) {
                System.out.print("Enter the establishment name ");
                establishmentName = scanner.nextLine().trim();

                if (InputValidator.isValidEstablishmentName(establishmentName)) {
                    isValidEstablishmentName = true;
                } else {
                    System.out.print("Invalid establishment name. Please enter a valid establishment name. ");
                }
            }

            while (!isValidExpenseName) {
                System.out.print("Enter the item name ");
                expenseName = scanner.nextLine().trim();

                if (InputValidator.isValidExpenseName(expenseName)) {
                    isValidExpenseName = true;
                } else {
                    System.out.print("Invalid expense name. Please enter a valid expense name. ");
                }
            }

            while (!isValidExpenseCost) {
                System.out.print("Enter the item total cost ");
                String expenseCostString = scanner.nextLine().trim();

                if (InputValidator.isValidCost(expenseCostString)) {
                    expenseCostDouble = Double.parseDouble(expenseCostString);
                    isValidExpenseCost = true;
                } else {
                    System.out.println("Invalid expense cost. Please enter a valid expense cost. ");
                }
            }

            while (!isValidDebtorNumber) {
                System.out.print("How many user are shared this item? ");
                String numberOfDebtorsString = scanner.nextLine().trim();

                if (InputValidator.isValidInteger(numberOfDebtorsString)) {
                    numberOfDebtorsInteger = Integer.parseInt(numberOfDebtorsString);
                    isValidDebtorNumber = true;
                } else {
                    System.out.println("Invalid number of user. Please enter a valid number of user.");
                }
            }

            String name;
            for (int i = 0; i < numberOfDebtorsInteger; i++) {
                isValidName = false;
                while (!isValidName) {
                    System.out.print("Enter name " + (i + 1) + " ");
                    name = scanner.nextLine();

                    if (InputValidator.isValidName(name)) {
                        debtorNames.add(name);
                        System.out.println(name + " has been added");
                        isValidName = true;
                    } else {
                        System.out.println("Invalid name. Please enter a valid name.");
                    }
                }
            }

            System.out.println("Who paid for the item? ");
            for (int i = 0; i < debtorNames.size(); i++) {
                System.out.println((i + 1) + ". " + debtorNames.get(i));
            }
            int creditorNameIndex = Integer.parseInt(scanner.nextLine());
            String creditorName = debtorNames.get(creditorNameIndex - 1);

            Expense expense = new Expense(date, establishmentName, expenseName, expenseCostDouble, debtorNames, creditorName);
            expenseList.add(expense);


            System.out.print("Add more expense? [Y/n] ");
            String moreItems = scanner.nextLine().trim().toLowerCase();
            if (!(moreItems.isEmpty() || moreItems.equals("y") || moreItems.equals("yes"))) {
                addMoreExpense = false;
            }
        }

        for (Expense expenseItem : expenseList) {
            expenseItem.displayExpense();
            saveExpenseDataToDatabase(expenseItem);
        }
    }

    public static void createAllTables() {
        // Create a TableCreationManager to create all tables
        TableCreationManager tableCreationManager = new TableCreationManager();

        tableCreationManager.createUserTable();
        tableCreationManager.createExpenseTable();
        tableCreationManager.createUserExpenseTable();
        tableCreationManager.createCombinedUserExpenseView();
    }

    public static void saveExpenseDataToDatabase(Expense expense) {

        // Create a JdbcUserDAO object to perform SQL operations to the User table
        JdbcUserDAO jdbcUserDAO = new JdbcUserDAO();

        // Create a JdbcExpensePersonsDAO object to perform SQL operations to the ExpensePersons table
        JdbcUserExpenseDAO jdbcUserExpenseDAO = new JdbcUserExpenseDAO();

        // Create a JdbcExpenseDAO object to perform SQL operations to the Expense table
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();

        createAllTables();

        List<Integer> personIds = jdbcUserDAO.insertUserData(expense);
        int expenseId = jdbcExpenseDAO.insertExpenseData(expense);
        jdbcUserExpenseDAO.insertUserExpenseData(expense, personIds, expenseId);
    }
    
    public static void displayEditExpenseMenu(Scanner scanner) {

        System.out.println("Displaying all transactions...");
        System.out.println();
        displayCombinedExpenseTransactions();

        int expenseId;

        while (true) {
            System.out.println("Enter the expense ID: ");
            String expenseIdString = scanner.nextLine().trim();

            if (expenseIdString.isBlank()) {
                System.out.println("Expense ID cannot be empty. Please try again.");
                continue;
            }
            try {
                expenseId = Integer.parseInt(expenseIdString);
                if (expenseIdExists(expenseId)) {
                    break;
                }
                else {
                    System.out.println("Invalid input. Please enter a valid expense ID");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number");
            }
        }

        System.out.println("Displaying all transaction with expense ID: " + expenseId + " ...");
        System.out.println();
        displayFilteredCombinedExpenseTransactions(expenseId);
        System.out.println();

        System.out.println("What would you like to edit?");
        System.out.println("1. Update expense date");
        System.out.println("2. Update establishment name");
        System.out.println("3. Update expense item name");
        System.out.println("4. Update expense cost");
        System.out.println("5. Add debtor");
        System.out.println("6. Remove debtor");
        System.out.println("7. Update creditor name");
        System.out.println("8. Update payment status");
        System.out.println("9. Delete expense");
        System.out.println("0. Cancel");

        try {
            updateExpense(expenseId, scanner);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //	TODO: Requires testing
    public static void updateExpense(int expenseId, Scanner scanner) throws ParseException {
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();
        JdbcUserExpenseDAO jdbcUserExpenseDAO = new JdbcUserExpenseDAO();

        while (true) {
            int updateOption;

            try {
                updateOption = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                continue;
            }

            switch (updateOption) {
                case 0:
                    break;
                case 1:
                    jdbcExpenseDAO.updateExpenseDate(expenseId, scanner);
                    break;
                case 2:
                    jdbcExpenseDAO.updateExpenseEstablishmentName(expenseId, scanner);
                    break;
                case 3:
                    jdbcExpenseDAO.updateExpenseName(expenseId, scanner);
                    break;
                case 4:
                    jdbcExpenseDAO.updateExpenseCost(expenseId, scanner);
                    break;
                case 5:
                    jdbcUserExpenseDAO.addDebtorName(expenseId, scanner);
                    break;
                case 6:
                    jdbcUserExpenseDAO.removeDebtorName(expenseId, scanner);
                    break;
                case 7:
                    jdbcExpenseDAO.updateCreditorName(expenseId, scanner);
                    break;
                case 8:
                    jdbcUserExpenseDAO.updatePaymentStatus(expenseId, scanner);
                    break;
                case 9:
                    jdbcExpenseDAO.deleteExpense(expenseId);
                    break; // breaks the switch case
                default:
                    System.out.println("Invalid update option. Please provide a number between 0 and 9.");
                    continue;
            }
            break; // breaks the while loop
        }
    }

    public static void displayExpenseTransactions() {
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();
        jdbcExpenseDAO.displayExpenseTransactions();
    }

    public static void displayCombinedExpenseTransactions() {
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();
        jdbcExpenseDAO.displayCombinedExpenseTransactions();
    }

    public static void displayFilteredCombinedExpenseTransactions(int expenseId) {
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();
        jdbcExpenseDAO.displayFilteredCombinedExpenseTransactions(expenseId);
    }

    public static boolean expenseIdExists(int expenseId) {
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();
        return jdbcExpenseDAO.expenseIdExists(expenseId);
    }

    public static void displayNetDebts() {
        try {
            JdbcUserDAO jdbcUserDAO = new JdbcUserDAO();
            DebtCalculator debtCalculator = new DebtCalculator();
            List<DebtRecord> debtRecords = debtCalculator.calculateDebt();
            List<String> userNames = jdbcUserDAO.getAllUserNames();

            if (debtRecords.isEmpty()) {
                System.out.println("There are no debts.");
                return;
            }

            if (userNames.isEmpty()) {
                System.out.println("There are no user.");
                return;
            }

            double[][] debtMatrix = debtCalculator.createDebtMatrix(debtRecords, userNames);
            debtCalculator.displayDebtMatrix(debtMatrix, userNames);

            double[][] netDebts = debtCalculator.calculateNetDebts(debtMatrix, userNames);
            System.out.println();
            debtCalculator.displayNetDebts(netDebts, userNames);
        } catch (Exception e) {
            System.err.println("An error occurred while displaying net debts: " + e.getMessage());
        }
    }

    public static void dropAllTables() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            stmt = conn.createStatement();

            String[] tableNames = {"user_expense", "expense", "user"};

            for (String tableName : tableNames) {
                rs = metaData.getTables(null, null, tableName, null);
                if (rs.next()) {
                    String dropTableSQL = "DROP TABLE " + tableName;
                    stmt.executeUpdate(dropTableSQL);
                    System.out.println("Table " + tableName + " dropped successfully.");
                } else {
                    System.out.println("Table " + tableName + " does not exist.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error occurred while dropping tables: " + e.getMessage());
        } finally {
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closeConnection(conn);
        }
    }

    public static void dropAllViews() {
        Connection conn = null;
        ResultSet rs = null;
        Statement stmt = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            stmt = conn.createStatement();

            String[] viewNames = {"combined_user_expense"};

            for (String viewName : viewNames) {
                rs = metaData.getTables(null, null, viewName, null);
                if (rs.next()) {
                    String dropTableSQL = "DROP VIEW " + viewName;
                    stmt.executeUpdate(dropTableSQL);
                    System.out.println("View " + viewName + " dropped successfully.");
                } else {
                    System.out.println("View " + viewName + " does not exist.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error occurred while dropping tables: " + e.getMessage());
        } finally {
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closeConnection(conn);
        }
    }
}