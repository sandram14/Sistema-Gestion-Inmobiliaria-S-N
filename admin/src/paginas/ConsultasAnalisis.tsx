import PestanasConsultas from '../componentes/PestanasConsultas'
import { Ficha, FichaNumero, Medidor, Torta, deConteo, dePropiedades, useAnalisis } from '../componentes/fichas'
import ui from '../ui.module.css'

/**
 * Análisis técnico de las consultas (ADMIN y GERENTE), en gráficos circulares con porcentajes: preguntas recibidas,
 * conversaciones, sin servicio, propiedades y zonas más consultadas, rangos de precios y temas. Solo datos guardados;
 * lo inferido se indica.
 */
export default function ConsultasAnalisis() {
  const { datos: a, error, filtros } = useAnalisis()
  const i = a?.indicadores

  return (
    <>
      <PestanasConsultas />
      {filtros}
      {error && <div className={ui.avisoError} role="alert">{error}</div>}
      {!a && !error && <p className={ui.cargando}>Calculando…</p>}
      {a && i && i.consultas === 0 && <div className={ui.aviso}>No hay consultas en este periodo.</div>}

      {a && i && i.consultas > 0 && (
        <>
          <div className={ui.gridNumeros}>
            <FichaNumero titulo="Preguntas recibidas por el asistente" valor={i.consultas} origen="comprobado" />
            <FichaNumero titulo="Conversaciones" valor={i.conversaciones} origen="comprobado" />
          </div>
          <div className={ui.gridFichas}>
            <Ficha titulo="Sin servicio" origen="comprobado">
              <Medidor valor={i.sinServicio} total={i.consultas} etiqueta="Sin servicio" resto="Respondidas" />
            </Ficha>
            <Ficha titulo="Propiedades más consultadas" origen="comprobado">
              <Torta titulo="Propiedades más consultadas" filas={dePropiedades(i.propiedadesConsultadas)} />
            </Ficha>
            <Ficha titulo="Zonas más consultadas" origen="comprobado">
              <Torta titulo="Zonas más consultadas" filas={deConteo(i.zonasComprobadas)} />
            </Ficha>
            <Ficha titulo="Rangos de precios" origen="comprobado">
              <Torta titulo="Rangos de precios" filas={i.rangosPrecio.map((r) => {
                const nombre = `${r.operacion}: ${r.rango}`
                return { clave: `${r.operacion}${r.rango}`, etiqueta: nombre, texto: nombre, cantidad: r.cantidad }
              })} />
            </Ficha>
            <Ficha titulo="Temas consultados" origen="inferido">
              <Torta titulo="Temas consultados" filas={deConteo(i.temasInferidos)} />
            </Ficha>
          </div>
        </>
      )}
    </>
  )
}
