-- Datos de ejemplo para la tabla `coche`
-- Ejecuta primero coche.sql. Luego pega este script en la pestaña SQL de phpMyAdmin.

USE coche_db;

INSERT INTO coche (marca, modelo, matricula, anio, color, precio, kilometraje, combustible, transmision) VALUES
('Toyota', 'Corolla', '1234ABC', 2020, 'Blanco', 18500.00, 45000, 'Gasolina', 'Automática'),
('Volkswagen', 'Golf', '5678BCD', 2019, 'Gris', 15200.00, 62000, 'Diésel', 'Manual'),
('Tesla', 'Model 3', '9012CDE', 2022, 'Blanco', 39900.00, 12000, 'Eléctrico', 'Automática'),
('Seat', 'Ibiza', '3456DEF', 2018, 'Rojo', 9800.00, 88000, 'Gasolina', 'Manual'),
('Renault', 'Clio', '7890EFG', 2021, 'Azul', 13500.00, 30000, 'Gasolina', 'Manual'),
('BMW', 'Serie 3', '2345FGH', 2020, 'Negro', 27500.00, 40000, 'Diésel', 'Automática'),
('Toyota', 'Prius', '6789GHI', 2019, 'Plata', 21000.00, 35000, 'Híbrido', 'Automática'),
('Ford', 'Focus', '1122HIJ', 2017, 'Blanco', 8700.00, 95000, 'Gasolina', 'Manual'),
('Audi', 'A4', '3344IJK', 2021, 'Gris', 32000.00, 20000, 'Diésel', 'Automática'),
('Hyundai', 'Kona', '5566JKL', 2022, 'Verde', 24500.00, 8000, 'Eléctrico', 'Automática');
