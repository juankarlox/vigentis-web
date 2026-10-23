package com.vigentis.documentos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 5) AS SIGNED)), 0) FROM documentos WHERE empresa_id = :empresa",
           nativeQuery = true)
    Long ultimoNumero(@Param("empresa") Long empresa);

    @Query("""
        SELECT d FROM Documento d
        WHERE (:texto IS NULL
               OR LOWER(d.titulo) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(d.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:tipo IS NULL OR d.tipo = :tipo)
          AND (:estado IS NULL OR d.estado = :estado)
        ORDER BY d.codigo
        """)
    List<Documento> buscar(@Param("texto") String texto,
                           @Param("tipo") Documento.Tipo tipo,
                           @Param("estado") Documento.Estado estado);

    // ----- Relación documento <-> control (tabla documento_control) -----

    @Query(value = """
        SELECT d.* FROM documentos d
        JOIN documento_control dc ON dc.documento_id = d.id
        WHERE dc.control_id = :controlId
        ORDER BY d.codigo
        """, nativeQuery = true)
    List<Documento> buscarPorControl(@Param("controlId") Long controlId);

    @Query(value = "SELECT control_id FROM documento_control WHERE documento_id = :documentoId",
           nativeQuery = true)
    List<Long> idsControles(@Param("documentoId") Long documentoId);

    @Query(value = "SELECT COUNT(*) FROM documento_control WHERE documento_id = :d AND control_id = :c",
           nativeQuery = true)
    long contarVinculo(@Param("d") Long documentoId, @Param("c") Long controlId);

    @Modifying
    @Query(value = "INSERT INTO documento_control (documento_id, control_id) VALUES (:d, :c)",
           nativeQuery = true)
    void vincular(@Param("d") Long documentoId, @Param("c") Long controlId);

    @Modifying
    @Query(value = "DELETE FROM documento_control WHERE documento_id = :d AND control_id = :c",
           nativeQuery = true)
    int desvincular(@Param("d") Long documentoId, @Param("c") Long controlId);
}
