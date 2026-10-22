#!/bin/bash
# =====================================================================
# S&N Inmobiliaria - Carga inicial 04: Administrador inicial (desarrollo)
# =====================================================================
# Crea el primer usuario administrador con los valores de las variables
# de entorno ADMIN_EMAIL y ADMIN_PASSWORD (definidas en docker/.env).
#
# La contraseña NUNCA se escribe en el repositorio: PostgreSQL genera el
# hash BCrypt (factor 12) con pgcrypto, compatible con jBCrypt del backend.
# Si ADMIN_PASSWORD no está definida, se omite el paso sin error.
# =====================================================================
set -euo pipefail

if [[ -z "${ADMIN_PASSWORD:-}" || -z "${ADMIN_EMAIL:-}" ]]; then
    echo "[04_admin_dev] ADMIN_EMAIL o ADMIN_PASSWORD no definidas: no se crea el administrador."
    exit 0
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v admin_email="$ADMIN_EMAIL" -v admin_password="$ADMIN_PASSWORD" <<'SQL'
INSERT INTO usuario (nombre, apellido, email, password_hash, id_rol)
SELECT 'Administrador', 'S&N', :'admin_email',
       crypt(:'admin_password', gen_salt('bf', 12)),
       (SELECT id_rol FROM rol WHERE nombre = 'ADMIN')
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE lower(email) = lower(:'admin_email'));
SQL

echo "[04_admin_dev] Administrador listo: ${ADMIN_EMAIL}"
