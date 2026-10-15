-- =====================================================================
-- 008: palabras destacadas en tres títulos del contenido del sitio (solo datos, sin cambios de estructura)
-- =====================================================================
-- El sitio muestra en verde e itálica lo que está entre asteriscos (*así*). Este script marca las mismas
-- palabras que el diseño destacaba antes, SOLO si cada título conserva su texto original: si ya lo editaste
-- desde el panel, no lo toca. Se puede ejecutar más de una vez sin efecto adicional.
-- Uso: docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/008_titulos_destacados.sql
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
UPDATE seccion_sitio SET contenido = jsonb_set(contenido::jsonb, '{titulo}', to_jsonb('Encuentra el lugar donde comenzará *tu próxima historia.*'::text))::text
 WHERE seccion = 'INICIO' AND contenido::jsonb ->> 'titulo' = 'Encuentra el lugar donde comenzará tu próxima historia.';
UPDATE seccion_sitio SET contenido = jsonb_set(contenido::jsonb, '{bloqueTitulo}', to_jsonb('Encontrá el terreno o la propiedad *ideal para tu proyecto.*'::text))::text
 WHERE seccion = 'SERVICIOS' AND contenido::jsonb ->> 'bloqueTitulo' = 'Encontrá el terreno o la propiedad ideal para tu proyecto.';
UPDATE seccion_sitio SET contenido = jsonb_set(contenido::jsonb, '{historiaTitulo}', to_jsonb('Un equipo que conoce *Alto Paraná de cerca.*'::text))::text
 WHERE seccion = 'NOSOTROS' AND contenido::jsonb ->> 'historiaTitulo' = 'Un equipo que conoce Alto Paraná de cerca.';
COMMIT;
SELECT seccion, CASE seccion WHEN 'INICIO' THEN contenido::jsonb ->> 'titulo'
                            WHEN 'SERVICIOS' THEN contenido::jsonb ->> 'bloqueTitulo'
                            ELSE contenido::jsonb ->> 'historiaTitulo' END AS titulo_destacado
  FROM seccion_sitio WHERE seccion IN ('INICIO', 'SERVICIOS', 'NOSOTROS') ORDER BY seccion;
