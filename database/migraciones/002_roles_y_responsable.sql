-- =====================================================================
-- Migración 002: roles GERENTE y VENDEDOR, contraseña temporal y responsable
-- =====================================================================
-- Agrega (no modifica ni borra datos):
--   * los roles GERENTE y VENDEDOR;
--   * usuario.debe_cambiar_password (false para las cuentas existentes);
--   * propiedad.id_responsable, opcional (las propiedades existentes quedan sin asignar).
--
-- Todo en UNA transacción. Antes de cambiar nada comprueba la estructura real; después
-- comprueba que no se perdió ni cambió ningún usuario ni propiedad. Si algo no coincide,
-- lanza un error y PostgreSQL deshace TODO (ROLLBACK).
--
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/002_roles_y_responsable.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;

-- Estado anterior, para comparar al final
CREATE TEMP TABLE usuarios_antes ON COMMIT DROP AS
SELECT id_usuario, nombre, apellido, email, password_hash, id_rol, activo FROM usuario;
CREATE TEMP TABLE conteos_antes ON COMMIT DROP AS
SELECT (SELECT count(*) FROM propiedad) AS propiedades, (SELECT count(*) FROM rol) AS roles;

DO $$
DECLARE n integer;
BEGIN
    -- Estructura esperada de rol
    SELECT count(*) INTO n FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'rol' AND column_name IN ('id_rol', 'nombre', 'descripcion');
    IF n <> 3 THEN
        RAISE EXCEPTION 'La tabla rol no tiene la estructura esperada (id_rol, nombre, descripcion). Nada fue modificado.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'ADMIN') THEN
        RAISE EXCEPTION 'No existe el rol ADMIN. Nada fue modificado.';
    END IF;
    -- ¿Ya aplicada?
    SELECT count(*) INTO n FROM information_schema.columns
     WHERE table_schema = 'public'
       AND ((table_name = 'usuario' AND column_name = 'debe_cambiar_password')
         OR (table_name = 'propiedad' AND column_name = 'id_responsable'));
    IF n > 0 THEN
        RAISE EXCEPTION 'Las columnas nuevas ya existen: la migración 002 parece aplicada. Nada fue modificado.';
    END IF;
    IF EXISTS (SELECT 1 FROM rol WHERE nombre IN ('GERENTE', 'VENDEDOR')) THEN
        RAISE EXCEPTION 'Los roles GERENTE o VENDEDOR ya existen. Revise la tabla rol. Nada fue modificado.';
    END IF;
END $$;

INSERT INTO rol (nombre, descripcion) VALUES
    ('GERENTE',  'Gerente: gestiona todas las propiedades, ofertas, contenido y estadísticas; consulta usuarios'),
    ('VENDEDOR', 'Vendedor: ve todas las propiedades, crea y edita las asignadas a él y ve sus consultas');

ALTER TABLE usuario
    ADD COLUMN debe_cambiar_password boolean NOT NULL DEFAULT false;

ALTER TABLE propiedad
    ADD COLUMN id_responsable integer REFERENCES usuario (id_usuario) ON DELETE SET NULL;

CREATE INDEX ix_propiedad_responsable ON propiedad (id_responsable);

DO $$
DECLARE n integer;
BEGIN
    -- Usuarios: los mismos, sin cambios en sus datos, rol, estado ni contraseña
    SELECT count(*) INTO n FROM usuarios_antes a JOIN usuario u USING (id_usuario)
     WHERE u.nombre = a.nombre AND u.apellido = a.apellido AND u.email = a.email
       AND u.password_hash = a.password_hash AND u.id_rol = a.id_rol AND u.activo = a.activo
       AND u.debe_cambiar_password = false;
    IF n <> (SELECT count(*) FROM usuarios_antes) OR (SELECT count(*) FROM usuario) <> (SELECT count(*) FROM usuarios_antes) THEN
        RAISE EXCEPTION 'Los usuarios no coinciden con el estado anterior. Se deshace todo.';
    END IF;
    -- Propiedades: las mismas, todas sin responsable
    IF (SELECT count(*) FROM propiedad) <> (SELECT propiedades FROM conteos_antes) THEN
        RAISE EXCEPTION 'La cantidad de propiedades cambió. Se deshace todo.';
    END IF;
    IF EXISTS (SELECT 1 FROM propiedad WHERE id_responsable IS NOT NULL) THEN
        RAISE EXCEPTION 'Hay propiedades con responsable asignado. Se deshace todo.';
    END IF;
    -- Roles: los anteriores más los dos nuevos
    IF (SELECT count(*) FROM rol) <> (SELECT roles FROM conteos_antes) + 2 THEN
        RAISE EXCEPTION 'La cantidad de roles no es la esperada. Se deshace todo.';
    END IF;
    RAISE NOTICE 'OK: migración 002 aplicada. Usuarios y propiedades sin cambios; roles: %.',
        (SELECT string_agg(nombre, ', ' ORDER BY id_rol) FROM rol);
END $$;

COMMIT;

SELECT r.nombre AS rol, count(u.id_usuario) AS usuarios
  FROM rol r LEFT JOIN usuario u USING (id_rol) GROUP BY r.id_rol, r.nombre ORDER BY r.id_rol;
