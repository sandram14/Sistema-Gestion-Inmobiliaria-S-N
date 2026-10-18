-- =====================================================================
-- Migración 010: barrios y localidades oficiales de todo Alto Paraná (cartografía censal 2022 del INE)
-- =====================================================================
-- Antes había 4 ciudades y 12 barrios (los de las propiedades de ejemplo), y una propiedad fuera de ellos
-- no se podía cargar. Esta migración:
--   1. agrega las 18 ciudades (distritos) que faltan de Alto Paraná;
--   2. agrega los 702 barrios y localidades que faltan (urbanos y rurales), con su código oficial CLAVE_BAR;
--   3. pasa los 12 barrios de antes al contorno exacto del INE (estaban con 5 decimales: se mueven menos
--      de 1 m), así no quedan franjas superpuestas con sus vecinos;
--   4. quita la regla de nombre único por ciudad (el INE repite nombres entre la parte urbana y la rural).
-- No cambia ninguna propiedad: comprueba que cada una siga dentro de su barrio y se detiene si alguna no.
-- Hacer un respaldo antes (pg_dump) y guardar la salida de 010_verificar_barrios_ine.sql.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/010_barrios_ine.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE ine_distrito (distrito char(2) PRIMARY KEY, nombre text NOT NULL, longitud float8 NOT NULL, latitud float8 NOT NULL) ON COMMIT DROP;
CREATE TEMP TABLE ine_barrio (clave integer PRIMARY KEY, nombre text NOT NULL, distrito char(2) NOT NULL, area char(1) NOT NULL, geojson text NOT NULL) ON COMMIT DROP;
\ir ../datos/ine2022_alto_parana.sql

CREATE TEMP TABLE antes_propiedad ON COMMIT DROP AS SELECT id_propiedad, id_barrio, latitud, longitud FROM propiedad;
CREATE TEMP TABLE antes_ciudad ON COMMIT DROP AS SELECT id_ciudad, nombre, codigo_distrito, id_departamento, ST_AsBinary(centro) AS centro FROM ciudad;
CREATE TEMP TABLE antes_barrio ON COMMIT DROP AS SELECT id_barrio, nombre, id_ciudad FROM barrio;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM departamento WHERE codigo = '10') THEN
        RAISE EXCEPTION 'No existe el departamento Alto Paraná (10). Nada fue modificado.';
    END IF;
    IF (SELECT count(*) FROM ine_distrito) <> 22 OR (SELECT count(*) FROM ine_barrio) <> 714 THEN
        RAISE EXCEPTION 'Los datos del INE no son los esperados (22 distritos y 714 barrios). Nada fue modificado.';
    END IF;
    IF (SELECT count(*) FROM ciudad) > 4 OR EXISTS (SELECT 1 FROM barrio WHERE id_barrio NOT IN (SELECT clave FROM ine_barrio))
       OR (SELECT count(*) FROM barrio) > 12 THEN
        RAISE EXCEPTION 'La migración 010 ya está aplicada (o la base tiene ciudades o barrios que no son los de antes). Nada fue modificado.';
    END IF;
    -- Cada ciudad y barrio actual debe estar en el INE con el mismo nombre y distrito
    IF EXISTS (SELECT 1 FROM ciudad c LEFT JOIN ine_distrito i ON i.distrito = c.codigo_distrito
                WHERE i.distrito IS NULL OR i.nombre <> c.nombre) THEN
        RAISE EXCEPTION 'Hay ciudades que no coinciden con el INE (código de distrito o nombre). Nada fue modificado.';
    END IF;
    IF EXISTS (SELECT 1 FROM barrio b JOIN ciudad c USING (id_ciudad) JOIN ine_barrio i ON i.clave = b.id_barrio
                WHERE i.nombre <> b.nombre OR i.distrito <> c.codigo_distrito) THEN
        RAISE EXCEPTION 'Hay barrios que no coinciden con el INE (nombre o ciudad). Nada fue modificado.';
    END IF;
    -- Ninguna propiedad puede quedar fuera de su barrio con el contorno oficial
    IF EXISTS (SELECT 1 FROM propiedad p JOIN ine_barrio i ON i.clave = p.id_barrio
                WHERE NOT ST_Covers(ST_SetSRID(ST_GeomFromGeoJSON(i.geojson), 4326), p.ubicacion)) THEN
        RAISE EXCEPTION 'Alguna propiedad quedaría fuera de su barrio con el contorno del INE (ver 010_verificar). Nada fue modificado.';
    END IF;
    IF EXISTS (SELECT 1 FROM ine_barrio WHERE NOT ST_IsValid(ST_GeomFromGeoJSON(geojson))) THEN
        RAISE EXCEPTION 'Hay contornos del INE inválidos. Nada fue modificado.';
    END IF;
    RAISE NOTICE 'Antes: % ciudades, % barrios, % propiedades.',
        (SELECT count(*) FROM ciudad), (SELECT count(*) FROM barrio), (SELECT count(*) FROM propiedad);
END $$;

-- 1. Sin nombre único por ciudad
ALTER TABLE barrio DROP CONSTRAINT IF EXISTS uq_barrio_nombre;

-- 2. Ciudades que faltan (en orden de código de distrito, como en una base nueva)
INSERT INTO ciudad (nombre, codigo_distrito, id_departamento, centro)
SELECT i.nombre, i.distrito, d.id_departamento, ST_SetSRID(ST_MakePoint(i.longitud, i.latitud), 4326)
  FROM ine_distrito i
 CROSS JOIN departamento d
 WHERE d.codigo = '10'
   AND NOT EXISTS (SELECT 1 FROM ciudad c WHERE c.id_departamento = d.id_departamento AND c.codigo_distrito = i.distrito)
 ORDER BY i.distrito;

-- 3. Los barrios de antes, al contorno exacto del INE (el trigger vuelve a comprobar cada propiedad)
UPDATE barrio b
   SET geom = ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(i.geojson), 4326))
  FROM ine_barrio i
 WHERE i.clave = b.id_barrio;

-- 4. Barrios y localidades que faltan
INSERT INTO barrio (id_barrio, nombre, id_ciudad, geom)
SELECT i.clave, i.nombre, c.id_ciudad, ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(i.geojson), 4326))
  FROM ine_barrio i
  JOIN ciudad c       ON c.codigo_distrito = i.distrito
  JOIN departamento d ON d.id_departamento = c.id_departamento AND d.codigo = '10'
 WHERE NOT EXISTS (SELECT 1 FROM barrio b WHERE b.id_barrio = i.clave)
 ORDER BY i.clave;

DO $$
BEGIN
    IF (SELECT count(*) FROM ciudad) <> 22 OR (SELECT count(*) FROM barrio) <> 714 THEN
        RAISE EXCEPTION 'No quedaron 22 ciudades y 714 barrios. Se deshace todo.';
    END IF;
    IF EXISTS (SELECT 1 FROM barrio WHERE NOT ST_IsValid(geom)) THEN
        RAISE EXCEPTION 'Quedó algún contorno inválido. Se deshace todo.';
    END IF;
    -- Las ciudades y barrios de antes conservan id, nombre, ciudad y centro
    IF EXISTS (SELECT 1 FROM antes_ciudad a LEFT JOIN ciudad c USING (id_ciudad)
                WHERE c.id_ciudad IS NULL OR c.nombre <> a.nombre OR c.codigo_distrito <> a.codigo_distrito
                   OR ST_AsBinary(c.centro) <> a.centro) THEN
        RAISE EXCEPTION 'Cambió alguna ciudad de antes. Se deshace todo.';
    END IF;
    IF EXISTS (SELECT 1 FROM antes_barrio a LEFT JOIN barrio b USING (id_barrio)
                WHERE b.id_barrio IS NULL OR b.nombre <> a.nombre OR b.id_ciudad <> a.id_ciudad) THEN
        RAISE EXCEPTION 'Cambió algún barrio de antes (nombre o ciudad). Se deshace todo.';
    END IF;
    -- Las propiedades no cambian y siguen dentro de su barrio
    IF (SELECT count(*) FROM propiedad) <> (SELECT count(*) FROM antes_propiedad)
       OR EXISTS (SELECT 1 FROM antes_propiedad a LEFT JOIN propiedad p USING (id_propiedad)
                   WHERE p.id_propiedad IS NULL OR p.id_barrio <> a.id_barrio OR p.latitud <> a.latitud OR p.longitud <> a.longitud)
       OR EXISTS (SELECT 1 FROM propiedad p JOIN barrio b USING (id_barrio) WHERE NOT ST_Covers(b.geom, p.ubicacion)) THEN
        RAISE EXCEPTION 'Cambió alguna propiedad o alguna quedó fuera de su barrio. Se deshace todo.';
    END IF;
    RAISE NOTICE 'OK: migración 010 aplicada. 22 ciudades y 714 barrios de Alto Paraná (INE 2022); % propiedades sin cambios, todas dentro de su barrio.',
        (SELECT count(*) FROM propiedad);
END $$;
COMMIT;
