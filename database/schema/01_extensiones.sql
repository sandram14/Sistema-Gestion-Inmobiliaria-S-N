-- =====================================================================
-- S&N Inmobiliaria - 01: Extensiones
-- =====================================================================
-- postgis  : tipos y funciones geográficas (polígonos de barrios, puntos).
-- pgcrypto : crypt()/gen_salt('bf') para generar el hash BCrypt del
--            administrador inicial sin escribir contraseñas en el repositorio.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
