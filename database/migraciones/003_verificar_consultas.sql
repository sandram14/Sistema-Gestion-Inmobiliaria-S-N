-- =====================================================================
-- Verificación previa de la migración 003 (NO modifica nada)
-- =====================================================================
-- Esperado antes de migrar: las 6 columnas actuales de consulta, columnas_nuevas = 0,
-- tabla_recomendaciones = 0 y la cantidad de consultas y conversaciones actual.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/003_verificar_consultas.sql
-- =====================================================================
\d consulta

SELECT count(*) FILTER (WHERE column_name IN ('resultado', 'estado_revision', 'revisada_por', 'revisada_en')) AS columnas_nuevas,
       count(*) FILTER (WHERE column_name IN ('id_consulta', 'id_sesion', 'consulta', 'respuesta', 'id_propiedad', 'fecha_hora')) AS columnas_actuales
  FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'consulta';

SELECT count(*) AS tabla_recomendaciones FROM information_schema.tables
 WHERE table_schema = 'public' AND table_name = 'consulta_recomendacion';

SELECT count(*) AS consultas, count(DISTINCT id_sesion) AS conversaciones, count(id_propiedad) AS con_propiedad FROM consulta;
