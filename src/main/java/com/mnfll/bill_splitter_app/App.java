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

	public static void main(String[] args) throws ParseException, Exception {
		// Create a Scanner object to read user input
		Scanner scanner = new Scanner(System.in);
		ExpenseDAO expenseDAO  = new ExpenseDAO();
		
		// Create a list to store expenses
		List<Expense> expenses = new ArrayList<>();

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
			boolean isValidPortionName = false;
			
			while (!isValidDate) {
				System.out.print("Enter the date [dd/MM/yyyy] ");
				dateString = scanner.nextLine();
				
				if(InputValidator.isValidDate(dateString)) {
					date = new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
					isValidDate = true;
				}
				else {
				System.out.print("Invalid date format. Please enter the date in dd/mm/yyyy format.");
				}
			}
			
			while(!isValidEstablishmentName) {
				System.out.print("Enter the establishment name ");
				establishmentName = scanner.nextLine();
				
				if (InputValidator.isValidEstablishmentName(establishmentName)) {
					isValidEstablishmentName = true;
				}
				else {
					System.out.print("Invalid establishment name. Please enter a valid establishment name ");
				}
			}
			
			while(!isValidExpenseName) {
				System.out.print("Enter the item name ");
				expenseName = scanner.nextLine();
				
				if (InputValidator.isValidEstablishmentName(expenseName)) {
					isValidExpenseName = true;
				}
				else {
					System.out.print("Invalid expense name. Please enter a valid expense name ");
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
					System.out.print("Invalid expense cost. Please enter a valid expense cost ");
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
					System.out.print("Invalid expense cost. Please enter a valid expense cost ");
				}
			}
			
			String name;
			for (int i = 0; i < portionNumber; i++) {
				isValidPortionName = false;
				
				while(!isValidPortionName) {
					System.out.print("Enter portion name " + (i+1) + " ");
					name = scanner.nextLine();
					
					if (InputValidator.isValidPortionName(name)) {
						portionNames.add(name);
						System.out.println(name + " has been added");
						isValidPortionName = true;
					}
					else {
						System.out.print("Invalid portion name. Please enter a valid portion name ");
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
		
		scanner.close();
		
		for (Expense expense : expenses) {
			expense.displayExpense();
			expenseDAO.saveExpenseDataToDatabase(expense);
		}		
	}
}