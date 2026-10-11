-- =====================================================================
-- S&N Inmobiliaria - Carga inicial 01: Catálogos
-- =====================================================================

INSERT INTO rol (nombre, descripcion) VALUES
    ('ADMIN', 'Administrador de la inmobiliaria: gestiona propiedades, imágenes, operaciones y estados'),
    ('GERENTE',  'Gerente: gestiona todas las propiedades, ofertas, contenido y estadísticas; consulta usuarios'),
    ('VENDEDOR', 'Vendedor: ve todas las propiedades, crea y edita las asignadas a él y ve sus consultas');

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
