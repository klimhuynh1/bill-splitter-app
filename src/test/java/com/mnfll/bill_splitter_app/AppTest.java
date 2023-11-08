package com.mnfll.bill_splitter_app;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.testng.Assert.*;

public class AppTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayInputStream inContent = new ByteArrayInputStream("6\n".getBytes());  // Simulate user input for exit

    @BeforeMethod
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setIn(inContent);
    }

    @AfterMethod
    public void resetOutputStream() {
        // Reset the output stream
        outContent.reset();
    }

    @Test
    public void testDisplayMainMenu() {
        // Call the displayMainMenu method
        App.displayMainMenu();

        // Get the output after the method call
        String displayedOutput = outContent.toString();

        String msg = "Welcome to the bill splitter app";
        // Add your assertions to validate the menu
        assertTrue(displayedOutput.contains(msg));
    }
}
