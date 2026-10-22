#!/bin/sh
# =====================================================================
# Migración 001 - Copia las fotos de demostración al volumen compartido
# =====================================================================
# Copia database/seed/imagenes/<codigo>/<uuid>.png al volumen de imágenes
# (sn-uploads, que el backend usa como UPLOAD_DIR) SIN SOBRESCRIBIR nada:
# si un archivo ya existe, no se toca. Después verifica la huella SHA-256
# de las 66 fotos dentro del volumen. Ante cualquier diferencia termina con
# error y explica cuál es. Se puede ejecutar varias veces sin efectos extra.
#
# Uso (desde la raíz del repositorio, en PowerShell):
#   docker run --rm -v sn-uploads:/destino -v "${PWD}\database:/database:ro" alpine:3.20 sh /database/migraciones/001_copiar_al_volumen.sh
# =====================================================================
set -eu

ORIGEN=/database/seed/imagenes
DESTINO=/destino

[ -d "$DESTINO" ] || { echo "ERROR: no se encontró el volumen montado en $DESTINO"; exit 1; }
[ -f "$ORIGEN/SHA256SUMS" ] || { echo "ERROR: falta $ORIGEN/SHA256SUMS"; exit 1; }

esperadas=$(grep -c . "$ORIGEN/SHA256SUMS")
[ "$esperadas" -eq 66 ] || { echo "ERROR: SHA256SUMS debe tener 66 líneas y tiene $esperadas. Nada fue copiado."; exit 1; }

# 1. Las copias de origen deben coincidir con sus huellas antes de copiar nada
cd "$ORIGEN"
if ! tr -d '\r' < SHA256SUMS | sha256sum -c - > /dev/null 2>&1; then
    echo "ERROR: las fotos de database/seed/imagenes no coinciden con SHA256SUMS. Nada fue copiado:"
    tr -d '\r' < SHA256SUMS | sha256sum -c - 2>&1 | grep -v ': OK$' || true
    exit 1
fi

# 2. Copiar sin sobrescribir (solo las carpetas de propiedades; MAPEO.csv y SHA256SUMS no)
copiadas=0
existentes=0
while read -r huella ruta; do
    [ -n "$ruta" ] || continue
    if [ -e "$DESTINO/$ruta" ]; then
        existentes=$((existentes + 1))
    else
        mkdir -p "$DESTINO/$(dirname "$ruta")"
        cp -p "$ORIGEN/$ruta" "$DESTINO/$ruta.tmp"
        mv "$DESTINO/$ruta.tmp" "$DESTINO/$ruta"
        copiadas=$((copiadas + 1))
    fi
done <<EOF
$(tr -d '\r' < "$ORIGEN/SHA256SUMS")
EOF
# 3. Verificar las 66 huellas dentro del volumen
cd "$DESTINO"
if ! tr -d '\r' < "$ORIGEN/SHA256SUMS" | sha256sum -c - > /dev/null 2>&1; then
    echo "ERROR: hay archivos en el volumen que no coinciden con SHA256SUMS:"
    tr -d '\r' < "$ORIGEN/SHA256SUMS" | sha256sum -c - 2>&1 | grep -v ': OK$' || true
    echo "No se sobrescribió nada. Revise esos archivos antes de continuar con la migración."
    exit 1
fi

echo "OK: $copiadas fotos copiadas, $existentes ya existían; las 66 verificadas por SHA-256 en el volumen."
echo "Fotos (de cualquier origen) en el volumen: $(find "$DESTINO" -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.webp' \) | wc -l)"
