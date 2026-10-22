-- =====================================================================
-- Reversión de la migración 003. Quita la tabla consulta_recomendacion y las columnas
-- resultado, estado_revision, revisada_por y revisada_en. Las consultas (pregunta,
-- respuesta, propiedad, sesión y fecha) no se tocan.
-- AVISO: se pierden las recomendaciones, los resultados y las revisiones registrados desde la migración.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/003_revertir_consultas.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
DROP TABLE IF EXISTS consulta_recomendacion;
DROP INDEX IF EXISTS ix_consulta_revision;
ALTER TABLE consulta DROP COLUMN IF EXISTS resultado, DROP COLUMN IF EXISTS estado_revision,
                     DROP COLUMN IF EXISTS revisada_por, DROP COLUMN IF EXISTS revisada_en;
COMMIT;
SELECT count(*) AS consultas FROM consulta;
