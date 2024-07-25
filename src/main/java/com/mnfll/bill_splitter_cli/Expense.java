package com.mnfll.bill_splitter_cli;

import java.util.Date;
import java.util.List;

public class Expense {
    private final Date date;
    private final String establishmentName;
    private final String itemName;
    private final double itemCost;
    private final List<String> debtorNames;
    private final String creditorName;

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
        System.out.println("Debtor Names " + getDebtorNames());
        System.out.println("Creditor Name: " + getCreditorName());
        System.out.println();
    }
}
