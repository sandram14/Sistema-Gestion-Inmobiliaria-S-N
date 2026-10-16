-- Verificación previa de la migración 009 (NO modifica nada).
-- Esperado: la restricción ck_operacion_moneda con ('PYG', 'USD'), sin valor por defecto en la columna moneda,
-- y la lista de precios en USD que pasarán a PYG CON EL MISMO NÚMERO (no se convierte: 45.000 USD queda Gs. 45.000).
-- Conviene guardar esta salida: es el registro de qué precios estaban en dólares antes de migrar.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/009_verificar_solo_guaranies.sql
SELECT conname, pg_get_constraintdef(oid) AS definicion FROM pg_constraint WHERE conname = 'ck_operacion_moneda';
SELECT column_default AS valor_por_defecto FROM information_schema.columns
 WHERE table_name = 'operacion_propiedad' AND column_name = 'moneda';
SELECT moneda, count(*) AS operaciones FROM operacion_propiedad GROUP BY moneda ORDER BY moneda;
SELECT p.codigo, p.titulo, o.nombre AS operacion, op.precio AS precio_actual, op.moneda AS moneda_actual,
       'Gs. ' || replace(to_char(op.precio, 'FM999,999,999,999,999'), ',', '.') AS quedara_como
  FROM operacion_propiedad op
  JOIN propiedad p USING (id_propiedad)
  JOIN operacion o USING (id_operacion)
 WHERE op.moneda <> 'PYG'
 ORDER BY p.codigo, o.nombre;
