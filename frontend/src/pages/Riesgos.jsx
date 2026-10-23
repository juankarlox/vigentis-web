import { useState } from 'react'
import { api, query } from '../api'
import { useAuth } from '../auth'
import {
  AccionesModal, Alerta, Campo, Cargando, Encabezado, Estado, Modal, Nivel, Pestanas, Selector, Vacio,
  useDatos, useFormulario, useResponsables, useToast,
} from '../components/ui'
import {
  ESTADOS_RIESGO, OPCIONES_TRATAMIENTO, criteriosActuales, etiqueta, fecha, fechaHora, nivelRiesgo, numeroONull,
  opciones, vacioANull,
} from '../util'

const NUMEROS = [1, 2, 3, 4, 5].map((n) => ({ valor: String(n), texto: String(n) }))

/** Opciones 1–5 con el nombre corto de la escala definida en los criterios (ej.: "3 · Posible"). */
function escala(lineas) {
  if (!lineas || lineas.length < 5) return NUMEROS
  return lineas.slice(0, 5).map((l, i) => ({ valor: String(i + 1), texto: `${i + 1} · ${l.split(':')[0].trim()}` }))
}

const IMPACTO_SUGERIDO = { ALTA: 'El activo tiene criticidad alta: se sugiere impacto 4 o 5.',
  MEDIA: 'El activo tiene criticidad media: se sugiere impacto alrededor de 3.',
  BAJA: 'El activo tiene criticidad baja: se sugiere impacto 1 o 2.' }

export default function Riesgos() {
  const { puede } = useAuth()
  const toast = useToast()
  const responsables = useResponsables()
  const [filtros, setFiltros] = useState({ q: '', estado: '', activoId: '' })
  const [nuevo, setNuevo] = useState(false)
  const [detalle, setDetalle] = useState(null)
  const { datos, error, cargando, recargar } = useDatos(() => api(`/riesgos${query(filtros)}`), [filtros])
  const activos = useDatos(() => (puede('ACTIVOS_VER') ? api('/activos') : Promise.resolve([])))
  const gestionar = puede('RIESGOS_GESTIONAR')

  return (
    <>
      <Encabezado titulo="Riesgos"
        descripcion="Cada riesgo se valora por probabilidad × impacto (1 a 5). Los cambios quedan en su bitácora (ISO 6.1.2, 6.1.3 y 8.3).">
        {gestionar && <button className="btn btn-primary" onClick={() => setNuevo(true)}>Nuevo riesgo</button>}
      </Encabezado>

      <div className="toolbar">
        <input type="search" placeholder="Buscar por código o nombre" value={filtros.q}
          onChange={(e) => setFiltros({ ...filtros, q: e.target.value })} />
        <Selector valor={filtros.estado} onCambio={(v) => setFiltros({ ...filtros, estado: v })} vacio="Todos los estados" opciones={opciones(ESTADOS_RIESGO)} />
        <Selector valor={filtros.activoId} onCambio={(v) => setFiltros({ ...filtros, activoId: v })} vacio="Todos los activos"
          opciones={(activos.datos || []).map((a) => ({ valor: String(a.id), texto: `${a.codigo} · ${a.nombre}` }))} />
        <span className="spacer" />
        {datos && <span className="count">{datos.length} riesgo(s)</span>}
      </div>

      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <div className="table-wrap">
          {datos?.length === 0 ? <Vacio>No hay riesgos que coincidan.</Vacio> : (
            <table className="data">
              <thead>
                <tr><th>Código</th><th>Riesgo</th><th>Nivel</th><th>Tratamiento</th><th>Residual</th><th>Estado</th><th>Revisión</th><th>Responsable</th></tr>
              </thead>
              <tbody>
                {datos?.map((r) => (
                  <tr key={r.id} className="clickable" onClick={() => setDetalle(r)} title="Ver detalle y bitácora">
                    <td><code>{r.codigo}</code></td>
                    <td>{r.nombre}<span className="sub">{r.activo ? `${r.activo.codigo} · ${r.activo.nombre}` : 'Sin activo asociado'}</span></td>
                    <td className="nowrap"><Nivel nivel={r.nivel} valor={r.nivelValor} /><span className="sub">P{r.probabilidad} × I{r.impacto}</span></td>
                    <td>{etiqueta(r.opcionTratamiento)}</td>
                    <td className="nowrap">{r.nivelResidual ? <Nivel nivel={r.nivelResidualTexto} valor={r.nivelResidual} /> : <span className="muted">—</span>}</td>
                    <td><Estado valor={r.estado} />
                      {!r.dentroDeNivelAceptable && r.estado !== 'ACEPTADO' && r.estado !== 'CERRADO' &&
                        <span className="sub" style={{ color: 'var(--danger)' }}>Sobre el nivel aceptable</span>}
                    </td>
                    <td className="nowrap">{fecha(r.fechaRevision)}
                      {r.revisionVencida && <span className="sub" style={{ color: 'var(--warning)', fontWeight: 600 }}>Vencida</span>}
                    </td>
                    <td>{responsables.nombre(r.responsableId)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {nuevo && (
        <Modal titulo="Nuevo riesgo" onCerrar={() => setNuevo(false)} ancho="wide">
          <FormRiesgo riesgo={{}} activos={activos.datos || []} responsables={responsables.lista}
            onCancelar={() => setNuevo(false)}
            onGuardado={(r) => { setNuevo(false); toast(`Riesgo ${r.codigo} registrado`); recargar() }} />
        </Modal>
      )}

      {detalle && (
        <DetalleRiesgo riesgo={detalle} activos={activos.datos || []} responsables={responsables}
          gestionar={gestionar}
          onCerrar={() => setDetalle(null)}
          onCambio={(r) => { setDetalle(r); recargar() }} />
      )}
    </>
  )
}

function DetalleRiesgo({ riesgo, activos, responsables, gestionar, onCerrar, onCambio }) {
  const [pestana, setPestana] = useState('datos')
  const toast = useToast()
  return (
    <Modal titulo={`${riesgo.codigo} · ${riesgo.nombre}`} onCerrar={onCerrar} ancho="xwide">
      <Pestanas activa={pestana} onCambio={setPestana} opciones={[
        { id: 'datos', texto: gestionar ? 'Datos y tratamiento' : 'Datos' },
        { id: 'decision', texto: 'Aceptación y revisión' },
        { id: 'bitacora', texto: 'Bitácora' },
        { id: 'controles', texto: 'Controles ISO' },
      ]} />
      {pestana === 'datos' && (gestionar
        ? <FormRiesgo riesgo={riesgo} activos={activos} responsables={responsables.lista} onCancelar={onCerrar}
            onGuardado={(r) => { toast(`Riesgo ${r.codigo} actualizado`); onCambio(r) }} />
        : <VistaRiesgo riesgo={riesgo} responsables={responsables} />)}
      {pestana === 'decision' && <AceptacionRevision riesgo={riesgo} gestionar={gestionar} onCambio={onCambio} />}
      {pestana === 'bitacora' && <Bitacora riesgo={riesgo} gestionar={gestionar} />}
      {pestana === 'controles' && <ControlesDelRiesgo riesgo={riesgo} gestionar={gestionar} />}
    </Modal>
  )
}

function VistaRiesgo({ riesgo: r, responsables }) {
  return (
    <dl className="kv">
      <dt>Activo</dt><dd>{r.activo ? `${r.activo.codigo} · ${r.activo.nombre}` : '—'}</dd>
      <dt>Descripción</dt><dd>{r.descripcion || '—'}</dd>
      <dt>Amenaza</dt><dd>{r.amenaza || '—'}</dd>
      <dt>Vulnerabilidad</dt><dd>{r.vulnerabilidad || '—'}</dd>
      <dt>Valoración</dt><dd>P{r.probabilidad} × I{r.impacto} = <Nivel nivel={r.nivel} valor={r.nivelValor} /></dd>
      <dt>Tratamiento</dt><dd>{etiqueta(r.opcionTratamiento)}{r.tratamiento ? ` — ${r.tratamiento}` : ''}</dd>
      <dt>Riesgo residual</dt><dd>{r.nivelResidual ? <Nivel nivel={r.nivelResidualTexto} valor={r.nivelResidual} /> : '—'}</dd>
      <dt>Estado</dt><dd><Estado valor={r.estado} /></dd>
      <dt>Responsable</dt><dd>{responsables.nombre(r.responsableId)}</dd>
      <dt>Próxima revisión</dt><dd>{fecha(r.fechaRevision)}</dd>
      <dt>Actualizado</dt><dd>{fechaHora(r.actualizadoEn)}</dd>
    </dl>
  )
}

function FormRiesgo({ riesgo, activos, responsables, onCancelar, onGuardado }) {
  const nuevo = !riesgo.id
  const [v, set] = useFormulario({
    nombre: riesgo.nombre || '',
    descripcion: riesgo.descripcion || '',
    amenaza: riesgo.amenaza || '',
    vulnerabilidad: riesgo.vulnerabilidad || '',
    fechaRevision: riesgo.fechaRevision || '',
    activoId: riesgo.activo ? String(riesgo.activo.id) : '',
    probabilidad: String(riesgo.probabilidad || 3),
    impacto: String(riesgo.impacto || 3),
    estado: riesgo.estado || 'ABIERTO',
    opcionTratamiento: riesgo.opcionTratamiento || '',
    tratamiento: riesgo.tratamiento || '',
    probabilidadResidual: riesgo.probabilidadResidual ? String(riesgo.probabilidadResidual) : '',
    impactoResidual: riesgo.impactoResidual ? String(riesgo.impactoResidual) : '',
    responsableId: riesgo.responsableId ? String(riesgo.responsableId) : '',
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const nivel = Number(v.probabilidad) * Number(v.impacto)
  const residual = v.probabilidadResidual && v.impactoResidual ? Number(v.probabilidadResidual) * Number(v.impactoResidual) : null
  const c = criteriosActuales()
  const escalaP = escala(c.escalaProbabilidad)
  const escalaI = escala(c.escalaImpacto)
  const activoElegido = activos.find((a) => String(a.id) === v.activoId)
  // ACEPTADO solo se asigna con la acción formal de aceptación
  const estados = ESTADOS_RIESGO.filter((e) => e !== 'ACEPTADO' || riesgo.estado === 'ACEPTADO')

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    const body = {
      nombre: v.nombre,
      descripcion: v.descripcion,
      amenaza: v.amenaza,
      vulnerabilidad: v.vulnerabilidad,
      fechaRevision: vacioANull(v.fechaRevision),
      activoId: numeroONull(v.activoId),
      probabilidad: Number(v.probabilidad),
      impacto: Number(v.impacto),
      estado: v.estado,
      opcionTratamiento: vacioANull(v.opcionTratamiento),
      tratamiento: v.tratamiento,
      probabilidadResidual: numeroONull(v.probabilidadResidual),
      impactoResidual: numeroONull(v.impactoResidual),
      responsableId: numeroONull(v.responsableId),
    }
    try {
      const r = await api(nuevo ? '/riesgos' : `/riesgos/${riesgo.id}`, { method: nuevo ? 'POST' : 'PUT', body })
      onGuardado(r)
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  return (
    <form onSubmit={guardar}>
      <Alerta>{error}</Alerta>
      <Campo etiqueta="Nombre del riesgo"><input required maxLength={200} value={v.nombre} onChange={set('nombre')} /></Campo>
      <div className="field-row">
        <Campo etiqueta="Activo afectado">
          <Selector valor={v.activoId} onCambio={set('activoId')} vacio="Ninguno"
            opciones={activos.map((a) => ({ valor: String(a.id), texto: `${a.codigo} · ${a.nombre}` }))} />
        </Campo>
        <Campo etiqueta="Responsable">
          <Selector valor={v.responsableId} onCambio={set('responsableId')} vacio="Sin asignar"
            opciones={responsables.map((u) => ({ valor: String(u.id), texto: u.nombre }))} />
        </Campo>
      </div>
      <div className="field-row">
        <Campo etiqueta="Amenaza" ayuda="Qué podría pasar. Ej.: correo de phishing que roba contraseñas">
          <input maxLength={200} value={v.amenaza} onChange={set('amenaza')} />
        </Campo>
        <Campo etiqueta="Vulnerabilidad" ayuda="Qué debilidad lo permite. Ej.: no hay doble factor de autenticación">
          <input maxLength={200} value={v.vulnerabilidad} onChange={set('vulnerabilidad')} />
        </Campo>
      </div>
      <Campo etiqueta="Descripción"><textarea value={v.descripcion} onChange={set('descripcion')} /></Campo>
      <div className="field-row three">
        <Campo etiqueta="Probabilidad"><Selector valor={v.probabilidad} onCambio={set('probabilidad')} opciones={escalaP} /></Campo>
        <Campo etiqueta="Impacto"><Selector valor={v.impacto} onCambio={set('impacto')} opciones={escalaI} /></Campo>
        <Campo etiqueta="Estado"><Selector valor={v.estado} onCambio={set('estado')} opciones={opciones(estados)} /></Campo>
      </div>
      <p className="field-hint">
        Nivel inherente: {nivel} · {etiqueta(nivelRiesgo(nivel))}
        {nivel <= c.nivelAceptable ? ' · dentro del nivel aceptable' : ` · supera el nivel aceptable (${c.nivelAceptable})`}
      </p>
      {activoElegido && IMPACTO_SUGERIDO[activoElegido.criticidad] && (
        <p className="field-help" style={{ margin: '-8px 0 16px' }}>{IMPACTO_SUGERIDO[activoElegido.criticidad]}</p>
      )}
      <div className="field-row">
        <Campo etiqueta="Opción de tratamiento">
          <Selector valor={v.opcionTratamiento} onCambio={set('opcionTratamiento')} vacio="Sin definir" opciones={opciones(OPCIONES_TRATAMIENTO)} />
        </Campo>
        <div className="field-row" style={{ gap: '0 10px' }}>
          <Campo etiqueta="Prob. residual"><Selector valor={v.probabilidadResidual} onCambio={set('probabilidadResidual')} vacio="—" opciones={NUMEROS} /></Campo>
          <Campo etiqueta="Impacto residual"><Selector valor={v.impactoResidual} onCambio={set('impactoResidual')} vacio="—" opciones={NUMEROS} /></Campo>
        </div>
      </div>
      <Campo etiqueta="Plan de tratamiento"><textarea value={v.tratamiento} onChange={set('tratamiento')} placeholder="Ej.: activar autenticación de dos factores" /></Campo>
      {residual && (
        <p className="field-hint">
          Nivel residual esperado: {residual} · {etiqueta(nivelRiesgo(residual))}
          {residual <= c.nivelAceptable ? ' · dentro del nivel aceptable' : ` · sigue sobre el nivel aceptable (${c.nivelAceptable})`}
        </p>
      )}
      <Campo etiqueta="Próxima revisión" ayuda={`Si la dejas vacía al crearlo, se programa a ${c.mesesRevision} meses.`}>
        <input type="date" value={v.fechaRevision} onChange={set('fechaRevision')} />
      </Campo>
      <AccionesModal enviando={enviando} onCancelar={onCancelar} textoGuardar={nuevo ? 'Registrar riesgo' : 'Guardar cambios'} />
    </form>
  )
}

function Bitacora({ riesgo, gestionar }) {
  const { datos, error, cargando, recargar } = useDatos(() => api(`/riesgos/${riesgo.id}/bitacora`), [riesgo.id, riesgo.actualizadoEn])
  const [comentario, setComentario] = useState('')
  const [errorComentario, setErrorComentario] = useState('')

  async function comentar(e) {
    e.preventDefault()
    setErrorComentario('')
    try {
      await api(`/riesgos/${riesgo.id}/comentarios`, { method: 'POST', body: { comentario } })
      setComentario('')
      recargar()
    } catch (err) { setErrorComentario(err.message) }
  }

  return (
    <>
      {gestionar && (
        <form onSubmit={comentar} style={{ marginBottom: 14 }}>
          <Alerta>{errorComentario}</Alerta>
          <Campo etiqueta="Agregar comentario de seguimiento">
            <textarea required maxLength={2000} value={comentario} onChange={(e) => setComentario(e.target.value)} />
          </Campo>
          <div className="modal-actions"><button className="btn btn-primary">Agregar a la bitácora</button></div>
        </form>
      )}
      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <ul className="timeline">
          {datos?.map((b) => (
            <li key={b.id}>
              <time>{fechaHora(b.fecha)}<span className="who"><br />{b.usuarioNombre || 'Sistema'}</span></time>
              <div>
                <strong>{etiqueta(b.accion)}</strong>
                {b.estadoAnterior && <> · {etiqueta(b.estadoAnterior)} → {etiqueta(b.estadoNuevo)}</>}
                {!b.estadoAnterior && b.estadoNuevo && <> · {etiqueta(b.estadoNuevo)}</>}
                {b.nivelAnterior && b.nivelNuevo && <> · nivel {b.nivelAnterior} → {b.nivelNuevo}</>}
                {!b.nivelAnterior && b.nivelNuevo && <> · nivel {b.nivelNuevo}</>}
                {b.comentario && <div className="muted" style={{ marginTop: 3 }}>{b.comentario}</div>}
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}

function ControlesDelRiesgo({ riesgo, gestionar }) {
  const toast = useToast()
  const vinculados = useDatos(() => api(`/riesgos/${riesgo.id}/controles`), [riesgo.id])
  const catalogo = useDatos(() => (gestionar ? api('/controles?aplica=true') : Promise.resolve([])), [gestionar])
  const [seleccion, setSeleccion] = useState('')

  async function vincular() {
    if (!seleccion) return
    try {
      await api(`/riesgos/${riesgo.id}/controles/${seleccion}`, { method: 'POST' })
      setSeleccion('')
      toast('Control vinculado y anotado en la bitácora')
      vinculados.recargar()
    } catch (e) { toast(e.message, 'error') }
  }

  async function quitar(c) {
    try {
      await api(`/riesgos/${riesgo.id}/controles/${c.id}`, { method: 'DELETE' })
      toast(`Control ${c.codigoIso} desvinculado`)
      vinculados.recargar()
    } catch (e) { toast(e.message, 'error') }
  }

  const ids = new Set((vinculados.datos || []).map((c) => c.id))
  return (
    <>
      <p className="muted" style={{ marginTop: 0, fontSize: 13.5 }}>Controles del Anexo A que tratan este riesgo.</p>
      {gestionar && (
        <div className="toolbar">
          <select value={seleccion} onChange={(e) => setSeleccion(e.target.value)} style={{ flex: 1 }}>
            <option value="">Elegir un control para vincular…</option>
            {(catalogo.datos || []).filter((c) => !ids.has(c.id)).map((c) => (
              <option key={c.id} value={c.id}>{c.codigoIso} · {c.nombre}</option>
            ))}
          </select>
          <button className="btn btn-primary" onClick={vincular} disabled={!seleccion}>Vincular</button>
        </div>
      )}
      <Alerta>{vinculados.error}</Alerta>
      {vinculados.datos?.length === 0 ? <Vacio>Este riesgo todavía no tiene controles vinculados.</Vacio> : (
        <table className="data">
          <tbody>
            {vinculados.datos?.map((c) => (
              <tr key={c.id}>
                <td><code>{c.codigoIso}</code></td>
                <td>{c.nombre}<span className="sub">{etiqueta(c.tema)}</span></td>
                <td><Estado valor={c.estado} /></td>
                {gestionar && <td><button className="btn-link" style={{ color: 'var(--danger)' }} onClick={() => quitar(c)}>Quitar</button></td>}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

function AceptacionRevision({ riesgo: r, gestionar, onCambio }) {
  const { puede } = useAuth()
  const toast = useToast()
  const c = criteriosActuales()
  const [justificacion, setJustificacion] = useState('')
  const [comentario, setComentario] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState('')
  const aceptable = r.nivelActual <= c.nivelAceptable
  const puedeAceptar = puede('RIESGOS_ACEPTAR') && r.estado !== 'ACEPTADO' && r.estado !== 'CERRADO'

  async function accion(ruta, texto, mensaje) {
    setError('')
    setEnviando(ruta)
    try {
      const nuevo = await api(`/riesgos/${r.id}/${ruta}`, { method: 'POST', body: { texto } })
      toast(mensaje)
      setJustificacion('')
      setComentario('')
      onCambio(nuevo)
    } catch (e) { setError(e.message) } finally { setEnviando('') }
  }

  return (
    <>
      <Alerta>{error}</Alerta>
      <div className="decision">
        <h4>Aceptación del riesgo (ISO/IEC 27001, 6.1.3 f)</h4>
        <p>
          Nivel actual: <strong>{r.nivelActual}</strong> ({r.nivelResidual ? 'residual' : 'inherente, aún sin residual'}) ·
          nivel aceptable de la organización: <strong>{c.nivelAceptable}</strong>.{' '}
          {aceptable ? 'Está dentro de lo aceptable.' : 'Supera lo aceptable: solo se puede aceptar de forma excepcional, con justificación.'}
        </p>
        {r.estado === 'ACEPTADO' ? (
          <dl className="kv" style={{ margin: 0 }}>
            <dt>Aceptado por</dt><dd>{r.aceptadoPorNombre || '—'}</dd>
            <dt>Fecha</dt><dd>{fechaHora(r.aceptadoEn)}</dd>
            <dt>Justificación</dt><dd>{r.justificacionAceptacion || '—'}</dd>
          </dl>
        ) : puedeAceptar ? (
          <>
            <Campo etiqueta={aceptable ? 'Justificación (opcional)' : 'Justificación (obligatoria)'}>
              <textarea value={justificacion} onChange={(e) => setJustificacion(e.target.value)} maxLength={2000}
                placeholder="Ej.: el costo del control supera el impacto esperado; se acepta hasta la próxima revisión" />
            </Campo>
            <div className="modal-actions">
              <button className="btn btn-primary" disabled={enviando === 'aceptar' || (!aceptable && !justificacion.trim())}
                onClick={() => accion('aceptar', justificacion, `Riesgo ${r.codigo} aceptado formalmente`)}>
                {enviando === 'aceptar' ? 'Guardando…' : 'Aceptar riesgo'}
              </button>
            </div>
          </>
        ) : (
          <p className="muted" style={{ margin: 0 }}>
            {r.estado === 'CERRADO' ? 'El riesgo está cerrado.' : 'Solo un rol con el permiso “Aprobar la aceptación de riesgos” puede aceptarlo.'}
          </p>
        )}
      </div>

      <div className="decision">
        <h4>Revisión periódica (ISO/IEC 27001, 8.2)</h4>
        <p>
          Próxima revisión: <strong>{fecha(r.fechaRevision)}</strong>
          {r.revisionVencida && <strong style={{ color: 'var(--warning)' }}> · vencida</strong>}.
          Al registrar la revisión se programa la siguiente a {c.mesesRevision} meses.
        </p>
        {gestionar && r.estado !== 'CERRADO' && (
          <>
            <Campo etiqueta="Comentario de la revisión (opcional)">
              <textarea value={comentario} onChange={(e) => setComentario(e.target.value)} maxLength={2000}
                placeholder="Ej.: se verificó que el control sigue funcionando; la valoración no cambia" />
            </Campo>
            <div className="modal-actions">
              <button className="btn btn-ghost" disabled={enviando === 'revisar'}
                onClick={() => accion('revisar', comentario, `Revisión de ${r.codigo} registrada`)}>
                {enviando === 'revisar' ? 'Guardando…' : 'Registrar revisión'}
              </button>
            </div>
          </>
        )}
      </div>
    </>
  )
}
