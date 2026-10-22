# S&N Inmobiliaria — Contexto del proyecto (para Claude Code)

> Documento de traspaso. Léelo completo antes de proponer o modificar nada.
> Estado al **22 de octubre de 2026 (preparación final de la entrega)**. Trabajo en equipo, con Claude Code como asistente.
>
> Este archivo se llama `CLAUDE.md` y va en la raíz del repositorio: Claude Code lo lee solo al iniciar cada sesión.

---

## 0. Lo más importante, en cinco líneas

1. Todo lo hecho hasta la **preparación de la entrega (19/10)** está **commiteado** (rama `reorganizacion-estructura`, último commit `34309b5`): W1 a W4, migraciones 001 a 011, backend **0.24.0**, barrio sin panel lateral, limpieza y `README.md`. `main` sigue en el punto de partida.
2. La base de desarrollo tiene **aplicadas las migraciones hasta la 011** y Docker corre el backend 0.24.0 (comprobado el 19/10 con `/api/health` y la tabla `auditoria`).
3. La **Fase 17 (pruebas)** (19/10) y los **ajustes finales del 22/10** (backend **0.25.0**: modelo por defecto, IP real para el límite de intentos; aviso de permisos en el editor del sitio; títulos de demostración) están **sin commitear**. La Fase 17 sube a Git las pruebas de interfaz y las herramientas, agrega un **entorno de pruebas** de Docker separado del real, los recorridos de punta a punta y los manuales por rol, y pasa el GeoJSON de demostración a PYG (secciones 9 y 10). Guía para personas: `docs/pruebas.md`.
4. Lo pendiente para considerar el proyecto terminado está en la sección 9; lo que queda para después, en la sección 12.
5. Dinámica obligatoria (sección 11): revisar, proponer con decisiones, **esperar aprobación**, implementar, verificar, tabla de revisión. **Nunca hacer commit ni push**: lo hace el equipo.

---

## 1. Qué es el proyecto

Plataforma inmobiliaria para **S&N Inmobiliaria**, que opera en **Alto Paraná, Paraguay** (Ciudad del Este, Hernandarias, Minga Guazú y Presidente Franco).

| Parte | Para quién | Qué hace |
|---|---|---|
| **Sitio público** (`frontend/`) | Clientes | Buscar propiedades en un mapa por departamento, ciudad y barrio; ver el detalle; asistente virtual con IA (Gemini); páginas Inicio, Nosotros (con los servicios) y Contacto |
| **Panel administrativo** (`admin/`) | Equipo de la inmobiliaria | Propiedades, consultas del chat IA, clientes potenciales, usuarios y roles, contenido del sitio y la propia cuenta |
| **Backend** (`backend/`) + **base** (`database/`) | — | API REST en Java y PostgreSQL con PostGIS |

## 2. Tecnologías

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Maven (WAR), Servlets Jakarta EE en Tomcat 10.1, JPA/Hibernate 6 (más JDBC directo en algunos módulos), HikariCP, Jackson, BCrypt, SLF4J + Logback, JUnit 5, Gemini API (`gemini-3.5-flash-lite`, por defecto en el código y en `.env.example` desde el 22/10; `thinkingLevel=minimal`) |
| Base | PostgreSQL 16 + PostGIS 3.4 |
| Sitio y panel | React 19, TypeScript, Vite 8, React Router 7, Leaflet / react-leaflet, Tailwind CSS 4 (solo la base), CSS Modules, íconos `lucide-react` 1.34.0 |
| Docker | `docker/docker-compose.yml` con 4 servicios: `db`, `imagenes-init`, `backend` y `web` (nginx 1.27 sirve el sitio en `/` y el panel en `/admin/`, y reenvía `/api/` y `/uploads/` al backend) |

## 3. Estructura y entorno

```
C:\Proyectos\sn-inmobiliaria          (rama de trabajo: reorganizacion-estructura; main quedó en el punto de partida)
├── frontend/   sitio público
├── admin/      panel administrativo (independiente; se sirve en /admin/)
├── backend/    Java (0.25.0 sin commitear; en Docker corre el 0.24.0 hasta redesplegar)
├── database/   schema/, seed/, datos/ (INE 2022), migraciones/ (001 a 011, commiteadas y aplicadas en la base de desarrollo), pruebas/
├── docker/     docker-compose.yml, docker-compose.pruebas.yml (entorno de pruebas), Dockerfiles, nginx, .env y .env.pruebas (NUNCA en Git), plantillas, README
├── docs/       pruebas.md, recorridos-manuales.md y contratos de integración para el sitio público
└── herramientas/, admin/verificacion/, frontend/verificacion/   pruebas y herramientas de verificación (Fase 17, sin commitear; sección 10)
README.md       documentación para personas (propósito, requisitos, variables, Docker, pruebas). CLAUDE.md es para Claude Code
```

- Volúmenes externos (Compose nunca los crea ni borra): `sn-inmobiliaria_pgdata` (base) y `sn-uploads` (fotos).
- **Entorno de pruebas** (Fase 17): proyecto `sn-prueba`, sitio en **8081**, base `sn_prueba` en **5434** (en memoria, nace de `schema/` y `seed/`), fotos en su propio volumen `sn-prueba_imagenes`, imágenes `:prueba`. No hace referencia a los volúmenes reales. Variables en `docker/.env.pruebas` (local; en esta máquina tiene contraseñas al azar y el administrador `prueba@sn.test`). **Usarlo para toda prueba que escriba en la base** (sección 10.2).
- Base de desarrollo: puerto **5433**, usuario `sn_app`, base `sn_inmobiliaria`. **Es la base con los datos reales**: nunca ensayar migraciones en ella sin respaldo, y preferir una base de prueba (sección 10.2).
- Respaldos: `C:\Users\sanie\respaldos-sn` (dumps `base-AAAAMMDD-HHMM.dump`).
- Sistema: **Windows con PowerShell**, Docker Desktop (si está apagado, el daemon no responde: hay que iniciarlo). La copia local usa finales de línea CRLF; `.gitattributes` fuerza LF en `*.sh`, `*.sql`, `*.yml` y `*.yaml`.
- Instalados en la máquina: Java 21, Maven 3.9.16, Node, Python 3.14 (`py`), Playwright con Chromium (instalado con permiso el 16/10).
- La pantalla de la PC de desarrollo tiene **escala mayor al 100 %**: en sus capturas todo se ve un 14 % más grande, y no es un error.

## 4. Estado de las fases

| Fases | Estado |
|---|---|
| 1 a 14 | Completas: análisis, arquitectura, PostgreSQL, entidades, DAOs, servicios, DTOs, servlets públicos, autenticación, CRUD administrativo, imágenes, filtro por departamento, ubicación por PostGIS, asistente Gemini |
| 15A, 15B, 15C | Completas: sitio conectado a la API, panel independiente, 66 fotos migradas a `/uploads` |
| 16 | Completa: sistema completo con Docker Compose |
| Mejoras del panel | Completas (sección 6) |
| Sitio público | W1 a W4 **commiteadas** (secciones 7 y 8) |
| **17 (pruebas)** | Implementada el 19/10, **sin commitear** (sección 9.1). Gemini real pasó el 19/10 con `gemini-3.5-flash-lite` |
| **18 (documentación)** | Pendiente (sección 12) |

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
| **009** | **Solo guaraníes**: USD → PYG con el mismo número, `CHECK (moneda = 'PYG')` y `DEFAULT 'PYG'`. **Aplicada en la base de desarrollo el 16/10** (sección 8.1) |
| **010** | **Barrios oficiales del INE 2022 de todo Alto Paraná**: 22 ciudades y 714 barrios y localidades (urbanos y rurales), los 12 de antes pasan al contorno exacto (< 1 m), sin nombre único por ciudad. Datos en `database/datos/ine2022_alto_parana.sql` (10 MB, generado por `generar_ine2022.mjs`), compartido con `seed/02_ubicacion.sql`. Ensayada (18/10). **Aplicada** en la base de desarrollo (comprobado el 19/10 por la API: 217 barrios en las 4 ciudades con propiedades) |
| **011** | **Auditoría y cierre de sesiones**: `usuario.version_credencial` (sube al restablecer la contraseña: las sesiones abiertas con el número anterior se cierran) y tabla `auditoria` (fecha, acción, quién y a quién; sin contraseñas ni IP). Ensayada (19/10). **Aplicada** en la base de desarrollo (comprobado el 19/10: existe `auditoria`). El backend 0.24.0 **necesita** la 011 |

Se aplican con:

```powershell
docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -v ON_ERROR_STOP=1 -f /database/migraciones/00X_....sql
```

## 6. Panel administrativo (completo)

| Sección | Funciones |
|---|---|
| Estructura | Menú lateral (en celular se abre con un botón), carga diferida, tema claro, oscuro o según el sistema (por navegador), botón Volver. Desde el 18/10 el menú es **una sola lista sin subtítulos**: Inicio, Propiedades, Consultas IA, Gestión del sitio web, Usuarios y Mi cuenta (cada una según el permiso). El tipo "Casa" se **muestra** como "Vivienda" (`nombreDelTipo` en `formato.ts`; el valor enviado sigue siendo "Casa") |
| **Inicio** (18/10) | `/admin/` y destino fijo al iniciar sesión (ya no se vuelve a la última pantalla). Saludo según la hora con el nombre ("Buenas tardes, <nombre>", sin "Bienvenido/a") y una frase al azar. Fichas centradas según el rol (siempre 4 hoy): **clientes por contactar** (cuenta `conteos.pendientes`, destacada en ámbar si hay alguno, lleva a la lista filtrada `?estado=PENDIENTE`; al vendedor le cuenta solo los suyos), Nueva propiedad, y luego Nuevo usuario y Sitio web (administrador), Sitio web y Consultas IA (gerente), Consultas IA y Listado (vendedor). "Nueva propiedad" salió del menú: está en el listado. Archivo `paginas/Inicio.tsx`. **Desde el 19/10**, imagen de fondo (`src/assets/inicio-fondo.webp`, 1280 px, 17 KB) desenfocada 3 px, alineada a la derecha, con una capa del color de fondo encima (`--velo-inicio`: 55 % en claro, 80 % en oscuro) |
| Propiedades | Listado, alta y edición con mapa (ubicación por PostGIS), operaciones según el tipo, fotos, baja lógica y responsable (el vendedor solo edita las suyas) |
| Gestión del sitio web | Pestañas Inicio (portada y ofertas destacadas), Nosotros (con los servicios) y Contacto. Se publica al guardar |
| Consultas IA | Pestañas Historial (paginado, revisión interna), Análisis técnico y Clientes potenciales. **Desde el 18/10** el Análisis técnico muestra solo 7 indicadores: Preguntas recibidas por el asistente y Conversaciones (números), Sin servicio (medidor circular) y Propiedades más consultadas, Zonas más consultadas, Rangos de precios y Temas consultados (inferido) en **gráficos circulares con porcentajes** (las 5 principales y el resto en "Otras"; leyenda con cantidad y %; porcentajes que suman 100). Componentes `Torta`, `Medidor` y `porcentajes` en `componentes/fichas.tsx`; colores `--serie-1…5`, `--serie-otras` y `--pista-grafico` en `global.css` (claro y oscuro). **Calidad de respuestas se retiró** (`/consultas/calidad` lleva al Historial); el backend no cambió y sigue enviando todos los datos |
| Usuarios | Alta con contraseña temporal (se muestra una vez), edición, rol, activar y desactivar. **Recuperación asistida (19/10):** "Generar contraseña temporal" (solo ADMIN) además **cierra las sesiones abiertas** de ese usuario y queda en la **auditoría**. Nadie puede ver contraseñas actuales |
| Mi cuenta | Datos propios, cambio de email (con contraseña) y de contraseña, preferencia de tema. Todos los campos de contraseña (inicio de sesión, cambiar contraseña y confirmar el cambio de email) tienen el **botón del ojo** (`componentes/CampoContrasena.tsx`; se oculta el ojo propio de Edge) |

### Roles y permisos (fijos por rol, verificados en el backend)

| Permiso | ADMIN | GERENTE | VENDEDOR |
|---|:-:|:-:|:-:|
| Ver propiedades | ✓ | ✓ | ✓ |
| Crear y editar propiedades | Todas | Todas | Solo las suyas (crea a su nombre) |
| Dar de baja y asignar responsable | ✓ | ✓ | — |
| Gestión del sitio web | ✓ | ✓ | — |
| Consultas IA: historial | Todas | Todas | Generales y de sus propiedades |
| Análisis técnico | ✓ | ✓ | — |
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
| **W4** Presupuesto y solo guaraníes | ✅ | Ver 7.1 y sección 8 |

### 7.1 W4: decisiones aprobadas

La idea inicial era elegir guaraníes o dólares. **Se decidió eliminar el dólar de todo el sistema** "para evitar problemas".

| # | Decisión aprobada |
|---|---|
| Moneda | **Solo guaraníes (PYG)** en todo el sistema. El panel ya no permite cargar USD |
| D9 · precios en USD existentes | Pasan a PYG **con el mismo número, sin conversión** (45.000 USD queda Gs. 45.000). Se corrigen a mano desde el panel. El tipo de cambio que dio (1 USD = 5.692,38 Gs.) **no se usa** |
| D10 · columna `moneda` | Se **conserva**, limitada a `CHECK (moneda = 'PYG')` con `DEFAULT 'PYG'`. La API mantiene `"moneda": "PYG"` |
| D11 · formato en pantalla | **"Gs. 450.000.000"** (punto de miles, sin decimales) en el sitio y el panel; alquiler con "/ mes" |
| D12 · presupuesto en el chat | **Ni botones ni campo de monto**: el bot pregunta el presupuesto y el cliente lo **escribe con sus palabras** en el cuadro normal del chat ("entre 300 y 500 millones"). Queda **tal cual** en el resumen y en el WhatsApp. Botón **"Omitir"** → "No indicado". Máximo 150 caracteres. No se envía a Gemini |
| D12b · presupuesto en preguntas libres | Regla 9 de las instrucciones del asistente: si el cliente indica un presupuesto, Gemini menciona solo las propiedades que entran en ese rango ("millones" = 1.000.000) y, si no hay, lo dice |
| D13 · historial de consultas | Las consultas guardadas que mencionan USD **no se tocan** (es un registro) |
| D1 · cuándo se aplican los filtros | **Reemplazada el 17/10:** toda la barra (departamento, ciudad, barrio, tipo, operación, "Desde" y "Hasta") se aplica **solo al pulsar Buscar** (o Enter en el presupuesto); salir del campo no busca. Las listas de la barra se ajustan entre sí sin buscar. Los clics en el mapa (ciudad, barrio, propiedad, "Volver") se aplican al instante y actualizan la barra. En el código: estado `borrador` (barra) y `filtros` (aplicado) en `SearchFilter.tsx` |
| D4 · formato al escribir | **Puntos de miles automáticos** y teclado numérico en el celular |
| D5 · aclaración junto al filtro | **No** se pone texto aclaratorio |
| D7 · presupuesto en la URL | **No** por ahora |
| Tipografía (18/10, reemplaza la del 17/10) | **Roboto** en el sitio y en el panel. Sitio: se carga con los `<link>` de Google Fonts en `frontend/index.html` (ya no hay `@import` en `index.css`); `--fuente-titulos` y `--fuente-texto` son Roboto y los **títulos importantes van en 700** (portada, títulos de página y de sección, ofertas, búsqueda, detalle, Nosotros, Servicios, Contacto). Panel (`global.css`): **títulos en 700** (página 26 px, bloque 19 px), etiquetas, menú, pestañas y botones en **600**, contenido en 400. Las palabras con asteriscos siguen en **verde, sin cursiva**. Antes: Montserrat e Inter (y antes Fraunces y Outfit) |
| Textos de ayuda del panel (18/10) | Se quitaron los repetidos (aclaración de la revisión interna, alcance del vendedor, subtítulos de Clientes potenciales, Usuarios y Mi cuenta, "Los cambios se publican…", etc.). Se mantienen los que evitan errores: contraseña temporal y sus requisitos, asteriscos, cifras reales, formatos de imagen, instrucción del mapa y avisos de estado |
| Página de búsqueda (17/10) | Margen verde de **40 px** (PC) y **20 px** (≤ 900 px) alrededor de la barra, el mapa y el panel, con bordes redondeados, para poder bajar la página con la rueda (sobre el mapa la rueda hace zoom). Fichas más compactas |
| Servicios dentro de Nosotros (18/10, backend 0.22.0) | La página Servicios se unió a Nosotros: **historia → valores → servicios → invitación a buscar**. `/servicios` redirige a `/nosotros#servicios`; "Servicios" salió del menú y del pie. Se retiraron "Lo que tenemos para vos" (con su carrusel) y "¿Por qué elegir S&N?" (con su imagen). En el panel la pestaña Servicios desapareció (`/sitio/servicios` redirige a Nosotros): Nosotros edita las dos secciones (`NOSOTROS` y `SERVICIOS`) con **un solo botón**, que avisa qué parte se publicó si una falla, y guarda vacíos `razones`, `bloqueTitulo` y `bloqueTexto`. El backend acepta 0 razones |
| Zonas del mapa (18/10, migración 010 y backend 0.23.0) | Se cargan **todos los barrios y localidades de Alto Paraná del INE 2022** (opción A; dibujar zonas desde el panel queda para más adelante si hace falta). Nombres repetidos permitidos. `/api/barrios` devuelve contornos **simplificados para dibujar** (`TOLERANCIA_DIBUJO` = 0.00003°, unos 3 m; la validación usa el exacto). El sitio pide `conPropiedades=true`: mapa y filtro solo con **ciudades con propiedades Disponibles** y todos sus barrios; el panel pide todo. Otro departamento: descargar su GeoJSON del INE y repetir el procedimiento |
| Zonas con propiedades (19/10, backend 0.24.0) | El mapa y las listas Ciudad y Barrio muestran **solo los barrios con al menos una propiedad Disponible** y sus ciudades ("N barrio(s)" recalculado). Lo calcula el sitio con la lista de propiedades (`soloConPropiedades` en `useGeoData.ts`), que nunca se guarda en el navegador: un barrio aparece o desaparece al instante. Los contornos con `conPropiedades=true` se guardan **5 minutos** (antes 1 hora); sin ese parámetro (panel), 1 hora |
| Barrio sin panel lateral (19/10, reemplaza "La X del detalle" del 18/10) | Se quitó el panel lateral del barrio ("Propiedades que hay en este barrio", con sus contadores y tarjetas). Al elegir un barrio (clic en el mapa, o Ciudad + Barrio y Buscar) el **mapa a pantalla completa** se acerca a esa zona, la resalta y muestra sus marcadores; un marcador abre el detalle. La **X del detalle** vuelve al mapa **como estaba antes de abrir la propiedad**: mismos filtros, barra, barrio, centro y zoom (`antesDelDetalle` y `vistaRestaurada` en `SearchFilter.tsx`; `vistaRestaurada` y `onVistaCambia` en `MapCanvas.tsx`). Si la propiedad se abrió desde un enlace (ofertas de la portada), vuelve al resultado de la última búsqueda. "Volver" en el mapa: barrio → ciudad → mapa general |
| Textos sobre el mapa (19/10) | Se quitaron "Tenemos propiedades en los barrios de …" y "… barrios/zona: …". Quedan la ruta con "Volver" dentro del mapa ("Alto Paraná › Ciudad › Barrio", nunca debajo de los botones de zoom) y el aviso inferior ("Hacé clic en un marcador…" o "No hay propiedades en este barrio con estos filtros…"). El encuadre mide la ruta y el aviso para no dejar marcadores debajo, y el mapa se vuelve a medir antes de encuadrar (`invalidateSize`): en el celular la barra cambia de alto |
| Tipos de propiedad | Solo **Casa y Terreno**. En el sitio la casa se muestra como **"Vivienda"** (sin "depto"), también en la etiqueta de las ofertas (`grupoDeCategoria`, 19/10). El chat ya no pregunta "casa o departamento": de "Comprar/Alquilar una propiedad" pasa directo a la ciudad |

Las decisiones D2, D3, D6 y D8 de la propuesta original (moneda inicial, selector de moneda, etc.) quedaron **sin efecto** al no haber dos monedas.

**Ojo con "departamento":** también es la división territorial (Alto Paraná): filtro "Departamento", `departamentoId`, "departamento de Alto Paraná". Eso **sigue** y no debe tocarse.

## 8. W4: qué se hizo y cómo se verificó

Parte se hizo en claude.ai (`avance-w4.zip`, en `Downloads`) y el resto en Claude Code el 16/10. Es el registro histórico de la W4: todo quedó **commiteado** después. Las cifras de pruebas de esta sección son las de ese momento; las actuales están en las secciones 9 y 10.

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

**Verificado:** `mvn clean package` con la base migrada → **313 pruebas, 0 fallas, 2 omitidas** (las omitidas necesitan `VERIFICAR_DATOS_DEMO` o `PROBAR_GEMINI_REAL`). También se corrió contra la base migrada y se desplegó.

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

## 9. Entrega: estado al 19/10 y pendientes

### 9.1 Fase 17: pruebas (19/10, sin commitear)

La preparación de la entrega (barrio sin panel lateral, sin textos sobre el mapa, Vivienda en las ofertas, limpieza de código sin uso y `README.md`) quedó commiteada en `34309b5`.

| Cambio | Archivos |
|---|---|
| Pruebas de interfaz en Git, con `npm test` | `admin/verificacion/`, `frontend/verificacion/`; `package.json` y `package-lock.json` de `admin/` y `frontend/` (devDependencies `vitest ^2.1.9`, `jsdom ^25.0.1`, `@testing-library/react ^16.3.3`, `@testing-library/dom ^10.4.2`, las mismas versiones de antes; ningún paquete existente cambió de versión) |
| Herramientas en Git | `herramientas/`: `simulador.mjs`, `capturar.py` (sin imports repetidos; el chat usa `SITIO` en lugar de `localhost:5173` fijo), JSON de contenido (datos ficticios), `fuentes/package.json` solo con Roboto, `README.md` reescrito |
| Entorno de pruebas en Docker | `docker/docker-compose.pruebas.yml`, `docker/pruebas.env.example` (sección 3) |
| GeoJSON de demostración en PYG | `frontend/public/data/propiedades.geojson`: 28 líneas `"moneda": "USD"` → `"PYG"` con el mismo número (como la 009). Antes se comparó campo por campo con la API: la moneda era la **única** diferencia |
| Seguro de la prueba con Gemini real | `AsistenteIntegracionTest`: se omite si `DB_URL` apunta a `sn_inmobiliaria`; prueba nueva `SeguroGeminiRealTest` (2) |
| Recorridos de punta a punta | `herramientas/recorridos_e2e.py` (Python + Playwright): búsqueda y detalle (1366 y 390 px), chat con "Dejar mis datos", inicio de sesión y alta de propiedad. Se niega a correr contra `http://localhost`; limpia solo lo que crea (marca `E2E <fecha-hora>`) |
| Documentación | `docs/pruebas.md` y `docs/recorridos-manuales.md` (nuevos); `README.md`, `backend/README.md`, `docker/README.md`, `herramientas/README.md`, `CLAUDE.md`; `.gitignore` (`/capturas/`) |

**Verificado el 19/10:**

| Verificación | Resultado |
|---|---|
| Instalación limpia (copia sin `node_modules`): `npm ci`, `npm test`, `tsc`, `build` | Panel **92 de 92**, sitio **83 de 83**, compilan |
| Backend sin base | **328 pruebas, 0 fallas, 67 omitidas** |
| Backend contra el entorno de pruebas (con `VERIFICAR_DATOS_DEMO=true`) | **328, 0 fallas, 1 omitida** (Gemini real). `losDatosDeDemostracionCoincidenConElGeoJsonOriginal`, que fallaba desde la 009, pasa |
| `verificar.sql` en el entorno de pruebas | 24 de 24 |
| Seguro de Gemini (con una copia llamada `sn_inmobiliaria` **dentro del contenedor de pruebas** y una clave falsa) | Omitida, como debe |
| Gemini real en `sn_prueba` | Con `gemini-3.1-flash-lite`: **omitida** (HTTP 503 y una vez sin respuesta a tiempo) en 4 intentos. Base de desarrollo sin cambios (21 consultas, última 235, antes y después). La clave es válida (lista de modelos, sin cuota). `gemini-2.5-flash-lite`: **falla** con 404 (no disponible para claves nuevas, aunque figura en la lista). Con `gemini-3.5-flash-lite` y `gemini-3.5-flash`: **pasa** (3 de 3, 0 omitidas; 19/10) |
| Modelo del sistema real | `GEMINI_MODEL` pasó a `gemini-3.5-flash-lite` en el `docker/.env` de desarrollo (19/10); el chat en `localhost` responde. Desde el 22/10 también es el valor por defecto (9.3) |
| Recorridos de punta a punta (2 ejecuciones seguidas) | **7 de 7**; la base de prueba vuelve a 28 propiedades y 0 clientes potenciales |
| Seguro de los recorridos (`http://localhost`, `:80`, `127.0.0.1`) | Se niega, código 1 |
| `capturar.py` con el simulador | 16 capturas, 0 problemas; Roboto local y chat correctos (mirado) |
| Secretos | Ningún valor de `docker/.env` ni `docker/.env.pruebas` aparece en los 40 archivos nuevos o modificados; `docker/.env.pruebas` ignorado por Git |

Recorridos manuales: sondeados con un gerente y un vendedor creados en el entorno de pruebas (borrados después). Observación: un vendedor que escribe `/admin/sitio/inicio` ve el editor vacío con "Guardar y publicar" desactivado (el backend responde 403 y no cambió nada); en otras rutas ve "No tiene permisos para esta operación". No se cambió (no es un problema de seguridad).

### 9.3 Ajustes finales (22/10, sin commitear; backend 0.25.0)

| Cambio | Archivos |
|---|---|
| Modelo de Gemini por defecto `gemini-3.5-flash-lite` (el 3.1 daba 503 sostenido; los 2.5 dan 404 con claves nuevas) | `config/AsistenteConfig.java`, `AsistenteConfigTest`, `docker/.env.example`, `README.md`, `backend/README.md`, `docs/pruebas.md` |
| Límite de intentos por **IP real**: `RemoteIpValve` con `X-Real-IP` (nginx la reemplaza con `$remote_addr`), solo desde 172.16.0.0/12; `protocolHeader` vacío (no cambia esquema ni puerto). No se usa `X-Forwarded-For` porque nginx le agrega lo que manda el visitante. nginx sin cambios | `src/main/webapp/META-INF/context.xml`, `backend/README.md` |
| Aviso "No tiene permisos para gestionar el sitio web." en `/sitio/inicio`, `/nosotros` y `/contacto` sin `CONTENIDO_EDITAR` (antes: editor vacío). Componente general `auth/RutaConPermiso.tsx`; 2 pruebas nuevas (falla sin el cambio) | `admin/src/App.tsx`, `admin/src/auth/RutaConPermiso.tsx`, `admin/verificacion/panel.test.tsx` |
| Títulos de demostración sin "Departamento en …": los del panel del equipo, salvo prop-002 y prop-007 (en la demo son Venta: "Casa en venta en …", para no repetir títulos) y prop-013 sin el error de tipeo. Mismo cambio en el GeoJSON (la prueba los compara) | `database/seed/03_propiedades_demo.sql` (títulos y comentarios), `frontend/public/data/propiedades.geojson`, `database/README.md` |
| Sin nombres de personas en los archivos (datos de prueba: "Paula") | `CLAUDE.md`, pruebas del panel y del backend |
| Versión | `backend/pom.xml` 0.25.0; `docs/recorridos-manuales.md` (4.10 y 5.4) |

**Verificado el 22/10:** backend sin base **328, 0 fallas, 67 omitidas**; contra el entorno de pruebas con `VERIFICAR_DATOS_DEMO=true` **328, 0 fallas, 1 omitida** (Gemini real sin activar); panel `tsc`, `build` y **94 de 94**; recorridos de punta a punta **7 de 7**; en el navegador, el vendedor ve el aviso en las 3 pestañas, sin botones de guardar ni pedidos a `/api/admin/sitio`; registro del backend con `modelo=gemini-3.5-flash-lite` sin variable; una base nueva tiene los títulos nuevos; `/api/health` 0.25.0. **Límite por IP** (entorno de pruebas): 5 fallos desde la PC → la contraseña correcta da 429; con `X-Real-IP` y `X-Forwarded-For` inventadas sigue en 429; desde otro contenedor (otra IP) entra con 200. No se probó el caso de control con el backend anterior. Con Docker Desktop, todos los visitantes externos pueden llegar con la misma IP interna: ahí el límite queda como antes (por email); en un servidor real, por IP.

### 9.2 Pendientes para considerar el proyecto completamente listo

| # | Pendiente | Detalle |
|---|---|---|
| 1 | Revisión y commit de la 9.1 | No hace falta redesplegar: no cambió nada que se ejecute en el sistema (solo pruebas, herramientas, documentación, dependencias de desarrollo y un GeoJSON que el sitio no lee). El `web` se construye con `npm ci`, que ahora también instala las dependencias de prueba |
| 2 | Gemini real | **Resuelto el 19/10**: pasa con `gemini-3.5-flash-lite` (9.1) |
| 3 | Contenido (desde el panel) | Precios y títulos corregidos por el equipo (22/10: ningún título "Departamento en …"). **Inferido, a revisar:** prop-012 Venta Gs. 10.000.000 (bajo para una vivienda) y terr-002 Venta Gs. 18.000.000; prop-013 dice "Casa disposnible…" (error de tipeo) |
| 4 | Límite de intentos de inicio de sesión | **Resuelto el 22/10** (9.3): por IP real + email |
| 5 | Aviso de permisos en el editor del sitio para el vendedor | **Resuelto el 22/10** (9.3) |
| 6 | Documentación para el docente (Fase 18) y llevar el trabajo a `main` | El equipo pasa su documentación para adaptarla; después, commit, push y `main` guiados |

## 10. Herramientas de verificación

Desde la Fase 17 entran al repositorio (sin commitear todavía). La guía para personas, con comandos y resultados esperados, es `docs/pruebas.md`; los recorridos manuales, `docs/recorridos-manuales.md`.

| Carpeta o archivo | Contenido |
|---|---|
| `admin/verificacion/` | 92 pruebas del panel (Vitest + jsdom + Testing Library) con backend simulado |
| `frontend/verificacion/` | 83 pruebas del sitio, incluidos `w4`, `tipografia`, `zonas` y `volver-resultados` |
| `herramientas/recorridos_e2e.py` | 7 recorridos de punta a punta contra el entorno de pruebas |
| `herramientas/` (resto) | `simulador.mjs` (backend simulado, puerto 8080), `capturar.py` (revisión visual), JSON de contenido normal, largo y provisional, y `fuentes/` (Roboto; su lista debe coincidir con `frontend/index.html`) |
| `docker/docker-compose.pruebas.yml` | Entorno de pruebas (sección 3) |

### 10.1 Pruebas de interfaz

En `admin/` o en `frontend/`: `npm ci` (la primera vez) y `npm test`.

### 10.2 Base de prueba para migraciones y pruebas de integración

Para pruebas de integración y recorridos: el **entorno de pruebas** (sección 3), que ya tiene `sn_prueba` y no toca la base real. Para **ensayar una migración**: crear una base aparte (en el contenedor `sn-prueba-db-1`, o en el real sin tocar `sn_inmobiliaria`), desde `schema/` y `seed/` **anteriores** a la migración (los del último commit, con `git show HEAD:database/...`, copiados al contenedor con `docker cp`, porque `/database` montado ya tiene los nuevos), para ensayar:

- migrar, repetir, revertir y volver a migrar;
- comparar con una base creada desde los `schema/` y `seed/` nuevos (`pg_dump --schema-only`).

Para `mvn package`, apuntar `DB_URL` a esa base. No tocar `sn_inmobiliaria` sin respaldo. Borrar las bases de prueba al terminar.

### 10.3 Verificación visual del sitio

1. `node herramientas/simulador.mjs` (variable `CONTENIDO` con la ruta de `contenido_largo.json` para el contenido largo).
2. `cd frontend; npx vite --port 5173`.
3. `py herramientas/capturar.py <carpeta>` (Playwright ya está instalado; `capturas/` en la raíz está ignorada por Git).

Captura 7 páginas y el chat en 1366 px y 390 px, y escribe `informe.json` con los textos fuera de su recuadro o recortados, los desbordes horizontales y los emojis. **No cubre estados** (errores, avisos, pasos del chat) ni la **alineación**: para eso, escribir un script aparte que reutilice su revisión y medir con `getBoundingClientRect`. Además, **mirar las capturas**: la desalineación de la barra en la W4 solo se vio así.

## 11. Cómo trabajamos (la dinámica, obligatoria)

1. **Revisar el estado real** antes de proponer: el código actual, la estructura real de la base (con scripts de solo lectura) y lo que ya existe para reutilizar. Revisar también los cambios sin commitear (`git status`).
2. **Presentar el alcance**: qué cambia, en qué archivos y por qué. Plantear **decisiones con una recomendación**, en tablas. **Esperar la aprobación.**
3. **Implementar** solo lo aprobado, sin tocar archivos de más.
4. **Verificar**: compilación (`mvn package`, `tsc`, `build`), pruebas (agregar pruebas nuevas en cada entrega), migraciones ensayadas en una copia y verificación visual en el sitio.
5. **Entregar**: los cambios quedan **sin commitear** en la copia local, con una **tabla de revisión** ("qué hacer / qué esperar") y los comandos de PowerShell (respaldo, migración, `mvn package` **antes** de `docker compose up --build`, porque Docker compila sin pruebas).
6. **El equipo revisa y hace el commit.** **Nunca hacer commit ni push** y nunca avanzar a la etapa siguiente sin aprobación.

### Preferencias del equipo

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

- Implementada el 19/10 (sección 9.1), incluida la prueba con Gemini real.
- Posibles ampliaciones: recorridos de punta a punta como gerente y vendedor, y subida de fotos en el navegador real (hoy cubiertas por las pruebas de interfaz, las de integración y los recorridos manuales).

### Fase 18 (documentación)

- Documentación técnica en `docs/`: arquitectura, API, roles, despliegue y manual del panel.

### Contenido que se cargará desde el panel (no requiere programar)

- **Precios reales en guaraníes** (sección 9, tarea 4).
- Datos de contacto reales (hoy provisionales), imagen de portada propia, ofertas destacadas, responsables y cifras de Nosotros.
- Revisión legal del texto de aceptación (versión `2026-10`) y de la política de privacidad.

### Postergado o para el final

- **Recuperación de contraseña por correo** ("Olvidé mi contraseña"): postergada por completo el 19/10 (no hay nada en el código). Requiere elegir un servicio de correo (Gmail con contraseña de aplicación mientras no haya dominio propio; Brevo cuando lo haya), enlace de un solo uso de 30 minutos con `#token=`, misma respuesta exista o no el email.
- Vista previa del contenido antes de publicar.
- Tema del panel guardado por cuenta (hoy por navegador).
- Límites de envío en nginx (`limit_req`).
- HTTPS y dominio; Tomcat como usuario no-root.
- Borrar las imágenes viejas del volumen.
- `frontend/public/data/*.geojson`: no los usa el sitio, pero sí el simulador y una prueba opcional del backend; desde la Fase 17 tienen la moneda en PYG (sección 9.1). Las imágenes `inicio_sobre_nosotros.jpg` y `nosotros_page2.jpg` no se usan.
- **Descartados:** tipo de cambio y conversión de monedas, estadísticas de visitas, mensajes recibidos y redes sociales.

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
| Comandos de la tabla de revisión que dependían de variables de otro paso: sin `$fecha`/`$resp` no se guardó la verificación ni se hizo el respaldo | Cada bloque de comandos para el equipo debe ser **independiente** (definir sus variables) y detenerse si falla el respaldo |
| Un cambio de CSS en una clase compartida (`.searchBtn`) movía otro botón | Antes de cambiar una clase, buscar todos sus usos (`grep`) y limitar el cambio con un selector más específico |
| La revisión automática dio 0 problemas pero había una desalineación | Mirar las capturas además del informe |
| Con el simulador, la prueba de "solo barrios con propiedades" no distinguía nada (sus 12 barrios tienen propiedades) | Comprobar también con los datos reales, en solo lectura (Vite con `VITE_BACKEND_URL=http://localhost:80`), y probar que la prueba falla sin el cambio |
| Al hacer clic en el centro de un barrio, la prueba cayó en un marcador y abrió una propiedad | Para hacer clic en una zona del mapa, buscar un punto con `document.elementFromPoint` que sea de ese polígono |
| En el celular, el barrio se encuadraba más abajo de lo pedido: Leaflet seguía con el alto anterior del mapa (la barra había crecido) | Llamar a `map.invalidateSize()` antes de encuadrar, y medir los elementos que se dibujan sobre el mapa en lugar de usar márgenes fijos |
| El inicio de sesión con `ADMIN_PASSWORD` de `docker/.env` dio 401 en la base de desarrollo | Esa contraseña solo crea el primer administrador: no reintentar (límite de intentos); para pruebas, usar una base de prueba con su propio administrador |
| Una prueba opcional (`VERIFICAR_DATOS_DEMO`) falló al activarla | Antes de atribuir una falla a un cambio, comprobar si la prueba ya fallaba (aquí: GeoJSON en USD desde la 009) |
| `python` mezclado con `node` en un solo comando quedó colgado | Usar `py` con `timeout` y no encadenar intérpretes |
| En Git Bash, `psql -f /database/...` dentro de `docker compose exec` buscó `C:/Program Files/Git/database/...` | Anteponer `MSYS_NO_PATHCONV=1` o usar PowerShell |
| `CREATE DATABASE … TEMPLATE sn_prueba` falló: el backend de pruebas estaba conectado | Crear la base vacía y cargar `schema/` y `seed/` con `psql` |
| Un booleano concatenado a texto en psql da `true`, no `t` | Comparar con `true` (o pedir la columna sola) |
| Gemini real dio HTTP 503 dos veces seguidas | Es saturación de Google: la prueba se omite con el motivo. No insistir (cada intento puede consumir cuota); reintentar más tarde |
| Un comando de PowerShell corrió en otra carpeta (la anterior había cambiado de directorio) y no ejecutó nada | Empezar con `Set-Location C:\Proyectos\sn-inmobiliaria` o usar rutas absolutas |
| Un heredoc de Bash con comillas simples y dobles mezcladas no cerró | Escribir el script en un archivo y ejecutarlo |
| Al copiar títulos del panel a los datos de demostración, dos quedaban contradictorios ("Alquiler en …" en propiedades de Venta en la demo) y otros dos repetidos | Comparar operación y unicidad antes de copiar datos entre la base real y la demo |
| Se reinició el backend real en la misma ventana donde había definido `$env:DB_PASSWORD` y `$env:GEMINI_MODEL` para la prueba: Compose usa las variables de la ventana antes que `docker/.env`, y el backend arrancó con la contraseña de la base de prueba (unhealthy, autenticación rechazada; sin daño a los datos). El error fue de la instrucción, que no lo advertía | Nunca indicar `docker compose -f docker/docker-compose.yml ...` en la misma ventana que los comandos de prueba: pedir una ventana nueva. Para diagnosticar, comparar con `docker inspect` el origen de las variables sin mostrarlas |

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

# Entorno de pruebas: iniciar, y detener borrando solo lo suyo
docker compose -f docker/docker-compose.pruebas.yml --env-file docker/.env.pruebas up -d --build
docker compose -f docker/docker-compose.pruebas.yml --env-file docker/.env.pruebas down -v

# Pruebas del backend contra el entorno de pruebas (nunca contra sn_inmobiliaria)
function Leer-Env($n) { (Select-String -Path docker\.env.pruebas -Pattern "^$n=").Line.Split('=', 2)[1].Trim() }
$env:DB_URL = "jdbc:postgresql://localhost:5434/sn_prueba"; $env:DB_USER = "sn_app"; $env:DB_PASSWORD = Leer-Env "PRUEBA_DB_PASSWORD"
$env:ADMIN_EMAIL = Leer-Env "PRUEBA_ADMIN_EMAIL"; $env:ADMIN_PASSWORD = Leer-Env "PRUEBA_ADMIN_PASSWORD"; $env:VERIFICAR_DATOS_DEMO = "true"
mvn -f backend/pom.xml clean package | Select-String "Tests run: [0-9]+, F.*Skipped: [0-9]+$|BUILD"

# Recorridos de punta a punta (con el entorno de pruebas iniciado)
py herramientas/recorridos_e2e.py

# Sitio y panel
cd frontend; npx tsc --noEmit; npm run build; npm test; cd ..
cd admin; npx tsc --noEmit; npm run build; npm test; cd ..

# Sistema completo (después de que pasen las pruebas)
docker compose -f docker/docker-compose.yml up -d --build backend web
```

El `README.md` de la raíz tiene la versión para personas (instalación, variables y pruebas). URLs: sitio `http://localhost/` y panel `http://localhost/admin/`. En desarrollo: sitio en `5173` y panel en `5174/admin/`.
