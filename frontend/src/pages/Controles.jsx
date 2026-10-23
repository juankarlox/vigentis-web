import { useState } from 'react'
import { api, descargar, query } from '../api'
import { useAuth } from '../auth'
import {
  AccionesModal, Alerta, Campo, Cargando, Encabezado, Estado, Modal, Selector, Vacio,
  useDatos, useFormulario, useResponsables, useToast,
} from '../components/ui'
import { ESTADOS_CONTROL, TEMAS_CONTROL, etiqueta, numeroONull, opciones } from '../util'

export default function Controles() {
  const { puede } = useAuth()
  const toast = useToast()
  const responsables = useResponsables()
  const [filtros, setFiltros] = useState({ q: '', tema: '', estado: '', aplica: '' })
  const [editando, setEditando] = useState(null)
  const { datos, error, cargando, recargar } = useDatos(() => api(`/controles${query(filtros)}`), [filtros])
  const resumen = useDatos(() => api('/controles/resumen'), [datos])
  const gestionar = puede('CONTROLES_GESTIONAR')
  const r = resumen.datos

  return (
    <>
      <Encabezado titulo="Controles ISO/IEC 27001"
        descripcion="Los 93 controles del Anexo A. Para cada uno se define si aplica, su estado y su responsable: esto forma la Declaración de Aplicabilidad.">
        {puede('REPORTES_VER') && (
          <button className="btn btn-ghost" onClick={() => descargar('/reportes/controles', 'declaracion_aplicabilidad.csv').catch((e) => toast(e.message, 'error'))}>
            Exportar Declaración de Aplicabilidad
          </button>
        )}
      </Encabezado>

      {r && (
        <dl className="stat-strip">
          <div className="stat-item"><dt>Aplican</dt><dd>{r.aplican} <span className="stat-of">de {r.total}</span></dd></div>
          <div className="stat-item"><dt>Implementados</dt><dd>{r.implementados}</dd></div>
          <div className="stat-item"><dt>En progreso</dt><dd>{r.enProgreso}</dd></div>
          <div className="stat-item"><dt>Avance</dt><dd>{r.porcentajeImplementacion}%</dd></div>
          <div className="stat-item"><dt>Sin justificar</dt><dd>{r.sinJustificacion} <span className="stat-of">de {r.total}</span></dd></div>
        </dl>
      )}

      <div className="toolbar">
        <input type="search" placeholder="Buscar por código (A.8.5) o nombre" value={filtros.q}
          onChange={(e) => setFiltros({ ...filtros, q: e.target.value })} />
        <Selector valor={filtros.tema} onCambio={(v) => setFiltros({ ...filtros, tema: v })} vacio="Todos los temas" opciones={opciones(TEMAS_CONTROL)} />
        <Selector valor={filtros.estado} onCambio={(v) => setFiltros({ ...filtros, estado: v })} vacio="Todos los estados" opciones={opciones(ESTADOS_CONTROL)} />
        <Selector valor={filtros.aplica} onCambio={(v) => setFiltros({ ...filtros, aplica: v })} vacio="Aplica y no aplica"
          opciones={[{ valor: 'true', texto: 'Solo los que aplican' }, { valor: 'false', texto: 'Solo excluidos' }]} />
        <span className="spacer" />
        {datos && <span className="count">{datos.length} control(es)</span>}
      </div>

      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <div className="table-wrap">
          {datos?.length === 0 ? <Vacio>No hay controles que coincidan.</Vacio> : (
            <table className="data">
              <thead><tr><th>Control</th><th>Nombre</th><th>Tema</th><th>Aplica</th><th>Estado</th><th>Responsable</th></tr></thead>
              <tbody>
                {datos?.map((c) => (
                  <tr key={c.id} className="clickable" onClick={() => setEditando(c)}>
                    <td><code>{c.codigoIso}</code></td>
                    <td>{c.nombre}{c.justificacion
                      ? <span className="sub">{c.aplica ? 'Aplica' : 'Excluido'}: {c.justificacion}</span>
                      : <span className="sub" style={{ color: 'var(--danger)' }}>Falta justificar</span>}</td>
                    <td>{etiqueta(c.tema)}</td>
                    <td>{c.aplica ? 'Sí' : <span className="pill pill-neutral">No aplica</span>}</td>
                    <td>{c.aplica ? <Estado valor={c.estado} /> : <span className="muted">—</span>}</td>
                    <td>{responsables.nombre(c.responsableId)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {editando && (
        <DetalleControl control={editando} gestionar={gestionar} responsables={responsables}
          onCerrar={() => setEditando(null)}
          onGuardado={(c) => { setEditando(null); toast(`Control ${c.codigoIso} actualizado`); recargar() }} />
      )}
    </>
  )
}

function DetalleControl({ control, gestionar, responsables, onCerrar, onGuardado }) {
  const [v, set] = useFormulario({
    aplica: control.aplica,
    justificacion: control.justificacion || '',
    estado: control.estado,
    responsableId: control.responsableId ? String(control.responsableId) : '',
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const evidencias = useDatos(() => (control.id ? api(`/controles/${control.id}/documentos`) : Promise.resolve([])), [control.id])

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    try {
      const c = await api(`/controles/${control.id}`, {
        method: 'PUT',
        body: { aplica: v.aplica, justificacion: v.justificacion, estado: v.estado, responsableId: numeroONull(v.responsableId) },
      })
      onGuardado(c)
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  return (
    <Modal titulo={`${control.codigoIso} · ${control.nombre}`} onCerrar={onCerrar} ancho="wide">
      <p className="muted" style={{ marginTop: 0 }}>Tema: {etiqueta(control.tema)}</p>
      {gestionar ? (
        <form onSubmit={guardar}>
          <Alerta>{error}</Alerta>
          <label className="check">
            <input type="checkbox" checked={v.aplica} onChange={(e) => set('aplica')(e.target.checked)} />
            Este control aplica a la organización
          </label>
          <Campo etiqueta={v.aplica ? 'Justificación de la inclusión (obligatoria)' : 'Justificación de la exclusión (obligatoria)'}
            ayuda="La Declaración de Aplicabilidad debe justificar tanto los controles incluidos como los excluidos (ISO/IEC 27001, 6.1.3 d).">
            <textarea value={v.justificacion} onChange={set('justificacion')} required maxLength={1000}
              placeholder={v.aplica ? 'Ej.: Reduce el riesgo RSG-001 de acceso no autorizado al correo' : 'Ej.: La organización no desarrolla software propio'} />
          </Campo>
          {v.aplica && (
            <div className="field-row">
              <Campo etiqueta="Estado de implementación"><Selector valor={v.estado} onCambio={set('estado')} opciones={opciones(ESTADOS_CONTROL)} /></Campo>
              <Campo etiqueta="Responsable">
                <Selector valor={v.responsableId} onCambio={set('responsableId')} vacio="Sin asignar"
                  opciones={responsables.lista.map((u) => ({ valor: String(u.id), texto: u.nombre }))} />
              </Campo>
            </div>
          )}
          <AccionesModal enviando={enviando} onCancelar={onCerrar} />
        </form>
      ) : (
        <dl className="kv">
          <dt>Aplica</dt><dd>{control.aplica ? 'Sí' : 'No'}</dd>
          <dt>Justificación</dt><dd>{control.justificacion || '—'}</dd>
          <dt>Estado</dt><dd><Estado valor={control.estado} /></dd>
          <dt>Responsable</dt><dd>{responsables.nombre(control.responsableId)}</dd>
        </dl>
      )}

      <h3 className="section-label" style={{ marginTop: 22 }}>Documentos de evidencia</h3>
      {evidencias.datos?.length === 0
        ? <p className="muted" style={{ fontSize: 13.5 }}>No hay documentos vinculados. Se vinculan desde la sección Documentos.</p>
        : (
          <ul className="timeline">
            {evidencias.datos?.map((d) => (
              <li key={d.id}><code>{d.codigo}</code><span>{d.titulo} · v{d.version} · <Estado valor={d.estado} /></span></li>
            ))}
          </ul>
        )}
    </Modal>
  )
}
