-- =====================================================================
-- S&N Inmobiliaria - Verificación de la base de datos
-- =====================================================================
-- Comprueba que los datos se cargaron y que cada regla rechaza lo que
-- debe rechazar. Todo ocurre dentro de una transacción que se deshace
-- al final (ROLLBACK): la base queda exactamente igual.
--
-- Uso:
--   docker compose -f docker/docker-compose.yml exec db \
--     psql -U sn_app -d sn_inmobiliaria -f /database/pruebas/verificar.sql
-- Resultado esperado: todas las líneas "OK" y ninguna "FALLÓ".
-- =====================================================================
\set ON_ERROR_STOP on
\pset footer off
-- Sin paginador: evita que las tablas anchas queden ocultas en ventanas angostas
\pset pager off

\echo
\echo '=== 1. Cantidad de registros cargados ==='
SELECT 'departamento' AS tabla, count(*) AS filas FROM departamento
UNION ALL SELECT 'ciudad',              count(*) FROM ciudad
UNION ALL SELECT 'barrio',              count(*) FROM barrio
UNION ALL SELECT 'propiedad',           count(*) FROM propiedad
UNION ALL SELECT 'casa',                count(*) FROM casa
UNION ALL SELECT 'terreno',             count(*) FROM terreno
UNION ALL SELECT 'operacion_propiedad', count(*) FROM operacion_propiedad
UNION ALL SELECT 'imagen',              count(*) FROM imagen
UNION ALL SELECT 'usuario',             count(*) FROM usuario;

\echo '=== 2. Propiedades por tipo y operación ==='
SELECT tp.nombre AS tipo, o.nombre AS operacion, op.moneda, count(*) AS cantidad
  FROM propiedad p
  JOIN tipo_propiedad tp USING (id_tipo_propiedad)
  JOIN operacion_propiedad op USING (id_propiedad)
  JOIN operacion o USING (id_operacion)
 GROUP BY 1, 2, 3 ORDER BY 1, 2;

\echo '=== 3. Coherencia geográfica (todas deben ser 0) ==='
SELECT
  (SELECT count(*) FROM propiedad p JOIN barrio b USING (id_barrio)
    WHERE NOT ST_Covers(b.geom, p.ubicacion))                          AS propiedades_fuera_de_su_barrio,
  (SELECT count(*) FROM barrio WHERE NOT ST_IsValid(geom))              AS barrios_con_poligono_invalido,
  (SELECT count(*) FROM propiedad p
    WHERE (SELECT count(*) FROM imagen i WHERE i.id_propiedad = p.id_propiedad AND i.es_principal) <> 1)
                                                                        AS propiedades_sin_una_imagen_principal;

\echo '=== 4. Pruebas de reglas (se deshacen al final) ==='
-- Se oculta la salida vacía de cada SELECT; los avisos OK/FALLÓ se siguen mostrando
\o /dev/null
BEGIN;

-- Ejecuta una sentencia que DEBE fallar con el código SQLSTATE indicado
CREATE FUNCTION pg_temp.debe_fallar(p_descripcion text, p_sql text, p_sqlstate text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE p_sql;
    RAISE EXCEPTION 'FALLÓ: % (la sentencia fue aceptada y debía rechazarse)', p_descripcion;
EXCEPTION WHEN OTHERS THEN
    IF SQLSTATE = p_sqlstate THEN
        RAISE NOTICE 'OK: %', p_descripcion;
    ELSIF SQLERRM LIKE 'FALLÓ:%' THEN
        RAISE;
    ELSE
        RAISE EXCEPTION 'FALLÓ: % (se esperaba %, llegó %: %)', p_descripcion, p_sqlstate, SQLSTATE, SQLERRM;
    END IF;
END;
$$;

-- Ejecuta una sentencia que DEBE funcionar
CREATE FUNCTION pg_temp.debe_funcionar(p_descripcion text, p_sql text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE p_sql;
    RAISE NOTICE 'OK: %', p_descripcion;
EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION 'FALLÓ: % (%: %)', p_descripcion, SQLSTATE, SQLERRM;
END;
$$;

SELECT pg_temp.debe_fallar('Un terreno no puede tener operación Alquiler',
  $q$INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
     SELECT p.id_propiedad, o.id_operacion, 1000000, 'PYG'
       FROM propiedad p, operacion o WHERE p.codigo = 'terr-001' AND o.nombre = 'Alquiler'$q$, '23514');

SELECT pg_temp.debe_funcionar('Una casa puede tener Venta y Alquiler (con monedas distintas)',
  $q$INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
     SELECT p.id_propiedad, o.id_operacion, 4000000, 'PYG'
       FROM propiedad p, operacion o WHERE p.codigo = 'prop-001' AND o.nombre = 'Alquiler'$q$);

SELECT pg_temp.debe_fallar('Una propiedad no puede repetir la misma operación',
  $q$INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
     SELECT p.id_propiedad, o.id_operacion, 1, 'USD'
       FROM propiedad p, operacion o WHERE p.codigo = 'prop-001' AND o.nombre = 'Venta'$q$, '23505');

SELECT pg_temp.debe_fallar('Moneda distinta de PYG/USD se rechaza',
  $q$UPDATE operacion_propiedad SET moneda = 'EUR'
      WHERE id_propiedad = (SELECT id_propiedad FROM propiedad WHERE codigo = 'prop-001')$q$, '23514');

SELECT pg_temp.debe_fallar('Precio 0 se rechaza',
  $q$UPDATE operacion_propiedad SET precio = 0
      WHERE id_propiedad = (SELECT id_propiedad FROM propiedad WHERE codigo = 'prop-001')$q$, '23514');

SELECT pg_temp.debe_fallar('Solo puede haber una imagen principal por propiedad',
  $q$INSERT INTO imagen (id_propiedad, url, es_principal, orden)
     SELECT id_propiedad, '/uploads/prueba/otra.jpg', true, 9 FROM propiedad WHERE codigo = 'prop-001'$q$, '23505');

SELECT pg_temp.debe_funcionar('Se pueden agregar imágenes no principales',
  $q$INSERT INTO imagen (id_propiedad, url, es_principal, orden)
     SELECT id_propiedad, '/uploads/prueba/otra.jpg', false, 9 FROM propiedad WHERE codigo = 'prop-001'$q$);

SELECT pg_temp.debe_fallar('Datos de Casa en una propiedad de tipo Terreno se rechazan',
  $q$INSERT INTO casa (id_propiedad, habitaciones, banos, superficie_construida)
     SELECT id_propiedad, 2, 1, 80 FROM propiedad WHERE codigo = 'terr-001'$q$, '23514');

SELECT pg_temp.debe_fallar('Datos de Terreno en una propiedad de tipo Casa se rechazan',
  $q$INSERT INTO terreno (id_propiedad, superficie)
     SELECT id_propiedad, 300 FROM propiedad WHERE codigo = 'prop-001'$q$, '23514');

SELECT pg_temp.debe_fallar('Cambiar a Terreno una propiedad con datos de Casa se rechaza',
  $q$UPDATE propiedad SET id_tipo_propiedad = (SELECT id_tipo_propiedad FROM tipo_propiedad WHERE nombre = 'Terreno')
      WHERE codigo = 'prop-001'$q$, '23514');

SELECT pg_temp.debe_fallar('Ubicación fuera del barrio asignado se rechaza',
  $q$UPDATE propiedad SET latitud = -25.300000, longitud = -54.500000 WHERE codigo = 'prop-001'$q$, '23514');

SELECT pg_temp.debe_funcionar('Mover la ubicación dentro del mismo barrio funciona',
  $q$UPDATE propiedad p
        SET latitud  = round(ST_Y(ST_PointOnSurface(b.geom))::numeric, 6),
            longitud = round(ST_X(ST_PointOnSurface(b.geom))::numeric, 6)
       FROM barrio b
      WHERE b.id_barrio = p.id_barrio AND p.codigo = 'prop-001'$q$);

SELECT pg_temp.debe_fallar('El código público no puede repetirse',
  $q$INSERT INTO propiedad (codigo, titulo, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
     SELECT 'prop-001', 'Duplicada', latitud, longitud, id_tipo_propiedad, id_estado, id_barrio
       FROM propiedad WHERE codigo = 'prop-002'$q$, '23505');

SELECT pg_temp.debe_fallar('El código público debe tener el formato prefijo-000',
  $q$INSERT INTO propiedad (codigo, titulo, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
     SELECT 'PROP_1', 'Formato inválido', latitud, longitud, id_tipo_propiedad, id_estado, id_barrio
       FROM propiedad WHERE codigo = 'prop-002'$q$, '23514');

SELECT pg_temp.debe_funcionar('Crear un usuario con hash BCrypt válido',
  $q$INSERT INTO usuario (nombre, apellido, email, password_hash, id_rol)
     SELECT 'Prueba', 'Test', 'Prueba@Test.com', crypt('clave', gen_salt('bf', 4)), id_rol FROM rol WHERE nombre = 'ADMIN'$q$);

SELECT pg_temp.debe_fallar('El email se repite sin distinguir mayúsculas',
  $q$INSERT INTO usuario (nombre, apellido, email, password_hash, id_rol)
     SELECT 'Otro', 'Test', 'prueba@test.com', crypt('clave', gen_salt('bf', 4)), id_rol FROM rol WHERE nombre = 'ADMIN'$q$, '23505');

SELECT pg_temp.debe_fallar('No se acepta una contraseña sin hashear',
  $q$INSERT INTO usuario (nombre, apellido, email, password_hash, id_rol)
     SELECT 'Plano', 'Test', 'plano@test.com', 'clave123', id_rol FROM rol WHERE nombre = 'ADMIN'$q$, '23514');

SELECT pg_temp.debe_funcionar('Borrado lógico: pasar una propiedad a Inactivo',
  $q$UPDATE propiedad SET id_estado = (SELECT id_estado FROM estado_propiedad WHERE nombre = 'Inactivo')
      WHERE codigo = 'prop-003'$q$);

ROLLBACK;
\o

\echo
\echo '=== Verificación terminada: si no hubo ningún FALLÓ, todo está correcto ==='
