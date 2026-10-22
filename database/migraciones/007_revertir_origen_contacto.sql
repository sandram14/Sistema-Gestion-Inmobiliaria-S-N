-- Reversión de la migración 007. Se detiene si ya hay clientes potenciales con origen CONTACTO
-- (son datos reales: no se modifican automáticamente).
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/007_revertir_origen_contacto.sql
\set ON_ERROR_STOP on
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cliente_potencial WHERE origen = 'CONTACTO') THEN
        RAISE EXCEPTION 'Hay clientes potenciales con origen CONTACTO. Nada fue modificado.';
    END IF;
END $$;
ALTER TABLE cliente_potencial DROP CONSTRAINT ck_cliente_origen;
ALTER TABLE cliente_potencial ADD CONSTRAINT ck_cliente_origen CHECK (origen IN ('CHAT', 'WHATSAPP', 'LLAMADA', 'OTRO'));
ALTER TABLE cliente_potencial DROP CONSTRAINT ck_cliente_consentimiento;
ALTER TABLE cliente_potencial ADD CONSTRAINT ck_cliente_consentimiento
    CHECK (origen <> 'CHAT' OR (consentimiento_en IS NOT NULL AND consentimiento_version IS NOT NULL));
COMMIT;
