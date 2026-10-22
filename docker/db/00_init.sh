#!/bin/bash
# =====================================================================
# Inicialización de la base de datos (solo la PRIMERA vez que se crea el volumen)
# =====================================================================
# La imagen de PostgreSQL ejecuta los archivos de /docker-entrypoint-initdb.d
# únicamente cuando el directorio de datos está vacío. Este script recorre
# /database (montado desde la carpeta del repositorio) en orden:
#   1. schema/*.sql  -> extensiones, tablas, índices y triggers
#   2. seed/*        -> datos iniciales (.sql con psql, .sh con bash)
# Cualquier error detiene la inicialización (ON_ERROR_STOP).
# =====================================================================
set -euo pipefail

ejecutar_sql() {
    echo "[init] Ejecutando $1"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --quiet -f "$1"
}

for archivo in /database/schema/*.sql; do
    ejecutar_sql "$archivo"
done

for archivo in /database/seed/*; do
    case "$archivo" in
        *.sql) ejecutar_sql "$archivo" ;;
        *.sh)  echo "[init] Ejecutando $archivo"; bash "$archivo" ;;
    esac
done

echo "[init] Base de datos inicializada correctamente"
