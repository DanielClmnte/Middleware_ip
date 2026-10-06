package com.example.coche.conexion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    // La URL, el usuario y la contraseña se leen de application.properties (claves db.*)
    private static final String ARCHIVO_CONFIGURACION = "/application.properties";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static ConexionBD instancia;

    private final Connection conexion;

    private ConexionBD() {
        try {
            Properties configuracion = cargarConfiguracion();
            Class.forName(DRIVER);
            conexion = DriverManager.getConnection(
                    configuracion.getProperty("db.url"),
                    configuracion.getProperty("db.usuario"),
                    configuracion.getProperty("db.password", ""));
        } catch (ClassNotFoundException | SQLException | IOException e) {
            throw new RuntimeException("Error al conectar con la base de datos", e);
        }
    }

    private static Properties cargarConfiguracion() throws IOException {
        try (InputStream entrada = ConexionBD.class.getResourceAsStream(ARCHIVO_CONFIGURACION)) {
            if (entrada == null) {
                throw new IOException("No se encuentra " + ARCHIVO_CONFIGURACION);
            }
            Properties configuracion = new Properties();
            configuracion.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            return configuracion;
        }
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
