-- =====================================================================
-- Reversión de la migración 001: /uploads/... vuelve a /images/...
-- =====================================================================
-- Devuelve las 66 imágenes a sus URLs originales. Solo sirve mientras existan los
-- archivos en frontend/public/images (es decir, antes del último paso de la Fase 15C).
--
-- Todo ocurre en UNA transacción. Antes de cambiar nada se verifica que cada imagen
-- exista exactamente una vez, con la propiedad, el orden y la condición de principal
-- registrados en el mapeo. Después del cambio se verifica que:
--   * se actualizaron exactamente 66 filas;
--   * ninguna fila cambió de propiedad, orden, principal ni descripción;
--   * no se tocó ninguna otra imagen (por ejemplo, las subidas desde el panel);
--   * la cantidad total de imágenes es la misma.
-- Si algo no coincide, se lanza un error y PostgreSQL deshace TODO (ROLLBACK).
--
-- Uso (desde la raíz del repositorio; el contenedor monta database/ en /database):
--   docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/001_revertir_imagenes.sql
-- =====================================================================

\set ON_ERROR_STOP on
BEGIN;

CREATE TEMP TABLE mapeo (
    codigo        text     NOT NULL,
    orden         smallint NOT NULL,
    es_principal  boolean  NOT NULL,
    url_anterior  text     NOT NULL UNIQUE,
    url_nueva     text     NOT NULL UNIQUE
) ON COMMIT DROP;

-- Generado desde database/seed/imagenes/MAPEO.csv (66 filas)
INSERT INTO mapeo VALUES
    ('prop-001', 0, true, '/images/propiedades/prop-001/1.png', '/uploads/prop-001/0a4c5cd8-14cc-4bbc-a4d8-0f7bc29c541d.png'),
    ('prop-001', 1, false, '/images/propiedades/prop-001/2.png', '/uploads/prop-001/9b6cdf5a-ad00-4faf-88b5-06a081d64632.png'),
    ('prop-001', 2, false, '/images/propiedades/prop-001/3.png', '/uploads/prop-001/45595d51-cdfc-4b11-9b4f-2a0f8df042d8.png'),
    ('prop-001', 3, false, '/images/propiedades/prop-001/4.png', '/uploads/prop-001/0ca0f1e2-3507-40c5-98bc-bc787f579916.png'),
    ('prop-001', 4, false, '/images/propiedades/prop-001/5.png', '/uploads/prop-001/7dd2e726-6743-433c-b0f5-7ee56c6ccffd.png'),
    ('prop-002', 0, true, '/images/propiedades/prop-002/1.png', '/uploads/prop-002/c981819c-fcb0-42d2-b7bb-60f4c91f50ef.png'),
    ('prop-002', 1, false, '/images/propiedades/prop-002/2.png', '/uploads/prop-002/888f85e6-e1a7-4940-bd49-253a907a5980.png'),
    ('prop-002', 2, false, '/images/propiedades/prop-002/3.png', '/uploads/prop-002/c068029c-5760-4f54-b24a-4bdf7db26069.png'),
    ('prop-002', 3, false, '/images/propiedades/prop-002/4.png', '/uploads/prop-002/ab788543-ce3b-420a-9b6d-610ff36359cc.png'),
    ('prop-003', 0, true, '/images/propiedades/prop-003/1.png', '/uploads/prop-003/f6f6ad9a-12c0-4c79-a928-063817c6b264.png'),
    ('prop-003', 1, false, '/images/propiedades/prop-003/2.png', '/uploads/prop-003/1f1fd7c1-ae0f-4748-ab70-6e6e08878978.png'),
    ('prop-003', 2, false, '/images/propiedades/prop-003/3.png', '/uploads/prop-003/25330c44-6624-4c76-b393-776c63961547.png'),
    ('prop-003', 3, false, '/images/propiedades/prop-003/4.png', '/uploads/prop-003/24188424-0ca9-4388-a95c-39009118e65e.png'),
    ('prop-004', 0, true, '/images/propiedades/prop-004/1.png', '/uploads/prop-004/fa9f0f45-f5eb-4649-be3d-d5a23058a2d1.png'),
    ('prop-004', 1, false, '/images/propiedades/prop-004/2.png', '/uploads/prop-004/15373603-6c0d-4a35-b4d4-666d1e4eb424.png'),
    ('prop-004', 2, false, '/images/propiedades/prop-004/3.png', '/uploads/prop-004/16655961-0a93-474e-a10e-8d0cd548c622.png'),
    ('prop-005', 0, true, '/images/propiedades/prop-005/1.png', '/uploads/prop-005/af376fd1-d4fe-4b56-a5c9-17a1277eba37.png'),
    ('prop-005', 1, false, '/images/propiedades/prop-005/2.png', '/uploads/prop-005/7e447ff4-7fb9-4dcc-a608-4495ca31d3f4.png'),
    ('prop-005', 2, false, '/images/propiedades/prop-005/3.png', '/uploads/prop-005/64cbdc3b-e105-40d6-827a-85d0a5f3ab8e.png'),
    ('prop-006', 0, true, '/images/propiedades/prop-006/1.png', '/uploads/prop-006/daac2482-a9a1-4758-b882-896797ae6593.png'),
    ('prop-006', 1, false, '/images/propiedades/prop-006/2.png', '/uploads/prop-006/f62c219b-abab-4502-a670-9652e573f833.png'),
    ('prop-006', 2, false, '/images/propiedades/prop-006/3.png', '/uploads/prop-006/8503476e-f91f-4557-a575-7de8d3aeb822.png'),
    ('prop-006', 3, false, '/images/propiedades/prop-006/4.png', '/uploads/prop-006/c470bb83-7db2-4a27-917c-db03ad51fa3c.png'),
    ('prop-007', 0, true, '/images/propiedades/prop-007/1.png', '/uploads/prop-007/bd726b05-f577-49bd-b2db-f0c5cc12392e.png'),
    ('prop-007', 1, false, '/images/propiedades/prop-007/2.png', '/uploads/prop-007/536b9b24-f8c0-41c1-afae-4ce226ef78d9.png'),
    ('prop-007', 2, false, '/images/propiedades/prop-007/3.png', '/uploads/prop-007/406800b4-c18b-43aa-ab02-d68f239c7321.png'),
    ('prop-007', 3, false, '/images/propiedades/prop-007/4.png', '/uploads/prop-007/0a7887c9-74e6-4a7b-9394-9d5c94493857.png'),
    ('prop-008', 0, true, '/images/propiedades/prop-008/1.png', '/uploads/prop-008/0d2da80b-8f2c-4903-84ba-5585d5b0d71a.png'),
    ('prop-008', 1, false, '/images/propiedades/prop-008/2.png', '/uploads/prop-008/60a2abd7-8c0e-46fd-a965-8c9d7b2a2a70.png'),
    ('prop-008', 2, false, '/images/propiedades/prop-008/3.png', '/uploads/prop-008/085333bc-7d2e-44c7-922c-f189c311c63b.png'),
    ('prop-009', 0, true, '/images/propiedades/prop-009/1.png', '/uploads/prop-009/8c9f705d-e45b-4c4c-b2fd-7553ab972f33.png'),
    ('prop-009', 1, false, '/images/propiedades/prop-009/2.png', '/uploads/prop-009/16817fe0-1d2f-4755-a699-89b5f7ef1437.png'),
    ('prop-009', 2, false, '/images/propiedades/prop-009/3.png', '/uploads/prop-009/d64ba193-4a9d-4bf6-bd83-37e5ba0cfd55.png'),
    ('prop-010', 0, true, '/images/propiedades/prop-010/1.png', '/uploads/prop-010/983c2120-2168-49fc-a106-ceb324e25c4b.png'),
    ('prop-010', 1, false, '/images/propiedades/prop-010/2.png', '/uploads/prop-010/3537679a-cc92-4c10-a0c0-bd7db2e0b16b.png'),
    ('prop-010', 2, false, '/images/propiedades/prop-010/3.png', '/uploads/prop-010/3d44defb-2aaa-4897-b103-cfdc55746a3d.png'),
    ('prop-011', 0, true, '/images/propiedades/prop-011/1.png', '/uploads/prop-011/fa3f8cc7-11f7-469f-a965-eb18b71f24f4.png'),
    ('prop-011', 1, false, '/images/propiedades/prop-011/2.png', '/uploads/prop-011/fd203a0e-96e7-4233-ba7f-ae768db5d410.png'),
    ('prop-011', 2, false, '/images/propiedades/prop-011/3.png', '/uploads/prop-011/2900183d-45a6-4282-a769-88bc545d3dc8.png'),
    ('prop-012', 0, true, '/images/propiedades/prop-012/1.png', '/uploads/prop-012/a9dba0ca-557b-4936-8549-02e90a91a3a3.png'),
    ('prop-012', 1, false, '/images/propiedades/prop-012/2.png', '/uploads/prop-012/bc515289-7ef4-4cbd-9e63-ffc468f7fff4.png'),
    ('prop-012', 2, false, '/images/propiedades/prop-012/3.png', '/uploads/prop-012/fc7a9014-25ee-4cf1-9e08-b0bf227ac0c9.png'),
    ('prop-013', 0, true, '/images/propiedades/prop-013/1.png', '/uploads/prop-013/c0f9acd3-5742-41a0-a2d3-58ba38c5056f.png'),
    ('prop-013', 1, false, '/images/propiedades/prop-013/2.png', '/uploads/prop-013/ad1b08d5-25f9-47fd-b1b4-287e659760eb.png'),
    ('prop-013', 2, false, '/images/propiedades/prop-013/3.png', '/uploads/prop-013/5941fd9c-3aad-47e7-98b2-7d2aedeb09d7.png'),
    ('prop-014', 0, true, '/images/propiedades/prop-014/1.png', '/uploads/prop-014/12b5d089-315d-4045-ab1e-a4f4bb73a2e6.png'),
    ('prop-014', 1, false, '/images/propiedades/prop-014/2.png', '/uploads/prop-014/d6af547f-4614-45b8-8fe2-45a56657ad5c.png'),
    ('prop-014', 2, false, '/images/propiedades/prop-014/3.png', '/uploads/prop-014/86bc6d27-3c42-4f86-995e-2030c721ea69.png'),
    ('prop-015', 0, true, '/images/propiedades/prop-015/1.png', '/uploads/prop-015/f6ea7867-1ab2-48ea-8629-df6457aaed7a.png'),
    ('prop-015', 1, false, '/images/propiedades/prop-015/2.png', '/uploads/prop-015/b46b6283-24dc-4c50-8612-789c600331b9.png'),
    ('prop-015', 2, false, '/images/propiedades/prop-015/3.png', '/uploads/prop-015/435cc7d3-d183-4c58-b345-b9cfdcab0bc7.png'),
    ('prop-016', 0, true, '/images/propiedades/prop-016/1.png', '/uploads/prop-016/90ab572b-450c-4699-be9c-44bfe851a1f2.png'),
    ('prop-016', 1, false, '/images/propiedades/prop-016/2.png', '/uploads/prop-016/08a0dd94-cb42-4c77-a59f-3f97933b3c67.png'),
    ('prop-016', 2, false, '/images/propiedades/prop-016/3.png', '/uploads/prop-016/83d03865-43df-4e38-a021-829584ea21f0.png'),
    ('terr-001', 0, true, '/images/terrenos/terr-001/1.png', '/uploads/terr-001/106ca67f-f87c-4432-a266-87289a0fc25c.png'),
    ('terr-002', 0, true, '/images/terrenos/terr-002/1.png', '/uploads/terr-002/dbc41ee5-357e-411b-9e1e-34737dad64ec.png'),
    ('terr-003', 0, true, '/images/terrenos/terr-003/1.png', '/uploads/terr-003/fdb86a25-0fc2-4c45-98ae-9cd30215785a.png'),
    ('terr-004', 0, true, '/images/terrenos/terr-004/1.png', '/uploads/terr-004/efc97db1-01b1-48a2-b6d9-6396dfd055a8.png'),
    ('terr-005', 0, true, '/images/terrenos/terr-005/1.png', '/uploads/terr-005/5be4827a-977d-463e-9639-11259b489e23.png'),
    ('terr-006', 0, true, '/images/terrenos/terr-006/1.png', '/uploads/terr-006/cba22d76-e3f3-4fde-b399-ba347b754d76.png'),
    ('terr-007', 0, true, '/images/terrenos/terr-007/1.png', '/uploads/terr-007/026d06c0-1d67-423a-af2c-34ad199d4d56.png'),
    ('terr-008', 0, true, '/images/terrenos/terr-008/1.png', '/uploads/terr-008/079e68f3-c3c5-48f3-b86c-b96d59fd75f8.png'),
    ('terr-009', 0, true, '/images/terrenos/terr-009/1.png', '/uploads/terr-009/4936f032-254a-493c-8ab0-6fdefe279ce6.png'),
    ('terr-010', 0, true, '/images/terrenos/terr-010/1.png', '/uploads/terr-010/90902fd6-9c09-4b73-93dc-f3f09e51db73.png'),
    ('terr-011', 0, true, '/images/terrenos/terr-011/1.png', '/uploads/terr-011/afb507e7-d719-4667-b780-1ad9a04b7e8f.png'),
    ('terr-012', 0, true, '/images/terrenos/terr-012/1.png', '/uploads/terr-012/259e225e-9b65-49be-a191-fd2f5e85538b.png');

-- Fotografía del estado anterior, para comparar después del cambio
CREATE TEMP TABLE antes ON COMMIT DROP AS
SELECT id_imagen, id_propiedad, url, descripcion, es_principal, orden FROM imagen;

DO $$
DECLARE
    n integer;
    total integer;
BEGIN
    SELECT count(*) INTO n FROM mapeo;
    IF n <> 66 THEN
        RAISE EXCEPTION 'El mapeo debe tener 66 filas y tiene %. Nada fue modificado.', n;
    END IF;

    SELECT count(*) INTO n FROM imagen WHERE url IN (SELECT url_anterior FROM mapeo);
    IF n > 0 THEN
        RAISE EXCEPTION '% imágenes ya tienen las URLs de destino: este paso parece aplicado. Nada fue modificado.', n;
    END IF;

    -- Cada URL de origen: exactamente una fila, con la propiedad, el orden y la principal esperados
    SELECT count(*) INTO n
      FROM mapeo m
      JOIN imagen i    ON i.url = m.url_nueva
      JOIN propiedad p ON p.id_propiedad = i.id_propiedad
     WHERE p.codigo = m.codigo AND i.orden = m.orden AND i.es_principal = m.es_principal;
    IF n <> 66 THEN
        RAISE EXCEPTION 'Solo % de 66 imágenes coinciden con el mapeo (URL, propiedad, orden y principal). '
            'Ejecute 001_verificar_imagenes.sql para ver cuáles. Nada fue modificado.', n;
    END IF;

    -- El cambio
    UPDATE imagen i SET url = m.url_anterior FROM mapeo m WHERE i.url = m.url_nueva;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n <> 66 THEN
        RAISE EXCEPTION 'Se actualizaron % filas en lugar de 66. Se deshace todo.', n;
    END IF;

    -- Asociaciones intactas
    SELECT count(*) INTO n
      FROM antes a JOIN imagen i USING (id_imagen)
     WHERE i.id_propiedad <> a.id_propiedad OR i.orden <> a.orden OR i.es_principal <> a.es_principal
        OR i.descripcion IS DISTINCT FROM a.descripcion;
    IF n > 0 THEN
        RAISE EXCEPTION '% filas cambiaron de propiedad, orden, principal o descripción. Se deshace todo.', n;
    END IF;

    -- Ninguna otra imagen fue modificada
    SELECT count(*) INTO n
      FROM antes a JOIN imagen i USING (id_imagen)
     WHERE a.url NOT IN (SELECT url_nueva FROM mapeo) AND i.url <> a.url;
    IF n > 0 THEN
        RAISE EXCEPTION '% imágenes fuera del mapeo fueron modificadas. Se deshace todo.', n;
    END IF;

    SELECT count(*) INTO total FROM antes;
    SELECT count(*) INTO n FROM imagen;
    IF n <> total THEN
        RAISE EXCEPTION 'La cantidad de imágenes cambió (% antes, % después). Se deshace todo.', total, n;
    END IF;

    RAISE NOTICE 'OK: 66 imágenes devueltas a /images/. Total de imágenes: %.', total;
END $$;

COMMIT;

SELECT count(*) FILTER (WHERE url LIKE '/uploads/%') AS en_uploads,
       count(*) FILTER (WHERE url LIKE '/images/%')  AS en_images,
       count(*)                                      AS total
  FROM imagen;
