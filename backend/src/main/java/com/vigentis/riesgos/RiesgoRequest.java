package com.vigentis.riesgos;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RiesgoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "Máximo 200 caracteres")
        String nombre,

        String descripcion,

        @Size(max = 200, message = "Máximo 200 caracteres")
        String amenaza,

        @Size(max = 200, message = "Máximo 200 caracteres")
        String vulnerabilidad,

        Long activoId,

        @NotNull(message = "La probabilidad es obligatoria")
        @Min(value = 1, message = "Debe estar entre 1 y 5") @Max(value = 5, message = "Debe estar entre 1 y 5")
        Integer probabilidad,

        @NotNull(message = "El impacto es obligatorio")
        @Min(value = 1, message = "Debe estar entre 1 y 5") @Max(value = 5, message = "Debe estar entre 1 y 5")
        Integer impacto,

        Riesgo.Opcion opcionTratamiento,
        String tratamiento,

        @Min(value = 1, message = "Debe estar entre 1 y 5") @Max(value = 5, message = "Debe estar entre 1 y 5")
        Integer probabilidadResidual,

        @Min(value = 1, message = "Debe estar entre 1 y 5") @Max(value = 5, message = "Debe estar entre 1 y 5")
        Integer impactoResidual,

        Riesgo.Estado estado,
        Long responsableId,
        LocalDate fechaRevision
) {}
