# Migraciones de la base de datos

Scripts que se aplican **una sola vez** a una base existente. Una base creada desde cero
(`docker compose up` con el volumen vacío) ya nace con estos cambios por la carga inicial de
`database/seed/`, así que no necesita ejecutarlos.

## 001 — Fotos de demostración al almacenamiento compartido (Fase 15C)

Las 66 fotos de demostración estaban en `frontend/public/images/{propiedades,terrenos}` y la
tabla `imagen` las referenciaba como `/images/...`. Esta migración las lleva al almacenamiento
compartido del backend (el volumen Docker `sn-uploads`, montado como `UPLOAD_DIR`) con el mismo
formato que las fotos subidas desde el panel: `/uploads/<codigo>/<uuid>.png`.

No cambia la estructura de la tabla ni el backend: solo el texto de `imagen.url`. La relación con
la propiedad (`id_propiedad`), el `orden`, la imagen principal y la descripción se conservan, y
la transacción lo verifica.

| Archivo | Qué hace | ¿Modifica? |
|---------|----------|------------|
| `database/seed/imagenes/MAPEO.csv` | Para cada foto: código, orden, principal, URL anterior, URL nueva, SHA-256 y tamaño | — |
| `database/seed/imagenes/SHA256SUMS` | Huellas en el formato de `sha256sum -c` | — |
| `database/seed/imagenes/<codigo>/<uuid>.png` | Copias de los originales (fuente de la carga inicial) | — |
| `001_verificar_originales.ps1` | Originales del frontend = copias = huellas del mapeo | No |
| `001_copiar_al_volumen.sh` | Copia al volumen **sin sobrescribir** y verifica las 66 huellas dentro | Solo agrega archivos |
| `001_verificar_imagenes.sql` | Estado de la base frente al mapeo, con la lista de diferencias | No (ROLLBACK) |
| `001_imagenes_a_uploads.sql` | Actualiza las 66 URLs en **una transacción** con verificaciones | Sí, todo o nada |
| `001_verificar_http.ps1` | Cada URL de la base responde 200 y las 66 llegan con su huella | No |
| `001_revertir_imagenes.sql` | Devuelve las 66 URLs a `/images/...`, con las mismas verificaciones | Sí, todo o nada |

### Cuándo se detiene (sin modificar nada)

- El mapeo no tiene 66 filas, o las URLs de destino ya existen (la migración ya fue aplicada).
- Alguna imagen no coincide en URL, propiedad, orden o principal (por ejemplo, porque se cambió
  la principal o se eliminó una foto de demostración desde el panel). `001_verificar_imagenes.sql`
  muestra exactamente cuáles.
- Una copia o un archivo del volumen no coincide con su huella. Nunca se sobrescribe nada: hay que
  revisar ese archivo antes de continuar.

### Reversión

| Situación | Cómo volver atrás |
|-----------|-------------------|
| Mientras las fotos sigan en `frontend/public/images` | Ejecutar `001_revertir_imagenes.sql`. Las copias en el volumen pueden quedar: no molestan |
| En cualquier momento | Restaurar los respaldos tomados antes de migrar: `pg_restore` de la base y el `.tgz` del volumen |
| Después de retirar las fotos del frontend | Recuperarlas desde Git (siguen en el historial) o desde `database/seed/imagenes` con `MAPEO.csv`, y luego revertir |

El procedimiento completo, paso a paso, está en la entrega de la Fase 15C.
