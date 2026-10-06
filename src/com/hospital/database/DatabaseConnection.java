package com.hospital.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton class to manage MySQL database connection.
 * Ensures only one connection instance is used throughout the application.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    // ═══════════════════════════════════════════════════════════════
    //  DATABASE CONFIGURATION — Edit these to match your MySQL setup
    // ═══════════════════════════════════════════════════════════════
    //  Instead of hardcoding, you can also set environment variables:
    //    HOSPITAL_DB_URL, HOSPITAL_DB_USER, HOSPITAL_DB_PASSWORD
    // ═══════════════════════════════════════════════════════════════

    private static final String DB_URL =
        System.getenv().getOrDefault("HOSPITAL_DB_URL",
            "jdbc:mysql://localhost:3306/hospital_db?useSSL=false&allowPublicKeyRetrieval=true");

    private static final String DB_USER =
        System.getenv().getOrDefault("HOSPITAL_DB_USER", "root");

    private static final String DB_PASSWORD =
        System.getenv().getOrDefault("HOSPITAL_DB_PASSWORD", "root");

    /**
     * Private constructor to prevent instantiation from outside.
     * Attempts to load the MySQL JDBC driver and establish connection.
     */
    private DatabaseConnection() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[DB] Database connection established successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB ERROR] MySQL JDBC Driver not found! Ensure mysql-connector-java.jar is in classpath.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns the singleton instance of DatabaseConnection.
     * Creates a new instance if none exists.
     *
     * @return DatabaseConnection singleton instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Returns the active database Connection object.
     * Attempts to reconnect if the connection is closed.
     *
     * @return Connection object
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[DB] Reconnecting to database...");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to reconnect: " + e.getMessage());
        }
        return connection;
    }

    /**
     * Closes the database connection gracefully.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error closing connection: " + e.getMessage());
        }
    }
}
