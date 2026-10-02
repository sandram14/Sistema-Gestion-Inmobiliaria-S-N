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
| `service`      | Reglas de negocio y transacciones                             |
| `repository`   | DAOs con consultas parametrizadas                             |
| `model`        | Entidades JPA (una por tabla; Hibernate valida el esquema al arrancar) |
| `dto`          | Objetos de entrada (`request`) y salida (`response`)          |
| `mapper`       | Conversión entidad ↔ DTO y GeoJSON                            |
| `integration`  | Servicios externos (Gemini)                                   |
| `exception`    | Errores de la API → JSON `{ "error": { ... } }`               |
| `util`         | Utilidades (JSON)                                             |

## Endpoints disponibles

| Método | Ruta          | Descripción                    |
|--------|---------------|--------------------------------|
| GET    | `/api/health` | Estado del backend, la base y Hibernate (200 ok / 503 degradado) |
| *      | `/api/*`      | Cualquier otra ruta → 404 JSON |
