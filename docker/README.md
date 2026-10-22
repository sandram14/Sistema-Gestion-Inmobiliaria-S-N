# Docker — S&N Inmobiliaria

Todo el sistema con un solo comando: base de datos, backend, sitio público y panel.

| Servicio | Qué es | Acceso |
|---|---|---|
| `db` | PostgreSQL 16 + PostGIS 3.4 | `localhost:5433` (pruebas con Maven y Tomcat de desarrollo) |
| `imagenes-init` | Copia una vez las fotos iniciales al volumen, **sin sobrescribir** | — (termina y se detiene) |
| `backend` | API Java en Tomcat 10.1 (`APP_ENV=produccion`) | Solo a través de `web` |
| `web` | nginx: sitio en `/`, panel en `/admin/`, `/api/` y `/uploads/` al backend | `http://localhost` (puerto `WEB_PORT`) |

Volúmenes (**externos**: Compose los usa pero nunca los crea ni los borra, ni siquiera con `down -v`):

| Volumen | Contenido |
|---|---|
| `sn-inmobiliaria_pgdata` | Base de datos: propiedades, usuarios, consultas y metadatos de las imágenes |
| `sn-uploads` | Fotos de propiedades (`/uploads/...`), las mismas para el sitio y el panel |

Las claves solo están en `docker/.env` (no se sube a Git y no entra en ninguna imagen).
Todos los comandos se ejecutan **desde la raíz del repositorio**.

---

## Primera vez en un equipo nuevo (instalación desde cero)

```powershell
Copy-Item docker\.env.example docker\.env      # luego editar las contraseñas y GEMINI_API_KEY
docker volume create sn-inmobiliaria_pgdata
docker volume create sn-uploads
docker compose -f docker/docker-compose.yml up -d --build
```

La base se inicializa sola (esquema, datos de demostración y administrador inicial) y
`imagenes-init` copia las 66 fotos de demostración. Abrir `http://localhost`.

## Uso diario

| Acción | Comando |
|---|---|
| Iniciar todo | `docker compose -f docker/docker-compose.yml up -d` |
| Iniciar después de cambiar código | `docker compose -f docker/docker-compose.yml up -d --build` |
| Estado (salud de cada servicio) | `docker compose -f docker/docker-compose.yml ps` |
| Logs de todos | `docker compose -f docker/docker-compose.yml logs -f` |
| Logs de uno | `docker compose -f docker/docker-compose.yml logs -f backend` (o `web`, `db`, `imagenes-init`) |
| Últimas líneas del backend | `docker compose -f docker/docker-compose.yml logs --tail 100 backend` |
| Detener (conserva todo) | `docker compose -f docker/docker-compose.yml stop` |
| Detener y quitar contenedores (conserva los datos) | `docker compose -f docker/docker-compose.yml down` |
| Consola SQL | `docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria` |
| Verificar las reglas de la base | `docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/pruebas/verificar.sql` |
| Salud del backend | `Invoke-RestMethod http://localhost/api/health` |

Si el puerto 80 está ocupado, definir por ejemplo `WEB_PORT=8088` en `docker/.env` y abrir `http://localhost:8088`.

## Flujo de desarrollo (sin cambios)

Para trabajar en las interfaces se sigue usando lo de siempre, con recarga automática:
la base (`db`), el Tomcat de desarrollo con `docker run` y `npm run dev` en `frontend/`
(`http://localhost:5173`) y en `admin/` (`http://localhost:5174/admin/`). Puede convivir con el
sistema completo: el backend de Compose no publica puertos en la PC.

## Entorno de pruebas

`docker-compose.pruebas.yml` levanta una copia descartable del sistema para las pruebas de
integración y los recorridos de punta a punta. Es otro proyecto (`sn-prueba`), con la base en memoria,
sus propias fotos y los puertos 8081 (sitio) y 5434 (base). No usa los volúmenes reales y puede
funcionar al mismo tiempo que el sistema. Sus variables van en `docker/.env.pruebas`, que se copia
de `pruebas.env.example` y no se sube a Git. Uso: [`docs/pruebas.md`](../docs/pruebas.md), sección 3.

---

## Respaldar

Los respaldos se guardan **fuera del repositorio**, en `%USERPROFILE%\respaldos-sn`.

```powershell
$fecha = Get-Date -Format 'yyyyMMdd-HHmm'; $resp = "$HOME\respaldos-sn"; New-Item -ItemType Directory -Force $resp | Out-Null

# Base de datos (formato propio de PostgreSQL, comprimido)
docker compose -f docker/docker-compose.yml exec db pg_dump -U sn_app -d sn_inmobiliaria -Fc -f /tmp/respaldo.dump
docker compose -f docker/docker-compose.yml cp db:/tmp/respaldo.dump "$resp\base-$fecha.dump"

# Fotos (volumen sn-uploads; se monta en solo lectura)
docker run --rm -v sn-uploads:/datos:ro -v "${resp}:/respaldo" alpine:3.20 tar czf /respaldo/uploads-$fecha.tgz -C /datos .
```

### Verificar un respaldo (no modifica nada)

```powershell
docker compose -f docker/docker-compose.yml exec db pg_restore --list /tmp/respaldo.dump | Select-String "TABLE DATA public " | Measure-Object | Select-Object Count
docker run --rm -v "${resp}:/respaldo:ro" alpine:3.20 sh -c "tar tzf /respaldo/uploads-$fecha.tgz | grep -cE '\.(png|jpg|webp)$'"
Get-ChildItem $resp | Format-Table Name, Length
```

Esperado: `Count` = **15** en el esquema `public` (las 14 tablas del sistema más `spatial_ref_sys`
de PostGIS), al menos **66** fotos en el `.tgz` (más las subidas desde el panel) y los dos archivos
con tamaño mayor a 0. El respaldo completo tiene además tablas de configuración de las extensiones
que instala la imagen `postgis/postgis` (esquemas `topology` y `tiger`); también se respaldan.

Para ver todas las tablas incluidas: `docker compose -f docker/docker-compose.yml exec db pg_restore --list /tmp/respaldo.dump | Select-String "TABLE DATA"`

## Recuperar

**Antes de recuperar, hacer un respaldo del estado actual** (sección anterior), por si se necesita volver.

Base de datos (reemplaza el contenido actual por el del respaldo):

```powershell
docker compose -f docker/docker-compose.yml stop backend web
docker compose -f docker/docker-compose.yml cp "$HOME\respaldos-sn\base-AAAAMMDD-HHMM.dump" db:/tmp/recuperar.dump
docker compose -f docker/docker-compose.yml exec db pg_restore -U sn_app -d sn_inmobiliaria --clean --if-exists --no-owner /tmp/recuperar.dump
docker compose -f docker/docker-compose.yml up -d
```

Fotos (agrega las del respaldo al volumen; no borra las fotos más nuevas):

```powershell
docker run --rm -v sn-uploads:/datos -v "$HOME\respaldos-sn:/respaldo:ro" alpine:3.20 tar xzf /respaldo/uploads-AAAAMMDD-HHMM.tgz -C /datos
```

---

## Solución de problemas

| Síntoma | Causa y solución |
|---|---|
| `external volume "sn-uploads" not found` | Equipo nuevo: crear los volúmenes con `docker volume create` (ver "Primera vez") |
| `required variable DB_NAME is missing` | Falta `docker/.env`: copiarlo desde `docker/.env.example` |
| `port is already allocated` (80) | Otro programa usa el puerto: definir `WEB_PORT` en `docker/.env` |
| `web` no arranca y `backend` aparece como `unhealthy` | Ver `docker compose -f docker/docker-compose.yml logs backend`; suele ser la base o una variable de `docker/.env` |
| `imagenes-init` terminó con error | Ver sus logs: indica qué foto no coincide con su huella. No sobrescribe nada |
| El asistente responde "no disponible" | Falta `GEMINI_API_KEY` en `docker/.env`, o se alcanzó el límite gratuito de Google |

## Operaciones destructivas (solo con respaldo previo)

Recrear la base desde cero **borra todos los datos** (propiedades, usuarios y consultas). Solo si
realmente se necesita, después de respaldar y verificar:

```powershell
docker compose -f docker/docker-compose.yml down
docker volume rm sn-inmobiliaria_pgdata       # IRREVERSIBLE sin respaldo
docker volume create sn-inmobiliaria_pgdata
docker compose -f docker/docker-compose.yml up -d
```
