USE vigentis;

-- Solo para desarrollo: borra y recrea las tablas (todavía no hay datos)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS historial;
DROP TABLE IF EXISTS documento_control;
DROP TABLE IF EXISTS documentos;
DROP TABLE IF EXISTS hallazgos;
DROP TABLE IF EXISTS auditorias;
DROP TABLE IF EXISTS incidentes;
DROP TABLE IF EXISTS riesgo_control;
DROP TABLE IF EXISTS riesgo_bitacora;
DROP TABLE IF EXISTS riesgos;
DROP TABLE IF EXISTS controles;
DROP TABLE IF EXISTS activos;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS rol_permiso;
DROP TABLE IF EXISTS permisos;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

-- ================= ROLES, PERMISOS Y USUARIOS =================
CREATE TABLE roles (
  id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
  nombre       VARCHAR(50)  NOT NULL UNIQUE,
  descripcion  VARCHAR(255),
  es_sistema   BOOLEAN      NOT NULL DEFAULT FALSE,
  creado_en    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permisos (
  id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
  codigo       VARCHAR(50)  NOT NULL UNIQUE,
  modulo       VARCHAR(30)  NOT NULL,
  descripcion  VARCHAR(255) NOT NULL
);

CREATE TABLE rol_permiso (
  rol_id      BIGINT NOT NULL,
  permiso_id  BIGINT NOT NULL,
  PRIMARY KEY (rol_id, permiso_id),
  CONSTRAINT fk_rp_rol     FOREIGN KEY (rol_id)     REFERENCES roles(id)    ON DELETE CASCADE,
  CONSTRAINT fk_rp_permiso FOREIGN KEY (permiso_id) REFERENCES permisos(id) ON DELETE CASCADE
);

CREATE TABLE usuarios (
  id                 BIGINT       AUTO_INCREMENT PRIMARY KEY,
  nombre             VARCHAR(100) NOT NULL,
  correo             VARCHAR(150) NOT NULL UNIQUE,
  password_hash      VARCHAR(255) NOT NULL,
  rol_id             BIGINT       NOT NULL,
  activo             BOOLEAN      NOT NULL DEFAULT TRUE,
  ultimo_acceso      DATETIME,
  intentos_fallidos  INT          NOT NULL DEFAULT 0,
  bloqueado_hasta    DATETIME,
  creado_en          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_usuarios_rol
    FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE RESTRICT
);

-- ================= ACTIVOS =================
CREATE TABLE activos (
  id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
  codigo            VARCHAR(20)  NOT NULL UNIQUE,
  nombre            VARCHAR(150) NOT NULL,
  tipo              VARCHAR(20)  NOT NULL,
  confidencialidad  TINYINT      NOT NULL DEFAULT 2,
  integridad        TINYINT      NOT NULL DEFAULT 2,
  disponibilidad    TINYINT      NOT NULL DEFAULT 2,
  criticidad        VARCHAR(10)  AS (
                      CASE GREATEST(confidencialidad, integridad, disponibilidad)
                        WHEN 3 THEN 'ALTA' WHEN 2 THEN 'MEDIA' ELSE 'BAJA' END
                    ) STORED,
  estado            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  descripcion       TEXT,
  responsable_id    BIGINT,
  creado_en         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_activos_tipo   CHECK (tipo IN ('SERVIDOR', 'SOFTWARE', 'EQUIPO', 'RED')),
  CONSTRAINT chk_activos_c      CHECK (confidencialidad BETWEEN 1 AND 3),
  CONSTRAINT chk_activos_i      CHECK (integridad BETWEEN 1 AND 3),
  CONSTRAINT chk_activos_d      CHECK (disponibilidad BETWEEN 1 AND 3),
  CONSTRAINT chk_activos_estado CHECK (estado IN ('ACTIVO', 'EN_MANTENIMIENTO', 'DADO_DE_BAJA')),
  CONSTRAINT fk_activos_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- ================= RIESGOS Y BITÁCORA =================
CREATE TABLE riesgos (
  id                     BIGINT       AUTO_INCREMENT PRIMARY KEY,
  codigo                 VARCHAR(20)  NOT NULL UNIQUE,
  nombre                 VARCHAR(200) NOT NULL,
  descripcion            TEXT,
  activo_id              BIGINT,
  probabilidad           TINYINT      NOT NULL,
  impacto                TINYINT      NOT NULL,
  nivel_valor            TINYINT      AS (probabilidad * impacto) STORED,
  opcion_tratamiento     VARCHAR(20),
  tratamiento            TEXT,
  probabilidad_residual  TINYINT,
  impacto_residual       TINYINT,
  nivel_residual         TINYINT      AS (probabilidad_residual * impacto_residual) STORED,
  estado                 VARCHAR(20)  NOT NULL DEFAULT 'ABIERTO',
  responsable_id         BIGINT,
  creado_en              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_riesgos_probabilidad CHECK (probabilidad BETWEEN 1 AND 5),
  CONSTRAINT chk_riesgos_impacto      CHECK (impacto BETWEEN 1 AND 5),
  CONSTRAINT chk_riesgos_prob_res
    CHECK (probabilidad_residual IS NULL OR probabilidad_residual BETWEEN 1 AND 5),
  CONSTRAINT chk_riesgos_imp_res
    CHECK (impacto_residual IS NULL OR impacto_residual BETWEEN 1 AND 5),
  CONSTRAINT chk_riesgos_opcion
    CHECK (opcion_tratamiento IS NULL OR
           opcion_tratamiento IN ('MITIGAR', 'ACEPTAR', 'TRANSFERIR', 'EVITAR')),
  CONSTRAINT chk_riesgos_estado
    CHECK (estado IN ('ABIERTO', 'EN_TRATAMIENTO', 'MITIGADO', 'ACEPTADO', 'CERRADO')),
  CONSTRAINT fk_riesgos_activo
    FOREIGN KEY (activo_id) REFERENCES activos(id) ON DELETE SET NULL,
  CONSTRAINT fk_riesgos_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE riesgo_bitacora (
  id               BIGINT      AUTO_INCREMENT PRIMARY KEY,
  riesgo_id        BIGINT      NOT NULL,
  usuario_id       BIGINT,
  fecha            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  accion           VARCHAR(30) NOT NULL,
  estado_anterior  VARCHAR(20),
  estado_nuevo     VARCHAR(20),
  nivel_anterior   TINYINT,
  nivel_nuevo      TINYINT,
  comentario       TEXT,
  CONSTRAINT chk_bitacora_accion
    CHECK (accion IN ('CREADO', 'EVALUADO', 'CAMBIO_ESTADO', 'TRATAMIENTO', 'COMENTARIO')),
  CONSTRAINT fk_bitacora_riesgo
    FOREIGN KEY (riesgo_id) REFERENCES riesgos(id) ON DELETE RESTRICT,
  CONSTRAINT fk_bitacora_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
  INDEX idx_bitacora_riesgo_fecha (riesgo_id, fecha)
);

-- ================= CONTROLES ISO/IEC 27001 =================
CREATE TABLE controles (
  id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
  codigo_iso      VARCHAR(10)  NOT NULL UNIQUE,
  nombre          VARCHAR(200) NOT NULL,
  tema            VARCHAR(20)  NOT NULL,
  aplica          BOOLEAN      NOT NULL DEFAULT TRUE,
  justificacion   TEXT,
  estado          VARCHAR(20)  NOT NULL DEFAULT 'NO_IMPLEMENTADO',
  responsable_id  BIGINT,
  actualizado_en  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_controles_tema
    CHECK (tema IN ('ORGANIZACIONAL', 'PERSONAS', 'FISICO', 'TECNOLOGICO')),
  CONSTRAINT chk_controles_estado
    CHECK (estado IN ('NO_IMPLEMENTADO', 'EN_PROGRESO', 'IMPLEMENTADO')),
  CONSTRAINT fk_controles_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE riesgo_control (
  riesgo_id   BIGINT NOT NULL,
  control_id  BIGINT NOT NULL,
  PRIMARY KEY (riesgo_id, control_id),
  CONSTRAINT fk_rc_riesgo  FOREIGN KEY (riesgo_id)  REFERENCES riesgos(id)   ON DELETE CASCADE,
  CONSTRAINT fk_rc_control FOREIGN KEY (control_id) REFERENCES controles(id) ON DELETE CASCADE
);

-- ================= INCIDENTES =================
CREATE TABLE incidentes (
  id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
  codigo           VARCHAR(20)  NOT NULL UNIQUE,
  titulo           VARCHAR(200) NOT NULL,
  tipo             VARCHAR(30)  NOT NULL,
  descripcion      TEXT,
  activo_id        BIGINT,
  severidad        VARCHAR(10)  NOT NULL,
  estado           VARCHAR(20)  NOT NULL DEFAULT 'ABIERTO',
  fecha_deteccion  DATETIME     NOT NULL,
  fecha_cierre     DATETIME,
  acciones         TEXT,
  reportado_por    BIGINT,
  responsable_id   BIGINT,
  creado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_incidentes_tipo CHECK (tipo IN
    ('PHISHING', 'MALWARE', 'ACCESO_NO_AUTORIZADO', 'FUGA_DE_DATOS', 'CAIDA_DE_SERVICIO', 'OTRO')),
  CONSTRAINT chk_incidentes_severidad CHECK (severidad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
  CONSTRAINT chk_incidentes_estado    CHECK (estado IN ('ABIERTO', 'EN_ATENCION', 'CERRADO')),
  CONSTRAINT chk_incidentes_fechas
    CHECK (fecha_cierre IS NULL OR fecha_cierre >= fecha_deteccion),
  CONSTRAINT fk_incidentes_activo
    FOREIGN KEY (activo_id) REFERENCES activos(id) ON DELETE SET NULL,
  CONSTRAINT fk_incidentes_reporta
    FOREIGN KEY (reportado_por) REFERENCES usuarios(id) ON DELETE SET NULL,
  CONSTRAINT fk_incidentes_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- ================= AUDITORÍAS Y HALLAZGOS =================
CREATE TABLE auditorias (
  id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
  codigo           VARCHAR(20)  NOT NULL UNIQUE,
  nombre           VARCHAR(200) NOT NULL,
  tipo             VARCHAR(10)  NOT NULL,
  fecha_auditoria  DATE         NOT NULL,
  auditor          VARCHAR(150),
  archivo_origen   VARCHAR(255),
  importado_por    BIGINT,
  importado_en     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_auditorias_tipo CHECK (tipo IN ('INTERNA', 'EXTERNA')),
  CONSTRAINT fk_auditorias_importado
    FOREIGN KEY (importado_por) REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE hallazgos (
  id                 BIGINT      AUTO_INCREMENT PRIMARY KEY,
  auditoria_id       BIGINT      NOT NULL,
  codigo             VARCHAR(20) NOT NULL,
  tipo               VARCHAR(20) NOT NULL,
  descripcion        TEXT        NOT NULL,
  control_id         BIGINT,
  responsable_id     BIGINT,
  estado             VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
  fecha_limite       DATE,
  accion_correctiva  TEXT,
  CONSTRAINT uq_hallazgo_codigo UNIQUE (auditoria_id, codigo),
  CONSTRAINT chk_hallazgos_tipo
    CHECK (tipo IN ('NC_MAYOR', 'NC_MENOR', 'OBSERVACION', 'OPORTUNIDAD_MEJORA')),
  CONSTRAINT chk_hallazgos_estado CHECK (estado IN ('ABIERTO', 'EN_PROCESO', 'CERRADO')),
  CONSTRAINT fk_hallazgos_auditoria
    FOREIGN KEY (auditoria_id) REFERENCES auditorias(id) ON DELETE CASCADE,
  CONSTRAINT fk_hallazgos_control
    FOREIGN KEY (control_id) REFERENCES controles(id) ON DELETE SET NULL,
  CONSTRAINT fk_hallazgos_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- ================= GESTIÓN DOCUMENTAL =================
CREATE TABLE documentos (
  id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
  codigo            VARCHAR(20)  NOT NULL UNIQUE,
  titulo            VARCHAR(200) NOT NULL,
  tipo              VARCHAR(20)  NOT NULL,
  version           VARCHAR(10)  NOT NULL DEFAULT '1.0',
  estado            VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
  responsable_id    BIGINT,
  archivo_nombre    VARCHAR(255),
  archivo_ruta      VARCHAR(500),
  archivo_tipo      VARCHAR(100),
  archivo_tamano    BIGINT,
  fecha_aprobacion  DATE,
  fecha_revision    DATE,
  creado_en         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_documentos_tipo
    CHECK (tipo IN ('POLITICA', 'PROCEDIMIENTO', 'REGISTRO', 'EVIDENCIA', 'OTRO')),
  CONSTRAINT chk_documentos_estado CHECK (estado IN ('BORRADOR', 'APROBADO', 'OBSOLETO')),
  CONSTRAINT fk_documentos_responsable
    FOREIGN KEY (responsable_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE documento_control (
  documento_id  BIGINT NOT NULL,
  control_id    BIGINT NOT NULL,
  PRIMARY KEY (documento_id, control_id),
  CONSTRAINT fk_dc_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
  CONSTRAINT fk_dc_control   FOREIGN KEY (control_id)   REFERENCES controles(id)  ON DELETE CASCADE
);

-- ================= HISTORIAL (TRAZABILIDAD GENERAL) =================
CREATE TABLE historial (
  id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
  fecha           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  usuario_id      BIGINT,
  entidad         VARCHAR(30)  NOT NULL,
  entidad_id      BIGINT,
  entidad_codigo  VARCHAR(20),
  accion          VARCHAR(20)  NOT NULL,
  detalle         VARCHAR(500),
  CONSTRAINT chk_historial_accion
    CHECK (accion IN ('CREAR', 'EDITAR', 'ELIMINAR', 'IMPORTAR', 'INICIAR_SESION')),
  CONSTRAINT fk_historial_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
  INDEX idx_historial_fecha (fecha),
  INDEX idx_historial_entidad (entidad, entidad_id)
);