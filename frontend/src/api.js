// Conexión con el backend (Spring Boot). En desarrollo, Vite reenvía /api al puerto 8080.

const CLAVE_TOKEN = 'vigentis_token'

let alExpirarSesion = () => {}
export function configurarSesionExpirada(fn) { alExpirarSesion = fn }

export const token = {
  leer: () => sessionStorage.getItem(CLAVE_TOKEN),
  guardar: (t) => sessionStorage.setItem(CLAVE_TOKEN, t),
  borrar: () => sessionStorage.removeItem(CLAVE_TOKEN),
}

export class ApiError extends Error {
  constructor(mensaje, estado, campos) {
    super(mensaje)
    this.estado = estado
    this.campos = campos || {}
  }
}

/**
 * Llama a la API.
 *  api('/activos')                              -> GET
 *  api('/activos', { method: 'POST', body })    -> JSON
 *  api('/documentos/1/archivo', { method: 'POST', form })  -> archivo (FormData)
 */
export async function api(ruta, { method = 'GET', body, form, crudo = false } = {}) {
  const headers = {}
  const t = token.leer()
  if (t) headers.Authorization = `Bearer ${t}`
  let payload
  if (form) payload = form
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }

  let res
  try {
    res = await fetch(`/api${ruta}`, { method, headers, body: payload })
  } catch {
    throw new ApiError('No hay conexión con el servidor. ¿Está encendido el backend?', 0)
  }

  if (res.status === 401 && ruta !== '/auth/login') {
    token.borrar()
    alExpirarSesion()
  }
  if (!res.ok) {
    let mensaje = `Error ${res.status}`
    let campos
    try {
      const datos = await res.json()
      if (datos.error) mensaje = datos.error
      campos = datos.campos
    } catch { /* respuesta sin JSON */ }
    if (campos && Object.keys(campos).length) {
      mensaje = mensaje + ': ' + Object.entries(campos).map(([c, m]) => `${c} (${m})`).join(', ')
    }
    throw new ApiError(mensaje, res.status, campos)
  }
  if (crudo) return res
  if (res.status === 204) return null
  const tipo = res.headers.get('content-type') || ''
  return tipo.includes('json') ? res.json() : res.text()
}

/** Descarga un archivo de la API (reportes, documentos) respetando el nombre que envía el servidor. */
export async function descargar(ruta, nombrePorDefecto = 'archivo') {
  const res = await api(ruta, { crudo: true })
  const blob = await res.blob()
  const disposicion = res.headers.get('content-disposition') || ''
  let nombre = nombrePorDefecto
  const utf8 = disposicion.match(/filename\*=UTF-8''([^;]+)/i)
  const simple = disposicion.match(/filename="?([^";]+)"?/i)
  if (utf8) nombre = decodeURIComponent(utf8[1])
  else if (simple) nombre = simple[1]
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** Arma ?a=1&b=2 sin los valores vacíos. */
export function query(params) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.append(k, v)
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}
