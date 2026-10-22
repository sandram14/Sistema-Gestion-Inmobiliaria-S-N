-- =====================================================================
-- Migración 006: gestión del sitio web (contenido de Inicio, Servicios, Nosotros y Contacto, y propiedades destacadas)
-- =====================================================================
-- Crea dos tablas nuevas. No modifica ninguna tabla ni dato existente.
-- El contenido inicial es el que el sitio muestra hoy (escrito en su código), así que la web se ve igual hasta
-- que alguien edite. Contacto arranca con valores PROVISIONALES: dominio .example (reservado, no llega a nadie)
-- y el número inexistente +595 000 000 000. No hay propiedades destacadas: la portada sigue con 6 al azar.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/006_gestion_sitio.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE conteos_antes ON COMMIT DROP AS
SELECT (SELECT count(*) FROM propiedad) AS p, (SELECT count(*) FROM usuario) AS u, (SELECT count(*) FROM cliente_potencial) AS c;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('seccion_sitio', 'propiedad_destacada')) THEN
        RAISE EXCEPTION 'Las tablas del sitio ya existen: la migración 006 parece aplicada. Nada fue modificado.';
    END IF;
END $$;
CREATE TABLE seccion_sitio (
    seccion        varchar(12) PRIMARY KEY CONSTRAINT ck_seccion_sitio CHECK (seccion IN ('INICIO', 'SERVICIOS', 'NOSOTROS', 'CONTACTO')),
    contenido      text NOT NULL CONSTRAINT ck_seccion_contenido CHECK (contenido IS JSON OBJECT),
    actualizado_en timestamptz NOT NULL DEFAULT now(),
    actualizado_por integer REFERENCES usuario (id_usuario) ON DELETE SET NULL
);

-- Propiedades destacadas en la portada (sin duplicar propiedades: solo las referencia)
CREATE TABLE propiedad_destacada (
    id_propiedad bigint PRIMARY KEY REFERENCES propiedad (id_propiedad) ON DELETE CASCADE,
    orden        smallint NOT NULL CONSTRAINT ck_destacada_orden CHECK (orden BETWEEN 1 AND 99),
    CONSTRAINT uq_destacada_orden UNIQUE (orden) DEFERRABLE INITIALLY DEFERRED
);

INSERT INTO seccion_sitio (seccion, contenido) VALUES
    ('INICIO', $json${"imagenPortada": null, "etiqueta": "Más de 12.000 propiedades disponibles", "titulo": "Encuentra el lugar donde comenzará tu próxima historia.", "subtitulo": "S&N Inmobiliaria conecta compradores e inversores con las mejores propiedades, con tecnología que simplifica cada decisión.", "nosotrosTitulo": "Por qué elegirnos como inmobiliaria.", "nosotrosTexto": "Conocemos cada barrio de Alto Paraná casa por casa. Acompañamos todo el proceso —de la primera visita a la entrega de llaves— con información real, transparente y actualizada.", "nosotrosImagen": null}$json$),
    ('SERVICIOS', $json${"titulo": "Nuestros Servicios", "subtitulo": "Una solución para cada necesidad.", "servicios": [{"titulo": "Compra y Venta", "texto": "Te asesoramos durante todo el proceso de adquisición o venta de inmuebles."}, {"titulo": "Alquileres", "texto": "Encontrá la propiedad ideal para alquilar de forma rápida y confiable."}, {"titulo": "Terrenos", "texto": "Encontrá terrenos para construir, invertir o desarrollar tu próximo proyecto."}, {"titulo": "Asesoría Inmobiliaria", "texto": "Te acompañamos para tomar decisiones seguras y encontrar la mejor opción según tus necesidades."}], "bloqueTitulo": "Encontrá el terreno o la propiedad ideal para tu proyecto.", "bloqueTexto": "Contamos con terrenos y propiedades ubicados en diferentes zonas de Alto Paraná, ideales para vivienda, inversión o desarrollo comercial."}$json$),
    ('NOSOTROS', $json${"introduccion": "Somos una plataforma enfocada en conectar a las personas con las mejores oportunidades inmobiliarias, brindando un servicio transparente, moderno y adaptado a tus necesidades.", "historiaTitulo": "Un equipo que conoce Alto Paraná de cerca.", "historiaTexto": "Nacimos en Ciudad del Este con un objetivo simple: que comprar, vender o alquilar una propiedad sea un proceso claro, sin sorpresas. Hoy acompañamos a familias e inversores en toda la región, barrio por barrio.", "imagen": null, "cifras": [{"valor": "+150", "etiqueta": "Propiedades"}, {"valor": "+12", "etiqueta": "Barrios cubiertos"}, {"valor": "+6", "etiqueta": "Años de experiencia"}], "valores": [{"titulo": "Confianza", "texto": "Construimos cada relación sobre información real y compromisos que cumplimos."}, {"titulo": "Transparencia", "texto": "Sin letra chica: precios, estados y condiciones claras desde el primer contacto."}, {"titulo": "Compromiso", "texto": "Acompañamos todo el proceso, de la primera visita a la entrega de llaves."}, {"titulo": "Innovación", "texto": "Herramientas como el mapa por barrios para que encuentres más rápido."}], "razones": [{"titulo": "Conocemos cada barrio", "texto": "Cubrimos Alto Paraná zona por zona, con información actualizada de cada barrio."}, {"titulo": "Búsqueda inteligente", "texto": "Filtrá por mapa, precio y tipo de propiedad hasta encontrar justo lo que buscás."}, {"titulo": "Acompañamiento real", "texto": "Un agente especializado te guía en cada paso, sin apuros ni letra chica."}]}$json$),
    ('CONTACTO', $json${"nombreEmpresa": "S&N Inmobiliaria", "descripcion": "La plataforma inmobiliaria más avanzada de Alto Paraná para comprar, alquilar e invertir con confianza.", "direccion": "", "ciudad": "Ciudad del Este", "email": "contacto@sn-inmobiliaria.example", "telefono": "+595 000 000 000", "whatsapp": "595000000000", "horario": ""}$json$);

DO $$
BEGIN
    IF (SELECT count(*) FROM seccion_sitio) <> 4 OR EXISTS (SELECT 1 FROM propiedad_destacada) THEN
        RAISE EXCEPTION 'El contenido inicial no es el esperado. Se deshace todo.';
    END IF;
    IF (SELECT p FROM conteos_antes) <> (SELECT count(*) FROM propiedad) OR (SELECT u FROM conteos_antes) <> (SELECT count(*) FROM usuario)
       OR (SELECT c FROM conteos_antes) <> (SELECT count(*) FROM cliente_potencial) THEN
        RAISE EXCEPTION 'Cambiaron datos existentes. Se deshace todo.';
    END IF;
    RAISE NOTICE 'OK: migración 006 aplicada. 4 secciones con el contenido actual del sitio; sin propiedades destacadas.';
END $$;
COMMIT;
