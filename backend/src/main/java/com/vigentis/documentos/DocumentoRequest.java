package com.vigentis.documentos;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record DocumentoRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "Máximo 200 caracteres")
        String titulo,

        @NotNull(message = "El tipo es obligatorio")
        Documento.Tipo tipo,

        @Size(max = 10, message = "Máximo 10 caracteres")
        String version,

        // Solo BORRADOR u OBSOLETO; para aprobar se usa la acción "aprobar"
        Documento.Estado estado,

        Long responsableId,
        LocalDate fechaRevision
) {}
