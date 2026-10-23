import { useState } from 'react'
import { api, query } from '../api'
import { useAuth } from '../auth'
import {
  AccionesModal, Alerta, Campo, Cargando, Encabezado, Estado, Modal, Nivel, Selector, Vacio,
  useDatos, useFormulario, useResponsables, useToast,
} from '../components/ui'
import { ESTADOS_ACTIVO, TIPOS_ACTIVO, etiqueta, numeroONull, opciones } from '../util'

const CIA = [
  { valor: '1', texto: '1 · Bajo' },
  { valor: '2', texto: '2 · Medio' },
  { valor: '3', texto: '3 · Alto' },
]

export default function Activos() {
  const { puede } = useAuth()
  const toast = useToast()
  const responsables = useResponsables()
  const [filtros, setFiltros] = useState({ q: '', tipo: '', estado: '' })
  const [editando, setEditando] = useState(null)   // null = cerrado, {} = nuevo, objeto = editar
  const { datos, error, cargando, recargar } = useDatos(() => api(`/activos${query(filtros)}`), [filtros])
  const gestionar = puede('ACTIVOS_GESTIONAR')

  async function eliminar(a) {
    if (!window.confirm(`¿Eliminar el activo ${a.codigo} - ${a.nombre}? Los riesgos e incidentes asociados quedarán sin activo.`)) return
    try {
      await api(`/activos/${a.id}`, { method: 'DELETE' })
      toast(`Activo ${a.codigo} eliminado`)
      recargar()
    } catch (e) { toast(e.message, 'error') }
  }

  return (
    <>
      <Encabezado titulo="Activos"
        descripcion="Inventario de servidores, software, equipos y redes, con su valoración de confidencialidad, integridad y disponibilidad (ISO A.5.9 y A.5.12).">
        {gestionar && <button className="btn btn-primary" onClick={() => setEditando({})}>Nuevo activo</button>}
      </Encabezado>

      <div className="toolbar">
        <input type="search" placeholder="Buscar por código o nombre" value={filtros.q}
          onChange={(e) => setFiltros({ ...filtros, q: e.target.value })} />
        <Selector valor={filtros.tipo} onCambio={(v) => setFiltros({ ...filtros, tipo: v })} vacio="Todos los tipos" opciones={opciones(TIPOS_ACTIVO)} />
        <Selector valor={filtros.estado} onCambio={(v) => setFiltros({ ...filtros, estado: v })} vacio="Todos los estados" opciones={opciones(ESTADOS_ACTIVO)} />
        <span className="spacer" />
        {datos && <span className="count">{datos.length} activo(s)</span>}
      </div>

      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <div className="table-wrap">
          {datos?.length === 0 ? <Vacio>No hay activos que coincidan.</Vacio> : (
            <table className="data">
              <thead>
                <tr>
                  <th>Código</th><th>Nombre</th><th>Tipo</th><th title="Confidencialidad / Integridad / Disponibilidad">C / I / D</th>
                  <th>Criticidad</th><th>Estado</th><th>Responsable</th>{gestionar && <th className="col-actions" />}
                </tr>
              </thead>
              <tbody>
                {datos?.map((a) => (
                  <tr key={a.id}>
                    <td><code>{a.codigo}</code></td>
                    <td>{a.nombre}{a.descripcion && <span className="sub">{a.descripcion}</span>}</td>
                    <td>{etiqueta(a.tipo)}</td>
                    <td className="nowrap">{a.confidencialidad} / {a.integridad} / {a.disponibilidad}</td>
                    <td><Nivel nivel={a.criticidad} /></td>
                    <td><Estado valor={a.estado} /></td>
                    <td>{responsables.nombre(a.responsableId)}</td>
                    {gestionar && (
                      <td>
                        <div className="row-actions">
                          <button className="btn-link" onClick={() => setEditando(a)}>Editar</button>
                          <button className="btn-link" style={{ color: 'var(--danger)', marginLeft: 10 }} onClick={() => eliminar(a)}>Eliminar</button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {editando && (
        <FormActivo activo={editando} responsables={responsables.lista}
          onCerrar={() => setEditando(null)}
          onGuardado={(a, nuevo) => { setEditando(null); toast(nuevo ? `Activo ${a.codigo} creado` : `Activo ${a.codigo} actualizado`); recargar() }} />
      )}
    </>
  )
}

function FormActivo({ activo, responsables, onCerrar, onGuardado }) {
  const nuevo = !activo.id
  const [v, set] = useFormulario({
    nombre: activo.nombre || '',
    tipo: activo.tipo || 'SERVIDOR',
    confidencialidad: String(activo.confidencialidad || 2),
    integridad: String(activo.integridad || 2),
    disponibilidad: String(activo.disponibilidad || 2),
    estado: activo.estado || 'ACTIVO',
    descripcion: activo.descripcion || '',
    responsableId: activo.responsableId ? String(activo.responsableId) : '',
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const maximo = Math.max(Number(v.confidencialidad), Number(v.integridad), Number(v.disponibilidad))
  const criticidad = maximo === 3 ? 'ALTA' : maximo === 2 ? 'MEDIA' : 'BAJA'

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    const body = {
      ...v,
      confidencialidad: Number(v.confidencialidad),
      integridad: Number(v.integridad),
      disponibilidad: Number(v.disponibilidad),
      responsableId: numeroONull(v.responsableId),
    }
    try {
      const a = await api(nuevo ? '/activos' : `/activos/${activo.id}`, { method: nuevo ? 'POST' : 'PUT', body })
      onGuardado(a, nuevo)
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  return (
    <Modal titulo={nuevo ? 'Nuevo activo' : `Editar ${activo.codigo}`} onCerrar={onCerrar} ancho="wide">
      <form onSubmit={guardar}>
        <Alerta>{error}</Alerta>
        <div className="field-row">
          <Campo etiqueta="Nombre"><input required maxLength={150} value={v.nombre} onChange={set('nombre')} /></Campo>
          <Campo etiqueta="Tipo"><Selector valor={v.tipo} onCambio={set('tipo')} opciones={opciones(TIPOS_ACTIVO)} /></Campo>
        </div>
        <div className="field-row three">
          <Campo etiqueta="Confidencialidad"><Selector valor={v.confidencialidad} onCambio={set('confidencialidad')} opciones={CIA} /></Campo>
          <Campo etiqueta="Integridad"><Selector valor={v.integridad} onCambio={set('integridad')} opciones={CIA} /></Campo>
          <Campo etiqueta="Disponibilidad"><Selector valor={v.disponibilidad} onCambio={set('disponibilidad')} opciones={CIA} /></Campo>
        </div>
        <p className="field-hint">Criticidad calculada: {etiqueta(criticidad)} (el valor más alto de C, I y D)</p>
        <div className="field-row">
          <Campo etiqueta="Estado"><Selector valor={v.estado} onCambio={set('estado')} opciones={opciones(ESTADOS_ACTIVO)} /></Campo>
          <Campo etiqueta="Responsable">
            <Selector valor={v.responsableId} onCambio={set('responsableId')} vacio="Sin asignar"
              opciones={responsables.map((u) => ({ valor: String(u.id), texto: u.nombre }))} />
          </Campo>
        </div>
        <Campo etiqueta="Descripción"><textarea value={v.descripcion} onChange={set('descripcion')} /></Campo>
        <AccionesModal enviando={enviando} onCancelar={onCerrar} textoGuardar={nuevo ? 'Crear activo' : 'Guardar cambios'} />
      </form>
    </Modal>
  )
}
