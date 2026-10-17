package com.appregistros.practicaevaluadacorte2.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/papeleria_campus");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "andresmejia");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "amejia62");

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
