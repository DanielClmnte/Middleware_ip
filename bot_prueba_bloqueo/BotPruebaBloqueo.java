import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Herramienta de prueba del middleware IpFilter del proyecto Middleware_ip.
 *
 * Lanza una ráfaga corta de peticiones contra la propia aplicación en localhost para
 * cruzar el umbral de 20 peticiones por segundo y comprobar que el filtro responde 429
 * (Too Many Requests) y bloquea la IP. Sirve para demostrar el bloqueo en la defensa.
 *
 * NO es una herramienta de ataque: apunta fijo a localhost, hace una única ráfaga y termina.
 *
 * Uso (no necesita Maven):
 *   java BotPruebaBloqueo.java
 */
public class BotPruebaBloqueo {

    // Dirección fija de la propia aplicación en local. El filtro bloquea por IP, así que
    // para ver el bloqueo hay que lanzar las peticiones contra la máquina donde corre la app.
    private static final String URL = "http://localhost:8081/ping";

    // El filtro bloquea al superar 20 peticiones en 1 segundo: 30 bastan para cruzar el umbral.
    private static final int NUMERO_PETICIONES = 30;

    public static void main(String[] args) throws Exception {
        System.out.println("Bot de prueba del bloqueo por IP");
        System.out.println("Objetivo: " + URL);
        System.out.println("Enviando " + NUMERO_PETICIONES + " peticiones a la vez...\n");

        HttpClient cliente = HttpClient.newHttpClient();
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        AtomicInteger ok = new AtomicInteger();          // 200: la petición pasó
        AtomicInteger bloqueadas = new AtomicInteger();   // 429: el filtro la rechazó
        AtomicInteger errores = new AtomicInteger();      // la app no respondió (¿está arrancada?)

        // Un hilo por petición para que salgan casi a la vez y entren dentro de la misma ventana de 1 segundo
        ExecutorService hilos = Executors.newFixedThreadPool(NUMERO_PETICIONES);
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < NUMERO_PETICIONES; i++) {
            tareas.add(hilos.submit(() -> {
                try {
                    HttpResponse<Void> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.discarding());
                    if (respuesta.statusCode() == 429) {
                        bloqueadas.incrementAndGet();
                    } else if (respuesta.statusCode() == 200) {
                        ok.incrementAndGet();
                    } else {
                        bloqueadas.incrementAndGet(); // otros códigos (p. ej. 500) se cuentan como rechazo
                    }
                } catch (Exception e) {
                    errores.incrementAndGet();
                }
            }));
        }

        for (Future<?> tarea : tareas) {
            tarea.get();
        }
        hilos.shutdown();

        System.out.println("Resultado de la ráfaga:");
        System.out.println("  Peticiones aceptadas (200):   " + ok.get());
        System.out.println("  Peticiones bloqueadas (429):  " + bloqueadas.get());
        if (errores.get() > 0) {
            System.out.println("  Sin respuesta:                " + errores.get()
                    + "  (¿está arrancada la aplicación en " + URL + "?)");
        }

        System.out.println();
        if (bloqueadas.get() > 0) {
            System.out.println("El middleware bloqueó la IP: el filtro funciona.");
            System.out.println("Esa IP queda bloqueada 5 minutos. Compruébalo en /log y en log_transacciones.json.");
        } else if (errores.get() == NUMERO_PETICIONES) {
            System.out.println("No hubo respuesta. Arranca Middleware_ip antes de ejecutar el bot.");
        } else {
            System.out.println("No se produjo bloqueo. Quizá el umbral del filtro es mayor o las peticiones");
            System.out.println("no entraron en la misma ventana de 1 segundo. Prueba a ejecutarlo de nuevo.");
        }
    }
}
