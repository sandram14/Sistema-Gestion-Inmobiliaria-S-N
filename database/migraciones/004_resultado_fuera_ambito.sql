-- =====================================================================
-- Migración 004: resultado FUERA_DE_AMBITO para las consultas del chat IA
-- =====================================================================
-- Amplía la restricción de consulta.resultado con 'FUERA_DE_AMBITO' (el asistente lo registra
-- desde ahora cuando la pregunta no trata sobre propiedades ni sobre la inmobiliaria).
-- No modifica ningún dato: las consultas existentes conservan su resultado (o su ausencia).
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/004_resultado_fuera_ambito.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE antes ON COMMIT DROP AS SELECT id_consulta, resultado FROM consulta;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_consulta_resultado') THEN
        RAISE EXCEPTION 'No existe ck_consulta_resultado: falta la migración 003. Nada fue modificado.';
    END IF;
    IF (SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_consulta_resultado') LIKE '%FUERA_DE_AMBITO%' THEN
        RAISE EXCEPTION 'La migración 004 ya está aplicada. Nada fue modificado.';
    END IF;
END $$;

ALTER TABLE consulta DROP CONSTRAINT ck_consulta_resultado;
ALTER TABLE consulta ADD CONSTRAINT ck_consulta_resultado
    CHECK (resultado IN ('RESPONDIDA', 'DERIVADA_ASESOR', 'SIN_SERVICIO', 'BLOQUEADA', 'FUERA_DE_AMBITO'));

DO $$
BEGIN
    IF (SELECT count(*) FROM consulta) <> (SELECT count(*) FROM antes)
       OR EXISTS (SELECT 1 FROM antes a JOIN consulta c USING (id_consulta) WHERE c.resultado IS DISTINCT FROM a.resultado) THEN
        RAISE EXCEPTION 'Las consultas cambiaron. Se deshace todo.';
    END IF;
    RAISE NOTICE 'OK: migración 004 aplicada. % consultas sin cambios.', (SELECT count(*) FROM consulta);
END $$;
COMMIT;
