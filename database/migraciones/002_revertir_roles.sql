-- =====================================================================
-- Reversión de la migración 002
-- =====================================================================
-- Quita propiedad.id_responsable, usuario.debe_cambiar_password y los roles GERENTE y
-- VENDEDOR. Se detiene sin modificar nada si algún usuario tiene uno de esos roles
-- (habría que cambiarle el rol antes, para no perder su cuenta).
--
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/002_revertir_roles.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuario u JOIN rol r USING (id_rol) WHERE r.nombre IN ('GERENTE', 'VENDEDOR')) THEN
        RAISE EXCEPTION 'Hay usuarios con rol GERENTE o VENDEDOR. Cámbieles el rol antes de revertir. Nada fue modificado.';
    END IF;
END $$;

DROP INDEX IF EXISTS ix_propiedad_responsable;
ALTER TABLE propiedad DROP COLUMN IF EXISTS id_responsable;
ALTER TABLE usuario DROP COLUMN IF EXISTS debe_cambiar_password;
DELETE FROM rol WHERE nombre IN ('GERENTE', 'VENDEDOR');

COMMIT;

SELECT id_rol, nombre FROM rol ORDER BY id_rol;
