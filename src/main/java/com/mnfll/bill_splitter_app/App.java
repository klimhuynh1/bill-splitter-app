package com.mnfll.bill_splitter_app;

import com.mnfll.bill_splitter_app.utilities.InputValidator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class App {

    public static void main(String[] args) throws Exception {
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
            System.out.println("5. Exit");
            System.out.println();

            String userInput = scanner.nextLine();

            switch (userInput) {
                case "1":
                    addExpense(scanner);
                    break;
                case "2":
                    editExpense(scanner);
                    break;
                case "3":
//					displayTransactions();
                    //break;
                case "4":
                    displayNetDebts();
                    break;
                case "5":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid input. Please try again");
                    break;
            }
        }

        System.out.println("Thank you for using the bill splitter app");
        scanner.close();
    }

    public static void addExpense(Scanner scanner) throws ParseException {
        // Create a list to store expenses
        List<Expense> expenses = new ArrayList<>();
        JdbcPersonDAO jdbcPersonDAO = new JdbcPersonDAO();
        // Create a expenseDAO object to perform SQL operations
        ExpenseDAO expenseDAO = new ExpenseDAO(jdbcPersonDAO);
        TableCreationManager tableCreationManager = new TableCreationManager();
        DataInsertionManager dataInsertionManager = new DataInsertionManager(jdbcPersonDAO);

        boolean addMoreExpenses = true;

        while (addMoreExpenses) {
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
                System.out.print("How many people are shared this item?");
                String userInput = scanner.nextLine();

                if (InputValidator.isValidInteger(userInput)) {
                    numberOfDebtors = Integer.parseInt(userInput);
                    isValidDebtorNumber = true;
                } else {
                    System.out.println("Invalid number of people. Please enter a valid number of people. ");
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
                        System.out.println("Invalid name. Please enter a valid name. ");
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
            expenses.add(expense);


            System.out.print("Add more expenses? [Y/n]");
            String moreItems = scanner.nextLine();
            if (!(moreItems.isEmpty() || moreItems.equalsIgnoreCase("y"))) {
                addMoreExpenses = false;
            }
        }

        for (Expense expense : expenses) {
            expense.displayExpense();
            expenseDAO.saveExpenseDataToDatabase(expense, tableCreationManager, dataInsertionManager);
        }
    }

    // TODO: Validate the user inputs
    public static void editExpense(Scanner scanner) {
        JdbcPersonDAO jdbcPersonDAO = new JdbcPersonDAO();
        // Create a expenseDAO object to perform SQL operations
        ExpenseDAO expenseDAO = new ExpenseDAO(jdbcPersonDAO);
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
            expenseDAO.updateExpense(expenseId, updateOption, scanner);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

//    public static void displayAllTransactions() {
//        ExpenseDAO expenseDAO = new ExpenseDAO();
//        expenseDAO.displayAllTransactions();
//    }

    public static void displayNetDebts() {
        try {
        JdbcPersonDAO jdbcPersonDAO = new JdbcPersonDAO();
        ExpenseDAO expenseDAO = new ExpenseDAO(jdbcPersonDAO);
        DebtCalculator debtCalculator = new DebtCalculator();
        List<DebtRecord> debtRecords = debtCalculator.calculateDebt();
        List<String> peopleNames = expenseDAO.getAllPeopleNames();

        if (debtRecords.isEmpty()) {
            System.out.println("There are no debts.");
            return;
        }

        if (peopleNames.isEmpty()) {
            System.out.println("There are no people.");
            return;
        }

        double[][] debtMatrix = debtCalculator.createDebtMatrix(debtRecords, peopleNames);
        debtCalculator.displayDebtMatrix(debtMatrix, peopleNames);

        double[][] netDebts = debtCalculator.calculateNetDebts(debtMatrix, peopleNames);
        System.out.println();
        debtCalculator.displayNetDebts(netDebts, peopleNames);
    } catch (Exception e) {
            System.err.println("An error occurred while displaying net debts: " + e.getMessage());
        }
}
}