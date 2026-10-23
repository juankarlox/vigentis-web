package com.vigentis.controles;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ControlRequest(
        @NotNull(message = "Indica si el control aplica")
        Boolean aplica,

        @Size(max = 2000, message = "Máximo 2000 caracteres")
        String justificacion,

        Control.Estado estado,
        Long responsableId
) {}