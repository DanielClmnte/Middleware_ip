package com.example.coche.service;

import com.example.coche.conexion.ConexionBD;
import com.example.coche.exception.CocheNoEncontradoException;
import com.example.coche.model.Coche;
import com.example.coche.repository.CocheRepository;
import com.example.coche.repository.LogTransaccionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Service
public class CocheServiceImpl implements CocheService {

    private static final Logger log = LoggerFactory.getLogger(CocheServiceImpl.class);

    private static final String RESULTADO_OK = "OK";
    private static final String RESULTADO_ERROR = "ERROR";

    // Copia del log en formato JSON (se crea en la carpeta del proyecto, igual que ips.txt)
    private static final Path ARCHIVO_LOG_JSON = Path.of("log_transacciones.json");

    private final CocheRepository cocheRepository;
    private final LogTransaccionDAO logTransaccionDAO;
    private final ObjectMapper objectMapper;

    public CocheServiceImpl(CocheRepository cocheRepository, LogTransaccionDAO logTransaccionDAO,
                            ObjectMapper objectMapper) {
        this.cocheRepository = cocheRepository;
        this.logTransaccionDAO = logTransaccionDAO;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Coche> findAll() {
        return cocheRepository.findAll();
    }

    @Override
    public ResultadoPagina<Coche> buscar(String marca, String combustible, String transmision,
                                          int pagina, int tamanioPagina) {
        long totalElementos = cocheRepository.contar(marca, combustible, transmision);
        int totalPaginas = (int) Math.max(1, Math.ceil((double) totalElementos / tamanioPagina));
        int paginaSegura = Math.max(0, Math.min(pagina, totalPaginas - 1));
        List<Coche> contenido = cocheRepository.buscar(marca, combustible, transmision, paginaSegura, tamanioPagina);
        return new ResultadoPagina<>(contenido, paginaSegura, totalPaginas, totalElementos);
    }

    @Override
    public List<String> listarMarcasDisponibles() {
        return cocheRepository.findDistinctMarcas();
    }

    @Override
    public List<String> listarCombustiblesDisponibles() {
        return cocheRepository.findDistinctCombustibles();
    }

    @Override
    public List<String> listarTransmisionesDisponibles() {
        return cocheRepository.findDistinctTransmisiones();
    }

    @Override
    public Coche findById(Long id) {
        return cocheRepository.findById(id)
                .orElseThrow(() -> new CocheNoEncontradoException(id));
    }

    @Override
    public Coche save(Coche coche, String ip) {
        ejecutarEnTransaccion(ip, "CREAR", coche, () -> cocheRepository.save(coche));
        return coche;
    }

    @Override
    public void update(Long id, Coche coche, String ip) {
        findById(id);
        coche.setId(id);
        ejecutarEnTransaccion(ip, "EDITAR", coche, () -> cocheRepository.update(coche));
    }

    @Override
    public void deleteById(Long id, String ip) {
        Coche coche = findById(id);
        ejecutarEnTransaccion(ip, "ELIMINAR", coche, () -> cocheRepository.deleteById(id));
    }

    /**
     * Ejecuta el cambio sobre la tabla coche y su registro en log_transaccion como UNA sola
     * transacción: o se guardan los dos (commit) o no se guarda ninguno (rollback).
     */
    private void ejecutarEnTransaccion(String ip, String accion, Coche coche, Runnable cambio) {
        // ConexionBD reparte la misma Connection a todas las peticiones, así que las transacciones
        // entran de una en una. Se bloquea sobre ConexionBD.class porque getInstancia() usa ese mismo
        // candado: mientras dura la transacción, ninguna otra petición puede coger la conexión.
        synchronized (ConexionBD.class) {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try {
                conexion.setAutoCommit(false);
                cambio.run();
                logTransaccionDAO.registrar(ip, accion, coche.getId(), describir(coche), RESULTADO_OK);
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                deshacer(conexion);
                registrarFallo(ip, accion, coche.getId(), e);
                throw e instanceof RuntimeException fallo
                        ? fallo
                        : new RuntimeException("Error al confirmar la operación " + accion, e);
            } finally {
                restaurarAutocommit(conexion);
                actualizarArchivoJson();
            }
        }
    }

    // El archivo se regenera entero a partir de la tabla cuando la transacción ya ha terminado:
    // así nunca contiene un cambio que el rollback haya deshecho
    private void actualizarArchivoJson() {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(logTransaccionDAO.listarTodos());
            Files.writeString(ARCHIVO_LOG_JSON, json);
        } catch (IOException | RuntimeException e) {
            // Si falla la escritura, lo avisamos, pero la operación sobre el coche ya está resuelta
            log.error("No se pudo actualizar {}", ARCHIVO_LOG_JSON, e);
        }
    }

    // Tras el rollback se vuelve a autocommit para que el registro del fallo se guarde por su cuenta
    private void deshacer(Connection conexion) {
        try {
            conexion.rollback();
            conexion.setAutoCommit(true);
        } catch (SQLException e) {
            log.error("No se pudo deshacer la transacción", e);
        }
    }

    // Va fuera de la transacción: si estuviera dentro, el rollback borraría también este registro
    private void registrarFallo(String ip, String accion, Long cocheId, Exception fallo) {
        // El DAO envuelve la SQLException; el motivo real (p. ej. matrícula duplicada) está en la causa más interna
        Throwable causa = fallo;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        try {
            logTransaccionDAO.registrar(ip, accion, cocheId, causa.getMessage(), RESULTADO_ERROR);
        } catch (RuntimeException e) {
            log.error("No se pudo registrar el fallo en log_transaccion", e);
        }
    }

    private void restaurarAutocommit(Connection conexion) {
        try {
            conexion.setAutoCommit(true);
        } catch (SQLException e) {
            log.error("No se pudo restaurar el autocommit de la conexión", e);
        }
    }

    private String describir(Coche coche) {
        return coche.getMarca() + " " + coche.getModelo() + " (" + coche.getMatricula() + ")";
    }
}
