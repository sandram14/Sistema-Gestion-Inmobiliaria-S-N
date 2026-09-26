# Backend — S&N Inmobiliaria

API REST en **Java 21 + Maven + Servlets (Jakarta EE / Tomcat 10.1)**.
Arquitectura: Controller (Servlet) → Service → Repository/DAO → JPA/Hibernate → PostgreSQL/PostGIS.

## Requisitos

- JDK 21
- Maven 3.9+
- Docker (para ejecutar Tomcat y, desde la Fase 4, PostgreSQL/PostGIS)

## Compilar y ejecutar pruebas

```bash
mvn -f backend/pom.xml clean package
```

Genera `backend/target/sn-inmobiliaria.war`.

## Ejecutar localmente (Tomcat 10.1 en Docker)

PowerShell, desde la raíz del repositorio:

```powershell
docker run --rm -p 8080:8080 -e APP_ENV=desarrollo -e TZ=America/Asuncion `
  -v "${PWD}\backend\target\sn-inmobiliaria.war:/usr/local/tomcat/webapps/ROOT.war" `
  tomcat:10.1-jdk21-temurin
```

Probar: `curl.exe -i http://localhost:8080/api/health`

## Variables de entorno

| Variable  | Uso                                              | Por defecto  |
|-----------|--------------------------------------------------|--------------|
| `APP_ENV` | `desarrollo` o `produccion`                      | `desarrollo` |
| `LOG_LEVEL` | Nivel de log del paquete `py.sn.inmobiliaria`  | `INFO`       |

Las credenciales de base de datos y la API key de Gemini se agregarán en sus fases,
siempre como variables de entorno (nunca en el código ni en Git).

## Estructura de paquetes (`py.sn.inmobiliaria`)

| Paquete        | Responsabilidad                                               |
|----------------|---------------------------------------------------------------|
| `config`       | Arranque (`AppContextListener`) y configuración (`AppConfig`) |
| `controller`   | Servlets de la API (`ApiServlet` es la base común)            |
| `filter`       | Filtros HTTP (UTF-8, autenticación admin en la Fase 10)       |
| `service`      | Reglas de negocio y transacciones                             |
| `repository`   | DAOs con consultas parametrizadas                             |
| `model`        | Entidades JPA                                                 |
| `dto`          | Objetos de entrada (`request`) y salida (`response`)          |
| `mapper`       | Conversión entidad ↔ DTO y GeoJSON                            |
| `integration`  | Servicios externos (Gemini)                                   |
| `exception`    | Errores de la API → JSON `{ "error": { ... } }`               |
| `util`         | Utilidades (JSON)                                             |

## Endpoints disponibles

| Método | Ruta          | Descripción                    |
|--------|---------------|--------------------------------|
| GET    | `/api/health` | Estado del backend             |
| *      | `/api/*`      | Cualquier otra ruta → 404 JSON |
