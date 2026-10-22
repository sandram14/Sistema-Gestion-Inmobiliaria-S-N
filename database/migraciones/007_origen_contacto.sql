-- =====================================================================
-- Migración 007: origen CONTACTO para clientes potenciales (formulario de la página Contacto)
-- =====================================================================
-- Amplía ck_cliente_origen con 'CONTACTO' y exige la aceptación del tratamiento también para ese origen
-- (como en el chat: el propio visitante deja sus datos). No modifica ningún registro.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/007_origen_contacto.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE antes ON COMMIT DROP AS SELECT id_cliente_potencial, origen, consentimiento_en FROM cliente_potencial;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cliente_origen') THEN
        RAISE EXCEPTION 'No existe ck_cliente_origen: falta la migración 005. Nada fue modificado.';
    END IF;
    IF (SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_cliente_origen') LIKE '%CONTACTO%' THEN
        RAISE EXCEPTION 'La migración 007 ya está aplicada. Nada fue modificado.';
    END IF;
END $$;

ALTER TABLE cliente_potencial DROP CONSTRAINT ck_cliente_origen;
ALTER TABLE cliente_potencial ADD CONSTRAINT ck_cliente_origen
    CHECK (origen IN ('CHAT', 'CONTACTO', 'WHATSAPP', 'LLAMADA', 'OTRO'));
ALTER TABLE cliente_potencial DROP CONSTRAINT ck_cliente_consentimiento;
ALTER TABLE cliente_potencial ADD CONSTRAINT ck_cliente_consentimiento
    CHECK (origen NOT IN ('CHAT', 'CONTACTO') OR (consentimiento_en IS NOT NULL AND consentimiento_version IS NOT NULL));

DO $$
BEGIN
    IF (SELECT count(*) FROM cliente_potencial) <> (SELECT count(*) FROM antes)
       OR EXISTS (SELECT 1 FROM antes a JOIN cliente_potencial c USING (id_cliente_potencial)
                  WHERE c.origen IS DISTINCT FROM a.origen OR c.consentimiento_en IS DISTINCT FROM a.consentimiento_en) THEN
        RAISE EXCEPTION 'Los clientes potenciales cambiaron. Se deshace todo.';
    END IF;
    RAISE NOTICE 'OK: migración 007 aplicada. % clientes potenciales sin cambios.', (SELECT count(*) FROM cliente_potencial);
END $$;
COMMIT;
