package com.example.coche.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import com.example.coche.log.RegistroTransacciones;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(IpFilter.class);

    // Archivo donde se guardan las visitas (se crea en la carpeta del proyecto)
    private static final Path ARCHIVO_IPS = Path.of("ips.txt");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Reglas de seguridad: más de 20 peticiones en 1 segundo = bloqueo de 5 minutos
    private static final int  MAX_PETICIONES = 20;
    private static final long VENTANA_NS     = 1_000_000_000L;           // 1 segundo
    private static final long BLOQUEO_NS     = 5L * 60 * 1_000_000_000L; // 5 minutos

    // IP -> momentos (en nanosegundos) de sus últimas peticiones
    private final Map<String, Deque<Long>> historial = new ConcurrentHashMap<>();
    // IP -> momento (en nanosegundos) en que termina su bloqueo
    private final Map<String, Long> bloqueadas = new ConcurrentHashMap<>();

    // Historial de transacciones (el mismo transacciones.json que usan los coches):
    // aqui lo usamos para apuntar tambien los bloqueos de IP.
    private final RegistroTransacciones registro;

    public IpFilter(RegistroTransacciones registro) {
        this.registro = registro;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String ip = obtenerIp(req);
        String metodo = req.getMethod();
        String ruta = req.getRequestURI();
        long ahora = System.nanoTime();

        log.info("Visita -> IP: {} | {} {}", ip, metodo, ruta);
        guardarEnArchivo(ip, metodo + " " + ruta);

        // 1. ¿Esta IP ya está bloqueada?
        Long finBloqueo = bloqueadas.get(ip);
        if (finBloqueo != null) {
            if (ahora < finBloqueo) {
                // Excepción: el CSS sí pasa, para que la página de bloqueo tenga los estilos de la web
                if (ruta.startsWith("/css/")) {
                    chain.doFilter(req, res);
                    return;
                }
                res.sendError(429);   // 429 = Too Many Requests
                return;               // NO llamamos a chain.doFilter -> la petición muere aquí
            }

            bloqueadas.remove(ip);    // ya pasaron los 5 minutos: se le perdona
        }

        // 2. Contar peticiones en el último segundo (ventana deslizante)
        Deque<Long> tiempos = historial.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (tiempos) {
            // tirar las peticiones que ya se salieron de la ventana de 1 segundo
            while (!tiempos.isEmpty() && ahora - tiempos.peekFirst() > VENTANA_NS) {
                tiempos.pollFirst();
            }
            tiempos.addLast(ahora);

            // 3. ¿Se ha pasado del límite?
            if (tiempos.size() > MAX_PETICIONES) {
                bloqueadas.put(ip, ahora + BLOQUEO_NS);
                tiempos.clear();
                log.warn("IP {} BLOQUEADA: más de {} peticiones en 1 segundo", ip, MAX_PETICIONES);
                guardarEnArchivo(ip, "*** BLOQUEADA 5 min por exceso de peticiones ***");
                // Tambien lo apuntamos en el historial de transacciones (transacciones.json)
                registro.registrar(ip, "BLOQUEO", null,
                        "mas de " + MAX_PETICIONES + " peticiones en 1 segundo", "BLOQUEADA");
                res.sendError(429);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    /**
     * Devuelve la IP del cliente. Si viene la cabecera X-Forwarded-For (la que ponen
     * los proxys/balanceadores con la IP real), usa la primera IP de esa cabecera;
     * si no, usa la IP de la conexion (getRemoteAddr).
     *
     * OJO (importante para la memoria): confiar en X-Forwarded-For solo es seguro si la
     * cabecera la pone TU proxy. Un cliente puede falsificarla para fingir muchas IPs
     * distintas (justo lo que hace nuestro bot de pruebas). En produccion solo deberia
     * leerse esta cabecera cuando viene de un proxy de confianza.
     */
    private String obtenerIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();   // la primera IP de la lista es la del cliente original
        }
        return req.getRemoteAddr();
    }

    // synchronized: si llegan dos peticiones a la vez, escriben de una en una
    // y no se mezclan las líneas en el archivo
    private synchronized void guardarEnArchivo(String ip, String texto) {
        String fecha = LocalDateTime.now().format(FORMATO_FECHA);
        String linea = fecha + " | " + ip + " | " + texto + System.lineSeparator();
        try {
            // CREATE: crea el archivo si no existe. APPEND: añade al final sin borrar lo anterior
            Files.writeString(ARCHIVO_IPS, linea,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Si falla la escritura, lo avisamos, pero la web sigue funcionando
            log.error("No se pudo guardar la IP en {}", ARCHIVO_IPS, e);
        }
    }
}
