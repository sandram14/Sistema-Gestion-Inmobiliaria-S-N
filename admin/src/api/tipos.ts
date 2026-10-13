// Formas de los datos que intercambia el panel con la API Java.
// Coinciden con los DTOs del backend (dto/request y dto/response).

export type TipoPropiedad = 'Casa' | 'Terreno'
export type Operacion = 'Venta' | 'Alquiler'
export type Moneda = 'PYG' | 'USD'

/** Permisos que asigna el backend según el rol (ver seguridad/Permiso.java). */
export type Permiso =
  | 'PROPIEDADES_VER' | 'PROPIEDADES_EDITAR' | 'PROPIEDADES_EDITAR_TODAS' | 'PROPIEDADES_BAJA'
  | 'OFERTAS_GESTIONAR' | 'CONTENIDO_EDITAR' | 'CONFIGURACION_VER' | 'CONFIGURACION_EDITAR'
  | 'CONSULTAS_VER' | 'CONSULTAS_VER_TODAS' | 'ESTADISTICAS_VER' | 'USUARIOS_VER' | 'USUARIOS_GESTIONAR'
  | 'CLIENTES_VER' | 'CLIENTES_VER_TODOS' | 'CLIENTES_ASIGNAR' | 'CUENTA_PROPIA'

export interface UsuarioSesion {
  id: number
  nombre: string
  apellido: string
  email: string
  rol: string
  permisos: Permiso[]
  debeCambiarPassword: boolean
}

export interface OperacionPrecio {
  operacion: Operacion
  precio: number
  moneda: Moneda
}

/** Fila del listado (GET /api/admin/propiedades) */
export interface PropiedadResumen {
  id: number
  codigo: string
  titulo: string
  tipo: TipoPropiedad
  estado: string
  ciudad: string
  barrio: string
  operaciones: OperacionPrecio[]
  imagenPrincipal: string | null
  actualizadoEn: string
  responsable: ResponsableDto | null
}

/** Responsable de una propiedad ("activo" en false si la cuenta fue desactivada). */
export interface ResponsableDto {
  id: number
  nombre: string
  activo: boolean
}

/** Usuario del panel (GET /api/admin/usuarios). Nunca incluye contraseñas. */
export interface UsuarioDto {
  id: number
  nombre: string
  apellido: string
  email: string
  cedula: string | null
  telefono: string | null
  rol: string
  activo: boolean
  debeCambiarPassword: boolean
  creadoEn: string
  propiedadesAsignadas: number
}

/** Respuesta del alta o del restablecimiento: la contraseña temporal se muestra una sola vez. */
export interface UsuarioConContrasena {
  usuario: UsuarioDto
  contrasenaTemporal: string
}

export interface ImagenDto {
  id: number
  url: string
  descripcion: string | null
  esPrincipal: boolean
  orden: number
}

/** Detalle para editar (GET /api/admin/propiedades/{id}) */
export interface PropiedadDetalle {
  id: number
  codigo: string
  titulo: string
  descripcion: string | null
  direccion: string | null
  latitud: number
  longitud: number
  tipo: TipoPropiedad
  estado: string
  ubicacion: {
    idBarrio: number
    barrio: string
    idCiudad: number
    ciudad: string
    idDepartamento: number
    departamento: string
  }
  casa: {
    habitaciones: number
    banos: number
    sala: boolean
    comedor: boolean
    cocina: boolean
    superficieConstruida: number
    superficieTerreno: number | null
  } | null
  terreno: {
    superficie: number
    frente: number | null
    fondo: number | null
    tieneTitulo: boolean
  } | null
  operaciones: OperacionPrecio[]
  imagenes: ImagenDto[]
  creadoEn: string
  actualizadoEn: string
  responsable: ResponsableDto | null
}

/** Cuerpo de POST/PUT /api/admin/propiedades */
export interface PropiedadRequest {
  titulo: string
  descripcion: string | null
  direccion: string | null
  latitud: number | null
  longitud: number | null
  idBarrio: number | null
  tipo: TipoPropiedad
  estado: string | null
  casa: {
    habitaciones: number | null
    banos: number | null
    sala: boolean
    comedor: boolean
    cocina: boolean
    superficieConstruida: number | null
    superficieTerreno: number | null
  } | null
  terreno: {
    superficie: number | null
    frente: number | null
    fondo: number | null
    tieneTitulo: boolean
  } | null
  operaciones: { operacion: string; precio: number | null; moneda: string }[]
  /** Solo lo considera el backend si el usuario puede editar todas las propiedades */
  idResponsable: number | null
}

/** GET /api/catalogos */
export interface Catalogos {
  tipos: TipoPropiedad[]
  estados: string[]
  operaciones: Operacion[]
  monedas: Moneda[]
  operacionesPorTipo: Record<string, Operacion[]>
}

/** GET /api/admin/ubicacion */
export interface UbicacionDetectada {
  latitud: number
  longitud: number
  departamento: { id: number; nombre: string }
  ciudad: { id: number; nombre: string }
  barrio: { id: number; nombre: string }
}

/** Barrios para dibujar en el mapa (GET /api/barrios) */
export interface BarriosGeoJson {
  type: 'FeatureCollection'
  features: {
    type: 'Feature'
    geometry: { type: string; coordinates: unknown }
    properties: { id: number; barrio: string; ciudad: string }
  }[]
}

// ---- Consultas del chat IA ----
export interface PropiedadRef { id: number; codigo: string; titulo: string }
export type ResultadoConsulta = 'RESPONDIDA' | 'DERIVADA_ASESOR' | 'SIN_SERVICIO' | 'BLOQUEADA' | 'FUERA_DE_AMBITO'
export interface PaginaConsultas { elementos: ConsultaResumen[]; total: number; pagina: number; tamano: number }

/** registroCompleto en false: consulta anterior al registro de resultados y recomendaciones */
export interface ConsultaResumen {
  id: number; fechaHora: string; pregunta: string; propiedad: PropiedadRef | null
  resultado: ResultadoConsulta | null; estadoRevision: 'PENDIENTE' | 'REVISADA'; registroCompleto: boolean
}
export interface ConsultaDetalle {
  id: number; fechaHora: string; pregunta: string; respuesta: string | null; propiedad: PropiedadRef | null
  recomendadas: PropiedadRef[]; resultado: ResultadoConsulta | null; registroCompleto: boolean
  estadoRevision: 'PENDIENTE' | 'REVISADA'; revisadaPor: string | null; revisadaEn: string | null
  conversacion: { id: number; fechaHora: string; pregunta: string; actual: boolean }[]
}
export interface Conteo { nombre: string; cantidad: number }
export interface ConteoPropiedad { propiedad: PropiedadRef; cantidad: number }
export interface AnalisisConsultas {
  indicadores: {
    consultas: number; conversaciones: number; conPropiedad: number; sinServicio: number; conRegistroCompleto: number
    preguntasRepetidas: Conteo[]; propiedadesConsultadas: ConteoPropiedad[]; tiposComprobados: Conteo[]; zonasComprobadas: Conteo[]
    temasInferidos: Conteo[]; tiposInferidos: Conteo[]; zonasInferidas: Conteo[]; masRecomendadas: ConteoPropiedad[]
    rangosPrecio: { operacion: string; moneda: string; rango: string; cantidad: number }[]
    caracteristicasInferidas: Conteo[]
    calidad: { fueraDeAmbitoNoRegistrado: number }
  }
  sinServicio: ConsultaResumen[]; derivadas: ConsultaResumen[]; sinInformacionInferida: ConsultaResumen[]
  sinInformacionUtil: ConsultaResumen[]; fueraDeAmbito: ConsultaResumen[]
}

// ---- Clientes potenciales ----
export type EstadoCliente = 'PENDIENTE' | 'CONTACTADO' | 'DESCARTADO'
export type OrigenCliente = 'CHAT' | 'WHATSAPP' | 'LLAMADA' | 'OTRO'
export interface PersonaRef { id: number; nombre: string; activo: boolean }
export interface ClienteResumen {
  id: number; nombre: string; contacto: string; interes: string | null; propiedad: PropiedadRef | null
  origen: OrigenCliente; estado: EstadoCliente; responsable: PersonaRef | null; creadoEn: string
}
export interface ClienteDetalle {
  id: number; nombre: string; telefono: string | null; email: string | null; interes: string | null; propiedad: PropiedadRef | null
  idConsulta: number | null; origen: OrigenCliente; estado: EstadoCliente; responsable: PersonaRef | null; registradoPor: PersonaRef | null
  consentimientoEn: string | null; consentimientoVersion: string | null; creadoEn: string; actualizadoEn: string
}
export interface PaginaClientes {
  elementos: ClienteResumen[]; total: number; pagina: number; tamano: number
  conteos: { total: number; pendientes: number; contactados: number; descartados: number }
}
