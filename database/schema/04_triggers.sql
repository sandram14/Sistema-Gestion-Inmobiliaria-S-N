-- =====================================================================
-- S&N Inmobiliaria - 04: Reglas de negocio garantizadas por la base
-- =====================================================================
-- El backend (capa Service) valida estas mismas reglas para devolver
-- mensajes claros; los triggers son la última barrera por si algún
-- dato llega por otro camino (scripts, consola, errores de programación).
--
-- Todas las violaciones usan ERRCODE 23514 (check_violation) y un nombre
-- de regla en CONSTRAINT, para que el backend pueda identificarlas.
-- =====================================================================

-- Devuelve el nombre del tipo de una propiedad ('Casa' / 'Terreno')
CREATE FUNCTION fn_tipo_de_propiedad(p_id_propiedad bigint)
RETURNS varchar
LANGUAGE sql STABLE AS $$
    SELECT tp.nombre
      FROM propiedad p
      JOIN tipo_propiedad tp ON tp.id_tipo_propiedad = p.id_tipo_propiedad
     WHERE p.id_propiedad = p_id_propiedad;
$$;

-- ---------------------------------------------------------------------
-- Regla 1: un Terreno solamente puede tener operación Venta
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_validar_operacion_propiedad()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF fn_tipo_de_propiedad(NEW.id_propiedad) = 'Terreno'
       AND (SELECT nombre FROM operacion WHERE id_operacion = NEW.id_operacion) = 'Alquiler' THEN
        RAISE EXCEPTION 'Un terreno solamente puede tener la operación Venta'
              USING ERRCODE = 'check_violation', CONSTRAINT = 'regla_terreno_solo_venta';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_operacion_propiedad_validar
    BEFORE INSERT OR UPDATE ON operacion_propiedad
    FOR EACH ROW EXECUTE FUNCTION fn_validar_operacion_propiedad();

-- ---------------------------------------------------------------------
-- Regla 2: los datos de CASA solo pueden pertenecer a una propiedad de tipo Casa,
--          y los de TERRENO solo a una de tipo Terreno (nunca ambos)
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_validar_casa()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF fn_tipo_de_propiedad(NEW.id_propiedad) IS DISTINCT FROM 'Casa' THEN
        RAISE EXCEPTION 'La propiedad % no es de tipo Casa', NEW.id_propiedad
              USING ERRCODE = 'check_violation', CONSTRAINT = 'regla_subtipo_casa';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_casa_validar
    BEFORE INSERT OR UPDATE OF id_propiedad ON casa
    FOR EACH ROW EXECUTE FUNCTION fn_validar_casa();

CREATE FUNCTION fn_validar_terreno()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF fn_tipo_de_propiedad(NEW.id_propiedad) IS DISTINCT FROM 'Terreno' THEN
        RAISE EXCEPTION 'La propiedad % no es de tipo Terreno', NEW.id_propiedad
              USING ERRCODE = 'check_violation', CONSTRAINT = 'regla_subtipo_terreno';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_terreno_validar
    BEFORE INSERT OR UPDATE OF id_propiedad ON terreno
    FOR EACH ROW EXECUTE FUNCTION fn_validar_terreno();

-- ---------------------------------------------------------------------
-- Regla 3: cambiar el tipo de una propiedad no puede dejar datos incoherentes
--          (por ejemplo, pasar a Terreno teniendo datos de Casa o una operación Alquiler).
--          Para cambiar el tipo, el backend primero elimina los datos del subtipo anterior
--          dentro de la misma transacción.
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_validar_cambio_tipo()
RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_nuevo_tipo varchar;
BEGIN
    SELECT nombre INTO v_nuevo_tipo
      FROM tipo_propiedad WHERE id_tipo_propiedad = NEW.id_tipo_propiedad;

    IF v_nuevo_tipo = 'Terreno' THEN
        IF EXISTS (SELECT 1 FROM casa WHERE id_propiedad = NEW.id_propiedad) THEN
            RAISE EXCEPTION 'La propiedad % tiene datos de Casa; no puede pasar a Terreno', NEW.id_propiedad
                  USING ERRCODE = 'check_violation', CONSTRAINT = 'regla_subtipo_casa';
        END IF;
        IF EXISTS (SELECT 1
                     FROM operacion_propiedad op
                     JOIN operacion o ON o.id_operacion = op.id_operacion
                    WHERE op.id_propiedad = NEW.id_propiedad AND o.nombre = 'Alquiler') THEN
            RAISE EXCEPTION 'La propiedad % tiene operación Alquiler; un terreno solamente puede venderse', NEW.id_propiedad
                  USING ERRCODE = 'check_violation', CONSTRAINT = 'regla_terreno_solo_venta';
        END IF;
    ELSIF v_nuevo_tipo = 'Casa' THEN
        IF EXISTS (SELECT 1 FROM terreno WHERE id_propiedad = NEW.id_propiedad) THEN
            RAISE EXCEPTION 'La propiedad % tiene datos de Terreno; no puede pasar a Casa', NEW.id_propiedad
                  USING ERRCODE = 'check_violation', CONSTRAINT = 'regla_subtipo_terreno';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_propiedad_cambio_tipo
    BEFORE UPDATE OF id_tipo_propiedad ON propiedad
    FOR EACH ROW
    WHEN (OLD.id_tipo_propiedad IS DISTINCT FROM NEW.id_tipo_propiedad)
    EXECUTE FUNCTION fn_validar_cambio_tipo();

-- ---------------------------------------------------------------------
-- Regla 4: la ubicación de la propiedad debe estar dentro del barrio asignado.
--          Se usa ST_Covers (incluye el borde del polígono) en lugar de
--          ST_Contains (lo excluye), para no rechazar puntos justo en el límite.
--          Si el barrio no existe, la clave foránea se encarga del error.
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_validar_ubicacion_en_barrio()
RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_geom geometry;
BEGIN
    SELECT geom INTO v_geom FROM barrio WHERE id_barrio = NEW.id_barrio;

    IF v_geom IS NOT NULL AND NOT ST_Covers(
           v_geom,
           ST_SetSRID(ST_MakePoint(NEW.longitud::double precision, NEW.latitud::double precision), 4326)) THEN
        RAISE EXCEPTION 'La ubicación (%, %) no está dentro del barrio %', NEW.latitud, NEW.longitud, NEW.id_barrio
              USING ERRCODE = 'check_violation', CONSTRAINT = 'regla_ubicacion_en_barrio';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_propiedad_ubicacion
    BEFORE INSERT OR UPDATE OF latitud, longitud, id_barrio ON propiedad
    FOR EACH ROW EXECUTE FUNCTION fn_validar_ubicacion_en_barrio();

-- ---------------------------------------------------------------------
-- Mantenimiento: fecha de última modificación
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_actualizar_fecha_modificacion()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.actualizado_en := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_propiedad_actualizado_en
    BEFORE UPDATE ON propiedad
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_modificacion();
