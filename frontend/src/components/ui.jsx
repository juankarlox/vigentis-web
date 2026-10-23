import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { api } from '../api'
import { claseNivel, etiqueta } from '../util'

// ---------- Carga de datos ----------

/** Carga datos de la API y permite recargarlos. */
export function useDatos(cargar, deps = []) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(true)
  const [version, setVersion] = useState(0)
  const cargarRef = useRef(cargar)
  cargarRef.current = cargar

  useEffect(() => {
    let vigente = true
    setCargando(true)
    cargarRef.current()
      .then((d) => { if (vigente) { setDatos(d); setError('') } })
      .catch((e) => { if (vigente) setError(e.message) })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version])

  const recargar = useCallback(() => setVersion((v) => v + 1), [])
  return { datos, error, cargando, recargar }
}

/** Lista de usuarios activos (id y nombre) para los campos "responsable". */
let cacheResponsables = null
export function useResponsables() {
  const [lista, setLista] = useState(cacheResponsables || [])
  useEffect(() => {
    if (cacheResponsables) return
    api('/usuarios/opciones').then((l) => { cacheResponsables = l; setLista(l) }).catch(() => {})
  }, [])
  const nombre = (id) => (id ? (lista.find((u) => u.id === id)?.nombre || `Usuario ${id}`) : '—')
  return { lista, nombre }
}
export function olvidarResponsables() { cacheResponsables = null }

// ---------- Avisos flotantes ----------

const ToastContext = createContext(() => {})
export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null)
  const mostrar = useCallback((texto, tipo = 'ok') => {
    setToast({ texto, tipo })
    clearTimeout(window.__vigentisToast)
    window.__vigentisToast = setTimeout(() => setToast(null), 3500)
  }, [])
  return (
    <ToastContext.Provider value={mostrar}>
      {children}
      {toast && <div className={`toast ${toast.tipo === 'error' ? 'error' : ''}`} role="status">{toast.texto}</div>}
    </ToastContext.Provider>
  )
}
export function useToast() { return useContext(ToastContext) }

// ---------- Estructura ----------

export function Encabezado({ titulo, descripcion, children }) {
  return (
    <header className="view-head">
      <div>
        <h1>{titulo}</h1>
        {descripcion && <p className="view-desc">{descripcion}</p>}
      </div>
      {children && <div className="view-head-actions">{children}</div>}
    </header>
  )
}

export function Alerta({ tipo = 'error', children }) {
  if (!children) return null
  return <div className={`alert alert-${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>{children}</div>
}

export function Cargando() {
  return <div className="loading">Cargando…</div>
}

export function Vacio({ children }) {
  return <div className="empty">{children}</div>
}

export function Pestanas({ opciones, activa, onCambio }) {
  return (
    <div className="tabs" role="tablist">
      {opciones.map((o) => (
        <button key={o.id} role="tab" aria-selected={activa === o.id}
          className={`tab ${activa === o.id ? 'is-active' : ''}`} onClick={() => onCambio(o.id)}>
          {o.texto}
        </button>
      ))}
    </div>
  )
}

// ---------- Modal ----------

export function Modal({ titulo, onCerrar, ancho = '', children }) {
  useEffect(() => {
    const tecla = (e) => { if (e.key === 'Escape') onCerrar() }
    document.addEventListener('keydown', tecla)
    return () => document.removeEventListener('keydown', tecla)
  }, [onCerrar])
  return (
    <div className="modal-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) onCerrar() }}>
      <div className={`modal ${ancho}`} role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="modal-head">
          <h2>{titulo}</h2>
          <button className="modal-close" onClick={onCerrar} aria-label="Cerrar">×</button>
        </div>
        {children}
      </div>
    </div>
  )
}

// ---------- Campos de formulario ----------

export function Campo({ etiqueta: texto, ayuda, children }) {
  return (
    <label className="field">
      <span>{texto}</span>
      {children}
      {ayuda && <span className="field-help">{ayuda}</span>}
    </label>
  )
}

export function Selector({ valor, onCambio, opciones, vacio, required, disabled }) {
  return (
    <select value={valor ?? ''} onChange={(e) => onCambio(e.target.value)} required={required} disabled={disabled}>
      {vacio !== undefined && <option value="">{vacio}</option>}
      {opciones.map((o) => <option key={o.valor} value={o.valor}>{o.texto}</option>)}
    </select>
  )
}

/** Hook para formularios: guarda los valores y ofrece set('campo')(valor). */
export function useFormulario(inicial) {
  const [valores, setValores] = useState(inicial)
  const set = (campo) => (v) => setValores((a) => ({ ...a, [campo]: v?.target ? v.target.value : v }))
  return [valores, set, setValores]
}

/** Botones Guardar / Cancelar con estado de envío. */
export function AccionesModal({ enviando, onCancelar, textoGuardar = 'Guardar', children }) {
  return (
    <div className="modal-actions">
      {children}
      <button type="button" className="btn btn-ghost" onClick={onCancelar}>Cancelar</button>
      <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Guardando…' : textoGuardar}</button>
    </div>
  )
}

// ---------- Indicadores ----------

/** Nivel con punto de color + texto (nunca solo color). */
export function Nivel({ nivel, valor }) {
  if (!nivel) return <span className="muted">—</span>
  return (
    <span className={`level level-${claseNivel(nivel)}`}>
      <span className={`dot dot-${claseNivel(nivel)}`} aria-hidden="true" />
      {etiqueta(nivel)}{valor ? ` (${valor})` : ''}
    </span>
  )
}

const CLASE_ESTADO = {
  // verde
  IMPLEMENTADO: 'ok', APROBADO: 'ok', CERRADO: 'ok', MITIGADO: 'ok', ACTIVO: 'ok',
  // amarillo
  EN_PROGRESO: 'progress', EN_TRATAMIENTO: 'progress', EN_ATENCION: 'progress', EN_PROCESO: 'progress',
  BORRADOR: 'progress', EN_MANTENIMIENTO: 'progress',
  // rojo
  NO_IMPLEMENTADO: 'none', ABIERTO: 'none',
  // neutro
  ACEPTADO: 'info', OBSOLETO: 'neutral', DADO_DE_BAJA: 'neutral',
}

export function Estado({ valor }) {
  return <span className={`pill pill-${CLASE_ESTADO[valor] || 'neutral'}`}>{etiqueta(valor)}</span>
}
