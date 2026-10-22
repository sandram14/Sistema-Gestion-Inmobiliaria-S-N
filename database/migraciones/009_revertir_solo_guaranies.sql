-- Reversión de la migración 009: vuelve a aceptar PYG y USD y quita el valor por defecto.
-- NO devuelve ningún precio a USD: no hay forma segura de saber cuáles estaban en dólares ni si ya se
-- corrigieron desde el panel. Para recuperar los valores anteriores, restaurar el respaldo hecho antes de migrar.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/009_revertir_solo_guaranies.sql
\set ON_ERROR_STOP on
BEGIN;
DO $$
BEGIN
    IF (SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_operacion_moneda') LIKE '%USD%' THEN
        RAISE EXCEPTION 'La migración 009 no está aplicada. Nada fue modificado.';
    END IF;
END $$;
ALTER TABLE operacion_propiedad DROP CONSTRAINT ck_operacion_moneda;
ALTER TABLE operacion_propiedad ADD CONSTRAINT ck_operacion_moneda CHECK (moneda IN ('PYG', 'USD'));
ALTER TABLE operacion_propiedad ALTER COLUMN moneda DROP DEFAULT;
COMMIT;
