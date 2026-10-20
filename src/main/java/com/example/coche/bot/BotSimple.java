package com.example.coche.bot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Bot atacante SENCILLO para probar tu propia web.
 *
 * Manda muchas peticiones seguidas a la web de coches para comprobar que el filtro
 * de seguridad bloquea la IP cuando hay demasiadas (responde 429).
 *
 * Como usarlo: arranca la web (CocheApplication) y luego ejecuta este main (boton ▶).
 */
public class BotSimple {

    public static void main(String[] args) throws Exception {
        String direccion = "http://127.0.0.1:8081/ping";   // la web que vamos a atacar (tu propia IP)
        int totalPeticiones = 30;                           // cuantas peticiones enviamos

        // El "cliente" es quien envia las peticiones por internet.
        HttpClient cliente = HttpClient.newHttpClient();
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(direccion)).build();

        int correctas = 0;    // cuantas responden 200 (OK)
        int bloqueadas = 0;   // cuantas responden 429 (bloqueadas)

        // Enviamos las peticiones una tras otra dentro de un bucle.
        for (int i = 1; i <= totalPeticiones; i++) {
            int codigo = cliente.send(peticion, HttpResponse.BodyHandlers.discarding()).statusCode();

            if (codigo == 429) {
                bloqueadas++;
            } else {
                correctas++;
            }

            System.out.println("Peticion " + i + " -> codigo " + codigo);
        }

        // Resumen final.
        System.out.println("-----------------------------");
        System.out.println("Correctas (200):  " + correctas);
        System.out.println("Bloqueadas (429): " + bloqueadas);
        if (bloqueadas > 0) {
            System.out.println("El filtro funciona: ha bloqueado la IP.");
        }
    }
}
