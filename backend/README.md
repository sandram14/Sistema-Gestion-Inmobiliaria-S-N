# Backend — S&N Inmobiliaria

API REST en **Java 21 + Maven + Servlets (Jakarta EE / Tomcat 10.1)**.
Arquitectura: Controller (Servlet) → Service → Repository/DAO → JPA/Hibernate → PostgreSQL/PostGIS.

## Requisitos

- JDK 21
- Maven 3.9+
- Docker (Tomcat y PostgreSQL/PostGIS; ver `docker/README.md` para levantar la base)

## Compilar y ejecutar pruebas

```bash
mvn -f backend/pom.xml clean package
```

Genera `backend/target/sn-inmobiliaria.war`.

Las pruebas unitarias no necesitan base de datos.

### Pruebas de integración (necesitan la base de Docker)

Solo se ejecutan si `DB_URL` está definida; sin ella, Maven las marca como omitidas.
Las escrituras que hacen se deshacen siempre (la base queda igual).

| Clase | Qué prueba |
|---|---|
| `ModeloJpaIntegracionTest` | Hibernate valida el esquema; entidades y relaciones; escrituras en cascada |
| `PropiedadDaoIntegracionTest` | Búsqueda pública con filtros, catálogos, imágenes, usuarios |
| `UbicacionDaoIntegracionTest` | Consultas PostGIS: ciudades, polígonos GeoJSON, punto dentro de barrio |
| `ServiciosIntegracionTest` | Crear/editar/eliminar propiedades, cambio de tipo, imágenes, búsqueda. Borra al final lo que crea |
| `AuthIntegracionTest` | Login con el administrador real (requiere además `ADMIN_EMAIL` y `ADMIN_PASSWORD`) |
| `CompatibilidadFrontendIntegracionTest` | El GeoJSON de la API coincide campo por campo con `frontend/public/data/propiedades.geojson` |

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5433/sn_inmobiliaria"
$env:DB_USER = "sn_app"
$env:DB_PASSWORD = "<la de docker/.env>"
mvn -f backend/pom.xml test
```

## Ejecutar localmente (Tomcat 10.1 en Docker)

PowerShell, desde la raíz del repositorio:

```powershell
docker run --rm -p 8080:8080 -e APP_ENV=desarrollo -e TZ=America/Asuncion `
  -e DB_URL=jdbc:postgresql://host.docker.internal:5433/sn_inmobiliaria `
  -e DB_USER=sn_app -e DB_PASSWORD=<la de docker/.env> `
  -e UPLOAD_DIR=/data/uploads -v sn-uploads:/data/uploads `
  -v "${PWD}\backend\target\sn-inmobiliaria.war:/usr/local/tomcat/webapps/ROOT.war" `
  tomcat:10.1-jdk21-temurin
```

`host.docker.internal` es el nombre con el que un contenedor llega a la PC anfitriona,
donde Docker publica la base en el puerto 5433.

Probar: `curl.exe -i http://localhost:8080/api/health`

## Variables de entorno

| Variable  | Uso                                              | Por defecto  |
|-----------|--------------------------------------------------|--------------|
| `APP_ENV` | `desarrollo` o `produccion`                      | `desarrollo` |
| `LOG_LEVEL` | Nivel de log del paquete `py.sn.inmobiliaria`  | `INFO`       |
| `DB_URL`    | URL JDBC de PostgreSQL                         | —            |
| `DB_USER`   | Usuario de la base                             | —            |
| `DB_PASSWORD` | Contraseña de la base                        | —            |
| `APP_ORIGENES_PERMITIDOS` | Orígenes extra que pueden modificar datos (coma) | — |
| `UPLOAD_DIR` | Carpeta de imágenes subidas (en Docker: un volumen en `/data/uploads`) | carpeta temporal |

Si falta alguna variable `DB_*`, el backend arranca igual y `/api/health` responde 503
con `"baseDatos": {"estado": "no_configurada"}`. La API key de Gemini se agregará en su fase,
también como variable de entorno.

## Estructura de paquetes (`py.sn.inmobiliaria`)

| Paquete        | Responsabilidad                                               |
|----------------|---------------------------------------------------------------|
| `config`       | Arranque (`AppContextListener`), configuración, pool de conexiones, `JpaProvider` (Hibernate) y `TxRunner` (transacciones) |
| `controller`   | Servlets de la API (`ApiServlet` es la base común)            |
| `filter`       | Filtros HTTP: UTF-8 y `AuthFilter` (sesión + protección CSRF de `/api/admin/*`) |
| `seguridad`    | BCrypt (`ContrasenaHasher`), `UsuarioSesion`, `LimitadorIntentos` |
| `service`      | Reglas de negocio (`ReglasNegocio`), transacciones, validación (`service.validacion`) y traducción de errores de la base (`TraductorErroresBd`) |
| `repository`   | DAOs con consultas parametrizadas                             |
| `model`        | Entidades JPA (una por tabla; Hibernate valida el esquema al arrancar) |
| `dto`          | Entrada (`request`) y salida (`response`, `response.geojson`, `response.admin`) |
| `mapper`       | Conversión entidad → DTO: GeoJSON del mapa (`GeoJsonMapper`), panel (`PropiedadAdminMapper`), catálogos (`CatalogoMapper`) |
| `integration`  | Servicios externos (Gemini)                                   |
| `exception`    | Errores de la API → JSON `{ "error": { ... } }` (400 validación, 404, 409 conflicto, 422 regla de negocio, 503) |
| `util`         | Utilidades (JSON)                                             |

## Endpoints disponibles

### API pública (sin autenticación)

| Método | Ruta | Respuesta |
|--------|------|-----------|
| GET | `/api/health` | Estado del backend, la base y Hibernate (200 ok / 503 degradado) |
| GET | `/api/ciudades` | FeatureCollection de puntos (caché 1 h) |
| GET | `/api/barrios?ciudadId=` | FeatureCollection de polígonos; `ciudadId` opcional (caché 1 h) |
| GET | `/api/propiedades` | FeatureCollection de propiedades Disponibles |
| GET | `/api/propiedades/{codigo}` | Feature de una propiedad (`prop-001`) |
| GET | `/api/catalogos` | Tipos, estados, operaciones, monedas, reglas y árbol de ubicaciones |
| * | `/api/*` | Cualquier otra ruta → 404 JSON |

Filtros de `/api/propiedades` (todos opcionales): `ciudadId`, `barrioId`,
`categoria` (`Casa`, `Vivienda` o `Terreno`), `operacion` o `tipoOperacion` (`Venta`, `Alquiler`),
`presupuestoMax` y `moneda` (`USD` por defecto, o `PYG`). Ejemplo:

```
GET /api/propiedades?ciudadId=1&categoria=Vivienda&operacion=Alquiler&presupuestoMax=500&moneda=USD
```

### Autenticación administrativa

Sesión HTTP con cookie `JSESSIONID` (`HttpOnly`, `SameSite=Lax`; además `Secure` si `APP_ENV=produccion`).
La sesión expira tras 30 minutos sin actividad.

| Método | Ruta | Respuesta |
|--------|------|-----------|
| POST | `/api/admin/auth/login` | Cuerpo `{"email","password"}` → 200 `{"usuario": {...}}` y cookie de sesión |
| POST | `/api/admin/auth/logout` | 204, cierra la sesión |
| GET | `/api/admin/auth/me` | 200 `{"usuario": {...}}` o 401 |

El filtro `AuthFilter` protege todo `/api/admin/*`:

- sin sesión de administrador → 401 (salvo login y logout);
- en POST/PUT/PATCH/DELETE exige `Content-Type: application/json` (o `multipart/form-data`)
  y, si llega la cabecera `Origin`, que sea este mismo sitio o uno de `APP_ORIGENES_PERMITIDOS` → si no, 403/415;
- 5 intentos fallidos de login (por IP y email) bloquean 15 minutos → 429.

### CRUD administrativo (requiere sesión)

| Método | Ruta | Respuesta |
|--------|------|-----------|
| GET | `/api/admin/propiedades` | Listado de todas las propiedades (cualquier estado) |
| POST | `/api/admin/propiedades` | Crear (JSON `PropiedadRequest`) → 201 + `Location` + detalle |
| GET | `/api/admin/propiedades/{id}` | Detalle para el formulario de edición |
| PUT | `/api/admin/propiedades/{id}` | Actualizar → 200 + detalle |
| DELETE | `/api/admin/propiedades/{id}` | Borrado lógico (pasa a Inactivo) → 204 |
| POST | `/api/admin/propiedades/{id}/imagenes` | Subir imagen (`multipart/form-data`: `archivo` y `descripcion` opcional) → 201 + detalle |
| DELETE | `/api/admin/propiedades/{id}/imagenes/{idImagen}` | Eliminar imagen (y su archivo) → 200 + detalle |
| PATCH | `/api/admin/propiedades/{id}/imagenes/{idImagen}/principal` | Marcar como principal → 200 + detalle |

Imágenes: JPEG, PNG o WebP de hasta 5 MB, reconocidas por su contenido. Se guardan en
`UPLOAD_DIR/{codigo}/{uuid}.{ext}` y se sirven en `GET /uploads/{codigo}/{uuid}.{ext}`
(caché de un año: el nombre nunca se reutiliza). Las imágenes originales del frontend (`/images/...`)
no se modifican ni se borran.

### Errores

Siempre en el mismo formato, sin detalles internos:

```json
{ "error": { "codigo": "VALIDACION", "mensaje": "...", "campos": { "ciudadId": "Debe ser un número entero" } } }
```

| HTTP | Código | Cuándo |
|------|--------|--------|
| 400 | `VALIDACION` | Parámetros o datos inválidos (incluye Terreno + Alquiler) |
| 404 | `NO_ENCONTRADO` | Recurso o ruta inexistente |
| 401 | `NO_AUTENTICADO` | Sin sesión, sesión expirada o credenciales incorrectas |
| 403 | `ACCESO_DENEGADO` | Origen no permitido o usuario sin permisos |
| 405 | `METODO_NO_PERMITIDO` | Método HTTP no admitido |
| 415 | `TIPO_NO_SOPORTADO` | El cuerpo no es JSON |
| 429 | `DEMASIADOS_INTENTOS` | Demasiados logins fallidos |
| 413 | `ARCHIVO_DEMASIADO_GRANDE` | Imagen de más de 5 MB |
| 409 | `CONFLICTO` | Dato repetido (código, imagen principal, email) |
| 422 | `REGLA_*` / `RESTRICCION` | Regla de negocio rechazada por la base |
| 503 | `SERVICIO_NO_DISPONIBLE` | La base de datos no responde o no está configurada |
