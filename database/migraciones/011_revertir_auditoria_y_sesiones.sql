-- Reversión de la migración 011. Se detiene si la auditoría ya tiene registros (son un historial: no se borran
-- automáticamente) o si la migración no está aplicada. Antes de revertir, volver al backend 0.23.0.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/011_revertir_auditoria_y_sesiones.sql
\set ON_ERROR_STOP on
BEGIN;
DO $$
BEGIN
    IF to_regclass('public.auditoria') IS NULL
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'usuario' AND column_name = 'version_credencial') THEN
        RAISE EXCEPTION 'La migración 011 no está aplicada. Nada fue modificado.';
    END IF;
    IF EXISTS (SELECT 1 FROM auditoria) THEN
        RAISE EXCEPTION 'La auditoría tiene % registros. Nada fue modificado.', (SELECT count(*) FROM auditoria);
    END IF;
END $$;
DROP TABLE auditoria;
ALTER TABLE usuario DROP COLUMN version_credencial;
COMMIT;
