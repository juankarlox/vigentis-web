-- V4: criterios de riesgo definidos por la organización y ciclo completo del riesgo
-- ISO/IEC 27001: 6.1.2 a) criterios de aceptación, 6.1.3 f) aprobación del riesgo residual, 8.2 revisión periódica

-- ===== Criterios de riesgo (una sola fila, la edita la organización) =====
CREATE TABLE criterios_riesgo (
  id                    TINYINT     PRIMARY KEY,
  umbral_medio          TINYINT     NOT NULL DEFAULT 5,
  umbral_alto           TINYINT     NOT NULL DEFAULT 10,
  umbral_critico        TINYINT     NOT NULL DEFAULT 20,
  nivel_aceptable       TINYINT     NOT NULL DEFAULT 4,
  meses_revision        TINYINT     NOT NULL DEFAULT 6,
  escala_probabilidad   TEXT,
  escala_impacto        TEXT,
  actualizado_por       BIGINT,
  actualizado_en        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_criterios_unica  CHECK (id = 1),
  CONSTRAINT chk_criterios_orden  CHECK (umbral_medio >= 2 AND umbral_medio < umbral_alto
                                         AND umbral_alto < umbral_critico AND umbral_critico <= 25),
  CONSTRAINT chk_criterios_acept  CHECK (nivel_aceptable BETWEEN 1 AND 25),
  CONSTRAINT chk_criterios_meses  CHECK (meses_revision BETWEEN 1 AND 36),
  CONSTRAINT fk_criterios_usuario FOREIGN KEY (actualizado_por) REFERENCES usuarios(id) ON DELETE SET NULL
);

INSERT INTO criterios_riesgo (id, escala_probabilidad, escala_impacto) VALUES (1,
'Rara: casi imposible que ocurra (menos de una vez en 5 años)
Improbable: podría ocurrir alguna vez (una vez en 2 a 5 años)
Posible: puede ocurrir (una vez al año)
Probable: ocurre con frecuencia (varias veces al año)
Casi segura: se espera que ocurra (mensual o más)',
'Insignificante: sin efecto apreciable en la operación ni en los datos
Menor: molestias internas, se corrige en horas
Moderado: afecta un proceso o datos internos, se corrige en días
Mayor: afecta clientes o datos personales, pérdidas económicas o legales
Catastrófico: detiene la operación o expone información crítica');

-- ===== Nuevos datos del riesgo =====
ALTER TABLE riesgos
  ADD COLUMN amenaza                  VARCHAR(200) AFTER descripcion,
  ADD COLUMN vulnerabilidad           VARCHAR(200) AFTER amenaza,
  ADD COLUMN fecha_revision           DATE         AFTER responsable_id,
  ADD COLUMN aceptado_por             BIGINT       AFTER fecha_revision,
  ADD COLUMN aceptado_en              DATETIME     AFTER aceptado_por,
  ADD COLUMN justificacion_aceptacion TEXT         AFTER aceptado_en,
  ADD CONSTRAINT fk_riesgos_aceptado FOREIGN KEY (aceptado_por) REFERENCES usuarios(id) ON DELETE SET NULL;

-- Los riesgos existentes quedan con revisión dentro de 6 meses
UPDATE riesgos SET fecha_revision = DATE_ADD(CURDATE(), INTERVAL 6 MONTH) WHERE fecha_revision IS NULL;

-- ===== Bitácora: dos acciones nuevas =====
ALTER TABLE riesgo_bitacora DROP CHECK chk_bitacora_accion;
ALTER TABLE riesgo_bitacora ADD CONSTRAINT chk_bitacora_accion
  CHECK (accion IN ('CREADO', 'EVALUADO', 'CAMBIO_ESTADO', 'TRATAMIENTO', 'COMENTARIO', 'ACEPTACION', 'REVISION'));

-- ===== Permisos nuevos =====
INSERT INTO permisos (codigo, modulo, descripcion) VALUES
('RIESGOS_ACEPTAR',     'RIESGOS', 'Aprobar formalmente la aceptación de riesgos (dueño del riesgo)'),
('CRITERIOS_GESTIONAR', 'RIESGOS', 'Definir los criterios de evaluación y aceptación de riesgos');

-- El administrador tiene todos los permisos; el Responsable del SGSI aprueba y define criterios
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p ON p.codigo IN ('RIESGOS_ACEPTAR', 'CRITERIOS_GESTIONAR')
WHERE r.es_sistema = TRUE OR r.nombre = 'Responsable del SGSI';
