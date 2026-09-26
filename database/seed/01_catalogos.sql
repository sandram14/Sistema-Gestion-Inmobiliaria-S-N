-- =====================================================================
-- S&N Inmobiliaria - Carga inicial 01: Catálogos
-- =====================================================================

INSERT INTO rol (nombre, descripcion) VALUES
    ('ADMIN', 'Administrador de la inmobiliaria: gestiona propiedades, imágenes, operaciones y estados');

INSERT INTO tipo_propiedad (nombre) VALUES
    ('Casa'),
    ('Terreno');

INSERT INTO estado_propiedad (nombre) VALUES
    ('Disponible'),
    ('Reservado'),
    ('Vendido'),
    ('Alquilado'),
    ('Inactivo');

INSERT INTO operacion (nombre) VALUES
    ('Venta'),
    ('Alquiler');
