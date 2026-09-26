-- =====================================================================
-- S&N Inmobiliaria - Carga inicial 03: Propiedades de demostración
-- =====================================================================
-- Las 28 propiedades que el frontend tenía en propiedades.geojson.
-- Criterios de migración (decisiones de la Fase 2):
--   * El 'id' del GeoJSON pasa a ser propiedad.codigo (prop-001, terr-001).
--   * Las 7 propiedades de categoría 'Departamento' se cargan como Casa;
--     sus títulos se conservan y conviene revisarlos desde el panel.
--   * dormitorios -> casa.habitaciones; m2 -> casa.superficie_construida
--     o terreno.superficie.
--   * Datos que el GeoJSON no tenía (valores supuestos, a revisar):
--     casa.sala/comedor/cocina = true; terreno.tiene_titulo = false.
--   * Precio y moneda (USD) van a operacion_propiedad.
--   * fotos[] -> imagen; la primera es la principal y 'orden' sigue el arreglo.
-- =====================================================================

BEGIN;

-- prop-001: Casa en Area 1 (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-001', 'Casa en Area 1', 'Calle Curupayty 519', -25.51757, -54.63972, tp.id_tipo_propiedad, e.id_estado, 10011013
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 2, true, true, true, 182 FROM propiedad WHERE codigo = 'prop-001';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 45000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-001' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-001/1.png', true, 0),
    ('/images/propiedades/prop-001/2.png', false, 1),
    ('/images/propiedades/prop-001/3.png', false, 2),
    ('/images/propiedades/prop-001/4.png', false, 3),
    ('/images/propiedades/prop-001/5.png', false, 4)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-001';

-- prop-002: Departamento en Area 1 (Departamento / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-002', 'Departamento en Area 1', 'Av. Monseñor Rodríguez 995', -25.51581, -54.63752, tp.id_tipo_propiedad, e.id_estado, 10011013
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 1, true, true, true, 49 FROM propiedad WHERE codigo = 'prop-002';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 70000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-002' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-002/1.png', true, 0),
    ('/images/propiedades/prop-002/2.png', false, 1),
    ('/images/propiedades/prop-002/3.png', false, 2),
    ('/images/propiedades/prop-002/4.png', false, 3)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-002';

-- prop-003: Casa en San Jose (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-003', 'Casa en San Jose', 'Av. Del Lago 1002', -25.51391, -54.6458, tp.id_tipo_propiedad, e.id_estado, 10011014
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 2, true, true, true, 263 FROM propiedad WHERE codigo = 'prop-003';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 120000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-003' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-003/1.png', true, 0),
    ('/images/propiedades/prop-003/2.png', false, 1),
    ('/images/propiedades/prop-003/3.png', false, 2),
    ('/images/propiedades/prop-003/4.png', false, 3)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-003';

-- prop-004: Departamento en San Jose (Departamento / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-004', 'Departamento en San Jose', 'Av. Del Lago 1493', -25.51157, -54.65519, tp.id_tipo_propiedad, e.id_estado, 10011014
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 2, true, true, true, 45 FROM propiedad WHERE codigo = 'prop-004';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 420, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-004' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-004/1.png', true, 0),
    ('/images/propiedades/prop-004/2.png', false, 1),
    ('/images/propiedades/prop-004/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-004';

-- prop-005: Casa en Ciudad Nueva (Casa / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-005', 'Casa en Ciudad Nueva', 'Av. Monseñor Rodríguez 479', -25.52037, -54.67079, tp.id_tipo_propiedad, e.id_estado, 10011024
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 2, true, true, true, 315 FROM propiedad WHERE codigo = 'prop-005';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 372, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-005' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-005/1.png', true, 0),
    ('/images/propiedades/prop-005/2.png', false, 1),
    ('/images/propiedades/prop-005/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-005';

-- prop-006: Departamento en Ciudad Nueva (Departamento / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-006', 'Departamento en Ciudad Nueva', 'Av. Kubitschek 1981', -25.507, -54.66748, tp.id_tipo_propiedad, e.id_estado, 10011024
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 2, true, true, true, 89 FROM propiedad WHERE codigo = 'prop-006';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 168, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-006' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-006/1.png', true, 0),
    ('/images/propiedades/prop-006/2.png', false, 1),
    ('/images/propiedades/prop-006/3.png', false, 2),
    ('/images/propiedades/prop-006/4.png', false, 3)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-006';

-- prop-007: Departamento en San Juan (Departamento / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-007', 'Departamento en San Juan', 'Ruta Internacional 384', -25.48256, -54.67572, tp.id_tipo_propiedad, e.id_estado, 10011003
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 1, 2, true, true, true, 82 FROM propiedad WHERE codigo = 'prop-007';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 58000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-007' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-007/1.png', true, 0),
    ('/images/propiedades/prop-007/2.png', false, 1),
    ('/images/propiedades/prop-007/3.png', false, 2),
    ('/images/propiedades/prop-007/4.png', false, 3)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-007';

-- prop-008: Casa en San Juan (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-008', 'Casa en San Juan', 'Av. Monseñor Rodríguez 1053', -25.48446, -54.68288, tp.id_tipo_propiedad, e.id_estado, 10011003
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 2, true, true, true, 317 FROM propiedad WHERE codigo = 'prop-008';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 150000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-008' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-008/1.png', true, 0),
    ('/images/propiedades/prop-008/2.png', false, 1),
    ('/images/propiedades/prop-008/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-008';

-- prop-009: Casa en Domingo Sabio (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-009', 'Casa en Domingo Sabio', 'Calle Curupayty 1616', -25.48785, -54.76431, tp.id_tipo_propiedad, e.id_estado, 10111016
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 2, true, true, true, 236 FROM propiedad WHERE codigo = 'prop-009';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 95000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-009' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-009/1.png', true, 0),
    ('/images/propiedades/prop-009/2.png', false, 1),
    ('/images/propiedades/prop-009/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-009';

-- prop-010: Casa en Maria Auxiliadora (Casa / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-010', 'Casa en Maria Auxiliadora', 'Calle Curupayty 2287', -25.48553, -54.75711, tp.id_tipo_propiedad, e.id_estado, 10111015
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 1, true, true, true, 299 FROM propiedad WHERE codigo = 'prop-010';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 372, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-010' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-010/1.png', true, 0),
    ('/images/propiedades/prop-010/2.png', false, 1),
    ('/images/propiedades/prop-010/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-010';

-- prop-011: Casa en Virgen Del Carmen (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-011', 'Casa en Virgen Del Carmen', 'Ruta Internacional 1428', -25.47525, -54.75835, tp.id_tipo_propiedad, e.id_estado, 10111004
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 4, 2, true, true, true, 217 FROM propiedad WHERE codigo = 'prop-011';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 62000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-011' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-011/1.png', true, 0),
    ('/images/propiedades/prop-011/2.png', false, 1),
    ('/images/propiedades/prop-011/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-011';

-- prop-012: Casa en Calle 14 Monday (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-012', 'Casa en Calle 14 Monday', 'Av. San Blas 371', -25.54501, -54.74456, tp.id_tipo_propiedad, e.id_estado, 10116200
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 3, true, true, true, 200 FROM propiedad WHERE codigo = 'prop-012';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 62000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-012' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-012/1.png', true, 0),
    ('/images/propiedades/prop-012/2.png', false, 1),
    ('/images/propiedades/prop-012/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-012';

-- prop-013: Departamento en Parana Country Club (Departamento / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-013', 'Departamento en Parana Country Club', 'Av. Del Lago 1979', -25.47921, -54.62177, tp.id_tipo_propiedad, e.id_estado, 10051023
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 3, true, true, true, 72 FROM propiedad WHERE codigo = 'prop-013';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 70000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-013' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-013/1.png', true, 0),
    ('/images/propiedades/prop-013/2.png', false, 1),
    ('/images/propiedades/prop-013/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-013';

-- prop-014: Departamento en Nuestra Sra De La Asuncion (Departamento / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-014', 'Departamento en Nuestra Sra De La Asuncion', 'Av. Del Lago 2490', -25.40076, -54.6431, tp.id_tipo_propiedad, e.id_estado, 10051007
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 2, true, true, true, 76 FROM propiedad WHERE codigo = 'prop-014';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 42000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-014' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-014/1.png', true, 0),
    ('/images/propiedades/prop-014/2.png', false, 1),
    ('/images/propiedades/prop-014/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-014';

-- prop-015: Departamento en San Antonio (Departamento / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-015', 'Departamento en San Antonio', 'Av. Monseñor Rodríguez 292', -25.55971, -54.60829, tp.id_tipo_propiedad, e.id_estado, 10021001
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 3, true, true, true, 62 FROM propiedad WHERE codigo = 'prop-015';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 252, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-015' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-015/1.png', true, 0),
    ('/images/propiedades/prop-015/2.png', false, 1),
    ('/images/propiedades/prop-015/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-015';

-- prop-016: Casa en San Miguel (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-016', 'Casa en San Miguel', 'Av. Monseñor Rodríguez 1676', -25.55538, -54.65128, tp.id_tipo_propiedad, e.id_estado, 10021010
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 3, true, true, true, 294 FROM propiedad WHERE codigo = 'prop-016';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 62000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'prop-016' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/propiedades/prop-016/1.png', true, 0),
    ('/images/propiedades/prop-016/2.png', false, 1),
    ('/images/propiedades/prop-016/3.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-016';

-- terr-001: Terreno en NUESTRA SRA DE LA ASUNCION (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-001', 'Terreno en NUESTRA SRA DE LA ASUNCION', 'NUESTRA SRA DE LA ASUNCION, HERNANDARIAS', -25.40701, -54.640313, tp.id_tipo_propiedad, e.id_estado, 10051007
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 750, false FROM propiedad WHERE codigo = 'terr-001';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 41000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-001' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-001/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-001';

-- terr-002: Terreno en SAN MIGUEL (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-002', 'Terreno en SAN MIGUEL', 'SAN MIGUEL, PRESIDENTE FRANCO', -25.56093, -54.640562, tp.id_tipo_propiedad, e.id_estado, 10021010
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 1000, false FROM propiedad WHERE codigo = 'terr-002';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 46000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-002' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-002/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-002';

-- terr-003: Terreno en SAN ANTONIO (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-003', 'Terreno en SAN ANTONIO', 'SAN ANTONIO, PRESIDENTE FRANCO', -25.560088, -54.61054, tp.id_tipo_propiedad, e.id_estado, 10021001
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 1000, false FROM propiedad WHERE codigo = 'terr-003';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 54000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-003' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-003/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-003';

-- terr-004: Terreno en DOMINGO SABIO (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-004', 'Terreno en DOMINGO SABIO', 'DOMINGO SABIO, MINGA GUAZÚ', -25.484932, -54.762922, tp.id_tipo_propiedad, e.id_estado, 10111016
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 300, false FROM propiedad WHERE codigo = 'terr-004';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 23000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-004' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-004/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-004';

-- terr-005: Terreno en MARIA AUXILIADORA (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-005', 'Terreno en MARIA AUXILIADORA', 'MARIA AUXILIADORA, MINGA GUAZÚ', -25.486921, -54.758836, tp.id_tipo_propiedad, e.id_estado, 10111015
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 750, false FROM propiedad WHERE codigo = 'terr-005';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 38000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-005' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-005/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-005';

-- terr-006: Terreno en VIRGEN DEL CARMEN (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-006', 'Terreno en VIRGEN DEL CARMEN', 'VIRGEN DEL CARMEN, MINGA GUAZÚ', -25.474217, -54.74545, tp.id_tipo_propiedad, e.id_estado, 10111004
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 400, false FROM propiedad WHERE codigo = 'terr-006';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 24000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-006' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-006/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-006';

-- terr-007: Terreno en CIUDAD NUEVA (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-007', 'Terreno en CIUDAD NUEVA', 'CIUDAD NUEVA, CIUDAD DEL ESTE', -25.523153, -54.67457, tp.id_tipo_propiedad, e.id_estado, 10011024
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 750, false FROM propiedad WHERE codigo = 'terr-007';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 58000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-007' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-007/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-007';

-- terr-008: Terreno en SAN JUAN (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-008', 'Terreno en SAN JUAN', 'SAN JUAN, CIUDAD DEL ESTE', -25.475909, -54.670143, tp.id_tipo_propiedad, e.id_estado, 10011003
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 1000, false FROM propiedad WHERE codigo = 'terr-008';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 48000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-008' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-008/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-008';

-- terr-009: Terreno en SAN JOSE (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-009', 'Terreno en SAN JOSE', 'SAN JOSE, CIUDAD DEL ESTE', -25.50749, -54.651267, tp.id_tipo_propiedad, e.id_estado, 10011014
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 900, false FROM propiedad WHERE codigo = 'terr-009';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 61000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-009' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-009/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-009';

-- terr-010: Terreno en AREA 1 (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-010', 'Terreno en AREA 1', 'AREA 1, CIUDAD DEL ESTE', -25.519365, -54.638091, tp.id_tipo_propiedad, e.id_estado, 10011013
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 400, false FROM propiedad WHERE codigo = 'terr-010';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 32000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-010' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-010/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-010';

-- terr-011: Terreno en PARANA COUNTRY CLUB (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-011', 'Terreno en PARANA COUNTRY CLUB', 'PARANA COUNTRY CLUB, HERNANDARIAS', -25.492722, -54.601243, tp.id_tipo_propiedad, e.id_estado, 10051023
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 1200, false FROM propiedad WHERE codigo = 'terr-011';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 85000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-011' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-011/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-011';

-- terr-012: Terreno en CALLE 14 MONDAY (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-012', 'Terreno en CALLE 14 MONDAY', 'CALLE 14 MONDAY, MINGA GUAZÚ', -25.561758, -54.747106, tp.id_tipo_propiedad, e.id_estado, 10116200
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 600, false FROM propiedad WHERE codigo = 'terr-012';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 43000, 'USD' FROM propiedad p, operacion o WHERE p.codigo = 'terr-012' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/images/terrenos/terr-012/1.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-012';

COMMIT;
