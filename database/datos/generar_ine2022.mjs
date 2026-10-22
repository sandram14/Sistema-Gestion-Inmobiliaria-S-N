// =====================================================================
// Genera database/datos/ine2022_alto_parana.sql a partir de la cartografía censal 2022 del INE
// (https://www.ine.gov.py/microdatos/cartografia-digital-2022.php, "10 ALTO PARANA", formato GeoJSON).
//
// Uso (desde la raíz del repositorio, con el .rar del INE ya descomprimido):
//   node database/datos/generar_ine2022.mjs "<carpeta>/Barrios Localidades_Alto Parana.geojson" "<carpeta>/Ciudades_Alto Parana.geojson"
//
// El archivo generado solo inserta en dos tablas temporales (ine_distrito e ine_barrio) que crea quien lo
// incluye: la migración 010 y la carga inicial seed/02_ubicacion.sql. Las geometrías se copian tal cual
// (EPSG:4674, SIRGAS 2000, equivalente a WGS84 a escala de metros); los centros de las ciudades, con 6 decimales.
// =====================================================================
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const [archivoBarrios, archivoCiudades] = process.argv.slice(2)
if (!archivoBarrios || !archivoCiudades) {
  console.error('Uso: node database/datos/generar_ine2022.mjs <Barrios Localidades.geojson> <Ciudades.geojson>')
  process.exit(1)
}
const destino = path.join(path.dirname(fileURLToPath(import.meta.url)), 'ine2022_alto_parana.sql')
const barrios = JSON.parse(fs.readFileSync(archivoBarrios, 'utf8')).features
const ciudades = JSON.parse(fs.readFileSync(archivoCiudades, 'utf8')).features

const texto = (s) => `'${String(s).trim().replace(/'/g, "''")}'`
const seis = (n) => Number(n).toFixed(6)

const distritos = ciudades
  .map((c) => ({ d: c.properties.DISTRITO, n: c.properties.DIST_DESC_, x: c.geometry.coordinates[0], y: c.geometry.coordinates[1] }))
  .sort((a, b) => a.d.localeCompare(b.d))
const filasBarrios = barrios
  .map((b) => ({ p: b.properties, g: b.geometry }))
  .sort((a, b) => a.p.CLAVE_BAR.localeCompare(b.p.CLAVE_BAR))

// Comprobaciones mínimas antes de escribir: un departamento, claves únicas, todo con nombre y polígono
if (new Set(filasBarrios.map((f) => f.p.DPTO)).size !== 1 || filasBarrios[0].p.DPTO !== '10') throw new Error('Se esperaba solo Alto Paraná (10)')
if (new Set(filasBarrios.map((f) => f.p.CLAVE_BAR)).size !== filasBarrios.length) throw new Error('CLAVE_BAR repetida')
if (filasBarrios.some((f) => !f.p.BARLO_DESC?.trim() || f.g?.type !== 'MultiPolygon')) throw new Error('Barrio sin nombre o sin MultiPolygon')
const codigosDistrito = new Set(distritos.map((d) => d.d))
if (filasBarrios.some((f) => !codigosDistrito.has(f.p.DISTRITO))) throw new Error('Barrio de un distrito sin ciudad')

const lineas = [
  '-- =====================================================================',
  '-- Cartografía censal 2022 del INE: distritos (ciudades) y barrios/localidades de Alto Paraná',
  '-- Fuente: https://www.ine.gov.py/microdatos/cartografia-digital-2022.php (Licencia de Uso de la',
  '-- Información Pública). GENERADO por database/datos/generar_ine2022.mjs: no editar a mano.',
  `-- ${distritos.length} distritos y ${filasBarrios.length} barrios/localidades. area: 1 urbano, 3 urbano en zona rural, 6 rural.`,
  '-- Inserta en las tablas temporales ine_distrito e ine_barrio, que crea quien incluye este archivo.',
  '-- =====================================================================',
  '',
  'INSERT INTO ine_distrito (distrito, nombre, longitud, latitud) VALUES',
  distritos.map((d) => `    (${texto(d.d)}, ${texto(d.n)}, ${seis(d.x)}, ${seis(d.y)})`).join(',\n') + ';',
  '',
]
// De a 25 barrios por sentencia: archivos legibles y sentencias de tamaño razonable
for (let i = 0; i < filasBarrios.length; i += 25) {
  lineas.push('INSERT INTO ine_barrio (clave, nombre, distrito, area, geojson) VALUES')
  lineas.push(filasBarrios.slice(i, i + 25).map(({ p, g }) =>
    `    (${Number(p.CLAVE_BAR)}, ${texto(p.BARLO_DESC)}, ${texto(p.DISTRITO)}, ${texto(p.AREA)}, ${texto(JSON.stringify(g))})`).join(',\n') + ';')
  lineas.push('')
}
fs.writeFileSync(destino, lineas.join('\n'), 'utf8')
console.log(`${destino}: ${distritos.length} distritos, ${filasBarrios.length} barrios, ${(fs.statSync(destino).size / 1e6).toFixed(1)} MB`)
