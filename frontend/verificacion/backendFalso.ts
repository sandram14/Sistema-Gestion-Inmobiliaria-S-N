// Backend simulado: responde como la API Java y guarda las URLs pedidas.
type Json = unknown
export const pedidos: { metodo: string; url: string; cuerpo?: unknown }[] = []
export const control = { caido: false, respuestaAsistente: null as null | { status: number; json: Json },
  sitio: null as null | Json, respuestaCliente: null as null | { status: number; json: Json },
  /** Errores por campo que devuelve /api/propiedades (400 VALIDACION), por ejemplo { presupuestoMax: '...' } */
  erroresBusqueda: null as null | Record<string, string> }
const ciudades = { type: 'FeatureCollection', features: [
  { type: 'Feature', geometry: { type: 'Point', coordinates: [-54.64, -25.51] }, properties: { id: '1', ciudad: 'CIUDAD DEL ESTE', departamento: 'ALTO PARANÁ', totalBarrios: 1, color: '#2E5438', departamentoId: 1 } },
  { type: 'Feature', geometry: { type: 'Point', coordinates: [-54.75, -25.40] }, properties: { id: '3', ciudad: 'HERNANDARIAS', departamento: 'ALTO PARANÁ', totalBarrios: 1, color: '#345A3E', departamentoId: 1 } },
]}
const cuadrado = (x: number, y: number) => ({ type: 'MultiPolygon', coordinates: [[[[x, y], [x + 0.01, y], [x + 0.01, y + 0.01], [x, y + 0.01], [x, y]]]] })
const barrios = { type: 'FeatureCollection', features: [
  { type: 'Feature', geometry: cuadrado(-54.62, -25.52), properties: { id: 10011013, barrio: 'AREA 1', distritoCod: '1', ciudad: 'CIUDAD DEL ESTE', departamento: 'ALTO PARANÁ', color: '#3A6B46' } },
  { type: 'Feature', geometry: cuadrado(-54.76, -25.41), properties: { id: 10051007, barrio: 'CENTRO', distritoCod: '3', ciudad: 'HERNANDARIAS', departamento: 'ALTO PARANÁ', color: '#4A7A56' } },
]}
const props = [
  { id: 'prop-001', titulo: 'Casa en Area 1', categoria: 'Casa', tipoOperacion: 'Venta', ciudad: 'CIUDAD DEL ESTE', barrio: 'AREA 1', barrioId: 10011013,
    precio: 450000000, moneda: 'PYG', dormitorios: 3, banos: 2, m2: 182, direccion: 'Calle 1', fotos: [], descripcion: 'Casa luminosa con patio.',
    operaciones: [{ operacion: 'Venta', precio: 450000000, moneda: 'PYG' }, { operacion: 'Alquiler', precio: 4000000, moneda: 'PYG' }] },
  { id: 'terr-001', titulo: 'Terreno en Centro', categoria: 'Terreno', tipoOperacion: 'Venta', ciudad: 'HERNANDARIAS', barrio: 'CENTRO', barrioId: 10051007,
    precio: 280000000, moneda: 'PYG', dormitorios: 0, banos: 0, m2: 750, direccion: '', fotos: [], descripcion: null,
    operaciones: [{ operacion: 'Venta', precio: 280000000, moneda: 'PYG' }] },
]
const coords: Record<string, [number, number]> = { 'prop-001': [-54.615, -25.515], 'terr-001': [-54.755, -25.405] }
const fc = (lista: typeof props) => ({ type: 'FeatureCollection', features: lista.map((p) => ({ type: 'Feature', geometry: { type: 'Point', coordinates: coords[p.id] }, properties: p })) })
const ciudadDe: Record<string, string> = { 'CIUDAD DEL ESTE': '1', HERNANDARIAS: '3' }
function filtrar(q: URLSearchParams) {
  return props.filter((p) => {
    if (q.get('ciudadId') && ciudadDe[p.ciudad] !== q.get('ciudadId')) return false
    if (q.get('barrioId') && String(p.barrioId) !== q.get('barrioId')) return false
    const cat = q.get('categoria')
    if (cat && !((cat === 'Vivienda' || cat === 'Casa') ? p.categoria !== 'Terreno' : p.categoria === cat)) return false
    // Como el backend 0.21.0: solo guaraníes, "moneda" es opcional y el rango se compara contra cualquier precio
    const op = q.get('operacion'); const min = q.get('presupuestoMin'); const max = q.get('presupuestoMax')
    if (op || min || max) return p.operaciones.some((o) => (!op || o.operacion === op)
      && (!min || o.precio >= Number(min)) && (!max || o.precio <= Number(max)))
    return true
  })
}
const res = (status: number, json: Json) => new Response(JSON.stringify(json), { status, headers: { 'Content-Type': 'application/json' } })
export async function fetchFalso(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const url = String(input); const metodo = init?.method ?? 'GET'
  pedidos.push({ metodo, url, cuerpo: init?.body ? JSON.parse(String(init.body)) : undefined })
  if (control.caido) throw new TypeError('Failed to fetch')
  const u = new URL(url, 'http://localhost')
  if (u.pathname === '/api/ciudades') return res(200, ciudades)
  if (u.pathname === '/api/barrios') return res(200, barrios)
  if (u.pathname === '/api/propiedades') {
    const moneda = u.searchParams.get('moneda')
    const conPresupuesto = u.searchParams.has('presupuestoMin') || u.searchParams.has('presupuestoMax')
    const campos = (conPresupuesto ? control.erroresBusqueda : null)
      ?? (moneda && moneda !== 'PYG' ? { moneda: 'Solo se aceptan guaraníes (PYG)' } : null)
    if (campos) return res(400, { error: { codigo: 'VALIDACION', mensaje: 'Datos no válidos', campos } })
    return res(200, fc(filtrar(u.searchParams)))
  }
  if (u.pathname === '/api/sitio') return control.sitio ? res(200, control.sitio) : res(503, { error: { codigo: 'SERVICIO_NO_DISPONIBLE', mensaje: 'Sin base' } })
  if (u.pathname === '/api/clientes-potenciales' && metodo === 'POST') {
    const r = control.respuestaCliente ?? { status: 201, json: { registrado: true, mensaje: 'Gracias. Un asesor se pondrá en contacto con usted.' } }
    return res(r.status, r.json)
  }
  if (u.pathname === '/api/asistente/consultas' && metodo === 'POST') {
    const r = control.respuestaAsistente ?? { status: 200, json: { respuesta: 'Respuesta', sugerirWhatsApp: false, propiedadesRelacionadas: [] } }
    return res(r.status, r.json)
  }
  return res(404, { error: { codigo: 'NO_ENCONTRADO', mensaje: 'No existe' } })
}
