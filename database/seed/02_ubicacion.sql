-- =====================================================================
-- S&N Inmobiliaria - Carga inicial 02: Ubicación
-- =====================================================================
-- Departamento, ciudades (distritos) y barrios/localidades de Alto Paraná desde la cartografía censal
-- 2022 del INE (database/datos/ine2022_alto_parana.sql, generado por database/datos/generar_ine2022.mjs).
--   ciudad: nombre y código de distrito del INE; 'centro' = punto de la capa de ciudades (6 decimales)
--   barrio: id = CLAVE_BAR oficial, nombre tal como figura en el INE, polígono MultiPolygon
-- Es la misma carga que hace la migración 010 en una base existente: una base nueva nace igual.
-- Antes de la 010 se cargaban solo 12 barrios (los de las propiedades de ejemplo), con 5 decimales.
-- =====================================================================

INSERT INTO departamento (nombre, codigo) VALUES ('ALTO PARANÁ', '10');

CREATE TEMP TABLE ine_distrito (distrito char(2) PRIMARY KEY, nombre text NOT NULL, longitud float8 NOT NULL, latitud float8 NOT NULL);
CREATE TEMP TABLE ine_barrio (clave integer PRIMARY KEY, nombre text NOT NULL, distrito char(2) NOT NULL, area char(1) NOT NULL, geojson text NOT NULL);
\ir ../datos/ine2022_alto_parana.sql

INSERT INTO ciudad (nombre, codigo_distrito, id_departamento, centro)
SELECT i.nombre, i.distrito, d.id_departamento, ST_SetSRID(ST_MakePoint(i.longitud, i.latitud), 4326)
  FROM ine_distrito i
 CROSS JOIN departamento d
 WHERE d.codigo = '10'
 -- Primero las 4 ciudades de antes de la 010 (mismos id_ciudad que en una base migrada), luego las demás
 ORDER BY CASE WHEN i.distrito IN ('01', '02', '05', '11') THEN 0 ELSE 1 END, i.distrito;

INSERT INTO barrio (id_barrio, nombre, id_ciudad, geom)
SELECT i.clave, i.nombre, c.id_ciudad, ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(i.geojson), 4326))
  FROM ine_barrio i
  JOIN ciudad c       ON c.codigo_distrito = i.distrito
  JOIN departamento d ON d.id_departamento = c.id_departamento AND d.codigo = '10'
 ORDER BY i.clave;

DROP TABLE ine_barrio;
DROP TABLE ine_distrito;
