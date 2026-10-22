# Pruebas del proyecto

Cómo comprobar que el sistema funciona, desde una copia nueva del repositorio. Hay cuatro niveles.
Los dos primeros no necesitan Docker.

| Nivel | Qué comprueba | Necesita | Cantidad (19/10/2026) |
|---|---|---|---|
| 1. Pruebas unitarias del backend | Reglas, validaciones, permisos, servlets, Gemini simulado | JDK 21 y Maven | 328, de las cuales 67 (integración) se omiten sin base |
| 2. Pruebas de interfaz del panel y del sitio | Pantallas, formularios, filtros y mapa, con un backend simulado | Node.js 22 | Panel 92 · Sitio 83 |
| 3. Pruebas de integración del backend y de la base | Las mismas pruebas del nivel 1, ahora contra PostgreSQL + PostGIS real; reglas de la base | Docker (entorno de pruebas) | 328 (1 omitida: Gemini real) · `verificar.sql` 24 |
| 4. Recorridos de punta a punta | Sitio y panel en un navegador real, con el sistema completo | Docker y Python con Playwright | 7 recorridos |

Además hay una revisión visual automática (sección 6) y una lista de recorridos manuales por rol
([`recorridos-manuales.md`](recorridos-manuales.md)).

Todos los comandos son de PowerShell y se ejecutan **desde la raíz del repositorio**.

---

## 1. Pruebas unitarias del backend

```powershell
mvn -f backend/pom.xml clean package
```

Resultado esperado: `Tests run: 328, Failures: 0, Errors: 0, Skipped: 67` y `BUILD SUCCESS`. Las 67
omitidas son las de integración, que necesitan `DB_URL` (sección 3).

## 2. Pruebas de interfaz del panel y del sitio

Usan Vitest, jsdom y Testing Library, que son dependencias de desarrollo de cada proyecto. No
necesitan backend: cada carpeta `verificacion/` tiene un backend simulado (`backendFalso.ts`).

```powershell
cd admin; npm ci; npm test; cd ..
cd frontend; npm ci; npm test; cd ..
```

Resultado esperado: `Tests 92 passed` en el panel y `Tests 83 passed` en el sitio.

| Carpeta | Archivos |
|---|---|
| `admin/verificacion/` | `panel.test.tsx` (inicio de sesión, menú por rol, propiedades, fotos, usuarios, Mi cuenta, consultas, clientes potenciales, gestión del sitio, temas) |
| `frontend/verificacion/` | `etapa1`, `fase15a`, `inicio`, `servicios`, `tipografia`, `volver-resultados`, `w2`, `w3`, `w4` y `zonas` (`.test.tsx`): identidad visual, contenido desde el panel, chat, presupuesto en guaraníes, zonas con propiedades y la X del detalle |

Para compilar sin pruebas: `npx tsc --noEmit` y `npm run build` en cada carpeta.

## 3. Entorno de pruebas (Docker)

Los niveles 3 y 4 necesitan el sistema completo. Para no tocar nunca los datos reales, hay un
**entorno de pruebas separado** (`docker/docker-compose.pruebas.yml`):

| | Sistema real | Entorno de pruebas |
|---|---|---|
| Proyecto de Compose | `sn-inmobiliaria` | `sn-prueba` |
| Base de datos | Volumen `sn-inmobiliaria_pgdata` | En memoria: nace de `database/schema` y `database/seed` en cada inicio y desaparece al detenerlo |
| Fotos | Volumen `sn-uploads` | Volumen propio `sn-prueba_imagenes` |
| Puertos | Sitio 80, base 5433 | Sitio **8081**, base **5434** |
| Gemini | Con la clave de `docker/.env` | Sin clave: el asistente responde que no está disponible |

Los dos pueden funcionar a la vez.

> **Importante:** los comandos de prueba definen variables en la ventana de PowerShell (`$env:DB_PASSWORD`,
> `$env:GEMINI_MODEL`, etc.), y Docker Compose las usa **antes** que las de `docker/.env`. No ejecutar
> `docker compose -f docker/docker-compose.yml ...` (el sistema real) en la misma ventana: el backend real
> arrancaría con la contraseña de la base de prueba y no podría conectarse. Usar otra ventana, o cerrar
> esta antes de volver al sistema real.

**Primera vez:** crear el archivo de variables (no se sube a Git) y cambiar las dos contraseñas.
Son solo para la base de prueba.

```powershell
Copy-Item docker\pruebas.env.example docker\.env.pruebas
notepad docker\.env.pruebas
```

**Iniciar** (la primera vez tarda unos minutos porque construye las imágenes):

```powershell
docker compose -f docker/docker-compose.pruebas.yml --env-file docker/.env.pruebas up -d --build
Invoke-RestMethod http://localhost:8081/api/health   # estado "ok", 28 propiedadesDisponibles
```

**Detener y borrar todo lo del entorno de pruebas.** Solo afecta al proyecto `sn-prueba`: la base
en memoria y el volumen `sn-prueba_imagenes`.

```powershell
docker compose -f docker/docker-compose.pruebas.yml --env-file docker/.env.pruebas down -v
```

## 3.1 Pruebas de integración del backend y de la base

Con el entorno de pruebas iniciado:

```powershell
function Leer-Env($n) { (Select-String -Path docker\.env.pruebas -Pattern "^$n=").Line.Split('=', 2)[1].Trim() }
$env:DB_URL = "jdbc:postgresql://localhost:5434/sn_prueba"; $env:DB_USER = "sn_app"; $env:DB_PASSWORD = Leer-Env "PRUEBA_DB_PASSWORD"
$env:ADMIN_EMAIL = Leer-Env "PRUEBA_ADMIN_EMAIL"; $env:ADMIN_PASSWORD = Leer-Env "PRUEBA_ADMIN_PASSWORD"; $env:VERIFICAR_DATOS_DEMO = "true"
mvn -f backend/pom.xml clean package | Select-String "Tests run: [0-9]+, F.*Skipped: [0-9]+$|BUILD"
```

Resultado esperado: `Tests run: 328, Failures: 0, Errors: 0, Skipped: 1` y `BUILD SUCCESS`. La
omitida es la prueba con Gemini real (sección 3.2).

| Variable | Para qué |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Activan las pruebas de integración. Las escrituras que hacen se limpian al terminar |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Prueba de inicio de sesión con el administrador de la base |
| `VERIFICAR_DATOS_DEMO=true` | Compara cada propiedad de la API con `frontend/public/data/propiedades.geojson`. Solo tiene sentido con los datos de demostración, sin cambios desde el panel: por eso se usa en el entorno de pruebas |

Reglas de la base (cada restricción rechaza lo inválido, dentro de una transacción que se deshace):

```powershell
docker compose -f docker/docker-compose.pruebas.yml --env-file docker/.env.pruebas exec -T db psql -U sn_app -d sn_prueba -f /database/pruebas/verificar.sql
```

Resultado esperado: 24 líneas `OK:` y ninguna `FALLÓ:`. La última línea es el mensaje final del
script, "si no hubo ningún FALLÓ…".

## 3.2 Prueba con Gemini real

La prueba `AsistenteIntegracionTest.unaConsultaRealAGemini` hace **una única consulta real** al
modelo configurado (por defecto `gemini-3.5-flash-lite`), para comprobar la clave, el modelo y el
formato de la respuesta.

| Tema | Detalle |
|---|---|
| Cuota | Consume **una solicitud** de la cuota diaria de Gemini por cada ejecución (dos si Google responde un error temporal, porque el cliente reintenta una vez). Las demás pruebas usan un Gemini simulado y no consumen nada |
| Datos | Escribe una fila en `consulta` y la borra al terminar |
| Protección | **Se omite si `DB_URL` apunta a la base `sn_inmobiliaria`** (la real), aunque se active. Se ejecuta en el entorno de pruebas |
| Resultado | Si Google está saturado o la cuota se agotó (HTTP 503 o 429), la prueba se **omite** con el motivo: no es un error del proyecto, se reintenta más tarde. Si la clave o el modelo están mal, **falla** |
| Requisitos | Entorno de pruebas iniciado, `GEMINI_API_KEY` definida (se lee de `docker/.env`, nunca se escribe en Git) y `PROBAR_GEMINI_REAL=true` |

```powershell
function Leer-Env($archivo, $n) { $l = (Select-String -Path $archivo -Pattern "^$n=").Line; if ($l) { $l.Split('=', 2)[1].Trim() } }
$env:DB_URL = "jdbc:postgresql://localhost:5434/sn_prueba"; $env:DB_USER = "sn_app"; $env:DB_PASSWORD = Leer-Env docker\.env.pruebas "PRUEBA_DB_PASSWORD"
$env:PROBAR_GEMINI_REAL = "true"; $env:GEMINI_API_KEY = Leer-Env docker\.env "GEMINI_API_KEY"
$env:GEMINI_MODEL = Leer-Env docker\.env "GEMINI_MODEL"; $env:GEMINI_THINKING_LEVEL = Leer-Env docker\.env "GEMINI_THINKING_LEVEL"
mvn -f backend/pom.xml test "-Dtest=AsistenteIntegracionTest" "-Dsurefire.failIfNoSpecifiedTests=false" | Select-String "Respuesta de Gemini|Tests run:|HTTP"
```

Resultado esperado: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` y una línea
`Respuesta de Gemini (...)` con el texto del modelo. Si aparece `Skipped: 1`, leer el motivo en
`backend/target/surefire-reports/TEST-py.sn.inmobiliaria.service.asistente.AsistenteIntegracionTest.xml`.

## 4. Recorridos de punta a punta (navegador real)

`herramientas/recorridos_e2e.py` usa Playwright con Chromium contra el entorno de pruebas
(`http://localhost:8081`). Se niega a ejecutarse contra `http://localhost`, que es el sistema real.

**Primera vez:** instalar Playwright para Python.

```powershell
py -m pip install playwright
py -m playwright install chromium
```

**Ejecutar** (con el entorno de pruebas iniciado):

```powershell
py herramientas/recorridos_e2e.py
```

| # | Recorrido | Qué se comprueba | Anchos |
|---|---|---|---|
| 1 | Búsqueda | Ciudad del Este y Buscar → un marcador por cada propiedad que devuelve la API | 1366 y 390 px |
| 2 | Detalle | Un marcador abre la propiedad con su título y precio en Gs.; la X vuelve al mapa con los mismos marcadores | 1366 y 390 px |
| 3 | Chat | Comprar → ciudad → presupuesto escrito → nombre y teléfono → "Dejar mis datos" → cliente potencial con origen CHAT, aceptación registrada y el presupuesto tal cual | 1366 px |
| 4 | Panel: inicio de sesión | Administrador de prueba → Inicio con el saludo y su nombre | 1366 px |
| 5 | Panel: alta de propiedad | Formulario completo, punto en el mapa (detecta barrio y ciudad), crear → visible en la API pública y en el sitio con "Gs. 350.000.000" | 1366 px |

Resultado esperado: `7 de 7 recorridos correctos` y código de salida 0.

Todo lo que crea lleva la marca `E2E <fecha-hora>`. Al terminar, pase o falle, borra **solo eso**:
la propiedad nueva (con sus operaciones) y el cliente potencial. Lo informa así:
`Limpieza (filas borradas): propiedad prop-017: 1, cliente potencial: 1`.

Con `E2E_VISIBLE=1` se ve el navegador mientras trabaja.

## 5. Recorridos manuales por rol

Ver [`recorridos-manuales.md`](recorridos-manuales.md): pasos y resultados esperados para
Administrador, Gerente y Vendedor, incluidas las restricciones.

## 6. Revisión visual automática del sitio

Captura 7 páginas y el chat en 1366 y 390 px. Informa los textos que se salen de su recuadro o
quedan recortados, los desbordes horizontales y los emojis. Usa el backend simulado
(`herramientas/simulador.mjs`) y la fuente Roboto local. Detalle en
[`herramientas/README.md`](../herramientas/README.md).

## 7. Qué no está cubierto

| Tema | Situación |
|---|---|
| Subida de fotos en el navegador real | La cubren las pruebas de integración (`ServiciosIntegracionTest`) y las de interfaz, no los recorridos de punta a punta |
| Respuestas de Gemini en el chat del sitio | Los recorridos usan el entorno sin clave. La conexión real se prueba en la sección 3.2 |
| Recorridos de punta a punta como gerente y vendedor | Están en los recorridos manuales. Los permisos por rol los cubren el backend (`PermisoTest`, `ReglasAccesoTest`, `AutenticacionTest`) y las pruebas del panel |
