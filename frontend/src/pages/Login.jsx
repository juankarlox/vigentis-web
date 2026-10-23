import { useState } from 'react'
import { useAuth } from '../auth'
import { Alerta, Campo } from '../components/ui'

const SECTORES = ['Tecnología', 'Salud', 'Educación', 'Financiero', 'Comercio', 'Manufactura',
  'Gobierno / sector público', 'Servicios', 'Otro']

export default function Login() {
  const [modo, setModo] = useState('entrar')   // 'entrar' | 'registrar'
  return (
    <div className="login-page">
      <section className="login-side">
        <div className="rail-brand">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M12 2.5 4 5.5v6c0 5.2 3.4 8.9 8 10 4.6-1.1 8-4.8 8-10v-6L12 2.5Z" />
            </svg>
          </span>
          <span className="brand-name">Vigentis</span>
        </div>
        <div>
          <h2>No se puede proteger lo que no se conoce.</h2>
          <p>Activos, riesgos, controles, incidentes, auditorías y documentos de seguridad de tu empresa en un solo lugar, con base en ISO/IEC 27001.</p>
          {modo === 'registrar' && (
            <ol className="login-steps">
              <li>Registras tu empresa y quedas como administrador.</li>
              <li>Tu espacio arranca con los 93 controles ISO, roles y criterios de riesgo listos.</li>
              <li>Creas las cuentas de tu equipo, cada una con su rol.</li>
            </ol>
          )}
        </div>
        <p className="rail-note">Vigentis 1.0 · Proyecto Integrador I</p>
      </section>

      <section className="login-form-wrap">
        {modo === 'entrar'
          ? <FormEntrar onRegistrar={() => setModo('registrar')} />
          : <FormRegistro onEntrar={() => setModo('entrar')} />}
      </section>
    </div>
  )
}

function FormEntrar({ onRegistrar }) {
  const { entrar, aviso } = useAuth()
  const [correo, setCorreo] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await entrar(correo, clave)
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="login-form" onSubmit={enviar}>
      <h1>Iniciar sesión</h1>
      <p>Entra con el correo y la contraseña que te dio el administrador de tu empresa.</p>
      <Alerta tipo="info">{aviso}</Alerta>
      <Alerta>{error}</Alerta>
      <Campo etiqueta="Correo">
        <input type="email" autoComplete="username" required value={correo} onChange={(e) => setCorreo(e.target.value)} />
      </Campo>
      <Campo etiqueta="Contraseña">
        <input type="password" autoComplete="current-password" required value={clave} onChange={(e) => setClave(e.target.value)} />
      </Campo>
      <button className="btn btn-primary" disabled={enviando}>{enviando ? 'Entrando…' : 'Entrar'}</button>
      <p className="login-switch">
        ¿Tu empresa aún no usa Vigentis? <button type="button" className="btn-link" onClick={onRegistrar}>Registrar mi empresa</button>
      </p>
    </form>
  )
}

function FormRegistro({ onEntrar }) {
  const { registrar } = useAuth()
  const [v, setV] = useState({ empresa: '', nit: '', sector: '', nombre: '', correo: '', clave: '', repetir: '' })
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const set = (campo) => (e) => setV({ ...v, [campo]: e.target.value })

  async function enviar(e) {
    e.preventDefault()
    setError('')
    if (v.clave !== v.repetir) { setError('Las contraseñas no coinciden'); return }
    setEnviando(true)
    try {
      const { repetir, ...datos } = v
      await registrar(datos)
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="login-form wide" onSubmit={enviar}>
      <h1>Registrar mi empresa</h1>
      <p>Crea el espacio de tu empresa en Vigentis. Tú quedarás como administrador.</p>
      <Alerta>{error}</Alerta>
      <Campo etiqueta="Nombre de la empresa">
        <input required maxLength={150} value={v.empresa} onChange={set('empresa')} autoComplete="organization" />
      </Campo>
      <div className="field-row">
        <Campo etiqueta="NIT (opcional)"><input maxLength={30} value={v.nit} onChange={set('nit')} /></Campo>
        <Campo etiqueta="Sector">
          <select value={v.sector} onChange={set('sector')}>
            <option value="">Elegir…</option>
            {SECTORES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </Campo>
      </div>
      <Campo etiqueta="Tu nombre completo">
        <input required maxLength={100} value={v.nombre} onChange={set('nombre')} autoComplete="name" />
      </Campo>
      <Campo etiqueta="Tu correo">
        <input type="email" required maxLength={150} value={v.correo} onChange={set('correo')} autoComplete="email" />
      </Campo>
      <div className="field-row">
        <Campo etiqueta="Contraseña" ayuda="Mínimo 10 caracteres, con letras y números.">
          <input type="password" required minLength={10} value={v.clave} onChange={set('clave')} autoComplete="new-password" />
        </Campo>
        <Campo etiqueta="Repite la contraseña">
          <input type="password" required value={v.repetir} onChange={set('repetir')} autoComplete="new-password" />
        </Campo>
      </div>
      <button className="btn btn-primary" disabled={enviando}>{enviando ? 'Creando tu espacio…' : 'Crear mi empresa'}</button>
      <p className="login-switch">
        ¿Ya tienes cuenta? <button type="button" className="btn-link" onClick={onEntrar}>Iniciar sesión</button>
      </p>
    </form>
  )
}
