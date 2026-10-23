package com.vigentis.incidentes;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record IncidenteRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "Máximo 200 caracteres")
        String titulo,

        @NotNull(message = "El tipo es obligatorio")
        Incidente.Tipo tipo,

        String descripcion,
        Long activoId,

        @NotNull(message = "La severidad es obligatoria")
        Incidente.Severidad severidad,

        Incidente.Estado estado,

        @NotNull(message = "La fecha de detección es obligatoria")
        @PastOrPresent(message = "La fecha de detección no puede estar en el futuro")
        LocalDateTime fechaDeteccion,

        LocalDateTime fechaCierre,
        String acciones,
        Long responsableId
) {}
