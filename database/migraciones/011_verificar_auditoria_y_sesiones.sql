-- Verificación previa de la migración 011 (NO modifica nada).
-- Esperado antes de migrar: version_credencial y auditoria = no existe, y la cantidad actual de usuarios por rol.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/011_verificar_auditoria_y_sesiones.sql
SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'usuario' AND column_name = 'version_credencial')
            THEN 'existe' ELSE 'no existe' END AS version_credencial,
       CASE WHEN to_regclass('public.auditoria') IS NOT NULL THEN 'existe' ELSE 'no existe' END AS auditoria;
SELECT r.nombre AS rol, count(*) AS usuarios, count(*) FILTER (WHERE u.activo) AS activos,
       count(*) FILTER (WHERE u.debe_cambiar_password) AS con_contrasena_temporal
  FROM usuario u JOIN rol r USING (id_rol) GROUP BY r.nombre ORDER BY r.nombre;
