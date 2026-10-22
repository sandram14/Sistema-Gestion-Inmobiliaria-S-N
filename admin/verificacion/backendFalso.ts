// Backend simulado con las mismas respuestas que la API Java (incluidos 401, 400 con campos y FUERA_DE_ZONA)
export const pedidos: { metodo: string; url: string; cuerpo?: unknown; archivo?: File | null }[] = []
export const estado = { sesion: false, expirarEnListado: false, errorAlCrear: null as null | Record<string, string> }
const detalle = (id: number, titulo = 'Departamento en San Jose') => ({
  id, codigo: id === 4 ? 'prop-004' : 'prop-017', titulo, descripcion: null, direccion: 'Calle 1', latitud: -25.5117, longitud: -54.6118,
  tipo: 'Casa', estado: 'Disponible',
  ubicacion: { idBarrio: 10011013, barrio: 'AREA 1', idCiudad: 1, ciudad: 'CIUDAD DEL ESTE', idDepartamento: 1, departamento: 'ALTO PARANÁ' },
  casa: { habitaciones: 2, banos: 2, sala: true, comedor: true, cocina: true, superficieConstruida: 45, superficieTerreno: null },
  terreno: null, operaciones: [{ operacion: 'Alquiler', precio: 4000000, moneda: 'PYG' }],
  responsable: { id: 50, nombre: 'Vera Vendedora', activo: true },
  imagenes: [{ id: 10, url: '/uploads/prop-004/a.png', descripcion: null, esPrincipal: true, orden: 0 }, { id: 11, url: '/uploads/prop-004/b.png', descripcion: null, esPrincipal: false, orden: 1 }],
  creadoEn: '2026-10-01T10:00:00-03:00', actualizadoEn: '2026-10-01T10:00:00-03:00',
})
export const db = {
  lista: [
    { id: 4, codigo: 'prop-004', titulo: 'Departamento en San Jose', tipo: 'Casa', estado: 'Disponible', ciudad: 'CIUDAD DEL ESTE', barrio: 'SAN JOSE',
      operaciones: [{ operacion: 'Alquiler', precio: 4000000, moneda: 'PYG' }], imagenPrincipal: '/uploads/prop-004/a.png', actualizadoEn: '2026-10-01T10:00:00-03:00', responsable: { id: 50, nombre: 'Vera Vendedora', activo: true } },
    { id: 20, codigo: 'terr-001', titulo: 'Terreno en Centro', tipo: 'Terreno', estado: 'Disponible', ciudad: 'HERNANDARIAS', barrio: 'CENTRO',
      operaciones: [{ operacion: 'Venta', precio: 300000000, moneda: 'PYG' }], imagenPrincipal: null, actualizadoEn: '2026-10-01T10:00:00-03:00', responsable: null },
  ],
  detalle4: detalle(4),
}
const catalogos = { tipos: ['Casa', 'Terreno'], estados: ['Disponible', 'Reservado', 'Vendido', 'Alquilado', 'Inactivo'],
  operaciones: ['Venta', 'Alquiler'], monedas: ['PYG'], operacionesPorTipo: { Casa: ['Venta', 'Alquiler'], Terreno: ['Venta'] }, departamentos: [] }
const TODOS = ['CLIENTES_ASIGNAR','CLIENTES_VER','CLIENTES_VER_TODOS','CONFIGURACION_EDITAR','CONFIGURACION_VER','CONSULTAS_VER','CONSULTAS_VER_TODAS','CONTENIDO_EDITAR','CUENTA_PROPIA','ESTADISTICAS_VER',
  'OFERTAS_GESTIONAR','PROPIEDADES_BAJA','PROPIEDADES_EDITAR','PROPIEDADES_EDITAR_TODAS','PROPIEDADES_VER','USUARIOS_GESTIONAR','USUARIOS_VER']
export const PERMISOS: Record<string, string[]> = {
  ADMIN: TODOS,
  GERENTE: TODOS.filter((p) => p !== 'USUARIOS_GESTIONAR' && p !== 'CONFIGURACION_EDITAR'),
  VENDEDOR: ['CLIENTES_VER', 'CONSULTAS_VER', 'CUENTA_PROPIA', 'PROPIEDADES_EDITAR', 'PROPIEDADES_VER'],
}
export const usuarioDe = (rol: string, id = 1) => ({ id, nombre: 'Paula', apellido: 'Admin', email: 'admin@sn.local', rol, permisos: PERMISOS[rol], debeCambiarPassword: false })
export let usuario = usuarioDe('ADMIN')
export function conRol(rol: string, id = 1) { usuario = usuarioDe(rol, id) }
const persona = (id: number, nombre: string, rol: string, activo = true) => ({ id, nombre, apellido: 'Prueba', email: `${nombre.toLowerCase()}@sn.local`,
  cedula: null, telefono: null, rol, activo, debeCambiarPassword: false, creadoEn: '2026-10-01T10:00:00-03:00', propiedadesAsignadas: id === 50 ? 1 : 0 })
export const usuariosDb = { lista: [persona(1, 'Paula', 'ADMIN'), persona(50, 'Vera', 'VENDEDOR'), persona(51, 'Gino', 'GERENTE', false)] }
const res = (status: number, json?: unknown) => new Response(json === undefined ? null : JSON.stringify(json), { status, headers: { 'Content-Type': 'application/json' } })
const noAut = () => res(401, { error: { codigo: 'NO_AUTENTICADO', mensaje: 'Inicie sesión para continuar' } })
export async function fetchFalso(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const url = String(input); const metodo = init?.method ?? 'GET'; const body = init?.body
  const reg: (typeof pedidos)[number] = { metodo, url }
  if (body instanceof FormData) reg.archivo = body.get('archivo') as File | null
  else if (typeof body === 'string') reg.cuerpo = JSON.parse(body)
  pedidos.push(reg)
  const u = new URL(url, 'http://localhost'); const p = u.pathname
  if (p === '/api/catalogos') return res(200, catalogos)
  if (p === '/api/barrios') return res(200, { type: 'FeatureCollection', features: [] })
  if (p === '/api/admin/auth/login') {
    const c = reg.cuerpo as { email: string; password: string }
    if (c.email === 'admin@sn.local' && c.password === 'Clave-Correcta') { estado.sesion = true; return res(200, { usuario }) }
    return res(401, { error: { codigo: 'NO_AUTENTICADO', mensaje: 'Email o contraseña incorrectos' } })
  }
  if (p === '/api/admin/auth/logout') { estado.sesion = false; return res(204) }
  if (p === '/api/admin/auth/me') return estado.sesion ? res(200, { usuario }) : noAut()
  if (p.startsWith('/api/admin/') && !estado.sesion) return noAut()
  if (p === '/api/admin/clientes-potenciales' && metodo === 'GET') return res(200, { elementos: clientesDb.lista, total: clientesDb.lista.length, pagina: 1, tamano: 20,
    conteos: { total: 2, pendientes: 1, contactados: 1, descartados: 0 } })
  if (p === '/api/admin/clientes-potenciales' && metodo === 'POST') {
    const c = reg.cuerpo as any
    if (!c.telefono && !c.email) return res(400, { error: { codigo: 'VALIDACION', mensaje: 'Hay datos inválidos.', campos: { contacto: 'Indique un teléfono o un email para que podamos contactarle' } } })
    return res(201, { ...clientesDb.detalle, id: 30, nombre: c.nombre, origen: c.origen, consentimientoEn: null, consentimientoVersion: null })
  }
  const mcl = p.match(/^\/api\/admin\/clientes-potenciales\/(\d+)(.*)$/)
  if (mcl) {
    const d = clientesDb.detalle
    if (mcl[2] === '/estado') d.estado = (reg.cuerpo as any).estado
    if (mcl[2] === '/responsable') d.responsable = (reg.cuerpo as any).idResponsable ? { id: (reg.cuerpo as any).idResponsable, nombre: 'Vera Prueba', activo: true } : null
    return res(200, { ...d, id: Number(mcl[1]) })
  }
  if (p === '/api/admin/sitio/secciones/inicio' && metodo === 'GET') return res(200, sitioDb.inicio)
  if (p === '/api/admin/sitio/secciones/inicio' && metodo === 'PUT') { sitioDb.inicio = reg.cuerpo as any; return res(200, sitioDb.inicio) }
  if (p === '/api/admin/sitio/secciones/contacto' && metodo === 'GET') return res(200, sitioDb.contacto)
  if (p === '/api/admin/sitio/secciones/contacto' && metodo === 'PUT') {
    const c = reg.cuerpo as any
    if (!/^[0-9]{8,15}$/.test(String(c.whatsapp).replace(/[ +()-]/g, ''))) return res(400, { error: { codigo: 'VALIDACION', mensaje: 'Hay datos inválidos.', campos: { whatsapp: 'Use el formato internacional, solo números (por ejemplo 595981123456)' } } })
    sitioDb.contacto = { ...c, whatsapp: String(c.whatsapp).replace(/[ +()-]/g, '') }; return res(200, sitioDb.contacto)
  }
  if (p === '/api/admin/sitio/secciones/servicios' && metodo === 'GET') return res(200, sitioDb.servicios)
  if (p === '/api/admin/sitio/secciones/servicios' && metodo === 'PUT') {
    const c = reg.cuerpo as any
    const i = c.servicios.findIndex((x: any) => !x.texto.trim())
    if (i >= 0) return res(400, { error: { codigo: 'VALIDACION', mensaje: 'Hay datos inválidos.', campos: { [`servicios[${i}].texto`]: 'Es obligatorio' } } })
    sitioDb.servicios = c; return res(200, c)
  }
  if (p === '/api/admin/sitio/secciones/nosotros' && metodo === 'GET') return res(200, sitioDb.nosotros)
  if (p === '/api/admin/sitio/secciones/nosotros' && metodo === 'PUT') { sitioDb.nosotros = reg.cuerpo as any; return res(200, sitioDb.nosotros) }
  if (p === '/api/admin/sitio/imagenes') return res(201, { url: '/uploads/sitio-001/0a4c5cd8-14cc-4bbc-a4d8-0f7bc29c541d.png' })
  if (p === '/api/admin/sitio/ofertas' && metodo === 'GET') return res(200, sitioDb.ofertas)
  if (p === '/api/admin/sitio/ofertas' && metodo === 'PUT') {
    const ids = (reg.cuerpo as any).ids as number[]
    sitioDb.ofertas = ids.map((id, i) => { const x = db.lista.find((l) => l.id === id)!; return { idPropiedad: id, codigo: x.codigo, titulo: x.titulo, estado: x.estado, orden: i + 1 } })
    return res(200, sitioDb.ofertas)
  }
  if (p === '/api/admin/consultas/analisis') return res(200, consultasDb.analisis)
  if (p === '/api/admin/consultas' && metodo === 'GET') {
    const pagina = Number(u.searchParams.get('pagina') ?? '1')
    return res(200, { elementos: consultasDb.lista, total: consultasDb.total, pagina, tamano: 20 })
  }
  const mc = p.match(/^\/api\/admin\/consultas\/(\d+)(.*)$/)
  if (mc) {
    const d = consultasDb.detalles[Number(mc[1])]
    if (!d) return res(404, { error: { codigo: 'NO_ENCONTRADO', mensaje: 'La consulta no existe' } })
    if (mc[2] === '/revision') {
      const e = (reg.cuerpo as any).estado
      Object.assign(d, { estadoRevision: e, revisadaPor: e === 'REVISADA' ? 'Paula Admin' : null, revisadaEn: e === 'REVISADA' ? '2026-10-06T10:00:00-03:00' : null })
    }
    return res(200, d)
  }
  if (p === '/api/admin/usuarios' && metodo === 'GET') return res(200, usuariosDb.lista)
  if (p === '/api/admin/usuarios' && metodo === 'POST') {
    const c = reg.cuerpo as any
    if (usuariosDb.lista.some((u) => u.email.toLowerCase() === String(c.email).toLowerCase())) return res(409, { error: { codigo: 'CONFLICTO', mensaje: 'Ya existe otro usuario con ese email' } })
    const nuevo = { ...persona(60, c.nombre, c.rol), email: c.email, debeCambiarPassword: true }
    usuariosDb.lista.push(nuevo); return res(201, { usuario: nuevo, contrasenaTemporal: 'Tmp7kQ9xWz2a' })
  }
  const mu = p.match(/^\/api\/admin\/usuarios\/(\d+)(.*)$/)
  if (mu) {
    const u = usuariosDb.lista.find((x) => x.id === Number(mu[1]))!
    if (mu[2] === '' && metodo === 'GET') return res(200, u)
    if (mu[2] === '' && metodo === 'PUT') { Object.assign(u, reg.cuerpo as any); return res(200, u) }
    if (mu[2] === '/estado') { u.activo = (reg.cuerpo as any).activo; return res(200, u) }
    if (mu[2] === '/contrasena-temporal') { u.debeCambiarPassword = true; return res(200, { usuario: u, contrasenaTemporal: 'Nueva8mPq3Rt' }) }
  }
  if (p === '/api/admin/cuenta' && metodo === 'GET') return res(200, usuariosDb.lista[0])
  if (p === '/api/admin/cuenta' && metodo === 'PUT') { Object.assign(usuariosDb.lista[0], reg.cuerpo as any); return res(200, usuariosDb.lista[0]) }
  if (p === '/api/admin/cuenta/email') {
    const c = reg.cuerpo as any
    if (c.passwordActual !== 'Clave-Correcta') return res(400, { error: { codigo: 'VALIDACION', mensaje: 'Hay datos inválidos.', campos: { passwordActual: 'La contraseña actual no es correcta' } } })
    usuariosDb.lista[0].email = c.email; return res(200, usuariosDb.lista[0])
  }
  if (p === '/api/admin/cuenta/contrasena') {
    const c = reg.cuerpo as any
    if (c.passwordActual !== 'Clave-Correcta') return res(400, { error: { codigo: 'VALIDACION', mensaje: 'Hay datos inválidos.', campos: { passwordActual: 'La contraseña actual no es correcta' } } })
    usuario.debeCambiarPassword = false; return res(204)
  }
  if (p === '/api/admin/propiedades' && metodo === 'GET') { if (estado.expirarEnListado) { estado.sesion = false; return noAut() } return res(200, db.lista) }
  if (p === '/api/admin/ubicacion') {
    const lat = Number(u.searchParams.get('latitud')); const lng = Number(u.searchParams.get('longitud'))
    if (lat > -25.6 && lat < -25.4 && lng > -54.7 && lng < -54.5)
      return res(200, { latitud: Number(lat.toFixed(6)), longitud: Number(lng.toFixed(6)), departamento: { id: 1, nombre: 'ALTO PARANÁ' }, ciudad: { id: 1, nombre: 'CIUDAD DEL ESTE' }, barrio: { id: 10011013, nombre: 'AREA 1' } })
    return res(404, { error: { codigo: 'FUERA_DE_ZONA', mensaje: 'El punto marcado está fuera de las zonas registradas.' } })
  }
  if (p === '/api/admin/propiedades' && metodo === 'POST') {
    if (estado.errorAlCrear) return res(400, { error: { codigo: 'VALIDACION', mensaje: 'Hay datos inválidos. Revise los campos indicados.', campos: estado.errorAlCrear } })
    return res(201, { ...detalle(17, (reg.cuerpo as { titulo: string }).titulo), imagenes: [] })
  }
  const m = p.match(/^\/api\/admin\/propiedades\/(\d+)(.*)$/)
  if (m) {
    const id = Number(m[1]); const resto = m[2]
    if (resto === '' && metodo === 'GET') { if (id === 4) return res(200, db.detalle4); if (id === 17) return res(200, { ...detalle(17, 'Casa nueva'), imagenes: [] }); return res(404, { error: { codigo: 'NO_ENCONTRADO', mensaje: 'La propiedad no existe' } }) }
    if (resto === '' && metodo === 'PUT') { db.detalle4 = { ...db.detalle4, titulo: (reg.cuerpo as { titulo: string }).titulo }; return res(200, db.detalle4) }
    if (resto === '' && metodo === 'DELETE') { db.lista = db.lista.map((x) => (x.id === id ? { ...x, estado: 'Inactivo' } : x)); return res(204) }
    if (resto === '/imagenes' && metodo === 'POST') return res(201, db.detalle4)
    if (/^\/imagenes\/\d+\/principal$/.test(resto)) return res(200, db.detalle4)
    if (/^\/imagenes\/\d+$/.test(resto) && metodo === 'DELETE') return res(200, db.detalle4)
  }
  return res(404, { error: { codigo: 'NO_ENCONTRADO', mensaje: 'No existe' } })
}

const ref = { id: 4, codigo: 'prop-004', titulo: 'Departamento en San Jose' }
export const consultasDb = {
  total: 2,
  lista: [
    { id: 2, fechaHora: '2026-10-05T11:00:00-03:00', pregunta: '¿Tiene cochera?', propiedad: ref, resultado: null, estadoRevision: 'PENDIENTE', registroCompleto: false },
    { id: 9, fechaHora: '2026-10-06T09:00:00-03:00', pregunta: 'Casas en alquiler', propiedad: null, resultado: 'RESPONDIDA', estadoRevision: 'REVISADA', registroCompleto: true },
  ],
  detalles: {
    2: { id: 2, fechaHora: '2026-10-05T11:00:00-03:00', pregunta: '¿Tiene cochera?', respuesta: 'No dispongo de esa información.', propiedad: ref,
      recomendadas: [], resultado: null, registroCompleto: false, estadoRevision: 'PENDIENTE', revisadaPor: null, revisadaEn: null,
      conversacion: [{ id: 1, fechaHora: '2026-10-05T10:59:00-03:00', pregunta: '¿Cuánto cuesta?', actual: false }, { id: 2, fechaHora: '2026-10-05T11:00:00-03:00', pregunta: '¿Tiene cochera?', actual: true }] },
    9: { id: 9, fechaHora: '2026-10-06T09:00:00-03:00', pregunta: 'Casas en alquiler', respuesta: '- prop-004…', propiedad: null,
      recomendadas: [ref], resultado: 'RESPONDIDA', registroCompleto: true, estadoRevision: 'REVISADA', revisadaPor: 'Paula Admin', revisadaEn: '2026-10-06T10:00:00-03:00',
      conversacion: [{ id: 9, fechaHora: '2026-10-06T09:00:00-03:00', pregunta: 'Casas en alquiler', actual: true }] },
  } as Record<number, any>,
  analisis: {
    indicadores: { consultas: 2, conversaciones: 2, conPropiedad: 1, sinServicio: 0, conRegistroCompleto: 1,
      preguntasRepetidas: [], propiedadesConsultadas: [{ propiedad: ref, cantidad: 1 }], tiposComprobados: [{ nombre: 'Casa', cantidad: 1 }],
      zonasComprobadas: [{ nombre: 'CIUDAD DEL ESTE', cantidad: 1 }], temasInferidos: [{ nombre: 'Características', cantidad: 1 }],
      tiposInferidos: [{ nombre: 'Casa', cantidad: 1 }], zonasInferidas: [], masRecomendadas: [{ propiedad: ref, cantidad: 1 }],
      rangosPrecio: [{ operacion: 'Alquiler', moneda: 'PYG', rango: 'Gs. 2.000.000 a Gs. 4.000.000', cantidad: 1 }],
      caracteristicasInferidas: [{ nombre: 'Cochera', cantidad: 1 }], calidad: { fueraDeAmbitoNoRegistrado: 1 } },
    sinServicio: [], derivadas: [],
    sinInformacionUtil: [{ id: 2, fechaHora: '2026-10-05T11:00:00-03:00', pregunta: '¿Tiene cochera?', propiedad: ref, resultado: null, estadoRevision: 'PENDIENTE', registroCompleto: false }],
    fueraDeAmbito: [{ id: 12, fechaHora: '2026-10-07T10:00:00-03:00', pregunta: '¿Quién ganó el mundial?', propiedad: null, resultado: 'FUERA_DE_AMBITO', estadoRevision: 'PENDIENTE', registroCompleto: true }], sinInformacionInferida: [{ id: 2, fechaHora: '2026-10-05T11:00:00-03:00', pregunta: '¿Tiene cochera?', propiedad: ref, resultado: null, estadoRevision: 'PENDIENTE', registroCompleto: false }],
  },
}

export const clientesDb = {
  lista: [
    { id: 20, nombre: 'Ana Torres', contacto: '0981 123 456', interes: 'Casa con patio', propiedad: ref, origen: 'CHAT', estado: 'PENDIENTE', responsable: null, creadoEn: '2026-10-12T10:00:00-03:00' },
    { id: 21, nombre: 'Carlos López', contacto: 'carlos@correo.test', interes: null, propiedad: null, origen: 'CONTACTO', estado: 'CONTACTADO', responsable: { id: 50, nombre: 'Vera Prueba', activo: true }, creadoEn: '2026-10-11T10:00:00-03:00' },
  ],
  detalle: { id: 20, nombre: 'Ana Torres', telefono: '0981 123 456', email: null, interes: 'Casa con patio', propiedad: ref, idConsulta: 9, origen: 'CHAT',
    estado: 'PENDIENTE', responsable: null as any, registradoPor: null, consentimientoEn: '2026-10-12T10:00:00-03:00', consentimientoVersion: '2026-10',
    creadoEn: '2026-10-12T10:00:00-03:00', actualizadoEn: '2026-10-12T10:00:00-03:00' },
}

export const sitioDb = {
  inicio: { imagenPortada: null as string | null, etiqueta: '', titulo: 'Encuentra el lugar', subtitulo: 'Texto', nosotrosTitulo: 'Por qué elegirnos', nosotrosTexto: 'Conocemos', nosotrosImagen: null as string | null } as any,
  servicios: { titulo: 'Nuestros Servicios', subtitulo: 'Sub', servicios: [{ titulo: 'Compra y Venta', texto: 'Asesoramos' }, { titulo: 'Alquileres', texto: 'Rápido' }], bloqueTitulo: '', bloqueTexto: '' } as any,
  nosotros: { introduccion: 'Somos', historiaTitulo: 'Equipo', historiaTexto: 'Nacimos', imagen: null, cifras: [{ valor: '+150', etiqueta: 'Propiedades' }],
    valores: [{ titulo: 'Confianza', texto: 'Real' }], razones: [{ titulo: 'Barrios', texto: 'Zona por zona' }] } as any,
  contacto: { nombreEmpresa: 'S&N Inmobiliaria', descripcion: 'Desc', direccion: '', ciudad: 'Ciudad del Este', email: 'contacto@sn-inmobiliaria.example', telefono: '+595 000 000 000', whatsapp: '595000000000', horario: '' },
  ofertas: [] as any[],
}
