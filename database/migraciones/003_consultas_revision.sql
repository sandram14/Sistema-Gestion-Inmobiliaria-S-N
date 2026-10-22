-- =====================================================================
-- Migración 003: resultado, revisión interna y recomendaciones de las consultas del chat IA
-- =====================================================================
-- Agrega (no modifica ni borra datos):
--   * consulta.resultado: RESPONDIDA, DERIVADA_ASESOR, SIN_SERVICIO o BLOQUEADA.
--     Vacío en las consultas existentes ("no registrado"): no se deduce ni se inventa.
--   * consulta.estado_revision (PENDIENTE o REVISADA), revisada_por y revisada_en:
--     revisión INTERNA del equipo, no indica si el cliente fue atendido.
--   * tabla consulta_recomendacion (consulta <-> propiedad recomendada). Vacía para las
--     consultas existentes: en su momento no se guardaban las recomendaciones.
--
-- Una transacción: comprueba la estructura antes y, después, que cada consulta conserve
-- exactamente su pregunta, respuesta, propiedad, sesión y fecha. Si algo no coincide, ROLLBACK.
--
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/003_consultas_revision.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;

CREATE TEMP TABLE consultas_antes ON COMMIT DROP AS
SELECT id_consulta, id_sesion, md5(consulta) AS h_consulta, md5(coalesce(respuesta, '<null>')) AS h_respuesta,
       id_propiedad, fecha_hora
  FROM consulta;

DO $$
DECLARE n integer;
BEGIN
    SELECT count(*) INTO n FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'consulta'
       AND column_name IN ('id_consulta', 'id_sesion', 'consulta', 'respuesta', 'id_propiedad', 'fecha_hora');
    IF n <> 6 THEN
        RAISE EXCEPTION 'La tabla consulta no tiene la estructura esperada (% de 6 columnas). Nada fue modificado.', n;
    END IF;
    SELECT count(*) INTO n FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'consulta'
       AND column_name IN ('resultado', 'estado_revision', 'revisada_por', 'revisada_en');
    IF n > 0 OR EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'consulta_recomendacion') THEN
        RAISE EXCEPTION 'La migración 003 parece aplicada (ya existen columnas o la tabla nueva). Nada fue modificado.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'usuario' AND column_name = 'debe_cambiar_password') THEN
        RAISE EXCEPTION 'Falta la migración 002 (aplíquela primero). Nada fue modificado.';
    END IF;
END $$;

ALTER TABLE consulta
    ADD COLUMN resultado varchar(20)
        CONSTRAINT ck_consulta_resultado CHECK (resultado IN ('RESPONDIDA', 'DERIVADA_ASESOR', 'SIN_SERVICIO', 'BLOQUEADA')),
    ADD COLUMN estado_revision varchar(10) NOT NULL DEFAULT 'PENDIENTE'
        CONSTRAINT ck_consulta_revision CHECK (estado_revision IN ('PENDIENTE', 'REVISADA')),
    ADD COLUMN revisada_por integer REFERENCES usuario (id_usuario) ON DELETE SET NULL,
    ADD COLUMN revisada_en timestamptz;

CREATE TABLE consulta_recomendacion (
    id_consulta  bigint NOT NULL REFERENCES consulta (id_consulta) ON DELETE CASCADE,
    id_propiedad bigint NOT NULL REFERENCES propiedad (id_propiedad) ON DELETE CASCADE,
    PRIMARY KEY (id_consulta, id_propiedad)
);
CREATE INDEX ix_consulta_recomendacion_propiedad ON consulta_recomendacion (id_propiedad);
CREATE INDEX ix_consulta_revision ON consulta (estado_revision);

DO $$
DECLARE n integer;
BEGIN
    SELECT count(*) INTO n FROM consultas_antes a JOIN consulta c USING (id_consulta)
     WHERE c.id_sesion = a.id_sesion AND md5(c.consulta) = a.h_consulta
       AND md5(coalesce(c.respuesta, '<null>')) = a.h_respuesta
       AND c.id_propiedad IS NOT DISTINCT FROM a.id_propiedad AND c.fecha_hora = a.fecha_hora;
    IF n <> (SELECT count(*) FROM consultas_antes) OR (SELECT count(*) FROM consulta) <> (SELECT count(*) FROM consultas_antes) THEN
        RAISE EXCEPTION 'Las consultas no coinciden con el estado anterior. Se deshace todo.';
    END IF;
    IF EXISTS (SELECT 1 FROM consulta WHERE resultado IS NOT NULL OR estado_revision <> 'PENDIENTE' OR revisada_por IS NOT NULL) THEN
        RAISE EXCEPTION 'Las consultas existentes deben quedar sin resultado y pendientes. Se deshace todo.';
    END IF;
    IF EXISTS (SELECT 1 FROM consulta_recomendacion) THEN
        RAISE EXCEPTION 'No debe haber recomendaciones para las consultas existentes. Se deshace todo.';
    END IF;
    RAISE NOTICE 'OK: migración 003 aplicada. % consultas conservadas sin cambios; sin recomendaciones ni resultados inventados.',
        (SELECT count(*) FROM consulta);
END $$;

COMMIT;
