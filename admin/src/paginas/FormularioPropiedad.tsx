import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import BotonVolver from '../componentes/BotonVolver'
import { useSesion } from '../auth/Sesion'
import { api, ApiError } from '../api/cliente'
import type { Catalogos, PropiedadDetalle, PropiedadRequest, TipoPropiedad, UsuarioDto } from '../api/tipos'
import EditorOperaciones, { type FilaOperacion } from '../componentes/EditorOperaciones'
import GaleriaImagenes from '../componentes/GaleriaImagenes'
import SelectorUbicacion, { type ValorUbicacion } from '../componentes/SelectorUbicacion'
import { nombreDelTipo, textoFecha } from '../componentes/formato'
import ui from '../ui.module.css'

/** Valores del formulario como texto (lo que escribe el usuario), convertidos al guardar. */
interface Formulario {
  titulo: string
  descripcion: string
  direccion: string
  tipo: TipoPropiedad
  estado: string
  habitaciones: string
  banos: string
  sala: boolean
  comedor: boolean
  cocina: boolean
  superficieConstruida: string
  superficieTerreno: string
  superficie: string
  frente: string
  fondo: string
  tieneTitulo: boolean
}

const VACIO: Formulario = {
  titulo: '', descripcion: '', direccion: '', tipo: 'Casa', estado: 'Disponible',
  habitaciones: '', banos: '', sala: false, comedor: false, cocina: false, superficieConstruida: '', superficieTerreno: '',
  superficie: '', frente: '', fondo: '', tieneTitulo: false,
}

const texto = (v: number | null | undefined) => (v == null ? '' : String(v))
const numero = (s: string): number | null => (s.trim() === '' ? null : Number(s))
const nulo = (s: string): string | null => (s.trim() === '' ? null : s.trim())

function desdeDetalle(d: PropiedadDetalle): Formulario {
  return {
    titulo: d.titulo, descripcion: d.descripcion ?? '', direccion: d.direccion ?? '', tipo: d.tipo, estado: d.estado,
    habitaciones: texto(d.casa?.habitaciones), banos: texto(d.casa?.banos), sala: d.casa?.sala ?? false,
    comedor: d.casa?.comedor ?? false, cocina: d.casa?.cocina ?? false,
    superficieConstruida: texto(d.casa?.superficieConstruida), superficieTerreno: texto(d.casa?.superficieTerreno),
    superficie: texto(d.terreno?.superficie), frente: texto(d.terreno?.frente), fondo: texto(d.terreno?.fondo),
    tieneTitulo: d.terreno?.tieneTitulo ?? false,
  }
}

export default function FormularioPropiedad() {
  const { id } = useParams()
  const esEdicion = id !== undefined
  const navegar = useNavigate()
  const estadoNavegacion = useLocation().state as { mensaje?: string } | null

  const { puede, usuario: yo } = useSesion()
  const editaTodas = puede('PROPIEDADES_EDITAR_TODAS')
  const [responsables, setResponsables] = useState<UsuarioDto[]>([])
  const [idResponsable, setIdResponsable] = useState('')
  const [catalogos, setCatalogos] = useState<Catalogos | null>(null)
  const [detalle, setDetalle] = useState<PropiedadDetalle | null>(null)
  const [form, setForm] = useState<Formulario>(VACIO)
  const [operaciones, setOperaciones] = useState<FilaOperacion[]>([{ operacion: 'Venta', precio: '' }])
  const [ubicacion, setUbicacion] = useState<ValorUbicacion>({ latitud: null, longitud: null, detectada: null })
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(estadoNavegacion?.mensaje ?? null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)

  function cargarDetalle(d: PropiedadDetalle) {
    setDetalle(d)
    setForm(desdeDetalle(d))
    setIdResponsable(d.responsable ? String(d.responsable.id) : '')
    setOperaciones(d.operaciones.map((o) => ({ operacion: o.operacion, precio: String(o.precio) })))
    setUbicacion({
      latitud: d.latitud,
      longitud: d.longitud,
      detectada: {
        latitud: d.latitud, longitud: d.longitud,
        departamento: { id: d.ubicacion.idDepartamento, nombre: d.ubicacion.departamento },
        ciudad: { id: d.ubicacion.idCiudad, nombre: d.ubicacion.ciudad },
        barrio: { id: d.ubicacion.idBarrio, nombre: d.ubicacion.barrio },
      },
    })
  }

  useEffect(() => {
    setCargando(true)
    setErrores({})
    setErrorGeneral(null)
    Promise.all([
      api.get<Catalogos>('/api/catalogos'),
      esEdicion ? api.get<PropiedadDetalle>(`/api/admin/propiedades/${id}`) : Promise.resolve(null),
    ])
      .then(([c, d]) => {
        setCatalogos(c)
        if (d) cargarDetalle(d)
      })
      .catch((e) => setErrorGeneral(e instanceof ApiError ? e.message : 'No se pudieron cargar los datos.'))
      .finally(() => setCargando(false))
  }, [id, esEdicion])

  useEffect(() => {
    if (editaTodas) api.get<UsuarioDto[]>('/api/admin/usuarios').then(setResponsables).catch(() => setResponsables([]))
  }, [editaTodas])

  // Un vendedor puede ver cualquier propiedad, pero solo editar las asignadas a él
  const soloLectura = esEdicion && detalle !== null && !editaTodas && detalle.responsable?.id !== yo?.id

  function cambiar<K extends keyof Formulario>(campo: K, valor: Formulario[K]) {
    setForm((f) => ({ ...f, [campo]: valor }))
  }

  function cambiarTipo(tipo: TipoPropiedad) {
    cambiar('tipo', tipo)
    // Regla del negocio: un terreno solo se vende. Se quitan las operaciones no permitidas.
    const permitidas = catalogos?.operacionesPorTipo[tipo] ?? []
    const filtradas = operaciones.filter((o) => permitidas.includes(o.operacion))
    setOperaciones(filtradas.length ? filtradas : [{ operacion: 'Venta', precio: '' }])
  }

  function armarRequest(): PropiedadRequest {
    const esCasa = form.tipo === 'Casa'
    return {
      titulo: form.titulo,
      descripcion: nulo(form.descripcion),
      direccion: nulo(form.direccion),
      latitud: ubicacion.detectada?.latitud ?? null,
      longitud: ubicacion.detectada?.longitud ?? null,
      idBarrio: ubicacion.detectada?.barrio.id ?? null, // siempre el barrio que determinó el backend
      tipo: form.tipo,
      estado: form.estado,
      casa: esCasa
        ? {
            habitaciones: numero(form.habitaciones), banos: numero(form.banos),
            sala: form.sala, comedor: form.comedor, cocina: form.cocina,
            superficieConstruida: numero(form.superficieConstruida), superficieTerreno: numero(form.superficieTerreno),
          }
        : null,
      terreno: esCasa
        ? null
        : { superficie: numero(form.superficie), frente: numero(form.frente), fondo: numero(form.fondo), tieneTitulo: form.tieneTitulo },
      operaciones: operaciones.map((o) => ({ operacion: o.operacion, precio: numero(o.precio), moneda: 'PYG' as const })),
      // Solo ADMIN y GERENTE eligen el responsable; lo que crea un vendedor queda a su nombre (lo decide el backend)
      idResponsable: editaTodas && idResponsable ? Number(idResponsable) : null,
    }
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setErrores({})
    setErrorGeneral(null)
    setMensaje(null)
    // Sin ubicación detectada (punto fuera de las zonas o sin marcar) no se guarda
    if (!ubicacion.detectada) {
      const msg = 'Marque en el mapa un punto dentro de uno de los barrios registrados.'
      setErrores({ ubicacion: msg })
      setErrorGeneral(`No se puede guardar: ${msg}`)
      window.scrollTo?.({ top: 0, behavior: 'smooth' })
      return
    }
    setGuardando(true)
    try {
      if (esEdicion) {
        const d = await api.put<PropiedadDetalle>(`/api/admin/propiedades/${id}`, armarRequest())
        cargarDetalle(d)
        setMensaje('Cambios guardados.')
      } else {
        const d = await api.post<PropiedadDetalle>('/api/admin/propiedades', armarRequest())
        navegar(`/propiedades/${d.id}`, {
          replace: true,
          state: { mensaje: `Propiedad ${d.codigo} creada. Ahora puede agregar las fotos.` },
        })
      }
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.campos).length > 0) {
        setErrores(err.campos) // cada mensaje se muestra junto a su campo
        setErrorGeneral(err.message)
      } else {
        setErrorGeneral(err instanceof ApiError ? err.message : 'No se pudo guardar la propiedad.')
      }
    } finally {
      setGuardando(false)
      window.scrollTo?.({ top: 0, behavior: 'smooth' })
    }
  }

  if (cargando) return <p className={ui.cargando}>Cargando…</p>
  if (!catalogos) return <div className={ui.avisoError}>{errorGeneral ?? 'No se pudieron cargar los datos.'}</div>

  // Errores que no corresponden a un campo visible (para no perder ninguno)
  const camposVisibles = /^(titulo|descripcion|direccion|tipo|estado|ubicacion|latitud|longitud|idBarrio|idResponsable|casa|terreno|operaciones)/
  const otrosErrores = Object.entries(errores).filter(([k]) => !camposVisibles.test(k))
  const err = (campo: string) => errores[campo] && <span className={ui.errorCampo}>{errores[campo]}</span>
  const clase = (campo: string) => `${ui.input} ${errores[campo] ? ui.inputConError : ''}`
  const errorUbicacion = errores['ubicacion'] ?? errores['latitud'] ?? errores['longitud'] ?? errores['idBarrio']

  return (
    <>
      <div className={ui.tituloPagina}>
        <div className={ui.tituloConVolver}><BotonVolver alternativa="/propiedades" /><h1>{esEdicion ? `${soloLectura ? 'Propiedad' : 'Editar'} ${detalle?.codigo ?? ''}` : 'Nueva propiedad'}</h1></div>
        
      </div>

      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {errorGeneral && (
        <div className={ui.avisoError} role="alert">
          {errorGeneral}
          {otrosErrores.length > 0 && <ul>{otrosErrores.map(([k, v]) => <li key={k}>{v}</li>)}</ul>}
        </div>
      )}
      {detalle && <p className={ui.ayuda}>Creada {textoFecha(detalle.creadoEn)} · Última modificación {textoFecha(detalle.actualizadoEn)}</p>}

      {soloLectura && (
        <div className={ui.aviso}>Esta propiedad está asignada a otro responsable. Puede verla, pero no modificarla.</div>
      )}
      <form onSubmit={guardar} noValidate>
        <fieldset disabled={soloLectura} style={{ border: 'none', padding: 0, margin: 0, minWidth: 0 }}>
        <section className={ui.tarjeta}>
          <h2>Datos generales</h2>
          <div className={ui.grilla}>
            <label className={`${ui.campo} ${ui.campoAncho}`}>
              <span className={ui.etiqueta}>Título</span>
              <input className={clase('titulo')} maxLength={150} value={form.titulo} onChange={(e) => cambiar('titulo', e.target.value)} />
              {err('titulo')}
            </label>
            <label className={ui.campo}>
              <span className={ui.etiqueta}>Tipo</span>
              <select className={clase('tipo')} value={form.tipo} onChange={(e) => cambiarTipo(e.target.value as TipoPropiedad)}>
                {catalogos.tipos.map((t) => <option key={t} value={t}>{nombreDelTipo(t)}</option>)}
              </select>
              {err('tipo')}
            </label>
            <label className={ui.campo}>
              <span className={ui.etiqueta}>Estado</span>
              <select className={clase('estado')} value={form.estado} onChange={(e) => cambiar('estado', e.target.value)}>
                {catalogos.estados.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
              <span className={ui.ayuda}>Solo las propiedades "Disponible" se muestran en la web.</span>
              {err('estado')}
            </label>
            <label className={`${ui.campo} ${ui.campoAncho}`}>
              <span className={ui.etiqueta}>Dirección (opcional)</span>
              <input className={clase('direccion')} maxLength={200} value={form.direccion} onChange={(e) => cambiar('direccion', e.target.value)} />
              {err('direccion')}
            </label>
            <label className={`${ui.campo} ${ui.campoAncho}`}>
              <span className={ui.etiqueta}>Descripción (opcional)</span>
              <textarea className={clase('descripcion')} rows={4} maxLength={5000} value={form.descripcion}
                onChange={(e) => cambiar('descripcion', e.target.value)} />
              {err('descripcion')}
            </label>
          </div>
        </section>

        {form.tipo === 'Casa' ? (
          <section className={ui.tarjeta}>
            <h2>Datos de la vivienda</h2>
            <div className={ui.grilla}>
              <label className={ui.campo}>
                <span className={ui.etiqueta}>Habitaciones</span>
                <input className={clase('casa.habitaciones')} type="number" min="0" value={form.habitaciones} onChange={(e) => cambiar('habitaciones', e.target.value)} />
                {err('casa.habitaciones')}
              </label>
              <label className={ui.campo}>
                <span className={ui.etiqueta}>Baños</span>
                <input className={clase('casa.banos')} type="number" min="0" value={form.banos} onChange={(e) => cambiar('banos', e.target.value)} />
                {err('casa.banos')}
              </label>
              <label className={ui.campo}>
                <span className={ui.etiqueta}>Superficie construida (m²)</span>
                <input className={clase('casa.superficieConstruida')} type="number" min="0" step="any" value={form.superficieConstruida} onChange={(e) => cambiar('superficieConstruida', e.target.value)} />
                {err('casa.superficieConstruida')}
              </label>
              <label className={ui.campo}>
                <span className={ui.etiqueta}>Superficie del terreno (m², opcional)</span>
                <input className={clase('casa.superficieTerreno')} type="number" min="0" step="any" value={form.superficieTerreno} onChange={(e) => cambiar('superficieTerreno', e.target.value)} />
                {err('casa.superficieTerreno')}
              </label>
              <div className={ui.campo}>
                <span className={ui.etiqueta}>Ambientes</span>
                <div className={ui.checks}>
                  <label><input type="checkbox" checked={form.sala} onChange={(e) => cambiar('sala', e.target.checked)} /> Sala</label>
                  <label><input type="checkbox" checked={form.comedor} onChange={(e) => cambiar('comedor', e.target.checked)} /> Comedor</label>
                  <label><input type="checkbox" checked={form.cocina} onChange={(e) => cambiar('cocina', e.target.checked)} /> Cocina</label>
                </div>
              </div>
            </div>
            {err('casa')}
          </section>
        ) : (
          <section className={ui.tarjeta}>
            <h2>Datos del terreno</h2>
            <div className={ui.grilla}>
              <label className={ui.campo}>
                <span className={ui.etiqueta}>Superficie (m²)</span>
                <input className={clase('terreno.superficie')} type="number" min="0" step="any" value={form.superficie} onChange={(e) => cambiar('superficie', e.target.value)} />
                {err('terreno.superficie')}
              </label>
              <label className={ui.campo}>
                <span className={ui.etiqueta}>Frente (m, opcional)</span>
                <input className={clase('terreno.frente')} type="number" min="0" step="any" value={form.frente} onChange={(e) => cambiar('frente', e.target.value)} />
                {err('terreno.frente')}
              </label>
              <label className={ui.campo}>
                <span className={ui.etiqueta}>Fondo (m, opcional)</span>
                <input className={clase('terreno.fondo')} type="number" min="0" step="any" value={form.fondo} onChange={(e) => cambiar('fondo', e.target.value)} />
                {err('terreno.fondo')}
              </label>
              <div className={ui.campo}>
                <span className={ui.etiqueta}>Título de propiedad</span>
                <div className={ui.checks}>
                  <label><input type="checkbox" checked={form.tieneTitulo} onChange={(e) => cambiar('tieneTitulo', e.target.checked)} /> Cuenta con título de propiedad</label>
                </div>
              </div>
            </div>
            {err('terreno')}
          </section>
        )}

        <section className={ui.tarjeta}>
          <h2>Operaciones y precios</h2>
          <EditorOperaciones tipo={form.tipo} filas={operaciones} onCambio={setOperaciones} catalogos={catalogos} errores={errores} />
        </section>

        <section className={ui.tarjeta}>
          <h2>Ubicación</h2>
          <SelectorUbicacion valor={ubicacion} onCambio={setUbicacion} error={errorUbicacion} />
        </section>

        <section className={ui.tarjeta}>
          <h2>Responsable</h2>
          {editaTodas ? (
            <div className={ui.grilla}>
            <label className={ui.campo}>
              <span className={ui.etiqueta}>Usuario responsable</span>
              <select className={ui.input} value={idResponsable} onChange={(e) => setIdResponsable(e.target.value)}>
                <option value="">Sin asignar</option>
                {responsables.filter((u) => u.activo || String(u.id) === idResponsable).map((u) => (
                  <option key={u.id} value={u.id}>{u.nombre} {u.apellido}{u.activo ? '' : ' (desactivado)'}</option>
                ))}
              </select>
              {errores['idResponsable'] && <span className={ui.errorCampo}>{errores['idResponsable']}</span>}
            </label>
            </div>
          ) : (
            <p className={ui.ayuda}>{detalle?.responsable ? detalle.responsable.nombre : esEdicion ? 'Sin asignar' : 'Usted quedará como responsable de esta propiedad.'}</p>
          )}
        </section>
        </fieldset>

        <div className={ui.acciones}>
          <Link to="/propiedades" className={ui.botonSecundario}>Cancelar</Link>
          {!soloLectura && <button type="submit" className={ui.boton} disabled={guardando}>
            {guardando ? 'Guardando…' : esEdicion ? 'Guardar cambios' : 'Crear propiedad'}
          </button>}
        </div>
      </form>

      {esEdicion && detalle && (
        <section className={ui.tarjeta} style={{ marginTop: '1.5rem' }}>
          <h2>Fotos</h2>
          <GaleriaImagenes idPropiedad={detalle.id} imagenes={detalle.imagenes} onActualizado={cargarDetalle} soloLectura={soloLectura} />
        </section>
      )}
    </>
  )
}
