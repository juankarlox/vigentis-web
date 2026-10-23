import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import Layout from './components/Layout'
import { Cargando } from './components/ui'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Activos from './pages/Activos'
import Riesgos from './pages/Riesgos'
import Criterios from './pages/Criterios'
import Controles from './pages/Controles'
import Incidentes from './pages/Incidentes'
import Auditorias from './pages/Auditorias'
import Documentos from './pages/Documentos'
import Reportes from './pages/Reportes'
import Historial from './pages/Historial'
import Usuarios from './pages/Usuarios'
import MiCuenta from './pages/MiCuenta'

/** Muestra la página solo si el usuario tiene alguno de los permisos. */
function Protegida({ permisos, children }) {
  const { puede } = useAuth()
  if (permisos && !puede(...permisos)) {
    return <div className="alert alert-error">Tu rol no tiene permiso para ver esta sección.</div>
  }
  return children
}

export default function App() {
  const { usuario, cargando } = useAuth()

  if (cargando) return <div className="main"><Cargando /></div>
  if (!usuario) return <Login />

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="activos" element={<Protegida permisos={['ACTIVOS_VER']}><Activos /></Protegida>} />
        <Route path="riesgos" element={<Protegida permisos={['RIESGOS_VER']}><Riesgos /></Protegida>} />
        <Route path="criterios" element={<Protegida permisos={['RIESGOS_VER']}><Criterios /></Protegida>} />
        <Route path="controles" element={<Protegida permisos={['CONTROLES_VER']}><Controles /></Protegida>} />
        <Route path="incidentes" element={<Protegida permisos={['INCIDENTES_VER']}><Incidentes /></Protegida>} />
        <Route path="auditorias" element={<Protegida permisos={['AUDITORIAS_VER']}><Auditorias /></Protegida>} />
        <Route path="documentos" element={<Protegida permisos={['DOCUMENTOS_VER']}><Documentos /></Protegida>} />
        <Route path="reportes" element={<Protegida permisos={['REPORTES_VER']}><Reportes /></Protegida>} />
        <Route path="historial" element={<Protegida permisos={['HISTORIAL_VER']}><Historial /></Protegida>} />
        <Route path="usuarios" element={<Protegida permisos={['USUARIOS_GESTIONAR', 'ROLES_GESTIONAR']}><Usuarios /></Protegida>} />
        <Route path="mi-cuenta" element={<MiCuenta />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
