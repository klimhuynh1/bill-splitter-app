package com.mnfll.bill_splitter_app;

public class DebtRecord {
	private String creditor;
	private String debtor;
	private double amountOwed;
	
	public DebtRecord(String creditor, String debtor, double amountOwed) {
		this.creditor = creditor;
		this.debtor = debtor;
		this.amountOwed = amountOwed;
	}

	public String getCreditor() {
		return creditor;
	}

	public void setCreditor(String creditor) {
		this.creditor = creditor;
	}

	public String getDebtor() {
		return debtor;
	}

	public void setDebtor(String debtor) {
		this.debtor = debtor;
	}

	public double getAmountOwed() {
		return amountOwed;
	}

	public void setAmountOwed(double amountOwed) {
		this.amountOwed = amountOwed;
	}
	
	
	
	@Override
	public String toString() {
		// Create a report for this DebtRecord
		return debtor + " owes " + creditor + " $" + amountOwed;
	}
	
}