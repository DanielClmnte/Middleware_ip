# Bot de prueba del bloqueo por IP

Herramienta para **probar el `IpFilter` de esta misma web** en local. No es parte de la aplicación
(por eso está en `bot/` y no en `src/`): es un cliente HTTP aparte que no necesita Maven.

## Qué hace

Lanza 30 peticiones a la vez (el límite del filtro es 20 por segundo) contra la web, que debe estar
arrancada en el puerto 8081. Lo repite contra las tres direcciones de tu ordenador, y el filtro ve
cada una como una IP distinta:

| Dirección | IP que ve el filtro |
|---|---|
| `http://127.0.0.1:8081` | `127.0.0.1` (IPv4) |
| `http://[::1]:8081` | `0:0:0:0:0:0:0:1` (IPv6, la del navegador con `localhost`) |
| `http://TU_IP_DE_RED:8081` | tu IP del wifi (se detecta sola) |

## Cómo usarlo

1. Arranca la web (`./mvnw spring-boot:run`, con JDK 21).
2. En otra terminal, desde la raíz del proyecto:

```bash
java bot/BotPruebaBloqueo.java              # ataca /ping
java bot/BotPruebaBloqueo.java /coches      # ataca /coches
```

## Qué verás

Por cada dirección: unas 20 peticiones aceptadas (200) y 10 bloqueadas (429). Después abre
`http://localhost:8081` en el navegador: saldrá la página de bloqueo durante 5 minutos. Los bloqueos
quedan anotados en `ips.txt` y en `transacciones.json`.

Para desbloquearte antes de tiempo, reinicia la web.
