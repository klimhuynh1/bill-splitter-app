package com.mnfll.bill_splitter_app;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcPeopleDAO implements ExpensePersonsDAO {
    public List<Integer> insertPeopleData(Expense expense) {
        List<Integer> generatedKeys = new ArrayList<>();
        Connection connection = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Check if the name already exists
            String selectQuery = "SELECT person_id FROM people WHERE person_name = ?";

            // Add new records to the `people` table
            String insertQuery = "INSERT INTO people (person_name) VALUES (?)";

            for (String debtorName : expense.getDebtorNames()) {
                // Check if the name already exists
                PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                selectStatement.setString(1, debtorName);
                ResultSet selectResultSet = selectStatement.executeQuery();

                if (selectResultSet.next()) {
                    // Name already exists, retrieve the person id
                    generatedKeys.add(selectResultSet.getInt("person_id"));
                } else {
                    // Name doesn't exist, insert a new record
                    PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, debtorName);

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
        } catch (
                SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }

        return generatedKeys;
    }

    public int getPersonIdByName(String personName) {
        Connection connection = null;
        int personId = -1;
        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Retrieve person_id given person_name
            String selectQuery = "SELECT person_id FROM people WHERE person_name = ? LIMIT 1";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.setString(1, personName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                personId = resultSet.getInt("person_id");
            } else {
                System.out.println("Person Id not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnectionManager.closeConnection(connection);
        }
        return personId;
    }

    public List<String> getAllPeopleNames() {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        List<String> peopleNames = new ArrayList<>();

        try {
            conn = DatabaseConnectionManager.establishConnection();
            String query = "SELECT person_name FROM people";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                String personName = rs.getString("person_name");
                peopleNames.add(personName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closeStatement(stmt);
            ResourcesUtils.closeConnection(conn);
        }
        return peopleNames;
    }

    public void deleteOrphanUsers() {
        Connection connection = null;
        PreparedStatement ps1 = null;
        ResultSet rs1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs2 = null;
        PreparedStatement ps3 = null;

        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Delete from `people` table if they're not associated with any expenses
            String selectQuery = "SELECT person_id from people";
            ps1 = connection.prepareStatement(selectQuery);
            rs1 = ps1.executeQuery();

            // Iterate through the result set
            while (rs1.next()) {
                int personId = rs1.getInt("person_id");

                // Check if the person_id exists in the person table
                String checkQuery = "SELECT COUNT(*) FROM expensePersons WHERE person_id = ?";
                ps2 = connection.prepareStatement(checkQuery);
                ps2.setInt(1, personId);
                rs2 = ps2.executeQuery();

                int count = rs2.getInt(1);

                if (count == 0) {
                    // Delete the user from the people table
                    String deleteQuery = "DELETE FROM people WHERE person_id = ?";
                    ps3 = connection.prepareStatement(deleteQuery);
                    ps3.setInt(1, personId);
                    int rowsAffected = ps3.executeUpdate();

                    System.out.println(rowsAffected + " rows(s) for user ID: " + personId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closePrepapredStatement(ps3);
            ResourcesUtils.closeResultSet(rs2);
            ResourcesUtils.closePrepapredStatement(ps2);
            ResourcesUtils.closeResultSet(rs1);
            ResourcesUtils.closePrepapredStatement(ps1);
            ResourcesUtils.closeConnection(connection);
        }
    }

    public int addNewPersonIfNotExists(Connection conn, String debtorName) throws SQLException {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            String selectQuery = "SELECT person_id FROM people WHERE person_name = ?";
            ps1 = conn.prepareStatement(selectQuery);
            ps1.setString(1, debtorName);
            rs = ps1.executeQuery();

            if (rs.next()) {
                // Person already exists, retrieve the person id
                return rs.getInt("person_id");
            } else {
                // Person does not exist, create a new record
                String insertQuery = "INSERT INTO people (person_name) VALUES (?)";
                ps2 = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                ps2.setString(1, debtorName);
                ps2.executeUpdate();

                // Retrieve the generated person_id
                ResultSet generatedKeys = ps2.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ResourcesUtils.closeResultSet(rs);
            ResourcesUtils.closePrepapredStatement(ps2);
            ResourcesUtils.closePrepapredStatement(ps1);
        }

        return -1;
    }
}
