import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'

const MENU = [
  { grupo: 'Gestión' },
  { ruta: '/', texto: 'Resumen' },
  { ruta: '/activos', texto: 'Activos', permiso: 'ACTIVOS_VER' },
  { ruta: '/riesgos', texto: 'Riesgos', permiso: 'RIESGOS_VER' },
  { ruta: '/criterios', texto: 'Criterios de riesgo', permiso: 'RIESGOS_VER' },
  { ruta: '/controles', texto: 'Controles ISO', permiso: 'CONTROLES_VER' },
  { ruta: '/incidentes', texto: 'Incidentes', permiso: 'INCIDENTES_VER' },
  { grupo: 'Cumplimiento' },
  { ruta: '/auditorias', texto: 'Auditorías', permiso: 'AUDITORIAS_VER' },
  { ruta: '/documentos', texto: 'Documentos', permiso: 'DOCUMENTOS_VER' },
  { ruta: '/reportes', texto: 'Reportes', permiso: 'REPORTES_VER' },
  { ruta: '/historial', texto: 'Historial', permiso: 'HISTORIAL_VER' },
  { grupo: 'Administración', permisos: ['USUARIOS_GESTIONAR', 'ROLES_GESTIONAR'] },
  { ruta: '/usuarios', texto: 'Usuarios y roles', permisos: ['USUARIOS_GESTIONAR', 'ROLES_GESTIONAR'] },
]

function iniciales(nombre) {
  return nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('')
}

export default function Layout() {
  const { usuario, puede, salir } = useAuth()

  const visibles = MENU.filter((m) => {
    if (m.permiso) return puede(m.permiso)
    if (m.permisos) return puede(...m.permisos)
    return true
  })

  return (
    <div className="shell">
      <aside className="rail">
        <div className="rail-brand">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M12 2.5 4 5.5v6c0 5.2 3.4 8.9 8 10 4.6-1.1 8-4.8 8-10v-6L12 2.5Z" />
            </svg>
          </span>
          <span className="brand-name">Vigentis</span>
        </div>
        <p className="rail-tagline">
          <span className="rail-empresa">{usuario.empresa}</span>
          Gestión de ciberseguridad basada en ISO/IEC 27001
        </p>

        <nav className="rail-nav" aria-label="Secciones">
          {visibles.map((m) =>
            m.grupo
              ? <div key={m.grupo} className="rail-group">{m.grupo}</div>
              : (
                <NavLink key={m.ruta} to={m.ruta} end={m.ruta === '/'}
                  className={({ isActive }) => `rail-link ${isActive ? 'is-active' : ''}`}>
                  {m.texto}
                </NavLink>
              ))}
        </nav>

        <div className="rail-foot">
          <div className="rail-user">
            <span className="rail-avatar" aria-hidden="true">{iniciales(usuario.nombre)}</span>
            <span className="rail-user-info">
              <span className="rail-user-name">{usuario.nombre}</span>
              <span className="rail-user-role">{usuario.rol}</span>
            </span>
          </div>
          <div className="rail-actions">
            <NavLink to="/mi-cuenta" className="btn btn-ghost">Mi cuenta</NavLink>
            <button className="btn btn-ghost" onClick={() => salir()}>Cerrar sesión</button>
          </div>
        </div>
      </aside>

      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
