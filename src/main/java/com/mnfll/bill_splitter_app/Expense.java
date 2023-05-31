package com.mnfll.bill_splitter_app;

import java.util.Date;
import java.util.List;

public class Expense {
	private Date date;
	private String establishmentName;
	private String expenseName;
	private double expenseCost;
	private List<String> portionNames;
	private String payerName;
	
	public Expense(Date date, String establishmentName, String expenseName, double itemCost, List<String> portionNames,
			String payerName) {
		super();
		this.date = date;
		this.establishmentName = establishmentName;
		this.expenseName = expenseName;
		this.expenseCost = itemCost;
		this.portionNames = portionNames;
		this.payerName = payerName;
	}

	public Date getDate() {
		return date;
	}



	public String getEstablishmentName() {
		return establishmentName;
	}



	public String getExpenseName() {
		return expenseName;
	}



	public double getExpenseCost() {
		return expenseCost;
	}



	public List<String> getPortionNames() {
		return portionNames;
	}



	public String getPayerName() {
		return payerName;
	}

	public void displayExpense() {
		System.out.println("Date: " + getDate());
		System.out.println("Establishment: " + getEstablishmentName());
		System.out.println("Item: " + getExpenseName());
		System.out.println("Cost: " + getExpenseCost());
		System.out.println("Portion Names " + getPortionNames());
		System.out.println("Payer Name: " + getPayerName());
		System.out.println("---------------------------");
	}
	
	public double calculateCostPerPortion() {		
		return getExpenseCost() / getPortionNames().size();
	}
}
