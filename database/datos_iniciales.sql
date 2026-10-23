USE vigentis;

-- Permisos que existen en Vigentis (los define el sistema)
INSERT INTO permisos (codigo, modulo, descripcion) VALUES
('ACTIVOS_VER',          'ACTIVOS',     'Ver activos'),
('ACTIVOS_GESTIONAR',    'ACTIVOS',     'Crear, editar y eliminar activos'),
('RIESGOS_VER',          'RIESGOS',     'Ver riesgos y su bitácora'),
('RIESGOS_GESTIONAR',    'RIESGOS',     'Crear, evaluar y dar seguimiento a riesgos'),
('CONTROLES_VER',        'CONTROLES',   'Ver controles ISO/IEC 27001'),
('CONTROLES_GESTIONAR',  'CONTROLES',   'Actualizar estado y aplicabilidad de controles'),
('INCIDENTES_VER',       'INCIDENTES',  'Ver incidentes'),
('INCIDENTES_GESTIONAR', 'INCIDENTES',  'Registrar y atender incidentes'),
('AUDITORIAS_VER',       'AUDITORIAS',  'Ver auditorías y hallazgos'),
('AUDITORIAS_IMPORTAR',  'AUDITORIAS',  'Importar resultados de auditorías'),
('DOCUMENTOS_VER',       'DOCUMENTOS',  'Ver documentos'),
('DOCUMENTOS_GESTIONAR', 'DOCUMENTOS',  'Subir y editar documentos'),
('DOCUMENTOS_APROBAR',   'DOCUMENTOS',  'Aprobar documentos'),
('REPORTES_VER',         'REPORTES',    'Ver resumen y generar reportes'),
('HISTORIAL_VER',        'HISTORIAL',   'Ver el historial de acciones'),
('USUARIOS_GESTIONAR',   'USUARIOS',    'Crear y administrar usuarios'),
('ROLES_GESTIONAR',      'USUARIOS',    'Crear roles y asignar permisos');

-- Roles de ejemplo (cada empresa puede editarlos, borrarlos o crear otros)
INSERT INTO roles (nombre, descripcion, es_sistema) VALUES
('Administrador de TI',        'Acceso total, incluida la administración de usuarios y roles', TRUE),
('Analista de ciberseguridad', 'Evalúa riesgos y atiende incidentes', FALSE),
('Responsable del SGSI',       'Gestiona controles, documentos y auditorías', FALSE),
('Auditor',                    'Solo consulta, con acceso al historial', FALSE);

-- Administrador de TI: todos los permisos
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.nombre = 'Administrador de TI';

-- Analista de ciberseguridad
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p
  ON p.codigo IN ('ACTIVOS_VER', 'RIESGOS_VER', 'RIESGOS_GESTIONAR', 'CONTROLES_VER',
                  'INCIDENTES_VER', 'INCIDENTES_GESTIONAR', 'DOCUMENTOS_VER', 'REPORTES_VER')
WHERE r.nombre = 'Analista de ciberseguridad';

-- Responsable del SGSI
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p
  ON p.codigo IN ('ACTIVOS_VER', 'RIESGOS_VER', 'CONTROLES_VER', 'CONTROLES_GESTIONAR',
                  'INCIDENTES_VER', 'AUDITORIAS_VER', 'AUDITORIAS_IMPORTAR',
                  'DOCUMENTOS_VER', 'DOCUMENTOS_GESTIONAR', 'DOCUMENTOS_APROBAR',
                  'REPORTES_VER', 'HISTORIAL_VER')
WHERE r.nombre = 'Responsable del SGSI';

-- Auditor: todo lo de "ver", nada de modificar
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p
  ON p.codigo LIKE '%\_VER'
WHERE r.nombre = 'Auditor';