-- =====================================================================
-- Migración 009: el sistema trabaja solo en guaraníes (PYG)
-- =====================================================================
-- 1. Los precios en USD pasan a PYG CONSERVANDO EL MISMO NÚMERO (no se convierte: 45.000 USD queda
--    Gs. 45.000). Los precios reales se corrigen después desde el panel.
-- 2. ck_operacion_moneda pasa a aceptar solo 'PYG', y la columna moneda toma 'PYG' por defecto.
-- No cambia ningún precio, ninguna operación ni ninguna propiedad. Hacer un respaldo antes (pg_dump):
-- es la única forma de recuperar qué precios estaban en dólares (también los lista 009_verificar).
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/009_solo_guaranies.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE antes ON COMMIT DROP AS SELECT id_propiedad, id_operacion, precio, moneda FROM operacion_propiedad;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_operacion_moneda') THEN
        RAISE EXCEPTION 'No existe ck_operacion_moneda. Nada fue modificado.';
    END IF;
    IF (SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_operacion_moneda') NOT LIKE '%USD%' THEN
        RAISE EXCEPTION 'La migración 009 ya está aplicada. Nada fue modificado.';
    END IF;
    IF EXISTS (SELECT 1 FROM operacion_propiedad WHERE moneda NOT IN ('PYG', 'USD')) THEN
        RAISE EXCEPTION 'Hay monedas distintas de PYG y USD. Nada fue modificado.';
    END IF;
    RAISE NOTICE 'Antes: % operaciones en USD y % en PYG.',
        (SELECT count(*) FROM antes WHERE moneda = 'USD'), (SELECT count(*) FROM antes WHERE moneda = 'PYG');
END $$;

UPDATE operacion_propiedad SET moneda = 'PYG' WHERE moneda = 'USD';
ALTER TABLE operacion_propiedad DROP CONSTRAINT ck_operacion_moneda;
ALTER TABLE operacion_propiedad ADD CONSTRAINT ck_operacion_moneda CHECK (moneda = 'PYG');
ALTER TABLE operacion_propiedad ALTER COLUMN moneda SET DEFAULT 'PYG';

DO $$
BEGIN
    IF (SELECT count(*) FROM operacion_propiedad) <> (SELECT count(*) FROM antes)
       OR EXISTS (SELECT 1 FROM antes a LEFT JOIN operacion_propiedad op USING (id_propiedad, id_operacion)
                  WHERE op.id_propiedad IS NULL OR op.precio IS DISTINCT FROM a.precio)
       OR EXISTS (SELECT 1 FROM operacion_propiedad WHERE moneda <> 'PYG') THEN
        RAISE EXCEPTION 'Las operaciones o sus precios cambiaron. Se deshace todo.';
    END IF;
    RAISE NOTICE 'OK: migración 009 aplicada. % operaciones, todas en PYG, con los mismos precios.',
        (SELECT count(*) FROM operacion_propiedad);
END $$;
COMMIT;
