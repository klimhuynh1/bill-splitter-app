package com.mnfll.bill_splitter_app;

import java.util.Date;
import java.util.List;

public class Expense {
	private Date date;
	private String establishmentName;
	private String itemName;
	private double itemCost;
	private List<String> debtorNames;
	private String creditorName;
	
	public Expense(Date date, String establishmentName, String itemName, double itemCost, List<String> debtorNames,
			String creditorName) {
		super();
		this.date = date;
		this.establishmentName = establishmentName;
		this.itemName = itemName;
		this.itemCost = itemCost;
		this.debtorNames = debtorNames;
		this.creditorName = creditorName;
	}

	public Date getDate() {
		return date;
	}



	public String getEstablishmentName() {
		return establishmentName;
	}



	public String getItemName() {
		return itemName;
	}



	public double getItemCost() {
		return itemCost;
	}



	public List<String> getDebtorNames() {
		return debtorNames;
	}



	public String getCreditorName() {
		return creditorName;
	}

	public void displayExpense() {
		System.out.println("Date: " + getDate());
		System.out.println("Establishment: " + getEstablishmentName());
		System.out.println("Item: " + getItemName());
		System.out.println("Cost: " + getItemCost());
		System.out.println("Portion Names " + getDebtorNames());
		System.out.println("Payer Name: " + getCreditorName());
		System.out.println("---------------------------");
	}
}
