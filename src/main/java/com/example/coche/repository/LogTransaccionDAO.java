package com.example.coche.repository;

import com.example.coche.conexion.ConexionBD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final Logger log = LoggerFactory.getLogger(LogTransaccionDAO.class);

    private static final int LONGITUD_MAXIMA_DETALLE = 255;

    // Copia del log en formato JSON (se crea en la carpeta del proyecto, igual que ips.txt)
    private static final Path ARCHIVO_LOG_JSON = Path.of("log_transacciones.json");

    private static final String SQL_INSERT =
            "INSERT INTO log_transaccion (ip, accion, coche_id, detalle, resultado) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_ALL =
            "SELECT id, fecha, ip, accion, coche_id, detalle, resultado FROM log_transaccion ORDER BY id DESC";

    private final ObjectMapper objectMapper;

    public LogTransaccionDAO(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        // Mismo candado que usa CocheServiceImpl para sus transacciones: un registro que llegue desde
        // otra petición (p. ej. un bloqueo del IpFilter) espera a que termine la transacción en curso,
        // en vez de colarse dentro de ella y perderse si acaba en rollback.
        synchronized (ConexionBD.class) {
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
    }

    // El archivo se regenera entero a partir de la tabla. Hay que llamarlo cuando la transacción ya ha
    // terminado: así nunca contiene un cambio que el rollback haya deshecho.
    public void actualizarArchivoJson() {
        // synchronized: si dos peticiones lo regeneran a la vez, escriben de una en una
        synchronized (ConexionBD.class) {
            try {
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listarTodos());
                Files.writeString(ARCHIVO_LOG_JSON, json);
            } catch (IOException | RuntimeException e) {
                // Si falla la escritura, lo avisamos, pero la web sigue funcionando
                log.error("No se pudo actualizar {}", ARCHIVO_LOG_JSON, e);
            }
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
