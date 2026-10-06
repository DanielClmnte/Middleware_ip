package com.example.coche.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Historial de movimientos de la web: guarda cada operacion (crear, editar, eliminar)
 * en el archivo transacciones.json como un array de objetos JSON.
 */
@Component
public class RegistroTransacciones {

    private static final Logger log = LoggerFactory.getLogger(RegistroTransacciones.class);

    private static final Path ARCHIVO = Path.of("transacciones.json");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ObjectMapper es la herramienta de Jackson que convierte objetos Java <-> JSON.
    // INDENT_OUTPUT: escribe el JSON bonito, con saltos de linea y sangria.
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    // synchronized: si dos operaciones ocurren a la vez, no se pisan al leer/escribir el archivo.
    public synchronized void registrar(String ip, String accion, Long cocheId,
                                       String detalle, String resultado) {
        Transaccion nueva = new Transaccion(
                LocalDateTime.now().format(FORMATO_FECHA), ip, accion, cocheId, detalle, resultado);
        try {
            List<Transaccion> historial = leerHistorial();  // lo que ya habia
            historial.add(nueva);                            // añado el movimiento nuevo
            mapper.writeValue(ARCHIVO.toFile(), historial);  // reescribo el array completo
        } catch (IOException e) {
            // Si falla la escritura, lo avisamos pero la web sigue funcionando.
            log.error("No se pudo escribir en {}", ARCHIVO, e);
        }
    }

    // Lee el array actual del JSON; si el archivo no existe o esta vacio, devuelve una lista vacia.
    private List<Transaccion> leerHistorial() throws IOException {
        if (!Files.exists(ARCHIVO) || Files.size(ARCHIVO) == 0) {
            return new ArrayList<>();
        }
        Transaccion[] existentes = mapper.readValue(ARCHIVO.toFile(), Transaccion[].class);
        return new ArrayList<>(Arrays.asList(existentes));
    }
}
