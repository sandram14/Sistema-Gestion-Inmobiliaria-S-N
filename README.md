# S&N Inmobiliaria — Plataforma inmobiliaria

Sistema web de **S&N Inmobiliaria**, que opera en Alto Paraná (Paraguay). Permite a los clientes buscar
viviendas y terrenos en un mapa, consultar con un asistente virtual con IA y dejar sus datos de contacto,
y al equipo de la inmobiliaria administrar las propiedades, las consultas, los clientes potenciales,
los usuarios y el contenido del sitio.

| Parte | Carpeta | Para quién | Qué hace |
|---|---|---|---|
| Sitio público | `frontend/` | Clientes | Mapa por departamento, ciudad y barrio (solo zonas con propiedades disponibles), filtros con presupuesto en guaraníes, detalle de cada propiedad, asistente de IA (Gemini), páginas Inicio, Nosotros (con los servicios) y Contacto |
| Panel administrativo | `admin/` | Equipo de la inmobiliaria | Propiedades con ubicación en el mapa y fotos, consultas del asistente (historial y análisis), clientes potenciales, usuarios y roles, contenido del sitio y la cuenta propia. Se sirve en `/admin/` |
| Backend | `backend/` | — | API REST en Java: búsqueda, ubicación con PostGIS, autenticación con sesión, permisos por rol, imágenes y asistente |
| Base de datos | `database/` | — | Esquema, datos iniciales, migraciones y pruebas de PostgreSQL + PostGIS |
| Docker | `docker/` | — | Sistema completo con Docker Compose (base, backend y nginx) |

## Tecnologías

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Maven (WAR), Servlets Jakarta EE en Tomcat 10.1, JPA/Hibernate 6 y JDBC, HikariCP, Jackson, BCrypt, SLF4J + Logback, JUnit 5, API de Gemini |
| Base de datos | PostgreSQL 16 + PostGIS 3.4 (barrios y localidades oficiales del INE, censo 2022) |
| Sitio y panel | React 19, TypeScript, Vite, React Router 7, Leaflet / react-leaflet, CSS Modules, íconos lucide-react; el sitio usa además Tailwind CSS 4 (solo la base) |
| Despliegue | Docker Compose: `db` (postgis/postgis:16-3.4), `imagenes-init`, `backend` (Tomcat 10.1, JDK 21) y `web` (nginx 1.27, que sirve el sitio en `/`, el panel en `/admin/` y reenvía `/api/` y `/uploads/` al backend) |

## Estructura

```
├── frontend/     sitio público (React + Vite)
├── admin/        panel administrativo (React + Vite, base /admin/)
├── backend/      API Java (Maven); su README tiene los endpoints y las variables
├── database/
│   ├── schema/       estructura (tablas, índices, triggers)
│   ├── seed/         datos iniciales: catálogos, ubicación INE 2022, 28 propiedades de demostración, administrador inicial y contenido del sitio
│   ├── datos/        cartografía del INE (generada) y el script que la genera
│   ├── migraciones/  cambios para bases existentes (001 a 011), cada uno con verificación previa, migración y reversión
│   └── pruebas/      verificar.sql: reglas de la base
├── docker/       docker-compose.yml, entorno de pruebas (docker-compose.pruebas.yml), Dockerfiles, nginx, plantillas de variables y su README
├── docs/         guía de pruebas, recorridos manuales y contratos de integración del sitio con la API
└── herramientas/ backend simulado, revisión visual y recorridos de punta a punta (Playwright)
```

## Requisitos

| Para | Qué se necesita |
|---|---|
| Ejecutar el sistema completo | **Docker** con Docker Compose v2 (en Windows, Docker Desktop iniciado). Puertos libres: **80** (sitio y panel; se puede cambiar con `WEB_PORT`) y **5433** (base; `DB_PORT`) |
| Desarrollar y probar el backend | JDK 21 y Maven 3.9 o posterior |
| Desarrollar el sitio o el panel | Node.js 22 (la versión que usan las imágenes) y npm |
| Recorridos de punta a punta y revisión visual | Python 3 con Playwright (`py -m pip install playwright` y `py -m playwright install chromium`) |
| Usar el asistente de IA | Una clave de la API de Gemini (Google AI Studio). Sin clave, el sistema funciona y el asistente responde que no está disponible |

## Configuración

Las claves **nunca** se escriben en el código ni se suben a Git: viven solo en `docker/.env`, que está
excluido del repositorio y de las imágenes. Se crea a partir de la plantilla:

```powershell
Copy-Item docker\.env.example docker\.env
```

y se editan, como mínimo, `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` y (opcional) `GEMINI_API_KEY`.

| Variable | Obligatoria | Uso | Valor en la plantilla |
|---|:-:|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Sí | Base de datos | `sn_inmobiliaria`, `sn_app`, contraseña a cambiar |
| `DB_PORT` | No | Puerto de la base en la PC (pruebas de integración y desarrollo) | `5433` |
| `WEB_PORT` | No | Puerto del sitio y el panel | `80` |
| `APP_ENV` | No | `produccion` (cookies seguras y errores sin detalles internos) o `desarrollo` | `produccion` |
| `LOG_LEVEL` | No | Nivel de registro del backend | `INFO` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Primera vez | Administrador inicial: se crea **solo** al inicializar una base vacía. Después, las contraseñas se cambian desde el panel | a cambiar |
| `GEMINI_API_KEY` | No | Clave del asistente de IA | vacía |
| `GEMINI_MODEL`, `GEMINI_THINKING_LEVEL` | No | Modelo y razonamiento interno de Gemini | `gemini-3.5-flash-lite`, `minimal` |
| `ASISTENTE_LIMITE_MINUTO_SESION`, `ASISTENTE_LIMITE_DIA_SESION`, `ASISTENTE_LIMITE_MINUTO_GLOBAL`, `ASISTENTE_LIMITE_DIA_GLOBAL` | No | Límites de uso del asistente | `10`, `30`, `8`, `200` |
| `GEMINI_TIMEOUT_SEGUNDOS`, `APP_ORIGENES_PERMITIDOS` | No | Tiempo máximo de espera de Gemini y orígenes extra que pueden modificar datos (separados por coma). No están en la plantilla: se agregan solo si hacen falta | — |

## Instalación y ejecución con Docker

Todos los comandos se ejecutan **desde la raíz del repositorio**.

**Primera vez en un equipo** (los volúmenes son externos: Compose nunca los crea ni los borra):

```powershell
Copy-Item docker\.env.example docker\.env      # y editar las claves
docker volume create sn-inmobiliaria_pgdata
docker volume create sn-uploads
docker compose -f docker/docker-compose.yml up -d --build
```

La base se inicializa sola con el esquema, los datos de demostración y el administrador inicial, y
`imagenes-init` copia las fotos de demostración al volumen. Luego:

| Qué | Dirección |
|---|---|
| Sitio público | `http://localhost/` |
| Panel administrativo | `http://localhost/admin/` (ingresar con `ADMIN_EMAIL` y `ADMIN_PASSWORD`) |
| Estado del backend | `http://localhost/api/health` |

| Acción | Comando |
|---|---|
| Iniciar | `docker compose -f docker/docker-compose.yml up -d` |
| Iniciar después de cambiar código | `docker compose -f docker/docker-compose.yml up -d --build` |
| Estado de los servicios | `docker compose -f docker/docker-compose.yml ps` |
| Registros del backend | `docker compose -f docker/docker-compose.yml logs --tail 100 backend` |
| Detener (conserva los datos) | `docker compose -f docker/docker-compose.yml stop` |

Docker compila el backend **sin ejecutar las pruebas**: conviene correrlas antes (sección Pruebas).
Respaldos, recuperación y solución de problemas: [`docker/README.md`](docker/README.md).

### Actualizar una base existente

Una base creada desde cero ya incluye todas las migraciones. Una base anterior se actualiza con los
scripts de `database/migraciones/`, en orden y siempre con un respaldo previo: verificación previa
(solo lectura), migración y, si hiciera falta, reversión. El detalle de cada una está en
[`database/migraciones/README.md`](database/migraciones/README.md). Desde el backend 0.24.0 se necesita la
migración 011.

## Desarrollo

| Parte | Comandos | Dirección |
|---|---|---|
| Sitio | `cd frontend; npm ci; npm run dev` | `http://localhost:5173` |
| Panel | `cd admin; npm ci; npm run dev` | `http://localhost:5174/admin/` |
| Backend | Ver [`backend/README.md`](backend/README.md) (Tomcat de desarrollo con la base de Docker) | — |

En desarrollo, Vite reenvía `/api` y `/uploads` al backend de `http://localhost:8080`
(se cambia con la variable `VITE_BACKEND_URL`).

## Pruebas

La guía completa, con cada comando y su resultado esperado, está en [`docs/pruebas.md`](docs/pruebas.md).

| Nivel | Comando | Qué comprueba |
|---|---|---|
| Backend (unitarias) | `mvn -f backend/pom.xml clean package` | Reglas, validaciones, permisos, servlets y Gemini simulado. Sin base, las de integración se omiten |
| Panel y sitio | `npm ci` y `npm test` en `admin/` y en `frontend/` | Pantallas, formularios, filtros y mapa, con un backend simulado (Vitest + Testing Library) |
| Backend (integración) y base | `mvn` con `DB_URL` apuntando al **entorno de pruebas**, y `verificar.sql` | Las mismas pruebas contra PostgreSQL + PostGIS real, y cada regla de la base |
| Punta a punta | `py herramientas/recorridos_e2e.py` | Búsqueda, detalle, chat, inicio de sesión en el panel y alta de una propiedad, en un navegador real |
| Manual | [`docs/recorridos-manuales.md`](docs/recorridos-manuales.md) | Pasos y resultados esperados para Administrador, Gerente y Vendedor |

Las pruebas que necesitan base de datos se hacen en un **entorno de pruebas separado**
(`docker/docker-compose.pruebas.yml`): otro proyecto de Compose, con la base en memoria y sus propios
puertos (sitio en 8081, base en 5434). Así nunca escriben en los datos reales.

## Usuarios y permisos

Hay tres roles, verificados en el backend en cada petición:

| Permiso | Administrador | Gerente | Vendedor |
|---|:-:|:-:|:-:|
| Ver propiedades | ✓ | ✓ | ✓ |
| Crear y editar propiedades | Todas | Todas | Solo las suyas |
| Dar de baja y asignar responsable | ✓ | ✓ | — |
| Gestión del sitio web | ✓ | ✓ | — |
| Consultas del asistente | Todas | Todas | Generales y de sus propiedades |
| Análisis técnico de las consultas | ✓ | ✓ | — |
| Clientes potenciales | Todos, y asigna | Todos, y asigna | Solo los suyos |
| Usuarios | Gestiona | Solo consulta | — |

Los usuarios nuevos reciben una contraseña temporal que deben cambiar al ingresar. Si alguien pierde el
acceso, el administrador genera una nueva desde Usuarios: se muestra una sola vez, cierra las sesiones
abiertas de esa persona y queda registrado en la tabla `auditoria`. Las contraseñas se guardan solo como
hash BCrypt; nadie puede verlas.

## Documentación

| Documento | Contenido |
|---|---|
| [`backend/README.md`](backend/README.md) | Endpoints, variables, paquetes y pruebas del backend |
| [`docker/README.md`](docker/README.md) | Instalación, uso diario, respaldos y recuperación |
| [`database/README.md`](database/README.md) | Modelo de datos y reglas garantizadas por la base |
| [`database/migraciones/README.md`](database/migraciones/README.md) | Cada migración, su orden y cómo se ensayó |
| [`docs/pruebas.md`](docs/pruebas.md) | Cómo ejecutar cada nivel de pruebas, el entorno de pruebas y la prueba con Gemini real |
| [`docs/recorridos-manuales.md`](docs/recorridos-manuales.md) | Revisión manual del panel por rol |
| [`herramientas/README.md`](herramientas/README.md) | Backend simulado, revisión visual y recorridos de punta a punta |
| [`docs/`](docs/) | Además, contratos de integración del sitio con la API (búsqueda y presupuesto, clientes potenciales, gestión del sitio) |
