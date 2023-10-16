package com.mnfll.bill_splitter_app;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.mnfll.bill_splitter_app.utilities.InputValidator;

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
	
	public void saveExpenseDataToDatabase(Expense expense) throws SQLException {
		List<Integer> personIds = insertPeopleData(expense);
		int expenseId = insertExpenseData(expense);
	    insertExpensePersonsData(expense, personIds, expenseId);
	    createJoinedPersonName();
	    createCombinedExpensePersons();
	    generateNamePermutations();
	    calculateDebts();
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

	        // Prepare the select statement to check if the name already exists
	        String selectQuery = "SELECT person_id FROM people WHERE person_name = ?"; 
	        
	        // Prepare the insert statement to add new records to the `people` table
	        String insertQuery = "INSERT INTO people (person_name) VALUES (?)";

	        for (String portionName : expense.getPortionNames()) {
	        	// Check if the name already exists
	        	PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
	        	selectStatement.setString(1, portionName);
	        	ResultSet selectResultSet = selectStatement.executeQuery();
	        	
	        	if (selectResultSet.next()) {
	        		// Name already exists, retrieve the person id
	        		generatedKeys.add(selectResultSet.getInt("person_id"));
	        	}
	        	else {
	        		// Name doesn't exist, insert a new record
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
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return generatedKeys;
	}

	public int getPersonIdByName(String personName) {
		int personId = -1;
		try (Connection connection = establishConnection()) {
	        // Get the payer_id for the 
	        String selectQuery = "SELECT person_id FROM people WHERE person_name = ? LIMIT 1";
	        PreparedStatement statement = connection.prepareStatement(selectQuery);
	        statement.setString(1, personName);
	        ResultSet resultSet = statement.executeQuery();
	        if (resultSet.next()) {
	        	personId = resultSet.getInt("person_id");
	        	System.out.println("Person ID: " + personId);
	        }
	        else {
	        	System.out.println("Person Id not found");
	        }
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return personId;
	}
	
	public int getPayerIdByExpenseId(int expenseId) {
		int payerId = -1;
		try (Connection connection = establishConnection()) {
	        // Get the payer_id for the 
	        String selectQuery = "SELECT payer_id FROM expenses WHERE expense_id = ? LIMIT 1";
	        PreparedStatement statement = connection.prepareStatement(selectQuery);
	        statement.setInt(1, expenseId);
	        ResultSet resultSet = statement.executeQuery();
	        if (resultSet.next()) {
	        	payerId = resultSet.getInt("payer_id");
	        	System.out.println("Payer ID: " + payerId);
	        }
	        else {
	        	System.out.println("Payer Id not found");
	        }
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return payerId;
	}
	
	public int insertExpenseData(Expense expense) {
	    int generatedExpenseId = -1;
	    int payerId = getPersonIdByName(expense.getPayerName());

	    try (Connection connection = establishConnection()) {
	        // Create the table if it doesn't exist
	        String createTableQuery = "CREATE TABLE IF NOT EXISTS expenses (" +
	                "expense_id INT AUTO_INCREMENT PRIMARY KEY, " +
	                "expense_date DATE NOT NULL, " +
	                "expense_name VARCHAR(255) NOT NULL, " +
	                "establishment_name VARCHAR(255) NOT NULL, " +
	                "total_cost DECIMAL(10,2) NOT NULL, " +
	                "split_count INT NOT NULL, " +
	                "payer_id INT NOT NULL," +
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
	        String insertQuery = "INSERT INTO expenses (expense_date, expense_name, establishment_name, total_cost, split_count, payer_id, payer_name) " +
	                "VALUES (?, ?, ?, ?, ?, ?, ?)";

	        // Convert java.util.Date to java.sql.Date
	        Date sqlDate = new Date(expense.getDate().getTime());

	        PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
	        statement.setDate(1, sqlDate);
	        statement.setString(2, expense.getItemName());
	        statement.setString(3, expense.getEstablishmentName());
	        statement.setDouble(4, expense.getItemCost());
	        statement.setInt(5, expense.getPortionNames().size());
	        statement.setInt(6, payerId);
	        statement.setString(7, expense.getPayerName());

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
	        String createQuery = "CREATE TABLE IF NOT EXISTS expensePersons (" +
	                "expense_id INT NOT NULL, " +
	        		"payer_id INT NOT NULL, " +
	                "portion_id INT NOT NULL, " +
	                "amount_owed DECIMAL(10,2) NOT NULL, " +
	                "PRIMARY KEY (expense_id, portion_id), " +
	                "FOREIGN KEY (expense_id) REFERENCES expenses(expense_id), " +
	                "FOREIGN KEY (portion_id) REFERENCES people(person_id)" +
	                ")";

	        Statement createTableStatement = connection.createStatement();
	        int tableCreated = createTableStatement.executeUpdate(createQuery);

	        if (tableCreated >= 0) {
	            System.out.println("Table 'expensePersons' created successfully");
	        } else {
	            System.out.println("Failed to create table 'expensePersons'");
	        }
	        
	        // Retrieve payer_id based on expenseId
	        String selectQuery = "SELECT payer_id FROM expenses WHERE expense_id = ?";
	        PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
			preparedStatement.setInt(1, expenseId);
			ResultSet resultSet = preparedStatement.executeQuery();
			int payerId = 0;
			
			if (resultSet.next()) {
				payerId = resultSet.getInt("payer_id"); 		
			}
	        	        
	        // Prepare the insert statement
	        String insertQuery = "INSERT INTO expensePersons (expense_id, payer_id, portion_id, amount_owed) " +
	                "VALUES (?, ?, ?, ?)";

	        PreparedStatement insertTableStatement = connection.prepareStatement(insertQuery);

	        for (Integer personId : personIds) {
	        	insertTableStatement.setInt(1, expenseId);
	        	insertTableStatement.setInt(2, payerId);
	        	insertTableStatement.setInt(3, personId);
	        	insertTableStatement.setDouble(4, expense.getItemCost()/expense.getPortionNames().size());

	            int rowsInserted = insertTableStatement.executeUpdate();

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
	
	public List<List<String>> generateNamePermutations() {
		List<String> names = new ArrayList<>();
		List<List<String>> permutations = new ArrayList<>(); 

		// Retrieve a list of all the portion names
		try(Connection connection = establishConnection()) {
			String query = "SELECT person_name FROM people";

			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);

			while(resultSet.next()) {
				String personName = resultSet.getString("person_name");
				names.add(personName);
			}			
		}
		catch (SQLException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < names.size(); i++) {
			for (int j = 0; j < names.size(); j++) {
				List<String> permutation = new ArrayList<>();
				permutation.add(names.get(i));
				permutation.add(names.get(j));
				permutations.add(permutation);
			}
		}
		
	return permutations;
	}
	
	public void createJoinedPersonName() {
		String createViewQuery = "CREATE VIEW joinedPersonName AS "
				+ "SELECT ep.expense_id, ep.payer_id, p.person_name AS payer_name, ep.portion_id, ep.amount_owed "
				+ "FROM expensePersons ep "
				+ "JOIN people p ON ep.payer_id = p.person_id" ;
		try (Connection connection = establishConnection()) {
			Statement statement = connection.createStatement();
			statement.execute(createViewQuery);
			System.out.println("View `createJoinedPersonName` created successfully.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createCombinedExpensePersons() {
		String createViewQuery = "CREATE VIEW combinedExpensePersons AS "
				+ "SELECT v.expense_id, v.payer_id, v.payer_name, v.portion_id, p.person_name AS portion_name, v.amount_owed "
				+ "FROM joinedPersonName v "
				+ "JOIN people p ON v.portion_id = p.person_id";
		try (Connection connection = establishConnection()) {
			Statement statement = connection.createStatement();
			statement.execute(createViewQuery);
			System.out.println("View `createCombinedExpensePersons` created successfully.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void calculateDebts() throws SQLException {
	    List<List<String>> nameList = generateNamePermutations();

	    // Create a table joining expensePersons and people to get person_name
	    try (Connection connection = establishConnection()) {
	        String selectQuery = "SELECT payer_id, payer_name, portion_id, portion_name, SUM(amount_owed) AS total_amount_owed " +
	                            "FROM combinedExpensePersons " +
	                            "WHERE payer_name = ? AND portion_name = ? " +
	                            "GROUP BY payer_id, payer_name, portion_id, portion_name";

	        PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
	        // Print the table headers with tabs to align
	        ResultSetMetaData metaData = preparedStatement.getMetaData();
	        int columnCount = metaData.getColumnCount();
	        for (int i = 1; i <= columnCount; i++) {
	            System.out.print(metaData.getColumnName(i));
	            if (i < columnCount) {
	                System.out.print("\t\t"); // Use tabs to separate headers and align columns
	            } else {
	                System.out.println(); // Start a new line after the last header
	            }
	        }
	        
	        for (List<String> namePair : nameList) {
	            if (namePair.size() >= 2) {
	                String payerName = namePair.get(0);
	                String portionName = namePair.get(1);
	                preparedStatement.setString(1, payerName);
	                preparedStatement.setString(2, portionName);

	                try (ResultSet resultSet = preparedStatement.executeQuery()) {
	                    while (resultSet.next()) {
	                        // Print the data for each row
	                        for (int i = 1; i <= columnCount; i++) {
	                            System.out.print(String.format("%-12s", resultSet.getString(i)));
	                            if (i < columnCount) {
	                                System.out.print("\t\t"); // Use tabs to separate data and align columns
	                            } else {
	                                System.out.println(); // Start a new line after the last data
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	//	TODO: Requires testing
	public void updateExpense(int expenseId, int updateOption, Scanner scanner) throws ParseException {
		try (Connection connection = establishConnection()) {
			switch (updateOption) {
		    case 1:
		    	updateExpenseDate(expenseId, scanner);   	
		        break;
		    case 2:
		        updateExpenseEstablishmentName(expenseId, scanner);
		        break;
		    case 3:
		    	updateExpenseName(expenseId, scanner);
		        break;
		    case 4:
		    	updateExpenseCost(expenseId, scanner);
		        break;
		    case 5:
		    	addPortionName(expenseId, scanner);
		        break;
		    case 6:
		    	removePortionName(expenseId, scanner);
		        break;
		    case 7:
		        // Edit payer_name in `expense` table based on expense_id
		    	updatePayerName(expenseId, scanner);
		        break;
		    case 8:
		        deleteExpense(expenseId);
		        break;
		    default:
		        System.out.println("Invalid update option");
			}	
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// TODO: Update both payer_id and payer_name
	// TODO: Create a new entry for people table if they don't exist
	public void updatePayerName(int expenseId, Scanner scanner) {
		try (Connection connection = establishConnection()) {
			boolean isValidName = false;
			String newPayerName = "";
			
			while (!isValidName) {
				System.out.println("Enter a new payer name");
				newPayerName = scanner.nextLine();
				
				if (InputValidator.isValidName(newPayerName)) {
					isValidName = true;
				}
				else {
					System.out.println("Invalid name. Please enter a valid name");
				}					
			}
			
			String updateStatement = "UPDATE expenses SET payer_name = ? WHERE expense_id = ?";
			PreparedStatement preparedStatement = connection.prepareStatement(updateStatement);
			preparedStatement.setString(1, newPayerName);
			preparedStatement.setInt(2, expenseId);
			
			int rowsAffected = preparedStatement.executeUpdate();
			
			if (rowsAffected > 0) {
				System.out.println("Update successful.");
			}
			else {
				System.out.println("No rows were updated.");
			}			
		} catch (SQLException e) {
			e.printStackTrace();
		}		
	}
	
	public void removePortionName(int expenseId, Scanner scanner) {
		try (Connection connection = establishConnection()) {
			System.out.println("Enter the portion name you would like to remove");
			
			String portionName = scanner.nextLine();
			if (InputValidator.isValidName(portionName)) {
				int personId = getPersonIdByName(portionName);
				int payerId = getPayerIdByExpenseId(expenseId);
				
				// FIXME: temporary fix, will implement either singleton pattern and/or command pattern
				if (personId == payerId) {
					throw new IllegalArgumentException("You cannot remove portion name as they are the payer");
				} else {
					// Otherwise, remove record in `expensePersons` table based on person_id and expense_id
					String deleteQuery = "DELETE FROM expensePersons WHERE expense_id = ? AND person_id = ?";
					PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
					deleteStatement.setInt(1, expenseId);
					deleteStatement.setInt(2, personId);
					deleteStatement.executeUpdate();
					
					// Update split_count in `expenses` table based on expense_id
					int splitCount = updateSplitCount(connection, expenseId, false);
					// Re-calculate cost per portion 
					double newAmountOwed = calculateNewAmountOwed(connection, expenseId, splitCount);
					// Update amount_owed based on expense_id
					updateAmountOwed(connection, expenseId, newAmountOwed);		
				}
			}
		} catch (SQLException e) {
            e.printStackTrace();
        }
	}
	
	public void addPortionName(int expenseId, Scanner scanner) {
	    try (Connection connection = establishConnection()) {
	        String newPortionName = scanner.nextLine();
	        if (InputValidator.isValidName(newPortionName)) {
		        int personId = addNewPersonIfNotExists(connection, newPortionName);
		        int splitCount = updateSplitCount(connection, expenseId, true);
		        double newAmountOwed = calculateNewAmountOwed(connection, expenseId, splitCount);
		        updateAmountOwed(connection, expenseId, newAmountOwed);
		        addExpensePersonRecord(connection, expenseId, personId, newAmountOwed);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	private int addNewPersonIfNotExists(Connection connection, String portionName) throws SQLException {
	    String selectQuery = "SELECT person_id FROM people WHERE person_name = ?";
	    PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
	    selectStatement.setString(1, portionName);
	    ResultSet selectResultSet = selectStatement.executeQuery();
	    
	    if (selectResultSet.next()) {
	        // Person already exists, retrieve the person id
	        return selectResultSet.getInt("person_id");
	    } else {
	        // Person does not exist, create a new record
	        String insertQuery = "INSERT INTO people (person_name) VALUES (?)";
	        PreparedStatement insertStatement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
	        insertStatement.setString(1, portionName);
	        insertStatement.executeUpdate();
	        
	        // Retrieve the generated person_id
	        ResultSet generatedKeys = insertStatement.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            return generatedKeys.getInt(1);
	        }
	    }
	    
	    return -1; // Return a default value if personId is not found (shouldn't happen)
	}

	private int updateSplitCount(Connection connection, int expenseId, Boolean increment) throws SQLException {
	    String selectQuery = "SELECT split_count FROM expenses WHERE expense_id = ?";
	    PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
	    selectStatement.setInt(1, expenseId);
	    ResultSet selectResultSet = selectStatement.executeQuery();
	    int splitCount = 0;
	    
	    if (selectResultSet.next()) {
	    	if (increment) {
	    		splitCount = selectResultSet.getInt("split_count") + 1;
	    	}
	    	else {
	    		splitCount = selectResultSet.getInt("split_count") - 1;
	    	}
	        
	        
	        String updateQuery = "UPDATE expenses SET split_count = ? WHERE expense_id = ?";
	        PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
	        updateStatement.setInt(1, splitCount);
	        updateStatement.setInt(2, expenseId);
	        
	        int rowsAffected = updateStatement.executeUpdate();
	        System.out.println("Update split_count -- Rows affected: " + rowsAffected);
	        
	        return splitCount;
	    }
	    
	    return -1;
	}

	private double calculateNewAmountOwed(Connection connection, int expenseId, int splitCount) throws SQLException {
	    String selectQuery = "SELECT total_cost FROM expenses WHERE expense_id = ?";
	    PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
	    selectStatement.setInt(1, expenseId);
	    ResultSet selectResultSet = selectStatement.executeQuery();
	    
	    if (selectResultSet.next()) {
	        double expenseCost = selectResultSet.getDouble("total_cost");
	        return expenseCost / splitCount;
	    }
	    
	    return -1;
	}

	private void updateAmountOwed(Connection connection, int expenseId, double newAmountOwed) throws SQLException {
	    String updateQuery = "UPDATE expensePersons SET amount_owed = ? WHERE expense_id = ?";
	    PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
	    updateStatement.setDouble(1, newAmountOwed);
	    updateStatement.setInt(2, expenseId);
	    
	    int rowsAffected = updateStatement.executeUpdate();
	    System.out.println("Update amount owed -- Rows affected: " + rowsAffected);
	}

	private void addExpensePersonRecord(Connection connection, int expenseId, int personId, double newAmountOwed) throws SQLException {
	    String insertQuery = "INSERT INTO expensePersons (expense_id, person_id, amount_owed) VALUES (?, ?, ?)";
	    PreparedStatement statement = connection.prepareStatement(insertQuery);
	    statement.setInt(1, expenseId);
	    statement.setInt(2, personId);
	    statement.setDouble(3, newAmountOwed);
	    
	    int rowsAffected = statement.executeUpdate();
	    System.out.println("Add row for new portion name -- Rows affected: " + rowsAffected);
	}

	public void updateExpenseDate(int expenseId, Scanner scanner) {
		try (Connection connection = establishConnection()) {
	    	java.util.Date date = null;
	    	Date sqlDate = null;
	    	boolean isValidDate = false;
	    	
	    	while (!isValidDate) {
		    	System.out.println("Enter the new date [dd/MM/yyyy]");
		    	String dateString = scanner.nextLine();

		    	
		    	if (InputValidator.isValidDate(dateString)) {
		    		try {
						date = new SimpleDateFormat("dd/MM/yyyy").parse(dateString);
					} catch (ParseException e) {
						e.printStackTrace();
					}
		    		isValidDate = true;
		    	}
				else {
					System.out.println("Invalid date format. Please enter the date in dd/mm/yyyy format. ");
				}	
	    	}
	    	
	        // Convert java.util.Date to java.sql.Date
	    	sqlDate = new Date(date.getTime());
	        
	    	// Update expense_date in `expenses` table based on expense_id
	    	String query = "UPDATE expenses SET expense_date = ? WHERE expense_id = ?";
	    	PreparedStatement statement = connection.prepareStatement(query);
	    	
	    	statement.setDate(1, sqlDate);
	    	statement.setInt(2, expenseId);
	    	
	    	// Execute the update query
	    	int rowsAffected = statement.executeUpdate();
	    	
	    	// Check the number of rows affected
	    	System.out.println("Rows affected: " + rowsAffected);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateExpenseEstablishmentName(int expenseId, Scanner scanner) {
		try (Connection connection = establishConnection()) {
			String establishmentName = null;
	    	boolean isValidEstablishmentName = false;
	    	
	    	while (!isValidEstablishmentName) {
	    		System.out.println("Enter the new establishment name");
	    		 establishmentName = scanner.nextLine();
	    		
		    	if (InputValidator.isValidEstablishmentName(establishmentName)) {
		    		
		    		isValidEstablishmentName = true;
		    	}
				else {
					System.out.println("Invalid establishment name. Please enter a valid establishment name. ");
				}	
	    	}
	    	
	    	// Update establishment_name in `expenses` table based on expense_id
	    	String query = "UPDATE expenses SET establishment_name = ? WHERE expense_id = ?";
	    	PreparedStatement statement = connection.prepareStatement(query);
	    	
	    	statement.setString(1, establishmentName);
	    	statement.setInt(2, expenseId);
	    	
	    	// Execute the update query
	    	int rowsAffected = statement.executeUpdate();
	    	
	    	// Check the number of rows affected
	    	System.out.println("Rows affected: " + rowsAffected);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateExpenseName(int expenseId, Scanner scanner) {
		try (Connection connection = establishConnection()) {
			String expenseName = null;
	    	boolean isValidExpenseName = false;
	    	
	    	
	    	while (!isValidExpenseName) {
		    	System.out.println("Enter the new expense name");
		    	expenseName = scanner.nextLine();
		    	
		    	if (InputValidator.isValidExpenseName(expenseName)) {		    		
		    		isValidExpenseName = true;
		    	}
				else {
					System.out.println("Invalid expense name. Please enter a valid expense name. ");
				}	
	    	}
	    	
	    	// Update expense_date in `expenses` table based on expense_id
	    	String query = "UPDATE expenses SET expense_name = ? WHERE expense_id = ?";
	    	PreparedStatement statement = connection.prepareStatement(query);
	    	
	    	statement.setString(1, expenseName);
	    	statement.setInt(2, expenseId);
	    	
	    	// Execute the update query
	    	int rowsAffected = statement.executeUpdate();
	    	
	    	// Check the number of rows affected
	    	System.out.println("Rows affected: " + rowsAffected);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
    // Update total_cost in `expense` table based on expense_id
	public void updateExpenseCost(int expenseId, Scanner scanner) {
	    try (Connection connection = establishConnection()) {
	        double expenseCost = 0;
	        boolean isValidExpenseCost = false;

	        while (!isValidExpenseCost) {
	            System.out.println("Enter the new expense cost:");
	            String userInput = scanner.nextLine();

	            if (InputValidator.isValidCost(userInput)) {
	                expenseCost = Double.parseDouble(userInput);
	                isValidExpenseCost = true;
	            } else {
	                System.out.print("Invalid expense cost. Please enter a valid expense cost. ");
	            }
	        }

	        // Update expense_cost in `expenses` table based on expense_id
	        String updateQuery = "UPDATE expenses SET total_cost = ? WHERE expense_id = ?";
	        PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
	        updateStatement.setDouble(1, expenseCost);
	        updateStatement.setInt(2, expenseId);

	        // Execute the update query
	        int rowsAffected = updateStatement.executeUpdate();

	        // Check the number of rows affected
	        System.out.println("Rows affected: " + rowsAffected);

	        // Get the number of people that splitting this expense cost
	        String selectQuery = "SELECT split_count FROM expenses WHERE expense_id = ?";
	        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
	        selectStatement.setInt(1, expenseId);

	        // Execute the select query
	        ResultSet resultSet = selectStatement.executeQuery();
	        int splitCount = 0;
	        if (resultSet.next()) {
	            splitCount = resultSet.getInt("split_count");
	        }

	        // Re-calculate the cost per person
	        double newAmountOwed = expenseCost / splitCount;

	        // Update amount_owed in `expensePersons` table for each person
	        String updateAmountOwedQuery = "UPDATE expensePersons SET amount_owed = ? WHERE expense_id = ?";
	        PreparedStatement updateAmountOwedStatement = connection.prepareStatement(updateAmountOwedQuery);
	        updateAmountOwedStatement.setDouble(1, newAmountOwed);
	        updateAmountOwedStatement.setInt(2, expenseId);

	        // Execute the update query
	        int rowsUpdated = updateAmountOwedStatement.executeUpdate();

	        // Check the number of rows updated
	        System.out.println("Rows updated: " + rowsUpdated);
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	
	public void deleteOrphanUsers() {
		try (Connection connection = establishConnection()) {
	        // Delete from `people` table if they're not associated with any expenses
	        String selectQuery = "SELECT person_id from people";
	        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
	        ResultSet resultSet = selectStatement.executeQuery();
	        
	        // Iterate through the result set
	        while (resultSet.next()) {
	        	int personId = resultSet.getInt("person_id");
	        	
	        	// Check if the person_id exists in the person table
	        	String checkQuery = "SELECT COUNT(*) FROM expensePersons WHERE person_id = ?";
	        	PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
	        	checkStatement.setInt(1, personId);
	        	ResultSet checkResult = checkStatement.executeQuery();
	        	checkResult.next();
	        	
	        	int count = checkResult.getInt(1);
	        	
	        	if (count == 0) {
	        		// Delete the user from the people table
	        		String deleteQuery = "DELETE FROM people WHERE person_id = ?";
	        		PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
	        		deleteStatement.setInt(1, personId);
	        		int rowsAffected = deleteStatement.executeUpdate();
	        		
	        		System.out.println(rowsAffected + " rows(s) for user ID: " + personId);
	        	}
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
	// TODO: Refactor
	public void deleteExpense(int expenseId) {
	    try (Connection connection = establishConnection()) {
	        // Delete from expensePersons table
	        String deleteExpensePersonsQuery = "DELETE FROM expensePersons WHERE expense_id = ?";
	        PreparedStatement expensePersonsStatement = connection.prepareStatement(deleteExpensePersonsQuery);
	        expensePersonsStatement.setInt(1, expenseId);

	        // Execute the statement
	        int expensePersonsRowsAffected = expensePersonsStatement.executeUpdate();
	        if (expensePersonsRowsAffected > 0) {
	            System.out.println(expensePersonsRowsAffected + " row(s) deleted successfully from 'expensePersons' table.");
	        } else {
	            System.out.println("No rows deleted from 'expensePersons' table.");
	        }
	        
	        // Delete from expenses table
	        String deleteExpensesQuery = "DELETE FROM expenses WHERE expense_id = ?";

	        // Prepare the statement
	        PreparedStatement expensesStatement = connection.prepareStatement(deleteExpensesQuery);
	        expensesStatement.setInt(1, expenseId);

	        // Execute the statement
	        int expensesRowsAffected = expensesStatement.executeUpdate();
	        if (expensesRowsAffected > 0) {
	            System.out.println(expensesRowsAffected + " row(s) deleted successfully from 'expenses' table.");
	        } else {
	            System.out.println("No rows deleted from 'expenses' table.");
	        }
	        
	        // Delete users that are no longer associated with any expenses
	        deleteOrphanUsers();
   
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	public void displayTransactions() {
		// Display the following: date, establishment name, expenseid, expense name, cost, portion name, payer
	}
}