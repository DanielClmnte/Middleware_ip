package com.example.coche.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL = "jdbc:mysql://localhost:3306/coche_db?useSSL=false&serverTimezone=UTC";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "root";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static ConexionBD instancia;

    private final Connection conexion;

    private ConexionBD() {
        try {
            Class.forName(DRIVER);
            conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Error al conectar con la base de datos", e);
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
