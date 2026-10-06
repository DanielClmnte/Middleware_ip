-- Tabla de auditoría: una fila por cada alta, edición o borrado de un coche.
-- Ejecuta primero coche.sql. Luego pega este script en la pestaña SQL de phpMyAdmin.

USE coche_db;

-- coche_id NO es clave foránea a propósito: el historial debe conservarse aunque el coche se borre.
-- ENGINE = InnoDB: es el motor que permite commit y rollback.
CREATE TABLE IF NOT EXISTS log_transaccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(45) NOT NULL,
    accion VARCHAR(10) NOT NULL,
    coche_id BIGINT NULL,
    detalle VARCHAR(255) NULL,
    resultado VARCHAR(5) NOT NULL
) ENGINE = InnoDB;
