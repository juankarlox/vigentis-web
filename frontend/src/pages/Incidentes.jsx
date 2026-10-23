import { useState } from 'react'
import { api, query } from '../api'
import { useAuth } from '../auth'
import {
  AccionesModal, Alerta, Campo, Cargando, Encabezado, Estado, Modal, Nivel, Selector, Vacio,
  useDatos, useFormulario, useResponsables, useToast,
} from '../components/ui'
import {
  ESTADOS_INCIDENTE, SEVERIDADES, TIPOS_INCIDENTE, aInputFechaHora, ahoraInput, etiqueta, fechaHora, numeroONull, opciones, vacioANull,
} from '../util'

export default function Incidentes() {
  const { puede } = useAuth()
  const toast = useToast()
  const responsables = useResponsables()
  const [filtros, setFiltros] = useState({ q: '', estado: '', severidad: '', tipo: '' })
  const [editando, setEditando] = useState(null)
  const { datos, error, cargando, recargar } = useDatos(() => api(`/incidentes${query(filtros)}`), [filtros])
  const activos = useDatos(() => (puede('ACTIVOS_VER') ? api('/activos') : Promise.resolve([])))
  const gestionar = puede('INCIDENTES_GESTIONAR')

  return (
    <>
      <Encabezado titulo="Incidentes"
        descripcion="Registro y atención de incidentes de seguridad. Para cerrarlos se documentan las acciones tomadas (ISO A.5.24 a A.5.28).">
        {gestionar && <button className="btn btn-primary" onClick={() => setEditando({})}>Registrar incidente</button>}
      </Encabezado>

      <div className="toolbar">
        <input type="search" placeholder="Buscar por código o título" value={filtros.q}
          onChange={(e) => setFiltros({ ...filtros, q: e.target.value })} />
        <Selector valor={filtros.estado} onCambio={(v) => setFiltros({ ...filtros, estado: v })} vacio="Todos los estados" opciones={opciones(ESTADOS_INCIDENTE)} />
        <Selector valor={filtros.severidad} onCambio={(v) => setFiltros({ ...filtros, severidad: v })} vacio="Todas las severidades" opciones={opciones(SEVERIDADES)} />
        <Selector valor={filtros.tipo} onCambio={(v) => setFiltros({ ...filtros, tipo: v })} vacio="Todos los tipos" opciones={opciones(TIPOS_INCIDENTE)} />
        <span className="spacer" />
        {datos && <span className="count">{datos.length} incidente(s)</span>}
      </div>

      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <div className="table-wrap">
          {datos?.length === 0 ? <Vacio>No hay incidentes que coincidan.</Vacio> : (
            <table className="data">
              <thead><tr><th>Código</th><th>Incidente</th><th>Tipo</th><th>Severidad</th><th>Detección</th><th>Estado</th><th>Responsable</th></tr></thead>
              <tbody>
                {datos?.map((i) => (
                  <tr key={i.id} className="clickable" onClick={() => setEditando(i)}>
                    <td><code>{i.codigo}</code></td>
                    <td>{i.titulo}<span className="sub">{i.activo ? `${i.activo.codigo} · ${i.activo.nombre}` : 'Sin activo asociado'}</span></td>
                    <td>{etiqueta(i.tipo)}</td>
                    <td><Nivel nivel={i.severidad} /></td>
                    <td className="nowrap">{fechaHora(i.fechaDeteccion)}{i.fechaCierre && <span className="sub">Cerrado {fechaHora(i.fechaCierre)}</span>}</td>
                    <td><Estado valor={i.estado} /></td>
                    <td>{responsables.nombre(i.responsableId)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {editando && (
        <FormIncidente incidente={editando} gestionar={gestionar} activos={activos.datos || []} responsables={responsables}
          onCerrar={() => setEditando(null)}
          onGuardado={(i, nuevo) => { setEditando(null); toast(nuevo ? `Incidente ${i.codigo} registrado` : `Incidente ${i.codigo} actualizado`); recargar() }} />
      )}
    </>
  )
}

function FormIncidente({ incidente, gestionar, activos, responsables, onCerrar, onGuardado }) {
  const nuevo = !incidente.id
  const [v, set] = useFormulario({
    titulo: incidente.titulo || '',
    tipo: incidente.tipo || 'PHISHING',
    severidad: incidente.severidad || 'MEDIA',
    estado: incidente.estado || 'ABIERTO',
    activoId: incidente.activo ? String(incidente.activo.id) : '',
    fechaDeteccion: incidente.fechaDeteccion ? aInputFechaHora(incidente.fechaDeteccion) : ahoraInput(),
    fechaCierre: aInputFechaHora(incidente.fechaCierre),
    descripcion: incidente.descripcion || '',
    acciones: incidente.acciones || '',
    responsableId: incidente.responsableId ? String(incidente.responsableId) : '',
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const cerrando = v.estado === 'CERRADO'

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    const body = {
      titulo: v.titulo,
      tipo: v.tipo,
      severidad: v.severidad,
      estado: v.estado,
      activoId: numeroONull(v.activoId),
      fechaDeteccion: v.fechaDeteccion.length === 16 ? `${v.fechaDeteccion}:00` : v.fechaDeteccion,
      fechaCierre: cerrando && v.fechaCierre ? (v.fechaCierre.length === 16 ? `${v.fechaCierre}:00` : v.fechaCierre) : null,
      descripcion: v.descripcion,
      acciones: vacioANull(v.acciones),
      responsableId: numeroONull(v.responsableId),
    }
    try {
      const i = await api(nuevo ? '/incidentes' : `/incidentes/${incidente.id}`, { method: nuevo ? 'POST' : 'PUT', body })
      onGuardado(i, nuevo)
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  const titulo = nuevo ? 'Registrar incidente' : `${incidente.codigo} · ${incidente.titulo}`
  if (!gestionar) {
    return (
      <Modal titulo={titulo} onCerrar={onCerrar} ancho="wide">
        <dl className="kv">
          <dt>Tipo</dt><dd>{etiqueta(incidente.tipo)}</dd>
          <dt>Severidad</dt><dd><Nivel nivel={incidente.severidad} /></dd>
          <dt>Estado</dt><dd><Estado valor={incidente.estado} /></dd>
          <dt>Activo</dt><dd>{incidente.activo ? `${incidente.activo.codigo} · ${incidente.activo.nombre}` : '—'}</dd>
          <dt>Detección</dt><dd>{fechaHora(incidente.fechaDeteccion)}</dd>
          <dt>Cierre</dt><dd>{fechaHora(incidente.fechaCierre)}</dd>
          <dt>Descripción</dt><dd>{incidente.descripcion || '—'}</dd>
          <dt>Acciones tomadas</dt><dd>{incidente.acciones || '—'}</dd>
          <dt>Responsable</dt><dd>{responsables.nombre(incidente.responsableId)}</dd>
        </dl>
      </Modal>
    )
  }

  return (
    <Modal titulo={titulo} onCerrar={onCerrar} ancho="wide">
      <form onSubmit={guardar}>
        <Alerta>{error}</Alerta>
        <Campo etiqueta="Título"><input required maxLength={200} value={v.titulo} onChange={set('titulo')} /></Campo>
        <div className="field-row three">
          <Campo etiqueta="Tipo"><Selector valor={v.tipo} onCambio={set('tipo')} opciones={opciones(TIPOS_INCIDENTE)} /></Campo>
          <Campo etiqueta="Severidad"><Selector valor={v.severidad} onCambio={set('severidad')} opciones={opciones(SEVERIDADES)} /></Campo>
          <Campo etiqueta="Estado"><Selector valor={v.estado} onCambio={set('estado')} opciones={opciones(ESTADOS_INCIDENTE)} /></Campo>
        </div>
        <div className="field-row">
          <Campo etiqueta="Activo afectado">
            <Selector valor={v.activoId} onCambio={set('activoId')} vacio="Ninguno"
              opciones={activos.map((a) => ({ valor: String(a.id), texto: `${a.codigo} · ${a.nombre}` }))} />
          </Campo>
          <Campo etiqueta="Responsable">
            <Selector valor={v.responsableId} onCambio={set('responsableId')} vacio="Sin asignar"
              opciones={responsables.lista.map((u) => ({ valor: String(u.id), texto: u.nombre }))} />
          </Campo>
        </div>
        <div className="field-row">
          <Campo etiqueta="Fecha y hora de detección">
            <input type="datetime-local" required max={ahoraInput()} value={v.fechaDeteccion} onChange={set('fechaDeteccion')} />
          </Campo>
          {cerrando && (
            <Campo etiqueta="Fecha y hora de cierre" ayuda="Si la dejas vacía se usa la fecha actual.">
              <input type="datetime-local" max={ahoraInput()} value={v.fechaCierre} onChange={set('fechaCierre')} />
            </Campo>
          )}
        </div>
        <Campo etiqueta="Descripción"><textarea value={v.descripcion} onChange={set('descripcion')} /></Campo>
        <Campo etiqueta={cerrando ? 'Acciones tomadas (obligatorio para cerrar)' : 'Acciones tomadas'}>
          <textarea value={v.acciones} onChange={set('acciones')} required={cerrando} />
        </Campo>
        <AccionesModal enviando={enviando} onCancelar={onCerrar} textoGuardar={nuevo ? 'Registrar' : 'Guardar cambios'} />
      </form>
    </Modal>
  )
}
