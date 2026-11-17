package com.example.coche.model;

/**
 * Un movimiento de la web: cada entrada de historial.json (alta, edicion, borrado o bloqueo de IP).
 * Es un record: una clase corta de solo datos. Jackson lo convierte a/desde JSON.
 */
public record Movimiento(
        String fecha,
        String ip,
        String accion,     // CREAR, EDITAR, ELIMINAR, BLOQUEO, DESBLOQUEO
        Long cocheId,
        String detalle,
        String resultado   // OK, ERROR, BLOQUEADA
) {
}
