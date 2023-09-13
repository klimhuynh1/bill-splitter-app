package com.mnfll.bill_splitter_app;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import com.mnfll.bill_splitter_app.utilities.InputValidator;

public class App {

	public static void main(String[] args) throws Exception  {
		// Create a Scanner object to read user input
		Scanner scanner = new Scanner(System.in);
		

		
		boolean running = true;
		
		System.out.println("Welcome to the bill splitter app");
		while (running) {
			System.out.println("\nPlease select an option:");
			System.out.println("1. Add an expense");
			System.out.println("2. Edit an expense");
			System.out.println("3. Exit");
			
			String userInput = scanner.nextLine();
			
			switch(userInput) {
				case "1":
					addExpense(scanner);
					break;
				case "2":
					editExpense(scanner);
					break;
				case "3":
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
	
	public static void addExpense(Scanner scanner) throws ParseException, Exception {
		// Create a list to store expenses
		List<Expense> expenses = new ArrayList<>();
		// Create a expenseDAO object to perform SQL operations
		ExpenseDAO expenseDAO  = new ExpenseDAO();
		boolean addMoreExpenses = true;
		
		while (addMoreExpenses) {
			String dateString;
			Date date = null;
			String establishmentName = null;
			String expenseName = null;
			double expenseCost = 0;
			int portionNumber = 0;
			List<String> portionNames = new ArrayList<>();
			boolean isValidDate = false;
			boolean isValidEstablishmentName = false;
			boolean isValidExpenseName = false;
			boolean isValidExpenseCost = false;
			boolean isValidPortionNumber = false;
			boolean isValidName = false;
			
			while (!isValidDate) {
				System.out.print("Enter the date [dd/MM/yyyy] ");
				dateString = scanner.nextLine();
				
				if(InputValidator.isValidDate(dateString)) {
					date = new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
					isValidDate = true;
				}
				else {
				System.out.print("Invalid date format. Please enter the date in dd/mm/yyyy format. ");
				}
			}
			
			while(!isValidEstablishmentName) {
				System.out.print("Enter the establishment name ");
				establishmentName = scanner.nextLine();
				
				if (InputValidator.isValidEstablishmentName(establishmentName)) {
					isValidEstablishmentName = true;
				}
				else {
					System.out.print("Invalid establishment name. Please enter a valid establishment name. ");
				}
			}
			
			while(!isValidExpenseName) {
				System.out.print("Enter the item name ");
				expenseName = scanner.nextLine();
				
				if (InputValidator.isValidExpenseName(expenseName)) {
					isValidExpenseName = true;
				}
				else {
					System.out.print("Invalid expense name. Please enter a valid expense name. ");
				}
			}
			
			while(!isValidExpenseCost) {
				System.out.print("Enter the item total cost ");
				String userInput = scanner.nextLine();
				
				if (InputValidator.isValidCost(userInput)) {
					expenseCost = Double.parseDouble(userInput);
					isValidExpenseCost = true;
				}
				else {
					System.out.print("Invalid expense cost. Please enter a valid expense cost. ");
				}
			}
			
			while(!isValidPortionNumber) {
				System.out.print("How many people are shared this item?");
				String userInput = scanner.nextLine();
				
				if (InputValidator.isValidInteger(userInput)) {
					portionNumber = Integer.parseInt(userInput);
					isValidPortionNumber = true;
				}
				else {
					System.out.print("Invalid expense cost. Please enter a valid expense cost. ");
				}
			}
			
			String name;
			for (int i = 0; i < portionNumber; i++) {
				isValidName = false;
				
				while(!isValidName) {
					System.out.print("Enter portion name " + (i+1) + " ");
					name = scanner.nextLine();
					
					if (InputValidator.isValidName(name)) {
						portionNames.add(name);
						System.out.println(name + " has been added");
						isValidName  = true;
					}
					else {
						System.out.print("Invalid portion name. Please enter a valid portion name. ");
					}
				}
			}
			
			System.out.println("Who paid for the item? ");
			for (int i = 0; i < portionNames.size(); i++) {
				System.out.println((i+1) + ". " + portionNames.get(i));
			}
			int payerNameIndex = Integer.parseInt(scanner.nextLine());
			String payerName = portionNames.get(payerNameIndex - 1);
			
			Expense expense = new Expense(date, establishmentName, expenseName, expenseCost, portionNames, payerName);
			expenses.add(expense);
			
			
			System.out.print("Add more expenses? [Y/n]");
			String moreItems = scanner.nextLine();
			if (!(moreItems.isEmpty() || moreItems.equalsIgnoreCase("y"))) {
			    addMoreExpenses = false;
			}			
		}
		
		for (Expense expense : expenses) {
			expense.displayExpense();
			expenseDAO.saveExpenseDataToDatabase(expense);
		}
	}
	
	// TODO: Validate the user inputs
	public static void editExpense(Scanner scanner) {
		// Create a expenseDAO object to perform SQL operations
		ExpenseDAO expenseDAO  = new ExpenseDAO();
		System.out.println("Enter the expense ID: ");
		int expenseId = Integer.parseInt(scanner.nextLine());
		System.out.println("What would you like to edit?");
		System.out.println("1. Update expense date");
		System.out.println("2. Update establishment name");
		System.out.println("3. Update expense name");
		System.out.println("4. Update expense cost");
		System.out.println("5. Add portion name");
		System.out.println("6. Remove portion name");
		System.out.println("7. Update payer name");
		System.out.println("8. Delete expense");
		int updateOption = Integer.parseInt(scanner.nextLine());
		
		try {
			expenseDAO.updateExpense(expenseId, updateOption, scanner);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}