package com.example.coche.repository;

import com.example.coche.conexion.ConexionBD;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class LogTransaccionDAO {

    private static final int LONGITUD_MAXIMA_DETALLE = 255;

    private static final String SQL_INSERT =
            "INSERT INTO log_transaccion (ip, accion, coche_id, detalle, resultado) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_ALL =
            "SELECT id, fecha, ip, accion, coche_id, detalle, resultado FROM log_transaccion ORDER BY id DESC";

    // Cada fila se devuelve como un Map (clave = nombre del campo) para que Spring lo convierta a JSON.
    // LinkedHashMap mantiene el orden en que se añaden las claves.
    public List<Map<String, Object>> listarTodos() {
        List<Map<String, Object>> registros = new ArrayList<>();
        try (PreparedStatement stmt = ConexionBD.getInstancia().getConexion().prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> registro = new LinkedHashMap<>();
                registro.put("id", rs.getLong("id"));
                registro.put("fecha", rs.getString("fecha"));
                registro.put("ip", rs.getString("ip"));
                registro.put("accion", rs.getString("accion"));
                registro.put("cocheId", rs.getObject("coche_id", Long.class));
                registro.put("detalle", rs.getString("detalle"));
                registro.put("resultado", rs.getString("resultado"));
                registros.add(registro);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al leer el log de transacciones", e);
        }
        return registros;
    }

    public void registrar(String ip, String accion, Long cocheId, String detalle, String resultado) {
        try (PreparedStatement stmt = ConexionBD.getInstancia().getConexion().prepareStatement(SQL_INSERT)) {

            stmt.setString(1, ip);
            stmt.setString(2, accion);
            stmt.setObject(3, cocheId, Types.BIGINT);
            stmt.setString(4, recortar(detalle));
            stmt.setString(5, resultado);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar la transacción en el log", e);
        }
    }

    // Los mensajes de error de MySQL pueden superar el tamaño de la columna detalle
    private String recortar(String texto) {
        if (texto == null || texto.length() <= LONGITUD_MAXIMA_DETALLE) {
            return texto;
        }
        return texto.substring(0, LONGITUD_MAXIMA_DETALLE);
    }
}
