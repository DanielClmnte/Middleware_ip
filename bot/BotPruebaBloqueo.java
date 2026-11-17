import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
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
 * Tu ordenador tiene tres direcciones con las que puede entrar a la web, y el filtro ve cada
 * una como una IP distinta: 127.0.0.1 (IPv4), ::1 (IPv6, la que suelen usar los navegadores
 * con "localhost") y la IP de tu red wifi (192.168.x.x, se detecta sola). La ráfaga se lanza
 * contra las tres: así el navegador sale bloqueado y se ve que el bloqueo es POR IP.
 *
 * NO es una herramienta de ataque: apunta fijo a localhost, hace una única ráfaga y termina.
 *
 * Uso (no necesita Maven):
 *   java BotPruebaBloqueo.java              -> ataca /ping
 *   java BotPruebaBloqueo.java /coches      -> ataca /coches
 *   java BotPruebaBloqueo.java /            -> ataca la página de inicio
 */
public class BotPruebaBloqueo {

    // Puerto de la aplicación
    private static final int PUERTO = 8081;

    // Endpoint que se ataca si no se pasa ninguno como argumento
    private static final String ENDPOINT_POR_DEFECTO = "/ping";

    // El filtro bloquea al superar 20 peticiones en 1 segundo: 30 bastan para cruzar el umbral.
    private static final int NUMERO_PETICIONES = 30;

    public static void main(String[] args) throws Exception {
        String endpoint = args.length > 0 ? args[0] : ENDPOINT_POR_DEFECTO;
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;   // acepta "coches" igual que "/coches"
        }

        System.out.println("Bot de prueba del bloqueo por IP");
        System.out.println("Endpoint: " + endpoint);

        HttpClient cliente = HttpClient.newHttpClient();
        boolean algunBloqueo = false;
        boolean algunaRespuesta = false;

        for (String base : bases()) {
            int[] resultado = rafaga(cliente, base + endpoint);
            algunBloqueo |= resultado[1] > 0;
            algunaRespuesta |= resultado[2] < NUMERO_PETICIONES;
        }

        System.out.println();
        if (algunBloqueo) {
            System.out.println("El middleware bloqueó la IP: el filtro funciona.");
            System.out.println("Queda bloqueada 5 minutos. Abre http://localhost:8081 en el navegador para verlo.");
            System.out.println("Los bloqueos quedan registrados en ips.txt y historial.json.");
        } else if (!algunaRespuesta) {
            System.out.println("No hubo respuesta. Arranca Middleware_ip antes de ejecutar el bot.");
        } else {
            System.out.println("No se produjo bloqueo. Quizá el umbral del filtro es mayor o las peticiones");
            System.out.println("no entraron en la misma ventana de 1 segundo. Prueba a ejecutarlo de nuevo.");
        }
    }

    // Las direcciones de la propia máquina. El filtro ve cada una como una IP distinta.
    private static List<String> bases() {
        List<String> bases = new ArrayList<>();
        bases.add("http://127.0.0.1:" + PUERTO);   // IPv4
        bases.add("http://[::1]:" + PUERTO);       // IPv6 (la que suelen usar los navegadores con "localhost")
        String red = ipDeRed();
        if (red != null) {
            bases.add("http://" + red + ":" + PUERTO);   // IP de tu red wifi: una tercera IP distinta
        }
        return bases;
    }

    // Busca la IP de tu ordenador en la red (192.168.x.x, 10.x.x.x...). Devuelve null si no tiene.
    private static String ipDeRed() {
        try {
            for (NetworkInterface tarjeta : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!tarjeta.isUp() || tarjeta.isLoopback()) continue;
                for (InetAddress direccion : Collections.list(tarjeta.getInetAddresses())) {
                    if (direccion instanceof Inet4Address && direccion.isSiteLocalAddress()) {
                        return direccion.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // si falla, simplemente no se usa la IP de red
        }
        return null;
    }

    // Lanza NUMERO_PETICIONES a la vez contra url. Devuelve {aceptadas, bloqueadas, sinRespuesta}.
    private static int[] rafaga(HttpClient cliente, String url) throws Exception {
        System.out.println("\nObjetivo: " + url);
        System.out.println("Enviando " + NUMERO_PETICIONES + " peticiones a la vez...");

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(url))
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

        System.out.println("  Peticiones aceptadas (200):   " + ok.get());
        System.out.println("  Peticiones bloqueadas (429):  " + bloqueadas.get());
        if (errores.get() > 0) {
            System.out.println("  Sin respuesta:                " + errores.get()
                    + "  (¿está arrancada la aplicación?)");
        }
        return new int[]{ok.get(), bloqueadas.get(), errores.get()};
    }
}
