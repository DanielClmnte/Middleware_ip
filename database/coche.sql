-- Script de creación de la base de datos y la tabla `coche`
-- Ejecuta este script completo en la pestaña SQL de phpMyAdmin (no hace falta seleccionar antes ninguna base de datos).

CREATE DATABASE IF NOT EXISTS coche_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE coche_db;

CREATE TABLE IF NOT EXISTS coche (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(50) NOT NULL,
    matricula VARCHAR(10) NOT NULL UNIQUE,
    anio INT NOT NULL,
    color VARCHAR(30) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    kilometraje INT NOT NULL,
    combustible VARCHAR(20) NOT NULL,
    transmision VARCHAR(20) NOT NULL
);
