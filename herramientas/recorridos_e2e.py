"""Recorridos de punta a punta (Fase 17) con un navegador real (Playwright + Chromium).

Se ejecutan contra el ENTORNO DE PRUEBAS (docker/docker-compose.pruebas.yml), nunca contra el
sistema real: el script se niega a usar http://localhost (puerto 80).

  1. Búsqueda: elegir Ciudad del Este y Buscar → un marcador por cada propiedad de la API.
  2. Detalle: abrir una propiedad desde su marcador → título y precio en Gs.; la X vuelve al mapa.
  3. Chat: Comprar → ciudad → presupuesto escrito → nombre y teléfono → "Dejar mis datos" →
     queda un cliente potencial con origen CHAT y la aceptación registrada.
  4. Panel: iniciar sesión con el administrador de prueba → pantalla de Inicio con su nombre.
  5. Alta de propiedad: completar el formulario, marcar en el mapa y crear → la propiedad
     aparece en la API pública y en el sitio con su precio.

Limpieza: al terminar (pase o falle) borra SOLO lo que creó, identificado por una marca única
("E2E <fecha-hora>"): la propiedad nueva (con sus operaciones, en cascada) y el cliente potencial.

Uso (desde la raíz del repositorio, con el entorno de pruebas iniciado; ver docs/pruebas.md):
  py herramientas/recorridos_e2e.py
Variables opcionales: E2E_URL (por defecto http://localhost:<PRUEBA_WEB_PORT>), E2E_VISIBLE=1 (ver el navegador).
Las credenciales del administrador de prueba se leen de docker/.env.pruebas.
"""
import json, os, pathlib, re, subprocess, sys, time, urllib.request
from playwright.sync_api import sync_playwright, expect

RAIZ = pathlib.Path(__file__).resolve().parent.parent
ENV_PRUEBAS = RAIZ / "docker" / ".env.pruebas"
COMPOSE = ["docker", "compose", "-f", str(RAIZ / "docker" / "docker-compose.pruebas.yml"), "--env-file", str(ENV_PRUEBAS)]


def leer_env():
    if not ENV_PRUEBAS.exists():
        sys.exit(f"Falta {ENV_PRUEBAS}: copiar docker/pruebas.env.example (ver docs/pruebas.md)")
    return dict(l.split("=", 1) for l in ENV_PRUEBAS.read_text(encoding="utf-8").splitlines() if "=" in l and not l.startswith("#"))


ENV = leer_env()
URL = os.environ.get("E2E_URL", f"http://localhost:{ENV.get('PRUEBA_WEB_PORT', '8081')}").rstrip("/")
if re.fullmatch(r"https?://(localhost|127\.0\.0\.1)(:80)?", URL):
    sys.exit(f"{URL} es el sistema real: los recorridos solo se ejecutan contra el entorno de pruebas.")

MARCA = "E2E " + time.strftime("%Y%m%d-%H%M%S")
EMAIL_CHAT = "e2e-" + time.strftime("%Y%m%d%H%M%S") + "@sn.test"
TITULO = f"{MARCA} Vivienda de prueba"
PRECIO = 350000000
creado = {"codigo": None}


def api(ruta):
    with urllib.request.urlopen(URL + ruta, timeout=20) as r:
        return json.load(r)


def sql(consulta, **variables):
    """Ejecuta SQL en la base del ENTORNO DE PRUEBAS (contenedor sn-prueba-db)."""
    args = [*COMPOSE, "exec", "-T", "db", "psql", "-U", "sn_app", "-d", "sn_prueba", "-At", "-v", "ON_ERROR_STOP=1"]
    for k, v in variables.items():
        args += ["-v", f"{k}={v}"]
    r = subprocess.run(args, input=consulta, capture_output=True, text=True, encoding="utf-8", timeout=60)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.strip())
    return r.stdout.strip()


def selector_de(pg, etiqueta):
    return pg.locator(f"xpath=//label[normalize-space()='{etiqueta}']/following-sibling::select")


def r1_busqueda(pg):
    ciudad = next(c["properties"] for c in api("/api/ciudades")["features"] if c["properties"]["ciudad"] == "CIUDAD DEL ESTE")
    esperadas = len(api(f"/api/propiedades?ciudadId={ciudad['id']}")["features"])
    assert esperadas > 0, "la API no tiene propiedades en Ciudad del Este"
    pg.goto(URL + "/buscar", wait_until="networkidle")
    selector_de(pg, "Ciudad").select_option(label="CIUDAD DEL ESTE")
    pg.get_by_role("button", name="Buscar", exact=True).click()
    expect(pg.locator(".pm-propiedad-icon-wrapper")).to_have_count(esperadas, timeout=15000)
    return f"{esperadas} marcadores, igual que la API"


def r2_detalle(pg):
    marcadores = pg.locator(".pm-propiedad-icon-wrapper")
    antes = marcadores.count()
    marcadores.first.click(force=True)
    titulo = pg.locator("h3").filter(has_text=re.compile(r"\S")).first
    expect(titulo).to_be_visible(timeout=10000)
    expect(pg.get_by_text(re.compile(r"Gs\. [\d.]+")).first).to_be_visible()
    nombre = titulo.inner_text()
    x = pg.locator('button[aria-label="Volver a los resultados"]')  # la X (también hay un enlace con ese texto)
    x.click()
    expect(x).to_have_count(0)
    expect(marcadores).to_have_count(antes)
    return f"abrió \"{nombre}\" y la X volvió al mapa con {antes} marcadores"


def r3_chat(pg):
    pg.goto(URL + "/", wait_until="networkidle")
    pg.get_by_role("button", name="Abrir asistente").click()
    pg.get_by_role("button", name="Comprar una propiedad").click()
    pg.get_by_role("button", name="Ciudad del Este").click()
    presupuesto = pg.get_by_label("Tu presupuesto")
    presupuesto.fill("entre 300 y 500 millones"); presupuesto.press("Enter")
    pg.get_by_placeholder("Tu nombre").fill(f"{MARCA} Cliente")
    pg.get_by_placeholder("Tu teléfono").fill("0981 000 000")
    pg.get_by_role("button", name="Continuar").click()
    pg.get_by_role("button", name="Dejar mis datos para que me contacten").click()
    formulario = pg.get_by_role("form", name="Dejar mis datos")
    formulario.get_by_label("Email").fill(EMAIL_CHAT)
    formulario.get_by_role("checkbox").check()
    formulario.get_by_role("button", name="Enviar mis datos").click()
    expect(pg.get_by_text(re.compile("Gracias|asesor", re.I)).last).to_be_visible(timeout=15000)
    fila = sql("SELECT origen || '|' || (consentimiento_en IS NOT NULL) || '|' || coalesce(interes, '') FROM cliente_potencial WHERE email = :'e'", e=EMAIL_CHAT)
    assert fila, "no se registró el cliente potencial"
    origen, acepto, interes = fila.split("|", 2)
    assert origen == "CHAT" and acepto == "true", fila
    assert "entre 300 y 500 millones" in interes, "el presupuesto escrito no quedó en el resumen"
    return "cliente potencial CHAT con aceptación y presupuesto \"entre 300 y 500 millones\""


def r4_login(pg):
    pg.goto(URL + "/admin/", wait_until="networkidle")
    pg.get_by_label("Email").fill(ENV["PRUEBA_ADMIN_EMAIL"])
    pg.get_by_label(re.compile("^Contraseña")).first.fill(ENV["PRUEBA_ADMIN_PASSWORD"])
    pg.get_by_role("button", name="Ingresar").click()
    saludo = pg.locator("h1").first
    expect(saludo).to_have_text(re.compile(r"^Buen(os|as) (días|tardes|noches), \S+"), timeout=15000)
    return f"Inicio: \"{saludo.inner_text()}\""


def r5_alta(pg):
    pg.goto(URL + "/admin/propiedades/nueva", wait_until="networkidle")
    pg.get_by_text("Datos de la vivienda").wait_for()
    pg.get_by_label("Título", exact=True).fill(TITULO)
    pg.get_by_label("Habitaciones").fill("2"); pg.get_by_label("Baños").fill("1")
    pg.get_by_label("Superficie construida (m²)").fill("90")
    pg.get_by_label("Precio 1").fill(str(PRECIO))
    mapa = pg.locator(".leaflet-container").first; mapa.scroll_into_view_if_needed(); caja = mapa.bounding_box()
    mapa.click(position={"x": caja["width"] / 2, "y": caja["height"] / 2})
    expect(pg.get_by_text("CIUDAD DEL ESTE").first).to_be_visible(timeout=10000)
    pg.get_by_role("button", name="Crear propiedad").click()
    aviso = pg.get_by_text(re.compile(r"Propiedad prop-\d+ creada"))
    expect(aviso).to_be_visible(timeout=15000)
    creado["codigo"] = re.search(r"prop-\d+", aviso.inner_text()).group(0)
    publica = api(f"/api/propiedades/{creado['codigo']}")["properties"]
    assert publica["titulo"] == TITULO and int(publica["precio"]) == PRECIO, publica
    pg.goto(URL + f"/buscar?propiedad={creado['codigo']}", wait_until="networkidle")
    expect(pg.locator("h3", has_text=TITULO)).to_be_visible(timeout=10000)
    expect(pg.get_by_text("Gs. 350.000.000").first).to_be_visible()
    return f"{creado['codigo']} creada; visible en la API y en el sitio con Gs. 350.000.000"


def limpiar():
    hechos = []
    if creado["codigo"]:
        n = sql("WITH b AS (DELETE FROM propiedad WHERE codigo = :'c' AND titulo = :'t' RETURNING 1) SELECT count(*) FROM b",
                c=creado["codigo"], t=TITULO)
        hechos.append(f"propiedad {creado['codigo']}: {n}")
    n = sql("WITH b AS (DELETE FROM cliente_potencial WHERE email = :'e' RETURNING 1) SELECT count(*) FROM b", e=EMAIL_CHAT)
    hechos.append(f"cliente potencial: {n}")
    return ", ".join(hechos)


def main():
    salud = api("/api/health")
    print(f"Entorno: {URL} · backend {salud.get('version')} · marca \"{MARCA}\"")
    resultados = []
    with sync_playwright() as pw:
        navegador = pw.chromium.launch(headless=os.environ.get("E2E_VISIBLE") != "1")
        try:
            for ancho, alto in [(1366, 900), (390, 844)]:
                pg = navegador.new_page(viewport={"width": ancho, "height": alto})
                pasos = [("Búsqueda", r1_busqueda), ("Detalle", r2_detalle)]
                if ancho == 1366:
                    pasos += [("Chat", r3_chat), ("Panel: inicio de sesión", r4_login), ("Panel: alta de propiedad", r5_alta)]
                for nombre, paso in pasos:
                    try:
                        resultados.append((f"{nombre} ({ancho} px)", True, paso(pg)))
                    except Exception as e:  # se informa y se sigue con el siguiente recorrido
                        resultados.append((f"{nombre} ({ancho} px)", False, str(e).splitlines()[0][:200]))
                pg.close()
        finally:
            navegador.close()
            try:
                print("Limpieza (filas borradas):", limpiar())
            except Exception as e:
                print("LIMPIEZA FALLIDA:", e)
                resultados.append(("Limpieza", False, str(e)[:200]))
    for nombre, ok, detalle in resultados:
        print(f"{'OK   ' if ok else 'FALLA'} {nombre:32} {detalle}")
    fallas = sum(1 for _, ok, _ in resultados if not ok)
    print(f"{len(resultados) - fallas} de {len(resultados)} recorridos correctos")
    sys.exit(1 if fallas else 0)


if __name__ == "__main__":
    main()
