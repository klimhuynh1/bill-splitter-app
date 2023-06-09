package com.mnfll.bill_splitter_app;

import java.util.Date;
import java.util.List;

public class Expense {
	private Date date;
	private String establishmentName;
	private String itemName;
	private double itemCost;
	private List<String> portionNames;
	private String payerName;
	
	public Expense(Date date, String establishmentName, String itemName, double itemCost, List<String> portionNames,
			String payerName) {
		super();
		this.date = date;
		this.establishmentName = establishmentName;
		this.itemName = itemName;
		this.itemCost = itemCost;
		this.portionNames = portionNames;
		this.payerName = payerName;
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



	public List<String> getPortionNames() {
		return portionNames;
	}



	public String getPayerName() {
		return payerName;
	}

	public void displayExpense() {
		System.out.println("Date: " + getDate());
		System.out.println("Establishment: " + getEstablishmentName());
		System.out.println("Item: " + getItemName());
		System.out.println("Cost: " + getItemCost());
		System.out.println("Portion Names " + getPortionNames());
		System.out.println("Payer Name: " + getPayerName());
		System.out.println("---------------------------");
	}
}
