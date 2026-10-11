-- =====================================================================
-- Verificación previa de la migración 002 (NO modifica nada)
-- =====================================================================
-- Muestra la estructura real de las tablas que cambian y el estado de los datos.
-- Resultado esperado antes de migrar:
--   * tabla rol con las columnas id_rol, nombre y descripcion;
--   * un solo rol, ADMIN;
--   * columnas_nuevas_existentes = 0 (todavía no se aplicó);
--   * sus usuarios y la cantidad de propiedades, para compararlos después.
--
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/002_verificar_roles.sql
-- =====================================================================
\d rol

SELECT id_rol, nombre, descripcion FROM rol ORDER BY id_rol;

SELECT count(*) AS columnas_nuevas_existentes
  FROM information_schema.columns
 WHERE table_schema = 'public'
   AND ((table_name = 'usuario' AND column_name = 'debe_cambiar_password')
     OR (table_name = 'propiedad' AND column_name = 'id_responsable'));

SELECT u.id_usuario, u.email, r.nombre AS rol, u.activo
  FROM usuario u JOIN rol r USING (id_rol)
 ORDER BY u.id_usuario;

SELECT count(*) AS propiedades FROM propiedad;
