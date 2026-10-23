import { useState } from 'react'
import { api, query } from '../api'
import { Alerta, Cargando, Encabezado, Selector, Vacio, useDatos } from '../components/ui'
import { ACCIONES_HISTORIAL, ENTIDADES_HISTORIAL, etiqueta, fechaHora, opciones } from '../util'

export default function Historial() {
  const [filtros, setFiltros] = useState({ entidad: '', accion: '', q: '', desde: '', hasta: '' })
  const { datos, error, cargando } = useDatos(() => api(`/historial${query({ ...filtros, limite: 300 })}`), [filtros])
  const cambiar = (campo) => (v) => setFiltros({ ...filtros, [campo]: v?.target ? v.target.value : v })

  return (
    <>
      <Encabezado titulo="Historial"
        descripcion="Trazabilidad de todas las acciones: quién hizo qué y cuándo. No se puede editar ni borrar." />
      <div className="toolbar">
        <input type="search" placeholder="Buscar en el detalle o código" value={filtros.q} onChange={cambiar('q')} />
        <Selector valor={filtros.entidad} onCambio={cambiar('entidad')} vacio="Todos los módulos" opciones={opciones(ENTIDADES_HISTORIAL)} />
        <Selector valor={filtros.accion} onCambio={cambiar('accion')} vacio="Todas las acciones" opciones={opciones(ACCIONES_HISTORIAL)} />
        <label className="count">Desde <input type="date" value={filtros.desde} onChange={cambiar('desde')} /></label>
        <label className="count">Hasta <input type="date" value={filtros.hasta} onChange={cambiar('hasta')} /></label>
      </div>
      <Alerta>{error}</Alerta>
      {cargando && !datos ? <Cargando /> : (
        <div className="table-wrap">
          {datos?.length === 0 ? <Vacio>No hay registros con esos filtros.</Vacio> : (
            <table className="data">
              <thead><tr><th>Fecha</th><th>Usuario</th><th>Módulo</th><th>Acción</th><th>Código</th><th>Detalle</th></tr></thead>
              <tbody>
                {datos?.map((h) => (
                  <tr key={h.id}>
                    <td className="nowrap"><code>{fechaHora(h.fecha)}</code></td>
                    <td>{h.usuarioNombre || <span className="muted">Sistema</span>}</td>
                    <td>{etiqueta(h.entidad)}</td>
                    <td>{etiqueta(h.accion)}</td>
                    <td>{h.entidadCodigo ? <code>{h.entidadCodigo}</code> : <span className="muted">—</span>}</td>
                    <td>{h.detalle}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </>
  )
}
