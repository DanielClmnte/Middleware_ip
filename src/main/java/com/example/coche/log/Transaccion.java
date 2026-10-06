package com.example.coche.log;

/**
 * Un movimiento de la web para el historial transacciones.json.
 * Es un record: una clase corta de solo datos. Jackson lo convierte a/desde JSON.
 */
public record Transaccion(
        String fecha,
        String ip,
        String accion,     // CREAR, EDITAR, ELIMINAR
        Long cocheId,
        String detalle,
        String resultado   // OK, ERROR
) {
}
