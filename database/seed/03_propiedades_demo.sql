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
--   * Precio y moneda van a operacion_propiedad. Todo en guaraníes (PYG) desde la migración 009:
--     se conservaron los números originales, que se corrigen desde el panel.
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
SELECT p.id_propiedad, o.id_operacion, 45000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-001' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-001/0a4c5cd8-14cc-4bbc-a4d8-0f7bc29c541d.png', true, 0),
    ('/uploads/prop-001/9b6cdf5a-ad00-4faf-88b5-06a081d64632.png', false, 1),
    ('/uploads/prop-001/45595d51-cdfc-4b11-9b4f-2a0f8df042d8.png', false, 2),
    ('/uploads/prop-001/0ca0f1e2-3507-40c5-98bc-bc787f579916.png', false, 3),
    ('/uploads/prop-001/7dd2e726-6743-433c-b0f5-7ee56c6ccffd.png', false, 4)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-001';

-- prop-002: Departamento en Area 1 (Departamento / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-002', 'Departamento en Area 1', 'Av. Monseñor Rodríguez 995', -25.51581, -54.63752, tp.id_tipo_propiedad, e.id_estado, 10011013
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 1, true, true, true, 49 FROM propiedad WHERE codigo = 'prop-002';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 70000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-002' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-002/c981819c-fcb0-42d2-b7bb-60f4c91f50ef.png', true, 0),
    ('/uploads/prop-002/888f85e6-e1a7-4940-bd49-253a907a5980.png', false, 1),
    ('/uploads/prop-002/c068029c-5760-4f54-b24a-4bdf7db26069.png', false, 2),
    ('/uploads/prop-002/ab788543-ce3b-420a-9b6d-610ff36359cc.png', false, 3)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-002';

-- prop-003: Casa en San Jose (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-003', 'Casa en San Jose', 'Av. Del Lago 1002', -25.51391, -54.6458, tp.id_tipo_propiedad, e.id_estado, 10011014
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 2, true, true, true, 263 FROM propiedad WHERE codigo = 'prop-003';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 120000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-003' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-003/f6f6ad9a-12c0-4c79-a928-063817c6b264.png', true, 0),
    ('/uploads/prop-003/1f1fd7c1-ae0f-4748-ab70-6e6e08878978.png', false, 1),
    ('/uploads/prop-003/25330c44-6624-4c76-b393-776c63961547.png', false, 2),
    ('/uploads/prop-003/24188424-0ca9-4388-a95c-39009118e65e.png', false, 3)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-003';

-- prop-004: Departamento en San Jose (Departamento / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-004', 'Departamento en San Jose', 'Av. Del Lago 1493', -25.51157, -54.65519, tp.id_tipo_propiedad, e.id_estado, 10011014
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 2, true, true, true, 45 FROM propiedad WHERE codigo = 'prop-004';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 420, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-004' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-004/fa9f0f45-f5eb-4649-be3d-d5a23058a2d1.png', true, 0),
    ('/uploads/prop-004/15373603-6c0d-4a35-b4d4-666d1e4eb424.png', false, 1),
    ('/uploads/prop-004/16655961-0a93-474e-a10e-8d0cd548c622.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-004';

-- prop-005: Casa en Ciudad Nueva (Casa / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-005', 'Casa en Ciudad Nueva', 'Av. Monseñor Rodríguez 479', -25.52037, -54.67079, tp.id_tipo_propiedad, e.id_estado, 10011024
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 2, true, true, true, 315 FROM propiedad WHERE codigo = 'prop-005';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 372, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-005' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-005/af376fd1-d4fe-4b56-a5c9-17a1277eba37.png', true, 0),
    ('/uploads/prop-005/7e447ff4-7fb9-4dcc-a608-4495ca31d3f4.png', false, 1),
    ('/uploads/prop-005/64cbdc3b-e105-40d6-827a-85d0a5f3ab8e.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-005';

-- prop-006: Departamento en Ciudad Nueva (Departamento / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-006', 'Departamento en Ciudad Nueva', 'Av. Kubitschek 1981', -25.507, -54.66748, tp.id_tipo_propiedad, e.id_estado, 10011024
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 2, true, true, true, 89 FROM propiedad WHERE codigo = 'prop-006';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 168, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-006' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-006/daac2482-a9a1-4758-b882-896797ae6593.png', true, 0),
    ('/uploads/prop-006/f62c219b-abab-4502-a670-9652e573f833.png', false, 1),
    ('/uploads/prop-006/8503476e-f91f-4557-a575-7de8d3aeb822.png', false, 2),
    ('/uploads/prop-006/c470bb83-7db2-4a27-917c-db03ad51fa3c.png', false, 3)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-006';

-- prop-007: Departamento en San Juan (Departamento / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-007', 'Departamento en San Juan', 'Ruta Internacional 384', -25.48256, -54.67572, tp.id_tipo_propiedad, e.id_estado, 10011003
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 1, 2, true, true, true, 82 FROM propiedad WHERE codigo = 'prop-007';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 58000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-007' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-007/bd726b05-f577-49bd-b2db-f0c5cc12392e.png', true, 0),
    ('/uploads/prop-007/536b9b24-f8c0-41c1-afae-4ce226ef78d9.png', false, 1),
    ('/uploads/prop-007/406800b4-c18b-43aa-ab02-d68f239c7321.png', false, 2),
    ('/uploads/prop-007/0a7887c9-74e6-4a7b-9394-9d5c94493857.png', false, 3)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-007';

-- prop-008: Casa en San Juan (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-008', 'Casa en San Juan', 'Av. Monseñor Rodríguez 1053', -25.48446, -54.68288, tp.id_tipo_propiedad, e.id_estado, 10011003
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 2, true, true, true, 317 FROM propiedad WHERE codigo = 'prop-008';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 150000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-008' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-008/0d2da80b-8f2c-4903-84ba-5585d5b0d71a.png', true, 0),
    ('/uploads/prop-008/60a2abd7-8c0e-46fd-a965-8c9d7b2a2a70.png', false, 1),
    ('/uploads/prop-008/085333bc-7d2e-44c7-922c-f189c311c63b.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-008';

-- prop-009: Casa en Domingo Sabio (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-009', 'Casa en Domingo Sabio', 'Calle Curupayty 1616', -25.48785, -54.76431, tp.id_tipo_propiedad, e.id_estado, 10111016
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 2, true, true, true, 236 FROM propiedad WHERE codigo = 'prop-009';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 95000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-009' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-009/8c9f705d-e45b-4c4c-b2fd-7553ab972f33.png', true, 0),
    ('/uploads/prop-009/16817fe0-1d2f-4755-a699-89b5f7ef1437.png', false, 1),
    ('/uploads/prop-009/d64ba193-4a9d-4bf6-bd83-37e5ba0cfd55.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-009';

-- prop-010: Casa en Maria Auxiliadora (Casa / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-010', 'Casa en Maria Auxiliadora', 'Calle Curupayty 2287', -25.48553, -54.75711, tp.id_tipo_propiedad, e.id_estado, 10111015
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 1, true, true, true, 299 FROM propiedad WHERE codigo = 'prop-010';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 372, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-010' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-010/983c2120-2168-49fc-a106-ceb324e25c4b.png', true, 0),
    ('/uploads/prop-010/3537679a-cc92-4c10-a0c0-bd7db2e0b16b.png', false, 1),
    ('/uploads/prop-010/3d44defb-2aaa-4897-b103-cfdc55746a3d.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-010';

-- prop-011: Casa en Virgen Del Carmen (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-011', 'Casa en Virgen Del Carmen', 'Ruta Internacional 1428', -25.47525, -54.75835, tp.id_tipo_propiedad, e.id_estado, 10111004
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 4, 2, true, true, true, 217 FROM propiedad WHERE codigo = 'prop-011';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 62000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-011' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-011/fa3f8cc7-11f7-469f-a965-eb18b71f24f4.png', true, 0),
    ('/uploads/prop-011/fd203a0e-96e7-4233-ba7f-ae768db5d410.png', false, 1),
    ('/uploads/prop-011/2900183d-45a6-4282-a769-88bc545d3dc8.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-011';

-- prop-012: Casa en Calle 14 Monday (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-012', 'Casa en Calle 14 Monday', 'Av. San Blas 371', -25.54501, -54.74456, tp.id_tipo_propiedad, e.id_estado, 10116200
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 3, true, true, true, 200 FROM propiedad WHERE codigo = 'prop-012';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 62000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-012' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-012/a9dba0ca-557b-4936-8549-02e90a91a3a3.png', true, 0),
    ('/uploads/prop-012/bc515289-7ef4-4cbd-9e63-ffc468f7fff4.png', false, 1),
    ('/uploads/prop-012/fc7a9014-25ee-4cf1-9e08-b0bf227ac0c9.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-012';

-- prop-013: Departamento en Parana Country Club (Departamento / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-013', 'Departamento en Parana Country Club', 'Av. Del Lago 1979', -25.47921, -54.62177, tp.id_tipo_propiedad, e.id_estado, 10051023
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 3, true, true, true, 72 FROM propiedad WHERE codigo = 'prop-013';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 70000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-013' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-013/c0f9acd3-5742-41a0-a2d3-58ba38c5056f.png', true, 0),
    ('/uploads/prop-013/ad1b08d5-25f9-47fd-b1b4-287e659760eb.png', false, 1),
    ('/uploads/prop-013/5941fd9c-3aad-47e7-98b2-7d2aedeb09d7.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-013';

-- prop-014: Departamento en Nuestra Sra De La Asuncion (Departamento / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-014', 'Departamento en Nuestra Sra De La Asuncion', 'Av. Del Lago 2490', -25.40076, -54.6431, tp.id_tipo_propiedad, e.id_estado, 10051007
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 2, true, true, true, 76 FROM propiedad WHERE codigo = 'prop-014';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 42000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-014' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-014/12b5d089-315d-4045-ab1e-a4f4bb73a2e6.png', true, 0),
    ('/uploads/prop-014/d6af547f-4614-45b8-8fe2-45a56657ad5c.png', false, 1),
    ('/uploads/prop-014/86bc6d27-3c42-4f86-995e-2030c721ea69.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-014';

-- prop-015: Departamento en San Antonio (Departamento / Alquiler)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-015', 'Departamento en San Antonio', 'Av. Monseñor Rodríguez 292', -25.55971, -54.60829, tp.id_tipo_propiedad, e.id_estado, 10021001
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 2, 3, true, true, true, 62 FROM propiedad WHERE codigo = 'prop-015';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 252, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-015' AND o.nombre = 'Alquiler';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-015/f6ea7867-1ab2-48ea-8629-df6457aaed7a.png', true, 0),
    ('/uploads/prop-015/b46b6283-24dc-4c50-8612-789c600331b9.png', false, 1),
    ('/uploads/prop-015/435cc7d3-d183-4c58-b345-b9cfdcab0bc7.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-015';

-- prop-016: Casa en San Miguel (Casa / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'prop-016', 'Casa en San Miguel', 'Av. Monseñor Rodríguez 1676', -25.55538, -54.65128, tp.id_tipo_propiedad, e.id_estado, 10021010
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Casa' AND e.nombre = 'Disponible';
INSERT INTO casa (id_propiedad, habitaciones, banos, sala, comedor, cocina, superficie_construida)
SELECT id_propiedad, 3, 3, true, true, true, 294 FROM propiedad WHERE codigo = 'prop-016';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 62000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'prop-016' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/prop-016/90ab572b-450c-4699-be9c-44bfe851a1f2.png', true, 0),
    ('/uploads/prop-016/08a0dd94-cb42-4c77-a59f-3f97933b3c67.png', false, 1),
    ('/uploads/prop-016/83d03865-43df-4e38-a021-829584ea21f0.png', false, 2)
) AS v(url, es_principal, orden) WHERE p.codigo = 'prop-016';

-- terr-001: Terreno en NUESTRA SRA DE LA ASUNCION (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-001', 'Terreno en NUESTRA SRA DE LA ASUNCION', 'NUESTRA SRA DE LA ASUNCION, HERNANDARIAS', -25.40701, -54.640313, tp.id_tipo_propiedad, e.id_estado, 10051007
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 750, false FROM propiedad WHERE codigo = 'terr-001';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 41000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-001' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-001/106ca67f-f87c-4432-a266-87289a0fc25c.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-001';

-- terr-002: Terreno en SAN MIGUEL (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-002', 'Terreno en SAN MIGUEL', 'SAN MIGUEL, PRESIDENTE FRANCO', -25.56093, -54.640562, tp.id_tipo_propiedad, e.id_estado, 10021010
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 1000, false FROM propiedad WHERE codigo = 'terr-002';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 46000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-002' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-002/dbc41ee5-357e-411b-9e1e-34737dad64ec.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-002';

-- terr-003: Terreno en SAN ANTONIO (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-003', 'Terreno en SAN ANTONIO', 'SAN ANTONIO, PRESIDENTE FRANCO', -25.560088, -54.61054, tp.id_tipo_propiedad, e.id_estado, 10021001
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 1000, false FROM propiedad WHERE codigo = 'terr-003';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 54000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-003' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-003/fdb86a25-0fc2-4c45-98ae-9cd30215785a.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-003';

-- terr-004: Terreno en DOMINGO SABIO (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-004', 'Terreno en DOMINGO SABIO', 'DOMINGO SABIO, MINGA GUAZÚ', -25.484932, -54.762922, tp.id_tipo_propiedad, e.id_estado, 10111016
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 300, false FROM propiedad WHERE codigo = 'terr-004';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 23000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-004' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-004/efc97db1-01b1-48a2-b6d9-6396dfd055a8.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-004';

-- terr-005: Terreno en MARIA AUXILIADORA (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-005', 'Terreno en MARIA AUXILIADORA', 'MARIA AUXILIADORA, MINGA GUAZÚ', -25.486921, -54.758836, tp.id_tipo_propiedad, e.id_estado, 10111015
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 750, false FROM propiedad WHERE codigo = 'terr-005';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 38000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-005' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-005/5be4827a-977d-463e-9639-11259b489e23.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-005';

-- terr-006: Terreno en VIRGEN DEL CARMEN (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-006', 'Terreno en VIRGEN DEL CARMEN', 'VIRGEN DEL CARMEN, MINGA GUAZÚ', -25.474217, -54.74545, tp.id_tipo_propiedad, e.id_estado, 10111004
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 400, false FROM propiedad WHERE codigo = 'terr-006';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 24000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-006' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-006/cba22d76-e3f3-4fde-b399-ba347b754d76.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-006';

-- terr-007: Terreno en CIUDAD NUEVA (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-007', 'Terreno en CIUDAD NUEVA', 'CIUDAD NUEVA, CIUDAD DEL ESTE', -25.523153, -54.67457, tp.id_tipo_propiedad, e.id_estado, 10011024
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 750, false FROM propiedad WHERE codigo = 'terr-007';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 58000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-007' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-007/026d06c0-1d67-423a-af2c-34ad199d4d56.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-007';

-- terr-008: Terreno en SAN JUAN (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-008', 'Terreno en SAN JUAN', 'SAN JUAN, CIUDAD DEL ESTE', -25.475909, -54.670143, tp.id_tipo_propiedad, e.id_estado, 10011003
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 1000, false FROM propiedad WHERE codigo = 'terr-008';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 48000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-008' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-008/079e68f3-c3c5-48f3-b86c-b96d59fd75f8.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-008';

-- terr-009: Terreno en SAN JOSE (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-009', 'Terreno en SAN JOSE', 'SAN JOSE, CIUDAD DEL ESTE', -25.50749, -54.651267, tp.id_tipo_propiedad, e.id_estado, 10011014
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 900, false FROM propiedad WHERE codigo = 'terr-009';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 61000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-009' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-009/4936f032-254a-493c-8ab0-6fdefe279ce6.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-009';

-- terr-010: Terreno en AREA 1 (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-010', 'Terreno en AREA 1', 'AREA 1, CIUDAD DEL ESTE', -25.519365, -54.638091, tp.id_tipo_propiedad, e.id_estado, 10011013
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 400, false FROM propiedad WHERE codigo = 'terr-010';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 32000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-010' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-010/90902fd6-9c09-4b73-93dc-f3f09e51db73.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-010';

-- terr-011: Terreno en PARANA COUNTRY CLUB (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-011', 'Terreno en PARANA COUNTRY CLUB', 'PARANA COUNTRY CLUB, HERNANDARIAS', -25.492722, -54.601243, tp.id_tipo_propiedad, e.id_estado, 10051023
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 1200, false FROM propiedad WHERE codigo = 'terr-011';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 85000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-011' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-011/afb507e7-d719-4667-b780-1ad9a04b7e8f.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-011';

-- terr-012: Terreno en CALLE 14 MONDAY (Terreno / Venta)
INSERT INTO propiedad (codigo, titulo, direccion, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio)
SELECT 'terr-012', 'Terreno en CALLE 14 MONDAY', 'CALLE 14 MONDAY, MINGA GUAZÚ', -25.561758, -54.747106, tp.id_tipo_propiedad, e.id_estado, 10116200
  FROM tipo_propiedad tp, estado_propiedad e WHERE tp.nombre = 'Terreno' AND e.nombre = 'Disponible';
INSERT INTO terreno (id_propiedad, superficie, tiene_titulo)
SELECT id_propiedad, 600, false FROM propiedad WHERE codigo = 'terr-012';
INSERT INTO operacion_propiedad (id_propiedad, id_operacion, precio, moneda)
SELECT p.id_propiedad, o.id_operacion, 43000, 'PYG' FROM propiedad p, operacion o WHERE p.codigo = 'terr-012' AND o.nombre = 'Venta';
INSERT INTO imagen (id_propiedad, url, es_principal, orden)
SELECT p.id_propiedad, v.url, v.es_principal, v.orden FROM propiedad p, (VALUES
    ('/uploads/terr-012/259e225e-9b65-49be-a191-fd2f5e85538b.png', true, 0)
) AS v(url, es_principal, orden) WHERE p.codigo = 'terr-012';

COMMIT;
