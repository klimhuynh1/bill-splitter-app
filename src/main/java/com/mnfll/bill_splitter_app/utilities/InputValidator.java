package com.mnfll.bill_splitter_app.utilities;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputValidator {
	private static final String DATE_FORMAT = "dd/MM/yyyy";
	// Compiles the pattern only once, improves performance
	// Names and establishment names use the same pattern
	private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9\\s]+$");
	
	public static boolean isValidDate(String input) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
			LocalDate.parse(input, formatter);
			return true;
		}
		catch (DateTimeParseException e) {
			return false;
		}
	}
	
	public static boolean isValidCost(String input) {
		return (isValidInteger(input) || isValidDouble(input));
	}
	
	public static boolean isValidInteger(String input) {
		input = sanitizeInput(input);
		try {
			Integer.parseInt(input);
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static boolean isValidDouble(String input) {
		input = sanitizeInput(input);
		try {
			Double.parseDouble(input);
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static boolean isValidEstablishmentName(String input) {
		input = sanitizeInput(input);
		Matcher matcher = NAME_PATTERN.matcher(input);
		
		return matcher.matches();
	}
	
	public static boolean isValidExpenseName(String input) {
		input = sanitizeInput(input);
		Matcher matcher = NAME_PATTERN.matcher(input);
		
		return matcher.matches();
	}
	
	public static boolean isValidName(String input) {
		input = sanitizeInput(input);
		Matcher matcher = NAME_PATTERN.matcher(input);
		
		return matcher.matches();
	}
	
	public static String sanitizeInput(String input) {
		if (input == null) {
			return "";
		}
		return input.trim();
	}
}
