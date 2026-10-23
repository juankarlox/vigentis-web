-- V5: el contenido de los documentos se guarda en la base de datos,
-- para que no se pierda cuando la aplicación se publica en internet
CREATE TABLE documento_archivos (
  documento_id  BIGINT   PRIMARY KEY,
  contenido     LONGBLOB NOT NULL,
  CONSTRAINT fk_da_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE
);
