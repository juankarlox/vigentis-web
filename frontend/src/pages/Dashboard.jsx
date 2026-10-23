import { Link } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { Alerta, Cargando, Encabezado, Nivel, useDatos } from '../components/ui'
import { etiqueta, fechaHora, nivelRiesgo, rangosNivel } from '../util'

const COLOR_SEVERIDAD = { CRITICA: '#8E1F1A', ALTA: 'var(--danger)', MEDIA: 'var(--warning)', BAJA: 'var(--success)' }

export default function Dashboard() {
  const { usuario, puede } = useAuth()
  const { datos: d, error, cargando } = useDatos(() => api('/dashboard'))

  if (cargando) return <Cargando />
  if (error) return <Alerta>{error}</Alerta>

  const altos = (d.riesgosPorNivel.CRITICO || 0) + (d.riesgosPorNivel.ALTO || 0)
  const c = d.controles

  return (
    <>
      <Encabezado titulo={`Hola, ${usuario.nombre.split(' ')[0]}`}
        descripcion="Estado actual de la seguridad de la información de la organización." />

      <div className="hero-stat">
        <span className="hero-num" style={altos === 0 ? { color: 'var(--success)' } : undefined}>{altos}</span>
        <span className="hero-label">
          {altos === 1 ? 'riesgo alto o crítico vigente' : 'riesgos altos o críticos vigentes'}
          {puede('RIESGOS_VER') && <><br /><Link to="/riesgos">Ver riesgos</Link></>}
        </span>
      </div>

      <dl className="stat-strip">
        <div className="stat-item"><dt>Activos registrados</dt><dd>{d.activos}</dd></div>
        <div className="stat-item"><dt>Riesgos vigentes</dt><dd>{d.riesgosVigentes}</dd></div>
        <div className="stat-item"><dt>Incidentes abiertos</dt><dd>{d.incidentesAbiertos}</dd></div>
        <div className="stat-item">
          <dt>Controles implementados</dt>
          <dd>{c.porcentajeImplementacion}% <span className="stat-of">de {c.aplican} aplicables</span></dd>
        </div>
      </dl>

      <div className="grid-2" style={{ marginBottom: 22 }}>
        <section className="card">
          <h3>Matriz de riesgos</h3>
          <p>Riesgos vigentes según probabilidad e impacto. Cada número es la cantidad de riesgos en esa celda.</p>
          <MatrizRiesgos matriz={d.matrizRiesgos} criterios={d.criterios} />
        </section>

        <section className="card">
          <h3>Riesgos prioritarios</h3>
          <p>Abiertos o en tratamiento con nivel alto o crítico.</p>
          <div className="alert-row">
            <span className={d.riesgosSobreNivelAceptable ? 'pill pill-none' : 'pill pill-ok'}>
              {d.riesgosSobreNivelAceptable} sobre el nivel aceptable ({d.criterios.nivelAceptable})
            </span>
            <span className={d.riesgosRevisionVencida ? 'pill pill-progress' : 'pill pill-ok'}>
              {d.riesgosRevisionVencida} con revisión vencida
            </span>
          </div>
          {d.riesgosPrioritarios.length === 0
            ? <p className="muted">No hay riesgos prioritarios. </p>
            : (
              <table className="data">
                <tbody>
                  {d.riesgosPrioritarios.map((r) => (
                    <tr key={r.id}>
                      <td><code>{r.codigo}</code></td>
                      <td>{r.nombre}<span className="sub">{r.activo ? r.activo.nombre : 'Sin activo'} · {etiqueta(r.estado)}</span></td>
                      <td className="nowrap"><Nivel nivel={r.nivel} valor={r.nivelValor} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
        </section>
      </div>

      <div className="grid-3" style={{ marginBottom: 34 }}>
        <section className="card">
          <h3>Incidentes abiertos por severidad</h3>
          <p>{d.incidentesUltimos30Dias} detectados en los últimos 30 días.</p>
          <Barras valores={d.incidentesAbiertosPorSeveridad} colores={COLOR_SEVERIDAD} />
        </section>

        <section className="card">
          <h3>Declaración de Aplicabilidad</h3>
          <p>{c.aplican} controles aplican · {c.excluidos} excluidos.</p>
          <div className="progress" role="img"
            aria-label={`${c.implementados} implementados, ${c.enProgreso} en progreso, ${c.noImplementados} sin implementar`}>
            <div style={{ width: `${pct(c.implementados, c.aplican)}%`, background: 'var(--success)' }} title={`Implementados: ${c.implementados}`} />
            <div style={{ width: `${pct(c.enProgreso, c.aplican)}%`, background: 'var(--warning)' }} title={`En progreso: ${c.enProgreso}`} />
          </div>
          <div className="legend">
            <span><i style={{ background: 'var(--success)' }} />Implementados {c.implementados}</span>
            <span><i style={{ background: 'var(--warning)' }} />En progreso {c.enProgreso}</span>
            <span><i style={{ background: 'var(--paper)', border: '1px solid var(--line)' }} />Sin implementar {c.noImplementados}</span>
          </div>
        </section>

        <section className="card">
          <h3>Auditorías y documentos</h3>
          <dl className="kv" style={{ gridTemplateColumns: '1fr auto', margin: 0 }}>
            <dt>Hallazgos abiertos</dt><dd><strong>{d.hallazgosAbiertos}</strong></dd>
            <dt>Hallazgos vencidos</dt>
            <dd><strong style={{ color: d.hallazgosVencidos ? 'var(--danger)' : undefined }}>{d.hallazgosVencidos}</strong></dd>
            <dt>Documentos aprobados</dt><dd><strong>{d.documentosPorEstado.APROBADO}</strong></dd>
            <dt>En borrador</dt><dd><strong>{d.documentosPorEstado.BORRADOR}</strong></dd>
            <dt>Revisión próxima o vencida</dt>
            <dd><strong style={{ color: d.documentosRevisionPendiente ? 'var(--warning)' : undefined }}>{d.documentosRevisionPendiente}</strong></dd>
          </dl>
        </section>
      </div>

      <h2 className="section-label">Actividad reciente</h2>
      {d.actividadReciente.length === 0
        ? <p className="muted">Todavía no hay actividad registrada.</p>
        : (
          <ul className="activity">
            {d.actividadReciente.map((h) => (
              <li key={h.id}>
                <time>{fechaHora(h.fecha)}</time>
                <span>
                  {h.entidadCodigo && <><code>{h.entidadCodigo}</code>{' '}</>}
                  {h.detalle}
                  <span className="muted"> — {h.usuarioNombre || 'Sistema'}</span>
                </span>
              </li>
            ))}
          </ul>
        )}
    </>
  )
}

function pct(parte, total) {
  return total ? Math.round((parte * 100) / total) : 0
}

/** Matriz 5x5: filas = probabilidad (5 arriba), columnas = impacto. */
function MatrizRiesgos({ matriz, criterios }) {
  const rangos = rangosNivel(criterios)
  const filas = [5, 4, 3, 2, 1]
  const columnas = [1, 2, 3, 4, 5]
  return (
    <>
      <div className="matrix" role="table" aria-label="Matriz de riesgos">
        {filas.map((p) => (
          <div key={p} style={{ display: 'contents' }} role="row">
            <div className="axis" role="rowheader">{p}</div>
            {columnas.map((i) => {
              const n = matriz[p - 1][i - 1]
              const nivel = nivelRiesgo(p * i, criterios)
              return (
                <div key={i} role="cell"
                  className={`cell lv-${nivel} ${n ? 'has' : 'empty-cell'}`}
                  title={`Probabilidad ${p} × Impacto ${i} = ${p * i} (${etiqueta(nivel)}): ${n} riesgo(s)`}>
                  {n || ''}
                </div>
              )
            })}
          </div>
        ))}
        <div />
        {columnas.map((i) => <div key={i} className="axis">{i}</div>)}
      </div>
      <div className="matrix-caption"><span>↑ Probabilidad</span><span>Impacto →</span></div>
      <div className="legend">
        {['BAJO', 'MEDIO', 'ALTO', 'CRITICO'].map((n) => (
          <span key={n}><i className={`sw-${n}`} />{etiqueta(n)} ({rangos[n]})</span>
        ))}
      </div>
    </>
  )
}

function Barras({ valores, colores }) {
  const max = Math.max(1, ...Object.values(valores))
  return (
    <div className="bars">
      {Object.entries(valores).map(([clave, n]) => (
        <div key={clave} className="bar-row" title={`${etiqueta(clave)}: ${n}`}>
          <span>{etiqueta(clave)}</span>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${(n * 100) / max}%`, background: colores[clave] }} />
          </div>
          <span className="num">{n}</span>
        </div>
      ))}
    </div>
  )
}
