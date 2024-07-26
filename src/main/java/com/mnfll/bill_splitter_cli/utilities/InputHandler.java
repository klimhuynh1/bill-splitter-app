package com.mnfll.bill_splitter_cli.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class InputHandler {

    public static Date promptForDate(Scanner scanner) throws ParseException {
        while (true) {
            System.out.print("Please enter the date [dd/mm/yyyy]. Enter '0' to cancel. ");
            String dateString = scanner.nextLine().trim();
            if (dateString.equals("0")) return null;

            if (InputValidator.isValidDate(dateString)) {
                return new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
            } else {
                System.out.println("Invalid date format. Please enter the date in dd/mm/yyyy format.");
            }
        }
    }

    public static String promptForEstablishmentName(Scanner scanner) {
        while (true) {
            System.out.print("Please enter the establishment name. Enter '0' to cancel. ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) return null;

            if (InputValidator.isValidEstablishmentName(input)) {
                return input;
            } else {
                System.out.println("Invalid input. Please try again.");
            }
        }
    }

    public static String promptForExpenseName(Scanner scanner) {
        while (true) {
            System.out.print("Please enter the item name. Enter '0' to cancel. ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) return null;

            if (InputValidator.isValidEstablishmentName(input)) {
                return input;
            } else {
                System.out.println("Invalid input. Please try again.");
            }
        }
    }

    public static double promptForDouble(Scanner scanner) {
        while (true) {
            System.out.print("Please enter the item total cost. Enter '0' to cancel. ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) return Double.NaN;

            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid cost. Please enter a valid number.");
            }
        }
    }

    public static int promptForInteger(Scanner scanner) {
        while (true) {
            System.out.print("How many users are sharing this item? Enter '0' to cancel. ");
            String input = scanner.nextLine().trim();
            if (input.equals("0")) return -1;

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid integer.");
            }
        }
    }

    public static List<String> promptForNames(Scanner scanner, int numberOfDebtors) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < numberOfDebtors; i++) {
            while (true) {
                System.out.print("Enter name " + (i + 1) + ": ");
                String name = scanner.nextLine();
                if (name.equals("0")) return null;

                if (InputValidator.isValidName(name)) {
                    names.add(name);
                    System.out.println(name + " has been added.");
                    break;
                } else {
                    System.out.println("Invalid name. Please try again.");
                }
            }
        }
        return names;
    }

    public static String promptForCreditor(Scanner scanner, List<String> debtorNames) {
        while (true) {
            System.out.println("Who paid for the item? Enter '0' to cancel.");
            for (int i = 0; i < debtorNames.size(); i++) {
                System.out.println((i + 1) + ". " + debtorNames.get(i));
            }
            String input = scanner.nextLine().trim();
            if (input.equals("0")) return null;

            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < debtorNames.size()) {
                    return debtorNames.get(index);
                } else {
                    System.out.println("Invalid selection. Please enter a valid number.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid number.");
            }
        }
    }

    public static boolean promptForMoreItems(Scanner scanner) {
        while (true) {
            System.out.print("Add more expense? [Y/n] ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.isEmpty() || input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("n") || input.equals("no")) {
                return false;
            } else {
                System.out.println("Invalid choice. Please enter 'y' or 'n'.");
            }
        }
    }
}
