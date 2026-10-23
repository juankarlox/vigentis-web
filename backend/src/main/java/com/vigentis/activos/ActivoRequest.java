package com.vigentis.activos;

import jakarta.validation.constraints.*;

public record ActivoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "Máximo 150 caracteres")
        String nombre,

        @NotNull(message = "El tipo es obligatorio")
        Activo.Tipo tipo,

        @NotNull(message = "La confidencialidad es obligatoria")
        @Min(value = 1, message = "Debe estar entre 1 y 3") @Max(value = 3, message = "Debe estar entre 1 y 3")
        Integer confidencialidad,

        @NotNull(message = "La integridad es obligatoria")
        @Min(value = 1, message = "Debe estar entre 1 y 3") @Max(value = 3, message = "Debe estar entre 1 y 3")
        Integer integridad,

        @NotNull(message = "La disponibilidad es obligatoria")
        @Min(value = 1, message = "Debe estar entre 1 y 3") @Max(value = 3, message = "Debe estar entre 1 y 3")
        Integer disponibilidad,

        Activo.Estado estado,
        String descripcion,
        Long responsableId
) {}