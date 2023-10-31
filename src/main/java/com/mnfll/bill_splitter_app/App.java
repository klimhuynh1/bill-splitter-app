package com.mnfll.bill_splitter_app;

import com.mnfll.bill_splitter_app.utilities.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

        System.out.println("Welcome to the bill splitter app");
        while (running) {
            System.out.println();
            System.out.println("Please select an option:");
            System.out.println("1. Add an expense");
            System.out.println("2. Edit an expense");
            System.out.println("3. Display transactions");
            System.out.println("4. Display net debt");
            System.out.println("5. Clear data");
            System.out.println("6. Exit");
            System.out.println();

            String userInput = scanner.nextLine();

            switch (userInput) {
                case "1" -> addExpense(scanner);
                case "2" -> displayEditExpenseMenu(scanner);
                case "3" -> displayAllExpenseTransactions();
                case "4" -> displayNetDebts();
                case "5" -> {
                    dropAllTables();
                    dropAllViews();
                }
                case "6" -> running = false;
                default -> System.out.println("Invalid input. Please try again");
            }
        }

        System.out.println("Thank you for using the bill splitter app");
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
            double expenseCost = 0;
            int numberOfDebtors = 0;
            List<String> debtorNames = new ArrayList<>();
            boolean isValidDate = false;
            boolean isValidEstablishmentName = false;
            boolean isValidExpenseName = false;
            boolean isValidExpenseCost = false;
            boolean isValidDebtorNumber = false;
            boolean isValidName;

            while (!isValidDate) {
                System.out.print("Enter the date [dd/MM/yyyy] ");
                dateString = scanner.nextLine();

                if (InputValidator.isValidDate(dateString)) {
                    date = new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
                    isValidDate = true;
                } else {
                    System.out.print("Invalid date format. Please enter the date in dd/mm/yyyy format. ");
                }
            }

            while (!isValidEstablishmentName) {
                System.out.print("Enter the establishment name ");
                establishmentName = scanner.nextLine();

                if (InputValidator.isValidEstablishmentName(establishmentName)) {
                    isValidEstablishmentName = true;
                } else {
                    System.out.print("Invalid establishment name. Please enter a valid establishment name. ");
                }
            }

            while (!isValidExpenseName) {
                System.out.print("Enter the item name ");
                expenseName = scanner.nextLine();

                if (InputValidator.isValidExpenseName(expenseName)) {
                    isValidExpenseName = true;
                } else {
                    System.out.print("Invalid expense name. Please enter a valid expense name. ");
                }
            }

            while (!isValidExpenseCost) {
                System.out.print("Enter the item total cost ");
                String userInput = scanner.nextLine();

                if (InputValidator.isValidCost(userInput)) {
                    expenseCost = Double.parseDouble(userInput);
                    isValidExpenseCost = true;
                } else {
                    System.out.println("Invalid expense cost. Please enter a valid expense cost. ");
                }
            }

            while (!isValidDebtorNumber) {
                System.out.print("How many user are shared this item? ");
                String userInput = scanner.nextLine();

                if (InputValidator.isValidInteger(userInput)) {
                    numberOfDebtors = Integer.parseInt(userInput);
                    isValidDebtorNumber = true;
                } else {
                    System.out.println("Invalid number of user. Please enter a valid number of user.");
                }
            }

            String name;
            for (int i = 0; i < numberOfDebtors; i++) {
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

            Expense expense = new Expense(date, establishmentName, expenseName, expenseCost, debtorNames, creditorName);
            expenseList.add(expense);


            System.out.print("Add more expense? [Y/n] ");
            String moreItems = scanner.nextLine();
            if (!(moreItems.isEmpty() || moreItems.equalsIgnoreCase("y"))) {
                addMoreExpense = false;
            }
        }

        for (Expense expenseItem : expenseList) {
            expenseItem.displayExpense();
            saveExpenseDataToDatabase(expenseItem);
        }
    }

    public static void saveExpenseDataToDatabase(Expense expense) {
        // Create a TableCreationManager to create all tables
        TableCreationManager tableCreationManager = new TableCreationManager();

        // Create a JdbcUserDAO object to perform SQL operations to the User table
        JdbcUserDAO jdbcUserDAO = new JdbcUserDAO();

        // Create a JdbcExpensePersonsDAO object to perform SQL operations to the ExpensePersons table
        JdbcUserExpenseDAO jdbcUserExpenseDAO = new JdbcUserExpenseDAO();

        // Create a JdbcExpenseDAO object to perform SQL operations to the Expense table
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();

        tableCreationManager.createUserTable();
        tableCreationManager.createExpenseTable();
        tableCreationManager.createUserExpenseTable();
        tableCreationManager.createCombinedUserExpenseView();

        List<Integer> personIds = jdbcUserDAO.insertUserData(expense);
        int expenseId = jdbcExpenseDAO.insertExpenseData(expense);
        jdbcUserExpenseDAO.insertUserExpenseData(expense, personIds, expenseId);
    }

    // TODO: Validate the user inputs
    public static void displayEditExpenseMenu(Scanner scanner) {
        System.out.println("Enter the expense ID: ");

        int expenseId = Integer.parseInt(scanner.nextLine());

        System.out.println("What would you like to edit?");
        System.out.println("0. Cancel");
        System.out.println("1. Update expense date");
        System.out.println("2. Update establishment name");
        System.out.println("3. Update expense name");
        System.out.println("4. Update expense cost");
        System.out.println("5. Add debtor name");
        System.out.println("6. Remove debtor name");
        System.out.println("7. Update creditor name");
        System.out.println("8. Update payment status");
        System.out.println("9. Delete expense");

        int updateOption = Integer.parseInt(scanner.nextLine());

        try {
            updateExpense(expenseId, updateOption, scanner);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //	TODO: Requires testing
    public static void updateExpense(int expenseId, int updateOption, Scanner scanner) throws ParseException {
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();
        JdbcUserExpenseDAO jdbcUserExpenseDAO = new JdbcUserExpenseDAO();

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
                break;
            default:
                System.out.println("Invalid update option");
        }
    }

    public static void displayAllExpenseTransactions() {
        JdbcExpenseDAO jdbcExpenseDAO = new JdbcExpenseDAO();
        jdbcExpenseDAO.displayAllExpenseTransactions();
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

        try {
            conn = DatabaseConnectionManager.establishConnection();
            stmt = conn.createStatement();

            // Drop each table separately
            String dropTableUserExpense = "DROP TABLE user_expense";
            String dropTableExpense = "DROP TABLE expense";
            String dropTableUser = "DROP TABLE user";

            // Execute the drop statements
            stmt.executeUpdate(dropTableUserExpense);
            stmt.executeUpdate(dropTableExpense);
            stmt.executeUpdate(dropTableUser);

            System.out.println("All tables dropped successfully. ");
        } catch (
                SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeConnection(conn);
        }
    }

    public static void dropAllViews() {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = DatabaseConnectionManager.establishConnection();
            stmt = conn.createStatement();

            // Drop each view separately
            String dropViewCombinedUserExpense = "DROP VIEW combined_user_expense";

            // Execute the drop statements
            stmt.executeUpdate(dropViewCombinedUserExpense);

            System.out.println("All views dropped successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeConnection(conn);
        }
    }
}