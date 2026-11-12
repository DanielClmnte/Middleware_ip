package com.example.coche.security;

import com.example.coche.service.RegistroTransacciones;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

/**
 * Middleware de seguridad: se ejecuta ANTES que cualquier controller, en todas las peticiones.
 * Si una IP hace mas de 20 peticiones en 1 segundo, la bloquea durante 5 minutos (error 429).
 */
@Component
public class IpFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IpFilter.class);

    private static final Path ARCHIVO_IPS = Path.of("ips.txt");   // aqui se guardan las visitas
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int MAX_PETICIONES = 20;
    private static final long VENTANA_MS = 1000;            // 1 segundo
    private static final long BLOQUEO_MS = 5 * 60 * 1000;   // 5 minutos
    private static final int MAX_IPS_GUARDADAS = 1000;      // para que la memoria no crezca sin fin

    // IP -> momentos (en milisegundos) de sus ultimas peticiones
    private final Map<String, Deque<Long>> historial = new ConcurrentHashMap<>();
    // IP -> momento (en milisegundos) en que termina su bloqueo
    private final Map<String, Long> bloqueadas = new ConcurrentHashMap<>();

    private final RegistroTransacciones registro;

    public IpFilter(RegistroTransacciones registro) {
        this.registro = registro;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String ip = req.getRemoteAddr();
        String ruta = req.getRequestURI();
        long ahora = System.currentTimeMillis();

        log.info("Visita -> IP: {} | {} {}", ip, req.getMethod(), ruta);
        guardarEnArchivo(ip, req.getMethod() + " " + ruta);

        // Si hay demasiadas IPs guardadas, vaciamos los datos viejos
        if (historial.size() > MAX_IPS_GUARDADAS) {
            historial.clear();
        }
        if (bloqueadas.size() > MAX_IPS_GUARDADAS) {
            bloqueadas.values().removeIf(finBloqueo -> ahora >= finBloqueo);
        }

        // 1. ¿Esta IP esta bloqueada?
        Long finBloqueo = bloqueadas.get(ip);
        if (finBloqueo != null && ahora < finBloqueo) {
            if (ruta.startsWith("/css/")) {
                chain.doFilter(req, res);   // el CSS pasa para que la pagina de bloqueo tenga estilos
                return;
            }
            res.sendError(429);   // 429 = Too Many Requests
            return;               // NO llamamos a chain.doFilter -> la peticion muere aqui
        }
        bloqueadas.remove(ip);    // si ya pasaron los 5 minutos, se le perdona

        // 2. Contamos las peticiones del ultimo segundo
        Deque<Long> tiempos = historial.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (tiempos) {
            // tiramos las peticiones que ya tienen mas de 1 segundo
            while (!tiempos.isEmpty() && ahora - tiempos.peekFirst() > VENTANA_MS) {
                tiempos.pollFirst();
            }
            tiempos.addLast(ahora);

            // 3. ¿Se ha pasado del limite? -> bloqueamos la IP
            if (tiempos.size() > MAX_PETICIONES) {
                bloqueadas.put(ip, ahora + BLOQUEO_MS);
                tiempos.clear();
                log.warn("IP {} BLOQUEADA: mas de {} peticiones en 1 segundo", ip, MAX_PETICIONES);
                guardarEnArchivo(ip, "*** BLOQUEADA 5 min por exceso de peticiones ***");
                registro.registrar(ip, "BLOQUEO", null,
                        "mas de " + MAX_PETICIONES + " peticiones en 1 segundo", "BLOQUEADA");
                res.sendError(429);
                return;
            }
        }

        chain.doFilter(req, res);   // todo bien: la peticion sigue hacia el controller
    }

    // synchronized: si llegan dos peticiones a la vez, escriben de una en una
    private synchronized void guardarEnArchivo(String ip, String texto) {
        String linea = LocalDateTime.now().format(FORMATO_FECHA) + " | " + ip + " | " + texto
                + System.lineSeparator();
        try {
            Files.writeString(ARCHIVO_IPS, linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("No se pudo guardar la IP en {}", ARCHIVO_IPS, e);   // la web sigue funcionando
        }
    }
}
