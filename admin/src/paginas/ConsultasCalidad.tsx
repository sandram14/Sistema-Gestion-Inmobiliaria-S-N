import { Link } from 'react-router-dom'
import type { ConsultaResumen } from '../api/tipos'
import PestanasConsultas from '../componentes/PestanasConsultas'
import { Ficha, FichaNumero, ListaConsultas, Origen, useAnalisis, type OrigenDato } from '../componentes/fichas'
import { textoFecha } from '../componentes/formato'
import ui from '../ui.module.css'

type Problema = { c: ConsultaResumen; tipo: string; detalle: string; origen: OrigenDato }

/** Calidad de las respuestas del asistente (ADMIN y GERENTE). */
export default function ConsultasCalidad() {
  const { datos: a, error, filtros } = useAnalisis()
  const i = a?.indicadores
  const pct = (n: number) => (i && i.consultas ? `${Math.round((n * 100) / i.consultas)} % del total` : '')

  const problemas: Problema[] = a ? [
    ...a.sinInformacionUtil.map((c) => ({ c, tipo: 'Sin información útil', detalle: 'El asistente indicó no disponer del dato pedido.', origen: 'inferido' as const })),
    ...a.fueraDeAmbito.map((c) => ({ c, tipo: 'Fuera del ámbito', detalle: 'La pregunta no trataba sobre propiedades ni sobre la inmobiliaria.', origen: 'comprobado' as const })),
    ...a.sinServicio.map((c) => ({ c, tipo: 'Fallo de servicio', detalle: 'El asistente no pudo responder (servicio caído, saturado, sin cuota o contenido bloqueado).', origen: 'comprobado' as const })),
  ].sort((x, y) => y.c.fechaHora.localeCompare(x.c.fechaHora)) : []

  return (
    <>
      <PestanasConsultas />
      <p className={ui.ayuda}>Evalúa la utilidad de las respuestas y detecta oportunidades de mejora en el asistente.</p>
      {filtros}
      {error && <div className={ui.avisoError} role="alert">{error}</div>}
      {!a && !error && <p className={ui.cargando}>Calculando…</p>}
      {a && i && i.consultas === 0 && <div className={ui.aviso}>No hay consultas en este periodo.</div>}

      {a && i && i.consultas > 0 && (
        <>
          <div className={ui.gridNumeros}>
            <FichaNumero titulo="Respuestas sin información útil" valor={a.sinInformacionUtil.length} detalle={pct(a.sinInformacionUtil.length)} origen="inferido" />
            <FichaNumero titulo="Fuera del ámbito inmobiliario" valor={a.fueraDeAmbito.length}
              detalle={i.calidad.fueraDeAmbitoNoRegistrado > 0 ? `${i.calidad.fueraDeAmbitoNoRegistrado} consultas anteriores: no registrado` : pct(a.fueraDeAmbito.length)} origen="comprobado" />
            <FichaNumero titulo="Fallos de servicio" valor={a.sinServicio.length} detalle={pct(a.sinServicio.length)} origen="comprobado" />
          </div>

          <section className={ui.tarjeta}>
            <h2>Consultas con problemas de respuesta</h2>
            {problemas.length === 0 ? <p className={ui.secundario}>No hay consultas con problemas en este periodo.</p> : (
              <div className={ui.tablaContenedor}>
                <table className={ui.tabla}>
                  <thead><tr><th>Fecha y hora</th><th>Pregunta (resumen)</th><th>Tipo</th><th>Detalle</th><th></th></tr></thead>
                  <tbody>{problemas.map((p) => (
                    <tr key={`${p.tipo}${p.c.id}`}>
                      <td className={ui.secundario} style={{ whiteSpace: 'nowrap' }}>{textoFecha(p.c.fechaHora)}</td>
                      <td>{p.c.pregunta}</td>
                      <td style={{ whiteSpace: 'nowrap' }}>{p.tipo} <Origen tipo={p.origen} /></td>
                      <td className={ui.secundario}>{p.detalle}</td>
                      <td><Link to={`/consultas/${p.c.id}`} className={`${ui.botonSecundario} ${ui.botonChico}`}>Ver</Link></td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            )}
          </section>

          <div className={ui.gridFichas}>
            <Ficha titulo="Consultas derivadas a un asesor" origen="comprobado"
              nota="El asistente sugirió contactar a un asesor porque faltaba información o el cliente mostró interés concreto.">
              <ListaConsultas filas={a.derivadas} />
            </Ficha>
          </div>
        </>
      )}
    </>
  )
}
