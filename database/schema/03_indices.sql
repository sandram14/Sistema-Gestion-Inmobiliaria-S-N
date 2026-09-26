-- =====================================================================
-- S&N Inmobiliaria - 03: Índices
-- =====================================================================

-- Email único sin distinguir mayúsculas (Admin@SN.com = admin@sn.com)
CREATE UNIQUE INDEX uq_usuario_email_lower ON usuario (lower(email));

-- Como máximo UNA imagen principal por propiedad (índice único parcial:
-- solo indexa las filas con es_principal = true)
CREATE UNIQUE INDEX uq_imagen_principal ON imagen (id_propiedad) WHERE es_principal;

-- Índices espaciales (GiST) para consultas geográficas
CREATE INDEX ix_barrio_geom         ON barrio    USING gist (geom);
CREATE INDEX ix_propiedad_ubicacion ON propiedad USING gist (ubicacion);

-- Claves foráneas usadas en filtros y joins
CREATE INDEX ix_barrio_ciudad          ON barrio (id_ciudad);
CREATE INDEX ix_propiedad_barrio       ON propiedad (id_barrio);
CREATE INDEX ix_propiedad_estado       ON propiedad (id_estado);
CREATE INDEX ix_propiedad_tipo         ON propiedad (id_tipo_propiedad);
CREATE INDEX ix_imagen_propiedad_orden ON imagen (id_propiedad, orden);
CREATE INDEX ix_operacion_prop_oper    ON operacion_propiedad (id_operacion);
CREATE INDEX ix_consulta_sesion        ON consulta (id_sesion);
CREATE INDEX ix_consulta_propiedad     ON consulta (id_propiedad);
CREATE INDEX ix_consulta_fecha         ON consulta (fecha_hora);
