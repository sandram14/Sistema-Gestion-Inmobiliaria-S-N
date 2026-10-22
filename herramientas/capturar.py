import os, re, sys, json, pathlib
from playwright.sync_api import sync_playwright
destino = sys.argv[1]
SITIO = os.environ.get('SITIO', 'http://localhost:5173')
PAGINAS = [("inicio", "/"), ("buscar", "/buscar"), ("barrio", "/buscar?ciudad=01&barrio=10011013"), ("detalle", "/buscar?propiedad=prop-001"),
           ("servicios", "/servicios"), ("nosotros", "/nosotros"), ("contacto", "/contacto")]
REVISION = """() => {
  const fondo = (el) => { const s = getComputedStyle(el); return (s.backgroundColor !== 'rgba(0, 0, 0, 0)' && s.backgroundColor !== 'transparent') || parseFloat(s.borderTopWidth) > 0 || s.backgroundImage !== 'none' }
  const problemas = []
  for (const el of document.querySelectorAll('body *')) {
    const s = getComputedStyle(el); if (s.display === 'none' || s.visibility === 'hidden') continue
    const texto = [...el.childNodes].filter(n => n.nodeType === 3).map(n => n.textContent.trim()).join(' ').trim()
    if (!texto) continue
    const r = el.getBoundingClientRect(); if (r.width === 0) continue
    // Dónde se dibuja realmente el texto (no la caja del elemento)
    const rango = document.createRange(); rango.selectNodeContents(el); const t = rango.getBoundingClientRect()
    if (t.width === 0) continue
    // Recuadro que lo contiene: el propio elemento o el ancestro más cercano con fondo o borde
    let p = fondo(el) ? el : el.parentElement, desplazable = false
    while (p && p !== document.body && !fondo(p)) { if (!['visible', 'clip'].includes(getComputedStyle(p).overflow)) desplazable = true; p = p.parentElement }
    if (desplazable || !p || p === document.body) continue   // menú plegado o panel con desplazamiento
    const q = p.getBoundingClientRect(), ov = getComputedStyle(p).overflow
    const sale = t.left < q.left - 1 || t.right > q.right + 1 || t.top < q.top - 1 || t.bottom > q.bottom + 1
    if (!sale) continue
    if (ov === 'visible') problemas.push(['texto fuera de su recuadro', texto.slice(0, 50), Math.round(t.width) + 'px en ' + Math.round(q.width) + 'px'])
    else if (t.top < q.bottom && t.left < q.right) problemas.push(['texto recortado por su recuadro', texto.slice(0, 50)])
  }
  const emojis = (document.body.innerText.match(/[\\u{1F300}-\\u{1FAFF}\\u{2600}-\\u{27BF}]/gu) || []).length
  return { problemas: problemas.slice(0, 15), total: problemas.length, desbordeHorizontal: document.documentElement.scrollWidth > window.innerWidth + 1, emojis }
}"""
FS = os.environ.get("FUENTES", str(pathlib.Path(__file__).resolve().parent / "fuentes/node_modules/@fontsource"))
def css_fuentes():
    partes = []
    # La misma fuente que carga frontend/index.html y admin/src/global.css (Roboto; títulos en 700)
    for fam, pesos in [("roboto", ["300", "400", "500", "600", "700"])]:
        for w in pesos:
            css = open(f"{FS}/{fam}/{w}.css").read()
            partes.append(re.sub(r"url\(\./files/", f"url(https://fuentes.local/{fam}/", css))
    return "\n".join(partes)
CSS_FUENTES = css_fuentes()
def interceptar(pagina):
    pagina.route("https://fonts.googleapis.com/**", lambda r: r.fulfill(status=200, content_type="text/css", body=CSS_FUENTES))
    pagina.route("https://fuentes.local/**", lambda r: r.fulfill(status=200, content_type="font/woff2",
        body=open(f"{FS}/{r.request.url.split('fuentes.local/')[1].split('/')[0]}/files/{r.request.url.rsplit('/', 1)[1]}", "rb").read()))
informe = {}
with sync_playwright() as pw:
    b = pw.chromium.launch()
    for ancho, nombre_ancho in [(1366, "pc"), (390, "movil")]:
        pg = b.new_page(viewport={"width": ancho, "height": 900 if ancho > 500 else 844}); interceptar(pg)
        for nombre, ruta in PAGINAS:
            pg.goto(SITIO + ruta, wait_until="networkidle"); pg.wait_for_timeout(700)
            pg.screenshot(path=f"{destino}/{nombre}-{nombre_ancho}.png", full_page=True)
            informe[f"{nombre}-{nombre_ancho}"] = pg.evaluate(REVISION)
        pg.goto(SITIO + "/nosotros", wait_until="networkidle"); pg.wait_for_timeout(400)
        try:
            pg.get_by_label("Abrir asistente").click(); pg.wait_for_timeout(600)
            pg.screenshot(path=f"{destino}/chat-{nombre_ancho}.png"); informe[f"chat-{nombre_ancho}"] = pg.evaluate(REVISION)
        except Exception as e: informe[f"chat-{nombre_ancho}"] = str(e)[:80]
        pg.close()
    b.close()
json.dump(informe, open(f"{destino}/informe.json", "w"), ensure_ascii=False, indent=1)
for k, v in informe.items():
    if isinstance(v, dict): print(f"{k:16} problemas={v['total']:3} desborde={v['desbordeHorizontal']!s:5} emojis={v['emojis']}")
    else: print(k, v)
