-- Reversión de la migración 005. Se detiene si la tabla tiene registros (son datos personales reales:
-- deben exportarse o eliminarse a conciencia, no como efecto de una reversión).
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/005_revertir_clientes.sql
\set ON_ERROR_STOP on
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cliente_potencial) THEN
        RAISE EXCEPTION 'cliente_potencial tiene registros. Nada fue modificado.';
    END IF;
END $$;
DROP TABLE cliente_potencial;
COMMIT;
