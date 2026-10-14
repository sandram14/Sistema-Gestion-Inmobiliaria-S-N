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

## 002 — Roles GERENTE y VENDEDOR, contraseña temporal y responsable (panel, etapa 2A)

Agrega, sin modificar ni borrar datos: los roles `GERENTE` y `VENDEDOR`, la columna
`usuario.debe_cambiar_password` (false para las cuentas existentes) y la columna opcional
`propiedad.id_responsable` (las propiedades existentes quedan sin responsable).

| Archivo | Qué hace | ¿Modifica? |
|---------|----------|------------|
| `002_verificar_roles.sql` | Estructura real de `rol`, roles, columnas nuevas, usuarios y cantidad de propiedades | No |
| `002_roles_y_responsable.sql` | La migración, en una transacción que comprueba la estructura antes y los datos después | Sí, todo o nada |
| `002_revertir_roles.sql` | Quita lo agregado; se detiene si algún usuario tiene rol GERENTE o VENDEDOR | Sí, todo o nada |

**Orden obligatorio:** aplicar la migración **antes** de desplegar el backend 0.13.0, porque
Hibernate valida al arrancar que exista la columna `debe_cambiar_password`.

## 003 — Resultado, revisión interna y recomendaciones de las consultas del chat IA (panel, etapa 3)

Agrega, sin modificar ni borrar datos: en `consulta`, `resultado` (RESPONDIDA, DERIVADA_ASESOR,
SIN_SERVICIO o BLOQUEADA), `estado_revision` (PENDIENTE o REVISADA), `revisada_por` y `revisada_en`;
y la tabla `consulta_recomendacion` (consulta ↔ propiedad recomendada).

Las consultas existentes conservan exactamente su pregunta, respuesta, propiedad, sesión y fecha
(la transacción lo comprueba). Quedan **sin resultado y sin recomendaciones**: en su momento no se
registraban, y no se deducen ni se inventan. El panel las muestra como "No registrado".

La revisión es **interna**: indica si alguien del equipo leyó la conversación, no si el cliente fue atendido.

| Archivo | Qué hace | ¿Modifica? |
|---------|----------|------------|
| `003_verificar_consultas.sql` | Estructura real de `consulta`, columnas nuevas y cantidades | No |
| `003_consultas_revision.sql` | La migración (requiere la 002), con comprobación consulta por consulta | Sí, todo o nada |
| `003_revertir_consultas.sql` | Quita lo agregado; las consultas no se tocan (se pierden resultados, revisiones y recomendaciones registrados) | Sí, todo o nada |

**Orden obligatorio:** aplicar la migración antes de desplegar el backend 0.15.0 (Hibernate valida las columnas al arrancar).

## 004 — Resultado FUERA_DE_AMBITO (panel, etapa 3, entrega 1)

Amplía la restricción `ck_consulta_resultado` con `FUERA_DE_AMBITO`, que el asistente registra desde
ahora cuando la pregunta no trata sobre propiedades ni sobre la inmobiliaria. **No modifica datos**:
las consultas anteriores no se reclasifican y en el análisis figuran como "No registrado".

| Archivo | Qué hace | ¿Modifica? |
|---------|----------|------------|
| `004_verificar_fuera_ambito.sql` | Muestra la restricción actual y los conteos | No |
| `004_resultado_fuera_ambito.sql` | La migración (requiere la 003), comprobando que ningún resultado cambie | Sí, todo o nada |
| `004_revertir_fuera_ambito.sql` | Restaura la restricción; se detiene si ya hay consultas FUERA_DE_AMBITO | Sí, todo o nada |

Aplicarla antes de desplegar el backend 0.16.0.

## 005 — Clientes potenciales (panel, etapa 3, entrega 2)

Crea la tabla `cliente_potencial`. **No modifica ninguna tabla ni dato existente.** Datos personales mínimos:
nombre, teléfono y/o email (al menos uno) y el detalle del interés; no se guarda la IP. Si el registro viene
del chat, la base exige la fecha y la versión del texto de aceptación del tratamiento.

| Archivo | Qué hace | ¿Modifica? |
|---------|----------|------------|
| `005_verificar_clientes.sql` | Comprueba que la tabla no exista y que la 004 esté aplicada | No |
| `005_clientes_potenciales.sql` | Crea la tabla (requiere la 004) y comprueba que no cambie ningún dato | Sí, todo o nada |
| `005_revertir_clientes.sql` | Elimina la tabla; se detiene si tiene registros (son datos personales reales) | Sí, todo o nada |

Aplicarla antes de desplegar el backend 0.17.0.

## 006 — Gestión del sitio web (contenido editable y propiedades destacadas)

Crea `seccion_sitio` (contenido JSON de Inicio, Servicios, Nosotros y Contacto, validado por la base con
`IS JSON OBJECT`) y `propiedad_destacada` (selección y orden de la portada; solo referencia propiedades).
**No modifica ninguna tabla ni dato existente.** El contenido inicial es el que el sitio muestra hoy;
Contacto arranca con valores provisionales (dominio `.example` y `+595 000 000 000`). Sin destacadas: la
portada sigue mostrando 6 al azar. Es la única migración de todo el módulo (también para Servicios y Nosotros).

| Archivo | Qué hace | ¿Modifica? |
|---------|----------|------------|
| `006_verificar_sitio.sql` | Comprueba que las tablas no existan y que la 005 esté aplicada | No |
| `006_gestion_sitio.sql` | Crea las tablas con el contenido actual y comprueba que no cambien otros datos | Sí, todo o nada |
| `006_revertir_sitio.sql` | Elimina ambas tablas (se pierden los textos editados desde el panel) | Sí, todo o nada |

Aplicarla antes de desplegar el backend 0.18.0.
