-- =====================================================================
-- Reversión de la migración 010: vuelve a las 4 ciudades y 12 barrios de antes
-- =====================================================================
-- Borra las 18 ciudades y los 702 barrios agregados y restaura la regla de nombre único por ciudad.
-- Se DETIENE si alguna propiedad ya está en un barrio agregado por la 010 (habría que moverla o
-- borrarla antes). Los 12 barrios de antes quedan con el contorno del INE (difiere menos de 1 m del
-- anterior); para volver exactamente al estado previo, restaurar el respaldo hecho antes de migrar.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/010_revertir_barrios_ine.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE barrios_de_antes (id_barrio integer PRIMARY KEY) ON COMMIT DROP;
INSERT INTO barrios_de_antes VALUES
    (10011003), (10011013), (10011014), (10011024), (10021001), (10021010),
    (10051007), (10051023), (10111004), (10111015), (10111016), (10116200);
CREATE TEMP TABLE antes_propiedad ON COMMIT DROP AS SELECT id_propiedad, id_barrio, latitud, longitud FROM propiedad;

DO $$
BEGIN
    IF (SELECT count(*) FROM ciudad) <= 4 AND (SELECT count(*) FROM barrio) <= 12 THEN
        RAISE EXCEPTION 'La migración 010 no está aplicada. Nada fue modificado.';
    END IF;
    IF EXISTS (SELECT 1 FROM propiedad WHERE id_barrio NOT IN (SELECT id_barrio FROM barrios_de_antes)) THEN
        RAISE EXCEPTION 'Hay % propiedad(es) en barrios agregados por la 010: moverlas o borrarlas antes de revertir. Nada fue modificado.',
            (SELECT count(*) FROM propiedad WHERE id_barrio NOT IN (SELECT id_barrio FROM barrios_de_antes));
    END IF;
END $$;

DELETE FROM barrio WHERE id_barrio NOT IN (SELECT id_barrio FROM barrios_de_antes);
DELETE FROM ciudad c
 USING departamento d
 WHERE d.id_departamento = c.id_departamento AND d.codigo = '10' AND c.codigo_distrito NOT IN ('01', '02', '05', '11');
ALTER TABLE barrio ADD CONSTRAINT uq_barrio_nombre UNIQUE (id_ciudad, nombre);

DO $$
BEGIN
    IF (SELECT count(*) FROM ciudad) <> 4 OR (SELECT count(*) FROM barrio) <> 12 THEN
        RAISE EXCEPTION 'No quedaron 4 ciudades y 12 barrios. Se deshace todo.';
    END IF;
    IF (SELECT count(*) FROM propiedad) <> (SELECT count(*) FROM antes_propiedad)
       OR EXISTS (SELECT 1 FROM antes_propiedad a LEFT JOIN propiedad p USING (id_propiedad)
                   WHERE p.id_propiedad IS NULL OR p.id_barrio <> a.id_barrio OR p.latitud <> a.latitud OR p.longitud <> a.longitud) THEN
        RAISE EXCEPTION 'Cambió alguna propiedad. Se deshace todo.';
    END IF;
    RAISE NOTICE 'OK: migración 010 revertida. 4 ciudades y 12 barrios (con el contorno del INE).';
END $$;
COMMIT;
