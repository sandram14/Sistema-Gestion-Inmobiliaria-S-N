-- Verificación previa de la migración 007 (NO modifica nada).
-- Esperado: la restricción ck_cliente_origen SIN 'CONTACTO' y la cantidad actual de clientes potenciales.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/007_verificar_origen_contacto.sql
SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE conname IN ('ck_cliente_origen', 'ck_cliente_consentimiento');
SELECT count(*) AS clientes_potenciales FROM cliente_potencial;
