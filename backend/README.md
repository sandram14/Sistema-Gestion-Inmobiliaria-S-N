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

Si falta alguna variable `DB_*`, el backend arranca igual y `/api/health` responde 503
con `"baseDatos": {"estado": "no_configurada"}`. La API key de Gemini se agregará en su fase,
también como variable de entorno.

## Estructura de paquetes (`py.sn.inmobiliaria`)

| Paquete        | Responsabilidad                                               |
|----------------|---------------------------------------------------------------|
| `config`       | Arranque (`AppContextListener`), configuración, pool de conexiones, `JpaProvider` (Hibernate) y `TxRunner` (transacciones) |
| `controller`   | Servlets de la API (`ApiServlet` es la base común)            |
| `filter`       | Filtros HTTP (UTF-8, autenticación admin en la Fase 10)       |
| `service`      | Reglas de negocio (`ReglasNegocio`), transacciones, validación (`service.validacion`) y traducción de errores de la base (`TraductorErroresBd`) |
| `repository`   | DAOs con consultas parametrizadas                             |
| `model`        | Entidades JPA (una por tabla; Hibernate valida el esquema al arrancar) |
| `dto`          | Entrada (`request`) y salida (`response`, `response.geojson`, `response.admin`) |
| `mapper`       | Conversión entidad → DTO: GeoJSON del mapa (`GeoJsonMapper`), panel (`PropiedadAdminMapper`), catálogos (`CatalogoMapper`) |
| `integration`  | Servicios externos (Gemini)                                   |
| `exception`    | Errores de la API → JSON `{ "error": { ... } }` (400 validación, 404, 409 conflicto, 422 regla de negocio, 503) |
| `util`         | Utilidades (JSON)                                             |

## Endpoints disponibles

| Método | Ruta          | Descripción                    |
|--------|---------------|--------------------------------|
| GET    | `/api/health` | Estado del backend, la base y Hibernate (200 ok / 503 degradado) |
| *      | `/api/*`      | Cualquier otra ruta → 404 JSON |
