package com.appregistros.practicaevaluadacorte2.Database;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseConnection {

    // Configuración centralizada de la conexión a la base de datos.
    // Los valores se toman, en este orden, de:
    //   1. Variables de entorno DB_URL, DB_USER y DB_PASSWORD.
    //   2. El archivo local config/db.properties (excluido del repositorio con .gitignore).
    //   3. Valores por defecto sin credenciales reales.
    // Ver config/db.properties.example.
    private static final Path ARCHIVO_CONFIGURACION = Path.of("config", "db.properties");
    private static final Properties CONFIGURACION = cargarConfiguracion();

    private static final String URL = valor("DB_URL", "db.url", "jdbc:postgresql://localhost:5432/papeleria_campus");
    private static final String USER = valor("DB_USER", "db.user", "postgres");
    private static final String PASSWORD = valor("DB_PASSWORD", "db.password", "");

    // Se evita crear instancias de esta clase utilitaria.
    private DatabaseConnection() {
    }

    // Abre una conexión con credenciales externas al código fuente.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String valor(String variableEntorno, String propiedad, String porDefecto) {
        String desdeEntorno = System.getenv(variableEntorno);
        if (desdeEntorno != null && !desdeEntorno.isBlank()) {
            return desdeEntorno;
        }
        return CONFIGURACION.getProperty(propiedad, porDefecto);
    }

    private static Properties cargarConfiguracion() {
        Properties propiedades = new Properties();
        if (Files.isReadable(ARCHIVO_CONFIGURACION)) {
            try (Reader reader = Files.newBufferedReader(ARCHIVO_CONFIGURACION, StandardCharsets.UTF_8)) {
                propiedades.load(reader);
            } catch (IOException ex) {
                Logger.getLogger(DatabaseConnection.class.getName())
                        .warning("No se pudo leer " + ARCHIVO_CONFIGURACION + ": " + ex.getMessage());
            }
        }
        return propiedades;
    }
}
