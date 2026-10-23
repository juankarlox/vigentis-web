import { useState } from 'react'
import { descargar } from '../api'
import { useAuth } from '../auth'
import { Encabezado, useToast } from '../components/ui'

const REPORTES = [
  { ruta: 'activos', titulo: 'Inventario de activos', texto: 'Todos los activos con su valoración C / I / D, criticidad y estado.', permiso: 'ACTIVOS_VER' },
  { ruta: 'riesgos', titulo: 'Matriz de riesgos', texto: 'Riesgos con nivel inherente, tratamiento, nivel residual y estado.', permiso: 'RIESGOS_VER' },
  { ruta: 'controles', titulo: 'Declaración de Aplicabilidad', texto: 'Los 93 controles del Anexo A: si aplican, justificación y estado.', permiso: 'CONTROLES_VER' },
  { ruta: 'incidentes', titulo: 'Incidentes', texto: 'Incidentes con severidad, fechas de detección y cierre y acciones tomadas.', permiso: 'INCIDENTES_VER' },
  { ruta: 'hallazgos', titulo: 'Hallazgos de auditoría', texto: 'Hallazgos con su control, fecha límite, estado y acción correctiva.', permiso: 'AUDITORIAS_VER' },
  { ruta: 'documentos', titulo: 'Documentos del SGSI', texto: 'Documentos con versión, estado, fecha de aprobación y de revisión.', permiso: 'DOCUMENTOS_VER' },
  { ruta: 'historial', titulo: 'Historial de cambios', texto: 'Las últimas 1.000 acciones registradas con usuario y fecha.', permiso: 'HISTORIAL_VER' },
]

export default function Reportes() {
  const { puede } = useAuth()
  const toast = useToast()
  const [descargando, setDescargando] = useState('')

  async function bajar(r) {
    setDescargando(r.ruta)
    try { await descargar(`/reportes/${r.ruta}`, `${r.ruta}.csv`) }
    catch (e) { toast(e.message, 'error') }
    finally { setDescargando('') }
  }

  return (
    <>
      <Encabezado titulo="Reportes"
        descripcion="Descarga la información en CSV para abrirla en Excel o adjuntarla a un informe. Cada archivo lleva la fecha del día." />
      <div className="grid-3">
        {REPORTES.map((r) => (
          <section className="card" key={r.ruta}>
            <h3>{r.titulo}</h3>
            <p>{r.texto}</p>
            <button className="btn btn-ghost btn-sm" onClick={() => bajar(r)}
              disabled={descargando === r.ruta || (r.permiso && !puede(r.permiso))}>
              {descargando === r.ruta ? 'Descargando…' : 'Descargar CSV'}
            </button>
          </section>
        ))}
      </div>
    </>
  )
}
