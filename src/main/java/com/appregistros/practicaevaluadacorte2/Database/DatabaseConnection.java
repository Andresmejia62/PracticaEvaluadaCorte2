package com.appregistros.practicaevaluadacorte2.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Configuración centralizada de la conexión a la base de datos.
    // Los valores reales deben definirse en variables de entorno del sistema.
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/papeleria_campus");
    //cambiar credenciales (usuario y contraseña) para que se pueda conectar a la base de datos
    private static final String USER = System.getenv().getOrDefault("DB_USER", "andresmejia");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "amejia62");

    // Se evita crear instancias de esta clase utilitaria.
    private DatabaseConnection() {
    }

    // Abre una conexión segura con credenciales externas al código fuente.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
