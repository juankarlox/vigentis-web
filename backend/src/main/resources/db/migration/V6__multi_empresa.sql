-- V6: Vigentis multi-empresa
-- Una sola aplicación web: cada empresa se registra y solo ve su propia información.
-- Lo que existía hasta ahora queda en la "Empresa demo" (id = 1).

CREATE TABLE empresas (
  id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
  nombre     VARCHAR(150) NOT NULL,
  nit        VARCHAR(30),
  sector     VARCHAR(60),
  activa     BOOLEAN      NOT NULL DEFAULT TRUE,
  creado_en  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO empresas (id, nombre, sector) VALUES (1, 'Empresa demo', 'Demostración');

-- Catálogo maestro de los 93 controles del Anexo A: se copia a cada empresa nueva
CREATE TABLE catalogo_controles (
  codigo_iso  VARCHAR(10)  PRIMARY KEY,
  orden       INT          NOT NULL,
  nombre      VARCHAR(200) NOT NULL,
  tema        VARCHAR(20)  NOT NULL
);
INSERT INTO catalogo_controles (codigo_iso, orden, nombre, tema)
SELECT codigo_iso, id, nombre, tema FROM controles;

ALTER TABLE roles ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE roles ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE roles ADD CONSTRAINT fk_roles_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE usuarios ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE usuarios ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE activos ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE activos ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE activos ADD CONSTRAINT fk_activos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE riesgos ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE riesgos ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE riesgos ADD CONSTRAINT fk_riesgos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE riesgo_bitacora ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE riesgo_bitacora ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE riesgo_bitacora ADD CONSTRAINT fk_riesgo_bitacora_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE controles ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE controles ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE controles ADD CONSTRAINT fk_controles_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE incidentes ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE incidentes ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE incidentes ADD CONSTRAINT fk_incidentes_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE auditorias ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE auditorias ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE auditorias ADD CONSTRAINT fk_auditorias_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE hallazgos ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE hallazgos ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE hallazgos ADD CONSTRAINT fk_hallazgos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE documentos ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE documentos ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE documentos ADD CONSTRAINT fk_documentos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE historial ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE historial ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE historial ADD CONSTRAINT fk_historial_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;
ALTER TABLE criterios_riesgo ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE criterios_riesgo ALTER COLUMN empresa_id DROP DEFAULT;
ALTER TABLE criterios_riesgo ADD CONSTRAINT fk_criterios_riesgo_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE;

-- ===== Los códigos y nombres pasan a ser únicos POR EMPRESA (cada una tiene su ACT-001) =====
ALTER TABLE roles      DROP INDEX nombre,     ADD CONSTRAINT uq_roles_empresa      UNIQUE (empresa_id, nombre);
ALTER TABLE activos    DROP INDEX codigo,     ADD CONSTRAINT uq_activos_empresa    UNIQUE (empresa_id, codigo);
ALTER TABLE riesgos    DROP INDEX codigo,     ADD CONSTRAINT uq_riesgos_empresa    UNIQUE (empresa_id, codigo);
ALTER TABLE controles  DROP INDEX codigo_iso, ADD CONSTRAINT uq_controles_empresa  UNIQUE (empresa_id, codigo_iso);
ALTER TABLE incidentes DROP INDEX codigo,     ADD CONSTRAINT uq_incidentes_empresa UNIQUE (empresa_id, codigo);
ALTER TABLE auditorias DROP INDEX codigo,     ADD CONSTRAINT uq_auditorias_empresa UNIQUE (empresa_id, codigo);
ALTER TABLE documentos DROP INDEX codigo,     ADD CONSTRAINT uq_documentos_empresa UNIQUE (empresa_id, codigo);

-- ===== Criterios de riesgo: una fila por empresa =====
ALTER TABLE criterios_riesgo DROP CHECK chk_criterios_unica;
ALTER TABLE criterios_riesgo MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE criterios_riesgo ADD CONSTRAINT uq_criterios_empresa UNIQUE (empresa_id);

-- El correo sigue siendo único en toda la plataforma: con él se sabe a qué empresa pertenece cada persona
