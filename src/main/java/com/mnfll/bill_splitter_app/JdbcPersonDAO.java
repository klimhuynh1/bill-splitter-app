package com.mnfll.bill_splitter_app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcPersonDAO implements PersonDAO {
    @Override
    public int getPersonIdByName(String personName) {
        Connection connection = null;
        int personId = -1;
        try {
            connection = DatabaseConnectionManager.establishConnection();
            // Get the creditor_id for the
            String selectQuery = "SELECT person_id FROM people WHERE person_name = ? LIMIT 1";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.setString(1, personName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                personId = resultSet.getInt("person_id");
//                System.out.println("Person ID: " + personId);
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
}
