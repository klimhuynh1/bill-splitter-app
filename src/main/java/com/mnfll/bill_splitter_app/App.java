package com.mnfll.bill_splitter_app;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class App {

	public static void main(String[] args) throws ParseException, Exception {
		// Create a Scanner object to read user input
		Scanner scanner = new Scanner(System.in);
		ExpenseDAO expenseDAO  = new ExpenseDAO();
		
		// Create a list to store expenses
		List<Expense> expenses = new ArrayList<>();

		boolean addMoreExpenses = true;
		
		while (addMoreExpenses) {
			// Create a list to store portion names
			List<String> portionNames = new ArrayList<>();
			
			System.out.print("Enter the date [dd/mm/yyyy] ");
			String dateString = scanner.nextLine();
			Date date = new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
			
			System.out.print("Enter the name of the establishment ");
			String establishmentName = scanner.nextLine();
			
			System.out.print("Enter the item name ");
			String itemName = scanner.nextLine();
			
			System.out.print("Enter the item total cost ");
			double itemCost = Double.parseDouble(scanner.nextLine());
			
			System.out.print("How many people are shared this item?");
			int portionNumber = Integer.parseInt(scanner.nextLine());
			
			String name;
			for (int i = 0; i < portionNumber; i++) {
				System.out.print("Enter portion name " + (i+1) + " ");
				name = scanner.nextLine();
				portionNames.add(name);
				System.out.println(name + " has been added");
			}
			
			System.out.println("Who paid for the item? ");
			for (int i = 0; i < portionNames.size(); i++) {
				System.out.println((i+1) + ". " + portionNames.get(i));
			}
			int payerNameIndex = Integer.parseInt(scanner.nextLine());
			String payerName = portionNames.get(payerNameIndex - 1);
			
			Expense expense = new Expense(date, establishmentName, itemName, itemCost, portionNames, payerName);
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