import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import BotonVolver from '../componentes/BotonVolver'
import { api } from '../api/cliente'
import type { ClienteDetalle, UsuarioDto } from '../api/tipos'
import { useSesion } from '../auth/Sesion'
import { erroresDe } from '../componentes/errores'
import ui from '../ui.module.css'

/** Alta manual de un cliente potencial que llegó por WhatsApp, llamada u otro medio. */
export default function NuevoClientePotencial() {
  const { puede } = useSesion()
  const navegar = useNavigate()
  const [f, setF] = useState({ nombre: '', telefono: '', email: '', interes: '', codigoPropiedad: '', origen: 'WHATSAPP', idResponsable: '' })
  const [usuarios, setUsuarios] = useState<UsuarioDto[]>([])
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)

  useEffect(() => { if (puede('CLIENTES_ASIGNAR')) api.get<UsuarioDto[]>('/api/admin/usuarios').then(setUsuarios).catch(() => setUsuarios([])) }, [puede])

  async function guardar(e: FormEvent) {
    e.preventDefault(); setCampos({}); setGeneral(null)
    try {
      const r = await api.post<ClienteDetalle>('/api/admin/clientes-potenciales', {
        ...f, telefono: f.telefono || null, email: f.email || null, interes: f.interes || null, codigoPropiedad: f.codigoPropiedad || null,
        idResponsable: f.idResponsable ? Number(f.idResponsable) : null,
      })
      navegar(`/consultas/clientes/${r.id}`, { replace: true, state: { mensaje: 'Cliente potencial registrado.' } })
    } catch (err) { const x = erroresDe(err); setCampos(x.campos); setGeneral(x.general) }
  }

  const err = (c: string) => campos[c] && <span className={ui.errorCampo}>{campos[c]}</span>
  const cambiar = (c: keyof typeof f, v: string) => setF({ ...f, [c]: v })

  return (
    <>
      <div className={ui.tituloPagina}><div className={ui.tituloConVolver}><BotonVolver alternativa="/consultas/clientes" /><h1>Registrar cliente potencial</h1></div></div>
      <p className={ui.ayuda}>Para personas que contactaron directamente a la inmobiliaria. Registre solo los datos necesarios para el seguimiento.</p>
      {general && <div className={ui.avisoError} role="alert">{general}</div>}
      <form onSubmit={guardar} noValidate>
        <section className={ui.tarjeta}>
          <h2>Datos del interesado</h2>
          <div className={ui.grilla}>
            <label className={ui.campo}><span className={ui.etiqueta}>Nombre</span><input className={ui.input} maxLength={120} value={f.nombre} onChange={(e) => cambiar('nombre', e.target.value)} />{err('nombre')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Medio de contacto</span>
              <select className={ui.input} value={f.origen} onChange={(e) => cambiar('origen', e.target.value)}>
                <option value="WHATSAPP">WhatsApp</option><option value="LLAMADA">Llamada</option><option value="OTRO">Otro</option>
              </select>{err('origen')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Teléfono</span><input className={ui.input} maxLength={30} value={f.telefono} onChange={(e) => cambiar('telefono', e.target.value)} />{err('telefono')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Email</span><input className={ui.input} type="email" maxLength={120} value={f.email} onChange={(e) => cambiar('email', e.target.value)} />{err('email')}</label>
            <label className={`${ui.campo} ${ui.campoAncho}`}><span className={ui.etiqueta}>Detalle de su interés (opcional)</span>
              <textarea className={ui.input} rows={3} maxLength={1000} value={f.interes} onChange={(e) => cambiar('interes', e.target.value)} />{err('interes')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Código de la propiedad (opcional)</span>
              <input className={ui.input} placeholder="prop-001" maxLength={20} value={f.codigoPropiedad} onChange={(e) => cambiar('codigoPropiedad', e.target.value)} />{err('codigoPropiedad')}</label>
            {puede('CLIENTES_ASIGNAR') && (
              <label className={ui.campo}><span className={ui.etiqueta}>Responsable</span>
                <select className={ui.input} value={f.idResponsable} onChange={(e) => cambiar('idResponsable', e.target.value)}>
                  <option value="">El de la propiedad (o sin asignar)</option>
                  {usuarios.filter((u) => u.activo).map((u) => <option key={u.id} value={u.id}>{u.nombre} {u.apellido}</option>)}
                </select>{err('idResponsable')}</label>
            )}
          </div>
          {err('contacto') && <div className={ui.avisoError} style={{ marginTop: 12 }}>{campos.contacto}</div>}
          {!puede('CLIENTES_ASIGNAR') && <p className={ui.ayuda}>Usted quedará como responsable del seguimiento.</p>}
          <div className={ui.acciones}><button type="submit" className={ui.boton}>Registrar</button></div>
        </section>
      </form>
    </>
  )
}
