-- Reversión de la migración 004. Se detiene si alguna consulta ya tiene resultado FUERA_DE_AMBITO
-- (habría que decidir qué hacer con ellas: no se modifican automáticamente).
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/004_revertir_fuera_ambito.sql
\set ON_ERROR_STOP on
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM consulta WHERE resultado = 'FUERA_DE_AMBITO') THEN
        RAISE EXCEPTION 'Hay consultas con resultado FUERA_DE_AMBITO. Nada fue modificado.';
    END IF;
END $$;
ALTER TABLE consulta DROP CONSTRAINT ck_consulta_resultado;
ALTER TABLE consulta ADD CONSTRAINT ck_consulta_resultado
    CHECK (resultado IN ('RESPONDIDA', 'DERIVADA_ASESOR', 'SIN_SERVICIO', 'BLOQUEADA'));
COMMIT;
