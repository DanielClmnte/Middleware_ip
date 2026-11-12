package com.example.coche.conexion;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static ConexionBD instancia;

    private final Connection conexion;

    private ConexionBD() {
        Properties config = cargarConfiguracion();
        // Si existe la variable de entorno DB_PASSWORD, manda sobre la del archivo
        String password = System.getenv("DB_PASSWORD");
        if (password == null) {
            password = config.getProperty("db.password");
        }
        try {
            Class.forName(DRIVER);
            conexion = DriverManager.getConnection(
                    config.getProperty("db.url"), config.getProperty("db.usuario"), password);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Error al conectar con la base de datos", e);
        }
    }

    // Lee url, usuario y contraseña de application.properties
    private static Properties cargarConfiguracion() {
        Properties config = new Properties();
        try (InputStream archivo = ConexionBD.class.getResourceAsStream("/application.properties")) {
            config.load(archivo);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer application.properties", e);
        }
        return config;
    }

    public static synchronized ConexionBD getInstancia() {
        try {
            if (instancia == null || instancia.conexion.isClosed()) {
                instancia = new ConexionBD();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al comprobar el estado de la conexión", e);
        }
        return instancia;
    }

    public Connection getConexion() {
        return conexion;
    }
}
