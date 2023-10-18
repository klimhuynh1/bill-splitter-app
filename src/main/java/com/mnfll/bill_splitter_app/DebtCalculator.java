package com.mnfll.bill_splitter_app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates debt based on business logic
 */
public class DebtCalculator {

    public List<DebtRecord> calculateDebt() {
        Connection connection = null;
        List<DebtRecord> debtRecords = new ArrayList<>();

        try {
            connection = DatabaseConnectionManager.establishConnection();
            String selectQuery = "SELECT creditor_name, debtor_name, SUM(amount_owed) AS total_amount_owed " +
                    "FROM combinedExpensePersons " +
                    "WHERE creditor_name <> debtor_name AND payment_status = 'n' " +
                    "GROUP BY creditor_name, debtor_name";

            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String creditorName = resultSet.getString("creditor_name");
                    String debtorName = resultSet.getString("debtor_name");
                    double totalAmountOwed = resultSet.getDouble("total_amount_owed");

                    // Create a DebtRecord and add it to the list
                    DebtRecord debtRecord = new DebtRecord(creditorName, debtorName, totalAmountOwed);
                    debtRecords.add(debtRecord);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }

        return debtRecords;
    }

    public double[][] createDebtMatrix(List<DebtRecord> debtRecords, List<String> peopleNames) {
        int n = peopleNames.size();
        double[][] debtMatrix = new double[n][n];

        // Initialize the debt matrix with zeros
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Transpose the matrix
                debtMatrix[j][i] = 0.0;
            }
        }

        // Fill the debt matrix with debt data from debtRecords
        for (DebtRecord debtRecord : debtRecords) {
            String creditorName = debtRecord.getCreditor();
            String debtorName = debtRecord.getDebtor();
            double amountOwed = debtRecord.getAmountOwed();

            int creditorIndex = peopleNames.indexOf(creditorName);
            int debtorIndex = peopleNames.indexOf(debtorName);

            // Update the debt matrix with the amount owed
            debtMatrix[creditorIndex][debtorIndex] = amountOwed;
        }

        return debtMatrix;
    }

    public double[][] calculateNetDebts(double[][] debtMatrix, List<String> names) {
        int n = names.size();
        double[][] netDebts = new double[n][n];

        // Calculate and display net debts
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                netDebts[i][j] = debtMatrix[i][j] - debtMatrix[j][i];
            }
        }

        return netDebts;
    }

    public void displayDebtMatrix(double[][] debtMatrix, List<String> peopleNames) {
        int n = peopleNames.size();

        System.out.println("Debt Matrix:");

        // Print the column headers (people names)
        System.out.print("         ");  // Indent for row labels
        for (String personName : peopleNames) {
            System.out.printf("%10s", personName);
        }
        System.out.println();

        // Print the matrix rows
        for (int i = 0; i < n; i++) {
            System.out.printf("%10s", peopleNames.get(i));  // Row label (person name)
            for (int j = 0; j < n; j++) {
                System.out.printf("%10.2f", debtMatrix[j][i]);
            }
            System.out.println();
        }
    }

    public void displayNetDebts(double[][] netDebts, List<String> names) {
        int n = names.size();

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double netDebt = netDebts[i][j];
                if (netDebt > 0) {
                    System.out.println(names.get(j) + " owes " + names.get(i) + " " + netDebt);
                } else if (netDebt < 0) {
                    System.out.println(names.get(i) + " owes " + names.get(j) + " " + -netDebt);
                }
            }
        }
    }
}
