import { useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'
import { Alerta, Campo, Encabezado } from '../components/ui'

export default function MiCuenta() {
  const { usuario } = useAuth()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [repetir, setRepetir] = useState('')
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function cambiar(e) {
    e.preventDefault()
    setError('')
    setOk('')
    if (nueva !== repetir) { setError('Las contraseñas nuevas no coinciden'); return }
    setEnviando(true)
    try {
      await api('/auth/clave', { method: 'PUT', body: { actual, nueva } })
      setOk('Contraseña cambiada correctamente.')
      setActual(''); setNueva(''); setRepetir('')
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  return (
    <>
      <Encabezado titulo="Mi cuenta" />
      <div className="grid-2">
        <section className="card">
          <h3>Mis datos</h3>
          <dl className="kv" style={{ marginTop: 12 }}>
            <dt>Empresa</dt><dd>{usuario.empresa}</dd>
            <dt>Nombre</dt><dd>{usuario.nombre}</dd>
            <dt>Correo</dt><dd>{usuario.correo}</dd>
            <dt>Rol</dt><dd>{usuario.rol}</dd>
            <dt>Permisos</dt><dd>{usuario.permisos.length}</dd>
          </dl>
        </section>
        <section className="card">
          <h3>Cambiar contraseña</h3>
          <p>Mínimo 10 caracteres, combinando letras y números.</p>
          <form onSubmit={cambiar}>
            <Alerta>{error}</Alerta>
            <Alerta tipo="ok">{ok}</Alerta>
            <Campo etiqueta="Contraseña actual"><input type="password" autoComplete="current-password" required value={actual} onChange={(e) => setActual(e.target.value)} /></Campo>
            <Campo etiqueta="Nueva contraseña"><input type="password" autoComplete="new-password" required minLength={10} value={nueva} onChange={(e) => setNueva(e.target.value)} /></Campo>
            <Campo etiqueta="Repite la nueva contraseña"><input type="password" autoComplete="new-password" required value={repetir} onChange={(e) => setRepetir(e.target.value)} /></Campo>
            <button className="btn btn-primary" disabled={enviando}>{enviando ? 'Guardando…' : 'Cambiar contraseña'}</button>
          </form>
        </section>
      </div>
    </>
  )
}
