import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, configurarSesionExpirada, token } from './api'
import { usarCriterios } from './util'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [aviso, setAviso] = useState('')
  const [criterios, setCriterios] = useState(null)

  // Los criterios de riesgo se usan en varias pantallas para clasificar los niveles
  const cargarCriterios = useCallback(() =>
    api('/criterios-riesgo').then((c) => { usarCriterios(c); setCriterios(c) }).catch(() => {}), [])

  const salir = useCallback((mensaje = '') => {
    token.borrar()
    setUsuario(null)
    setAviso(mensaje)
  }, [])

  useEffect(() => {
    configurarSesionExpirada(() => salir('Tu sesión expiró. Vuelve a iniciar sesión.'))
    if (!token.leer()) { setCargando(false); return }
    api('/auth/yo')
      .then(async (u) => { await cargarCriterios(); setUsuario(u) })
      .catch(() => token.borrar())
      .finally(() => setCargando(false))
  }, [salir, cargarCriterios])

  async function entrar(correo, clave) {
    const sesion = await api('/auth/login', { method: 'POST', body: { correo, clave } })
    token.guardar(sesion.token)
    await cargarCriterios()
    setAviso('')
    setUsuario(sesion.usuario)
  }

  // "Registrar mi empresa": crea la empresa y entra directamente como su administrador
  async function registrar(datos) {
    const sesion = await api('/auth/registro', { method: 'POST', body: datos })
    token.guardar(sesion.token)
    await cargarCriterios()
    setAviso('')
    setUsuario(sesion.usuario)
  }

  const puede = useCallback((...permisos) =>
    !!usuario && permisos.some((p) => usuario.permisos.includes(p)), [usuario])

  return (
    <AuthContext.Provider value={{ usuario, cargando, aviso, entrar, registrar, salir, puede, criterios, cargarCriterios }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
