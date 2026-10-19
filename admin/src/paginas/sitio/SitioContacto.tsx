import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import type { ContenidoContacto } from '../../api/tipos'
import { erroresDe } from '../../componentes/errores'
import { PestanasSitio } from '../../componentes/sitio'
import ui from '../../ui.module.css'

/** Mismo criterio que el backend: dominio .example o número terminado en 000000000. */
const esProvisional = (c: ContenidoContacto) =>
  c.email.endsWith('.example') || c.telefono.replace(/\D/g, '').endsWith('000000000') || c.whatsapp.endsWith('000000000')

export default function SitioContacto() {
  const [c, setC] = useState<ContenidoContacto | null>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  useEffect(() => { api.get<ContenidoContacto>('/api/admin/sitio/secciones/contacto').then(setC).catch((e) => setGeneral(erroresDe(e).general)) }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault(); setCampos({}); setGeneral(null); setMensaje(null)
    try { setC(await api.put<ContenidoContacto>('/api/admin/sitio/secciones/contacto', c)); setMensaje('Datos de contacto publicados.') }
    catch (err) { const r = erroresDe(err); setCampos(r.campos); setGeneral(r.general) }
  }

  const campo = (k: keyof ContenidoContacto, etiqueta: string, max: number, ayuda?: string, ancho = false) => (
    <label className={`${ui.campo} ${ancho ? ui.campoAncho : ''}`}><span className={ui.etiqueta}>{etiqueta}</span>
      <input className={ui.input} maxLength={max} value={c?.[k] ?? ''} onChange={(e) => c && setC({ ...c, [k]: e.target.value })} />
      {ayuda && <span className={ui.ayuda}>{ayuda}</span>}
      {campos[k] && <span className={ui.errorCampo}>{campos[k]}</span>}</label>
  )

  return (
    <>
      <PestanasSitio />
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}
      {c && esProvisional(c) && (
        <div className={ui.aviso}>Hay datos de contacto <strong>provisionales</strong> (de prueba). Reemplácelos por los datos reales de la inmobiliaria antes de publicar el sitio.</div>
      )}
      {!c ? (!general && <p className={ui.cargando}>Cargando…</p>) : (
        <form onSubmit={guardar} noValidate>
          <section className={ui.tarjeta}>
            <h2>Empresa</h2>
            <div className={ui.grilla}>
              {campo('nombreEmpresa', 'Nombre comercial', 120)}
              {campo('ciudad', 'Ciudad', 100)}
              {campo('direccion', 'Dirección', 200, undefined, true)}
              <label className={`${ui.campo} ${ui.campoAncho}`}><span className={ui.etiqueta}>Descripción breve</span>
                <textarea className={ui.input} rows={2} maxLength={400} value={c.descripcion} onChange={(e) => setC({ ...c, descripcion: e.target.value })} />
                {campos.descripcion && <span className={ui.errorCampo}>{campos.descripcion}</span>}</label>
            </div>
          </section>
          <section className={ui.tarjeta}>
            <h2>Contacto</h2>
            <div className={ui.grilla}>
              {campo('email', 'Email', 120)}
              {campo('telefono', 'Teléfono', 30)}
              {campo('whatsapp', 'WhatsApp', 20, 'Formato internacional, por ejemplo 595981123456')}
            </div>
          </section>
          <section className={ui.tarjeta}>
            <h2>Horario de atención</h2>
            <div className={ui.grilla}>{campo('horario', 'Horario', 200, 'Por ejemplo: Lunes a viernes de 8:00 a 18:00', true)}</div>
          </section>
          <div className={ui.acciones}><button type="submit" className={ui.boton}>Guardar y publicar</button></div>
        </form>
      )}
    </>
  )
}
