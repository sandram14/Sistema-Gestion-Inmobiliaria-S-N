// Backend simulado para ver el sitio: responde como la API Java con los datos de ejemplo.
import http from 'node:http'; import fs from 'node:fs'; import path from 'node:path'; import { fileURLToPath } from 'node:url'
const AQUI = path.dirname(fileURLToPath(import.meta.url)) // carpeta herramientas/ del proyecto
const F = path.resolve(AQUI, '../frontend/public/data'), IMG = path.resolve(AQUI, '../database/seed/imagenes')
const leer = (f) => JSON.parse(fs.readFileSync(`${F}/${f}.geojson`, 'utf8'))
const mapeo = fs.readFileSync(`${IMG}/MAPEO.csv`, 'utf8').trim().split('\n').slice(1).map((l) => l.split(';'))
const fotos = {}; for (const [c, o, , , u] of mapeo) (fotos[c] ??= [])[Number(o)] = u
const ciudades = leer('ciudades'); ciudades.features.forEach((f) => { f.properties.departamentoId = 1; f.properties.departamento = 'ALTO PARANÁ' })
const barrios = leer('barrios'); const props = leer('propiedades')
props.features.forEach((f) => { const p = f.properties; p.fotos = (fotos[p.id] ?? []).filter(Boolean)
  p.moneda = 'PYG' // solo guaraníes (W4): mismo número, como la migración 009
  p.operaciones = [{ operacion: p.tipoOperacion, precio: p.precio, moneda: p.moneda }]
  p.descripcion = 'Propiedad de ejemplo con buena ubicación y fácil acceso.' })
const contenido = () => JSON.parse(fs.readFileSync(process.env.CONTENIDO || path.resolve(AQUI, 'contenido_normal.json'), 'utf8'))
const json = (r, o, s = 200) => { r.writeHead(s, { 'Content-Type': 'application/json; charset=utf-8' }); r.end(JSON.stringify(o)) }
http.createServer((q, r) => {
  const u = new URL(q.url, 'http://x'), p = u.pathname
  if (p === '/api/ciudades') return json(r, ciudades)
  if (p === '/api/barrios') return json(r, barrios)
  if (p === '/api/sitio') return json(r, contenido())
  if (p.startsWith('/api/propiedades/')) { const f = props.features.find((x) => x.properties.id === p.split('/')[3]); return f ? json(r, f) : json(r, { error: { codigo: 'NO_ENCONTRADO', mensaje: 'No existe' } }, 404) }
  if (p === '/api/propiedades') {
    const s = u.searchParams, ciu = ciudades.features.find((c) => c.properties.id === s.get('ciudadId'))
    const l = props.features.filter((f) => { const x = f.properties
      if (ciu && x.ciudad !== ciu.properties.ciudad) return false
      if (s.get('barrioId') && String(x.barrioId) !== s.get('barrioId')) return false
      if (s.get('operacion') && x.tipoOperacion !== s.get('operacion')) return false
      if (s.get('categoria') && !(s.get('categoria') === 'Vivienda' ? x.categoria !== 'Terreno' : x.categoria === s.get('categoria'))) return false
      if (s.get('presupuestoMax') && x.precio > Number(s.get('presupuestoMax'))) return false
      if (s.get('presupuestoMin') && x.precio < Number(s.get('presupuestoMin'))) return false
      return true })
    return json(r, { type: 'FeatureCollection', features: l })
  }
  if (p === '/api/asistente/consultas' && q.method === 'POST') return json(r, { respuesta: 'Tenemos una casa en Area 1, Ciudad del Este. Venta: Gs. 45.000. 3 habitaciones y 2 baños.', sugerirWhatsApp: true, propiedadesRelacionadas: ['prop-001'] })
  if (p === '/api/clientes-potenciales' && q.method === 'POST') return json(r, { registrado: true, mensaje: 'Gracias. Un asesor se pondrá en contacto con usted.' }, 201)
  if (p.startsWith('/uploads/')) { const a = path.join(IMG, p.replace('/uploads/', '')); if (fs.existsSync(a)) { r.writeHead(200, { 'Content-Type': 'image/png' }); return fs.createReadStream(a).pipe(r) } }
  json(r, { error: { codigo: 'NO_ENCONTRADO', mensaje: 'No existe' } }, 404)
}).listen(Number(process.env.PORT || 8080), () => console.log('simulador en ' + (process.env.PORT || 8080)))
