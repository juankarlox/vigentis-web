import { useState } from 'react'
import { api, descargar, query } from '../api'
import { useAuth } from '../auth'
import {
  AccionesModal, Alerta, Campo, Cargando, Encabezado, Estado, Modal, Pestanas, Selector, Vacio,
  useDatos, useFormulario, useResponsables, useToast,
} from '../components/ui'
import {
  ESTADOS_HALLAZGO, TIPOS_AUDITORIA, TIPOS_HALLAZGO, etiqueta, fecha, hoyInput, numeroONull, opciones, vacioANull,
} from '../util'

export default function Auditorias() {
  const { puede } = useAuth()
  const [pestana, setPestana] = useState('auditorias')
  const gestionar = puede('AUDITORIAS_IMPORTAR')
  const toast = useToast()

  return (
    <>
      <Encabezado titulo="Auditorías"
        descripcion="Resultados de auditorías internas y externas. Los hallazgos se importan desde un archivo CSV y se les da seguimiento hasta cerrarlos (ISO 9.2 y 10.2).">
        {gestionar && (
          <button className="btn btn-ghost" onClick={() => descargar('/auditorias/plantilla', 'plantilla_hallazgos.csv').catch((e) => toast(e.message, 'error'))}>
            Descargar plantilla CSV
          </button>
        )}
      </Encabezado>
      <Pestanas activa={pestana} onCambio={setPestana} opciones={[
        { id: 'auditorias', texto: 'Auditorías' },
        { id: 'hallazgos', texto: 'Todos los hallazgos' },
      ]} />
      {pestana === 'auditorias' ? <ListaAuditorias gestionar={gestionar} /> : <ListaHallazgos gestionar={gestionar} />}
    </>
  )
}

function ListaAuditorias({ gestionar }) {
  const toast = useToast()
  const [filtros, setFiltros] = useState({ q: '', tipo: '' })
  const [modal, setModal] = useState(null)   // 'importar' | 'nueva' | auditoria
  const { datos, error, cargando, recargar } = useDatos(() => api(`/auditorias${query(filtros)}`), [filtros])

  return (
    <>
      <div className="toolbar">
        <input type="search" placeholder="Buscar por código o nombre" value={filtros.q}
          onChange={(e) => setFiltros({ ...filtros, q: e.target.value })} />
        <Selector valor={filtros.tipo} onCambio={(v) => setFiltros({ ...filtros, tipo: v })} vacio="Internas y externas" opciones={opciones(TIPOS_AUDITORIA)} />
        <span className="spacer" />
        {gestionar && <>
          <button className="btn btn-ghost" onClick={() => setModal('nueva')}>Registrar manualmente</button>
          <button className="btn btn-primary" onClick={() => setModal('importar')}>Importar resultados</button>
        </>}
      </div>

      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <div className="table-wrap">
          {datos?.length === 0 ? <Vacio>No hay auditorías registradas. Usa “Importar resultados” para cargar la primera.</Vacio> : (
            <table className="data">
              <thead><tr><th>Código</th><th>Auditoría</th><th>Tipo</th><th>Fecha</th><th>Hallazgos</th><th>Abiertos</th><th>Origen</th></tr></thead>
              <tbody>
                {datos?.map((a) => (
                  <tr key={a.id} className="clickable" onClick={() => setModal(a)}>
                    <td><code>{a.codigo}</code></td>
                    <td>{a.nombre}{a.auditor && <span className="sub">Auditor: {a.auditor}</span>}</td>
                    <td>{etiqueta(a.tipo)}</td>
                    <td className="nowrap">{fecha(a.fechaAuditoria)}</td>
                    <td>{a.totalHallazgos ?? '—'}</td>
                    <td>{a.hallazgosAbiertos ? <strong style={{ color: 'var(--danger)' }}>{a.hallazgosAbiertos}</strong> : 0}</td>
                    <td className="muted">{a.archivoOrigen || 'Manual'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {modal === 'importar' && (
        <Importar onCerrar={() => setModal(null)}
          onImportado={(r) => { setModal(null); toast(`${r.codigo}: ${r.hallazgosImportados} hallazgos importados`); recargar() }} />
      )}
      {modal === 'nueva' && (
        <FormAuditoria auditoria={{}} onCerrar={() => setModal(null)}
          onGuardado={(a) => { setModal(null); toast(`Auditoría ${a.codigo} registrada`); recargar() }} />
      )}
      {modal && typeof modal === 'object' && (
        <DetalleAuditoria auditoria={modal} gestionar={gestionar}
          onCerrar={() => { setModal(null); recargar() }}
          onEliminada={() => { setModal(null); toast('Auditoría eliminada'); recargar() }} />
      )}
    </>
  )
}

function Importar({ onCerrar, onImportado }) {
  const [v, set] = useFormulario({ nombre: '', tipo: 'INTERNA', fecha: hoyInput(), auditor: '' })
  const [archivo, setArchivo] = useState(null)
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function importar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    const form = new FormData()
    form.append('archivo', archivo)
    form.append('nombre', v.nombre)
    form.append('tipo', v.tipo)
    form.append('fecha', v.fecha)
    if (v.auditor) form.append('auditor', v.auditor)
    try {
      onImportado(await api('/auditorias/importar', { method: 'POST', form }))
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  return (
    <Modal titulo="Importar resultados de auditoría" onCerrar={onCerrar} ancho="wide">
      <form onSubmit={importar}>
        <Alerta tipo="info">
          El archivo debe ser CSV (en Excel: Guardar como → CSV UTF-8) con las columnas
          <strong> codigo, tipo, descripcion, control, fecha_limite</strong>. Si alguna fila tiene un error no se importa nada y se indica qué corregir.
        </Alerta>
        <Alerta>{error}</Alerta>
        <Campo etiqueta="Archivo CSV">
          <input type="file" accept=".csv,text/csv" required onChange={(e) => setArchivo(e.target.files[0])} />
        </Campo>
        <Campo etiqueta="Nombre de la auditoría"><input required maxLength={200} value={v.nombre} onChange={set('nombre')} placeholder="Ej.: Auditoría interna 2026" /></Campo>
        <div className="field-row three">
          <Campo etiqueta="Tipo"><Selector valor={v.tipo} onCambio={set('tipo')} opciones={opciones(TIPOS_AUDITORIA)} /></Campo>
          <Campo etiqueta="Fecha"><input type="date" required max={hoyInput()} value={v.fecha} onChange={set('fecha')} /></Campo>
          <Campo etiqueta="Auditor"><input maxLength={150} value={v.auditor} onChange={set('auditor')} /></Campo>
        </div>
        <AccionesModal enviando={enviando} onCancelar={onCerrar} textoGuardar="Importar" />
      </form>
    </Modal>
  )
}

function FormAuditoria({ auditoria, onCerrar, onGuardado }) {
  const nueva = !auditoria.id
  const [v, set] = useFormulario({
    nombre: auditoria.nombre || '', tipo: auditoria.tipo || 'INTERNA',
    fechaAuditoria: auditoria.fechaAuditoria || hoyInput(), auditor: auditoria.auditor || '',
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    try {
      onGuardado(await api(nueva ? '/auditorias' : `/auditorias/${auditoria.id}`, { method: nueva ? 'POST' : 'PUT', body: v }))
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  return (
    <Modal titulo={nueva ? 'Registrar auditoría' : `Editar ${auditoria.codigo}`} onCerrar={onCerrar}>
      <form onSubmit={guardar}>
        <Alerta>{error}</Alerta>
        <Campo etiqueta="Nombre"><input required maxLength={200} value={v.nombre} onChange={set('nombre')} /></Campo>
        <div className="field-row">
          <Campo etiqueta="Tipo"><Selector valor={v.tipo} onCambio={set('tipo')} opciones={opciones(TIPOS_AUDITORIA)} /></Campo>
          <Campo etiqueta="Fecha"><input type="date" required max={hoyInput()} value={v.fechaAuditoria} onChange={set('fechaAuditoria')} /></Campo>
        </div>
        <Campo etiqueta="Auditor"><input maxLength={150} value={v.auditor} onChange={set('auditor')} /></Campo>
        <AccionesModal enviando={enviando} onCancelar={onCerrar} />
      </form>
    </Modal>
  )
}

function DetalleAuditoria({ auditoria, gestionar, onCerrar, onEliminada }) {
  const toast = useToast()
  const responsables = useResponsables()
  const { datos, error, recargar } = useDatos(() => api(`/auditorias/${auditoria.id}/hallazgos`), [auditoria.id])
  const [hallazgo, setHallazgo] = useState(null)

  async function eliminar() {
    if (!window.confirm(`¿Eliminar ${auditoria.codigo} y todos sus hallazgos? Úsalo solo si se importó por error.`)) return
    try {
      await api(`/auditorias/${auditoria.id}`, { method: 'DELETE' })
      onEliminada()
    } catch (e) { toast(e.message, 'error') }
  }

  return (
    <Modal titulo={`${auditoria.codigo} · ${auditoria.nombre}`} onCerrar={onCerrar} ancho="xwide">
      <dl className="kv">
        <dt>Tipo</dt><dd>{etiqueta(auditoria.tipo)}</dd>
        <dt>Fecha</dt><dd>{fecha(auditoria.fechaAuditoria)}</dd>
        <dt>Auditor</dt><dd>{auditoria.auditor || '—'}</dd>
        <dt>Origen</dt><dd>{auditoria.archivoOrigen || 'Registro manual'}</dd>
      </dl>
      {gestionar && (
        <div className="toolbar">
          <button className="btn btn-primary btn-sm" onClick={() => setHallazgo({})}>Agregar hallazgo</button>
          <span className="spacer" />
          <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={eliminar}>Eliminar auditoría</button>
        </div>
      )}
      <Alerta>{error}</Alerta>
      <TablaHallazgos hallazgos={datos} responsables={responsables} onElegir={setHallazgo} />
      {hallazgo && (
        <FormHallazgo hallazgo={hallazgo} auditoriaId={auditoria.id} gestionar={gestionar} responsables={responsables}
          onCerrar={() => setHallazgo(null)}
          onGuardado={(h) => { setHallazgo(null); toast(`Hallazgo ${h.codigo} guardado`); recargar() }} />
      )}
    </Modal>
  )
}

function TablaHallazgos({ hallazgos, responsables, onElegir }) {
  if (!hallazgos) return <Cargando />
  if (hallazgos.length === 0) return <Vacio>No hay hallazgos.</Vacio>
  return (
    <table className="data">
      <thead><tr><th>Código</th><th>Hallazgo</th><th>Tipo</th><th>Control</th><th>Fecha límite</th><th>Estado</th><th>Responsable</th></tr></thead>
      <tbody>
        {hallazgos.map((h) => (
          <tr key={h.id} className="clickable" onClick={() => onElegir(h)}>
            <td><code>{h.codigo}</code></td>
            <td>{h.descripcion}{h.accionCorrectiva && <span className="sub">Acción: {h.accionCorrectiva}</span>}</td>
            <td>{etiqueta(h.tipo)}</td>
            <td>{h.control ? <code>{h.control.codigoIso}</code> : <span className="muted">—</span>}</td>
            <td className="nowrap">
              {fecha(h.fechaLimite)}
              {h.vencido && <span className="sub" style={{ color: 'var(--danger)', fontWeight: 600 }}>Vencido</span>}
            </td>
            <td><Estado valor={h.estado} /></td>
            <td>{responsables.nombre(h.responsableId)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function ListaHallazgos({ gestionar }) {
  const toast = useToast()
  const responsables = useResponsables()
  const [filtros, setFiltros] = useState({ estado: '', tipo: '' })
  const [hallazgo, setHallazgo] = useState(null)
  const { datos, error, recargar } = useDatos(() => api(`/hallazgos${query(filtros)}`), [filtros])

  return (
    <>
      <div className="toolbar">
        <Selector valor={filtros.estado} onCambio={(v) => setFiltros({ ...filtros, estado: v })} vacio="Todos los estados" opciones={opciones(ESTADOS_HALLAZGO)} />
        <Selector valor={filtros.tipo} onCambio={(v) => setFiltros({ ...filtros, tipo: v })} vacio="Todos los tipos" opciones={opciones(TIPOS_HALLAZGO)} />
        <span className="spacer" />
        {datos && <span className="count">{datos.length} hallazgo(s)</span>}
      </div>
      <Alerta>{error}</Alerta>
      <div className="table-wrap"><TablaHallazgos hallazgos={datos} responsables={responsables} onElegir={setHallazgo} /></div>
      {hallazgo && (
        <FormHallazgo hallazgo={hallazgo} auditoriaId={hallazgo.auditoriaId} gestionar={gestionar} responsables={responsables}
          onCerrar={() => setHallazgo(null)}
          onGuardado={(h) => { setHallazgo(null); toast(`Hallazgo ${h.codigo} guardado`); recargar() }} />
      )}
    </>
  )
}

function FormHallazgo({ hallazgo, auditoriaId, gestionar, responsables, onCerrar, onGuardado }) {
  const nuevo = !hallazgo.id
  const controles = useDatos(() => (gestionar ? api('/controles') : Promise.resolve([])), [gestionar])
  const [v, set] = useFormulario({
    codigo: hallazgo.codigo || '',
    tipo: hallazgo.tipo || 'NC_MENOR',
    descripcion: hallazgo.descripcion || '',
    controlId: hallazgo.control ? String(hallazgo.control.id) : '',
    responsableId: hallazgo.responsableId ? String(hallazgo.responsableId) : '',
    estado: hallazgo.estado || 'ABIERTO',
    fechaLimite: hallazgo.fechaLimite || '',
    accionCorrectiva: hallazgo.accionCorrectiva || '',
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const cerrando = v.estado === 'CERRADO'

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    const body = {
      ...v,
      codigo: vacioANull(v.codigo),
      controlId: numeroONull(v.controlId),
      responsableId: numeroONull(v.responsableId),
      fechaLimite: vacioANull(v.fechaLimite),
      accionCorrectiva: vacioANull(v.accionCorrectiva),
    }
    try {
      const h = await api(nuevo ? `/auditorias/${auditoriaId}/hallazgos` : `/hallazgos/${hallazgo.id}`, { method: nuevo ? 'POST' : 'PUT', body })
      onGuardado(h)
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  if (!gestionar) {
    return (
      <Modal titulo={`Hallazgo ${hallazgo.codigo}`} onCerrar={onCerrar}>
        <dl className="kv">
          <dt>Tipo</dt><dd>{etiqueta(hallazgo.tipo)}</dd>
          <dt>Descripción</dt><dd>{hallazgo.descripcion}</dd>
          <dt>Control</dt><dd>{hallazgo.control ? `${hallazgo.control.codigoIso} · ${hallazgo.control.nombre}` : '—'}</dd>
          <dt>Estado</dt><dd><Estado valor={hallazgo.estado} /></dd>
          <dt>Fecha límite</dt><dd>{fecha(hallazgo.fechaLimite)}</dd>
          <dt>Acción correctiva</dt><dd>{hallazgo.accionCorrectiva || '—'}</dd>
        </dl>
      </Modal>
    )
  }

  return (
    <Modal titulo={nuevo ? 'Agregar hallazgo' : `Hallazgo ${hallazgo.codigo}`} onCerrar={onCerrar} ancho="wide">
      <form onSubmit={guardar}>
        <Alerta>{error}</Alerta>
        <div className="field-row three">
          <Campo etiqueta="Código" ayuda={nuevo ? 'Vacío = automático (H-01, H-02…)' : undefined}>
            <input maxLength={20} value={v.codigo} onChange={set('codigo')} disabled={!nuevo} />
          </Campo>
          <Campo etiqueta="Tipo"><Selector valor={v.tipo} onCambio={set('tipo')} opciones={opciones(TIPOS_HALLAZGO)} /></Campo>
          <Campo etiqueta="Estado"><Selector valor={v.estado} onCambio={set('estado')} opciones={opciones(ESTADOS_HALLAZGO)} /></Campo>
        </div>
        <Campo etiqueta="Descripción"><textarea required value={v.descripcion} onChange={set('descripcion')} /></Campo>
        <div className="field-row">
          <Campo etiqueta="Control ISO relacionado">
            <Selector valor={v.controlId} onCambio={set('controlId')} vacio="Ninguno"
              opciones={(controles.datos || []).map((c) => ({ valor: String(c.id), texto: `${c.codigoIso} · ${c.nombre}` }))} />
          </Campo>
          <Campo etiqueta="Responsable">
            <Selector valor={v.responsableId} onCambio={set('responsableId')} vacio="Sin asignar"
              opciones={responsables.lista.map((u) => ({ valor: String(u.id), texto: u.nombre }))} />
          </Campo>
        </div>
        <Campo etiqueta="Fecha límite"><input type="date" value={v.fechaLimite} onChange={set('fechaLimite')} /></Campo>
        <Campo etiqueta={cerrando ? 'Acción correctiva (obligatoria para cerrar)' : 'Acción correctiva'}>
          <textarea value={v.accionCorrectiva} onChange={set('accionCorrectiva')} required={cerrando} />
        </Campo>
        <AccionesModal enviando={enviando} onCancelar={onCerrar} />
      </form>
    </Modal>
  )
}
