package com.vigentis.auditorias;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record HallazgoRequest(
        @Size(max = 20, message = "Máximo 20 caracteres")
        String codigo,

        @NotNull(message = "El tipo es obligatorio")
        Hallazgo.Tipo tipo,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        Long controlId,
        Long responsableId,
        Hallazgo.Estado estado,
        LocalDate fechaLimite,
        String accionCorrectiva
) {}
