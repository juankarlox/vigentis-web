import { useState } from 'react'
import { api, descargar, query } from '../api'
import { useAuth } from '../auth'
import {
  AccionesModal, Alerta, Campo, Cargando, Encabezado, Estado, Modal, Selector, Vacio,
  useDatos, useFormulario, useResponsables, useToast,
} from '../components/ui'
import { ESTADOS_DOCUMENTO, TIPOS_DOCUMENTO, etiqueta, fecha, fechaHora, numeroONull, opciones, tamano, vacioANull } from '../util'

export default function Documentos() {
  const { puede } = useAuth()
  const toast = useToast()
  const responsables = useResponsables()
  const [filtros, setFiltros] = useState({ q: '', tipo: '', estado: '' })
  const [modal, setModal] = useState(null)   // 'nuevo' | documento
  const { datos, error, cargando, recargar } = useDatos(() => api(`/documentos${query(filtros)}`), [filtros])
  const gestionar = puede('DOCUMENTOS_GESTIONAR')

  return (
    <>
      <Encabezado titulo="Documentos"
        descripcion="Políticas, procedimientos, registros y evidencias del SGSI, con versión, aprobación y fecha de revisión (ISO 7.5).">
        {gestionar && <button className="btn btn-primary" onClick={() => setModal('nuevo')}>Nuevo documento</button>}
      </Encabezado>

      <div className="toolbar">
        <input type="search" placeholder="Buscar por código o título" value={filtros.q}
          onChange={(e) => setFiltros({ ...filtros, q: e.target.value })} />
        <Selector valor={filtros.tipo} onCambio={(v) => setFiltros({ ...filtros, tipo: v })} vacio="Todos los tipos" opciones={opciones(TIPOS_DOCUMENTO)} />
        <Selector valor={filtros.estado} onCambio={(v) => setFiltros({ ...filtros, estado: v })} vacio="Todos los estados" opciones={opciones(ESTADOS_DOCUMENTO)} />
        <span className="spacer" />
        {datos && <span className="count">{datos.length} documento(s)</span>}
      </div>

      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <div className="table-wrap">
          {datos?.length === 0 ? <Vacio>No hay documentos que coincidan.</Vacio> : (
            <table className="data">
              <thead><tr><th>Código</th><th>Título</th><th>Tipo</th><th>Versión</th><th>Estado</th><th>Próxima revisión</th><th>Archivo</th></tr></thead>
              <tbody>
                {datos?.map((d) => (
                  <tr key={d.id} className="clickable" onClick={() => setModal(d)}>
                    <td><code>{d.codigo}</code></td>
                    <td>{d.titulo}<span className="sub">{responsables.nombre(d.responsableId)}</span></td>
                    <td>{etiqueta(d.tipo)}</td>
                    <td>v{d.version}</td>
                    <td><Estado valor={d.estado} /></td>
                    <td className="nowrap">
                      {fecha(d.fechaRevision)}
                      {d.revisionPendiente && <span className="sub" style={{ color: 'var(--warning)', fontWeight: 600 }}>Revisar pronto</span>}
                    </td>
                    <td>{d.tieneArchivo ? <span title={d.archivoNombre}>{tamano(d.archivoTamano)}</span> : <span className="muted">Sin archivo</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {modal === 'nuevo' && (
        <Modal titulo="Nuevo documento" onCerrar={() => setModal(null)}>
          <FormDocumento documento={{}} responsables={responsables.lista} onCancelar={() => setModal(null)}
            onGuardado={(d) => { toast(`Documento ${d.codigo} creado. Ahora sube su archivo.`); recargar(); setModal(d) }} />
        </Modal>
      )}
      {modal && typeof modal === 'object' && (
        <DetalleDocumento documento={modal} responsables={responsables}
          onCerrar={() => setModal(null)}
          onCambio={(d) => { setModal(d); recargar() }} />
      )}
    </>
  )
}

function FormDocumento({ documento, responsables, onCancelar, onGuardado }) {
  const nuevo = !documento.id
  const [v, set] = useFormulario({
    titulo: documento.titulo || '',
    tipo: documento.tipo || 'POLITICA',
    version: documento.version || '1.0',
    estado: documento.estado === 'APROBADO' ? 'APROBADO' : (documento.estado || 'BORRADOR'),
    responsableId: documento.responsableId ? String(documento.responsableId) : '',
    fechaRevision: documento.fechaRevision || '',
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    const body = { ...v, responsableId: numeroONull(v.responsableId), fechaRevision: vacioANull(v.fechaRevision), estado: nuevo ? null : v.estado }
    try {
      onGuardado(await api(nuevo ? '/documentos' : `/documentos/${documento.id}`, { method: nuevo ? 'POST' : 'PUT', body }))
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  const estados = documento.estado === 'APROBADO' ? ['APROBADO', 'BORRADOR', 'OBSOLETO'] : ['BORRADOR', 'OBSOLETO']
  return (
    <form onSubmit={guardar}>
      <Alerta>{error}</Alerta>
      <Campo etiqueta="Título"><input required maxLength={200} value={v.titulo} onChange={set('titulo')} /></Campo>
      <div className="field-row">
        <Campo etiqueta="Tipo"><Selector valor={v.tipo} onCambio={set('tipo')} opciones={opciones(TIPOS_DOCUMENTO)} /></Campo>
        <Campo etiqueta="Versión"><input maxLength={10} value={v.version} onChange={set('version')} /></Campo>
      </div>
      <div className="field-row">
        <Campo etiqueta="Responsable">
          <Selector valor={v.responsableId} onCambio={set('responsableId')} vacio="Sin asignar"
            opciones={responsables.map((u) => ({ valor: String(u.id), texto: u.nombre }))} />
        </Campo>
        <Campo etiqueta="Próxima revisión" ayuda="Si se deja vacía, al aprobar se pone a un año.">
          <input type="date" value={v.fechaRevision} onChange={set('fechaRevision')} />
        </Campo>
      </div>
      {!nuevo && (
        <Campo etiqueta="Estado" ayuda="Para aprobar usa el botón Aprobar.">
          <Selector valor={v.estado} onCambio={set('estado')} opciones={opciones(estados)} />
        </Campo>
      )}
      <AccionesModal enviando={enviando} onCancelar={onCancelar} textoGuardar={nuevo ? 'Crear documento' : 'Guardar cambios'} />
    </form>
  )
}

function DetalleDocumento({ documento: d, responsables, onCerrar, onCambio }) {
  const { puede } = useAuth()
  const toast = useToast()
  const gestionar = puede('DOCUMENTOS_GESTIONAR')
  const aprobar = puede('DOCUMENTOS_APROBAR')
  const [editando, setEditando] = useState(false)
  const [subiendo, setSubiendo] = useState(false)
  const [error, setError] = useState('')
  const controles = useDatos(() => api(`/documentos/${d.id}/controles`), [d.id])
  const catalogo = useDatos(() => (gestionar ? api('/controles?aplica=true') : Promise.resolve([])), [gestionar])
  const [seleccion, setSeleccion] = useState('')

  async function subir(e) {
    const archivo = e.target.files[0]
    if (!archivo) return
    setSubiendo(true)
    setError('')
    const form = new FormData()
    form.append('archivo', archivo)
    try {
      const nuevo = await api(`/documentos/${d.id}/archivo`, { method: 'POST', form })
      toast(nuevo.version !== d.version ? `Nueva versión ${nuevo.version}: debe aprobarse otra vez` : 'Archivo cargado')
      onCambio(nuevo)
    } catch (err) { setError(err.message) } finally { setSubiendo(false); e.target.value = '' }
  }

  async function aprobarDocumento() {
    setError('')
    try {
      const nuevo = await api(`/documentos/${d.id}/aprobar`, { method: 'POST' })
      toast(`Documento ${nuevo.codigo} aprobado`)
      onCambio(nuevo)
    } catch (err) { setError(err.message) }
  }

  async function vincular() {
    try {
      await api(`/documentos/${d.id}/controles/${seleccion}`, { method: 'POST' })
      setSeleccion('')
      controles.recargar()
    } catch (e) { toast(e.message, 'error') }
  }

  async function quitar(c) {
    try {
      await api(`/documentos/${d.id}/controles/${c.id}`, { method: 'DELETE' })
      controles.recargar()
    } catch (e) { toast(e.message, 'error') }
  }

  if (editando) {
    return (
      <Modal titulo={`Editar ${d.codigo}`} onCerrar={() => setEditando(false)}>
        <FormDocumento documento={d} responsables={responsables.lista} onCancelar={() => setEditando(false)}
          onGuardado={(nuevo) => { setEditando(false); toast('Documento actualizado'); onCambio(nuevo) }} />
      </Modal>
    )
  }

  const ids = new Set((controles.datos || []).map((c) => c.id))
  return (
    <Modal titulo={`${d.codigo} · ${d.titulo}`} onCerrar={onCerrar} ancho="wide">
      <Alerta>{error}</Alerta>
      <dl className="kv">
        <dt>Tipo</dt><dd>{etiqueta(d.tipo)}</dd>
        <dt>Versión</dt><dd>v{d.version}</dd>
        <dt>Estado</dt><dd><Estado valor={d.estado} /></dd>
        <dt>Aprobado el</dt><dd>{fecha(d.fechaAprobacion)}</dd>
        <dt>Próxima revisión</dt><dd>{fecha(d.fechaRevision)}</dd>
        <dt>Responsable</dt><dd>{responsables.nombre(d.responsableId)}</dd>
        <dt>Archivo</dt>
        <dd>
          {d.tieneArchivo
            ? <button className="btn-link" onClick={() => descargar(`/documentos/${d.id}/archivo`, d.archivoNombre).catch((e) => toast(e.message, 'error'))}>
                {d.archivoNombre}</button>
            : <span className="muted">Sin archivo</span>}
          {d.tieneArchivo && <span className="muted"> · {tamano(d.archivoTamano)}</span>}
        </dd>
        <dt>Actualizado</dt><dd>{fechaHora(d.actualizadoEn)}</dd>
      </dl>

      <div className="toolbar">
        {gestionar && d.estado !== 'OBSOLETO' && (
          <label className="btn btn-ghost btn-sm" style={{ cursor: 'pointer' }}>
            {subiendo ? 'Subiendo…' : d.tieneArchivo ? 'Reemplazar archivo' : 'Subir archivo'}
            <input type="file" hidden onChange={subir} accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.odt,.ods,.txt,.csv,.png,.jpg,.jpeg" />
          </label>
        )}
        {gestionar && <button className="btn btn-ghost btn-sm" onClick={() => setEditando(true)}>Editar datos</button>}
        <span className="spacer" />
        {aprobar && d.estado === 'BORRADOR' && (
          <button className="btn btn-primary btn-sm" onClick={aprobarDocumento} disabled={!d.tieneArchivo}
            title={d.tieneArchivo ? '' : 'Primero sube el archivo'}>Aprobar</button>
        )}
      </div>

      <h3 className="section-label" style={{ marginTop: 10 }}>Controles que evidencia</h3>
      {gestionar && (
        <div className="toolbar">
          <select value={seleccion} onChange={(e) => setSeleccion(e.target.value)} style={{ flex: 1 }}>
            <option value="">Elegir un control…</option>
            {(catalogo.datos || []).filter((c) => !ids.has(c.id)).map((c) => (
              <option key={c.id} value={c.id}>{c.codigoIso} · {c.nombre}</option>
            ))}
          </select>
          <button className="btn btn-primary btn-sm" onClick={vincular} disabled={!seleccion}>Vincular</button>
        </div>
      )}
      {controles.datos?.length === 0 ? <p className="muted" style={{ fontSize: 13.5 }}>Sin controles vinculados.</p> : (
        <table className="data">
          <tbody>
            {controles.datos?.map((c) => (
              <tr key={c.id}>
                <td><code>{c.codigoIso}</code></td>
                <td>{c.nombre}</td>
                {gestionar && <td><button className="btn-link" style={{ color: 'var(--danger)' }} onClick={() => quitar(c)}>Quitar</button></td>}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Modal>
  )
}
