-- Verificación previa de la migración 005 (NO modifica nada). Esperado: tabla_existe = 0 y migracion_004 = 1.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/005_verificar_clientes.sql
SELECT count(*) AS tabla_existe FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'cliente_potencial';
SELECT count(*) AS migracion_004 FROM pg_constraint WHERE conname = 'ck_consulta_resultado' AND pg_get_constraintdef(oid) LIKE '%FUERA_DE_AMBITO%';
SELECT (SELECT count(*) FROM propiedad) AS propiedades, (SELECT count(*) FROM consulta) AS consultas, (SELECT count(*) FROM usuario) AS usuarios;
