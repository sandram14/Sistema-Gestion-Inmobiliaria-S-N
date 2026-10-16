import PestanasConsultas from '../componentes/PestanasConsultas'
import { Barras, Ficha, FichaNumero, deConteo, dePropiedades, useAnalisis } from '../componentes/fichas'
import ui from '../ui.module.css'

/** Análisis técnico de las consultas (ADMIN y GERENTE). Solo datos guardados; lo inferido se indica. */
export default function ConsultasAnalisis() {
  const { datos: a, error, filtros } = useAnalisis()
  const i = a?.indicadores
  const pct = (n: number) => (i && i.consultas ? ` (${Math.round((n * 100) / i.consultas)} %)` : '')

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
            <FichaNumero titulo="Consultas" valor={i.consultas} detalle="Preguntas recibidas por el asistente." origen="comprobado" />
            <FichaNumero titulo="Conversaciones (visitantes anónimos)" valor={i.conversaciones} detalle="Visitantes distintos que usaron el asistente." origen="comprobado" />
            <FichaNumero titulo="Iniciadas desde una propiedad" valor={<>{i.conPropiedad}<span className={ui.secundario}>{pct(i.conPropiedad)}</span></>}
              detalle="Consultas hechas desde el detalle de una propiedad." origen="comprobado" />
            <FichaNumero titulo="Sin servicio" valor={<>{i.sinServicio}<span className={ui.secundario}>{pct(i.sinServicio)}</span></>}
              detalle="El asistente no pudo responder." origen="comprobado" />
          </div>
          <div className={ui.gridFichas}>
            <Ficha titulo="Propiedades más consultadas" nota="Consultas iniciadas desde el detalle de cada propiedad." origen="comprobado">
              <Barras filas={dePropiedades(i.propiedadesConsultadas)} />
            </Ficha>
            <Ficha titulo="Zonas más consultadas" nota="Ciudad de la propiedad desde la que se inició la consulta." origen="comprobado">
              <Barras filas={deConteo(i.zonasComprobadas)} />
            </Ficha>
            <Ficha titulo="Rangos de precios (propiedades consultadas)" nota="Precio de cada operación de la propiedad consultada, en guaraníes." origen="comprobado">
              <Barras filas={i.rangosPrecio.map((r) => ({ clave: `${r.operacion}${r.rango}`, etiqueta: `${r.operacion}: ${r.rango}`, cantidad: r.cantidad }))} />
            </Ficha>
            <Ficha titulo="Características más solicitadas" nota="Características mencionadas en las preguntas (cochera, patio, piscina…)." origen="inferido">
              <Barras filas={deConteo(i.caracteristicasInferidas)} />
            </Ficha>
            <Ficha titulo="Temas consultados" nota="Clasificación por palabras clave. Una pregunta puede tener varios temas." origen="inferido">
              <Barras filas={deConteo(i.temasInferidos)} />
            </Ficha>
            <Ficha titulo="Tipos de las propiedades consultadas" nota="Tipo de la propiedad desde la que se inició la consulta." origen="comprobado">
              <Barras filas={deConteo(i.tiposComprobados)} />
            </Ficha>
            <Ficha titulo="Tipos y zonas mencionados en preguntas generales" nota="Coincidencias del texto con los tipos y las zonas del catálogo." origen="inferido">
              <Barras filas={deConteo(i.tiposInferidos)} vacio="No se mencionaron tipos en este periodo." />
              <div className={ui.separadorFicha} />
              <Barras filas={deConteo(i.zonasInferidas)} vacio="No se mencionaron zonas en este periodo." />
            </Ficha>
            <Ficha titulo="Propiedades más recomendadas por el asistente" nota="Solo consultas con recomendaciones registradas." origen="comprobado">
              <Barras filas={dePropiedades(i.masRecomendadas)} />
            </Ficha>
            <Ficha titulo="Preguntas repetidas" nota="Preguntas idénticas (sin distinguir mayúsculas, acentos ni signos) que aparecen dos o más veces." origen="comprobado">
              <Barras filas={deConteo(i.preguntasRepetidas)} vacio="No hay preguntas repetidas en este periodo." />
            </Ficha>
          </div>
          <p className={ui.pieTabla}>Con resultado y recomendadas registrados: {i.conRegistroCompleto} de {i.consultas}. Las consultas anteriores a la actualización del sistema no tienen estos datos.</p>
        </>
      )}
    </>
  )
}
