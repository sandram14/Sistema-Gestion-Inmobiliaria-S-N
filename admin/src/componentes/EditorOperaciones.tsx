import type { Catalogos, Operacion, TipoPropiedad } from '../api/tipos'
import ui from '../ui.module.css'

/** Una operación del formulario. El precio es siempre en guaraníes (no hay selector de moneda). */
export interface FilaOperacion {
  operacion: Operacion
  precio: string
}

/**
 * Operaciones con su precio. Las opciones salen de la regla del backend
 * (catalogos.operacionesPorTipo): un Terreno solo admite Venta; una Casa, Venta y/o Alquiler.
 * El backend vuelve a validar la regla al guardar.
 */
export default function EditorOperaciones({
  tipo,
  filas,
  onCambio,
  catalogos,
  errores,
}: {
  tipo: TipoPropiedad
  filas: FilaOperacion[]
  onCambio: (filas: FilaOperacion[]) => void
  catalogos: Catalogos
  errores: Record<string, string>
}) {
  const permitidas = catalogos.operacionesPorTipo[tipo] ?? []
  const usadas = new Set(filas.map((f) => f.operacion))
  const disponibles = permitidas.filter((o) => !usadas.has(o))

  function cambiar(i: number, cambio: Partial<FilaOperacion>) {
    onCambio(filas.map((f, j) => (j === i ? { ...f, ...cambio } : f)))
  }

  return (
    <div>
      {filas.map((f, i) => (
        <div key={i} className={ui.filaOperacion}>
          <label className={ui.campo}>
            <span className={ui.etiqueta}>Operación</span>
            <select className={ui.input} value={f.operacion} aria-label={`Operación ${i + 1}`}
              onChange={(e) => cambiar(i, { operacion: e.target.value as Operacion })}>
              {permitidas
                .filter((o) => o === f.operacion || !usadas.has(o))
                .map((o) => <option key={o} value={o}>{o}</option>)}
            </select>
            {errores[`operaciones[${i}].operacion`] && <span className={ui.errorCampo}>{errores[`operaciones[${i}].operacion`]}</span>}
          </label>
          <label className={ui.campo}>
            <span className={ui.etiqueta}>Precio{f.operacion === 'Alquiler' ? ' mensual' : ''} (Gs.)</span>
            <input className={`${ui.input} ${errores[`operaciones[${i}].precio`] ? ui.inputConError : ''}`} type="number"
              min="0" step="any" value={f.precio} aria-label={`Precio ${i + 1}`}
              onChange={(e) => cambiar(i, { precio: e.target.value })} />
            {errores[`operaciones[${i}].precio`] && <span className={ui.errorCampo}>{errores[`operaciones[${i}].precio`]}</span>}
          </label>
          <button type="button" className={`${ui.botonPeligro} ${ui.botonChico}`} style={{ marginTop: '1.6rem' }}
            onClick={() => onCambio(filas.filter((_, j) => j !== i))} aria-label={`Quitar operación ${i + 1}`}>
            Quitar
          </button>
        </div>
      ))}

      {disponibles.length > 0 && (
        <button type="button" className={ui.botonSecundario}
          onClick={() => onCambio([...filas, { operacion: disponibles[0], precio: '' }])}>
          + Agregar {disponibles.join(' o ')}
        </button>
      )}
      {tipo === 'Terreno' && <p className={ui.ayuda}>Un terreno solamente puede ponerse en venta.</p>}
      {errores['operaciones'] && <p className={ui.errorCampo}>{errores['operaciones']}</p>}
    </div>
  )
}
