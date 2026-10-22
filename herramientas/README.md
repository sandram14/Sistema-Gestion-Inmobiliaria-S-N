# Herramientas de verificación

Scripts de apoyo para las pruebas. La guía completa, con los resultados esperados, está en
[`docs/pruebas.md`](../docs/pruebas.md).

| Archivo | Para qué |
|---|---|
| `recorridos_e2e.py` | Recorridos de punta a punta con un navegador real (Playwright) contra el entorno de pruebas de Docker: búsqueda, detalle, chat, inicio de sesión en el panel y alta de una propiedad. Limpia lo que crea |
| `simulador.mjs` | Backend simulado (sin base de datos), en el puerto 8080 o el de `PORT`: sirve las 28 propiedades de demostración (`frontend/public/data/*.geojson`), sus fotos y el contenido del sitio |
| `capturar.py` | Revisión visual automática del sitio con Playwright |
| `contenido_normal.json` | Contenido del sitio de ejemplo (datos ficticios) para el simulador |
| `contenido_largo.json` | El mismo contenido con cada texto en su largo máximo, para ver que nada se salga de su recuadro |
| `contenido_prov.json` | Contenido con datos de contacto provisionales, para ver el aviso correspondiente |
| `fuentes/` | `package.json` con la fuente Roboto (`@fontsource/roboto`), que `capturar.py` sirve en lugar de Google Fonts. Su `node_modules/` no se sube a Git |

## Revisión visual del sitio

Requisitos: Node.js 22 y Python con Playwright (`py -m pip install playwright` y
`py -m playwright install chromium`).

```powershell
cd herramientas\fuentes; npm ci; cd ..\..                 # una sola vez: Roboto local
node herramientas/simulador.mjs                            # terminal 1 (backend simulado en 8080)
cd frontend; npx vite --port 5173                          # terminal 2 (sitio; el proxy de Vite usa el 8080)
New-Item -ItemType Directory -Force capturas | Out-Null; py herramientas/capturar.py capturas   # terminal 3
```

`capturar.py` recorre 7 páginas y el chat en 1366 y 390 px. Guarda las capturas en la carpeta
indicada y escribe `informe.json` con los textos fuera de su recuadro o recortados, los desbordes
horizontales y los emojis. Resultado esperado: `problemas=0`, `desborde=False` y `emojis=0` en todas
las líneas.

| Variable | Uso |
|---|---|
| `CONTENIDO` (simulador) | Ruta de otro JSON de contenido, por ejemplo `herramientas\contenido_largo.json` |
| `SITIO` (capturar.py) | Dirección del sitio, por defecto `http://localhost:5173` |
| `FUENTES` (capturar.py) | Otra carpeta de `@fontsource`, si las fuentes no están en `herramientas/fuentes` |

El informe no cubre los estados intermedios (errores, avisos, pasos del chat) ni la alineación
entre elementos: conviene mirar también las capturas. La carpeta de capturas no se sube a Git.
