-- =====================================================================
-- Verificación previa de la migración 010 (SOLO LECTURA: todo se deshace al final)
-- =====================================================================
-- Muestra qué hay hoy (ciudades, barrios, propiedades), qué agregaría la 010 desde la cartografía del INE
-- 2022 y si alguna propiedad quedaría fuera de su barrio con el contorno oficial. Guardar la salida.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/010_verificar_barrios_ine.sql
-- =====================================================================
BEGIN;
CREATE TEMP TABLE ine_distrito (distrito char(2) PRIMARY KEY, nombre text NOT NULL, longitud float8 NOT NULL, latitud float8 NOT NULL) ON COMMIT DROP;
CREATE TEMP TABLE ine_barrio (clave integer PRIMARY KEY, nombre text NOT NULL, distrito char(2) NOT NULL, area char(1) NOT NULL, geojson text NOT NULL) ON COMMIT DROP;
\ir ../datos/ine2022_alto_parana.sql

\echo '== Hoy: ciudades y barrios por ciudad'
SELECT c.codigo_distrito AS distrito, c.nombre AS ciudad, count(b.id_barrio) AS barrios
  FROM ciudad c LEFT JOIN barrio b USING (id_ciudad) GROUP BY 1, 2 ORDER BY 1;

\echo '== ¿Ya está aplicada? (más de 4 ciudades o barrios que no son de antes)'
SELECT (SELECT count(*) FROM ciudad) AS ciudades, (SELECT count(*) FROM barrio) AS barrios,
       (SELECT count(*) FROM ciudad) > 4 AS parece_aplicada;

\echo '== Barrios actuales frente al INE (mismo código, nombre y distrito; diferencia del contorno en metros)'
SELECT b.id_barrio, b.nombre, i.nombre = b.nombre AS mismo_nombre, i.distrito = c.codigo_distrito AS mismo_distrito,
       round((ST_HausdorffDistance(b.geom, ST_SetSRID(ST_GeomFromGeoJSON(i.geojson), 4326)) * 111000)::numeric, 2) AS diferencia_m
  FROM barrio b JOIN ciudad c USING (id_ciudad) LEFT JOIN ine_barrio i ON i.clave = b.id_barrio ORDER BY 1;

\echo '== Lo que agregaría: ciudades nuevas y barrios nuevos por ciudad (área 1 urbano, 3 urbano en zona rural, 6 rural)'
SELECT i.distrito, i.nombre AS ciudad, EXISTS (SELECT 1 FROM ciudad c WHERE c.codigo_distrito = i.distrito) AS ya_existe,
       count(b.clave) FILTER (WHERE NOT EXISTS (SELECT 1 FROM barrio x WHERE x.id_barrio = b.clave)) AS barrios_nuevos,
       count(b.clave) FILTER (WHERE b.area = '1') AS urbanos, count(b.clave) FILTER (WHERE b.area = '3') AS urbanos_en_zona_rural,
       count(b.clave) FILTER (WHERE b.area = '6') AS rurales
  FROM ine_distrito i LEFT JOIN ine_barrio b ON b.distrito = i.distrito GROUP BY 1, 2 ORDER BY 1;

\echo '== Propiedades: ¿quedan dentro de su barrio con el contorno del INE? (la 010 se detiene si alguna no)'
SELECT p.codigo, b.nombre AS barrio,
       ST_Covers(ST_SetSRID(ST_GeomFromGeoJSON(i.geojson), 4326), p.ubicacion) AS dentro_con_ine,
       round(ST_Distance(ST_Boundary(ST_SetSRID(ST_GeomFromGeoJSON(i.geojson), 4326))::geography, p.ubicacion::geography)::numeric, 1) AS metros_al_borde
  FROM propiedad p JOIN barrio b USING (id_barrio) LEFT JOIN ine_barrio i ON i.clave = p.id_barrio
 ORDER BY dentro_con_ine, metros_al_borde, p.codigo;

\echo '== Contornos del INE inválidos (deben ser 0)'
SELECT count(*) AS invalidos FROM ine_barrio WHERE NOT ST_IsValid(ST_GeomFromGeoJSON(geojson));
ROLLBACK;
