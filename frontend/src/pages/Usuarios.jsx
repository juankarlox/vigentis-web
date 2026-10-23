import { useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'
import {
  AccionesModal, Alerta, Campo, Cargando, Encabezado, Modal, Pestanas, Selector, Vacio,
  olvidarResponsables, useDatos, useFormulario, useToast,
} from '../components/ui'
import { etiqueta, fechaHora } from '../util'

export default function Usuarios() {
  const { puede } = useAuth()
  const [pestana, setPestana] = useState(puede('USUARIOS_GESTIONAR') ? 'usuarios' : 'roles')
  const opcionesPestana = [
    ...(puede('USUARIOS_GESTIONAR') ? [{ id: 'usuarios', texto: 'Usuarios' }] : []),
    { id: 'roles', texto: 'Roles y permisos' },
  ]
  return (
    <>
      <Encabezado titulo="Usuarios y roles"
        descripcion="Cada empresa define sus roles y qué permisos tiene cada uno. Los usuarios no se borran: se desactivan, para conservar la trazabilidad (ISO A.5.15 y A.5.18)." />
      <Pestanas activa={pestana} onCambio={setPestana} opciones={opcionesPestana} />
      {pestana === 'usuarios' ? <ListaUsuarios /> : <ListaRoles editable={puede('ROLES_GESTIONAR')} />}
    </>
  )
}

function ListaUsuarios() {
  const toast = useToast()
  const { usuario: yo } = useAuth()
  const { datos, error, cargando, recargar } = useDatos(() => api('/usuarios'))
  const roles = useDatos(() => api('/roles'))
  const [editando, setEditando] = useState(null)

  async function desbloquear(u) {
    try {
      await api(`/usuarios/${u.id}/desbloquear`, { method: 'POST' })
      toast(`${u.nombre} desbloqueado`)
      recargar()
    } catch (e) { toast(e.message, 'error') }
  }

  return (
    <>
      <div className="toolbar">
        {datos && <span className="count">{datos.length} usuario(s)</span>}
        <span className="spacer" />
        <button className="btn btn-primary" onClick={() => setEditando({})}>Nuevo usuario</button>
      </div>
      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Último acceso</th><th className="col-actions" /></tr></thead>
            <tbody>
              {datos?.map((u) => (
                <tr key={u.id}>
                  <td>{u.nombre}{u.id === yo.id && <span className="sub">Tú</span>}</td>
                  <td>{u.correo}</td>
                  <td>{u.rol.nombre}</td>
                  <td>
                    {!u.activo ? <span className="pill pill-neutral">Desactivado</span>
                      : u.bloqueado ? <span className="pill pill-none">Bloqueado</span>
                        : <span className="pill pill-ok">Activo</span>}
                  </td>
                  <td className="nowrap muted">{fechaHora(u.ultimoAcceso)}</td>
                  <td>
                    <div className="row-actions">
                      <button className="btn-link" onClick={() => setEditando(u)}>Editar</button>
                      {u.bloqueado && <button className="btn-link" style={{ marginLeft: 10 }} onClick={() => desbloquear(u)}>Desbloquear</button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {editando && (
        <FormUsuario usuario={editando} roles={roles.datos || []} onCerrar={() => setEditando(null)}
          onGuardado={(u, nuevo) => { setEditando(null); olvidarResponsables(); toast(nuevo ? `Usuario ${u.correo} creado` : 'Usuario actualizado'); recargar() }} />
      )}
    </>
  )
}

function FormUsuario({ usuario, roles, onCerrar, onGuardado }) {
  const nuevo = !usuario.id
  const [v, set] = useFormulario({
    nombre: usuario.nombre || '',
    correo: usuario.correo || '',
    rolId: usuario.rol ? String(usuario.rol.id) : '',
    clave: '',
    activo: usuario.activo ?? true,
  })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    const body = { ...v, rolId: Number(v.rolId), clave: v.clave || null }
    try {
      const u = await api(nuevo ? '/usuarios' : `/usuarios/${usuario.id}`, { method: nuevo ? 'POST' : 'PUT', body })
      onGuardado(u, nuevo)
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  return (
    <Modal titulo={nuevo ? 'Nuevo usuario' : `Editar ${usuario.nombre}`} onCerrar={onCerrar}>
      <form onSubmit={guardar}>
        <Alerta>{error}</Alerta>
        <Campo etiqueta="Nombre completo"><input required maxLength={100} value={v.nombre} onChange={set('nombre')} /></Campo>
        <Campo etiqueta="Correo"><input type="email" required maxLength={150} value={v.correo} onChange={set('correo')} /></Campo>
        <Campo etiqueta="Rol">
          <Selector valor={v.rolId} onCambio={set('rolId')} vacio="Elegir rol…" required
            opciones={roles.map((r) => ({ valor: String(r.id), texto: r.nombre }))} />
        </Campo>
        <Campo etiqueta={nuevo ? 'Contraseña inicial' : 'Nueva contraseña (opcional)'}
          ayuda="Mínimo 10 caracteres, con letras y números. Entrégasela al usuario por un medio seguro.">
          <input type="password" autoComplete="new-password" required={nuevo} minLength={10} value={v.clave} onChange={set('clave')} />
        </Campo>
        {!nuevo && (
          <label className="check">
            <input type="checkbox" checked={v.activo} onChange={(e) => set('activo')(e.target.checked)} />
            Usuario activo (si lo desactivas, no podrá iniciar sesión)
          </label>
        )}
        <AccionesModal enviando={enviando} onCancelar={onCerrar} textoGuardar={nuevo ? 'Crear usuario' : 'Guardar cambios'} />
      </form>
    </Modal>
  )
}

function ListaRoles({ editable }) {
  const toast = useToast()
  const { datos, error, cargando, recargar } = useDatos(() => api('/roles'))
  const permisos = useDatos(() => api('/permisos'))
  const [editando, setEditando] = useState(null)

  async function eliminar(r) {
    if (!window.confirm(`¿Eliminar el rol "${r.nombre}"?`)) return
    try {
      await api(`/roles/${r.id}`, { method: 'DELETE' })
      toast('Rol eliminado')
      recargar()
    } catch (e) { toast(e.message, 'error') }
  }

  return (
    <>
      <div className="toolbar">
        {datos && <span className="count">{datos.length} rol(es)</span>}
        <span className="spacer" />
        {editable && <button className="btn btn-primary" onClick={() => setEditando({})}>Nuevo rol</button>}
      </div>
      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : datos?.length === 0 ? <Vacio>No hay roles.</Vacio> : (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Rol</th><th>Permisos</th><th>Usuarios</th><th className="col-actions" /></tr></thead>
            <tbody>
              {datos?.map((r) => (
                <tr key={r.id}>
                  <td>{r.nombre}{r.esSistema && <span className="pill pill-info" style={{ marginLeft: 8 }}>Sistema</span>}
                    {r.descripcion && <span className="sub">{r.descripcion}</span>}</td>
                  <td>{r.permisos.length} de {permisos.datos?.length ?? '…'}</td>
                  <td>{r.totalUsuarios ?? 0}</td>
                  <td>
                    {editable && !r.esSistema && (
                      <div className="row-actions">
                        <button className="btn-link" onClick={() => setEditando(r)}>Editar</button>
                        <button className="btn-link" style={{ color: 'var(--danger)', marginLeft: 10 }} onClick={() => eliminar(r)}>Eliminar</button>
                      </div>
                    )}
                    {(!editable || r.esSistema) && <button className="btn-link" onClick={() => setEditando({ ...r, soloVer: true })}>Ver</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {editando && (
        <FormRol rol={editando} permisos={permisos.datos || []} onCerrar={() => setEditando(null)}
          onGuardado={() => { setEditando(null); toast('Rol guardado'); recargar() }} />
      )}
    </>
  )
}

function FormRol({ rol, permisos, onCerrar, onGuardado }) {
  const nuevo = !rol.id
  const soloVer = !!rol.soloVer
  const [nombre, setNombre] = useState(rol.nombre || '')
  const [descripcion, setDescripcion] = useState(rol.descripcion || '')
  const [marcados, setMarcados] = useState(new Set((rol.permisos || []).map((p) => p.codigo)))
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  const porModulo = permisos.reduce((acc, p) => { (acc[p.modulo] ||= []).push(p); return acc }, {})

  function alternar(codigo) {
    const s = new Set(marcados)
    s.has(codigo) ? s.delete(codigo) : s.add(codigo)
    setMarcados(s)
  }

  async function guardar(e) {
    e.preventDefault()
    setEnviando(true)
    setError('')
    try {
      await api(nuevo ? '/roles' : `/roles/${rol.id}`, { method: nuevo ? 'POST' : 'PUT', body: { nombre, descripcion, permisos: [...marcados] } })
      onGuardado()
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  return (
    <Modal titulo={nuevo ? 'Nuevo rol' : soloVer ? rol.nombre : `Editar rol: ${rol.nombre}`} onCerrar={onCerrar} ancho="wide">
      <form onSubmit={guardar}>
        <Alerta>{error}</Alerta>
        {soloVer && rol.esSistema && <Alerta tipo="info">El rol del administrador del sistema tiene todos los permisos y no se puede modificar.</Alerta>}
        <div className="field-row">
          <Campo etiqueta="Nombre"><input required maxLength={50} value={nombre} onChange={(e) => setNombre(e.target.value)} disabled={soloVer} /></Campo>
          <Campo etiqueta="Descripción"><input maxLength={255} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} disabled={soloVer} /></Campo>
        </div>
        <div className="grid-2" style={{ gap: 14 }}>
          {Object.entries(porModulo).map(([modulo, lista]) => (
            <fieldset key={modulo} style={{ border: '1px solid var(--line)', borderRadius: 8, padding: '10px 14px 2px' }}>
              <legend className="count" style={{ padding: '0 6px' }}>{etiqueta(modulo)}</legend>
              {lista.map((p) => (
                <label key={p.codigo} className="check" style={{ marginBottom: 8, fontSize: 13.5 }}>
                  <input type="checkbox" checked={marcados.has(p.codigo)} onChange={() => alternar(p.codigo)} disabled={soloVer} />
                  {p.descripcion}
                </label>
              ))}
            </fieldset>
          ))}
        </div>
        <div style={{ height: 16 }} />
        {soloVer
          ? <div className="modal-actions"><button type="button" className="btn btn-ghost" onClick={onCerrar}>Cerrar</button></div>
          : <AccionesModal enviando={enviando} onCancelar={onCerrar} textoGuardar={nuevo ? 'Crear rol' : 'Guardar cambios'} />}
      </form>
    </Modal>
  )
}
