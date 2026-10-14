-- Verificación previa de la migración 006 (NO modifica nada). Esperado: tablas_existentes = 0 y migracion_005 = 1.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/006_verificar_sitio.sql
SELECT count(*) AS tablas_existentes FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('seccion_sitio', 'propiedad_destacada');
SELECT count(*) AS migracion_005 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'cliente_potencial';
