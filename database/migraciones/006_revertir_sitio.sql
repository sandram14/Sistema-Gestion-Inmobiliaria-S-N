-- Reversión de la migración 006: elimina el contenido editable del sitio y la selección de destacadas.
-- AVISO: se pierden los textos editados desde el panel (el sitio vuelve a los textos escritos en su código).
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/006_revertir_sitio.sql
\set ON_ERROR_STOP on
BEGIN;
DROP TABLE IF EXISTS propiedad_destacada;
DROP TABLE IF EXISTS seccion_sitio;
COMMIT;
