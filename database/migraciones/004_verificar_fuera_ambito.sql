-- Verificación previa de la migración 004 (NO modifica nada).
-- Esperado: la restricción ck_consulta_resultado SIN 'FUERA_DE_AMBITO' y la cantidad de consultas actual.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/004_verificar_fuera_ambito.sql
SELECT pg_get_constraintdef(oid) AS restriccion_actual FROM pg_constraint WHERE conname = 'ck_consulta_resultado';
SELECT count(*) AS consultas, count(resultado) AS con_resultado FROM consulta;
