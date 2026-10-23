// Textos y formatos que se repiten en toda la aplicación

const ETIQUETAS = {
  // generales
  ACTIVO: 'Activo', EN_MANTENIMIENTO: 'En mantenimiento', DADO_DE_BAJA: 'Dado de baja',
  SERVIDOR: 'Servidor', SOFTWARE: 'Software', EQUIPO: 'Equipo', RED: 'Red',
  ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja', CRITICA: 'Crítica',
  ALTO: 'Alto', MEDIO: 'Medio', BAJO: 'Bajo', CRITICO: 'Crítico',
  // riesgos
  ABIERTO: 'Abierto', EN_TRATAMIENTO: 'En tratamiento', MITIGADO: 'Mitigado', ACEPTADO: 'Aceptado', CERRADO: 'Cerrado',
  MITIGAR: 'Mitigar', ACEPTAR: 'Aceptar', TRANSFERIR: 'Transferir', EVITAR: 'Evitar',
  CREADO: 'Creado', EVALUADO: 'Evaluado', CAMBIO_ESTADO: 'Cambio de estado', TRATAMIENTO: 'Tratamiento', COMENTARIO: 'Comentario',
  ACEPTACION: 'Aceptación formal', REVISION: 'Revisión periódica', CRITERIOS: 'Criterios de riesgo',
  // controles
  ORGANIZACIONAL: 'Organizacional', PERSONAS: 'Personas', FISICO: 'Físico', TECNOLOGICO: 'Tecnológico',
  NO_IMPLEMENTADO: 'No implementado', EN_PROGRESO: 'En progreso', IMPLEMENTADO: 'Implementado',
  // incidentes
  PHISHING: 'Phishing', MALWARE: 'Malware', ACCESO_NO_AUTORIZADO: 'Acceso no autorizado',
  FUGA_DE_DATOS: 'Fuga de datos', CAIDA_DE_SERVICIO: 'Caída de servicio', OTRO: 'Otro', EN_ATENCION: 'En atención',
  // auditorías
  INTERNA: 'Interna', EXTERNA: 'Externa', NC_MAYOR: 'No conformidad mayor', NC_MENOR: 'No conformidad menor',
  OBSERVACION: 'Observación', OPORTUNIDAD_MEJORA: 'Oportunidad de mejora', EN_PROCESO: 'En proceso',
  // documentos
  POLITICA: 'Política', PROCEDIMIENTO: 'Procedimiento', REGISTRO: 'Registro', EVIDENCIA: 'Evidencia',
  BORRADOR: 'Borrador', APROBADO: 'Aprobado', OBSOLETO: 'Obsoleto',
  // historial
  CREAR: 'Crear', EDITAR: 'Editar', ELIMINAR: 'Eliminar', IMPORTAR: 'Importar', INICIAR_SESION: 'Inicio de sesión',
  AUDITORIA: 'Auditoría', HALLAZGO: 'Hallazgo', DOCUMENTO: 'Documento', INCIDENTE: 'Incidente',
  RIESGO: 'Riesgo', CONTROL: 'Control', USUARIO: 'Usuario', ROL: 'Rol',
}

export function etiqueta(valor) {
  if (valor === null || valor === undefined || valor === '') return '—'
  return ETIQUETAS[valor] || String(valor).replaceAll('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase())
}

/** Opciones para un <select> a partir de una lista de valores. */
export function opciones(valores) {
  return valores.map((v) => ({ valor: v, texto: etiqueta(v) }))
}

export const TIPOS_ACTIVO = ['SERVIDOR', 'SOFTWARE', 'EQUIPO', 'RED']
export const ESTADOS_ACTIVO = ['ACTIVO', 'EN_MANTENIMIENTO', 'DADO_DE_BAJA']
export const ESTADOS_RIESGO = ['ABIERTO', 'EN_TRATAMIENTO', 'MITIGADO', 'ACEPTADO', 'CERRADO']
export const OPCIONES_TRATAMIENTO = ['MITIGAR', 'ACEPTAR', 'TRANSFERIR', 'EVITAR']
export const TEMAS_CONTROL = ['ORGANIZACIONAL', 'PERSONAS', 'FISICO', 'TECNOLOGICO']
export const ESTADOS_CONTROL = ['NO_IMPLEMENTADO', 'EN_PROGRESO', 'IMPLEMENTADO']
export const TIPOS_INCIDENTE = ['PHISHING', 'MALWARE', 'ACCESO_NO_AUTORIZADO', 'FUGA_DE_DATOS', 'CAIDA_DE_SERVICIO', 'OTRO']
export const SEVERIDADES = ['CRITICA', 'ALTA', 'MEDIA', 'BAJA']
export const ESTADOS_INCIDENTE = ['ABIERTO', 'EN_ATENCION', 'CERRADO']
export const TIPOS_AUDITORIA = ['INTERNA', 'EXTERNA']
export const TIPOS_HALLAZGO = ['NC_MAYOR', 'NC_MENOR', 'OBSERVACION', 'OPORTUNIDAD_MEJORA']
export const ESTADOS_HALLAZGO = ['ABIERTO', 'EN_PROCESO', 'CERRADO']
export const TIPOS_DOCUMENTO = ['POLITICA', 'PROCEDIMIENTO', 'REGISTRO', 'EVIDENCIA', 'OTRO']
export const ESTADOS_DOCUMENTO = ['BORRADOR', 'APROBADO', 'OBSOLETO']
export const ENTIDADES_HISTORIAL = ['ACTIVO', 'RIESGO', 'CRITERIOS', 'CONTROL', 'INCIDENTE', 'AUDITORIA', 'HALLAZGO', 'DOCUMENTO', 'USUARIO', 'ROL']
export const ACCIONES_HISTORIAL = ['CREAR', 'EDITAR', 'ELIMINAR', 'IMPORTAR', 'INICIAR_SESION']

// Criterios de riesgo de la organización (se cargan al iniciar sesión)
let criterios = { umbralMedio: 5, umbralAlto: 10, umbralCritico: 20, nivelAceptable: 4, mesesRevision: 6 }
export function usarCriterios(c) { if (c) criterios = c }
export function criteriosActuales() { return criterios }

/** Nivel de riesgo a partir de probabilidad x impacto, con los umbrales de la organización. */
export function nivelRiesgo(valor, c = criterios) {
  if (!valor) return null
  if (valor >= c.umbralCritico) return 'CRITICO'
  if (valor >= c.umbralAlto) return 'ALTO'
  if (valor >= c.umbralMedio) return 'MEDIO'
  return 'BAJO'
}

/** Rangos de texto para las leyendas: { BAJO: '1–4', ... } */
export function rangosNivel(c = criterios) {
  return {
    BAJO: `1–${c.umbralMedio - 1}`,
    MEDIO: `${c.umbralMedio}–${c.umbralAlto - 1}`,
    ALTO: `${c.umbralAlto}–${c.umbralCritico - 1}`,
    CRITICO: `${c.umbralCritico}–25`,
  }
}

/** Escala común (crítico/alto/medio/bajo) -> sufijo de clase CSS. */
export function claseNivel(nivel) {
  return { CRITICO: 'crit', CRITICA: 'crit', ALTO: 'high', ALTA: 'high', MEDIO: 'med', MEDIA: 'med', BAJO: 'low', BAJA: 'low' }[nivel] || 'low'
}

const fmtFecha = new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric' })
const fmtFechaHora = new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false })

/** "2026-10-08" o "2026-10-08T13:46:55" -> fecha local legible. */
export function fecha(iso) {
  if (!iso) return '—'
  const soloFecha = /^\d{4}-\d{2}-\d{2}$/.test(iso)
  const d = soloFecha ? new Date(`${iso}T00:00:00`) : new Date(iso)
  return isNaN(d) ? iso : fmtFecha.format(d)
}

export function fechaHora(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return isNaN(d) ? iso : fmtFechaHora.format(d)
}

/** Valor para <input type="datetime-local"> a partir de una fecha del backend. */
export function aInputFechaHora(iso) {
  return iso ? iso.slice(0, 16) : ''
}

/** Fecha y hora local actual en formato del input datetime-local. */
export function ahoraInput() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

export function hoyInput() {
  return ahoraInput().slice(0, 10)
}

export function tamano(bytes) {
  if (bytes === null || bytes === undefined) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/** Convierte '' en null y números en texto a número (para enviar formularios). */
export function vacioANull(v) {
  return v === '' || v === undefined ? null : v
}
export function numeroONull(v) {
  return v === '' || v === null || v === undefined ? null : Number(v)
}
