# S&N Inmobiliaria — Contexto del proyecto (para Claude Code)

> Documento de traspaso. Léelo completo antes de proponer o modificar nada.
> Estado al **16 de octubre de 2026 (tarde)**. Proyecto de **Sandra**. Lo que resta del proyecto lo trabajan **solo Sandra y Claude**
> (la compañera que colaboraba en `frontend/` ya no participa).
>
> Este archivo se llama `CLAUDE.md` y va en la raíz del repositorio: Claude Code lo lee solo al iniciar cada sesión.

---

## 0. Lo más importante, en cinco líneas

1. La **W4** del sitio (presupuesto y **solo guaraníes** en todo el sistema) está **terminada, verificada y desplegada**, pero **sin commitear**: falta la revisión de Sandra en el navegador y su commit (sección 9).
2. La **migración 009 ya está aplicada en la base de desarrollo** de Sandra y el backend **0.21.0** está corriendo en Docker.
3. Después del despliegue se quitaron los restos del tipo "departamento" (solo **Casa y Terreno**): ese cambio del sitio **todavía no está desplegado** (falta `docker compose up -d --build web`).
4. Lo pendiente está en la sección 9; lo que sigue después de la W4, en la sección 12 (Fases 17 y 18).
5. Dinámica obligatoria (sección 11): revisar, proponer con decisiones, **esperar aprobación**, implementar, verificar, tabla de revisión. **Nunca hacer commit**: lo hace Sandra.

---

## 1. Qué es el proyecto

Plataforma inmobiliaria para **S&N Inmobiliaria**, que opera en **Alto Paraná, Paraguay** (Ciudad del Este, Hernandarias, Minga Guazú y Presidente Franco).

| Parte | Para quién | Qué hace |
|---|---|---|
| **Sitio público** (`frontend/`) | Clientes | Buscar propiedades en un mapa por departamento, ciudad y barrio; ver el detalle; asistente virtual con IA (Gemini); páginas Inicio, Servicios, Nosotros y Contacto |
| **Panel administrativo** (`admin/`) | Equipo de la inmobiliaria | Propiedades, consultas del chat IA, clientes potenciales, usuarios y roles, contenido del sitio y la propia cuenta |
| **Backend** (`backend/`) + **base** (`database/`) | — | API REST en Java y PostgreSQL con PostGIS |

## 2. Tecnologías

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Maven (WAR), Servlets Jakarta EE en Tomcat 10.1, JPA/Hibernate 6 (más JDBC directo en algunos módulos), HikariCP, Jackson, BCrypt, SLF4J + Logback, JUnit 5, Gemini API (`gemini-3.1-flash-lite`, `thinkingLevel=minimal`) |
| Base | PostgreSQL 16 + PostGIS 3.4 |
| Sitio y panel | React 19, TypeScript, Vite 8, React Router 7, Leaflet / react-leaflet, Tailwind CSS 4 (solo la base), CSS Modules, íconos `lucide-react` 1.34.0 |
| Docker | `docker/docker-compose.yml` con 4 servicios: `db`, `imagenes-init`, `backend` y `web` (nginx 1.27 sirve el sitio en `/` y el panel en `/admin/`, y reenvía `/api/` y `/uploads/` al backend) |

## 3. Estructura y entorno

```
C:\Proyectos\sn-inmobiliaria          (rama de trabajo: reorganizacion-estructura; main quedó en el punto de partida)
├── frontend/   sitio público
├── admin/      panel administrativo (independiente; se sirve en /admin/)
├── backend/    Java (0.20.0 commiteado; 0.21.0 en la copia local, sin commitear, y desplegado)
├── database/   schema/, seed/, migraciones/ (001 a 008 commiteadas; 009 sin commitear y aplicada), pruebas/
├── docker/     docker-compose.yml, Dockerfiles, nginx, .env (NUNCA en Git), README
├── docs/       contratos de integración para el sitio público
└── herramientas/, admin/verificacion/, frontend/verificacion/   herramientas de verificación (sin commitear; sección 10)
```

- Volúmenes externos (Compose nunca los crea ni borra): `sn-inmobiliaria_pgdata` (base) y `sn-uploads` (fotos).
- Base de desarrollo: puerto **5433**, usuario `sn_app`, base `sn_inmobiliaria`. **Es la base de Sandra con sus datos**: nunca ensayar migraciones en ella sin respaldo, y preferir una base de prueba (sección 10.2).
- Respaldos de Sandra: `C:\Users\sanie\respaldos-sn` (dumps `base-AAAAMMDD-HHMM.dump`).
- Sistema: **Windows con PowerShell**, Docker Desktop (si está apagado, el daemon no responde: hay que iniciarlo). La copia local usa finales de línea CRLF; `.gitattributes` fuerza LF en `*.sh`, `*.sql`, `*.yml` y `*.yaml`.
- Instalados en la máquina: Java 21, Maven 3.9.16, Node, Python 3.14 (`py`), Playwright con Chromium (instalado con permiso de Sandra el 16/10).
- La pantalla de Sandra tiene **escala mayor al 100 %**: en sus capturas todo se ve un 14 % más grande, y no es un error.

## 4. Estado de las fases

| Fases | Estado |
|---|---|
| 1 a 14 | Completas: análisis, arquitectura, PostgreSQL, entidades, DAOs, servicios, DTOs, servlets públicos, autenticación, CRUD administrativo, imágenes, filtro por departamento, ubicación por PostGIS, asistente Gemini |
| 15A, 15B, 15C | Completas: sitio conectado a la API, panel independiente, 66 fotos migradas a `/uploads` |
| 16 | Completa: sistema completo con Docker Compose |
| Mejoras del panel | Completas (sección 6) |
| Sitio público | W1, W2 y W3 **commiteadas**. **W4 terminada, sin commitear** (secciones 7 y 8) |
| **17 (pruebas)** y **18 (documentación)** | Pendientes (sección 12) |

## 5. Base de datos: migraciones

Todas siguen el mismo patrón, con tres scripts:

- una **verificación previa** de solo lectura;
- la **migración** en una transacción, con comprobaciones antes y después que hacen `RAISE EXCEPTION` y deshacen todo si algo no coincide;
- una **reversión protegida**.

Además se actualizan `schema/` y `seed/`, para que una base nueva nazca igual que una migrada. Todo se documenta en `database/migraciones/README.md`.

| # | Contenido |
|---|---|
| 001 | Fotos de `/images/…` a `/uploads/…` (volumen compartido, verificación SHA-256) |
| 002 | Roles GERENTE y VENDEDOR, `usuario.debe_cambiar_password`, `propiedad.id_responsable` |
| 003 | `consulta.resultado`, revisión interna y tabla `consulta_recomendacion` |
| 004 | Resultado `FUERA_DE_AMBITO` |
| 005 | Tabla `cliente_potencial` (aceptación obligatoria si viene del chat) |
| 006 | `seccion_sitio` (contenido JSON) y `propiedad_destacada` |
| 007 | Origen `CONTACTO` para clientes potenciales |
| 008 | Solo datos: asteriscos en tres títulos del sitio (un único script, documentado así) |
| **009** | **Solo guaraníes**: USD → PYG con el mismo número, `CHECK (moneda = 'PYG')` y `DEFAULT 'PYG'`. Sin commitear. **Aplicada en la base de desarrollo el 16/10** (sección 8.1) |

Se aplican con:

```powershell
docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/00X_....sql
```

## 6. Panel administrativo (completo)

| Sección | Funciones |
|---|---|
| Estructura | Menú lateral (en celular se abre con un botón), carga diferida, tema claro, oscuro o según el sistema (por navegador), botón Volver |
| Propiedades | Listado, alta y edición con mapa (ubicación por PostGIS), operaciones según el tipo, fotos, baja lógica y responsable (el vendedor solo edita las suyas) |
| Gestión del sitio web | Pestañas Inicio (portada y ofertas destacadas), Servicios, Nosotros y Contacto. Se publica al guardar |
| Consultas IA | Pestañas Historial (paginado, revisión interna), Análisis técnico, Calidad de respuestas y Clientes potenciales |
| Usuarios | Alta con contraseña temporal (se muestra una vez), edición, rol, activar y desactivar |
| Mi cuenta | Datos propios, cambio de email (con contraseña) y de contraseña, preferencia de tema |

### Roles y permisos (fijos por rol, verificados en el backend)

| Permiso | ADMIN | GERENTE | VENDEDOR |
|---|:-:|:-:|:-:|
| Ver propiedades | ✓ | ✓ | ✓ |
| Crear y editar propiedades | Todas | Todas | Solo las suyas (crea a su nombre) |
| Dar de baja y asignar responsable | ✓ | ✓ | — |
| Gestión del sitio web | ✓ | ✓ | — |
| Consultas IA: historial | Todas | Todas | Generales y de sus propiedades |
| Análisis técnico y calidad | ✓ | ✓ | — |
| Clientes potenciales | Todos, y asigna | Todos, y asigna | Solo los suyos (cambia el estado) |
| Usuarios | Gestiona | Solo consulta | — |
| Mi cuenta | ✓ | ✓ | ✓ |

En el backend: `seguridad/Permiso.java` (matriz), `seguridad/ReglasAcceso.java` (endpoint → permiso; **una ruta sin regla queda bloqueada con 403**) y `filter/AuthFilter.java` (relee al usuario en cada petición; si la contraseña es temporal, solo permite cambiarla).

## 7. Sitio público: etapas W

| Etapa | Estado | Contenido |
|---|---|---|
| W1 Identidad visual | ✅ | Logo, íconos Lucide, encabezado legible, variables de color y tipografía, mapa a pantalla completa, filtros centrados |
| W2 Contenido desde el panel | ✅ | Portada, ofertas, Servicios, Nosotros, pie, Contacto con formulario (canal `CONTACTO`), `*asteriscos*`, aviso de datos provisionales |
| W3 Chat | ✅ | Saludo, sin códigos visibles, WhatsApp desde el panel, formulario "Dejar mis datos" en el chat (canal `CHAT`) |
| **W4** Presupuesto y solo guaraníes | ✅ **Terminada, sin commitear** | Ver 7.1 y sección 8 |

### 7.1 W4: decisiones aprobadas por Sandra

La idea inicial era elegir guaraníes o dólares. **Sandra decidió eliminar el dólar de todo el sistema** "para evitar problemas".

| # | Decisión aprobada |
|---|---|
| Moneda | **Solo guaraníes (PYG)** en todo el sistema. El panel ya no permite cargar USD |
| D9 · precios en USD existentes | Pasan a PYG **con el mismo número, sin conversión** (45.000 USD queda Gs. 45.000). Sandra los corrige a mano desde el panel. El tipo de cambio que dio (1 USD = 5.692,38 Gs.) **no se usa** |
| D10 · columna `moneda` | Se **conserva**, limitada a `CHECK (moneda = 'PYG')` con `DEFAULT 'PYG'`. La API mantiene `"moneda": "PYG"` |
| D11 · formato en pantalla | **"Gs. 450.000.000"** (punto de miles, sin decimales) en el sitio y el panel; alquiler con "/ mes" |
| D12 · presupuesto en el chat | **Ni botones ni campo de monto**: el bot pregunta el presupuesto y el cliente lo **escribe con sus palabras** en el cuadro normal del chat ("entre 300 y 500 millones"). Queda **tal cual** en el resumen y en el WhatsApp. Botón **"Omitir"** → "No indicado". Máximo 150 caracteres. No se envía a Gemini |
| D12b · presupuesto en preguntas libres | Regla 9 de las instrucciones del asistente: si el cliente indica un presupuesto, Gemini menciona solo las propiedades que entran en ese rango ("millones" = 1.000.000) y, si no hay, lo dice |
| D13 · historial de consultas | Las consultas guardadas que mencionan USD **no se tocan** (es un registro) |
| D1 · cuándo se aplica el presupuesto | "Desde" y "Hasta" se aplican **al pulsar Buscar, Enter o salir del campo**, no en cada tecla. Los demás filtros siguen aplicándose al instante |
| D4 · formato al escribir | **Puntos de miles automáticos** y teclado numérico en el celular |
| D5 · aclaración junto al filtro | **No** se pone texto aclaratorio |
| D7 · presupuesto en la URL | **No** por ahora |
| Tipos de propiedad | Solo **Casa y Terreno**. En el sitio la casa se muestra como **"Vivienda"** (sin "depto"). El chat ya no pregunta "casa o departamento": de "Comprar/Alquilar una propiedad" pasa directo a la ciudad |

Las decisiones D2, D3, D6 y D8 de la propuesta original (moneda inicial, selector de moneda, etc.) quedaron **sin efecto** al no haber dos monedas.

**Ojo con "departamento":** también es la división territorial (Alto Paraná): filtro "Departamento", `departamentoId`, "departamento de Alto Paraná". Eso **sigue** y no debe tocarse.

## 8. W4: qué se hizo y cómo se verificó

Parte se hizo en claude.ai (`avance-w4.zip`, en `Downloads`) y el resto en Claude Code el 16/10. Todo está en la copia local, **sin commitear**.

### 8.1 Base de datos

| Archivo | Cambio |
|---|---|
| `database/migraciones/009_verificar_solo_guaranies.sql` (nuevo) | Solo lectura: restricción, valor por defecto y lista de precios en USD con cómo quedarán |
| `database/migraciones/009_solo_guaranies.sql` (nuevo) | `UPDATE … SET moneda='PYG' WHERE moneda='USD'` (mismo número), `CHECK (moneda = 'PYG')` y `DEFAULT 'PYG'`. Comprueba que no cambie ningún precio ni cantidad de operaciones |
| `database/migraciones/009_revertir_solo_guaranies.sql` (nuevo) | Vuelve a aceptar PYG y USD y quita el valor por defecto. **No devuelve precios a USD** |
| `database/schema/02_tablas.sql`, `database/seed/03_propiedades_demo.sql`, `database/pruebas/verificar.sql` | Base nueva igual a una migrada; pruebas "USD se rechaza" y "sin moneda se guarda en PYG" |
| `database/README.md`, `database/migraciones/README.md` | 009 documentada con su orden de aplicación |

**Ensayo en Claude Code** (bases de prueba en el mismo contenedor, ya borradas): migrar, repetir (se detiene), revertir, revertir otra vez (se detiene), volver a migrar; base migrada **idéntica** a una creada desde los `schema/` y `seed/` nuevos (esquema y las 28 operaciones); `verificar.sql` 20 de 20.

**Aplicada en la base de desarrollo el 16/10** (31 operaciones: 17 pasaron de USD a PYG, 14 ya estaban en PYG):

- La salida de `009_verificar` **no se guardó** y **no se hizo respaldo previo** (no se ejecutó la línea que definía `$fecha` y `$resp`).
- Se **reconstruyó** la lista de lo que estaba en USD comparando el respaldo del 14/10 con la base migrada: `C:\Users\sanie\respaldos-sn\009-verificacion-RECONSTRUIDA-20261016.txt` (prop-012 a prop-016 y terr-001 a terr-012; cuadra 17 de 17).
- Respaldo posterior a la migración: `C:\Users\sanie\respaldos-sn\base-20261016-1455-migrada-009.dump`.

**Orden obligatorio en cualquier otra base:** respaldo → `009_verificar` (guardar la salida) → `009_solo_guaranies` → backend 0.21.0. El backend 0.21.0 **no arranca bien contra una base sin migrar** (Hibernate falla al leer `'USD'`). El 0.20.0 sí funciona con la base migrada.

### 8.2 Backend (0.21.0)

| Archivo | Cambio |
|---|---|
| `pom.xml` | Versión 0.21.0 |
| `model/Moneda.java` | Solo `PYG` |
| `service/validacion/NormalizadorBusqueda.java` | Con presupuesto y sin moneda asume PYG; otra moneda da error `moneda`: "Solo se aceptan guaraníes (PYG)" |
| `service/validacion/ValidadorPropiedad.java` | Moneda de cada operación **opcional** (vacía → PYG); USD da error "Solo se aceptan precios en guaraníes (PYG)" |
| `service/asistente/ContextoPropiedades.java` | Precios para Gemini como "Venta Gs. 45.000; Alquiler Gs. 4.000.000 por mes" |
| `service/asistente/AsistenteService.java` | Ejemplo del prompt con "Casa", regla 4 (todo en guaraníes, no convertir) y **regla 9** (respetar el presupuesto escrito, sección 7.1) |
| `service/consultas/AnalisisConsultas.java` | Rangos solo en PYG, etiquetas "Gs. 300.000.000" |
| Comentarios y Javadoc | `PropiedadServlet`, `BusquedaRequest`, `PropiedadRequest`, `FiltroPropiedades`, `OperacionPropiedad` |
| Pruebas | Datos de prueba en PYG; pruebas nuevas de USD rechazado, rangos y presupuesto; `AsistenteServiceTest` actualizado al prompt nuevo |

**Verificado:** `mvn clean package` con la base migrada → **313 pruebas, 0 fallas, 2 omitidas** (las omitidas necesitan `VERIFICAR_DATOS_DEMO` o `PROBAR_GEMINI_REAL`). Sandra también lo corrió contra su base migrada y desplegó.

Quedan **a propósito** con "USD": `AsistenteServiceTest` y `GeminiClientTest` (ejemplos de respuestas del modelo) y las pruebas que verifican que USD se rechaza.

### 8.3 Panel

`admin/src/api/tipos.ts` (`Moneda = 'PYG'`), `EditorOperaciones.tsx` (sin selector; "Precio (Gs.)" / "Precio mensual (Gs.)"), `FormularioPropiedad.tsx` (envía `moneda: 'PYG'`), `formato.ts` (`textoGuaranies`), `ConsultasAnalisis.tsx`, `ui.module.css`.

**Verificado:** `tsc` y `build` correctos; **67 de 67** pruebas (versión del 16/10 de `admin/verificacion`, sacada de `Downloads\herramientas-verificacion(1).zip`).

### 8.4 Sitio

| Archivo | Cambio |
|---|---|
| `src/services/guaranies.ts` (nuevo) | `conPuntosDeMiles`, `textoGuaranies`, `textoPrecioOperacion`, `textoDeCampo` (máx. 13 cifras), `montoDeCampo` |
| `busqueda/SearchFilter.tsx` y `.module.css` | "Desde (Gs.)" y "Hasta (Gs.)"; errores locales y del backend bajo cada campo; aviso sin resultados; ya no envía `moneda`; opción **"Vivienda"**; barra alineada arriba y "Buscar" centrado con los campos (`.filterBar > .searchBtn { margin-top: 23px }`, porque `.searchBtn` también lo usa "Consultar sobre esta propiedad") |
| `PropertyMap/MapCanvas.tsx`, `home/OffersSection.tsx` | Precios con `textoPrecioOperacion` |
| `PropertyMap/types.ts`, `PropertyMap/mapUtils.ts` | `presupuestoMin`; `CategoriaPropiedad = "Casa" \| "Terreno"` (sin ícono de departamento) |
| `chat/AssistantWidget.tsx` y `.module.css` | Presupuesto escrito con "Omitir" (D12); sin el paso "casa o departamento"; la fila del resumen corta el texto largo |

**Verificado:** `tsc`, `build`, **66 de 66** pruebas (48 anteriores y 18 nuevas en `frontend/verificacion/w4.test.tsx`) y revisión visual (sección 10.3) en 1366 px y 390 px con contenido normal y de largo máximo: **0 problemas**, incluidos montos máximos, errores, aviso, alineación de la barra y resumen del chat con 150 caracteres.

### 8.5 Herramientas y documentación

- `herramientas/simulador.mjs`: propiedades en PYG al cargarlas, sin filtro por moneda, respuesta "Gs. 45.000".
- `docs/integracion-busqueda-presupuesto.md` reescrito; ejemplo de `backend/README.md` corregido.

## 9. Lo que falta para cerrar la W4

| # | Tarea | Detalle |
|---|---|---|
| 1 | Desplegar el cambio de tipos | `docker compose -f docker/docker-compose.yml up -d --build web` (la opción "Vivienda" y el chat sin "departamento" se hicieron después del despliegue) |
| 2 | Revisión de Sandra en el navegador | `/buscar` (Desde/Hasta, puntos al escribir, Buscar/Enter), chat (Comprar → ciudad → presupuesto escrito, "Omitir"), panel (sin selector de moneda) |
| 3 | Commit de Sandra | **No incluir** `herramientas/fuentes/` (tiene `node_modules`). `herramientas/`, `admin/verificacion/` y `frontend/verificacion/` se deciden en la Fase 17 |
| 4 | Contenido (Sandra, desde el panel) | Corregir los 17 precios de la lista reconstruida. **Inferido, a revisar:** alquileres mensuales muy altos cargados a mano: prop-004 Gs. 420.000.000, prop-005 Gs. 100.000.000, prop-006 Gs. 168.000.000, prop-010 Gs. 372.000.000. Siete títulos todavía dicen "Departamento en …" (prop-002, 004, 006, 007, 013, 014 y 015): son datos, se editan desde el panel |

## 10. Herramientas de verificación

Están en la copia local, **sin commitear** (se decide en la Fase 17). Su `README.md` explica cómo usarlas.

| Carpeta | Contenido |
|---|---|
| `admin/verificacion/` | 67 pruebas del panel (Vitest + jsdom + Testing Library) con backend simulado |
| `frontend/verificacion/` | 66 pruebas del sitio, incluido `w4.test.tsx` |
| `herramientas/` | `simulador.mjs` (backend simulado, puerto 8080), `capturar.py` (revisión visual con Playwright), JSON de contenido normal, largo y provisional, y `fuentes/` (Outfit y Fraunces instaladas con npm) |

### 10.1 Pruebas de interfaz

En `admin/` o en `frontend/`:

```powershell
npm install --no-save vitest@2 jsdom@25 @testing-library/react@16 @testing-library/dom@10
npx vitest run --config verificacion/vitest.config.ts
```

### 10.2 Base de prueba para migraciones y pruebas de integración

Crear una base aparte dentro del mismo contenedor (por ejemplo `sn_prueba`), desde `schema/` y `seed/` **anteriores** a la migración (los del último commit, con `git show HEAD:database/...`, copiados al contenedor con `docker cp`, porque `/database` montado ya tiene los nuevos), para ensayar:

- migrar, repetir, revertir y volver a migrar;
- comparar con una base creada desde los `schema/` y `seed/` nuevos (`pg_dump --schema-only`).

Para `mvn package`, apuntar `DB_URL` a esa base. No tocar `sn_inmobiliaria` sin respaldo. Borrar las bases de prueba al terminar.

### 10.3 Verificación visual del sitio

1. `node herramientas/simulador.mjs` (variable `CONTENIDO` con la ruta de `contenido_largo.json` para el contenido largo).
2. `cd frontend; npx vite --port 5173`.
3. `py herramientas/capturar.py <carpeta>` (Playwright ya está instalado).

Captura 7 páginas y el chat en 1366 px y 390 px, y escribe `informe.json` con los textos fuera de su recuadro o recortados, los desbordes horizontales y los emojis. **No cubre estados** (errores, avisos, pasos del chat) ni la **alineación**: para eso, escribir un script aparte que reutilice su revisión y medir con `getBoundingClientRect`. Además, **mirar las capturas**: la desalineación de la barra en la W4 solo se vio así.

## 11. Cómo trabajamos (la dinámica, obligatoria)

1. **Revisar el estado real** antes de proponer: el código actual, la estructura real de la base (con scripts de solo lectura) y lo que ya existe para reutilizar. Revisar también los cambios sin commitear (`git status`).
2. **Presentar el alcance**: qué cambia, en qué archivos y por qué. Plantear **decisiones con una recomendación**, en tablas. **Esperar la aprobación.**
3. **Implementar** solo lo aprobado, sin tocar archivos de más.
4. **Verificar**: compilación (`mvn package`, `tsc`, `build`), pruebas (agregar pruebas nuevas en cada entrega), migraciones ensayadas en una copia y verificación visual en el sitio.
5. **Entregar**: los cambios quedan **sin commitear** en la copia local, con una **tabla de revisión** ("qué hacer / qué esperar") y los comandos de PowerShell (respaldo, migración, `mvn package` **antes** de `docker compose up --build`, porque Docker compila sin pruebas).
6. **Sandra revisa y hace el commit.** **Nunca hacer commit ni push** y nunca avanzar a la etapa siguiente sin su aprobación.

### Preferencias de Sandra

- Todo en **español**, con explicaciones claras y analogías simples para lo técnico.
- Salidas **estructuradas en tablas**; le sirven los documentos exportables (Markdown o Word).
- **No entregarle imágenes ni capturas**: los resultados visuales se informan **en texto**.
- **Diseño sobrio, ordenado y funcional**, sin apariencia de plantilla de IA: bordes de 4 a 6 px en el panel, sin sombras marcadas ni adornos, íconos solo donde aportan, acciones principales distinguibles de las secundarias.
- **Que ningún texto se salga de su recuadro.**
- **No inventar datos**: lo no registrado se muestra como "No registrado", y lo inferido se etiqueta como "Inferido".
- **Datos personales**: minimización, acceso restringido y registro de la aceptación.
- Ser honesto: informar los errores propios y cómo se corrigieron.

## 12. Pendientes después de la W4

### Fase 17 (pruebas)

- Incorporar al repositorio las pruebas de interfaz (`admin/verificacion` y `frontend/verificacion`), con `vitest`, `jsdom` y `@testing-library/react` como dependencias de desarrollo y un script `npm test`. Decidir qué hacer con `herramientas/` (sin `fuentes/node_modules`).
- Backend: 313 pruebas (`mvn package`; las de integración necesitan `DB_URL` y otras variables).
- Pruebas de punta a punta con navegador real y una lista de recorridos manuales por rol.

### Fase 18 (documentación)

- Documentación técnica en `docs/`: arquitectura, API, roles, despliegue y manual del panel.

### Contenido que cargará Sandra desde el panel (no requiere programar)

- **Precios reales en guaraníes** (sección 9, tarea 4).
- Datos de contacto reales (hoy provisionales), imagen de portada propia, ofertas destacadas, responsables y cifras de Nosotros.
- Revisión legal del texto de aceptación (versión `2026-10`) y de la política de privacidad.

### Postergado o para el final

- Vista previa del contenido antes de publicar.
- Tema del panel guardado por cuenta (hoy por navegador).
- Límites de envío en nginx (`limit_req`).
- HTTPS y dominio; Tomcat como usuario no-root.
- Borrar las imágenes viejas del volumen.
- Eliminar lo que no se use (por ejemplo, `frontend/public/data/*.geojson`, que todavía tiene precios en USD y solo lo usa el simulador).
- **Descartados por Sandra:** tipo de cambio y conversión de monedas, estadísticas de visitas, mensajes recibidos y redes sociales.

## 13. Lecciones aprendidas (evitar repetir)

| Problema | Regla |
|---|---|
| Entidad JPA nueva sin registrar en `persistence.xml` (`exclude-unlisted-classes=true`) | Agregarla a `persistence.xml` o usar JDBC directo |
| La base de Tailwind muestra los SVG como bloque | `.lucide { display: inline-block }` y botones con ícono en `inline-flex` (ya está en `frontend/src/index.css`) |
| Alturas en porcentaje del mapa fallaban | Usar `position: absolute; inset: 0` |
| Quitar estilos con expresiones regulares rompió una regla con varios selectores | Revisar los selectores compartidos y comparar contra la versión anterior |
| Un reemplazo masivo de `"USD"` por `"PYG"` en una prueba cambió también la prueba nueva que debía enviar `"USD"` | Reemplazar línea por línea y comprobar cada coincidencia; nunca reemplazos globales en pruebas |
| Un resultado obtenido con un servicio caído no vale | Comprobar que la base de prueba, Vite y el simulador estén activos antes de cada verificación |
| `pkill -f` con un patrón que coincide con el propio comando cierra la sesión | En Windows, detener por puerto: `Get-NetTCPConnection -LocalPort 8080` y `Stop-Process` |
| El zip de la rama `main` era la versión original | Trabajar sobre `reorganizacion-estructura` |
| Comparar contra una copia vieja | Comparar siempre contra lo realmente aplicado (`git diff`) |
| Un `import` faltante en un cambio sin commitear | Revisar también los cambios sin commitear |
| Se cambió el texto del prompt pero no las pruebas que lo verifican | Al cambiar `INSTRUCCIONES`, buscar sus `contains(...)` en `AsistenteServiceTest` |
| Las pruebas del panel eran la versión vieja del zip (64 en vez de 67) | Contar las pruebas y comparar con lo esperado antes de confiar en el resultado |
| Comandos de la tabla de revisión que dependían de variables de otro paso: sin `$fecha`/`$resp` no se guardó la verificación ni se hizo el respaldo | Cada bloque de comandos para Sandra debe ser **independiente** (definir sus variables) y detenerse si falla el respaldo |
| Un cambio de CSS en una clase compartida (`.searchBtn`) movía otro botón | Antes de cambiar una clase, buscar todos sus usos (`grep`) y limitar el cambio con un selector más específico |
| La revisión automática dio 0 problemas pero había una desalineación | Mirar las capturas además del informe |
| `python` mezclado con `node` en un solo comando quedó colgado | Usar `py` con `timeout` y no encadenar intérpretes |

## 14. Comandos de referencia (PowerShell, desde `C:\Proyectos\sn-inmobiliaria`)

Cada bloque define sus propias variables: se puede pegar por separado.

```powershell
# Base de desarrollo
docker compose -f docker/docker-compose.yml up -d db

# Respaldo (antes de cada migración)
$fecha = Get-Date -Format 'yyyyMMdd-HHmm'; $resp = "$HOME\respaldos-sn"; New-Item -ItemType Directory -Force $resp | Out-Null
docker compose -f docker/docker-compose.yml exec db pg_dump -U sn_app -d sn_inmobiliaria -Fc -f /tmp/respaldo.dump
docker compose -f docker/docker-compose.yml cp db:/tmp/respaldo.dump "$resp\base-$fecha.dump"
Get-Item "$resp\base-$fecha.dump"   # debe existir y tener tamaño; si no, NO migrar

# Verificación previa de una migración (guardar la salida)
$fecha = Get-Date -Format 'yyyyMMdd-HHmm'; $resp = "$HOME\respaldos-sn"
docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/migraciones/00X_verificar_....sql | Tee-Object "$resp\00X-verificacion-$fecha.txt"

# Variables para las pruebas del backend
function Leer-Env($n) { (Select-String -Path docker\.env -Pattern "^$n=").Line.Split('=', 2)[1].Trim() }
$env:DB_URL = "jdbc:postgresql://localhost:5433/sn_inmobiliaria"; $env:DB_USER = "sn_app"
$env:DB_PASSWORD = Leer-Env "DB_PASSWORD"; $env:ADMIN_EMAIL = Leer-Env "ADMIN_EMAIL"; $env:ADMIN_PASSWORD = Leer-Env "ADMIN_PASSWORD"
mvn -f backend/pom.xml clean package | Select-String "Tests run: [0-9]+, F.*Skipped: [0-9]+$|BUILD"

# Sitio y panel
cd frontend; npx tsc --noEmit; npm run build; cd ..
cd admin; npx tsc --noEmit; npm run build; cd ..

# Sistema completo (después de que pasen las pruebas)
docker compose -f docker/docker-compose.yml up -d --build backend web
```

URLs: sitio `http://localhost/` y panel `http://localhost/admin/`. En desarrollo: sitio en `5173` y panel en `5174/admin/`.
