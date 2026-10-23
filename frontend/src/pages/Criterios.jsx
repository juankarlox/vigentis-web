import { useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'
import { Alerta, Campo, Cargando, Encabezado, useToast } from '../components/ui'
import { etiqueta, fechaHora, nivelRiesgo, rangosNivel } from '../util'

const NIVELES = ['BAJO', 'MEDIO', 'ALTO', 'CRITICO']

export default function Criterios() {
  const { criterios, cargarCriterios, puede } = useAuth()
  const editable = puede('CRITERIOS_GESTIONAR')
  if (!criterios) return <Cargando />
  return <FormCriterios key={criterios.actualizadoEn} inicial={criterios} editable={editable} onGuardado={cargarCriterios} />
}

function FormCriterios({ inicial, editable, onGuardado }) {
  const toast = useToast()
  const [v, setV] = useState({
    umbralMedio: inicial.umbralMedio,
    umbralAlto: inicial.umbralAlto,
    umbralCritico: inicial.umbralCritico,
    nivelAceptable: inicial.nivelAceptable,
    mesesRevision: inicial.mesesRevision,
    escalaProbabilidad: completar(inicial.escalaProbabilidad),
    escalaImpacto: completar(inicial.escalaImpacto),
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const numero = (campo) => (e) => setV({ ...v, [campo]: Number(e.target.value) })
  const escala = (campo, i) => (e) => {
    const lista = [...v[campo]]
    lista[i] = e.target.value
    setV({ ...v, [campo]: lista })
  }
  const ordenOk = v.umbralMedio < v.umbralAlto && v.umbralAlto < v.umbralCritico

  async function guardar(e) {
    e.preventDefault()
    setError('')
    if (!ordenOk) { setError('Los umbrales deben ir en orden: medio < alto < crítico'); return }
    setEnviando(true)
    try {
      await api('/criterios-riesgo', { method: 'PUT', body: v })
      toast('Criterios guardados. Todos los riesgos se clasifican con los nuevos umbrales.')
      onGuardado()
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  const rangos = ordenOk ? rangosNivel(v) : null

  return (
    <>
      <Encabezado titulo="Criterios de riesgo"
        descripcion="La organización define cómo se valora la probabilidad y el impacto, desde qué nivel un riesgo es medio, alto o crítico, y hasta qué nivel lo acepta (ISO/IEC 27001, 6.1.2 a).">
        {inicial.actualizadoPorNombre && (
          <p className="hint-inline">Última modificación: {inicial.actualizadoPorNombre}, {fechaHora(inicial.actualizadoEn)}</p>
        )}
      </Encabezado>

      {!editable && <Alerta tipo="info">Solo los roles con el permiso “Definir los criterios de riesgo” pueden modificarlos.</Alerta>}

      <form onSubmit={guardar}>
        <Alerta>{error}</Alerta>
        <div className="grid-2" style={{ marginBottom: 22 }}>
          <section className="card">
            <h3>Clasificación del nivel</h3>
            <p>Nivel = probabilidad × impacto (de 1 a 25). Indica desde qué valor empieza cada categoría.</p>
            <div className="field-row three">
              <Campo etiqueta="Medio desde">
                <input type="number" min={2} max={23} required disabled={!editable} value={v.umbralMedio} onChange={numero('umbralMedio')} />
              </Campo>
              <Campo etiqueta="Alto desde">
                <input type="number" min={3} max={24} required disabled={!editable} value={v.umbralAlto} onChange={numero('umbralAlto')} />
              </Campo>
              <Campo etiqueta="Crítico desde">
                <input type="number" min={4} max={25} required disabled={!editable} value={v.umbralCritico} onChange={numero('umbralCritico')} />
              </Campo>
            </div>
            {rangos && (
              <div className="legend" style={{ marginTop: 0, marginBottom: 18 }}>
                {NIVELES.map((n) => <span key={n}><i className={`sw-${n}`} />{etiqueta(n)} ({rangos[n]})</span>)}
              </div>
            )}
            <div className="field-row">
              <Campo etiqueta="Nivel aceptable (máximo)"
                ayuda="Un riesgo con nivel actual mayor a este solo se acepta con justificación (aceptación excepcional).">
                <input type="number" min={1} max={25} required disabled={!editable} value={v.nivelAceptable} onChange={numero('nivelAceptable')} />
              </Campo>
              <Campo etiqueta="Revisar los riesgos cada (meses)" ayuda="Define la próxima fecha de revisión de cada riesgo (ISO 8.2).">
                <input type="number" min={1} max={36} required disabled={!editable} value={v.mesesRevision} onChange={numero('mesesRevision')} />
              </Campo>
            </div>
          </section>

          <section className="card">
            <h3>Vista previa de la matriz</h3>
            <p>Así quedan clasificadas las 25 combinaciones. Las celdas con borde están dentro del nivel aceptable.</p>
            {ordenOk ? <MatrizPrevia c={v} /> : <Alerta>Corrige el orden de los umbrales para ver la matriz.</Alerta>}
          </section>
        </div>

        <div className="grid-2" style={{ marginBottom: 22 }}>
          <EscalaCard titulo="Escala de probabilidad" descripcion="Qué significa cada valor al evaluar qué tan probable es el riesgo."
            valores={v.escalaProbabilidad} onCambio={(i) => escala('escalaProbabilidad', i)} editable={editable} />
          <EscalaCard titulo="Escala de impacto" descripcion="Qué significa cada valor al evaluar las consecuencias si ocurre."
            valores={v.escalaImpacto} onCambio={(i) => escala('escalaImpacto', i)} editable={editable} />
        </div>

        {editable && (
          <div className="modal-actions">
            <button className="btn btn-primary" disabled={enviando}>{enviando ? 'Guardando…' : 'Guardar criterios'}</button>
          </div>
        )}
      </form>
    </>
  )
}

function completar(lista) {
  const l = [...(lista || [])]
  while (l.length < 5) l.push('')
  return l.slice(0, 5)
}

function EscalaCard({ titulo, descripcion, valores, onCambio, editable }) {
  return (
    <section className="card">
      <h3>{titulo}</h3>
      <p>{descripcion}</p>
      {valores.map((texto, i) => (
        <label key={i} className="field" style={{ flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 10 }}>
          <span style={{ width: 18, fontFamily: 'var(--font-mono)', color: 'var(--emphasis)', fontWeight: 600 }}>{i + 1}</span>
          <input style={{ flex: 1 }} maxLength={200} required disabled={!editable} value={texto} onChange={onCambio(i)} />
        </label>
      ))}
    </section>
  )
}

function MatrizPrevia({ c }) {
  const filas = [5, 4, 3, 2, 1]
  const columnas = [1, 2, 3, 4, 5]
  return (
    <>
      <div className="matrix" role="table" aria-label="Vista previa de la matriz de riesgos">
        {filas.map((p) => (
          <div key={p} style={{ display: 'contents' }} role="row">
            <div className="axis" role="rowheader">{p}</div>
            {columnas.map((i) => {
              const valor = p * i
              const nivel = nivelRiesgo(valor, c)
              return (
                <div key={i} role="cell" className={`cell lv-${nivel} ${valor <= c.nivelAceptable ? 'has' : ''}`}
                  title={`P${p} × I${i} = ${valor}: ${etiqueta(nivel)}${valor <= c.nivelAceptable ? ' (aceptable)' : ''}`}
                  style={{ fontSize: 13, fontWeight: 500 }}>
                  {valor}
                </div>
              )
            })}
          </div>
        ))}
        <div />
        {columnas.map((i) => <div key={i} className="axis">{i}</div>)}
      </div>
      <div className="matrix-caption"><span>↑ Probabilidad</span><span>Impacto →</span></div>
    </>
  )
}
