package com.mnfll.bill_splitter_app;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ExpenseDAO {
	private static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + "\\config.properties";
    private static final String DB_URL_KEY = "db.url";
    private static final String DB_USERNAME_KEY = "db.username";
    private static final String DB_PASSWORD_KEY = "db.password";
	
	private Properties loadConfig() {
		Properties config = new Properties();
		try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH)) {
			config.load(fis);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return config;
	}
	
	public Connection establishConnection() throws SQLException {
		// Load the configuration from the properties file
		Properties config = loadConfig();
		
		// Get the database connection details from the properties file
        String dbUrl = config.getProperty(DB_URL_KEY);
        String dbUsername = config.getProperty(DB_USERNAME_KEY);
        String dbPassword = config.getProperty(DB_PASSWORD_KEY);
        
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
	}
	
	public void saveExpenseDataToDatabase(Expense expense) {
		List<Integer> personIds = insertPeopleData(expense);
		int expenseId = insertExpenseData(expense);
	    insertExpensePersonsData(expense, personIds, expenseId);
	}

	public List<Integer> insertPeopleData(Expense expense) {
	    List<Integer> generatedKeys = new ArrayList<>();

	    try (Connection connection = establishConnection()) {
	        // Create the 'people' table if it doesn't exist
	        String createTableQuery = "CREATE TABLE IF NOT EXISTS people (" +
	                "person_id INT AUTO_INCREMENT PRIMARY KEY, " +
	                "person_name VARCHAR(255)" +
	                ")";

	        Statement createTableStatement = connection.createStatement();
	        int tableCreated = createTableStatement.executeUpdate(createTableQuery);

	        if (tableCreated >= 0) {
	            System.out.println("Table 'people' created successfully");
	        } else {
	            System.out.println("Failed to create table 'people'");
	        }

	        // Prepare the insert statement
	        String insertQuery = "INSERT INTO people (person_name) VALUES (?)";

	        for (String portionName : expense.getPortionNames()) {
	            PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
	            statement.setString(1, portionName);

	            int rowsInserted = statement.executeUpdate();
	            if (rowsInserted > 0) {
	                System.out.println("Record inserted into `people` table successfully");

	                // Save the person_id to the ArrayList
	                ResultSet resultSet = statement.getGeneratedKeys();
	                if (resultSet.next()) {
	                    generatedKeys.add(resultSet.getInt(1));
	                }
	            } else {
	                System.out.println("Failed to insert into `people` table");
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return generatedKeys;
	}

	
	public int insertExpenseData(Expense expense) {
	    int generatedExpenseId = -1;

	    try (Connection connection = establishConnection()) {
	        // Create the table if it doesn't exist
	        String createTableQuery = "CREATE TABLE IF NOT EXISTS expenses (" +
	                "expense_id INT AUTO_INCREMENT PRIMARY KEY, " +
	                "expense_date DATE NOT NULL, " +
	                "expense_name VARCHAR(255) NOT NULL, " +
	                "establishment_name VARCHAR(255) NOT NULL, " +
	                "total_cost DECIMAL(10,2) NOT NULL, " +
	                "split_count INT NOT NULL, " +
	                "payer_name VARCHAR(255) NOT NULL" +
	                ")";

	        Statement createTableStatement = connection.createStatement();
	        int tableCreated = createTableStatement.executeUpdate(createTableQuery);

	        if (tableCreated >= 0) {
	            System.out.println("Table 'expenses' created successfully");
	        } else {
	            System.out.println("Failed to create table 'expenses'");
	        }


	        // Prepare the insert statement
	        String insertQuery = "INSERT INTO expenses (expense_date, expense_name, establishment_name, total_cost, split_count, payer_name) " +
	                "VALUES (?, ?, ?, ?, ?, ?)";

	        // Convert java.util.Date to java.sql.Date
	        Date sqlDate = new Date(expense.getDate().getTime());

	        PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
	        statement.setDate(1, sqlDate);
	        statement.setString(2, expense.getExpenseName());
	        statement.setString(3, expense.getEstablishmentName());
	        statement.setDouble(4, expense.getExpenseCost());
	        statement.setInt(5, expense.getPortionNames().size());
	        statement.setString(6, expense.getPayerName());

	        int rowsInserted = statement.executeUpdate();

	        if (rowsInserted > 0) {
	            System.out.println("Record inserted into `expenses` table successfully");

	            // Retrieves the expense_id that was generated
	            ResultSet generatedKeys = statement.getGeneratedKeys();
	            if (generatedKeys.next()) {
	                generatedExpenseId = generatedKeys.getInt(1);
	            }
	        } else {
	            System.out.println("Failed to insert into `expenses` table");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return generatedExpenseId;
	}

	
	public void insertExpensePersonsData(Expense expense, List<Integer> personIds, int expenseId) {
	    try (Connection connection = establishConnection()) {
	        // Create the 'expensePersons' table if it doesn't exist
	        String createTableQuery = "CREATE TABLE IF NOT EXISTS expensePersons (" +
	                "expense_id INT NOT NULL, " +
	                "person_id INT NOT NULL, " +
	                "amount_owed DECIMAL(10,2) NOT NULL, " +
	                "PRIMARY KEY (expense_id, person_id), " +
	                "FOREIGN KEY (expense_id) REFERENCES expenses(expense_id), " +
	                "FOREIGN KEY (person_id) REFERENCES people(person_id)" +
	                ")";

	        Statement createTableStatement = connection.createStatement();
	        int tableCreated = createTableStatement.executeUpdate(createTableQuery);

	        if (tableCreated >= 0) {
	            System.out.println("Table 'expensePersons' created successfully");
	        } else {
	            System.out.println("Failed to create table 'expensePersons'");
	        }

	        // Prepare the insert statement
	        String insertQuery = "INSERT INTO expensePersons (expense_id, person_id, amount_owed) " +
	                "VALUES (?, ?, ?)";

	        PreparedStatement statement = connection.prepareStatement(insertQuery);

	        for (Integer personId : personIds) {
	            statement.setInt(1, expenseId);
	            statement.setInt(2, personId);
	            statement.setDouble(3, expense.calculateCostPerPortion());

	            int rowsInserted = statement.executeUpdate();

	            if (rowsInserted > 0) {
	                System.out.println("Record inserted into `expensePersons` table successfully");
	            } else {
	                System.out.println("Failed to insert into `expensePersons` table");
	            }
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
}