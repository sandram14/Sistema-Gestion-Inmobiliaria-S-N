// Formas de los datos que intercambia el panel con la API Java.
// Coinciden con los DTOs del backend (dto/request y dto/response).

export type TipoPropiedad = 'Casa' | 'Terreno'
export type Operacion = 'Venta' | 'Alquiler'
export type Moneda = 'PYG' | 'USD'

export interface UsuarioSesion {
  id: number
  nombre: string
  apellido: string
  email: string
  rol: string
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
