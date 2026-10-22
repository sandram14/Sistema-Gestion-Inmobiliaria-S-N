import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import BotonVolver from '../componentes/BotonVolver'
import { api } from '../api/cliente'
import type { UsuarioConContrasena, UsuarioDto } from '../api/tipos'
import { useSesion } from '../auth/Sesion'
import { erroresDe } from '../componentes/errores'
import ui from '../ui.module.css'

const VACIO = { nombre: '', apellido: '', email: '', cedula: '', telefono: '', rol: 'VENDEDOR' }

/** Alta y edición de usuarios (solo ADMIN). La contraseña temporal se muestra una única vez. */
export default function FormularioUsuario() {
  const { id } = useParams()
  const esEdicion = id !== undefined
  const navegar = useNavigate()
  const { usuario: yo, puede } = useSesion()
  const [usuario, setUsuario] = useState<UsuarioDto | null>(null)
  const [form, setForm] = useState(VACIO)
  // Tras el alta, la pantalla de edición recibe la contraseña temporal por la navegación (se muestra una vez)
  const [temporal, setTemporal] = useState<string | null>((useLocation().state as { temporal?: string } | null)?.temporal ?? null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const esYo = esEdicion && yo?.id === Number(id)

  function cargar(u: UsuarioDto) {
    setUsuario(u)
    setForm({ nombre: u.nombre, apellido: u.apellido, email: u.email, cedula: u.cedula ?? '', telefono: u.telefono ?? '', rol: u.rol })
  }

  useEffect(() => {
    if (esEdicion) api.get<UsuarioDto>(`/api/admin/usuarios/${id}`).then(cargar).catch((e) => setGeneral(erroresDe(e).general))
  }, [id, esEdicion])

  async function ejecutar(accion: () => Promise<void>) {
    setCampos({})
    setGeneral(null)
    setMensaje(null)
    try {
      await accion()
    } catch (err) {
      const r = erroresDe(err)
      setCampos(r.campos)
      setGeneral(r.general)
    }
  }

  function guardar(e: FormEvent) {
    e.preventDefault()
    void ejecutar(async () => {
      if (esEdicion) {
        cargar(await api.put<UsuarioDto>(`/api/admin/usuarios/${id}`, form))
        setMensaje('Cambios guardados.')
      } else {
        const r = await api.post<UsuarioConContrasena>('/api/admin/usuarios', form)
        navegar(`/usuarios/${r.usuario.id}`, { replace: true, state: { temporal: r.contrasenaTemporal } })
      }
    })
  }

  const cambiarEstado = (activo: boolean) => ejecutar(async () => {
    if (!activo && !window.confirm(`¿Desactivar la cuenta de ${usuario?.nombre} ${usuario?.apellido}? No podrá ingresar al panel hasta que se reactive.`)) return
    cargar(await api.patch<UsuarioDto>(`/api/admin/usuarios/${id}/estado`, { activo }))
    setMensaje(activo ? 'Cuenta reactivada.' : 'Cuenta desactivada.')
  })

  const restablecer = () => ejecutar(async () => {
    if (!window.confirm('¿Generar una nueva contraseña temporal? La actual dejará de funcionar y se cerrarán las sesiones abiertas de este usuario.')) return
    const r = await api.post<UsuarioConContrasena>(`/api/admin/usuarios/${id}/contrasena-temporal`)
    cargar(r.usuario)
    setTemporal(r.contrasenaTemporal)
  })

  if (!puede('USUARIOS_GESTIONAR')) return <div className={ui.avisoError}>No tiene permisos para gestionar usuarios.</div>
  const err = (c: string) => campos[c] && <span className={ui.errorCampo}>{campos[c]}</span>
  const cambiar = (c: keyof typeof VACIO, v: string) => setForm({ ...form, [c]: v })

  return (
    <>
      <div className={ui.tituloPagina}>
        <div className={ui.tituloConVolver}><BotonVolver alternativa="/usuarios" /><h1>{esEdicion ? `${usuario?.nombre ?? ''} ${usuario?.apellido ?? ''}` : 'Nuevo usuario'}</h1></div>
      </div>

      {temporal && (
        <div className={ui.aviso} role="status">
          <strong>Contraseña temporal: </strong><code className={ui.codigo}>{temporal}</code>
          <div className={ui.ayuda}>Entréguela al usuario por un medio seguro. Se muestra una sola vez; deberá cambiarla al ingresar.</div>
        </div>
      )}
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}

      <form onSubmit={guardar} noValidate>
        <section className={ui.tarjeta}>
          <h2>Datos</h2>
          <div className={ui.grilla}>
            <label className={ui.campo}><span className={ui.etiqueta}>Nombre</span>
              <input className={ui.input} maxLength={60} value={form.nombre} onChange={(e) => cambiar('nombre', e.target.value)} />{err('nombre')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Apellido</span>
              <input className={ui.input} maxLength={60} value={form.apellido} onChange={(e) => cambiar('apellido', e.target.value)} />{err('apellido')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Email</span>
              <input className={ui.input} type="email" maxLength={120} value={form.email} onChange={(e) => cambiar('email', e.target.value)} />{err('email')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Rol</span>
              <select className={ui.input} value={form.rol} disabled={esYo} onChange={(e) => cambiar('rol', e.target.value)}>
                <option value="ADMIN">Administrador</option>
                <option value="GERENTE">Gerente</option>
                <option value="VENDEDOR">Vendedor</option>
              </select>
              {esYo && <span className={ui.ayuda}>No puede cambiar su propio rol.</span>}{err('rol')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Cédula (opcional)</span>
              <input className={ui.input} maxLength={20} value={form.cedula} onChange={(e) => cambiar('cedula', e.target.value)} />{err('cedula')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Teléfono (opcional)</span>
              <input className={ui.input} maxLength={30} value={form.telefono} onChange={(e) => cambiar('telefono', e.target.value)} />{err('telefono')}</label>
          </div>
          <div className={ui.acciones}><button type="submit" className={ui.boton}>{esEdicion ? 'Guardar cambios' : 'Crear usuario'}</button></div>
        </section>
      </form>

      {esEdicion && usuario && !esYo && (
        <section className={ui.tarjeta}>
          <h2>Acceso</h2>
          <dl className={ui.datos}>
            <dt>Estado</dt><dd>{usuario.activo ? 'Activo' : 'Desactivado'}</dd>
            <dt>Propiedades asignadas</dt><dd>{usuario.propiedadesAsignadas}</dd>
          </dl>
          <div className={ui.acciones}>
            <button type="button" className={ui.botonSecundario} onClick={() => void restablecer()}>Generar contraseña temporal</button>
            {usuario.activo
              ? <button type="button" className={ui.botonPeligro} onClick={() => void cambiarEstado(false)}>Desactivar cuenta</button>
              : <button type="button" className={ui.botonSecundario} onClick={() => void cambiarEstado(true)}>Reactivar cuenta</button>}
          </div>
        </section>
      )}
    </>
  )
}
